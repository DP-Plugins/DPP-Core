package com.darksoldier1404.dppc.utils;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Convenience wrappers around {@link World#spawnParticle} and {@link Player#spawnParticle}
 * for spawning particles at one location, broadcasting across a list of locations (e.g. the
 * shapes produced by {@code LocationUtil}), or showing particles to a single player.
 * <p>
 * The {@link Particle} constant is always supplied by the caller, so this class stays
 * version-agnostic across server versions where particle enum names differ
 * (e.g. {@code REDSTONE} vs {@code DUST}). All methods are null-safe: a null location, list,
 * or world is silently ignored.
 */
@SuppressWarnings("all")
@DPPCoreVersion(since = "5.4.5")
public class ParticleUtil {

    private ParticleUtil() {
    }

    /**
     * Spawn a single particle at a location (visible to all nearby players).
     *
     * @param loc      where to spawn
     * @param particle particle type
     */
    public static void spawn(@Nullable Location loc, @Nullable Particle particle) {
        spawn(loc, particle, 1);
    }

    /**
     * Spawn particles at a location (visible to all nearby players).
     *
     * @param loc      where to spawn
     * @param particle particle type
     * @param count    number of particles
     */
    public static void spawn(@Nullable Location loc, @Nullable Particle particle, int count) {
        if (loc == null || particle == null || loc.getWorld() == null) return;
        loc.getWorld().spawnParticle(particle, loc, count);
    }

    /**
     * Spawn particles with offset and speed (visible to all nearby players).
     *
     * @param loc      where to spawn
     * @param particle particle type
     * @param count    number of particles
     * @param offsetX  random spread on X (also direction when count is 0)
     * @param offsetY  random spread on Y
     * @param offsetZ  random spread on Z
     * @param speed    particle speed / "extra" data
     */
    public static void spawn(@Nullable Location loc, @Nullable Particle particle, int count,
                             double offsetX, double offsetY, double offsetZ, double speed) {
        if (loc == null || particle == null || loc.getWorld() == null) return;
        loc.getWorld().spawnParticle(particle, loc, count, offsetX, offsetY, offsetZ, speed);
    }

    /**
     * Spawn particles with offset, speed and typed data such as {@link Particle.DustOptions},
     * {@code BlockData}, or an {@code ItemStack} (visible to all nearby players).
     *
     * @param loc      where to spawn
     * @param particle particle type
     * @param count    number of particles
     * @param offsetX  random spread on X
     * @param offsetY  random spread on Y
     * @param offsetZ  random spread on Z
     * @param speed    particle speed / "extra" data
     * @param data     particle-specific data
     * @param <T>      data type expected by the particle
     */
    public static <T> void spawn(@Nullable Location loc, @Nullable Particle particle, int count,
                                 double offsetX, double offsetY, double offsetZ, double speed, @Nullable T data) {
        if (loc == null || particle == null || loc.getWorld() == null) return;
        loc.getWorld().spawnParticle(particle, loc, count, offsetX, offsetY, offsetZ, speed, data);
    }

    /**
     * Spawn particles visible only to the given player.
     *
     * @param player   the only viewer
     * @param loc      where to spawn
     * @param particle particle type
     * @param count    number of particles
     */
    public static void spawnToPlayer(@Nullable Player player, @Nullable Location loc, @Nullable Particle particle, int count) {
        if (player == null || loc == null || particle == null) return;
        player.spawnParticle(particle, loc, count);
    }

    /**
     * Spawn particles visible only to the given player, with offset, speed and typed data.
     *
     * @param player   the only viewer
     * @param loc      where to spawn
     * @param particle particle type
     * @param count    number of particles
     * @param offsetX  random spread on X
     * @param offsetY  random spread on Y
     * @param offsetZ  random spread on Z
     * @param speed    particle speed / "extra" data
     * @param data     particle-specific data
     * @param <T>      data type expected by the particle
     */
    public static <T> void spawnToPlayer(@Nullable Player player, @Nullable Location loc, @Nullable Particle particle, int count,
                                         double offsetX, double offsetY, double offsetZ, double speed, @Nullable T data) {
        if (player == null || loc == null || particle == null) return;
        player.spawnParticle(particle, loc, count, offsetX, offsetY, offsetZ, speed, data);
    }

    /**
     * Spawn a single particle at every location in the list. Pairs directly with the shapes
     * produced by {@code LocationUtil} (circle, line, sphere, helix, ...).
     *
     * @param locations points to draw at
     * @param particle  particle type
     */
    public static void spawnAll(@Nullable List<Location> locations, @Nullable Particle particle) {
        spawnAll(locations, particle, 1);
    }

    /**
     * Spawn particles at every location in the list.
     *
     * @param locations points to draw at
     * @param particle  particle type
     * @param count     number of particles per point
     */
    public static void spawnAll(@Nullable List<Location> locations, @Nullable Particle particle, int count) {
        if (locations == null || particle == null) return;
        for (int i = 0, n = locations.size(); i < n; i++) {
            spawn(locations.get(i), particle, count);
        }
    }

    /**
     * Spawn particles at every location in the list, with offset, speed and typed data.
     *
     * @param locations points to draw at
     * @param particle  particle type
     * @param count     number of particles per point
     * @param offsetX   random spread on X
     * @param offsetY   random spread on Y
     * @param offsetZ   random spread on Z
     * @param speed     particle speed / "extra" data
     * @param data      particle-specific data
     * @param <T>       data type expected by the particle
     */
    public static <T> void spawnAll(@Nullable List<Location> locations, @Nullable Particle particle, int count,
                                    double offsetX, double offsetY, double offsetZ, double speed, @Nullable T data) {
        if (locations == null || particle == null) return;
        for (int i = 0, n = locations.size(); i < n; i++) {
            spawn(locations.get(i), particle, count, offsetX, offsetY, offsetZ, speed, data);
        }
    }

    /**
     * Spawn a single particle at every location in the list, visible only to the given player.
     *
     * @param player    the only viewer
     * @param locations points to draw at
     * @param particle  particle type
     */
    public static void spawnAllToPlayer(@Nullable Player player, @Nullable List<Location> locations, @Nullable Particle particle) {
        if (player == null || locations == null || particle == null) return;
        for (int i = 0, n = locations.size(); i < n; i++) {
            spawnToPlayer(player, locations.get(i), particle, 1);
        }
    }

    /**
     * Spawn a single coloured dust particle at a location.
     * <p>
     * The dust particle constant is supplied by the caller for version safety
     * (e.g. {@code Particle.REDSTONE} on 1.20.4 and below, {@code Particle.DUST} on 1.20.5+).
     *
     * @param loc          where to spawn
     * @param dustParticle the dust particle constant for the running server version
     * @param color        dust colour
     * @param size         dust size
     */
    public static void spawnDust(@Nullable Location loc, @Nullable Particle dustParticle, @Nullable Color color, float size) {
        if (loc == null || dustParticle == null || color == null || loc.getWorld() == null) return;
        Particle.DustOptions options = new Particle.DustOptions(color, size);
        loc.getWorld().spawnParticle(dustParticle, loc, 1, 0, 0, 0, 0, options);
    }

    /**
     * Spawn a coloured dust particle at every location in the list.
     *
     * @param locations    points to draw at
     * @param dustParticle the dust particle constant for the running server version
     * @param color        dust colour
     * @param size         dust size
     */
    public static void spawnDustAll(@Nullable List<Location> locations, @Nullable Particle dustParticle, @Nullable Color color, float size) {
        if (locations == null || dustParticle == null || color == null) return;
        // build the DustOptions once and reuse it for every point
        Particle.DustOptions options = new Particle.DustOptions(color, size);
        for (int i = 0, n = locations.size(); i < n; i++) {
            Location loc = locations.get(i);
            if (loc == null || loc.getWorld() == null) continue;
            loc.getWorld().spawnParticle(dustParticle, loc, 1, 0, 0, 0, 0, options);
        }
    }
}
