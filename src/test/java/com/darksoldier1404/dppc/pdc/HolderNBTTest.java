package com.darksoldier1404.dppc.pdc;

import com.darksoldier1404.dppc.support.MockServerTest;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link HolderNBT} against an {@link ItemMeta}, which is a
 * {@code PersistentDataHolder}.
 */
class HolderNBTTest extends MockServerTest {

    private ItemMeta meta() {
        return new ItemStack(Material.STONE).getItemMeta();
    }

    @Test
    void primitiveRoundTrips() {
        ItemMeta meta = meta();
        HolderNBT.setStringTag(meta, "s", "value");
        HolderNBT.setIntTag(meta, "i", 5);
        HolderNBT.setBooleanTag(meta, "b", true);
        HolderNBT.setDoubleTag(meta, "d", 2.5);
        assertEquals("value", HolderNBT.getStringTag(meta, "s"));
        assertEquals(5, HolderNBT.getIntegerTag(meta, "i"));
        assertTrue(HolderNBT.getBooleanTag(meta, "b"));
        assertEquals(2.5, HolderNBT.getDoubleTag(meta, "d"));
    }

    @Test
    void materialAndEntityTypeRoundTrips() {
        ItemMeta meta = meta();
        HolderNBT.setMaterialTag(meta, "m", Material.DIAMOND);
        HolderNBT.setEntityTypeTag(meta, "e", EntityType.ZOMBIE);
        assertEquals(Material.DIAMOND, HolderNBT.getMaterialTag(meta, "m"));
        assertEquals(EntityType.ZOMBIE, HolderNBT.getEntityTypeTag(meta, "e"));
    }

    @Test
    void serializableRoundTrip() throws Exception {
        ItemMeta meta = meta();
        ArrayList<String> list = new ArrayList<>();
        list.add("x");
        HolderNBT.setSerializableTag(meta, "list", list);
        Optional<ArrayList> restored = HolderNBT.getSerializableTag(meta, "list", ArrayList.class);
        assertTrue(restored.isPresent());
        assertEquals(list, restored.get());
    }

    @Test
    void missingPrimitiveReturnsDefault() {
        assertEquals(0, HolderNBT.getIntegerTag(meta(), "missing"));
        assertEquals(null, HolderNBT.getStringTag(meta(), "missing"));
    }

    @Test
    void hasTagKeyRemoveAndKeys() {
        ItemMeta meta = meta();
        HolderNBT.setStringTag(meta, "Alpha", "1");
        HolderNBT.setStringTag(meta, "beta", "2");
        assertTrue(HolderNBT.hasTagKey(meta, "Alpha"));
        // keys are stored lowercased
        assertTrue(HolderNBT.getKeys(meta).contains("alpha"));
        assertEquals(2, HolderNBT.getKeys(meta).size());

        HolderNBT.removeTag(meta, "Alpha");
        assertFalse(HolderNBT.hasTagKey(meta, "Alpha"));
        assertEquals("2", HolderNBT.getAllStringTag(meta).get("beta"));

        HolderNBT.removeAllTags(meta);
        assertTrue(HolderNBT.getKeys(meta).isEmpty());
    }

    @Test
    void nullHolderIsSafe() {
        assertFalse(HolderNBT.hasTagKey(null, "k"));
        assertTrue(HolderNBT.getKeys(null).isEmpty());
    }
}
