package cn.whiteg.bnes.commands;

import cn.whiteg.bnes.BNes;
import cn.whiteg.bnes.common.HasCommandInterface;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class card extends HasCommandInterface {
    private final BNes plugin;

    public card(BNes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean executo(CommandSender sender,Command cmd,String str,String[] args) {
        if (args.length < 1){
            sender.sendMessage(getDescription());
            return false;
        }
        String arg = args[0];
        if (!new File(plugin.romDir,arg).exists()){
            sender.sendMessage(" §c文件不存在" + arg);
            return false;
        }
        if (sender instanceof Player player){
            player.getInventory().addItem(plugin.getCardFactory().makeCard(arg));
            return true;
        }
        return false;
    }

    @Override
    public List<String> complete(CommandSender sender,Command cmd,String str,String[] args) {
        var files = plugin.romDir.list();
        if (files == null) return null;
        return getMatches(args,Arrays.asList(files));
    }

    @Override
    public String getDescription() {
        return "获取卡带: <名字>";
    }
}
