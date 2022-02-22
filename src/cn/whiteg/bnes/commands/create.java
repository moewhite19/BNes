package cn.whiteg.bnes.commands;

import cn.whiteg.bnes.BNes;
import cn.whiteg.bnes.common.HasCommandInterface;
import cn.whiteg.bnes.render.BukkitRender;
import cn.whiteg.bnes.render.BukkitRender1x;
import cn.whiteg.bnes.utils.CommonUtils;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class create extends HasCommandInterface {
    private final BNes plugin;

    public create(BNes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean executo(CommandSender sender,Command cmd,String str,String[] args) {
        if (args.length < 1){
            sender.sendMessage(getDescription());
            return false;
        }
        String name = args[0];
        try{
            CommonUtils.validKey(name);
        }catch (Exception exception){
            sender.sendMessage(exception.getMessage());
            return false;
        }

        BukkitRender render;
        render = plugin.getRender(name);
        if (render != null){
            sender.sendMessage(" §b实例已存在");
            return false;
        }

        if (sender instanceof Player player && plugin.setting.createPrice > 0 && plugin.getEconomy() != null){
            var r = plugin.getEconomy().withdrawPlayer(player,plugin.setting.createPrice);
            if (r.type == EconomyResponse.ResponseType.SUCCESS){
                sender.sendMessage(" §b消费§f" + r.amount + "§b创建游戏机");
            } else {
                sender.sendMessage(" §b无法创建地图:§f" + r.errorMessage);
                return false;
            }
        }

        render = args.length >= 2 ? new BukkitRender1x(name,plugin) : new BukkitRender(name,plugin);
        plugin.putRender(render);
        render.open(plugin.NON_CARD.toString());
        render.start();
        sender.sendMessage(" §b创建游戏机" + name);
        if (sender instanceof Player player){
            plugin.getPlayerNms().addInvOrDrop(player.getInventory(),render.getMaps());
        }
        String errorMsg = render.getErrorMsg();
        if (errorMsg != null){
            sender.sendMessage(" §c出现错误:§f" + errorMsg);
        }

        return true;
    }

    @Override
    public List<String> complete(CommandSender sender,Command cmd,String str,String[] args) {
        if (args.length == 1){
            return Collections.emptyList();
        } else if (args.length == 2){
            return Collections.singletonList("min");
        }
        return null;
    }

    @Override
    public String getDescription() {
        return "创建游戏机:<实例名>";
    }
}
