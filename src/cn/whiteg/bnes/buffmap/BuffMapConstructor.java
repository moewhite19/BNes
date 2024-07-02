package cn.whiteg.bnes.buffmap;

import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.List;

public interface BuffMapConstructor {
    List<MapItemSavedData.MapPatch> makeUpdate(byte[] bytes);

    byte[] getBuff();
}
