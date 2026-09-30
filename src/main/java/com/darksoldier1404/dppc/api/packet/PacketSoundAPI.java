package com.darksoldier1404.dppc.api.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Packet-only sound playback tied to an entity ID rather than a fixed {@link org.bukkit.Location}.
 * For a sound anchored to a point in the world, {@link Player#playSound} already does that
 * without ProtocolLib. This class exists for sounds that should follow an entity (a fake one
 * from a future hologram/NPC feature, or a real one) as it moves.
 */
@DPPCoreVersion(since = "5.5.0")
public class PacketSoundAPI {

    /** Plays {@code sound} as if it originated from {@code entityId}; follows that entity if it moves. */
    public static void playFromEntity(Player viewer, int entityId, Sound sound,
                                       EnumWrappers.SoundCategory category, float volume, float pitch) {
        ProtocolManager mgr = PacketAPI.manager();
        if (mgr == null) return;

        // forceDefaults=true so the Seed field (sound variant selection) gets a valid default
        // instead of being left null/unset.
        PacketContainer packet = mgr.createPacket(PacketType.Play.Server.ENTITY_SOUND, true);
        packet.getIntegers().write(0, entityId);
        packet.getSoundEffects().write(0, sound);
        packet.getSoundCategories().write(0, category);
        packet.getFloat().write(0, volume);
        packet.getFloat().write(1, pitch);

        mgr.sendServerPacket(viewer, packet);
    }
}
