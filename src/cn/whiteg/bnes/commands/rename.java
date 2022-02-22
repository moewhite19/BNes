package cn.whiteg.bnes.commands;

import cn.whiteg.bnes.BNes;
import cn.whiteg.bnes.render.BukkitRender;
import cn.whiteg.bnes.common.HasCommandInterface;
import cn.whiteg.bnes.utils.CommonUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class rename extends HasCommandInterface {
    private final BNes plugin;

    public rename(BNes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean executo(CommandSender sender,Command cmd,String str,String[] args) {
        if (args.length < 2){
            sender.sendMessage(getDescription());
            return false;
        }
        var name = args[0];
        var newName = args[1];
        if (plugin.getRender(newName) != null){
            sender.sendMessage(newName + "§b已存在");
            return false;
        }
        CommonUtils.validKey(newName);
        BukkitRender render = plugin.removeRender(name);
        if (render == null){
            sender.sendMessage("找不到地图");
            return false;
        }
        render.setName(newName);
        plugin.putRender(render);
        var cs = plugin.getRenderStore();
        cs.set(name,null);
        try{
            render.saveTo(cs.createSection(newName));
        }catch (Exception exception){
            exception.printStackTrace();
        }
        sender.sendMessage("重命名完成");
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender,Command cmd,String str,String[] args) {
        if (args.length <= 1){
            return getMatches(args,new ArrayList<>(plugin.getRenders()));
        }
        return Collections.singletonList(args[0]);
    }

    @Override
    public String getDescription() {
        return "重命名实例: <实例名> <新名字>";
    }
}
