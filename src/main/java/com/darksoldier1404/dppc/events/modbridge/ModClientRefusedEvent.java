package com.darksoldier1404.dppc.events.modbridge;

import com.darksoldier1404.dppmc.protocol.core.ModOffer;
import com.darksoldier1404.dppmc.protocol.core.Verdict;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * A player's client mod offered a protocol bound on this server with an incompatible version
 * ({@link Verdict#CLIENT_TOO_OLD} or {@link Verdict#SERVER_TOO_OLD}). Main thread, once per connection.
 */
public class ModClientRefusedEvent extends PlayerEvent {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private final ModOffer offer;
    private final Verdict verdict;

    public ModClientRefusedEvent(@NotNull Player player, ModOffer offer, Verdict verdict) {
        super(player);
        this.offer = offer;
        this.verdict = verdict;
    }

    public String getModId() {
        return offer.modId();
    }

    public String getModVersion() {
        return offer.modVersion();
    }

    public ModOffer getOffer() {
        return offer;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }
}
