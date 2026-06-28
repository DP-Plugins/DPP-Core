package com.darksoldier1404.dppc.builder.itemstack;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import com.darksoldier1404.dppc.utils.ColorUtils;
import com.google.common.annotations.Beta;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.*;

@DPPCoreVersion(since = "5.3.1")
public class ItemStackBuilder {
    private final ItemStack itemStack;

    private ItemStackBuilder(Material material) {
        this.itemStack = new ItemStack(material);
    }

    private ItemStackBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
    }

    public static ItemStackBuilder of(Material material) {
        return new ItemStackBuilder(material);
    }

    public static ItemStackBuilder from(ItemStack itemStack) {
        return new ItemStackBuilder(itemStack);
    }

    public ItemStackBuilder amount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    public ItemStackBuilder name(String name) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.applyColor(name));
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemStackBuilder lore(List<String> lore) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            List<String> temp = new ArrayList<>();
            for (String s : lore) {
                temp.add(ColorUtils.applyColor(s));
            }
            meta.setLore(temp);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemStackBuilder lore(String... lore) {
        return lore(Arrays.asList(lore));
    }

    public ItemStackBuilder enchant(Enchantment enchantment, int level) {
        itemStack.addUnsafeEnchantment(enchantment, level);
        return this;
    }

    public ItemStackBuilder enchants(Map<Enchantment, Integer> enchants) {
        enchants.forEach(this::enchant);
        return this;
    }

    public ItemStackBuilder itemMeta(ItemMeta meta) {
        itemStack.setItemMeta(meta);
        return this;
    }

    public ItemStackBuilder addFlags(ItemFlag... flags) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(flags);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemStackBuilder removeFlags(ItemFlag... flags) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.removeItemFlags(flags);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemStackBuilder unbreakable(boolean unbreakable) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(unbreakable);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemStackBuilder customModelData(int data) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(data);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemStackBuilder damage(int damage) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta instanceof Damageable) {
            Damageable dmg = (Damageable) meta;
            dmg.setDamage(damage);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemStackBuilder color(Color color) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta instanceof LeatherArmorMeta) {
            LeatherArmorMeta leatherMeta = (LeatherArmorMeta) meta;
            leatherMeta.setColor(color);
            itemStack.setItemMeta(leatherMeta);
        }
        return this;
    }

    public ItemStackBuilder removeEnchant(Enchantment enchantment) {
        itemStack.removeEnchantment(enchantment);
        return this;
    }

    public ItemStackBuilder clearEnchants() {
        for (Enchantment e : itemStack.getEnchantments().keySet()) {
            itemStack.removeEnchantment(e);
        }
        return this;
    }

    public ItemStackBuilder clearLore() {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setLore(new ArrayList<>());
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public boolean isEmpty() {
        return itemStack.getType() == Material.AIR || itemStack.getAmount() <= 0;
    }

    public ItemMeta getMeta() {
        return itemStack.getItemMeta();
    }

    public ItemStack build() {
        return itemStack;
    }
}
