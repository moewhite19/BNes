package cn.whiteg.bnes.buffmap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 智能脏矩形计算核心（纯算法，不依赖任何 Minecraft 类，便于独立测试）
 * <p>
 * 相比固定的网格分块，该算法能够自动识别画面实际发生变化的区域，将其按矩形
 * 分块合并后发送，大幅节省带宽：
 * <ol>
 *   <li><b>块级扫描</b>：将 128x128 画面按 blockSize 划分网格，逐块对比上一帧缓存，标记脏块</li>
 *   <li><b>全帧兜底</b>：总变化像素超过画面 1/3 时直接发送整帧 —— 此时一个全帧包反而比几十个小包更省带宽</li>
 *   <li><b>贪心聚合</b>：脏块按"向右延伸 + 向下整行扩展"策略合并成矩形，相邻变化区域自动合并为一块发送</li>
 *   <li><b>像素级收缩</b>：每个矩形再裁剪掉四周没有变化的边缘行/列，把块粒度的矩形收敛到像素级</li>
 *   <li><b>稀疏拆分</b>：若矩形内部变化过于稀疏(空洞大)，按行段拆分为多个精确小矩形，避免浪费</li>
 *   <li><b>带宽预算</b>：超过每帧预算的矩形进入待发送队列，下一帧优先发送，平抑峰值带宽</li>
 * </ol>
 * <p>
 * 典型 NES 画面场景：
 * <ul>
 *   <li>滚屏 = 一条垂直条带变化，聚合后仅需 1 个窄矩形</li>
 *   <li>精灵动画 = 若干小区域，各自独立成块，互不干扰</li>
 *   <li>菜单/场景切换 = 大面积变化，直接全帧发送</li>
 * </ul>
 */
public class DirtyRectComputer {
    public static final int SIZE = 128;

    final int blockSize;               //检测块大小(像素),默认8
    final int blockCount;              //每行块数量 = SIZE / blockSize
    final byte[] buff = new byte[SIZE * SIZE]; //上一帧缓存
    final boolean[] dirty;             //脏块标记位图
    final ArrayDeque<RectData> pending = new ArrayDeque<>(); //超出预算延迟到下一帧的矩形
    final int maxBytesPerFrame;        //每帧最大发送数据量(像素字节数),0为不限
    final int fullFrameThreshold;      //总变化量超过此值时直接发送全帧
    final List<RectData> flashRegions = new ArrayList<>(8); //DEBUG模式下闪烁过的区域(用于恢复)
    boolean debug;                     //DEBUG模式: 更新区域闪烁
    int flashTick;                     //闪烁帧计数器

    public DirtyRectComputer(int blockSize) {
        this(blockSize,0,false);
    }

    public DirtyRectComputer(int blockSize,int maxBytesPerFrame) {
        this(blockSize,maxBytesPerFrame,false);
    }

    public DirtyRectComputer(int blockSize,int maxBytesPerFrame,boolean debug) {
        if (blockSize <= 0 || SIZE % blockSize != 0){
            throw new IllegalArgumentException("invalid block size: " + blockSize + " (must be a divisor of " + SIZE + ")");
        }
        this.blockSize = blockSize;
        this.blockCount = SIZE / blockSize;
        this.dirty = new boolean[blockCount * blockCount];
        this.maxBytesPerFrame = maxBytesPerFrame;
        this.debug = debug;
        //变化超过画面1/3时直接全帧，比几十个小包加起来更省带宽
        this.fullFrameThreshold = SIZE * SIZE / 3;
    }

    /**
     * 开关DEBUG闪烁模式。开启后更新区域会闪烁高亮，方便观察画面分区更新情况；
     * 关闭后残留的闪烁像素会自动恢复为真实画面
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * 对比新帧，计算出需要发送的矩形列表（为空表示无变化）
     * 发送前请勿修改传入的 bytes，计算过程中会读取它并更新内部缓存
     */
    public List<RectData> compute(byte[] bytes) {
        if (bytes == null || bytes.length != SIZE * SIZE) return Collections.emptyList();

        //待发送队列积压超过半帧数据量时直接全帧并清空，避免画面无限滞后
        if (pendingBytes() > SIZE * SIZE / 2){
            pending.clear();
            System.arraycopy(bytes,0,buff,0,buff.length);
            return Collections.singletonList(fullFrame(bytes));
        }

        //1. 块级扫描，标记脏块并统计变化像素数
        int diff = scanBlocks(bytes);

        //2. 大面积变化，直接全帧
        if (diff >= fullFrameThreshold){
            pending.clear();
            System.arraycopy(bytes,0,buff,0,buff.length);
            return Collections.singletonList(fullFrame(bytes));
        }

        //3. 恢复上一帧闪烁残留的区域(区域静止或DEBUG关闭时, 把真实画面刷回来)
        List<RectData> restores = null;
        if (!flashRegions.isEmpty()){
            restores = new ArrayList<>(4);
            collectRestores(bytes,restores);
        }

        //无变化且无积压且无恢复
        if (diff == 0 && pending.isEmpty() && (restores == null || restores.isEmpty())){
            return Collections.emptyList();
        }

        //4. 聚合脏块为矩形
        List<RectData> patches = new ArrayList<>(16);
        if (diff > 0){
            for (Rect rect : mergeBlocks()){
                RectData patch = trimToPatch(rect,bytes);
                if (patch == null) continue;
                //5. 稀疏矩形拆分
                List<RectData> split = splitIfSparse(patch,bytes);
                if (split != null) patches.addAll(split);
                else patches.add(patch);
            }
        }

        //6. DEBUG闪烁: 更新矩形加白色边框高亮并整体亮度交替
        if (debug && !patches.isEmpty()){
            flashRegions.clear();
            for (RectData patch : patches){
                applyDebugFlash(patch);
                flashRegions.add(patch);
            }
        }

        //7. 恢复patch优先发送, 再发新矩形
        if (restores != null && !restores.isEmpty()){
            List<RectData> all = new ArrayList<>(restores.size() + patches.size());
            all.addAll(restores);
            all.addAll(patches);
            patches = all;
        }

        //8. 合并积压队列并按预算裁剪
        patches = applyBudget(patches);

        if (patches.isEmpty()) return Collections.emptyList();
        System.arraycopy(bytes,0,buff,0,buff.length);
        return patches;
    }

    public byte[] getBuff() {
        return buff;
    }

    /**
     * 待发送队列中的总数据量
     */
    public int pendingBytes() {
        int total = 0;
        for (RectData rectData : pending){
            total += rectData.size();
        }
        return total;
    }

    public int getPendingCount() {
        return pending.size();
    }

    //--------------------------------------- 内部实现 ---------------------------------------

    /**
     * 块级扫描：逐块对比缓存帧，标记脏块，返回变化像素总数
     */
    private int scanBlocks(byte[] bytes) {
        int diff = 0;
        int di = 0;
        for (int by = 0; by < blockCount; by++){
            int baseY = by * blockSize * SIZE;
            for (int bx = 0; bx < blockCount; bx++){
                int baseX = bx * blockSize;
                boolean blockDirty = false;
                for (int py = 0; py < blockSize; py++){
                    int line = baseY + py * SIZE + baseX;
                    for (int px = 0; px < blockSize; px++){
                        if (bytes[line + px] != buff[line + px]){
                            blockDirty = true;
                            diff++;
                        }
                    }
                }
                dirty[di++] = blockDirty;
            }
        }
        return diff;
    }

    /**
     * 脏块贪心聚合：向右延伸连续脏块，再向下扩展"整行都脏"的行，合并为矩形
     */
    private List<Rect> mergeBlocks() {
        List<Rect> rects = new ArrayList<>(32);
        for (int y = 0; y < blockCount; y++){
            int base = y * blockCount;
            for (int x = 0; x < blockCount; ){
                if (!dirty[base + x]){
                    x++;
                    continue;
                }
                //向右延伸
                int x2 = x;
                while (x2 + 1 < blockCount && dirty[base + x2 + 1]) x2++;
                //向下扩展：下一行 [x, x2] 全脏才扩展
                int y2 = y;
                while (y2 + 1 < blockCount){
                    int nextBase = (y2 + 1) * blockCount;
                    boolean all = true;
                    for (int xx = x; xx <= x2; xx++){
                        if (!dirty[nextBase + xx]){
                            all = false;
                            break;
                        }
                    }
                    if (!all) break;
                    y2++;
                }
                //清除已被矩形覆盖的脏块标记
                for (int yy = y; yy <= y2; yy++){
                    int yyBase = yy * blockCount;
                    for (int xx = x; xx <= x2; xx++) dirty[yyBase + xx] = false;
                }
                rects.add(new Rect(x * blockSize,y * blockSize,x2 * blockSize + blockSize - 1,y2 * blockSize + blockSize - 1));
                x = x2 + 1;
            }
        }
        return rects;
    }

    /**
     * 像素级收缩：裁剪掉矩形四周没有变化的边缘，并将矩形内容拷贝为独立像素数组
     */
    private RectData trimToPatch(Rect rect,byte[] bytes) {
        int x1 = rect.x1, y1 = rect.y1, x2 = rect.x2, y2 = rect.y2;
        //上边缘收缩
        while (y1 <= y2 && rowClean(bytes,y1,x1,x2)) y1++;
        if (y1 > y2) return null;
        //下边缘收缩
        while (y2 >= y1 && rowClean(bytes,y2,x1,x2)) y2--;
        //左边缘收缩
        while (x1 <= x2 && colClean(bytes,x1,y1,y2)) x1++;
        if (x1 > x2) return null;
        //右边缘收缩
        while (x2 >= x1 && colClean(bytes,x2,y1,y2)) x2--;
        int w = x2 - x1 + 1, h = y2 - y1 + 1;
        byte[] pixels = new byte[w * h];
        for (int yy = 0; yy < h; yy++){
            System.arraycopy(bytes,(y1 + yy) * SIZE + x1,pixels,yy * w,w);
        }
        return new RectData(x1,y1,w,h,pixels);
    }

    private boolean rowClean(byte[] bytes,int y,int x1,int x2) {
        int line = y * SIZE;
        for (int xx = x1; xx <= x2; xx++){
            if (bytes[line + xx] != buff[line + xx]) return false;
        }
        return true;
    }

    private boolean colClean(byte[] bytes,int x,int y1,int y2) {
        for (int yy = y1; yy <= y2; yy++){
            if (bytes[yy * SIZE + x] != buff[yy * SIZE + x]) return false;
        }
        return true;
    }

    /**
     * 稀疏矩形拆分：若矩形内部实际变化像素占比过低(空洞大)，
     * 按行段拆分为精确的小矩形。最多拆 MAX_SPLIT 个，超出则放弃拆分保持原矩形。
     *
     * @return null 表示不拆分
     */
    private List<RectData> splitIfSparse(RectData patch,byte[] bytes) {
        int area = patch.w * patch.h;
        //小矩形或窄条形不拆
        if (area <= blockSize * blockSize * 2 || patch.w <= blockSize * 2 || patch.h <= blockSize * 2) return null;
        int diff = countDiff(patch.x,patch.y,patch.w,patch.h,bytes);
        //变化占比 >= 50% 说明矩形紧凑，不拆
        if (diff * 10 >= area * 5) return null;

        //按行段拆分，最多 MAX_SPLIT 个
        List<RectData> out = new ArrayList<>(8);
        int max = 12;
        for (int yy = patch.y; yy < patch.y + patch.h && out.size() < max; yy++){
            int line = yy * SIZE;
            int xx = patch.x;
            while (xx < patch.x + patch.w && out.size() < max){
                //跳过无变化像素
                while (xx < patch.x + patch.w && bytes[line + xx] == buff[line + xx]) xx++;
                if (xx >= patch.x + patch.w) break;
                int start = xx;
                while (xx < patch.x + patch.w && bytes[line + xx] != buff[line + xx]) xx++;
                int end = xx - 1;
                byte[] pixels = new byte[end - start + 1];
                for (int i = start; i <= end; i++){
                    pixels[i - start] = bytes[line + i];
                }
                out.add(new RectData(start,yy,end - start + 1,1,pixels));
            }
        }
        return out.size() >= max ? null : out;
    }

    private int countDiff(int x,int y,int w,int h,byte[] bytes) {
        int count = 0;
        for (int yy = y; yy < y + h; yy++){
            int line = yy * SIZE;
            for (int xx = x; xx < x + w; xx++){
                if (bytes[line + xx] != buff[line + xx]) count++;
            }
        }
        return count;
    }

    /**
     * 预算控制：积压队列优先发送，超出预算的矩形延迟到下一帧
     */
    private List<RectData> applyBudget(List<RectData> patches) {
        if (pending.isEmpty() && patches.isEmpty()) return patches;
        //无预算限制：直接合并
        if (maxBytesPerFrame <= 0){
            List<RectData> all = new ArrayList<>(pending.size() + patches.size());
            all.addAll(pending);
            pending.clear();
            all.addAll(patches);
            return all;
        }
        //有预算限制：先发积压，再发新矩形，超出的回积压队列
        List<RectData> all = new ArrayList<>(pending.size() + patches.size());
        all.addAll(pending);
        pending.clear();
        all.addAll(patches);
        List<RectData> allowed = new ArrayList<>(all.size());
        int total = 0;
        //至少允许发送一个矩形，避免预算过小导致永久积压
        int budget = Math.max(maxBytesPerFrame,16);
        for (RectData rectData : all){
            if (allowed.isEmpty() || total + rectData.size() <= budget){
                allowed.add(rectData);
                total += rectData.size();
            } else {
                pending.add(rectData);
            }
        }
        return allowed;
    }

    private RectData fullFrame(byte[] bytes) {
        return new RectData(0,0,SIZE,SIZE,bytes.clone());
    }

    //--------------------------------------- DEBUG闪烁 ---------------------------------------

    public static final byte FLASH_BORDER = (byte) 34; //纯白色, 用于更新矩形边框高亮
    public static final byte FLASH_BORDER_DARK = (byte) 1; //纯黑色, 与白色交替形成闪烁

    /**
     * DEBUG闪烁效果: 更新矩形四周画1像素边框, 边框颜色在纯白/纯黑之间交替闪烁
     * —— 清晰显示分块的位置和大小, 且任何画面背景下都醒目
     * <p>
     * 注意: 只修改边框像素, <b>矩形内部像素保持真实值不变</b>,
     * 否则运动中的画面会显示失真的颜色并留下拖影
     */
    private void applyDebugFlash(RectData patch) {
        int tick = flashTick++;
        //边框颜色交替: 白色/黑色每4帧切换
        byte border = ((tick >> 2) & 1) == 0 ? FLASH_BORDER : FLASH_BORDER_DARK;
        byte[] pixels = patch.pixels;
        int w = patch.w, h = patch.h;
        //矩形足够大时画1像素边框
        if (w > 2 && h > 2){
            for (int x = 0; x < w; x++){
                pixels[x] = border;
                pixels[(h - 1) * w + x] = border;
            }
            for (int y = 0; y < h; y++){
                pixels[y * w] = border;
                pixels[y * w + w - 1] = border;
            }
        }
    }

    /**
     * 收集需要恢复的区域: 遍历上一帧闪烁过的矩形, 对矩形四周的边框逐像素检查,
     * 若某个边框像素"被闪烁污染(闪烁值≠真实值) 且 本帧真实值未变化(新patch不会覆盖它)",
     * 则发送该边的真实像素恢复画面, 彻底消除闪烁残留拖影
     */
    private void collectRestores(byte[] bytes,List<RectData> restores) {
        java.util.Iterator<RectData> it = flashRegions.iterator();
        while (it.hasNext()){
            RectData r = it.next();
            //闪烁只污染矩形四周的1像素边框, 逐边检查并恢复
            restoreIfDirty(bytes,r,0,0,r.w,1,restores);           //上边
            restoreIfDirty(bytes,r,0,r.h - 1,r.w,1,restores);     //下边
            restoreIfDirty(bytes,r,0,1,1,r.h - 2,restores);       //左边
            restoreIfDirty(bytes,r,r.w - 1,1,1,r.h - 2,restores); //右边
            it.remove();
        }
    }

    /**
     * 检查闪烁矩形 r 的子区域 [ox,oy,w,h] 是否有需要恢复的像素,
     * 有则发送该子区域的真实像素恢复画面
     */
    private void restoreIfDirty(byte[] bytes,RectData r,int ox,int oy,int w,int h,List<RectData> restores) {
        if (w <= 0 || h <= 0) return;
        boolean dirty = false;
        for (int yy = 0; yy < h && !dirty; yy++){
            int line = (r.y + oy + yy) * SIZE;
            for (int xx = 0; xx < w; xx++){
                int pos = line + r.x + ox + xx;
                int idx = (oy + yy) * r.w + ox + xx;
                //被闪烁污染(闪烁值≠真实值) 且 本帧真实值未变化(新patch不会覆盖) 则需要恢复
                if (r.pixels[idx] != bytes[pos] && bytes[pos] == buff[pos]){
                    dirty = true;
                    break;
                }
            }
        }
        if (!dirty) return;
        byte[] pixels = new byte[w * h];
        for (int yy = 0; yy < h; yy++){
            System.arraycopy(bytes,(r.y + oy + yy) * SIZE + r.x + ox,pixels,yy * w,w);
        }
        restores.add(new RectData(r.x + ox,r.y + oy,w,h,pixels));
    }

    /**
     * 内部矩形（像素坐标，含端点）
     */
    private static final class Rect {
        final int x1, y1, x2, y2;

        Rect(int x1,int y1,int x2,int y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }
}
