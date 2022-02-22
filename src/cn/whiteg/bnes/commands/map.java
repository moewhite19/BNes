package cn.whiteg.bnes.commands;

import cn.whiteg.bnes.BNes;
import cn.whiteg.bnes.common.HasCommandInterface;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class map extends HasCommandInterface {
    private final BNes plugin;

    public map(BNes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean executo(CommandSender sender,Command cmd,String str,String[] args) {
        if (args.length < 1){
            sender.sendMessage(getDescription());
            return false;
        }
        var name = args[0];

        Player player = null;
        if (args.length >= 2) player = Bukkit.getPlayer(args[1]);
        else if (sender instanceof Player p) player = p;
        if (player == null){
            sender.sendMessage(" §b找不到玩家");
            return false;
        }
        var render = plugin.getRender(name);
        if (render == null){
            sender.sendMessage(" §b找不到地图");
            return false;
        }
        player.getInventory().addItem(render.getMaps());
        sender.sendMessage(" §b已将地图给与§f" + player.getDisplayName());
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender,Command cmd,String str,String[] args) {
        if (args.length == 1){
            return getMatches(args,new ArrayList<>(plugin.getRenders()));
        } else if (args.length == 2) return getPlayersList(args);
        return null;
    }

    @Override
    public String getDescription() {
        return "获取实例地图: <实例> [玩家]";
    }
}
