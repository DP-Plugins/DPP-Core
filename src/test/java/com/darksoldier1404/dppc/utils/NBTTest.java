package com.darksoldier1404.dppc.utils;

import com.darksoldier1404.dppc.support.MockServerTest;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the static {@code NBT} facade, which delegates to NBTAPI/NBTItem.
 */
class NBTTest extends MockServerTest {

    private ItemStack stone() {
        return new ItemStack(Material.STONE);
    }

    @Test
    void stringTagRoundTrip() {
        ItemStack item = NBT.setStringTag(stone(), "k", "v");
        assertEquals("v", NBT.getStringTag(item, "k"));
    }

    @Test
    void intTagRoundTrip() {
        ItemStack item = NBT.setIntTag(stone(), "n", 99);
        assertEquals(99, NBT.getIntegerTag(item, "n"));
    }

    @Test
    void booleanTagRoundTrip() {
        ItemStack item = NBT.setBooleanTag(stone(), "flag", true);
        assertTrue(NBT.getBooleanTag(item, "flag"));
    }

    @Test
    void materialTagRoundTrip() {
        ItemStack item = NBT.setMaterialTag(stone(), "mat", Material.DIAMOND);
        assertEquals(Material.DIAMOND, NBT.getMaterialTag(item, "mat"));
    }

    @Test
    void setMultipleStringTags() {
        ItemStack item = NBT.setStringTags(stone(), new String[]{"a", "b"}, new String[]{"1", "2"});
        assertEquals("1", NBT.getStringTag(item, "a"));
        assertEquals("2", NBT.getStringTag(item, "b"));
    }

    @Test
    void hasTagKeyReflectsPresence() {
        ItemStack item = NBT.setStringTag(stone(), "present", "x");
        assertTrue(NBT.hasTagKey(item, "present"));
        assertFalse(NBT.hasTagKey(item, "absent"));
        assertFalse(NBT.hasTagKey(null, "present"));
    }

    @Test
    void removeTagDeletesKey() {
        ItemStack item = NBT.setStringTag(stone(), "k", "v");
        item = NBT.removeTag(item, "k");
        assertFalse(NBT.hasTagKey(item, "k"));
    }

    @Test
    void getAllStringTagReturnsEntries() {
        ItemStack item = NBT.setStringTag(stone(), "a", "1");
        Map<String, String> all = NBT.getAllStringTag(item);
        assertEquals("1", all.get("a"));
    }
}
