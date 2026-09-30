package com.darksoldier1404.dppc.utils;

import com.darksoldier1404.dppc.support.MockServerTest;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * MockBukkit does not record particle spawns, so these tests only verify the null/empty
 * guard paths (which never reach {@code World#spawnParticle}) are safe and never throw.
 */
class ParticleUtilTest extends MockServerTest {

    @Test
    void singleSpawnGuardsAreSafe() {
        assertDoesNotThrow(() -> ParticleUtil.spawn(null, Particle.FLAME));
        assertDoesNotThrow(() -> ParticleUtil.spawn(new Location(null, 0, 0, 0), Particle.FLAME));
        assertDoesNotThrow(() -> ParticleUtil.spawn(new Location(null, 0, 0, 0), null, 5));
    }

    @Test
    void listSpawnGuardsAreSafe() {
        assertDoesNotThrow(() -> ParticleUtil.spawnAll(null, Particle.FLAME));
        assertDoesNotThrow(() -> ParticleUtil.spawnAll(Collections.emptyList(), Particle.FLAME));
        assertDoesNotThrow(() -> ParticleUtil.spawnAll(Collections.emptyList(), Particle.FLAME, 3, 0, 0, 0, 0, null));
    }

    @Test
    void playerSpawnGuardsAreSafe() {
        assertDoesNotThrow(() -> ParticleUtil.spawnToPlayer(null, new Location(null, 0, 0, 0), Particle.FLAME, 1));
        assertDoesNotThrow(() -> ParticleUtil.spawnAllToPlayer(null, Collections.emptyList(), Particle.FLAME));
    }

    @Test
    void dustGuardsAreSafe() {
        assertDoesNotThrow(() -> ParticleUtil.spawnDust(null, Particle.FLAME, Color.RED, 1.0f));
        assertDoesNotThrow(() -> ParticleUtil.spawnDust(new Location(null, 0, 0, 0), Particle.FLAME, null, 1.0f));
        assertDoesNotThrow(() -> ParticleUtil.spawnDustAll(Collections.emptyList(), Particle.FLAME, Color.RED, 1.0f));
    }
}
