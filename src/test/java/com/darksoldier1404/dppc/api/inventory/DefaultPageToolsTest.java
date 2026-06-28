package com.darksoldier1404.dppc.api.inventory;

import com.darksoldier1404.dppc.support.MockServerTest;
import com.darksoldier1404.dppc.utils.NBT;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPageToolsTest extends MockServerTest {

    private ItemStack named(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    @Test
    void roleCyclesThroughAllValues() {
        assertEquals(PageToolRole.PREV, PageToolRole.DECORATION.next());
        assertEquals(PageToolRole.NEXT, PageToolRole.PREV.next());
        assertEquals(PageToolRole.CURRENT, PageToolRole.NEXT.next());
        assertEquals(PageToolRole.DECORATION, PageToolRole.CURRENT.next());
    }

    @Test
    void roleFromStringFallsBackToDecoration() {
        assertEquals(PageToolRole.NEXT, PageToolRole.fromString("NEXT"));
        assertEquals(PageToolRole.DECORATION, PageToolRole.fromString("garbage"));
        assertEquals(PageToolRole.DECORATION, PageToolRole.fromString(null));
    }

    @Test
    void tagForDecorationOnlyCancelsClicks() {
        ItemStack tagged = DefaultPageTools.tagFor(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), PageToolRole.DECORATION, 0, 0);
        assertTrue(NBT.hasTagKey(tagged, "dppc_clickcancel"));
        assertFalse(NBT.hasTagKey(tagged, "dppc_nextpage"));
        assertFalse(NBT.hasTagKey(tagged, "dppc_prevpage"));
        assertFalse(NBT.hasTagKey(tagged, "dppc_currentpage"));
    }

    @Test
    void tagForPrevAndNextApplyNavigationTags() {
        ItemStack prev = DefaultPageTools.tagFor(new ItemStack(Material.ARROW), PageToolRole.PREV, 0, 0);
        assertTrue(NBT.hasTagKey(prev, "dppc_clickcancel"));
        assertTrue(NBT.hasTagKey(prev, "dppc_prevpage"));

        ItemStack next = DefaultPageTools.tagFor(new ItemStack(Material.ARROW), PageToolRole.NEXT, 0, 0);
        assertTrue(NBT.hasTagKey(next, "dppc_clickcancel"));
        assertTrue(NBT.hasTagKey(next, "dppc_nextpage"));
    }

    @Test
    void tagForCurrentSubstitutesPlaceholders() {
        ItemStack raw = named(Material.PAPER, "Page {current} / {total}");
        ItemStack current = DefaultPageTools.tagFor(raw, PageToolRole.CURRENT, 2, 4);
        assertTrue(NBT.hasTagKey(current, "dppc_currentpage"));
        // currentPage 2 -> displayed 3, pages 4 -> displayed 5
        assertEquals("Page 3 / 5", current.getItemMeta().getDisplayName());
    }

    @Test
    void tagForDoesNotMutateInput() {
        ItemStack raw = new ItemStack(Material.ARROW);
        DefaultPageTools.tagFor(raw, PageToolRole.NEXT, 0, 0);
        assertFalse(NBT.hasTagKey(raw, "dppc_nextpage"));
    }

    @Test
    void defaultSeedReproducesClassicArrangement() {
        YamlConfiguration config = new YamlConfiguration();
        DefaultPageTools.Layout layout = DefaultPageTools.defaultSeed(config);
        assertEquals(PageToolRole.PREV, layout.getRole(1));
        assertEquals(PageToolRole.CURRENT, layout.getRole(4));
        assertEquals(PageToolRole.NEXT, layout.getRole(7));
        for (int i : new int[]{0, 2, 3, 5, 6, 8}) {
            assertEquals(PageToolRole.DECORATION, layout.getRole(i), "slot " + i + " should be decoration");
        }
        assertNotNull(layout.getItem(1));
        assertNotNull(layout.getItem(4));
        assertNotNull(layout.getItem(7));
    }

    @Test
    void loadReturnsNullWhenSectionAbsent() {
        assertNull(DefaultPageTools.load(new YamlConfiguration()));
    }

    @Test
    void saveThenLoadRoundTripsItemsAndRoles() {
        YamlConfiguration config = new YamlConfiguration();
        DefaultPageTools.Layout layout = new DefaultPageTools.Layout();
        layout.setSlot(1, new ItemStack(Material.ARROW), PageToolRole.PREV);
        layout.setSlot(7, new ItemStack(Material.ARROW), PageToolRole.NEXT);

        DefaultPageTools.save(config, layout);
        DefaultPageTools.Layout loaded = DefaultPageTools.load(config);

        assertNotNull(loaded);
        assertEquals(PageToolRole.PREV, loaded.getRole(1));
        assertEquals(Material.ARROW, loaded.getItem(1).getType());
        assertEquals(PageToolRole.NEXT, loaded.getRole(7));
        assertNull(loaded.getItem(0));
    }

    @Test
    void renderProducesTaggedToolBar() {
        DefaultPageTools.Layout layout = DefaultPageTools.defaultSeed(new YamlConfiguration());
        ItemStack[] tools = DefaultPageTools.render(layout, 0, 0, 9);
        assertEquals(9, tools.length);
        assertTrue(NBT.hasTagKey(tools[1], "dppc_prevpage"));
        assertTrue(NBT.hasTagKey(tools[4], "dppc_currentpage"));
        assertTrue(NBT.hasTagKey(tools[7], "dppc_nextpage"));
        assertTrue(NBT.hasTagKey(tools[0], "dppc_clickcancel"));
    }
}
