package cn.whiteg.bnes.buffmap;

import net.minecraft.world.level.saveddata.maps.WorldMap;

import java.util.ArrayList;
import java.util.List;

public class CAndSConstructor implements BuffMapConstructor {
    private final int number; //区块数量
    private final int width; //单个区块屏幕宽度
    byte[] buff = new byte[16384];

    /**
     * 矩形加区块缓存图片地图更新器
     *
     * @param size 分割成多少行
     */
    public CAndSConstructor(int size) {
        double dValue = 128d / size;
        this.width = (int) dValue;
        if (dValue != width) throw new IllegalArgumentException("invalid size:" + size + "");
        this.number = size * size;
    }

    //矩形更新
    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public List<WorldMap.b> makeUpdate(byte[] bytes) {
        var chunks = new ArrayList<WorldMap.b>(number);
        //扫描区块
        for (int start_x = 0; start_x < 128; start_x += width) {
            for (int start_y = 0; start_y < 128; start_y += width) {
                //每个区块的处理
                int m_x = start_x, m_y = -1, end_x = start_x + width - 1, end_y = start_y + width - 1;
                //从上往下扫描执行第一次扫描
                loop:
                for (int y = start_y; y <= end_y; y++) {
                    for (int x = start_x; x <= end_x; x++) {
                        int index = y * 128 + x;
                        if (bytes[index] != buff[index]){
                            m_y = y;
                            break loop;
                        }
                    }
                }
                //无更新
                if (m_y == -1){
                    continue;
                }
                //再从下往上来一遍
                loop:
                for (int y = end_y; y >= m_y; y--) {
                    for (int x = m_x; x <= end_x; x++) {
                        int index = (y * 128) + x;
                        if (bytes[index] != buff[index]){
                            end_y = y;
                            break loop;
                        }
                    }
                }
                //从左往右
                loop:
                for (int x = m_x; x <= end_x; x++) {
                    for (int y = m_y; y <= end_y; y++) {
                        int index = (y * 128) + x;
                        if (bytes[index] != buff[index]){
                            m_x = x;
                            break loop;
                        }
                    }
                }
                //从右往左
                loop:
                for (int x = end_x; x >= m_x; x--) {
                    for (int y = m_y; y <= end_y; y++) {
                        int index = (y * 128) + x;
                        if (bytes[index] != buff[index]){
                            end_x = x;
                            break loop;
                        }
                    }
                }
                //是否矩形发送
                int w = end_x - m_x + 1, h = end_y - m_y + 1;
                var rectangle = new byte[w * h];

                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        int px = x + m_x, py = y + m_y;
                        rectangle[y * w + x] = bytes[py * 128 + px];
                    }
                }

                //调试，更新区域闪烁
//                byte b = (byte) new Random().nextInt(2);
//                for (int i = 0; i < rectangle.length; i++) {
//                    rectangle[i] = (byte) (rectangle[i] + b);
//                }

                chunks.add(new WorldMap.b(m_x,m_y,w,h,rectangle));

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
