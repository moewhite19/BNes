package cn.whiteg.bnes.listener;

import cn.whiteg.bnes.BNes;
import cn.whiteg.bnes.render.BukkitRender;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

public class PlayerListener implements Listener {
    private final BNes plugin;

    public PlayerListener(BNes plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEntityEvent event) {
        var entity = event.getRightClicked();
        if (entity instanceof ItemFrame itemFrame){
            if (useItem(event.getPlayer(),itemFrame.getItem(),false,event)){
                event.setCancelled(true);
            }
        }
    }


    public boolean useItem(Player player,ItemStack itemStack,boolean left,Cancellable event) {
        if (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta() instanceof MapMeta mapMeta){
            MapView view = mapMeta.getMapView();
            if (view == null) return false;
            int id = view.getId();
            BukkitRender render = plugin.getRender(id);
            if (render == null) return false;

            //换卡
            if (event != null && !event.isCancelled()){
                PlayerInventory inventory = player.getInventory();
                ItemStack itemInHand = inventory.getItemInMainHand();
                if (plugin.getCardFactory().isCard(itemInHand) && !player.hasCooldown(itemInHand.getType())){
                    player.setCooldown(itemInHand.getType(),20);
                    if (itemInHand.getAmount() > 1){
                        //paper方法
//                        itemInHand.subtract();
//                        itemInHand = itemInHand.asOne();

                        //spigot只能用原生BukkitAPI实现
                        itemInHand.setAmount(itemInHand.getAmount() - 1);
                        itemInHand = itemInHand.clone();
                        itemInHand.setAmount(1);
                    } else {
                        inventory.setItemInMainHand(null);
                    }
                    var callBack = render.setCard(itemInHand);
                    if (callBack != null){
                        plugin.getPlayerNms().addInvOrDrop(inventory,callBack);
                    }
                    String errorMsg = render.getErrorMsg();
                    if (errorMsg != null){
                        player.sendMessage(" §c加载Rom时出现错误:" + errorMsg);
                    } else {
                        render.getPlayerInput().broadcast(" §b已更换卡带");
                    }
                    return true;
                }
            }


            //左右键交互
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof ArmorStand || vehicle instanceof Minecart){
                return render.getPlayerInput().onClick(player,left);
            }
        }
        return false;
    }

}
