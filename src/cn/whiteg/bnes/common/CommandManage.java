package cn.whiteg.bnes.common;

import cn.whiteg.bnes.utils.PluginUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandManage extends CommandInterface {
    public Map<String, CommandInterface> commandMap = new HashMap<>();
    JavaPlugin plugin;
    String path;

    public CommandManage(JavaPlugin plugin) {
        this.plugin = plugin;
        path = plugin.getClass().getPackage().getName().replace('.','/') + "/commands";
        init();
    }

    public CommandManage(JavaPlugin plugin,String pack) {
        this.plugin = plugin;
        this.path = pack.replace('.','/');
        init();
    }

    //初始化
    private void init() {
        try{
            List<String> urls = PluginUtil.getUrls(plugin.getClass().getClassLoader(),false);
            for (String url : urls) {
                if (url.startsWith(path)){
                    int i = url.indexOf(".class");
                    if (i == -1) continue;
                    String path = url.replace('/','.').substring(0,i);
                    try{
                        Class<?> clazz = plugin.getClass().getClassLoader().loadClass(path);
                        if (CommandInterface.class.isAssignableFrom(clazz)){
                            CommandInterface ci = null;
                            //优先使用传入插件对象为参数的构造函数
                            for (Constructor<?> constructor : clazz.getConstructors()) {
                                Class<?>[] types = constructor.getParameterTypes();
                                if (types.length == 1 && plugin.getClass().isAssignableFrom(types[0])){
                                    ci = (CommandInterface) constructor.newInstance(plugin);
                                    break;
                                }
                            }
                            if (ci == null) ci = (CommandInterface) clazz.newInstance();
                            registerCommand(ci);
                        }
                    }catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException e){
                        plugin.getLogger().warning("无法构建指令: " + path);
                        e.printStackTrace();
                    }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        resizeMap();
    }

    @Override
    public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length == 0){
            return onMain(sender);
        }
        CommandInterface subCommand = commandMap.get(args[0]);
        if (subCommand != null){
            if (args.length > 1){
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args,1,subArgs,0,subArgs.length);
                return subCommand.onCommand(sender,cmd,label,subArgs);
            } else {
                return subCommand.onCommand(sender,cmd,label,new String[]{});
            }
        } else {
            sender.sendMessage("无效指令");
        }
        return false;
    }

    public boolean onMain(CommandSender sender) {
        sender.sendMessage("§3[§b" + plugin.getDescription().getFullName() + "§3]");
        for (Map.Entry<String, CommandInterface> entry : commandMap.entrySet()) {
            CommandInterface ci = entry.getValue();
            if (ci.canUseCommand(sender)) sender.sendMessage("§a" + ci.getName() + "§f:§b " + ci.getDescription());
        }
        return true;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length == 1){
            return getMatches(args[0].toLowerCase(),getCanUseCommands(sender));
        } else if (args.length > 1){
            CommandInterface subCommand = commandMap.get(args[0]);
            if (subCommand != null){
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args,1,subArgs,0,subArgs.length);
                return subCommand.onTabComplete(sender,cmd,label,subArgs);
            }
        }
        return null;
    }

    //获取玩家可用指令列表
    public List<String> getCanUseCommands(CommandSender sender) {
        List<String> list = new ArrayList<>(commandMap.size());
        commandMap.forEach((key,ci) -> {
            if (ci.canUseCommand(sender)) list.add(key);
        });
        return list;
    }

    public Map<String, CommandInterface> getCommandMap() {
        return commandMap;
    }

    //注册指令
    public void registerCommand(CommandInterface ci) {
        String name = ci.getName();
        commandMap.put(name,ci);
        PluginCommand pc = plugin.getCommand(name);
        if (pc != null){
            pc.setExecutor(ci);
            pc.setTabCompleter(ci);
        }
    }

    //构建完毕后固定map大小
    public void resizeMap() {
        commandMap = new HashMap<>(commandMap);
    }

    //设置指令执行器
    public void setExecutor(String name) {
        PluginCommand pc = plugin.getCommand(name);
        if (pc != null){
            pc.setExecutor(this);
            pc.setTabCompleter(this);
        }
    }

    public void setExecutor() {
        setExecutor(plugin.getName().toLowerCase());
    }
}
