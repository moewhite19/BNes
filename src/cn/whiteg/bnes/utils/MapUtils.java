package cn.whiteg.bnes.utils;

import net.minecraft.world.level.saveddata.maps.WorldMap;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapView;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MapUtils {
    public static Map<Integer, Byte> colorCacheMap = Collections.synchronizedMap(new HashMap<Integer, Byte>(64)); //缓存颜色,实际fc的颜色位数只有54,但是以防万一嘛
    static Field getWorldMapField = null;
    static Field getBytesField;

    static {
        //nms获取图片字节组
        try{
            for (Field field : WorldMap.class.getDeclaredFields()) {
                Class<?> type = field.getType();
                if (type.isArray() && type.getComponentType().equals(byte.class)){
                    field.setAccessible(true);
                    getBytesField = field;
                    break;
                }
            }
//            getBytesField = WorldMap.class.getDeclaredField("g");
//            getBytesField.setAccessible(true);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static WorldMap getWorldMap(MapView mapView) {
        if (getWorldMapField == null){
            //寻找WorldMap Field
            findField:
            {
                for (Field field : mapView.getClass().getDeclaredFields()) {
                    if (WorldMap.class.isAssignableFrom(field.getType())){
                        field.setAccessible(true);
                        getWorldMapField = field;
                        break findField; //找到后跳出label
                    }
                }
                throw new RuntimeException("cant find WorldMap field!");
            }
        }

        try{
            return ((WorldMap) getWorldMapField.get(mapView));
        }catch (IllegalAccessException e){
            throw new RuntimeException(e);
        }
    }

    public static byte[] getBytes(WorldMap worldMap) {
        try{
            return (byte[]) getBytesField.get(worldMap);
        }catch (Exception e){
            e.printStackTrace();
            return new byte[128 * 128];
        }
    }

    public static byte[] getBytes(MapView mapView) {
        return getBytes(getWorldMap(mapView));
    }

    public static byte matchColor(int color) {
        return colorCacheMap.computeIfAbsent(color,integer -> MapPalette.matchColor(new Color(color)));
    }

    public static byte[] matchColors(int[] colors) {
        byte[] bytes = new byte[colors.length];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = matchColor(colors[i]);
        }
        return bytes;
    }

}
