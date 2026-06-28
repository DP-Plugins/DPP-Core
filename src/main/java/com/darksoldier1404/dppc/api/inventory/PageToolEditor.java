package com.darksoldier1404.dppc.api.inventory;

import com.darksoldier1404.dppc.DPPCore;
import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;

/**
 * A GUI for editing the default DInventory paging tool bar
 * ({@link DefaultPageTools}). Opened with {@code /dppcdi edit}.
 *
 * <p>Layout (27 slots, 3 rows):</p>
 * <ul>
 *     <li>Row 1 (0-8): the live tool bar. Admins place any item here freely (WYSIWYG appearance).</li>
 *     <li>Row 2 (9-17): one function button per slot above; clicking cycles the role
 *         (DECORATION → PREV → NEXT → CURRENT → ...).</li>
 *     <li>Row 3: Save (18), Reset to default (22), Cancel (26); the rest is filler.</li>
 * </ul>
 *
 * <p>Click and drag handling lives in {@code PageToolEditorListener}.</p>
 */
@DPPCoreVersion(since = "5.4.3")
public class PageToolEditor implements InventoryHolder {

    public static final String TITLE = "§8Page Tool Editor";
    public static final int SAVE_SLOT = 18;
    public static final int RESET_SLOT = 22;
    public static final int CANCEL_SLOT = 26;

    private final Inventory inventory;
    private final PageToolRole[] roles = new PageToolRole[DefaultPageTools.SLOTS];

    public PageToolEditor() {
        this.inventory = Bukkit.createInventory(this, 27, TITLE);
        DefaultPageTools.Layout layout = DefaultPageTools.load(DPPCore.getInstance().getConfig());
        if (layout == null) {
            layout = DefaultPageTools.defaultSeed(DPPCore.getInstance().getConfig());
        }
        applyLayout(layout);
        buildControls();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    /**
     * Loads the given layout into the editor: raw items into the top row and the matching
     * function buttons into the second row.
     */
    public void applyLayout(DefaultPageTools.Layout layout) {
        for (int i = 0; i < DefaultPageTools.SLOTS; i++) {
            ItemStack item = layout.getItem(i);
            inventory.setItem(i, item == null ? null : item.clone());
            roles[i] = layout.getRole(i);
            inventory.setItem(DefaultPageTools.SLOTS + i, buildFunctionButton(roles[i]));
        }
    }

    /**
     * Resets the editor view to the built-in default arrangement. Does not persist anything
     * until the player clicks Save.
     */
    public void resetToDefault() {
        applyLayout(DefaultPageTools.defaultSeed(DPPCore.getInstance().getConfig()));
    }

    /**
     * Advances the role of the given top-row slot and refreshes its function button.
     */
    public void cycleRole(int topSlot) {
        if (topSlot < 0 || topSlot >= DefaultPageTools.SLOTS) return;
        roles[topSlot] = roles[topSlot].next();
        inventory.setItem(DefaultPageTools.SLOTS + topSlot, buildFunctionButton(roles[topSlot]));
    }

    /**
     * Snapshots the current top-row items and roles into a {@link DefaultPageTools.Layout}.
     */
    public DefaultPageTools.Layout toLayout() {
        DefaultPageTools.Layout layout = new DefaultPageTools.Layout();
        for (int i = 0; i < DefaultPageTools.SLOTS; i++) {
            ItemStack item = inventory.getItem(i);
            layout.setSlot(i, item == null ? null : item.clone(), roles[i]);
        }
        return layout;
    }

    private ItemStack buildFunctionButton(PageToolRole role) {
        Material material;
        String name;
        switch (role) {
            case PREV:
                material = Material.LIME_DYE;
                name = "§aFunction: Previous Page";
                break;
            case NEXT:
                material = Material.LIME_DYE;
                name = "§aFunction: Next Page";
                break;
            case CURRENT:
                material = Material.YELLOW_DYE;
                name = "§eFunction: Current Page";
                break;
            case DECORATION:
            default:
                material = Material.GRAY_DYE;
                name = "§7Function: Decoration";
                break;
        }
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (role == PageToolRole.CURRENT) {
                meta.setLore(Arrays.asList(
                        "§8Use {current} / {total} in the",
                        "§8item name above for page numbers.",
                        "§7Click to change function."));
            } else {
                meta.setLore(Collections.singletonList("§7Click to change function."));
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            button.setItemMeta(meta);
        }
        return button;
    }

    private void buildControls() {
        ItemStack filler = simpleItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = DefaultPageTools.SLOTS * 2; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler.clone());
        }
        inventory.setItem(SAVE_SLOT, namedItem(Material.LIME_WOOL, "§aSave & Apply",
                "§7Writes the current layout to config", "§7and applies it to new inventories."));
        inventory.setItem(RESET_SLOT, namedItem(Material.BARRIER, "§eReset to Default",
                "§7Restores the built-in arrangement.", "§7(not saved until you click Save)"));
        inventory.setItem(CANCEL_SLOT, namedItem(Material.RED_WOOL, "§cCancel",
                "§7Close without saving."));
    }

    private ItemStack simpleItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack namedItem(Material material, String name, String... lore) {
        ItemStack item = simpleItem(material, name);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
