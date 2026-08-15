package cn.whiteg.bnes.buffmap;

/**
 * 待发送的矩形区域数据（纯数据类，不依赖任何 Minecraft 类，方便独立测试）
 * <p>
 * 对应 Minecraft 地图更新包中的 MapPatch: (x,y) 为矩形左上角，w/h 为宽高，pixels 为按行优先的像素数组
 */
public class RectData {
    public final int x;
    public final int y;
    public final int w;
    public final int h;
    public final byte[] pixels;

    public RectData(int x,int y,int w,int h,byte[] pixels) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.pixels = pixels;
    }

    /**
     * 像素数据量(字节)
     */
    public int size() {
        return pixels == null ? 0 : pixels.length;
    }

    @Override
    public String toString() {
        return "RectData{" + x + "," + y + " " + w + "x" + h + " size=" + size() + "}";
    }
}
