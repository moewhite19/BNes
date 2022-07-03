package cn.whiteg.bnes.voicechat;

import cn.whiteg.bnes.render.BukkitRender;
import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.audio.AudioOutInterface;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.plugins.impl.opus.OpusManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VoiceChatAudioSystem implements AudioOutInterface {
    private final byte[] audiobuf;
    private final int samplesPerFrame;
    private int bufptr = 0;
    private final float outputVol;
    final private VoiceChatPlugin voiceChatPlugin;
    UUID session = UUID.randomUUID();
    final private BukkitRender render;
    final List<PlayerChannel> channels = new ArrayList<>(2);


    public VoiceChatAudioSystem(final BukkitRender render,VoiceChatPlugin voiceChatPlugin) {
        //voicechat插件采样率48000
        //位数??
        //帧大小960

        //模拟器默认输出采样率44100
        //16位数
        //帧大小= samplesPerFrame * 4 * 2 /*ch*/ * 2 /*bytes/sample*/; //大概23520

        //当前采样率
        int sampleRate = OpusManager.SAMPLE_RATE; //48000

        this.render = render;
        NES nes = render.getNes();
        this.voiceChatPlugin = voiceChatPlugin;
        outputVol = 0.5f;
        double fps;
        switch (nes.getMapper().getTVType()) {
            case NTSC:
            default:
                fps = 60;
                break;
            case PAL:
            case DENDY:
                fps = 50;
                break;
        }
        samplesPerFrame = (int) Math.ceil((sampleRate * 2) / fps);
//        samplesPerFrame = OpusManager.FRAME_SIZE;
//        System.out.println(frameSize);
        audiobuf = new byte[samplesPerFrame * 2];
//        audiobuf = new byte[2048];
/*        try{
            AudioFormat af = new AudioFormat(
                    sampleRate,
                    16,//bit
                    2,//channel
                    true,//signed
                    false //little endian
                    //(works everywhere, afaict, but macs need 44100 sample rate)
            );


            SourceDataLine sdl = AudioSystem.getSourceDataLine(af);

            final int bufferSize = samplesPerFrame * 4 * 2 *//*ch*//* * 2*//*bytes/sample*//*;
            sdl.open(af,bufferSize);
            //create 4 frame audio buffer
            sdl.start();
        }catch (LineUnavailableException | IllegalArgumentException a){
//                System.err.println(a);
            render.messageBox("Unable to inintialize sound.");
        }*/
    }

    public void addPlayer(Player player) {
//        final AudioChannel audioChannel = voiceChatPlugin.openAudio(session,player);
        //创建玩家通道
        final ServerPlayer serverPlayer = voiceChatPlugin.getApi().fromServerPlayer(player);
        final VoicechatConnection connection = voiceChatPlugin.getApi().getConnectionOf(serverPlayer);
        if (connection != null){
            synchronized (channels) {
                //如果玩家已存在就不再添加
                final UUID uniqueId = player.getUniqueId();
                for (PlayerChannel channel : channels) {
                    if (channel.getPlayer().getUniqueId().equals(uniqueId)){
                        return;
                    }
                }

                channels.add(new PlayerChannel(player,voiceChatPlugin.getApi().createStaticAudioChannel(session,serverPlayer.getServerLevel(),connection),serverPlayer));
            }
        }
    }

    public void removePlayer(Player player) {
        synchronized (channels) {
            if (channels.isEmpty()) return;
            for (int i = 0; i < channels.size(); i++) {
                final PlayerChannel channel = channels.get(i);
                if (channel.getPlayer().getUniqueId().equals(player.getUniqueId())){
                    channel.close();
                    channels.remove(i);
                    i--;
                }
            }
        }
    }

    @Override
    public final void flushFrame(final boolean waitIfBufferFull) {
/*
        //            if (sdl.available() == sdl.getBufferSize()) {
//                System.err.println("Audio is underrun");
//            }
        if (sdl.available() < bufptr){
//                System.err.println("Audio is blocking");
            if (waitIfBufferFull){

                //write to audio buffer and don't worry if it blocks
                sdl.write(audiobuf,0,bufptr);
            }
            //else don't bother to write if the buffer is full
        } else {
            sdl.write(audiobuf,0,bufptr);
        }
        bufptr = 0;
*/

        synchronized (channels) {
            if (!channels.isEmpty()){
                try{
                    byte[] buff = new byte[bufptr];
                    System.arraycopy(audiobuf,0,buff,0,bufptr);
                    for (int i = 0; i < channels.size(); i++) {
                        final PlayerChannel playerChannel = channels.get(i);
                        //如果已关闭则把玩家从队列移出
                        if (playerChannel.isClose()){
                            channels.remove(i);
                            i--;
                        } else {
                            playerChannel.sendMessage(buff);
                        }
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
            }

        }
        bufptr = 0;
    }

    @Override
    public final void outputSample(int sample) {
        if (bufptr + 4 > audiobuf.length) return;
        sample *= outputVol;
        if (sample < -32768){
            sample = -32768;
            //System.err.println("clip");
        }
        if (sample > 32767){
            sample = 32767;
            //System.err.println("clop");
        }
        //left ch
        int lch = sample;
        audiobuf[bufptr] = (byte) (lch & 0xff);
        audiobuf[bufptr + 1] = (byte) ((lch >> 8) & 0xff);
        //right ch
        int rch = sample;
        audiobuf[bufptr + 2] = (byte) (rch & 0xff);
        audiobuf[bufptr + 3] = (byte) ((rch >> 8) & 0xff);
        bufptr += 4;
    }

    @Override
    public void pause() {
        destroy();
    }

    @Override
    public void resume() {
        for (Player player : render.getPlayerInput().getPlayers()) {
            if (player != null) addPlayer(player);
        }
    }

    @Override
    public final void destroy() {
        synchronized (channels) {
            if (!channels.isEmpty()){
                for (PlayerChannel channel : channels) {
                    channel.close();
                }
                channels.clear();
            }
        }
    }

    @Override
    public final boolean bufferHasLessThan(final int samples) {
        //returns true if the audio buffer has less than the specified amt of samples remaining in it
//        return sdl != null && ((sdl.getBufferSize() - sdl.available()) <= samples);
//        return bufptr + samples < audiobuf.length;
        return true;
    }

    public int getSamplesPerFrame() {
        return samplesPerFrame;
    }
}
