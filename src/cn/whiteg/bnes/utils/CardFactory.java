package cn.whiteg.bnes.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CardFactory {
    final Material CARD_ITEM;
    private final File dir;

    public CardFactory(File dir,Material card_item) {
        this.dir = dir;
        CARD_ITEM = card_item;
    }

    public ItemStack makeCard(String fileName) {
        ItemStack item = new ItemStack(CARD_ITEM);
        String name = removeFormat(fileName);
        ItemMeta itemMeta = item.getItemMeta();
        //noinspection ConstantConditions
        itemMeta.setDisplayName(name);
        itemMeta.setLore(List.of(fileName));
        itemMeta.setCustomModelData(1);
        item.setItemMeta(itemMeta);
        return item;
    }

    public File readCard(ItemStack itemStack) {
        if (itemStack.getType() != CARD_ITEM || !itemStack.hasItemMeta()) return null;
        ItemMeta itemMeta = itemStack.getItemMeta();
        List<String> lore = itemMeta.getLore();
        if (lore == null || lore.size() > 1) return null;
        return new File(dir,lore.get(0));
    }

    public boolean isCard(ItemStack itemStack) {
        if (itemStack != null && itemStack.getType() == CARD_ITEM && itemStack.hasItemMeta()){
            ItemMeta meta = itemStack.getItemMeta();
            if (meta.hasCustomModelData()) return meta.getCustomModelData() == 1;
        }
        return false;
    }

    public String removeFormat(String fileName) {
        int index = fileName.indexOf('.');
        if (index == -1)
            return fileName;
        return fileName.substring(0,index);
    }

    public int getCardNumber() {
        if (!dir.exists() || !dir.isDirectory()) return 0;
        String[] list = dir.list();
        if (list == null) return 0;
        int i = 0;
        for (String s : list) {
            if (s.toLowerCase().endsWith(".nes")) i++;
        }
        return i;
    }

    public List<ItemStack> getCardItems(int page,int number) {
        if (!dir.exists() || !dir.isDirectory()) return Collections.emptyList();
        var files = dir.list();
        if (files == null) return Collections.emptyList();
        int mod = page * number;
        if (mod > files.length) return Collections.emptyList();
        var list = new ArrayList<ItemStack>(Math.min(number,files.length));
        int max = Math.min(mod + number,files.length);
        for (int i = mod; i < max; i++) {
            String file = files[i];
            if (file.intern().toLowerCase().endsWith(".nes")) list.add(makeCard(file));
        }
        return list;
    }

}
