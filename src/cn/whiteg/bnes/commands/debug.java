package cn.whiteg.bnes.commands;

import cn.whiteg.bnes.BNes;
import cn.whiteg.bnes.common.HasCommandInterface;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class debug extends HasCommandInterface {
    private final BNes plugin;

    public debug(BNes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean executo(CommandSender sender,Command cmd,String str,String[] args) {
        if (sender instanceof Player player){
            for (var render : plugin.getRenderMap().values()) {
                for (Integer id : render.getIds()) {
                    plugin.getPlayerNms().sendMap(player,id,new byte[16384]);
                }
            }
            sender.sendMessage("已清空地图画面");
            return true;
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "测试指令(仅限DEBUG)";
    }

    //仅在测试时可用
    @Override
    public boolean canUseCommand(CommandSender sender) {
        return plugin.setting.DEBUG;
    }
}
