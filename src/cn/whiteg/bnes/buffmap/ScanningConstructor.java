package cn.whiteg.bnes.buffmap;

import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.Collections;
import java.util.List;

public class ScanningConstructor implements BuffMapConstructor {
    byte[] buff = new byte[16384];

    //一个简单的矩形更新程序
    public ScanningConstructor() {
    }

    //矩形更新
    @Override
    public List<MapItemSavedData.MapPatch> makeUpdate(byte[] bytes) {
        //矩形更新
        int mw = 128, mh = 128, mx = 0, my = -1;
        //从上往下扫描执行第一次扫描
        loop:
        for (int y = 0; y < mh; y++) {
            for (int x = 0; x < 128; x++) {
                int index = (y * 128) + x;
                if (bytes[index] != buff[index]){
                    mh -= y;
                    my = y;
                    break loop;
                }
            }
        }
        //无更新
        if (my == -1){
            return null;
        }
        //再从下往上来一遍
        loop:
        for (int y = 127; y > my; y--) {
            for (int x = 0; x < 128; x++) {
                int index = (y * 128) + x;
                if (bytes[index] != buff[index]){
                    mh -= (127 - y);
                    break loop;
                }
            }
        }
        //从左往右
        int eh = my + mh;
        loop:
        for (int x = 0; x < 128; x++) {
            for (int y = my; y < eh; y++) {
                int index = (y * 128) + x;
                if (bytes[index] != buff[index]){
                    mw -= x;
                    mx += x;
                    break loop;
                }
            }
        }
        //从右往左
        loop:
        for (int x = 127; x >= 0; x--) {
            for (int y = my; y < eh; y++) {
                int index = (y * 128) + x;
                if (bytes[index] != buff[index]){
                    mw -= (127 - x);
                    break loop;
                }
            }
        }
        System.arraycopy(bytes,0,buff,0,bytes.length);
//        buff = bytes;
        //是否矩形发送
        if (mh < 128 || mw < 128){
            var rectangle = new byte[mw * mh];
            for (int y = 0; y < mh; y++) {
                for (int x = 0; x < mw; x++) {
                    rectangle[y * mw + x] = bytes[(my + y) * 128 + x + mx];
                }
            }

//            //调试，更新区域闪烁
//            if (BNes.plugin.setting.DEBUG){
//                byte b = (byte) new Random().nextInt(2);
//                for (int i = 0; i < rectangle.length; i++) {
//                    rectangle[i] = (byte) (rectangle[i] + b);
//                }
//            }

            return Collections.singletonList(new MapItemSavedData.MapPatch(mx,my,mw,mh,rectangle));
        }
        return Collections.singletonList(new MapItemSavedData.MapPatch(0,0,128,128,bytes));
    }

    @Override
    public byte[] getBuff() {
        return buff;
    }
}
