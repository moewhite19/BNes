package cn.whiteg.bnes.voicechat;

import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoderMode;
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
        encoder = OpusManager.createEncoder(OpusEncoderMode.AUDIO);
    }

    public void close() {
        if (!encoder.isClosed()){
            encoder.close();
        }
    }

    public void sendMessage(byte[] bytes) {
        if (audioChannel.isClosed()){
            close();
        }

        try{
            audioChannel.send(encoder.encode(Utils.bytesToShorts(bytes)));
        }catch (Exception e){
            e.printStackTrace();
            close();
        }
    }

    public void reset() {
        if (!encoder.isClosed()){
            encoder.close();
        }
        encoder = OpusManager.createEncoder(OpusEncoderMode.AUDIO);
    }

    public boolean isClose() {
        return encoder.isClosed() || audioChannel.isClosed();
    }

    public Player getPlayer() {
        return player;
    }
}
