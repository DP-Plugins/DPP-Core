package com.darksoldier1404.dppc.nbt;

import com.darksoldier1404.dppc.support.MockServerTest;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NBTItemTest extends MockServerTest {

    private NBTItem nbt() {
        return new NBTItem(new ItemStack(Material.STONE));
    }

    @Test
    void stringRoundTrip() {
        NBTItem nbt = nbt();
        nbt.setString("name", "value");
        assertEquals("value", nbt.getString("name"));
    }

    @Test
    void numericRoundTrips() {
        NBTItem nbt = nbt();
        nbt.setInteger("i", 7);
        nbt.setLong("l", 9L);
        nbt.setShort("s", (short) 3);
        nbt.setByte("b", (byte) 2);
        nbt.setFloat("f", 1.5f);
        nbt.setDouble("d", 2.5d);
        assertEquals(7, nbt.getInteger("i"));
        assertEquals(9L, nbt.getLong("l"));
        assertEquals((short) 3, nbt.getShort("s"));
        assertEquals((byte) 2, nbt.getByte("b"));
        assertEquals(1.5f, nbt.getFloat("f"));
        assertEquals(2.5d, nbt.getDouble("d"));
    }

    @Test
    void booleanRoundTrip() {
        NBTItem nbt = nbt();
        nbt.setBoolean("flag", true);
        assertTrue(nbt.getBoolean("flag"));
        nbt.setBoolean("flag", false);
        assertFalse(nbt.getBoolean("flag"));
    }

    @Test
    void arrayRoundTrips() {
        NBTItem nbt = nbt();
        nbt.setIntArray("ia", new int[]{1, 2, 3});
        nbt.setByteArray("ba", new byte[]{4, 5});
        assertArrayEquals(new int[]{1, 2, 3}, nbt.getIntArray("ia"));
        assertArrayEquals(new byte[]{4, 5}, nbt.getByteArray("ba"));
    }

    @Test
    void absentNumericReturnsZero() {
        assertEquals(0, nbt().getInteger("missing"));
    }

    @Test
    void hasTagAndRemoveKey() {
        NBTItem nbt = nbt();
        nbt.setString("k", "v");
        assertTrue(nbt.hasTag("k"));
        nbt.removeKey("k");
        assertFalse(nbt.hasTag("k"));
    }

    @Test
    void getKeysReturnsLowercasedNamespaceKeys() {
        NBTItem nbt = nbt();
        nbt.setString("Alpha", "1");
        nbt.setInteger("beta", 2);
        // Keys are lowercased by NBTItem#key.
        assertTrue(nbt.getKeys().contains("alpha"));
        assertTrue(nbt.getKeys().contains("beta"));
        assertEquals(2, nbt.getKeys().size());
    }
}
