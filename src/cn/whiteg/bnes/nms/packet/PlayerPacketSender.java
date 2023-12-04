package cn.whiteg.bnes.nms.packet;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public abstract class PlayerPacketSender {
    private static Method sendPacketMethod;

    static {
        for (Method method : NetworkManager.class.getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 2 && parameterTypes[0].equals(Packet.class) && parameterTypes[1].equals(PacketSendListener.class)){
                sendPacketMethod = method;
                sendPacketMethod.setAccessible(true);
                break;
            }
        }
        Objects.requireNonNull(sendPacketMethod);
    }

    public abstract NetworkManager getNetworkManage(Player player);

    public void sendPacket(Player player,Packet<?>... packets) {
        final NetworkManager manage = getNetworkManage(player);
        if (manage != null){
            try{
                for (Packet<?> p : packets) {
                    sendPacketMethod.invoke(manage,p,null);
                }
            }catch (IllegalAccessException | InvocationTargetException e){
                e.printStackTrace();
            }
        }
    }
}
