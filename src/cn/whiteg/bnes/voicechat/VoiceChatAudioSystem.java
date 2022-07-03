package cn.whiteg.bnes.voicechat;

import cn.whiteg.bnes.render.BukkitRender;
import com.grapeshot.halfnes.audio.AudioOutInterface;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.plugins.impl.opus.OpusManager;
import de.maxhenkel.voicechat.voice.common.Utils;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VoiceChatAudioSystem implements AudioOutInterface {
    //    public static AudioFormat CHAT_FORMAT = new AudioFormat(48000f,16,1,true,false);
    private final int samplesPerFrame;
    private final int pipeSize;
    private final float outputVol;
    final private VoiceChatPlugin voiceChatPlugin;
    UUID session = UUID.randomUUID();
    final private BukkitRender render;
    final List<PlayerChannel> channels = new ArrayList<>(2);
//    long nextSendTime = System.currentTimeMillis();
    PipedInputStream inputStream;
    PipedOutputStream outputStream;


    public VoiceChatAudioSystem(final BukkitRender render,VoiceChatPlugin voiceChatPlugin) {
        //voicechat插件采样率48000
        //位数??
        //帧大小960

        //模拟器默认输出采样率44100
        //16位数
        //帧大小= samplesPerFrame * 4 * 2 /*ch*/ * 2 /*bytes/sample*/; //大概23520

        //当前采样率
//        int sampleRate = PrefsSingleton.get().getInt("sampleRate",42000); //48000

        this.render = render;
        this.voiceChatPlugin = voiceChatPlugin;
        outputVol = 0.3f;
//        samplesPerFrame = (int) Math.ceil((sampleRate * 2) / fps);
        samplesPerFrame = OpusManager.FRAME_SIZE;
//        System.out.println(frameSize);
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
        try{
            outputStream = new PipedOutputStream();
            pipeSize = 1920 * 2; //缓冲区大小,缓存3帧的音频
            inputStream = new PipedInputStream(outputStream,pipeSize);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
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
/*f
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

//        //流需要保持在50帧
//        long now = System.currentTimeMillis();
//        if (now < nextSendTime) return;
//
//        //计算下一帧的时间
//        if (now - nextSendTime > 1000) nextSendTime = now + 20;
//        else nextSendTime += 20;

        final int read = 1920; //读取数组长度
        try{
            //如果缓存小于需要的数量，则不读取，防止堵塞
            if (inputStream.available() < read) return;
            synchronized (channels) {
                if (!channels.isEmpty()){
                    for (int i = 0; i < channels.size(); i++) {
                        final PlayerChannel playerChannel = channels.get(i);
                        //如果已关闭则把玩家从队列移出
                        if (playerChannel.isClose()){
                            channels.remove(i);
                            i--;
                            playerChannel.close();
                        } else {
                            playerChannel.sendMessage(inputStream.readNBytes(read));
                        }
                    }
                }
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public final void outputSample(int sample) {
        try{
            //通道没有玩家时不处理声音
            //防止堵塞，如果满了就丢了
            if (channels.isEmpty() || inputStream.available() >= pipeSize){
                return;
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        sample *= outputVol;
        if (sample < -32768){
            sample = -32768;
            //System.err.println("clip");
        }
        if (sample > 32767){
            sample = 32767;
            //System.err.println("clop");
        }
//        audiobuf[bufptr] = ((short) sample);
//        audiobuf[bufptr + 1] = ((short) sample);
//        bufptr += 1;

        try{
            outputStream.write(Utils.shortToBytes((short) sample));
        }catch (IOException e){
            throw new RuntimeException(e);
        }

        //left ch
        /*int lch = sample;
        audiobuf[bufptr] = (byte) (lch & 0xff);
        audiobuf[bufptr + 1] = (byte) ((lch >> 8) & 0xff);
        //right ch
        int rch = sample;
        audiobuf[bufptr + 2] = (byte) (rch & 0xff);
        audiobuf[bufptr + 3] = (byte) ((rch >> 8) & 0xff);
        bufptr += 4;*/
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

        try{
            inputStream.close();
            outputStream.close();
        }catch (IOException e){
            e.printStackTrace();
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
