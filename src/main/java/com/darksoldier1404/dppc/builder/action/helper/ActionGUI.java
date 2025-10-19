package com.darksoldier1404.dppc.builder.action.helper;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import com.darksoldier1404.dppc.builder.action.ActionBuilder;
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

@DPPCoreVersion(since = "5.3.0")
public class ActionGUI {
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

    public void setActionBuilder(ActionBuilder actionBuilder) {
        this.actionBuilder = actionBuilder;
    }

    public ActionBuilder getActionBuilder() {
        return actionBuilder;
    }

    public void openActionBuilderGUI(Player p) {
        DInventory inv = new DInventory("Action Builder", 54, plugin);
        inv.setChannel(0);
        if (!actionBuilder.getActions().isEmpty()) {
            actionBuilder.getActions().forEach(action -> {
                int index = actionBuilder.getActions().indexOf(action);
                ItemStack actionItem = new ItemStack(Material.PAPER);
                ItemMeta actionMeta = actionItem.getItemMeta();
                actionMeta.setDisplayName("§f[ §e" + index + " §f] §9" + action.getActionTypeName());
                actionMeta.setLore(Arrays.asList("§aClick to edit", "§cRight click to remove", "", "§f" + action.serialize()));
                actionItem.setItemMeta(actionMeta);
                actionItem = NBT.setStringTag(actionItem, "dppc.actionType", action.getActionTypeName().name());
                actionItem = NBT.setIntTag(actionItem, "dppc.actionIndex", index);
                inv.addItem(actionItem);
            });
        }
        if (!(actionBuilder.getActions().size() >= 52)) {
            ItemStack addAction = new ItemStack(Material.PAPER);
            ItemMeta im = addAction.getItemMeta();
            im.setDisplayName("§6Click to add action");
            addAction.setItemMeta(im);
            addAction = NBT.setStringTag(addAction, "dppc.action", "add");
            inv.addItem(addAction);
        }
        ItemStack saveAction = new ItemStack(Material.EMERALD);
        ItemMeta saveMeta = saveAction.getItemMeta();
        saveMeta.setDisplayName("§aSave");
        saveAction.setItemMeta(saveMeta);
        saveAction = NBT.setStringTag(saveAction, "dppc.action", "save");
        inv.setItem(53, saveAction);
        inv.setObj(this);
        inv.openInventory(p);
    }

    public void openActionSelectGUI(Player p) {
        DInventory inv = new DInventory( "Action Selector", 27, plugin);
        inv.setChannel(1);
        for (ActionType name : ActionType.values()) {
            ItemStack actionItem = new ItemStack(Material.REDSTONE);
            ItemMeta actionMeta = actionItem.getItemMeta();
            actionMeta.setDisplayName("§b" + name);
            actionItem.setItemMeta(actionMeta);
            actionItem = NBT.setStringTag(actionItem, "dppc.actionTypeSelect", name.name());
            inv.addItem(actionItem);
        }
        inv.setObj(this);
        inv.openInventory(p);
    }
}
