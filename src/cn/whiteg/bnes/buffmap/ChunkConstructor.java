package cn.whiteg.bnes.buffmap;

import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayList;
import java.util.List;

public class ChunkConstructor implements BuffMapConstructor {
    private final int number; //区块数量
    private final int width; //单个区块屏幕宽度
    byte[] buff = new byte[16384];

    /**
     * 区块缓存图片地图更新器
     *
     * @param image 图片输入
     * @param size  尺寸
     */
    public ChunkConstructor(int size) {
        double dValue = 128d / size;
        this.width = (int) dValue;
        if (dValue != width) throw new IllegalArgumentException("invalid size:" + size + "");
        this.number = size * size;
    }

    //矩形更新
    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public List<MapItemSavedData.MapPatch> makeUpdate(byte[] bytes) {
        var chunks = new ArrayList<MapItemSavedData.MapPatch>(number);
        //扫描区块
        for (int mod_x = 0; mod_x < 128; mod_x += width) {
            for (int mod_y = 0; mod_y < 128; mod_y += width) {
                //每个区块的处理
                int end_x = mod_x + width, end_y = mod_y + width;
                loop:
                for (int y = mod_y; y < end_y; y++) {
                    int line = y * 128;
                    for (int x = mod_x; x < end_x; x++) {
                        int index = line + x;
                        if (bytes[index] != buff[index]){ //有变化
                            var rectangle = new byte[width * width];
                            for (x = 0; x < width; x++) {
                                for (y = 0; y < width; y++) {
                                    rectangle[y * width + x] = bytes[(mod_y + y) * 128 + x + mod_x];
                                }
                            }
                            //调试，更新区域闪烁
//                            if (BNes.plugin.setting.DEBUG){
//                                byte b = (byte) new Random().nextInt(2);
//                                for (int i = 0; i < rectangle.length; i++) {
//                                    rectangle[i] = (byte) (rectangle[i] + b);
//                                }
//                            }
                            chunks.add(new MapItemSavedData.MapPatch(mod_x,mod_y,width,width,rectangle));
                            break loop;
                        }
                    }
                }
            }
        }

        if (chunks.isEmpty()) return null;
        System.arraycopy(bytes,0,buff,0,bytes.length);
        return chunks;
    }

    @Override
    public byte[] getBuff() {
        return buff;
    }
}
