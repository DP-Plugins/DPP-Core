package com.darksoldier1404.dppc.support;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for tests that need a mocked Bukkit server (ItemFactory, registries,
 * players, etc.) but do not need the DPP-Core plugin itself loaded.
 */
public abstract class MockServerTest {

    protected ServerMock server;

    @BeforeEach
    void startServer() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void stopServer() {
        MockBukkit.unmock();
    }

    /**
     * Adds a player guaranteed to be located in a freshly created world, so code
     * that touches {@code player.getWorld()} / location does not hit a null world.
     */
    protected PlayerMock spawnPlayerInWorld(String name) {
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer(name);
        player.teleport(new Location(world, 0, 64, 0));
        return player;
    }
}
