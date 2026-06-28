package com.darksoldier1404.dppc.utils;

import com.darksoldier1404.dppc.support.MockServerTest;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryUtilsTest extends MockServerTest {

    private ItemStack[] emptyContent() {
        return new ItemStack[36];
    }

    private ItemStack[] fullOfStone() {
        ItemStack[] content = new ItemStack[36];
        Arrays.fill(content, new ItemStack(Material.STONE, 64));
        return content;
    }

    @Test
    void hasEnoughSpaceInEmptyInventory() {
        assertTrue(InventoryUtils.hasEnoughSpace(emptyContent(), new ItemStack(Material.DIAMOND, 1)));
    }

    @Test
    void noSpaceInFullInventory() {
        assertFalse(InventoryUtils.hasEnoughSpace(fullOfStone(), new ItemStack(Material.DIAMOND, 1)));
    }

    @Test
    void nullItemHasNoSpace() {
        assertFalse(InventoryUtils.hasEnoughSpace(emptyContent(), (ItemStack) null));
    }

    @Test
    void varargsVariant() {
        assertTrue(InventoryUtils.hasEnoughSpace(emptyContent(),
                new ItemStack(Material.DIAMOND), new ItemStack(Material.GOLD_INGOT)));
        assertFalse(InventoryUtils.hasEnoughSpace(emptyContent(), new ItemStack[]{null}));
    }

    @Test
    void listVariant() {
        assertTrue(InventoryUtils.hasEnoughSpace(emptyContent(),
                Collections.singletonList(new ItemStack(Material.DIAMOND))));
        assertFalse(InventoryUtils.hasEnoughSpace(emptyContent(), Collections.emptyList()));
    }

    @Test
    void countsSimilarItems() {
        ItemStack[] content = new ItemStack[36];
        content[0] = new ItemStack(Material.STONE, 64);
        content[1] = new ItemStack(Material.STONE, 10);
        content[2] = new ItemStack(Material.DIRT, 5);
        assertEquals(74, InventoryUtils.getSimlarItemCount(content, new ItemStack(Material.STONE)));
    }

    @Test
    void mergeItemRejectsNonPlayerInventory() {
        Inventory chest = Bukkit.createInventory(null, 27);
        assertFalse(InventoryUtils.mergeItem(chest, new ItemStack(Material.STONE)));
    }
}
