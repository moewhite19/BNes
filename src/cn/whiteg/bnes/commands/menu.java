package cn.whiteg.bnes.commands;

import cn.whiteg.bnes.BNes;
import cn.whiteg.bnes.common.HasCommandInterface;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class menu extends HasCommandInterface {
    private final BNes plugin;

    public menu(BNes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean executo(CommandSender sender,Command cmd,String str,String[] args) {
        if (sender instanceof Player player){
            int page;
            if (args.length >= 1){
                try{
                    page = Integer.parseInt(args[0]);
                }catch (NumberFormatException e){
                    page = 0;
                }
            } else {
                page = 1;
            }
            if (page <= 0){
                player.sendMessage(" §b无效页面");
                return false;
            }
            page--;
            List<ItemStack> list = plugin.getCardFactory().getCardItems(page,54);
            if (list.isEmpty()){
                sender.sendMessage(" §b空页面");
                return false;
            }
            Inventory inv = Bukkit.createInventory(null,54,"模型列表");
            inv.setContents(list.toArray(new ItemStack[0]));
            player.openInventory(inv);
            return true;
        }
        return false;
    }

    @Override
    public List<String> complete(CommandSender sender,Command cmd,String str,String[] args) {
        int cardNumber = plugin.getCardFactory().getCardNumber() / 54;
        if (cardNumber <= 0) return Collections.emptyList();
        cardNumber++;
        ArrayList<String> list = new ArrayList<>(cardNumber);
        for (int i = 1; i <= cardNumber; i++) {
            list.add(String.valueOf(i));
        }
        return getMatches(args,list);
    }

    @Override
    public String getDescription() {
        return "获取卡带列表: <页数>";
    }
}
