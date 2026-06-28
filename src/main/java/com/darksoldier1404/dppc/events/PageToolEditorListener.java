package com.darksoldier1404.dppc.events;

import com.darksoldier1404.dppc.DPPCore;
import com.darksoldier1404.dppc.api.inventory.DefaultPageTools;
import com.darksoldier1404.dppc.api.inventory.PageToolEditor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

/**
 * Handles interaction with the {@link PageToolEditor} GUI.
 *
 * <p>The top row (slots 0-8) is freely editable so admins can place their own items. Every other
 * slot is locked: the second row cycles slot roles, the bottom row holds the Save / Reset / Cancel
 * controls. Shift-clicks and drags that would push items into the locked rows are cancelled.</p>
 */
public class PageToolEditorListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof PageToolEditor)) return;
        PageToolEditor editor = (PageToolEditor) e.getInventory().getHolder();
        Inventory clicked = e.getClickedInventory();

        // Clicks inside the player's own inventory: block shift-clicks (they would auto-move items
        // into the locked control rows), but otherwise allow normal item handling.
        if (clicked == null || clicked.getType() == InventoryType.PLAYER) {
            if (e.isShiftClick()) {
                e.setCancelled(true);
            }
            return;
        }

        int raw = e.getRawSlot();
        // Top row is the editable WYSIWYG tool bar.
        if (raw >= 0 && raw < DefaultPageTools.SLOTS) {
            return;
        }

        // Everything below the top row is locked.
        e.setCancelled(true);

        if (raw >= DefaultPageTools.SLOTS && raw < DefaultPageTools.SLOTS * 2) {
            editor.cycleRole(raw - DefaultPageTools.SLOTS);
            return;
        }

        Player player = (Player) e.getWhoClicked();
        switch (raw) {
            case PageToolEditor.SAVE_SLOT:
                DefaultPageTools.save(DPPCore.getInstance().getConfig(), editor.toLayout());
                DPPCore.getInstance().saveConfig();
                player.closeInventory();
                player.sendMessage("§aDefault page tool layout has been saved and applied.");
                break;
            case PageToolEditor.RESET_SLOT:
                editor.resetToDefault();
                player.sendMessage("§eLayout reset to default. Click Save to apply.");
                break;
            case PageToolEditor.CANCEL_SLOT:
                player.closeInventory();
                break;
            default:
                break;
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getInventory().getHolder() instanceof PageToolEditor)) return;
        for (int raw : e.getRawSlots()) {
            // Raw slots within the editor (top inventory) that fall outside the editable top row are locked.
            if (raw >= DefaultPageTools.SLOTS && raw < e.getInventory().getSize()) {
                e.setCancelled(true);
                return;
            }
        }
    }
}
