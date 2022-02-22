package cn.whiteg.bnes.nms;

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
import java.util.Arrays;

public class PlayerNms_Ref implements PlayerNms {

    private static final Field jump;
    private static final Field inputX;
    private static final Field inputZ;
    private static final Field inputY;
    private static Field craftHandler;
    private static Method sendPacketMethod;

    static {
        Class<?>[] find = new Class[]{boolean.class,float.class,float.class,float.class};
        Field[] result = new Field[find.length];
        Field[] fields = EntityLiving.class.getDeclaredFields();
        int index = 0;
        for (Field f : fields) {
            if (f.getType() == find[index]){
                result[index] = f;
                index++;
                if (index >= find.length) break;
            } else {
                index = 0;
            }
        }

        for (Field field : result) {
            if (field == null){
                throw new IllegalArgumentException("搜索不到方法" + Arrays.toString(fields));
            }
            field.setAccessible(true);
        }
        jump = result[0];
        inputX = result[1];
        inputY = result[2];
        inputZ = result[3];
        try{
            var clazz = PlayerNms_Ref.class.getClassLoader().loadClass(Bukkit.getServer().getClass().getPackage().getName() + ".entity.CraftEntity");
            craftHandler = clazz.getDeclaredField("entity");
            craftHandler.setAccessible(true);
        }catch (Exception e){
            e.printStackTrace();
        }

        for (Method method : NetworkManager.class.getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 2 && parameterTypes[0].equals(Packet.class) && parameterTypes[1].equals(GenericFutureListener.class)){
                sendPacketMethod = method;
                break;
            }
        }
    }

    @Override
    public boolean getJumping(LivingEntity entity) {
        try{
            return (boolean) jump.get(getNmsEntity(entity));
        }catch (Exception e){
            return false;
        }
    }

    //获取玩家控制坐骑的平行x轴
    @Override
    public float getInputX(LivingEntity entity) {
        try{
            return (float) inputX.get(getNmsEntity(entity));
        }catch (Exception e){
            return 0f;
        }
    }

    //获取玩家控制坐骑的前后y轴
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

    @SuppressWarnings("ConstantConditions")
    @Override
    public void sendPacket(Player player,Packet<?>... packets) {
        NetworkManager networkManager = ((EntityPlayer) getNmsEntity(player)).networkManager;
        if (networkManager != null) for (Packet<?> p : packets)
            if (p != null){
                try{
                    sendPacketMethod.invoke(networkManager,p,null);
                }catch (IllegalAccessException | InvocationTargetException e){
                    e.printStackTrace();
                }
//                networkManager.sendPacket(p);
            }
    }

    public net.minecraft.world.entity.Entity getNmsEntity(Entity entity) {
        try{
            return (net.minecraft.world.entity.Entity) craftHandler.get(entity);
        }catch (Throwable e){
            throw new RuntimeException(e);
        }
    }
}
