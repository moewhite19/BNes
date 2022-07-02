package cn.whiteg.bnes.voicechat;

import cn.whiteg.bnes.BNes;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import de.maxhenkel.voicechat.concentus.OpusApplication;
import de.maxhenkel.voicechat.plugins.impl.opus.OpusManager;
import de.maxhenkel.voicechat.voice.common.Utils;
import org.bukkit.entity.Player;

public class PlayerChannel {
    final Player player;
    final AudioChannel audioChannel;
    private OpusEncoder encoder;

    public PlayerChannel(Player player,AudioChannel audioChannel) {
        this.player = player;
        this.audioChannel = audioChannel;
        encoder = OpusManager.createEncoder(48000,960,1024,OpusApplication.OPUS_APPLICATION_AUDIO);//这个几乎改不了
    }

    public void close() {
        if (!encoder.isClosed()){
            encoder.close();
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

    public boolean isClose() {
        return encoder.isClosed() || audioChannel.isClosed() || BNes.plugin.getVoiceChatPlugin().getApi().getConnectionOf(player.getUniqueId()) == null;
    }

    public Player getPlayer() {
        return player;
    }
}
