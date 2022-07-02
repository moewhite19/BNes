package cn.whiteg.bnes.voicechat;

import cn.whiteg.bnes.BNes;
import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.audio.AudioConverter;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.plugins.impl.VoicechatServerApiImpl;
import de.maxhenkel.voicechat.voice.server.Server;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class VoiceChatPlugin implements VoicechatPlugin {
    final BNes plugin;
    Server SERVER;
    VoicechatServerApi API;
    AudioConverter CONVERTER;

    public VoiceChatPlugin(BNes plugin) {
        this.plugin = plugin;
        BukkitVoicechatService service = Bukkit.getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null){
            service.registerPlugin(this);
        }
        SERVER = Voicechat.SERVER.getServer();
        API = new VoicechatServerApiImpl(Bukkit.getServer());
        CONVERTER = API.getAudioConverter();
    }

    @Override
    public String getPluginId() {
        return plugin.getName();
    }

    @Override
    public void initialize(VoicechatApi api) {
        plugin.getLogger().info("初始化服务 " + api.toString());
    }


    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class,this::onServerStarted);
    }

    public void onServerStarted(VoicechatServerStartedEvent event) {
        var api = event.getVoicechat();
        plugin.getLogger().info("启动服务 " + api.toString());
//        final Group g = api.createGroup("1","");
//        final OpusEncoder encoder = api.createEncoder();
//        final LocationalAudioChannel channel = api.createLocationalAudioChannel(UUID.randomUUID(),null,null);
    }

    public AudioChannel openAudio(UUID uuid ,Player player) {
        final VoicechatConnection connection = API.getConnectionOf(API.fromServerPlayer(player));
        if (connection != null){
            return API.createStaticAudioChannel(uuid,connection.getPlayer().getServerLevel(),connection);
        }
        return null;
    }

    public Server getServer() {
        return SERVER;
    }

    public VoicechatServerApi getApi() {
        return API;
    }

    public AudioConverter getConverter() {
        return CONVERTER;
    }
}
