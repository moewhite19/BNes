package cn.whiteg.bnes.nms;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;

public interface PlayerNms {
    static PlayerNms getInstance() {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        String version = packageName.substring(packageName.lastIndexOf('.') + 1);
        var me = PlayerNms.class;
        //为反射无法适应的版本做适配，类名格式为PlayerNms_{服务端版本号}
        //例如PlayerNms_v1_18_R1
        String name = me.getPackageName() + "." + me.getSimpleName() + "_" + version;
        try{
            var clazz = me.getClassLoader().loadClass(name);
            PlayerNms playerNms = (PlayerNms) clazz.getConstructor().newInstance();
            playerNms.test();
            return playerNms;
        }catch (Exception ignored){
        }
        //如果没有适配的反射类,返回通用反射类
        try{
            return new PlayerNms_Paper();
        }catch (Exception e){
            return new PlayerNms_Ref();
        }
    }

    boolean getJumping(LivingEntity entity);

    //获取玩家控制坐骑的平行x轴
    float getInputX(LivingEntity entity);

    //获取玩家控制坐骑的前后y轴
    float getInputZ(LivingEntity entity);

    //??用处不明
    float getInputY(LivingEntity entity);

    //发送数据包
    void sendPacket(Player player,Packet<?>... packets);

    default void sendMap(Player player,int id,byte[] colors) {
        sendPacket(player,createMapPacket(id,colors));
    }

    static ClientboundMapItemDataPacket createMapPacket(int id,byte[] colors) {
        return new ClientboundMapItemDataPacket(new MapId(id),(byte) 1,true,Optional.empty(),Optional.of(new MapItemSavedData.MapPatch(0,0,128,128,colors)));
    }

    static ClientboundMapItemDataPacket createMapPacket(int id,MapItemSavedData.MapPatch mapPatch,byte scale) {
        return new ClientboundMapItemDataPacket(new MapId(id),scale,true,Optional.empty(),Optional.of(mapPatch));
    }

    static ClientboundMapItemDataPacket createMapPacket(int id,MapItemSavedData.MapPatch mapPatch) {
        return createMapPacket(id,mapPatch,(byte) 1);
    }

    default void addInvOrDrop(Inventory inv,ItemStack... itemStacks) {
        var callBack = inv.addItem(itemStacks);
        if (!callBack.isEmpty()){
            for (Map.Entry<Integer, ItemStack> entry : callBack.entrySet()) {
                if (inv.getHolder() instanceof Player player){
                    player.getWorld().dropItem(player.getLocation(),entry.getValue());
                }
            }
        }
    }


    @SuppressWarnings("RedundantThrows")
    default void test() throws Exception {
    }
}
