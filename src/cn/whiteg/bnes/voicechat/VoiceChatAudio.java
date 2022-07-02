package cn.whiteg.bnes.voicechat;

import cn.whiteg.bnes.render.BukkitRender;
import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.PrefsSingleton;
import com.grapeshot.halfnes.audio.AudioOutInterface;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VoiceChatAudio implements AudioOutInterface {
    private byte[] audiobuf;
    private int bufptr = 0;
    private float outputVol;
    private VoiceChatPlugin voiceChatPlugin;
    UUID session = UUID.randomUUID();
    private BukkitRender render;
    List<PlayerChannel> channels = new ArrayList<>(2);


    public VoiceChatAudio(final BukkitRender render,VoiceChatPlugin voiceChatPlugin) {
        //插件采样率48000
        //模拟器原生输出采样率44100
        double sampleRate = 30000;

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
        final int samplesPerFrame = (int) Math.ceil((sampleRate * 2) / fps);
        audiobuf = new byte[samplesPerFrame * 2];
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
        final AudioChannel audioChannel = voiceChatPlugin.openAudio(session,player);
        if (audioChannel != null){
            channels.add(new PlayerChannel(player,audioChannel));
        }
    }

    public void removePlayer(Player player) {
        if (channels.isEmpty()) return;
        for (int i = 0; i < channels.size(); i++) {
            final PlayerChannel channel = channels.get(i);
            if (channel.getPlayer().equals(player)){
                channel.close();
                channels.remove(i);
                return;
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


        if (!channels.isEmpty()){
            try{
                byte[] buff = new byte[bufptr];
                System.arraycopy(audiobuf,0,buff,0,bufptr);
                for (int i = 0; i < channels.size(); i++) {
                    final PlayerChannel playerChannel = channels.get(i);
                    if(playerChannel.isClose()){
                        channels.remove(i);
                        i--;
                    }
                    playerChannel.sendMessage(buff);
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
        bufptr = 0;
    }

    @Override
    public final void outputSample(int sample) {
        if(bufptr + 4 > audiobuf.length) return;
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
            if(player != null) addPlayer(player);
        }
    }

    @Override
    public final void destroy() {
        if(!channels.isEmpty()){
            for (PlayerChannel channel : channels) {
                channel.close();
            }
            channels.clear();
        }
    }

    @Override
    public final boolean bufferHasLessThan(final int samples) {
        //returns true if the audio buffer has less than the specified amt of samples remaining in it
//        return sdl != null && ((sdl.getBufferSize() - sdl.available()) <= samples);
//        return bufptr + samples < audiobuf.length;
        return true;
    }
}
