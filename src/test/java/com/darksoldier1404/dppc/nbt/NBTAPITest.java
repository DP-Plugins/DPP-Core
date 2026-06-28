package com.darksoldier1404.dppc.nbt;

import com.darksoldier1404.dppc.support.MockServerTest;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NBTAPITest extends MockServerTest {

    @Test
    void modifyNullItemReturnsNull() {
        assertNull(NBTAPI.modify(null, nbt -> nbt.setString("k", "v")));
    }

    @Test
    void getNullItemReturnsNull() {
        assertNull(NBTAPI.get(null, nbt -> nbt.getString("k")));
    }

    @Test
    void modifyThenGet() {
        ItemStack item = new ItemStack(Material.STONE);
        ItemStack modified = NBTAPI.modify(item, nbt -> nbt.setString("k", "v"));
        assertEquals("v", NBTAPI.get(modified, nbt -> nbt.getString("k")));
    }
}
