package cn.whiteg.bnes.utils;

import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class PluginUtil {

    public static void kickPlayer(Player p,String Message) {
        p.kickPlayer(Message);
    }

    @Deprecated
    public static PluginCommand getPluginCommanc(final JavaPlugin plugin,final String name) {
        return getPluginCommand(plugin,name);
    }

    public static PluginCommand getPluginCommand(final JavaPlugin plugin,final String name) {
        PluginCommand pc = plugin.getCommand(name);
        if (pc == null){
            try{
                final Constructor<PluginCommand> cr = PluginCommand.class.getDeclaredConstructor(String.class,Plugin.class);
                cr.setAccessible(true);
                pc = cr.newInstance(name,plugin);
                pc.setDescription("None " + name);
                pc.setUsage("/" + name);
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }
        return pc;
    }


    public static List<String> getUrls(ClassLoader loader) throws IOException {
        return getUrls(loader,false);
    }

    /**
     * 遍历插件内的资源文件
     *
     * @param loader Class加载器
     * @param folder 是否包含文件夹
     * @return 返回文件列表
     * @throws IOException IO异常
     */
    public static List<String> getUrls(ClassLoader loader,boolean folder) throws IOException {
        List<String> list = new ArrayList<>();
        if (loader instanceof URLClassLoader){
            URLClassLoader classLoader = (URLClassLoader) loader;
            URL[] jars = classLoader.getURLs();
            for (URL jar : jars) {
                JarInputStream jarInput = new JarInputStream(jar.openStream());
                while (true) {
                    JarEntry entry = jarInput.getNextJarEntry();
                    if (entry == null) break;
                    if (!folder && entry.isDirectory()) continue;
                    list.add(entry.getName());
                }
            }
        }
        return list;
    }
}
