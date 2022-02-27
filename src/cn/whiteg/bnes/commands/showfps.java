package cn.whiteg.bnes.commands;

import cn.whiteg.bnes.BNes;
import cn.whiteg.bnes.common.HasCommandInterface;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class showfps extends HasCommandInterface {
    private final BNes plugin;

    public showfps(BNes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean executo(CommandSender commandSender,Command command,String string,String[] strings) {
        //noinspection AssignmentUsedAsCondition
        commandSender.sendMessage(" §b显示FPS" + ((plugin.setting.showFps = !plugin.setting.showFps) ? "§a启用" : "§a关闭"));
        return true;
    }

    @Override
    public String getDescription() {
        return "开关显示FPS";
    }
}
