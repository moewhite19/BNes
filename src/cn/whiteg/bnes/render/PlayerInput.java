package cn.whiteg.bnes.render;

import cn.whiteg.bnes.BNes;
import cn.whiteg.bnes.Setting;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerInput implements Listener {
    final BNes plugin;
    private final BukkitRender render;
    private final PlayerController[] controllers;
    private final Player[] players = new Player[2];
    private final AtomicBoolean[] LMB = new AtomicBoolean[]{new AtomicBoolean(),new AtomicBoolean()};
    private final AtomicBoolean[] RMB = new AtomicBoolean[]{new AtomicBoolean(),new AtomicBoolean()};
    private final AtomicBoolean[] SWAP = new AtomicBoolean[]{new AtomicBoolean(),new AtomicBoolean()};
    private final AtomicBoolean[] DROP = new AtomicBoolean[]{new AtomicBoolean(),new AtomicBoolean()};
    long lastPlay;
    long nextUpdate;

    public PlayerInput(BukkitRender render,PlayerController[] controllers) {
        this.render = render;
        this.controllers = controllers;
        lastPlay = System.currentTimeMillis();
        nextUpdate = lastPlay;
        plugin = render.plugin;
    }

    public void update() {
        long now = System.currentTimeMillis();
        if (now < nextUpdate) return;
        nextUpdate += plugin.setting.playerInputFps;
        Setting setting = plugin.setting;
        for (int p = 0; p < 2; p += 1) {
            var player = players[p];
            if (player != null){
                if (player.isDead() || player.getVehicle() == null){
                    broadcast(player.getName() + " §b§l退出游戏§f");
                    players[p] = null;
                    continue;
                }

                lastPlay = now;
                PlayerController controller = controllers[p];
                controller.resetButtons();

                if (plugin.getPlayerNms().getJumping(player)) controller.pressButton(setting.onJump);


                if (SWAP[p].getAndSet(false)) controller.pressButton(setting.onSwap);
                if (DROP[p].getAndSet(false)) controller.pressButton(setting.onDrop);


                if (LMB[p].getAndSet(false)) controller.pressButton(setting.onLMB);
                if (RMB[p].getAndSet(false)) controller.pressButton(setting.onRMB);

                //重置游戏
                if (controller.statusButton(PlayerController.Button.START) && controller.statusButton(PlayerController.Button.A)){
                    render.reset();
                    broadcast(" §b§l重启游戏§f");
                    return;
                }
                //重置画面,用于测试矩形发送程序
                if (render.plugin.setting.DEBUG && controller.statusButton(PlayerController.Button.SELECT) && controller.statusButton(PlayerController.Button.A)){
                    for (Integer id : render.getIds()) {
                        plugin.getPlayerNms().sendMap(player,id,new byte[128 * 128]);
                    }
                    return;
                }


                //方向
                float ws = plugin.getPlayerNms().getInputZ(player);
                if (ws < -0.4) controller.pressButton(PlayerController.Button.DOWN);
                float ad = plugin.getPlayerNms().getInputX(player);
                if (ad > 0.4) controller.pressButton(PlayerController.Button.LEFT);
                if (ad < -0.4) controller.pressButton(PlayerController.Button.RIGHT);
                if (ws > 0.4) controller.pressButton(PlayerController.Button.UP);
                if (plugin.setting.DEBUG) player.sendActionBar("ws=" + ws + " ad=" + ad);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        boolean left;
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR){
            left = false;
        } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK){
            left = true;
        } else return;
        for (int i = 0; i < players.length; i++) {
            if (player.equals(players[i])){
                (left ? LMB[i] : RMB[i]).set(true);
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ItemFrame && event.getDamager() instanceof Player player){
            for (int i = 0; i < players.length; i++) {
                if (player.equals(players[i])){
                    LMB[i].set(true);
                    event.setCancelled(true);
                    return;
                }
            }
        }

    }


    //左右键
    public boolean onClick(Player player,boolean isLeft) {
        for (int i = 0; i < players.length; i++) {
            Player playing = players[i];
            if (playing == player){
                (isLeft ? LMB[i] : RMB[i]).set(true);
                return true;
            }
            if (playing == null){
                playerJoin(player,i);
                return true;
            }
        }
        return false;
    }

    private void playerJoin(Player player,int p) {
        players[p] = player;
        broadcast(player.getName() + " §b§l加入游戏§f" + " §a" + (p + 1) + "P");
        lastPlay = System.currentTimeMillis();

        if (!plugin.setting.activelyRenderEveryone){
            //当玩家加入游戏时发送一次完整地图
            List<Integer> ids = render.getIds();
            for (int i = 0; i < ids.size(); i++) {
                plugin.getPlayerNms().sendMap(player,ids.get(i),render.colors[i]);
            }
        }

        render.start(); //启动游戏线程

        //设定游戏机声音输出位置
        if (render.audioOutInterface != null && p == 0){
            render.audioOutInterface.updateLoc(player.getLocation().add(player.getFacing().getDirection())); //获取玩家位置，并向前位移一米
//            System.out.println("方向: " + player.getFacing());
        }

    }

    public boolean isPlaying(Player player) {
        for (Player p : players) {
            if (p == player){
                return true;
            }
        }
        return false;
    }

    public boolean isPlaying() {
        for (Player player : players) {
            if (player != null) return true;
        }
        return false;
    }

    public Player[] getPlayers() {
        return players;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (plugin.setting.onSwap == null) return;
        for (int i = 0; i < players.length; i++) {
            if (event.getPlayer() == players[i]){
                SWAP[i].set(true);
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onDroup(PlayerDropItemEvent event) {
        if (plugin.setting.onDrop == null) return;
        for (int i = 0; i < players.length; i++) {
            if (event.getPlayer() == players[i]){
                DROP[i].set(true);
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onBreakBlock(BlockBreakEvent event) {
        for (int i = 0; i < players.length; i++) {
            if (event.getPlayer() == players[i]){
                LMB[i].set(true);
                event.setCancelled(true);
                return;
            }
        }
    }

    public void broadcast(String str) {
        for (Player player : players) {
            if (player != null) player.sendTitle(str,render.getDisplayName(),5,20,5);
        }
    }

    public void message(String str) {
        for (Player player : players) {
            if (player != null) player.sendMessage("§b" + render.getDisplayName() + "§r: " + str);
        }
    }

    public PlayerController[] getControllers() {
        return controllers;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this,plugin);
        lastPlay = System.currentTimeMillis();
    }

    public void shutdown() {
        if (isPlaying()) broadcast("游戏意外关闭"); //在有玩家的情况下提示玩家
        Arrays.fill(players,null);
        HandlerList.unregisterAll(this);
    }
}
