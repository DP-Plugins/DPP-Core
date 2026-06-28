package com.darksoldier1404.dppc.events;

import com.darksoldier1404.dppc.DPPCore;
import com.darksoldier1404.dppc.api.inventory.DInventory;
import com.darksoldier1404.dppc.plugin.functions.DPPCPFunction;
import com.darksoldier1404.dppc.plugin.functions.UpdateStatus;
import com.darksoldier1404.dppc.utils.NBT;
import com.darksoldier1404.dppc.utils.PluginUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Routes clicks inside the {@code /dppcp} information panels.
 *
 * <p>The panels are read-only: every click is cancelled. In the list, left-clicking a plugin opens
 * its detail panel and right-clicking checks it for updates; the paging row also carries a
 * "Check All Updates" button. In a detail panel, the back button returns to the list and the
 * "Check for updates" button refreshes that plugin. Paging arrow clicks are handled by
 * {@link InventoryEventListener}, so they are left untouched here.</p>
 */
public class DPPCPPanelListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof DInventory)) return;
        DInventory inv = (DInventory) e.getInventory().getHolder();
        String marker = inv.getName();
        boolean isList = DPPCPFunction.LIST_MARKER.equals(marker);
        boolean isDetail = DPPCPFunction.DETAIL_MARKER.equals(marker);
        if (!isList && !isDetail) return;

        // Read-only panels: never let items be moved out or in.
        e.setCancelled(true);

        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType().isAir()) return;
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        if (isList) {
            if (NBT.hasTagKey(item, DPPCPFunction.OPEN_TAG)) {
                String name = NBT.getStringTag(item, DPPCPFunction.OPEN_TAG);
                if (e.isRightClick()) {
                    checkOne(p, name, false);
                } else {
                    JavaPlugin plugin = PluginUtil.getPluginByName(name);
                    if (plugin == null) {
                        p.sendMessage(prefix() + "§cPlugin not found: " + name);
                        return;
                    }
                    DPPCPFunction.openPluginDetailGUI(p, plugin);
                }
                return;
            }
            if (NBT.hasTagKey(item, DPPCPFunction.CHECK_ALL_TAG)) {
                p.sendMessage(prefix() + "§7Checking all plugins for updates...");
                PluginUtil.checkAllUpdatesAsync(() -> {
                    if (!p.isOnline()) return;
                    DPPCPFunction.refreshOpenList(p);
                    p.sendMessage(prefix() + "§aUpdate check complete.");
                });
            }
            return;
        }

        // Detail panel.
        if (NBT.hasTagKey(item, DPPCPFunction.BACK_TAG)) {
            DPPCPFunction.openDPPListGUI(p);
            return;
        }
        if (NBT.hasTagKey(item, DPPCPFunction.CHECK_ONE_TAG)) {
            checkOne(p, NBT.getStringTag(item, DPPCPFunction.CHECK_ONE_TAG), true);
        }
    }

    /**
     * Asynchronously checks one plugin and, once done, refreshes whichever panel the player is still
     * viewing and reports the resulting status.
     *
     * @param reopenDetail when true the detail panel for the plugin is reopened; otherwise the open
     *                     list is refreshed in place.
     */
    private void checkOne(Player p, String name, boolean reopenDetail) {
        p.sendMessage(prefix() + "§7Checking updates for §b" + name + "§7...");
        PluginUtil.checkUpdateAsync(name, () -> {
            if (!p.isOnline()) return;
            JavaPlugin plugin = PluginUtil.getPluginByName(name);
            if (reopenDetail) {
                if (plugin != null && DPPCPFunction.DETAIL_MARKER.equals(markerOf(p))) {
                    DPPCPFunction.openPluginDetailGUI(p, plugin);
                }
            } else {
                DPPCPFunction.refreshOpenList(p);
            }
            if (plugin != null) {
                p.sendMessage(prefix() + "§e" + name + " §f| " + statusText(plugin));
                p.sendMessage(prefix() + "§fGitHub: §e" + PluginUtil.getGithubUrl(name));
            }
        });
    }

    private static String statusText(JavaPlugin plugin) {
        UpdateStatus status = DPPCPFunction.getUpdateStatus(plugin);
        String latest = PluginUtil.getCachedLatestVersion(plugin.getName());
        switch (status) {
            case UP_TO_DATE:
                return "§aUp to date §f(§a" + plugin.getDescription().getVersion() + "§f)";
            case OUTDATED:
                return "§cOutdated §f| latest: §e" + latest;
            case UNKNOWN:
            default:
                return "§7Could not verify the latest version.";
        }
    }

    private static String markerOf(Player p) {
        org.bukkit.inventory.InventoryHolder holder = p.getOpenInventory().getTopInventory().getHolder();
        return holder instanceof DInventory ? ((DInventory) holder).getName() : null;
    }

    private static String prefix() {
        return DPPCore.getInstance().getPrefix();
    }
}
