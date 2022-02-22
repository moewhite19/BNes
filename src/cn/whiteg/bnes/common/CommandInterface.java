package cn.whiteg.bnes.common;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class CommandInterface implements CommandExecutor, TabCompleter {
    static int completionLengthLimit = 256; //补全长度限制

    public static List<String> getMatches(String[] args,List<String> list) {
        return args.length == 0 ? getMatches(((String) null),list) : getMatches(args[args.length - 1],list);
    }

    public static List<String> getMatches(List<String> list,String[] args) {
        return args.length == 0 ? getMatches(((String) null),list) : getMatches(args[args.length - 1],list);
    }

    public static List<String> getMatches(List<String> list,String value) {
        return getMatches(value,list);
    }

    public static List<String> getMatches(String value,List<String> list) {
        if (value == null || value.isEmpty()){
            return list.size() > completionLengthLimit ? list.subList(0,completionLengthLimit) : list;//限制list大小
        }
        List<String> result = new ArrayList<>(Math.min(list.size(),completionLengthLimit));
        for (String enter : list) {
            if (result.size() >= completionLengthLimit) break;
            if (enter.intern().toLowerCase().startsWith(value.toLowerCase())){
                result.add(enter);
            }
        }
        return result;
    }

    @Deprecated
    public static List<String> PlayersList(String[] arg) {
        return getPlayersList(arg);
    }

    //获取玩家列表,根据arg筛选
    public static List<String> getPlayersList() {
        Collection<? extends Player> collection = Bukkit.getOnlinePlayers();
        List<String> players = new ArrayList<>(collection.size());
        for (Player p : collection) players.add(p.getName());
        return players;
    }

    //获取玩家列表,根据args筛选
    public static List<String> getPlayersList(String[] args) {
        return getMatches(getPlayersList(),args);
    }

    //获取玩家列表，根据args筛选，排除自己
    public static List<String> getPlayersList(String[] args,CommandSender sender) {
        List<String> list = getPlayersList(args);
        list.remove(sender.getName());
        return list;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,Command cmd,String label,String[] args) {
        return getPlayersList(args);
    }

    //获取指令名称
    public String getName() {
        return getClass().getSimpleName().toLowerCase();
    }

    //获取指令介绍
    public String getDescription() {
        return "";
    }

    //发送者是否可以使用指令
    public boolean canUseCommand(CommandSender sender) {
        return sender.hasPermission("bnes.command." + getClass().getSimpleName());
    }
}
