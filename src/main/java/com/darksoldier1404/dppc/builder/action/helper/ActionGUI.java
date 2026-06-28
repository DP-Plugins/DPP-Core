package com.darksoldier1404.dppc.builder.action.helper;

import com.darksoldier1404.dppc.DPPCore;
import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import com.darksoldier1404.dppc.builder.action.ActionBuilder;
import com.darksoldier1404.dppc.lang.DLang;
import com.darksoldier1404.dppc.builder.action.obj.Action;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;
import com.darksoldier1404.dppc.api.inventory.DInventory;
import com.darksoldier1404.dppc.data.DPlugin;
import com.darksoldier1404.dppc.utils.NBT;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

@DPPCoreVersion(since = "5.4.0")
public class ActionGUI {
    static final int PAGE_SIZE = 45;

    private final DPlugin plugin;
    private ActionBuilder actionBuilder;

    public ActionGUI(ActionBuilder actionBuilder) {
        this.plugin = actionBuilder.getPlugin();
        this.actionBuilder = actionBuilder;
    }

    public ActionGUI(DPlugin plugin, String actionName) {
        this.plugin = plugin;
        this.actionBuilder = new ActionBuilder(plugin, actionName);
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    private DLang lang() {
        return DPPCore.getInstance().getLang();
    }

    public void setActionBuilder(ActionBuilder actionBuilder) {
        this.actionBuilder = actionBuilder;
    }

    public ActionBuilder getActionBuilder() {
        return actionBuilder;
    }

    public void openActionBuilderGUI(Player p) {
        openActionBuilderGUI(p, 0);
    }

    public void openActionBuilderGUI(Player p, int page) {
        List<Action> actions = actionBuilder.getActions();
        int totalPages = (int) Math.ceil(actions.size() / (double) PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        DInventory inv = new DInventory(lang().getWithArgs("ab.gui.title", actionBuilder.getActionName()), 54, plugin);
        inv.setChannel(0);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, actions.size());

        for (int i = start; i < end; i++) {
            Action action = actions.get(i);
            int slot = i - start;

            ItemStack item = buildActionItem(action, i);
            inv.setItem(slot, item);
        }

        // Navigation bar (row 6: slots 45-53)
        fillNavBar(inv, page, totalPages);

        inv.setObj(this);
        inv.openInventory(p);

        // Store current page in obj field via a wrapper so handler can access it
        // We encode page in NBT of the inventory holder's obj (handled in handler via ActionGUI)
        currentPage = page;
    }

    // Stored as instance field so the handler can read it
    int currentPage = 0;

    private ItemStack buildActionItem(Action action, int globalIndex) {
        ItemStack item = new ItemStack(getActionMaterial(action.getActionType()));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§f[§e" + globalIndex + "§f] §b" + action.getActionType().name());
        meta.setLore(Arrays.asList(
                "§7" + action.getDisplayText(),
                "",
                lang().get("ab.gui.edit_lore"),
                lang().get("ab.gui.remove_lore")
        ));
        item.setItemMeta(meta);
        item = NBT.setStringTag(item, "dppc.actionType", action.getActionType().name());
        item = NBT.setIntTag(item, "dppc.actionIndex", globalIndex);
        return item;
    }

    private void fillNavBar(DInventory inv, int page, int totalPages) {
        // Slot 45: Prev page
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta m = prev.getItemMeta();
            m.setDisplayName(lang().get("ab.gui.prev_page"));
            prev.setItemMeta(m);
            prev = NBT.setStringTag(prev, "dppc.action", "prev");
            prev = NBT.setIntTag(prev, "dppc.page", page - 1);
            inv.setItem(45, prev);
        }

        // Slot 46: Add action
        ItemStack addAction = new ItemStack(Material.LIME_DYE);
        ItemMeta addMeta = addAction.getItemMeta();
        addMeta.setDisplayName(lang().get("ab.gui.add_action"));
        addAction.setItemMeta(addMeta);
        addAction = NBT.setStringTag(addAction, "dppc.action", "add");
        inv.setItem(46, addAction);

        // Slot 49: Page indicator
        ItemStack pageInfo = new ItemStack(Material.COMPASS);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        pageMeta.setDisplayName(lang().getWithArgs("ab.gui.page_indicator", String.valueOf(page + 1), String.valueOf(totalPages)));
        pageMeta.setLore(Arrays.asList(lang().getWithArgs("ab.gui.total_actions", String.valueOf(actionBuilder.getActions().size()))));
        pageInfo.setItemMeta(pageMeta);
        inv.setItem(49, pageInfo);

        // Slot 52: Next page
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta m = next.getItemMeta();
            m.setDisplayName(lang().get("ab.gui.next_page"));
            next.setItemMeta(m);
            next = NBT.setStringTag(next, "dppc.action", "next");
            next = NBT.setIntTag(next, "dppc.page", page + 1);
            inv.setItem(52, next);
        }

        // Slot 53: Save
        ItemStack save = new ItemStack(Material.EMERALD);
        ItemMeta saveMeta = save.getItemMeta();
        saveMeta.setDisplayName(lang().get("ab.gui.save"));
        save.setItemMeta(saveMeta);
        save = NBT.setStringTag(save, "dppc.action", "save");
        inv.setItem(53, save);
    }

    public void openActionSelectGUI(Player p) {
        ActionType[] types = ActionType.values();
        int size = (int) Math.ceil(types.length / 9.0) * 9;
        size = Math.max(size, 9);
        if (size > 54) size = 54;

        DInventory inv = new DInventory(lang().get("ab.gui.select_title"), size, plugin);
        inv.setChannel(1);

        for (ActionType type : types) {
            ItemStack item = new ItemStack(getActionMaterial(type));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§b" + type.name());
            meta.setLore(Arrays.asList("§7" + getActionDescription(type)));
            item.setItemMeta(meta);
            item = NBT.setStringTag(item, "dppc.actionTypeSelect", type.name());
            inv.addItem(item);
        }

        inv.setObj(this);
        inv.openInventory(p);
    }

    private Material getActionMaterial(ActionType type) {
        switch (type) {
            case DELAY: return Material.CLOCK;
            case SEND_MESSAGE: return Material.PAPER;
            case SEND_TITLE: return Material.NAME_TAG;
            case SEND_ACTIONBAR: return Material.OAK_SIGN;
            case BROADCAST: return Material.BELL;
            case BROADCAST_WORLD: return Material.GRASS_BLOCK;
            case EXECUTE_AS_ADMIN: return Material.COMMAND_BLOCK;
            case EXECUTE_AS_PLAYER: return Material.WRITABLE_BOOK;
            case TELEPORT: return Material.ENDER_PEARL;
            case CLOSE_INVENTORY: return Material.BARRIER;
            case SET_GAMEMODE: return Material.ELYTRA;
            case GIVE_EXP: return Material.EXPERIENCE_BOTTLE;
            case TAKE_EXP: return Material.GLASS_BOTTLE;
            case SET_HEALTH: return Material.RED_DYE;
            case SET_HUNGER: return Material.COOKED_BEEF;
            case KICK: return Material.IRON_BOOTS;
            case PLAY_SOUND: return Material.NOTE_BLOCK;
            case PLAY_PARTICLE: return Material.BLAZE_POWDER;
            case ADD_POTION_EFFECT: return Material.POTION;
            case REMOVE_POTION_EFFECT: return Material.GLASS_BOTTLE;
            case CLEAR_EFFECTS: return Material.MILK_BUCKET;
            case GIVE_ITEM: return Material.CHEST;
            case TAKE_ITEM: return Material.TRAPPED_CHEST;
            case SET_TEMP_VARIABLE: return Material.BOOK;
            case ADD_TEMP_VARIABLE: return Material.WRITABLE_BOOK;
            case RANDOM_TEMP_NUMBER: return Material.NETHER_STAR;
            case SET_PLAYER_VARIABLE:
            case ADD_PLAYER_VARIABLE:
            case RANDOM_PLAYER_NUMBER: return Material.PLAYER_HEAD;
            case SET_GLOBAL_VARIABLE:
            case ADD_GLOBAL_VARIABLE:
            case RANDOM_GLOBAL_NUMBER: return Material.BEACON;
            case IF_HAS_PERMISSION:
            case IF_NOT_PERMISSION: return Material.GOLDEN_APPLE;
            case IF_TEMP_VARIABLE_EQUALS:
            case IF_TEMP_VARIABLE_NOT_EQUALS:
            case IF_TEMP_VARIABLE_GREATER:
            case IF_TEMP_VARIABLE_LESS: return Material.COMPARATOR;
            case IF_PLAYER_VARIABLE_EQUALS:
            case IF_PLAYER_VARIABLE_NOT_EQUALS:
            case IF_PLAYER_VARIABLE_GREATER:
            case IF_PLAYER_VARIABLE_LESS: return Material.OBSERVER;
            case IF_GLOBAL_VARIABLE_EQUALS:
            case IF_GLOBAL_VARIABLE_NOT_EQUALS:
            case IF_GLOBAL_VARIABLE_GREATER:
            case IF_GLOBAL_VARIABLE_LESS: return Material.DAYLIGHT_DETECTOR;
            case ELSE: return Material.REPEATER;
            case END_IF: return Material.REDSTONE_TORCH;
            case CANCEL: return Material.BARRIER;
            case CALL_ACTION: return Material.FIREWORK_ROCKET;
            default: return Material.PAPER;
        }
    }

    private String getActionDescription(ActionType type) {
        switch (type) {
            case DELAY: return "delay <ticks>";
            case SEND_MESSAGE: return "send_message <message>";
            case SEND_TITLE: return "send_title <title>|<subtitle>|<in>|<stay>|<out>";
            case SEND_ACTIONBAR: return "send_actionbar <message>";
            case BROADCAST: return "broadcast <message>";
            case BROADCAST_WORLD: return "broadcast_world <message>";
            case EXECUTE_AS_ADMIN: return "execute_as_admin <command>";
            case EXECUTE_AS_PLAYER: return "execute_as_player <command>";
            case TELEPORT: return "teleport <world> <x>,<y>,<z>";
            case CLOSE_INVENTORY: return "close_inventory";
            case SET_GAMEMODE: return "set_gamemode <SURVIVAL|CREATIVE|...>";
            case GIVE_EXP: return "give_exp <amount>";
            case TAKE_EXP: return "take_exp <amount>";
            case SET_HEALTH: return "set_health <amount>";
            case SET_HUNGER: return "set_hunger <0-20>";
            case KICK: return "kick <reason>";
            case PLAY_SOUND: return "play_sound <sound> [volume] [pitch]";
            case PLAY_PARTICLE: return "play_particle <type> <count> [ox] [oy] [oz]";
            case ADD_POTION_EFFECT: return "add_potion_effect <type> <secs> <amp>";
            case REMOVE_POTION_EFFECT: return "remove_potion_effect <type>";
            case CLEAR_EFFECTS: return "clear_effects";
            case GIVE_ITEM: return "give_item <material> <amount>";
            case TAKE_ITEM: return "take_item <material> <amount>";
            case SET_TEMP_VARIABLE: return "set_temp_variable <name> <value>";
            case ADD_TEMP_VARIABLE: return "add_temp_variable <name> <amount>";
            case RANDOM_TEMP_NUMBER: return "random_temp_number <name> <min> <max>";
            case SET_PLAYER_VARIABLE: return "set_player_variable <name> <value>";
            case ADD_PLAYER_VARIABLE: return "add_player_variable <name> <amount>";
            case RANDOM_PLAYER_NUMBER: return "random_player_number <name> <min> <max>";
            case SET_GLOBAL_VARIABLE: return "set_global_variable <name> <value>";
            case ADD_GLOBAL_VARIABLE: return "add_global_variable <name> <amount>";
            case RANDOM_GLOBAL_NUMBER: return "random_global_number <name> <min> <max>";
            case IF_HAS_PERMISSION: return "if_has_permission <perm>";
            case IF_NOT_PERMISSION: return "if_not_permission <perm>";
            case IF_TEMP_VARIABLE_EQUALS: return "if_temp_variable_equals <name> <value>";
            case IF_TEMP_VARIABLE_NOT_EQUALS: return "if_temp_variable_not_equals <name> <value>";
            case IF_TEMP_VARIABLE_GREATER: return "if_temp_variable_greater <name> <num>";
            case IF_TEMP_VARIABLE_LESS: return "if_temp_variable_less <name> <num>";
            case IF_PLAYER_VARIABLE_EQUALS: return "if_player_variable_equals <name> <value>";
            case IF_PLAYER_VARIABLE_NOT_EQUALS: return "if_player_variable_not_equals <name> <value>";
            case IF_PLAYER_VARIABLE_GREATER: return "if_player_variable_greater <name> <num>";
            case IF_PLAYER_VARIABLE_LESS: return "if_player_variable_less <name> <num>";
            case IF_GLOBAL_VARIABLE_EQUALS: return "if_global_variable_equals <name> <value>";
            case IF_GLOBAL_VARIABLE_NOT_EQUALS: return "if_global_variable_not_equals <name> <value>";
            case IF_GLOBAL_VARIABLE_GREATER: return "if_global_variable_greater <name> <num>";
            case IF_GLOBAL_VARIABLE_LESS: return "if_global_variable_less <name> <num>";
            case ELSE: return "else";
            case END_IF: return "end_if";
            case CANCEL: return "cancel";
            case CALL_ACTION: return "call_action <action_name>";
            default: return "";
        }
    }
}
