package cn.whiteg.bnes.nms;

import cn.whiteg.bnes.nms.packet.NetworkPacket;
import cn.whiteg.bnes.nms.packet.PlayerConnectionPacket;
import cn.whiteg.bnes.nms.packet.PlayerPacketSender;
import cn.whiteg.bnes.utils.NMSUtils;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.EntityLiving;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class PlayerNms_Ref implements PlayerNms {

    private static final Field jump;
    private static final Field inputX;
    private static final Field inputZ;
    private static final Field inputY;
    private static PlayerPacketSender packetSender;

    static {
        //根据结构获取骑乘输入控制
        try{
            Field[] result = NMSUtils.getFieldFormStructure(EntityLiving.class,boolean.class,float.class,float.class,float.class);
            jump = result[0];
            inputX = result[1];
            inputY = result[2];
            inputZ = result[3];


            //如果用NM
            try{
                packetSender = new NetworkPacket(); //1.20.2一下用这个，效率更高
            }catch (NoSuchFieldException e){
                packetSender = new PlayerConnectionPacket();//1.20.2或以上用这个
            }
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

    //获取玩家控制坐骑的y轴(前后)
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

    public static PlayerPacketSender getPacketSender() {
        return packetSender;
    }

    @Override
    public void sendPacket(Player player,Packet<?>... packets) {
        packetSender.sendPacket(player,packets);
//        final PlayerConnection connection = getConnection(player);
//        connection.a(packets[0]);
//        if (connection != null) for (Packet<?> p : packets)
//            if (p != null){
//                try{
//                    sendPacketMethod.invoke(connection,p);
//                }catch (IllegalAccessException | InvocationTargetException e){
//                    e.printStackTrace();
//                }
//            }
    }

    public NetworkManager getPlayerNetwork(Player player) {
        return packetSender.getNetworkManage(player);
    }
}
