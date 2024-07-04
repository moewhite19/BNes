package cn.whiteg.bnes.render;

import cn.whiteg.bnes.BNes;
import cn.whiteg.bnes.buffmap.BuffMapConstructor;
import cn.whiteg.bnes.buffmap.CAndSConstructor;
import cn.whiteg.bnes.buffmap.NoneConstructor;
import cn.whiteg.bnes.nms.PlayerNms;
import cn.whiteg.bnes.utils.FpsMonitor;
import cn.whiteg.bnes.utils.MapUtils;
import cn.whiteg.bnes.voicechat.VoiceChatAudioOut;
import com.grapeshot.halfnes.APU;
import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.ui.GUIInterface;
import com.grapeshot.halfnes.video.NesColors;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.*;

public class BukkitRender implements GUIInterface {
    final static long FULL_FPS_TIME = (long) ((1000d / 60d) * 1000000d); //内部框架fps时间
    final BNes plugin;
    final Map<Player, Long> observers = new HashMap<>(0);
    private final StringBuffer errorMsg = new StringBuffer(16);
    WeakReference<Set<Player>> observersSetTmp = new WeakReference<>(null);
    ItemStack card;
    PlayerInput playerInput;
    ImageMapRender[] imageMapRenders;
    MapView[] mapViews;
    byte[][] colors;
    long lastRender;
    String file;
    String name;
    FpsMonitor renderFps = new FpsMonitor();
    FpsMonitor updateFps = new FpsMonitor();
    BuffMapConstructor[] buffMap;
    int size = 2;
    private NES nes;
    private long nextSendTime;
    private List<Integer> ids = new ArrayList<>(4); //MapIds
    private Thread looper;
    private long nextLoop = System.nanoTime();
    VoiceChatAudioOut audioOutInterface;


    public BukkitRender(String name,BNes plugin) {
        this.name = name;
        this.plugin = plugin;
    }

    //初始化
    public synchronized void initRender() {
        if (mapViews != null) return;
        int blockSize = size * size;
        buffMap = new BuffMapConstructor[blockSize]; //图片主动发送缓存
        imageMapRenders = new ImageMapRender[blockSize];   //创建地图渲染块
        mapViews = new MapView[blockSize];
        colors = new byte[blockSize][];
        for (int i = 0; i < mapViews.length; i++) {
            MapView view;
            if (i < ids.size()){
                Integer id = ids.get(i);
                if (plugin.getRender(id) != null){ //如果id已存在一个实例
                    view = Bukkit.createMap(Bukkit.getWorlds().get(0));
                    int newId = view.getId();
                    ids.set(i,newId);
                    plugin.getLogger().warning(name + "存在MapId冲突 " + id + " 已创建新Id " + newId);
                } else {
                    view = Bukkit.getMap(id);
                    if (view == null){
                        view = Bukkit.createMap(Bukkit.getWorlds().get(0));
                        ids.set(i,view.getId());
                    }
                }
            } else {
                view = Bukkit.createMap(Bukkit.getWorlds().get(0));
                ids.add(view.getId());
            }
            mapViews[i] = view;
            view.setScale(MapView.Scale.FAR);

            view.setLocked(true); //锁住地图
            //获取颜色数组
            colors[i] = MapUtils.getBytes(view);

            //获取更新缓存
            buffMap[i] = plugin.setting.sendFullFrame ? new NoneConstructor() : new CAndSConstructor(4);

            buffMap[i].makeUpdate(colors[i]); //更新一下缓存

            //移除所有渲染器
            for (MapRenderer renderer : view.getRenderers()) {
                view.removeRenderer(renderer);
            }
            //添加新渲染器
            ImageMapRender renderBlock = new ImageMapRender(this,colors[i],ids.get(i));
            imageMapRenders[i] = renderBlock;
            view.addRenderer(renderBlock);
        }
        PlayerController[] controllers = new PlayerController[]{new PlayerController(),new PlayerController()};
        playerInput = new PlayerInput(this,controllers);
        if (file == null) file = plugin.NON_CARD.toString();
    }

    public synchronized void initNes(boolean load) {
        if (nes == null){
            initRender();
            nes = new NES(this);
            nes.setControllers(playerInput.getControllers()[0],playerInput.getControllers()[1]);
            if (load){
                nes.loadROM(file);
                if (!nes.runEmulation) nes.loadROM(plugin.NON_CARD.toString());
                initAudio();
                var error = getErrorMsg();
                if (error != null){
                    messageBox("加载 " + name + "出现错误:" + error);
                }
            }
        }
    }

    public synchronized void start() {
        if (looper != null && !nes.isShutdown()) return;
        initRender();
        initNes(true);
        //创建线程
        looper = new Thread(() -> {
            while (!nes.isShutdown()) {
                if (!plugin.isEnabled()){
                    close();
                    return;
                }
                try{
                    long now = System.nanoTime();
                    synchronized (this) {
                        if (now < nextLoop){
                            wait(1);
                            continue;
                        }
                        nextLoop += FULL_FPS_TIME;
                        if ((System.currentTimeMillis() - playerInput.lastPlay) >= plugin.setting.idleSleep)
                            break; //长时间无操作跳出
                        if (nes.runEmulation) nes.runframe(); //执行模拟器框架
                        playerInput.update(); //更新控制
                        render(); //更新画面

                        //不太准确的wait法
//                        long total = System.nanoTime() - now;
//                        long waitTime = (fpsTime - total) / 10000000;
//                        if (waitTime <= 0) continue;
//                        this.wait(waitTime);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    break;
                }
            }
            //跳出循环相当于暂停,线程也将被回收
            looper = null;
            playerInput.shutdown();
            nes.pause();
        });
        //初始化计时器
        nextLoop = System.nanoTime();
        nextSendTime = System.currentTimeMillis();
        //进入循环，启用游戏机
        looper.setName("BNes-" + name);
        looper.start();
        nes.resume();
        playerInput.start();
    }

    public synchronized void close() {
        if (nes != null) nes.quit();
        if (mapViews != null) for (int i = 0; i < mapViews.length; i++) {
            mapViews[i].removeRenderer(imageMapRenders[i]);
        }
    }

    @Override
    public NES getNes() {
        initNes(true);
        return nes;
    }

    @Override
    public void setNES(NES nes) {
        if (this.nes != null && nes != this.nes) throw new IllegalArgumentException(nes + " is What?");
    }

    @Override
    public void setFrame(int[] frame,int[] bgcolor,boolean dotcrawl) {
        for (int fi = 0; fi < colors.length; fi++) {
            int fx = (fi % size) * 128, fy = (fi / size) * 128; //地图切割坐标偏移
            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 128; y++) {
                    int index = (y + fy) * 256 + x + fx;
                    if (index >= frame.length) continue;
                    int rgb = frame[index];
                    rgb = NesColors.col[(rgb & 448) >> 6][rgb & 63];
                    colors[fi][y * 128 + x] = MapUtils.matchColor(rgb);
                }
            }
        }
        renderFps.draw();
    }

    @Override
    public void messageBox(String s) {
        if (!errorMsg.isEmpty()) errorMsg.append("\n");
        errorMsg.append(s);
    }

    @Override
    public void run() {
    }

    @Override
    public void render() {
        lastRender = System.currentTimeMillis();
        //主动更新画面到玩家
        if (lastRender >= nextSendTime){
            //获取发送字节数，自动调整fps
            int size = renderToPlayer();
            if (plugin.setting.updateMaxSizeLimit == 0 || size == 0){
                nextSendTime += plugin.setting.updateTime;
            } else {
                size = size / plugin.setting.updateMaxSizeLimit;
                nextSendTime += Math.max(plugin.setting.updateTime,size);
            }
        }
    }

    @Override
    public void loadROMs(String s) {
    }

    public synchronized void open(String file) {
        initNes(false);
        this.file = file;
        nes.loadROM(file);
        if (!nes.runEmulation){ //如果加载失败
            nes.loadROM(plugin.NON_CARD.toString());
        }

        initRender();

        //加载后重新写入语音聊天输出
        initAudio();
    }

    //初始化语音输出
    public void initAudio() {
        //创建语音输出
        if (plugin.getVoiceChatPlugin() != null){
            try{
                if (audioOutInterface == null){
                    audioOutInterface = new VoiceChatAudioOut(this,plugin.getVoiceChatPlugin());
                }
                final APU apu = nes.getApu();
                if (apu.getAi() != audioOutInterface){
                    apu.getAi().destroy();
                    apu.setAi(audioOutInterface);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void saveTo(ConfigurationSection sc) {
        if (!ids.isEmpty()) sc.set("ids",ids);
        sc.set("file",file);
        sc.set("card",card);
        sc.set("last-render",lastRender);
    }

    public void form(ConfigurationSection sc) {
        ids = sc.getIntegerList("ids");
        file = sc.getString("file");
        card = sc.getItemStack("card");
        lastRender = sc.getLong("last-render",System.currentTimeMillis());
    }

    public void readCard() {
        File file = plugin.getCardFactory().readCard(card);
        open((file == null ? plugin.NON_CARD : file).toString());
    }

    public ItemStack setCard(ItemStack itemStack) {
        ItemStack card1 = this.card;
        this.card = itemStack;
        readCard();
        return card1;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void reset() {
        getNes().reset();
    }

    //画面同步更新渲染
    public int renderToPlayer() {
        updateFps.draw();
        final Collection<Player> players = getObservers();
        if (players.isEmpty()){
            return 0;
        }
        int length = colors.length;
        Packet<?>[][] frames = new Packet[length][]; //画面集合
        int size = 0;
        for (int i = 0; i < length; i++) {
            List<MapItemSavedData.MapPatch> mapData = buffMap[i].makeUpdate(colors[i]);
            if (mapData != null && !mapData.isEmpty()){
                Packet<?>[] packets = new Packet[mapData.size()];
                for (int i1 = 0; i1 < packets.length; i1++) {
                    MapItemSavedData.MapPatch mapPatch = mapData.get(i1);
                    packets[i1] = PlayerNms.createMapPacket(ids.get(i),mapPatch);
//                    packets[i1] = new ClientboundMapItemDataPacket(ids.get(i),(byte) 1,false,null,mapPatch);
                    size += mapPatch.mapColors().length + 16;
                }
                frames[i] = packets;
            }
        }

        //发送错误信息 (感觉没太必要
        String errorMsg = getErrorMsg();
        if (errorMsg != null){
            playerInput.message(errorMsg);
        }

        final PlayerNms playerNms = plugin.getPlayerNms();

        for (Player player : players) {
            for (Packet<?>[] packets : frames) {
                if (packets == null) continue;
                playerNms.sendPacket(player,packets);
            }
            //显示Fps
            if (plugin.setting.showFps && System.nanoTime() % 3 == 1 && (!plugin.setting.activelyRenderEveryone || playerInput.isPlaying(player))){
                //使用spigot的ActionBar
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent("§b" + renderFps.getFpsText() + " §a" + updateFps.getFpsText() + (plugin.setting.DEBUG ? ("§7 " + playerNms.getInputY(player)) : "")));
            }
        }
        return size;
    }

    public String getErrorMsg() {
        if (errorMsg.isEmpty()) return null;
        var msg = errorMsg.toString();
        errorMsg.setLength(0);
        return msg;
    }

    public List<Integer> getIds() {
        initRender();
        return ids;
    }

    public MapView[] getMapViews() {
        initRender();
        return mapViews;
    }

    public ItemStack[] getMaps() {
        initRender();
        ItemStack[] itemStacks = new ItemStack[mapViews.length];
        for (int i = 0; i < mapViews.length; i++) {
            MapView mapView = mapViews[i];
            var item = new ItemStack(Material.FILLED_MAP);
            var meta = item.getItemMeta();
            if (meta instanceof MapMeta maps){
                maps.setMapView(mapView);
                item.setItemMeta(meta);
            }
            itemStacks[i] = item;
        }
        return itemStacks;
    }

    public boolean isActive() {
        return looper != null;
    }

    public long lastRender() {
        return lastRender;
    }

    public PlayerInput getPlayerInput() {
        return playerInput;
    }

    public String getDisplayName() {
        if (card != null && card.hasItemMeta()){
            ItemMeta meta = card.getItemMeta();
            if (meta != null && meta.hasDisplayName()){
                return meta.getDisplayName() + "§7(" + name + ")";
            }
        }
        return (file != null ? plugin.getCardFactory().removeFormat(new File(file).getName()) : "test_card") + "§7(" + name + ")";
    }

    public Collection<Player> getObservers() {
        if (plugin.setting.activelyRenderEveryone){
            synchronized (observers) {
                if (observers.isEmpty()){
                    return Collections.emptyList();
                }
                final long now = System.currentTimeMillis();
                final Set<Player> set = getObserversSetTmp(observers.size());
                final Iterator<Map.Entry<Player, Long>> it = observers.entrySet().iterator();
                while (it.hasNext()) {
                    final Map.Entry<Player, Long> entry = it.next();
                    final Player player = entry.getKey();
                    if (player.isDead() || entry.getValue() < now){
                        if (plugin.setting.DEBUG) plugin.getLogger().info(getName() + "已移除渲染" + player.getName());
                        //移出不在线，或者超时未刷新的玩家
                        it.remove();
                    } else {
                        set.add(player);
                    }
                }
                return set;
            }
        } else if (playerInput.isPlaying()){
            final Player[] players = playerInput.getPlayers();
            final Set<Player> set = getObserversSetTmp(players.length);
            for (Player player : players) {
                if (player != null && player.isOnline()) set.add(player);
            }
            return set;
        }
        return Collections.emptyList();
    }

    //添加观察者
    public void putObservers(Player player) {
        synchronized (observers) {
/*            if(!observers.containsKey(player)){
                //当玩家加入游戏时发送一次完整地图
                for (int i = 0; i < ids.size(); i++) {
                    plugin.getPlayerNms().sendMap(player,ids.get(i),colors[i]);
                }
            }*/
            observers.put(player,System.currentTimeMillis() + 1000L * 20); //每次调用这个方法，为玩家主动渲染20秒
        }
    }

    //获取观测者缓存
    Set<Player> getObserversSetTmp(int initialCapacity) {
        Set<Player> set = observersSetTmp.get();
        if (set == null){
            set = new HashSet<>(initialCapacity);
            observersSetTmp = new WeakReference<>(set);
        } else {
            set.clear();
        }
        return set;
    }
}
