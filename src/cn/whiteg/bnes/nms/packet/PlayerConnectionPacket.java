package cn.whiteg.bnes.nms.packet;

import cn.whiteg.bnes.utils.NMSUtils;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

public class PlayerConnectionPacket extends PlayerPacketSender {
    private final Field playerNetwork;
    private final Field playerConnection;

    public PlayerConnectionPacket() throws NoSuchFieldException {
        playerConnection = NMSUtils.getFieldFormType(EntityPlayer.class,PlayerConnection.class);
        playerNetwork = NMSUtils.getFieldFormType(PlayerConnection.class,NetworkManager.class);
    }

    @Override
    public NetworkManager getNetworkManage(Player player) {
        try{
            final Entity nmsEntity = NMSUtils.getNmsEntity(player);
            PlayerConnection connection = (PlayerConnection) playerConnection.get(nmsEntity);
            return (NetworkManager) playerNetwork.get(connection);
        }catch (IllegalAccessException e){
            return null;
        }
    }
}
