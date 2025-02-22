package cn.whiteg.bnes.nms;

import cn.whiteg.bnes.BNes;
import cn.whiteg.bnes.utils.NMSUtils;
import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Input;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

public class PlayerNms_Paper implements PlayerNms {
    static {
        try{
            ServerPlayer.class.getMethod("getLastClientInput");
        }catch (NoSuchMethodException e){
            throw new RuntimeException(e);
        }
    }

    //经过测试，Spigot无法使用这一映射
    @Override
    public boolean getJumping(LivingEntity entity) {
        if (NMSUtils.getNmsEntity(entity) instanceof ServerPlayer player){
            return player.getLastClientInput().jump();
        }
        return false;
    }

    //获取玩家控制坐骑的x轴(左右平行)
    @Override
    public float getInputX(LivingEntity entity) {

        if (NMSUtils.getNmsEntity(entity) instanceof ServerPlayer player){
            return (float) player.getLastClientMoveIntent().x;
        }
        return 0f;
    }

    //获取玩家控制坐骑的z轴(前后)
    @Override
    public float getInputZ(LivingEntity entity) {
        if (NMSUtils.getNmsEntity(entity) instanceof ServerPlayer player){
            return (float) player.getLastClientMoveIntent().z;
        }
        return 0f;
    }

    //??用处不明
    @Override
    public float getInputY(LivingEntity entity) {
        if (NMSUtils.getNmsEntity(entity) instanceof ServerPlayer player){
            return (float) player.getLastClientMoveIntent().y;
        }
        return 0f;
    }

    @Override
    public void sendPacket(Player player,Packet<?>... packets) {
        final Connection connection = getPlayerNetwork(player);
//        connection.a(packets[0]);
        if (connection != null){
            if (connection.isConnected()){
                final Channel channel = connection.channel;
                for (Packet<?> p : packets) {
                    if (p != null){
                        channel.write(p);
                    }
                }
                channel.flush();
            }

        }
    }

    public Connection getPlayerNetwork(Player player) {
        return ((CraftPlayer) player).getHandle().connection.connection;
    }
}
