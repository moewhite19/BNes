package cn.whiteg.bnes.commands;

import cn.whiteg.bnes.BNes;
import cn.whiteg.bnes.common.HasCommandInterface;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class reload extends HasCommandInterface {
    private final BNes plugin;

    public reload(BNes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean executo(CommandSender sender,Command cmd,String str,String[] args) {
        plugin.setting.reload();
        sender.sendMessage("重载完成");
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender,Command cmd,String str,String[] args) {
        if (args.length == 1){
            return getMatches(args,new ArrayList<>(plugin.getRenders()));
        }
        return null;
    }
}
