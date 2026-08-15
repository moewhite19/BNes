package cn.whiteg.bnes.buffmap;

import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能脏矩形地图更新器（smart 模式）
 * <p>
 * 自动识别画面中发生变化的区域，聚合成矩形分块发送，极大节省带宽。
 * 算法核心见 {@link DirtyRectComputer}，本类负责将算法结果转换为
 * Minecraft 的 {@link MapItemSavedData.MapPatch}。
 * <p>
 * 相比其它模式：
 * <ul>
 *   <li>scan：只发一个包围所有变化的大矩形，上下两处小更新会浪费整个区域</li>
 *   <li>chunk：固定网格分块，不合并相邻块、不裁剪块内区域</li>
 *   <li>cas：固定网格内裁剪，但跨块相邻区域不会合并</li>
 *   <li><b>smart：自动检测、自动合并、像素级裁剪、带宽预算</b></li>
 * </ul>
 */
public class SmartConstructor implements BuffMapConstructor {
    final DirtyRectComputer computer;

    public SmartConstructor() {
        this(8,0);
    }

    public SmartConstructor(int blockSize) {
        this(blockSize,0);
    }

    /**
     * @param blockSize        检测块大小(像素)，须能被128整除，越小越精细、扫描越慢。默认8
     * @param maxBytesPerFrame 每帧最大发送数据量(字节)，0为不限。超出部分延迟到下一帧发送
     */
    public SmartConstructor(int blockSize,int maxBytesPerFrame) {
        this.computer = new DirtyRectComputer(blockSize,maxBytesPerFrame);
    }

    @Override
    public List<MapItemSavedData.MapPatch> makeUpdate(byte[] bytes) {
        List<RectData> rects = computer.compute(bytes);
        if (rects.isEmpty()) return null;
        List<MapItemSavedData.MapPatch> patches = new ArrayList<>(rects.size());
        for (RectData rectData : rects){
            patches.add(new MapItemSavedData.MapPatch(rectData.x,rectData.y,rectData.w,rectData.h,rectData.pixels));
        }
        return patches;
    }

    @Override
    public byte[] getBuff() {
        return computer.getBuff();
    }

    @Override
    public void setDebug(boolean debug) {
        computer.setDebug(debug);
    }
}
