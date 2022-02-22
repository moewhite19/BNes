package cn.whiteg.bnes.buffmap;

import net.minecraft.world.level.saveddata.maps.WorldMap;

import java.util.List;

public interface BuffMapConstructor {
    List<WorldMap.b> makeUpdate(byte[] bytes);

    byte[] getBuff();
}
