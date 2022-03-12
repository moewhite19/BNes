package cn.whiteg.bnes.nms;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.EntityLiving;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

//为游戏版本号v1_17_R1混淆做的适配
@Deprecated(since = "已弃用")
public class PlayerNms_v1_17_R1 implements PlayerNms {
    @Override
    public boolean getJumping(LivingEntity entity) {
        EntityLiving e = ((CraftLivingEntity) entity).getHandle();
        return e.bn;
    }

    //获取玩家控制坐骑的平行x轴
    @Override
    public float getInputX(LivingEntity entity) {
        EntityLiving e = ((CraftLivingEntity) entity).getHandle();
        return e.bo;
    }

    //获取玩家控制坐骑的前后y轴
    @Override
    public float getInputZ(LivingEntity entity) {
        EntityLiving e = ((CraftLivingEntity) entity).getHandle();
        return e.bq;
    }

    //??用处不明
    @Override
    public float getInputY(LivingEntity entity) {
        EntityLiving e = ((CraftLivingEntity) entity).getHandle();
        return e.bp;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void sendPacket(Player player,Packet<?>... packets) {
        NetworkManager networkManager = ((CraftPlayer) player).getHandle().networkManager;
        if (networkManager != null){
            for (Packet<?> p : packets) {
                if (p != null) networkManager.sendPacket(p);
            }
        }
    }

    @Override
    public void test() throws Exception {
        Class<EntityLiving> el = EntityLiving.class;
        el.getField("bn").setAccessible(true);
        el.getField("bo").setAccessible(true);
        el.getField("bq").setAccessible(true);
        el.getField("bp").setAccessible(true);
    }
}
