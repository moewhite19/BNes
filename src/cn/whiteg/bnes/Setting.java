package cn.whiteg.bnes;

import cn.whiteg.bnes.render.PlayerController;
import cn.whiteg.bnes.utils.CommonUtils;
import com.grapeshot.halfnes.PrefsSingleton;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

public class Setting {
    private final static int CONFIGVER = 9;
    private static FileConfiguration storage;
    private final BNes plugin;
    public boolean DEBUG;
    public long idleSleep;
    public long updateTime = 33; //30fps
    public boolean showFps;
    public String defaultRom;
    public Material cardItem;
    public int updateMaxSizeLimit;
    public long letheTime;
    public boolean letheClearup;
    public double createPrice;
    public boolean sendFullFrame = false;
    public PlayerController.Button onSwap = PlayerController.Button.B;
    public PlayerController.Button onJump = PlayerController.Button.A;
    public PlayerController.Button onLMB = PlayerController.Button.SELECT;
    public PlayerController.Button onRMB = PlayerController.Button.START;
    public PlayerController.Button onDrop = null;
    public int playerInputFps = 50; // 20Fps
    public boolean activelyRenderEveryone;
    public float audioOutputVol = 0.3f;
    public float audioOutputDistance = 5f;

    public Setting(BNes plugin) {
        this.plugin = plugin;
        reload();
    }


    public void reload() {
        File file = new File(plugin.getDataFolder(),"config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        //自动更新配置文件
        if (config.getInt("ver") < CONFIGVER){
            plugin.saveResource("config.yml",true);
            config.set("ver",CONFIGVER);
            final FileConfiguration newcon = YamlConfiguration.loadConfiguration(file);
            Set<String> keys = newcon.getKeys(true);
            for (String k : keys) {
                if (config.contains(k)) continue;
                config.set(k,newcon.get(k));
                plugin.getLogger().info("新增配置节点: " + k);
            }
            try{
                config.save(file);
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        DEBUG = config.getBoolean("debug");

        idleSleep = CommonUtils.getTimeMintoh(config.getString("IdleSleep","50s"));
        updateTime = (long) (1000d / config.getDouble("UpdateMaxFps",30));
        showFps = config.getBoolean("ShowFps",false);
        defaultRom = config.getString("DefaultRom","none");
        updateMaxSizeLimit = Integer.parseInt(config.getString("UpdateMaxSizeLimit","1m"));
        sendFullFrame = config.getBoolean("SendFullFrame",false);

        letheTime = CommonUtils.getTimeMintoh(config.getString("LetheTime","30d"));
        letheClearup = config.getBoolean("LetheClose");
        createPrice = config.getDouble("CreatePrice",0);
        activelyRenderEveryone = config.getBoolean("ActivelyRenderEveryone",false);
        audioOutputVol = Math.max((float) config.getDouble("AudioOutputVol",audioOutputVol),0.1f);
        audioOutputDistance = Math.max((float) config.getDouble("AudioOutputDistance",audioOutputDistance),0.1f);

        //四舍五入
        playerInputFps = BigDecimal.valueOf(1000d / config.getDouble("PlayerInputFps",10d)).setScale(0,RoundingMode.HALF_UP).intValue();

        ConfigurationSection cs;
        cs = config.getConfigurationSection("KeyMapping");
        if (cs != null){
            Set<String> keys = cs.getKeys(false);
            for (String key : keys) {
                var value = cs.getString(key);
                if (value == null || value.isBlank()) continue;
                try{
                    Field f = this.getClass().getField(key);
                    f.set(this,PlayerController.Button.valueOf(value));
                }catch (Exception e){
                    plugin.getLogger().warning("无效配置: " + key + ": " + value);
                }
            }
        }

        try{
            cardItem = Material.valueOf(config.getString("CardItem"));
        }catch (Exception e){
            cardItem = Material.BRICK;
        }


        //创建可序列化内容
        storage = new YamlConfiguration();
        storage.options().pathSeparator('/');
        file = new File(file.getParentFile(),"storage.yml");
        try{
            if (file.exists()) storage.load(file);
        }catch (IOException | InvalidConfigurationException e){
            e.printStackTrace();
        }
        var p = PrefsSingleton.get();
        p.putBoolean("soundEnable",config.getBoolean("SoundEnable",false));
        if (DEBUG){
            plugin.getLogger().warning("已开启调试模式");
        }
    }

    public void saveStorage() {
        File file = new File(plugin.getDataFolder(),"storage.yml");
        try{
            storage.save(file);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public FileConfiguration getStorage() {
        return storage;
    }
}
