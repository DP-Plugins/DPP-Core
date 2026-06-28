package com.darksoldier1404.dppc.builder.itemstack;

import com.darksoldier1404.dppc.support.MockServerTest;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemStackBuilderTest extends MockServerTest {

    @Test
    void ofCreatesItemOfMaterial() {
        ItemStack item = ItemStackBuilder.of(Material.DIAMOND_SWORD).build();
        assertEquals(Material.DIAMOND_SWORD, item.getType());
    }

    @Test
    void amountIsApplied() {
        ItemStack item = ItemStackBuilder.of(Material.STONE).amount(16).build();
        assertEquals(16, item.getAmount());
    }

    @Test
    void nameIsColorTranslated() {
        ItemStack item = ItemStackBuilder.of(Material.STONE).name("&aNamed").build();
        assertEquals("§aNamed", item.getItemMeta().getDisplayName());
    }

    @Test
    void loreVarargsAndListAreColorTranslated() {
        ItemStack item = ItemStackBuilder.of(Material.STONE).lore("&aline1", "&bline2").build();
        assertEquals(Arrays.asList("§aline1", "§bline2"), item.getItemMeta().getLore());
    }

    @Test
    void clearLoreRemovesLore() {
        ItemStack item = ItemStackBuilder.of(Material.STONE).lore("&aline1").clearLore().build();
        // Setting an empty lore list makes Bukkit drop the lore entirely.
        assertFalse(item.getItemMeta().hasLore());
    }

    @Test
    void unbreakableAndCustomModelData() {
        ItemStack item = ItemStackBuilder.of(Material.DIAMOND_PICKAXE)
                .unbreakable(true)
                .customModelData(7)
                .build();
        assertTrue(item.getItemMeta().isUnbreakable());
        assertTrue(item.getItemMeta().hasCustomModelData());
        assertEquals(7, item.getItemMeta().getCustomModelData());
    }

    @Test
    void isEmptyForAirOrZeroAmount() {
        assertTrue(ItemStackBuilder.of(Material.AIR).isEmpty());
        assertTrue(ItemStackBuilder.of(Material.STONE).amount(0).isEmpty());
        assertFalse(ItemStackBuilder.of(Material.STONE).amount(1).isEmpty());
    }

    @Test
    void fromClonesSourceItem() {
        ItemStack source = new ItemStack(Material.STONE, 1);
        ItemStack built = ItemStackBuilder.from(source).amount(5).build();
        // The builder works on a clone, so the source must be untouched.
        assertNotSame(source, built);
        assertEquals(1, source.getAmount());
        assertEquals(5, built.getAmount());
    }
}
