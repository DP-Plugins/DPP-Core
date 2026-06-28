package com.darksoldier1404.dppc.pdc;

import com.darksoldier1404.dppc.support.MockServerTest;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PDCModifierTest extends MockServerTest {

    private ItemMeta meta() {
        return new ItemStack(Material.STONE).getItemMeta();
    }

    private NamespacedKey key(String k) {
        return NamespacedKey.minecraft(k);
    }

    @Test
    void setAndGetPrimitiveValue() {
        ItemMeta meta = meta();
        PDCModifier.setValue(meta, key("name"), PersistentDataType.STRING, "value");
        assertEquals("value", PDCModifier.getValue(meta, key("name"), PersistentDataType.STRING, "def"));
    }

    @Test
    void getValueReturnsDefaultWhenAbsent() {
        assertEquals("def", PDCModifier.getValue(meta(), key("missing"), PersistentDataType.STRING, "def"));
        assertEquals(5, PDCModifier.getValue(meta(), key("missing"), PersistentDataType.INTEGER, 5));
    }

    @Test
    void objectRoundTrip() throws Exception {
        ItemMeta meta = meta();
        ArrayList<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        PDCModifier.setObject(meta, key("obj"), list);

        Optional<ArrayList> restored = PDCModifier.getObject(meta, key("obj"), ArrayList.class);
        assertTrue(restored.isPresent());
        assertEquals(list, restored.get());
    }

    @Test
    void getObjectEmptyWhenAbsent() {
        assertFalse(PDCModifier.getObject(meta(), key("none"), ArrayList.class).isPresent());
    }

    @Test
    void hasDataAndRemoveData() throws Exception {
        ItemMeta meta = meta();
        PDCModifier.setObject(meta, key("obj"), "payload");
        assertTrue(PDCModifier.hasData(meta, key("obj")));
        PDCModifier.removeData(meta, key("obj"));
        assertFalse(PDCModifier.hasData(meta, key("obj")));
    }
}
