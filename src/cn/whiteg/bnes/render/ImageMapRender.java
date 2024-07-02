package cn.whiteg.bnes.render;

import cn.whiteg.bnes.buffmap.BuffMapConstructor;
import cn.whiteg.bnes.buffmap.ChunkConstructor;
import cn.whiteg.bnes.buffmap.NoneConstructor;
import cn.whiteg.bnes.nms.PlayerNms;
import cn.whiteg.bnes.utils.CommonUtils;
import cn.whiteg.bnes.voicechat.VoiceChatAudioOut;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class ImageMapRender extends MapRenderer {
    public static Map<String, BuffMapConstructor> cacheMap = new WeakHashMap<>();
    private final BukkitRender handler;
    private final byte[] colors;
    private final int index;

    public ImageMapRender(BukkitRender handler,byte[] colors,int index) {
        super(false);
        this.handler = handler;
        this.colors = colors;
        this.index = index;
    }

    @Override
    public void render(MapView mapView,MapCanvas mapCanvas,Player player) {
        //如果玩家在游玩时这地图存在其他观察者时，用这个方法去更新画面会极大影响当前玩家体验
        if (handler.isActive()){
            if (handler.plugin.setting.activelyRenderEveryone){
                //为围观的玩家也主动更新
                handler.putObservers(player);
                //如果游戏机当前不知道往哪输出音频
                final VoiceChatAudioOut audioOutInterface = handler.audioOutInterface;
                if (audioOutInterface != null && audioOutInterface.channelLost()){
                    audioOutInterface.updateLoc(player.getLocation().add(player.getFacing().getDirection())); //获取玩家位置，并向前位移一米
                }
            } else {
                //完全由自己来发包更新地图
                if (handler.playerInput.isPlaying(player)) return; //在游玩时使用主动更新,被动更新屏蔽
                String key = player.getName() + "#" + index;
                var buffer = cacheMap.computeIfAbsent(key,s -> handler.plugin.setting.sendFullFrame ? new NoneConstructor() : new ChunkConstructor(4));
                List<MapItemSavedData.MapPatch> chunks = buffer.makeUpdate(colors);
                if (chunks != null){
                    Packet<?>[] packets = new Packet[chunks.size()];
                    for (int i = 0; i < chunks.size(); i++) {
                        packets[i] = PlayerNms.createMapPacket(index,chunks.get(i),((byte) 2));
                    }
                    handler.plugin.getPlayerNms().sendPacket(player,packets);
                }
            }

        } else {
            handler.lastRender = System.currentTimeMillis(); //更新最近渲染时间
            //都已经休眠了还更新什么，基本不会有变化。加个随机检测吧，万一更新了呢
            int r = CommonUtils.RANDOM.nextInt(16383);
            int rx = r % 128, ry = r / 128;
            //noinspection removal
            if (mapCanvas.getPixel(rx,ry) == colors[r]) return; //像素没变化跳出
            for (int i = 0; i < colors.length; i++) {
                int x = i % 128, y = i / 128;
                mapCanvas.setPixel(x,y,colors[i]);
            }
        }
    }
}
