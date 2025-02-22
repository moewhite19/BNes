package cn.whiteg.bnes.nms;

import cn.whiteg.bnes.BNes;
import cn.whiteg.bnes.utils.NMSUtils;
import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

public class PlayerNms_Ref implements PlayerNms {

    private static final Field jump;
    private static final Field inputX;
    private static final Field inputZ;
    private static final Field inputY;

    static {
        //根据结构获取骑乘输入控制
        try{
            Field[] result = NMSUtils.getFieldFormStructure(net.minecraft.world.entity.LivingEntity.class,boolean.class,float.class,float.class,float.class);
            jump = result[0];
            inputX = result[1];
            inputY = result[2];
            inputZ = result[3];

            if (BNes.plugin.setting.DEBUG){
                System.out.println("jump映射: " + jump);
                System.out.println("inputX映射: " + inputX);
                System.out.println("inputY映射: " + inputY);
                System.out.println("inputZ映射: " + inputZ);
            }


//            //如果用NM
//            try{
//                packetSender = new NetworkPacket(); //1.20.2一下用这个，效率更高
//            }catch (NoSuchFieldException e){
//                packetSender = new PlayerConnectionPacket();//1.20.2或以上用这个
//            }
//            packetSender = new PaperSender();
//            playerConnection = NMSUtils.getFieldFormType(EntityPlayer.class,PlayerConnection.class);
        }catch (Exception e){
            throw new RuntimeException(e);
        }

    }

    //经过测试，Spigot无法使用这一映射
    @Override
    public boolean getJumping(LivingEntity entity) {
        try{
            return (boolean) jump.get(NMSUtils.getNmsEntity(entity));
        }catch (Exception e){
            return false;
        }
    }

    //获取玩家控制坐骑的x轴(左右平行)
    @Override
    public float getInputX(LivingEntity entity) {
        try{
            return (float) inputX.get(NMSUtils.getNmsEntity(entity));
        }catch (Exception e){
            return 0f;
        }
    }

    //获取玩家控制坐骑的z轴(前后)
    @Override
    public float getInputZ(LivingEntity entity) {
        try{
            return (float) inputZ.get(NMSUtils.getNmsEntity(entity));
        }catch (Exception e){
            return 0f;
        }
    }

    //??用处不明
    @Override
    public float getInputY(LivingEntity entity) {
        try{
            return (float) inputY.get(NMSUtils.getNmsEntity(entity));
        }catch (Exception e){
            return 0f;
        }
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
