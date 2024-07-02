package cn.whiteg.bnes.buffmap;

import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.Collections;
import java.util.List;

//无区域更新,直接发送完整画面
public class NoneConstructor implements BuffMapConstructor {
    @Override
    public List<MapItemSavedData.MapPatch> makeUpdate(byte[] bytes) {
        return Collections.singletonList(new MapItemSavedData.MapPatch(0,0,128,128,bytes));
    }

    @Override
    public byte[] getBuff() {
        return new byte[128 * 128];
    }
}
