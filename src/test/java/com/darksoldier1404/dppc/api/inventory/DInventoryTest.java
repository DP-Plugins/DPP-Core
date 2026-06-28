package com.darksoldier1404.dppc.api.inventory;

import com.darksoldier1404.dppc.support.PluginTest;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DInventoryTest extends PluginTest {

    private DInventory simple() {
        return new DInventory("Title", 27, plugin);
    }

    @Test
    void basicProperties() {
        DInventory inv = simple();
        assertEquals(27, inv.getSize());
        assertEquals("Title", inv.getTitle());
        assertEquals(plugin.getName(), inv.getHandlerName());
        assertNotNull(inv.getUniqueId());
        assertFalse(inv.isUsePage());
        assertTrue(inv.isValidHandler(plugin));
    }

    @Test
    void itemDelegation() {
        DInventory inv = simple();
        inv.setItem(0, new ItemStack(Material.DIAMOND, 3));
        assertEquals(Material.DIAMOND, inv.getItem(0).getType());
        assertTrue(inv.contains(Material.DIAMOND));
        assertTrue(inv.contains(Material.DIAMOND, 3));
        assertEquals(1, inv.firstEmpty());
        inv.clear();
        assertFalse(inv.contains(Material.DIAMOND));
    }

    @Test
    void pagedConstructorComputesToolSlots() {
        DInventory paged = new DInventory("P", 18, true, plugin);
        assertTrue(paged.isUsePage());
        assertTrue(paged.isUsePageTools());
    }

    @Test
    void addPageItemFillsAcrossPages() {
        // size 18 paged -> 9 content slots per page
        DInventory paged = new DInventory("P", 18, true, plugin);
        for (int i = 0; i < 10; i++) {
            paged.addPageItem(new ItemStack(Material.STONE));
        }
        assertEquals(2, paged.getPages());
        assertEquals(10, paged.getAllPageItems().size());
    }

    @Test
    void setPageItemRespectsBounds() {
        DInventory paged = new DInventory("P", 18, true, plugin);
        // 9 content slots -> valid 0..8
        assertTrue(paged.setPageItem(8, new ItemStack(Material.STONE)));
        assertFalse(paged.setPageItem(9, new ItemStack(Material.STONE)));
        assertFalse(paged.setPageItem(-1, new ItemStack(Material.STONE)));
    }

    @Test
    void pageNavigation() {
        DInventory paged = new DInventory("P", 27, true, plugin);
        paged.setPages(3);
        assertEquals(0, paged.getCurrentPage());
        assertTrue(paged.nextPage());
        assertEquals(1, paged.getCurrentPage());
        assertTrue(paged.prevPage());
        assertEquals(0, paged.getCurrentPage());
        assertFalse(paged.prevPage());
        assertTrue(paged.turnPage(2));
        assertEquals(2, paged.getCurrentPage());
        assertFalse(paged.turnPage(-1));
    }

    @Test
    void channelAndObjAccessors() {
        DInventory inv = simple();
        inv.setChannel(7);
        assertEquals(7, inv.getChannel());
        assertTrue(inv.isValidChannel(7));
        assertFalse(inv.isValidChannel(8));
        inv.setObj("payload");
        assertEquals("payload", inv.getObj());
        inv.setName("myname");
        assertEquals("myname", inv.getName());
    }

    @Test
    void equalsAndHashCodeByUuid() {
        DInventory a = simple();
        DInventory b = simple();
        assertNotEquals(a, b);
        assertEquals(a, a);
        assertEquals(a.hashCode(), a.getUniqueId().hashCode());
    }

    @Test
    void cloneIsIndependentWithNewUuid() {
        DInventory original = simple();
        original.setItem(0, new ItemStack(Material.DIAMOND));
        DInventory clone = original.clone();
        assertNotEquals(original.getUniqueId(), clone.getUniqueId());
        assertEquals(original.getSize(), clone.getSize());
        // Mutating the clone must not affect the original.
        clone.setItem(1, new ItemStack(Material.GOLD_INGOT));
        assertNull(original.getItem(1));
    }

    @Test
    void setTitlePreservesContents() {
        DInventory inv = simple();
        inv.setItem(0, new ItemStack(Material.DIAMOND));
        inv.setTitle("New");
        assertEquals("New", inv.getTitle());
        assertEquals(Material.DIAMOND, inv.getItem(0).getType());
    }

    @Test
    void base64ObjectRoundTrip() {
        ArrayList<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        String encoded = DInventory.encodeObjectToBase64(list);
        assertNotNull(encoded);
        Object decoded = DInventory.decodeObjectFromBase64(encoded);
        assertEquals(list, decoded);
    }

    @Test
    void base64NullHandling() {
        assertNull(DInventory.encodeObjectToBase64(null));
        assertNull(DInventory.decodeObjectFromBase64(null));
    }

    @Test
    void pageItemSetAccessors() {
        ItemStack item = new ItemStack(Material.STONE);
        DInventory.PageItemSet set = new DInventory.PageItemSet(1, 2, item);
        assertEquals(1, set.getPage());
        assertEquals(2, set.getSlot());
        assertEquals(item, set.getItem());
        ItemStack other = new ItemStack(Material.DIRT);
        set.setItem(other);
        assertEquals(other, set.getItem());
    }
}
