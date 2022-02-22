package cn.whiteg.bnes.common;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PluginBase extends JavaPlugin {
    private final Map<String, Listener> listenerMap = new HashMap<>();

    public void regListener(Listener listener) {
        regListener(listener.getClass().getName(),listener);

    }

    public void regListener(String key,Listener listener) {
//        getLogger().info("注册事件:" + key);
        listenerMap.put(key,listener);
        Bukkit.getPluginManager().registerEvents(listener,this);

    }

    public void unregListener() {
        try{
            for (Map.Entry<String, Listener> entry : listenerMap.entrySet()) {
                unregListener(entry.getValue());
            }
            listenerMap.clear();
        }catch (ConcurrentModificationException ignored){
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
     * 卸载事件
     *
     * @param Key "卸载"
     */
    public boolean unregListener(String Key) {
        Listener listenr = listenerMap.remove(Key);
        if (listenr == null){
            return false;
        }
        unregListener(listenr);
        return true;
    }

    /**
     * 卸载事件
     *
     * @param listener 事件对象
     */
    public void unregListener(Listener listener) {
        //注销事件
        HandlerList.unregisterAll(listener);
        if (listenerMap.remove(listener.getClass().getName()) == null){
            //如果按照类名没搜索到对象则遍历
            Iterator<Map.Entry<String, Listener>> it = listenerMap.entrySet().iterator();
            while (it.hasNext()) {
                if (it.next() == listener){
                    it.remove();
                    break;
                }
            }
        }
        //调用类中的unreg()方法
        try{
            Class listenerClass = listener.getClass();
            Method unreg = listenerClass.getDeclaredMethod("unreg");
            unreg.setAccessible(true);
            unreg.invoke(listener);
        }catch (Exception ignored){
        }
    }

    public Map<String, Listener> getListenerMap() {
        return listenerMap;
    }
}
