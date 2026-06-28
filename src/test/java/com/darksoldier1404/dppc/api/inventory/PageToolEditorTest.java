package com.darksoldier1404.dppc.api.inventory;

import com.darksoldier1404.dppc.support.PluginTest;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PageToolEditorTest extends PluginTest {

    @Test
    void seedsTopRowAndFunctionButtons() {
        PageToolEditor editor = new PageToolEditor();
        assertEquals(27, editor.getInventory().getSize());
        // Default seed: prev=1, current=4, next=7 in the top row.
        assertNotNull(editor.getInventory().getItem(1));
        assertNotNull(editor.getInventory().getItem(4));
        assertNotNull(editor.getInventory().getItem(7));
        // A function button sits directly below each top slot.
        for (int i = 0; i < DefaultPageTools.SLOTS; i++) {
            assertNotNull(editor.getInventory().getItem(DefaultPageTools.SLOTS + i), "button " + i);
        }
        // Controls.
        assertNotNull(editor.getInventory().getItem(PageToolEditor.SAVE_SLOT));
        assertNotNull(editor.getInventory().getItem(PageToolEditor.RESET_SLOT));
        assertNotNull(editor.getInventory().getItem(PageToolEditor.CANCEL_SLOT));
    }

    @Test
    void cycleRoleAdvancesAndIsReflectedInLayout() {
        PageToolEditor editor = new PageToolEditor();
        // Slot 0 starts as DECORATION in the default seed.
        assertEquals(PageToolRole.DECORATION, editor.toLayout().getRole(0));
        editor.cycleRole(0);
        assertEquals(PageToolRole.PREV, editor.toLayout().getRole(0));
        editor.cycleRole(0);
        assertEquals(PageToolRole.NEXT, editor.toLayout().getRole(0));
    }

    @Test
    void toLayoutCapturesPlacedItems() {
        PageToolEditor editor = new PageToolEditor();
        editor.getInventory().setItem(0, new ItemStack(Material.DIAMOND));
        DefaultPageTools.Layout layout = editor.toLayout();
        assertNotNull(layout.getItem(0));
        assertEquals(Material.DIAMOND, layout.getItem(0).getType());
    }

    @Test
    void resetRestoresDefaultArrangement() {
        PageToolEditor editor = new PageToolEditor();
        editor.cycleRole(0);
        assertEquals(PageToolRole.PREV, editor.toLayout().getRole(0));
        editor.resetToDefault();
        assertEquals(PageToolRole.DECORATION, editor.toLayout().getRole(0));
        assertEquals(PageToolRole.PREV, editor.toLayout().getRole(1));
        assertEquals(PageToolRole.NEXT, editor.toLayout().getRole(7));
    }

    @Test
    void savedLayoutIsPickedUpByLoad() {
        PageToolEditor editor = new PageToolEditor();
        editor.cycleRole(0); // slot 0 -> PREV
        DefaultPageTools.save(plugin.getConfig(), editor.toLayout());

        DefaultPageTools.Layout reloaded = DefaultPageTools.load(plugin.getConfig());
        assertNotNull(reloaded);
        assertEquals(PageToolRole.PREV, reloaded.getRole(0));
    }
}
