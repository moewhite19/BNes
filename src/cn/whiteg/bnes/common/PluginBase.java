package cn.whiteg.bnes.common;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class PluginBase extends JavaPlugin {
    private final List<Listener> listenerList = new LinkedList<>();

    public void regListener(Listener listener) {
        listenerList.add(listener);
        Bukkit.getPluginManager().registerEvents(listener,this);
    }

    public void regListener(String key,Listener listener) {
        getLogger().info("注册事件:" + key);
        Bukkit.getPluginManager().registerEvents(listener,this);
    }

    public void unregListener() {
        try{
            for (Listener listener : listenerList) {
                unregListener(listener);
            }
            listenerList.clear();
        }catch (ConcurrentModificationException ignored){
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 卸载事件
     *
     * @param listener 事件对象
     */
    public void unregListener(Listener listener) {
        if (listenerList.remove(listener)) HandlerList.unregisterAll(listener);

    }

    public List<Listener> getListenerList() {
        return listenerList;
    }
}
