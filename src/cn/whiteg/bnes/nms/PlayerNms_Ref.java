package cn.whiteg.bnes.nms;

import cn.whiteg.bnes.utils.NMSUtils;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.EntityLiving;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class PlayerNms_Ref implements PlayerNms {

    private static final Field jump;
    private static final Field inputX;
    private static final Field inputZ;
    private static final Field inputY;
    private static final Field craftHandler;
    private static Method sendPacketMethod;
    private static final Field playerNetwork;

    static {
        //根据结构获取骑乘输入控制
        try{
            Field[] result = NMSUtils.getFieldFormStructure(EntityLiving.class,boolean.class,float.class,float.class,float.class);
            jump = result[0];
            inputX = result[1];
            inputY = result[2];
            inputZ = result[3];


            var clazz = PlayerNms_Ref.class.getClassLoader().loadClass(Bukkit.getServer().getClass().getPackage().getName() + ".entity.CraftEntity");
            craftHandler = clazz.getDeclaredField("entity");
            craftHandler.setAccessible(true);

            playerNetwork = NMSUtils.getFieldFormType(EntityPlayer.class,NetworkManager.class);
        }catch (Exception e){
            throw new RuntimeException(e);
        }

        for (Method method : NetworkManager.class.getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 2 && parameterTypes[0].equals(Packet.class) && parameterTypes[1].equals(GenericFutureListener.class)){
                sendPacketMethod = method;
                break;
            }
        }
        Objects.requireNonNull(sendPacketMethod);

    }

    @Override
    public boolean getJumping(LivingEntity entity) {
        try{
            return (boolean) jump.get(getNmsEntity(entity));
        }catch (Exception e){
            return false;
        }
    }

    //获取玩家控制坐骑的x轴(左右平行)
    @Override
    public float getInputX(LivingEntity entity) {
        try{
            return (float) inputX.get(getNmsEntity(entity));
        }catch (Exception e){
            return 0f;
        }
    }

    //获取玩家控制坐骑的y轴(前后)
    @Override
    public float getInputZ(LivingEntity entity) {
        try{
            return (float) inputZ.get(getNmsEntity(entity));
        }catch (Exception e){
            return 0f;
        }
    }

    //??用处不明
    @Override
    public float getInputY(LivingEntity entity) {
        try{
            return (float) inputY.get(getNmsEntity(entity));
        }catch (Exception e){
            return 0f;
        }
    }

    @Override
    public void sendPacket(Player player,Packet<?>... packets) {
        NetworkManager networkManager = getPlayerNetwork(player);
        if (networkManager != null) for (Packet<?> p : packets)
            if (p != null){
                try{
                    sendPacketMethod.invoke(networkManager,p,null);
                }catch (IllegalAccessException | InvocationTargetException e){
                    e.printStackTrace();
                }
            }
    }

    public net.minecraft.world.entity.Entity getNmsEntity(Entity entity) {
        try{
            return (net.minecraft.world.entity.Entity) craftHandler.get(entity);
        }catch (Throwable e){
            throw new RuntimeException(e);
        }
    }

    public NetworkManager getPlayerNetwork(Player player) {
        try{
            return (NetworkManager) playerNetwork.get(getNmsEntity(player));
        }catch (IllegalAccessException e){
            throw new RuntimeException(e);
        }
    }
}
