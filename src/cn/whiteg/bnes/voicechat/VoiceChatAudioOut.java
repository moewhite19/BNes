package cn.whiteg.bnes.voicechat;

import cn.whiteg.bnes.render.BukkitRender;
import com.grapeshot.halfnes.audio.AudioOutInterface;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoderMode;
import de.maxhenkel.voicechat.plugins.impl.opus.OpusManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class VoiceChatAudioOut implements AudioOutInterface {
    private final int samplesPerFrame;
    private final int pipeSize;
    private final float outputVol;
    final private VoiceChatPlugin voiceChatPlugin;
    final private BukkitRender render;
    //    long nextSendTime = System.currentTimeMillis();
    int bufPer = 0;
    short[] audioBuff;
    LocationalAudioChannel audioChannel;
    Location loc;
    OpusEncoder encoder;


    public VoiceChatAudioOut(final BukkitRender render,VoiceChatPlugin voiceChatPlugin) {
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
        try{
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
//            outputStream = new PipedOutputStream();
//            inputStream = new PipedInputStream(outputStream,pipeSize);
            pipeSize = 960 * 3; //缓冲区大小,缓存3帧的音频
            audioBuff = new short[pipeSize];
        }catch (Exception e){
            throw new RuntimeException(e);
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

        final int read = 960; //读取数组长度
        //如果缓存小于需要的数量，则不读取
        if (bufPer < read) return;
        //如果通道不存在就尝试更新通道
        if (channelLost()){
            for (Player player : render.getPlayerInput().getPlayers()) {
                if (player != null && !player.isDead()){
                    updateLoc(player.getLocation());
                    break;
                }
            }
        }
        if (audioChannel != null && loc != null){
            audioChannel.send(encoder.encode(readAudioBuff(read)));
        }
//        synchronized (channels) {
//            if (!channels.isEmpty()){
//                final short[] soundBuff = readAudioBuff(read);
//                for (int i = 0; i < channels.size(); i++) {
//                    final PlayerChannel playerChannel = channels.get(i);
//                    //如果已关闭则把玩家从队列移出
//                    if (playerChannel.isClose()){
//                        channels.remove(i);
//                        i--;
//                        playerChannel.close();
//                    } else {
//                        playerChannel.sendMessage(soundBuff);
//                    }
//                }
//            }
//        }
    }

    public short[] readAudioBuff(int size) {
        if (bufPer < size) return new short[0]; //如果当前流没有那么多

        //读取的数组
        final short[] shorts = new short[size];
        System.arraycopy(audioBuff,0,shorts,0,size);

        //将后面的数组往前移
        bufPer -= size;
        System.arraycopy(audioBuff,size,audioBuff,0,size);
        return shorts;
    }

    @Override
    public final void outputSample(int sample) {
        //通道没有玩家时不处理声音
        //满了就把声音丢了
        if (channelLost() || bufPer >= pipeSize){
            return;
        }
        sample *= outputVol;
        if (sample < Short.MIN_VALUE){
            sample = Short.MIN_VALUE;
            //System.err.println("clip");
        }
        if (sample > Short.MAX_VALUE){
            sample = Short.MAX_VALUE;
            //System.err.println("clop");
        }
//        audiobuf[bufptr] = ((short) sample);
//        audiobuf[bufptr + 1] = ((short) sample);
//        bufptr += 1;

        audioBuff[bufPer] = (short) sample;
        bufPer++;
    }

    @Override
    public void pause() {
        flushFrame(false);
        bufPer = 0;
    }

    @Override
    public void resume() {
        updateLoc(loc);
    }

    @Override
    public final void destroy() {
        audioChannel = null;
        if (encoder != null){
            encoder.close();
            encoder = null;
        }
        bufPer = 0;
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

    public void updateLoc(Location loc) {
        //我也不知道在什么时候loc.getWorld()会是null， 测试的时候没遇到过。
        if (loc == null || loc.getWorld() == null || (this.loc != null && this.loc.equals(loc))) return;
        VoicechatServerApi api = voiceChatPlugin.getApi();
        final Position position = api.createPosition(loc.getX(),loc.getY(),loc.getZ());
        if (audioChannel == null || loc.getWorld() != this.loc.getWorld()){
            audioChannel = api.createLocationalAudioChannel(UUID.randomUUID(),api.fromServerLevel(loc.getWorld()),position);
            if (encoder == null) encoder = OpusManager.createEncoder(OpusEncoderMode.AUDIO);
        } else {
            audioChannel.updateLocation(position);
        }
        this.loc = loc;
    }

    public BukkitRender getRender() {
        return render;
    }

    //检查是否可用，如果不可用尝试让他启用
    public boolean channelLost() {
        if (audioChannel != null) return false;
        if (loc == null) return true;
        VoicechatServerApi api = voiceChatPlugin.getApi();
        audioChannel = api.createLocationalAudioChannel(UUID.randomUUID(),api.fromServerLevel(loc.getWorld()),api.createPosition(loc.getX(),loc.getY(),loc.getZ()));
        encoder = OpusManager.createEncoder(OpusEncoderMode.AUDIO);
        return false;
    }

    public boolean activate() {
        return audioChannel != null;
    }
}
