package com.darksoldier1404.dppc.api.inventory;

import com.darksoldier1404.dppc.support.PluginTest;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the builder / accessor / session logic of {@link DAnvilInventory} that
 * does not require actually opening an anvil menu.
 */
class DAnvilInventoryTest extends PluginTest {

    private DAnvilInventory anvil() {
        return new DAnvilInventory("Title", plugin);
    }

    @Test
    void builderStoresTitleAndPlugin() {
        DAnvilInventory a = anvil().title("New");
        assertEquals("New", a.getTitle());
        assertSame(plugin, a.getPlugin());
        assertTrue(a.isValidHandler(plugin));
    }

    @Test
    void setItemAndGetItem() {
        DAnvilInventory a = anvil();
        ItemStack item = new ItemStack(Material.GREEN_WOOL);
        a.setItem(2, item);
        assertEquals(item, a.getItem(2));
        assertNull(a.getItem(0));
    }

    @Test
    void slotOutOfRangeThrows() {
        DAnvilInventory a = anvil();
        assertThrows(IllegalArgumentException.class, () -> a.setItem(3, new ItemStack(Material.STONE)));
        assertThrows(IllegalArgumentException.class, () -> a.getItem(-1));
    }

    @Test
    void renameTextDefaultsAndCaching() {
        DAnvilInventory a = anvil();
        // No viewer yet -> returns the cached value (empty by default).
        assertEquals("", a.getRenameText());
        a.setRenameText("typed");
        assertEquals("typed", a.getRenameText());
        a.setRenameText(null);
        assertEquals("", a.getRenameText());
    }

    @Test
    void newPromptIsActiveUntilHandleClose() {
        DAnvilInventory a = anvil();
        assertTrue(a.isActive());
        assertNull(a.getViewer());
        a.handleClose();
        assertFalse(a.isActive());
    }

    @Test
    void staticSessionLookupHandlesNull() {
        assertNull(DAnvilInventory.getOpen(null));
        assertFalse(DAnvilInventory.isOpen(null));
    }

    @Test
    void sizeConstantIsThree() {
        assertEquals(3, DAnvilInventory.SIZE);
    }
}
