package com.darksoldier1404.dppc.plugin.functions;

import com.darksoldier1404.dppc.api.inventory.DInventory;
import com.darksoldier1404.dppc.support.PluginTest;
import com.darksoldier1404.dppc.utils.NBT;
import com.darksoldier1404.dppc.utils.PluginUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DPPCPFunctionTest extends PluginTest {

    private ItemStack listItemFor(DInventory list, String pluginName) {
        for (ItemStack item : list.getInventory().getContents()) {
            if (item != null && NBT.hasTagKey(item, DPPCPFunction.OPEN_TAG)
                    && pluginName.equals(NBT.getStringTag(item, DPPCPFunction.OPEN_TAG))) {
                return item;
            }
        }
        return null;
    }

    @Test
    void listInventoryIsMarkedAndContainsClickablePlugins() {
        DInventory list = DPPCPFunction.buildListInventory();
        assertEquals(DPPCPFunction.LIST_MARKER, list.getName());
        assertEquals(54, list.getSize());

        // The core registers itself, so at least one clickable plugin item must be present.
        boolean foundOpenable = false;
        for (ItemStack item : list.getInventory().getContents()) {
            if (item != null && NBT.hasTagKey(item, DPPCPFunction.OPEN_TAG)) {
                foundOpenable = true;
                break;
            }
        }
        assertTrue(foundOpenable, "list should contain at least one plugin item tagged OPEN_TAG");
    }

    @Test
    void detailInventoryIsMarkedAndHasBackButton() {
        DInventory detail = DPPCPFunction.buildDetailInventory(plugin);
        assertEquals(DPPCPFunction.DETAIL_MARKER, detail.getName());
        assertEquals(27, detail.getSize());

        ItemStack back = detail.getItem(22);
        assertNotNull(back);
        assertTrue(NBT.hasTagKey(back, DPPCPFunction.BACK_TAG), "slot 22 should be the back button");
        // Info items are present at the expected slots.
        assertNotNull(detail.getItem(4));
        assertNotNull(detail.getItem(10));
        assertNotNull(detail.getItem(13));
        assertNotNull(detail.getItem(16));
    }

    @Test
    void listItemWoolReflectsUpdateStatus() {
        String name = plugin.getName();
        String current = plugin.getDescription().getVersion();

        PluginUtil.cacheLatestVersion(name, current);
        assertEquals(Material.LIME_WOOL, listItemFor(DPPCPFunction.buildListInventory(), name).getType(),
                "same version should be green");

        PluginUtil.cacheLatestVersion(name, "999.0.0");
        assertEquals(Material.RED_WOOL, listItemFor(DPPCPFunction.buildListInventory(), name).getType(),
                "newer latest should be red");

        PluginUtil.cacheLatestVersion(name, "0.0.0");
        assertEquals(Material.GRAY_WOOL, listItemFor(DPPCPFunction.buildListInventory(), name).getType(),
                "unverified should be gray");
    }

    @Test
    void checkAllAndCheckOneButtonsArePresent() {
        DInventory list = DPPCPFunction.buildListInventory();
        // Check-all button lives in the paging row (content slots are 0..44, tools 45..53).
        boolean foundCheckAll = false;
        for (ItemStack item : list.getInventory().getContents()) {
            if (item != null && NBT.hasTagKey(item, DPPCPFunction.CHECK_ALL_TAG)) {
                foundCheckAll = true;
                break;
            }
        }
        assertTrue(foundCheckAll, "list paging row should contain the Check All Updates button");

        DInventory detail = DPPCPFunction.buildDetailInventory(plugin);
        ItemStack checkOne = detail.getItem(24);
        assertNotNull(checkOne);
        assertTrue(NBT.hasTagKey(checkOne, DPPCPFunction.CHECK_ONE_TAG), "detail should have a per-plugin check button");
    }

    @Test
    void githubUrlUsesPluginName() {
        assertEquals("https://github.com/DP-Plugins/DP-PlayerMarket", PluginUtil.getGithubUrl("DP-PlayerMarket"));
    }

    @Test
    void listItemCarriesPluginNameForRouting() {
        DInventory list = DPPCPFunction.buildListInventory();
        String openTarget = null;
        for (ItemStack item : list.getInventory().getContents()) {
            if (item != null && NBT.hasTagKey(item, DPPCPFunction.OPEN_TAG)) {
                openTarget = NBT.getStringTag(item, DPPCPFunction.OPEN_TAG);
                break;
            }
        }
        assertNotNull(openTarget);
        assertTrue(openTarget.length() > 0);
    }
}
