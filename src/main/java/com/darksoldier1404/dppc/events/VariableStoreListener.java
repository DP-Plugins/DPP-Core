package com.darksoldier1404.dppc.events;

import com.darksoldier1404.dppc.DPPCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Loads a player's persisted variables on join and saves/unloads them on quit.
 */
public class VariableStoreListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (DPPCore.variables == null) return;
        DPPCore.variables.loadPlayer(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (DPPCore.variables == null) return;
        DPPCore.variables.savePlayer(e.getPlayer().getUniqueId());
        DPPCore.variables.unloadPlayer(e.getPlayer().getUniqueId());
    }
}
