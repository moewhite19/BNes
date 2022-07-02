package cn.whiteg.bnes.voicechat;

import cn.whiteg.bnes.render.BukkitRender;
import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.PrefsSingleton;
import com.grapeshot.halfnes.audio.AudioOutInterface;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import org.bukkit.entity.Player;

import javax.sound.sampled.*;
import java.util.*;

public class VoiceChatAudio implements AudioOutInterface {
    private final OpusEncoder encoder;
    private byte[] audiobuf;
    private int bufptr = 0;
    private float outputvol;
    private VoiceChatPlugin voiceChatPlugin;
    UUID session = UUID.randomUUID();
    private BukkitRender render;
    HashMap<UUID, AudioChannel> channels = new HashMap<>(2);


    public VoiceChatAudio(final BukkitRender render,VoiceChatPlugin voiceChatPlugin,final int samplerate) {
        this.render = render;
        NES nes = render.getNes();
        this.voiceChatPlugin = voiceChatPlugin;
        encoder = voiceChatPlugin.getApi().createEncoder();
        outputvol = (float) (PrefsSingleton.get().getInt("outputvol",13107) / 16384.);
        double fps;
        switch (nes.getMapper().getTVType()) {
            case NTSC:
            default:
                fps = 60.;
                break;
            case PAL:
            case DENDY:
                fps = 50.;
                break;
        }
        final int samplesPerFrame = (int) Math.ceil((samplerate * 2) / fps);
        audiobuf = new byte[samplesPerFrame * 2];
/*        try{
            AudioFormat af = new AudioFormat(
                    samplerate,
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
            channels.put(player.getUniqueId(),audioChannel);
        }
    }

    public void removePlayer(Player player) {
        if (channels.isEmpty()) return;
        channels.remove(player.getUniqueId());
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
                buff = encoder.encode(voiceChatPlugin.getConverter().bytesToShorts(buff));
                for (Map.Entry<UUID, AudioChannel> entry : channels.entrySet()) {
                    AudioChannel channel = entry.getValue();
                    if (channel.isClosed()){
    //                    channels.set(i , voiceChatPlugin.openAudio(session,channel));
                        System.out.println("过期的会话: " + channel);
                    } else {
                        channel.send(buff);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        bufptr = 0;
    }

    @Override
    public final void outputSample(int sample) {
        sample *= outputvol;
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
        channels.clear();
        encoder.resetState();
    }

    @Override
    public void resume() {
    }

    @Override
    public final void destroy() {
        channels.clear();
        encoder.close();
    }

    @Override
    public final boolean bufferHasLessThan(final int samples) {
        //returns true if the audio buffer has less than the specified amt of samples remaining in it
//        return sdl != null && ((sdl.getBufferSize() - sdl.available()) <= samples);
        return true;
    }
}
