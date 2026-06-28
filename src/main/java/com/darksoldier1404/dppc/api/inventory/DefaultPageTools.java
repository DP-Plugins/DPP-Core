package com.darksoldier1404.dppc.api.inventory;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import com.darksoldier1404.dppc.utils.NBT;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

/**
 * Loads, saves and renders the configurable 9-slot tool bar used by
 * {@link DInventory#applyDefaultPageTools()} when {@code useDefaultPageTools} is enabled.
 *
 * <p>A {@link Layout} stores the raw (untagged) item and {@link PageToolRole} for each of the
 * 9 tool slots. {@link #render} produces the click-tagged item array that is shown in the
 * inventory, applying the same NBT tags the pagination listener reacts to.</p>
 *
 * <p>New configuration lives under {@code Settings.DInventory.defaultPageTools.slots}. When that
 * section is absent, {@link #defaultSeed} reproduces the classic hard-coded arrangement
 * (prev at slot 1, current at slot 4, next at slot 7, decorative panes elsewhere), honouring the
 * legacy {@code Settings.DInventory.defaultPageToolItem.*} appearance overrides for backward compatibility.</p>
 */
@DPPCoreVersion(since = "5.4.3")
public class DefaultPageTools {

    public static final int SLOTS = 9;
    public static final String LAYOUT_PATH = "Settings.DInventory.defaultPageTools";
    public static final String LEGACY_ITEM_PATH = "Settings.DInventory.defaultPageToolItem";

    private DefaultPageTools() {
    }

    /**
     * Mutable description of the 9-slot tool bar: a raw item and a role per slot.
     * Items are stored untagged so that what is persisted to config stays clean.
     */
    public static class Layout {
        private final ItemStack[] items = new ItemStack[SLOTS];
        private final PageToolRole[] roles = new PageToolRole[SLOTS];

        public Layout() {
            for (int i = 0; i < SLOTS; i++) {
                roles[i] = PageToolRole.DECORATION;
            }
        }

        @Nullable
        public ItemStack getItem(int slot) {
            return items[slot];
        }

        public PageToolRole getRole(int slot) {
            return roles[slot];
        }

        public void setSlot(int slot, @Nullable ItemStack item, PageToolRole role) {
            this.items[slot] = item;
            this.roles[slot] = role == null ? PageToolRole.DECORATION : role;
        }

        public void setRole(int slot, PageToolRole role) {
            this.roles[slot] = role == null ? PageToolRole.DECORATION : role;
        }
    }

    /**
     * Applies the click-cancel / navigation NBT tags for the given role to a copy of {@code raw}.
     * The input item is never mutated. For {@link PageToolRole#CURRENT} the {@code {current}} and
     * {@code {total}} placeholders in the display name are substituted with the 1-based page numbers.
     *
     * @return a tagged clone, or {@code null} if {@code raw} is {@code null}.
     */
    @Nullable
    public static ItemStack tagFor(@Nullable ItemStack raw, PageToolRole role, int currentPage, int pages) {
        if (raw == null) return null;
        ItemStack item = NBT.setStringTag(raw.clone(), "dppc_clickcancel", "true");
        switch (role) {
            case PREV:
                item = NBT.setStringTag(item, "dppc_prevpage", "true");
                break;
            case NEXT:
                item = NBT.setStringTag(item, "dppc_nextpage", "true");
                break;
            case CURRENT:
                item = NBT.setStringTag(item, "dppc_currentpage", "true");
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    String name = meta.getDisplayName()
                            .replace("{current}", String.valueOf(currentPage + 1))
                            .replace("{total}", String.valueOf(pages + 1));
                    meta.setDisplayName(name);
                    item.setItemMeta(meta);
                }
                break;
            case DECORATION:
            default:
                break;
        }
        return item;
    }

    /**
     * Renders a {@link Layout} into a click-tagged tool bar array.
     *
     * @param toolSlots length of the returned array (the inventory's reserved tool slots, normally 9).
     */
    public static ItemStack[] render(Layout layout, int currentPage, int pages, int toolSlots) {
        ItemStack[] tools = new ItemStack[toolSlots];
        for (int i = 0; i < SLOTS && i < toolSlots; i++) {
            tools[i] = tagFor(layout.getItem(i), layout.getRole(i), currentPage, pages);
        }
        return tools;
    }

    /**
     * Reads the saved layout from {@code Settings.DInventory.defaultPageTools.slots}.
     *
     * @return the stored layout, or {@code null} if no layout has been configured.
     */
    @Nullable
    public static Layout load(FileConfiguration config) {
        ConfigurationSection slots = config.getConfigurationSection(LAYOUT_PATH + ".slots");
        if (slots == null) return null;
        Layout layout = new Layout();
        for (String key : slots.getKeys(false)) {
            int slot;
            try {
                slot = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                continue;
            }
            if (slot < 0 || slot >= SLOTS) continue;
            ItemStack item = slots.getItemStack(key + ".item");
            PageToolRole role = PageToolRole.fromString(slots.getString(key + ".role"));
            layout.setSlot(slot, item, role);
        }
        return layout;
    }

    /**
     * Overwrites the saved layout section with the given layout. Slots without an item are omitted.
     * Callers are responsible for persisting the config afterwards ({@code plugin.saveConfig()}).
     */
    public static void save(FileConfiguration config, Layout layout) {
        config.set(LAYOUT_PATH + ".slots", null);
        for (int i = 0; i < SLOTS; i++) {
            ItemStack item = layout.getItem(i);
            if (item == null) continue;
            config.set(LAYOUT_PATH + ".slots." + i + ".item", item);
            config.set(LAYOUT_PATH + ".slots." + i + ".role", layout.getRole(i).name());
        }
    }

    /**
     * Builds the classic default arrangement (prev=1, current=4, next=7, decorative panes elsewhere),
     * honouring the legacy {@code defaultPageToolItem.*} appearance overrides when present. Items are
     * returned untagged so the same {@link Layout} can seed the editor or be passed to {@link #render}.
     */
    public static Layout defaultSeed(FileConfiguration config) {
        Layout layout = new Layout();

        ItemStack pane = legacyItem(config, "PANE", Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            paneMeta.setDisplayName(" ");
            pane.setItemMeta(paneMeta);
        }

        ItemStack prev = legacyItem(config, "PREV", Material.ARROW);
        applyDefaultName(prev, config, "PREV", "§aPrevious Page");

        ItemStack next = legacyItem(config, "NEXT", Material.ARROW);
        applyDefaultName(next, config, "NEXT", "§aNext Page");

        ItemStack current = legacyItem(config, "CURRENT", Material.PAPER);
        applyDefaultName(current, config, "CURRENT", "§aCurrent Page: {current} / {total}");

        for (int i = 0; i < SLOTS; i++) {
            layout.setSlot(i, pane.clone(), PageToolRole.DECORATION);
        }
        layout.setSlot(1, prev, PageToolRole.PREV);
        layout.setSlot(4, current, PageToolRole.CURRENT);
        layout.setSlot(7, next, PageToolRole.NEXT);
        return layout;
    }

    private static ItemStack legacyItem(FileConfiguration config, String type, Material fallback) {
        ItemStack custom = config.getItemStack(LEGACY_ITEM_PATH + "." + type);
        return custom != null ? custom.clone() : new ItemStack(fallback);
    }

    private static void applyDefaultName(ItemStack item, FileConfiguration config, String type, String defaultName) {
        boolean custom = config.getItemStack(LEGACY_ITEM_PATH + "." + type) != null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        if (!custom) {
            meta.setDisplayName(defaultName);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
    }
}
