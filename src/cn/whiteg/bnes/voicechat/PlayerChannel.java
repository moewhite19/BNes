package cn.whiteg.bnes.voicechat;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import de.maxhenkel.voicechat.concentus.OpusApplication;
import de.maxhenkel.voicechat.plugins.impl.opus.OpusManager;
import de.maxhenkel.voicechat.voice.common.Utils;
import org.bukkit.entity.Player;

public class PlayerChannel {
    final Player player;
    final AudioChannel audioChannel;
    private final ServerPlayer serverPlayer;
    private final OpusEncoder encoder;

    public PlayerChannel(Player player,AudioChannel audioChannel,ServerPlayer serverPlayer) {
        this.player = player;
        this.audioChannel = audioChannel;
        this.serverPlayer = serverPlayer;
        encoder = OpusManager.createEncoder(48000,960,1024,OpusApplication.OPUS_APPLICATION_AUDIO);//这个几乎改不了
    }

    public void close() {
        if (!encoder.isClosed()){
            encoder.close();
            if (!isClose()) audioChannel.flush();
        }
    }

    public void sendMessage(byte[] bytes) {
        try{
            audioChannel.send(encoder.encode(Utils.bytesToShorts(bytes)));
        }catch (Exception e){
            //出现错误关闭这个通道
            close();
        }
    }
    public void sendMessage(short[] shorts) {
        try{
            audioChannel.send(encoder.encode(shorts));
        }catch (Exception e){
            //出现错误关闭这个通道
            close();
        }
    }

    public boolean isClose() {
        return encoder.isClosed() || audioChannel.isClosed() || !Voicechat.SERVER.getServer().getConnections().containsKey(serverPlayer.getUuid());
    }

    public Player getPlayer() {
        return player;
    }
}
