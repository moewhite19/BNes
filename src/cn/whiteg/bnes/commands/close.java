package cn.whiteg.bnes.commands;

import cn.whiteg.bnes.BNes;
import cn.whiteg.bnes.render.BukkitRender;
import cn.whiteg.bnes.common.HasCommandInterface;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class close extends HasCommandInterface {
    private final BNes plugin;

    public close(BNes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean executo(CommandSender sender,Command cmd,String str,String[] args) {
        if (args.length < 1){
            sender.sendMessage(getDescription());
            return false;
        }
        String key = args[0];
        BukkitRender render = plugin.removeRender(key);
        if (render != null){
            render.close();
            sender.sendMessage("已关闭游戏机: " + key);
            plugin.getRenderStore().set(key,null);
        } else {
            sender.sendMessage("找不到游戏机: " + key);
        }
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender,Command cmd,String str,String[] args) {
        if (args.length == 1){
            return getMatches(args,new ArrayList<>(plugin.getRenders()));
        }
        return null;
    }

    @Override
    public String getDescription() {
        return "关闭指定游戏机: <实例名字>";
    }
}
