package cn.whiteg.bnes.buffmap;

import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.List;

public interface BuffMapConstructor {
    List<MapItemSavedData.MapPatch> makeUpdate(byte[] bytes);

    byte[] getBuff();

    /**
     * 开关DEBUG闪烁模式(仅smart模式生效)。开启后更新区域会闪烁高亮, 方便观察画面分区更新情况
     */
    default void setDebug(boolean debug) {
    }
}
