package cn.whiteg.bnes.voicechat;

import cn.whiteg.bnes.BNes;
import cn.whiteg.bnes.utils.CommonUtils;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoderMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class TestListener implements Listener {
    @EventHandler
    public void onChat(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) return;
        final VoiceChatPlugin voiceChatPlugin = BNes.plugin.getVoiceChatPlugin();
        if (voiceChatPlugin != null){
            final AudioChannel channel = voiceChatPlugin.openAudio(UUID.randomUUID(),event.getPlayer());
            if (channel != null){
                System.out.println("通道: " + channel);
//                OpusEncoder encoder = OpusManager.createEncoder(48000,960,5120,OpusApplication.OPUS_APPLICATION_AUDIO);
                OpusEncoder encoder = voiceChatPlugin.getApi().createEncoder(OpusEncoderMode.AUDIO);
                //加载文件音频
                try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(Objects.requireNonNull(VoiceChatPlugin.class.getResource("/dayi.wav")))){
                    //文件格式
                    AudioFormat format = audioInputStream.getFormat();
                    System.out.println(format);
                    //将码率转换为需要的格式
                    final AudioFormat target = new AudioFormat(format.getEncoding(),48000f,format.getSampleSizeInBits(),format.getChannels(),format.getFrameSize(),format.getFrameRate(),format.isBigEndian());
                    try (final AudioInputStream targetAudio = AudioSystem.getAudioInputStream(target,audioInputStream)){
                        byte[] data;

                        while ((data = audioInputStream.readNBytes(1024)).length > 0) {
                            final byte[] encode = encoder.encode(voiceChatPlugin.getConverter().bytesToShorts(data));
                            channel.send(encode);
                        }
                        channel.flush();
                    }
                }catch (UnsupportedAudioFileException | IOException e){
                    throw new RuntimeException(e);
                } finally {
                    if (encoder != null) encoder.close();
                }
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        final VoiceChatPlugin voiceChatPlugin = BNes.plugin.getVoiceChatPlugin();
        if (voiceChatPlugin != null){
//            final VoicechatServerApi api = voiceChatPlugin.getApi();
//            final AudioPlayer audioPlayer = api.createAudioPlayer(api.createEntityAudioChannel(UUID.randomUUID(),api.fromEntity(event.getPlayer())));
        }
    }

    @SuppressWarnings({"ControlFlowStatementWithoutBraces"})
    public static void main(String[] args) throws Throwable {
        for (AudioFileFormat.Type audioFileType : AudioSystem.getAudioFileTypes()) {
            System.out.println(audioFileType);
        }
        try (var input = Objects.requireNonNull(VoiceChatPlugin.class.getResourceAsStream("/dayi.wav")); AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(input)){
            AudioFormat format = audioInputStream.getFormat();
            System.out.println(format);
//            format = new AudioFormat(10240f,format.getSampleSizeInBits(),format.getChannels(),format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED,format.isBigEndian());
            final SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open();
            line.start();
            int count;
            long size = 0;
            byte[] buff = new byte[1024];
            while ((count = audioInputStream.read(buff,0,buff.length)) != -1) {
                line.write(buff,0,count);
                size += count;
                System.out.print("\r" + size);
            }
            System.out.println("");
            System.out.println(CommonUtils.tanSpace(size));
        }
    }

}
