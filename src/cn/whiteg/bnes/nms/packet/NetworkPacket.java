package cn.whiteg.bnes.nms.packet;

import cn.whiteg.bnes.utils.NMSUtils;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.level.EntityPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

public class NetworkPacket extends PlayerPacketSender {
    private final Field playerNetwork;

    public NetworkPacket() throws NoSuchFieldException {
        playerNetwork = NMSUtils.getFieldFormType(EntityPlayer.class,NetworkManager.class);
    }

    @Override
    public NetworkManager getNetworkManage(Player player) {
        try{
            return (NetworkManager) playerNetwork.get(NMSUtils.getNmsEntity(player));
        }catch (IllegalAccessException e){
            return null;
        }
    }
}
