package cn.whiteg.bnes.nms.packet;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class PaperSender extends PlayerPacketSender {
    @Override
    public Connection getNetworkManage(Player player) {
        return ((CraftPlayer) player).getHandle().connection.connection;
    }
}
