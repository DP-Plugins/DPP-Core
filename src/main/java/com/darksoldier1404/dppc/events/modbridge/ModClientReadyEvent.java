package com.darksoldier1404.dppc.events.modbridge;

import com.darksoldier1404.dppc.modbridge.ModSession;
import com.darksoldier1404.dppmc.protocol.core.ModOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/** A player's client mod completed the handshake for a protocol bound on this server. Main thread, once per connection. */
public class ModClientReadyEvent extends PlayerEvent {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private final ModOffer offer;
    private final ModSession session;

    public ModClientReadyEvent(@NotNull Player player, ModOffer offer, ModSession session) {
        super(player);
        this.offer = offer;
        this.session = session;
    }

    /** The protocol namespace, e.g. {@code dp_hudshop}. */
    public String getModId() {
        return offer.modId();
    }

    public String getModVersion() {
        return offer.modVersion();
    }

    public ModOffer getOffer() {
        return offer;
    }

    public ModSession getSession() {
        return session;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }
}
