package com.darksoldier1404.dppc.plugin.functions;

import com.darksoldier1404.dppc.DPPCore;
import com.darksoldier1404.dppc.api.inventory.DInventory;
import com.darksoldier1404.dppc.utils.NBT;
import com.darksoldier1404.dppc.utils.PluginUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Builds the {@code /dppcp} information panel: a paged list of every plugin registered with the core
 * and, on click, a read-only detail panel for the selected plugin.
 *
 * <p>List entries are coloured wool reflecting their {@link UpdateStatus} (green = up to date,
 * red = outdated, gray = unverified). The panel offers a "Check All Updates" button in its paging
 * row and a per-plugin "Check for updates" button in each detail view; both refresh the cached
 * statuses via {@link PluginUtil}.</p>
 */
public class DPPCPFunction {
    private static final DPPCore instance = DPPCore.getInstance();

    /** {@link DInventory#getName()} marker identifying the plugin list panel. */
    public static final String LIST_MARKER = "dppcp_list";
    /** {@link DInventory#getName()} marker identifying a plugin detail panel. */
    public static final String DETAIL_MARKER = "dppcp_detail";
    /** NBT tag on a list item; its value is the plugin name to open in detail. */
    public static final String OPEN_TAG = "dppc_dppcp_open";
    /** NBT tag on the detail back button. */
    public static final String BACK_TAG = "dppc_dppcp_back";
    /** NBT tag on the "check all updates" button. */
    public static final String CHECK_ALL_TAG = "dppc_dppcp_checkall";
    /** NBT tag on a "check this plugin" button; its value is the plugin name. */
    public static final String CHECK_ONE_TAG = "dppc_dppcp_checkone";

    private static final int LIST_SIZE = 54;
    private static final int DETAIL_SIZE = 27;
    private static final int BACK_SLOT = 22;
    private static final int CHECK_ONE_SLOT = 24;

    public static void openDPPListGUI(Player p) {
        buildListInventory().openInventory(p);
    }

    public static void openPluginDetailGUI(Player p, JavaPlugin plugin) {
        buildDetailInventory(plugin).openInventory(p);
    }

    /** @return the wool material representing the plugin's current cached update status. */
    public static UpdateStatus getUpdateStatus(JavaPlugin plugin) {
        return UpdateStatus.of(plugin.getDescription().getVersion(), PluginUtil.getCachedLatestVersion(plugin.getName()));
    }

    // ---------------------------------------------------------------------------------------------
    // List panel
    // ---------------------------------------------------------------------------------------------

    /**
     * Builds the paged plugin list inventory (not opened). Plugins are sorted alphabetically and
     * each is rendered as a wool item coloured by its update status and carrying {@link #OPEN_TAG}.
     */
    public static DInventory buildListInventory() {
        DInventory di = new DInventory("§8DP-Plugins", LIST_SIZE, true, instance);
        di.setName(LIST_MARKER);
        populateList(di);
        return di;
    }

    /**
     * (Re)fills a list inventory from the current plugin set and cached statuses, preserving the
     * current page where possible. Safe to call on an inventory a player is currently viewing.
     */
    public static void populateList(DInventory di) {
        List<JavaPlugin> plugins = sortedPlugins();
        List<ItemStack> items = new ArrayList<>();
        for (JavaPlugin plugin : plugins) {
            items.add(buildListItem(plugin));
        }
        di.getPageItems().clear();
        di.addPageItems(items);

        int contentPerPage = LIST_SIZE - 9; // bottom row reserved for paging / control tools
        int maxIndex = items.isEmpty() ? 0 : (items.size() - 1) / contentPerPage;
        di.setPages(maxIndex);
        if (di.getCurrentPage() > maxIndex) {
            di.setCurrentPage(maxIndex);
        }
        di.setPageTools(buildPageTools());
        di.update();
    }

    /** Refreshes the list a player is viewing in place, if any. */
    public static void refreshOpenList(Player p) {
        InventoryHolder holder = p.getOpenInventory().getTopInventory().getHolder();
        if (holder instanceof DInventory && LIST_MARKER.equals(((DInventory) holder).getName())) {
            populateList((DInventory) holder);
        }
    }

    private static List<JavaPlugin> sortedPlugins() {
        List<JavaPlugin> plugins = new ArrayList<>(PluginUtil.getLoadedPlugins().keySet());
        plugins.removeIf(Objects::isNull);
        plugins.sort(Comparator.comparing(JavaPlugin::getName, String.CASE_INSENSITIVE_ORDER));
        return plugins;
    }

    private static ItemStack buildListItem(JavaPlugin plugin) {
        PluginDescriptionFile desc = plugin.getDescription();
        UpdateStatus status = getUpdateStatus(plugin);
        String latest = PluginUtil.getCachedLatestVersion(plugin.getName());

        ItemStack item = simple(woolFor(status), "§b" + plugin.getName());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(List.of(
                    "§eVersion§f: §b" + desc.getVersion(),
                    statusLine(status, latest),
                    "§eEnabled§f: " + (plugin.isEnabled() ? "§aYes" : "§cNo"),
                    "",
                    "§eLeft-click§f: §7View details",
                    "§eRight-click§f: §7Check for updates"));
            item.setItemMeta(meta);
        }
        item = locked(item);
        return NBT.setStringTag(item, OPEN_TAG, plugin.getName());
    }

    private static ItemStack[] buildPageTools() {
        ItemStack[] tools = new ItemStack[9];
        ItemStack filler = locked(simple(Material.GRAY_STAINED_GLASS_PANE, " "));
        for (int i = 0; i < tools.length; i++) {
            tools[i] = filler.clone();
        }
        tools[1] = NBT.setStringTag(simple(Material.ARROW, "§aPrevious Page"), "dppc_prevpage", "true");
        tools[7] = NBT.setStringTag(simple(Material.ARROW, "§aNext Page"), "dppc_nextpage", "true");

        ItemStack checkAll = simple(Material.CLOCK, "§eCheck All Updates");
        ItemMeta meta = checkAll.getItemMeta();
        if (meta != null) {
            meta.setLore(List.of("§7Fetch the latest version", "§7for every plugin."));
            checkAll.setItemMeta(meta);
        }
        checkAll = locked(checkAll);
        tools[4] = NBT.setStringTag(checkAll, CHECK_ALL_TAG, "true");
        return tools;
    }

    // ---------------------------------------------------------------------------------------------
    // Detail panel
    // ---------------------------------------------------------------------------------------------

    /** Builds the read-only detail inventory for a single plugin (not opened). */
    public static DInventory buildDetailInventory(JavaPlugin plugin) {
        PluginDescriptionFile desc = plugin.getDescription();
        DInventory di = new DInventory("§8" + plugin.getName() + " Info", DETAIL_SIZE, instance);
        di.setName(DETAIL_MARKER);

        ItemStack filler = locked(simple(Material.GRAY_STAINED_GLASS_PANE, " "));
        for (int i = 0; i < DETAIL_SIZE; i++) {
            di.setItem(i, filler.clone());
        }

        di.setItem(4, buildStatusItem(plugin));
        di.setItem(10, buildGeneralItem(plugin, desc));
        di.setItem(12, buildUpdateStatusItem(plugin));
        di.setItem(14, buildDependItem(desc));
        di.setItem(16, buildCommandsItem(desc));
        di.setItem(BACK_SLOT, buildBackItem());
        di.setItem(CHECK_ONE_SLOT, buildCheckOneItem(plugin));
        return di;
    }

    private static ItemStack buildStatusItem(JavaPlugin plugin) {
        boolean enabled = plugin.isEnabled();
        ItemStack item = simple(enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                "§eStatus§f: " + (enabled ? "§aEnabled" : "§cDisabled"));
        Integer bStats = PluginUtil.getLoadedPlugins().get(plugin);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(List.of("§ebStats ID§f: §b" + (bStats == null || bStats == 0 ? "N/A" : bStats)));
            item.setItemMeta(meta);
        }
        return locked(item);
    }

    private static ItemStack buildUpdateStatusItem(JavaPlugin plugin) {
        UpdateStatus status = getUpdateStatus(plugin);
        String latest = PluginUtil.getCachedLatestVersion(plugin.getName());
        ItemStack item = simple(woolFor(status), "§eUpdate Status");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String latestText;
            if (latest == null) {
                latestText = "§7Not checked";
            } else if (latest.isEmpty() || latest.equals("0.0.0")) {
                latestText = "§7Unverified";
            } else {
                latestText = "§b" + latest;
            }
            meta.setLore(List.of(
                    "§eInstalled§f: §b" + plugin.getDescription().getVersion(),
                    "§eLatest§f: " + latestText,
                    statusLine(status, latest)));
            item.setItemMeta(meta);
        }
        return locked(item);
    }

    private static ItemStack buildGeneralItem(JavaPlugin plugin, PluginDescriptionFile desc) {
        ItemStack item = simple(Material.ENCHANTED_BOOK, "§b" + plugin.getName());
        List<String> lore = new ArrayList<>();
        lore.add("§eVersion§f: §b" + desc.getVersion());
        lore.add("§eMain§f: §b" + desc.getMain());
        lore.add("§eAPI Version§f: §b" + (desc.getAPIVersion() != null ? desc.getAPIVersion() : "N/A"));
        lore.add("§eAuthors§f: §b" + (desc.getAuthors().isEmpty() ? "N/A" : String.join(", ", desc.getAuthors())));
        lore.add("§eWebsite§f: §b" + (desc.getWebsite() != null ? desc.getWebsite() : "N/A"));
        if (desc.getDescription() != null && !desc.getDescription().isEmpty()) {
            lore.add("§eDescription§f: §7" + desc.getDescription());
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return locked(item);
    }

    private static ItemStack buildDependItem(PluginDescriptionFile desc) {
        ItemStack item = simple(Material.CHAIN, "§6Dependencies");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(List.of(
                    "§eDepend§f: §b" + joinOrNone(desc.getDepend()),
                    "§eSoft Depend§f: §b" + joinOrNone(desc.getSoftDepend()),
                    "§eLoad Before§f: §b" + joinOrNone(desc.getLoadBefore())));
            item.setItemMeta(meta);
        }
        return locked(item);
    }

    private static ItemStack buildCommandsItem(PluginDescriptionFile desc) {
        List<String> commands = new ArrayList<>(desc.getCommands().keySet());
        ItemStack item = simple(Material.COMMAND_BLOCK, "§dCommands (" + commands.size() + ")");
        List<String> lore = new ArrayList<>();
        int shown = Math.min(commands.size(), 10);
        for (int i = 0; i < shown; i++) {
            lore.add("§7- §f/" + commands.get(i));
        }
        if (commands.size() > shown) {
            lore.add("§8...and " + (commands.size() - shown) + " more");
        }
        if (commands.isEmpty()) {
            lore.add("§7None");
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return locked(item);
    }

    private static ItemStack buildBackItem() {
        ItemStack item = simple(Material.ARROW, "§aBack to list");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(List.of("§7Return to the plugin list"));
            item.setItemMeta(meta);
        }
        item = locked(item);
        return NBT.setStringTag(item, BACK_TAG, "true");
    }

    private static ItemStack buildCheckOneItem(JavaPlugin plugin) {
        ItemStack item = simple(Material.CLOCK, "§eCheck for updates");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(List.of("§7Fetch the latest version", "§7for §b" + plugin.getName()));
            item.setItemMeta(meta);
        }
        item = locked(item);
        return NBT.setStringTag(item, CHECK_ONE_TAG, plugin.getName());
    }

    // ---------------------------------------------------------------------------------------------
    // Shared helpers
    // ---------------------------------------------------------------------------------------------

    private static Material woolFor(UpdateStatus status) {
        switch (status) {
            case UP_TO_DATE:
                return Material.LIME_WOOL;
            case OUTDATED:
                return Material.RED_WOOL;
            case UNKNOWN:
            default:
                return Material.GRAY_WOOL;
        }
    }

    private static String statusLine(UpdateStatus status, String latest) {
        switch (status) {
            case UP_TO_DATE:
                return "§eStatus§f: §aUp to date";
            case OUTDATED:
                return "§eStatus§f: §cOutdated §7(latest: " + latest + ")";
            case UNKNOWN:
            default:
                return "§eStatus§f: §7Unknown §8(check to verify)";
        }
    }

    private static String joinOrNone(List<String> values) {
        return values == null || values.isEmpty() ? "None" : String.join(", ", values);
    }

    private static ItemStack simple(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack locked(ItemStack item) {
        return NBT.setStringTag(item, "dppc_clickcancel", "true");
    }
}
