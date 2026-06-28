package com.darksoldier1404.dppc;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test verifying that DPP-Core boots inside a MockBukkit server with no
 * soft-depend plugins present. All optional integrations (Essentials, LuckPerms,
 * WorldGuard, PlaceholderAPI, Vault) are expected to be skipped gracefully.
 */
class DPPCoreSmokeTest {

    private ServerMock server;
    private DPPCore plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(DPPCore.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void pluginEnablesSuccessfully() {
        assertTrue(plugin.isEnabled(), "DPP-Core should be enabled after loading");
    }

    @Test
    void staticInstanceIsWired() {
        assertSame(plugin, DPPCore.getInstance(), "DPPCore.getInstance() should return the loaded plugin");
    }

    @Test
    void declaredCommandsAreRegistered() {
        assertNotNull(plugin.getCommand("dppc"), "dppc command should be registered");
        assertNotNull(plugin.getCommand("dppca"), "dppca command should be registered");
        assertNotNull(plugin.getCommand("dppcp"), "dppcp command should be registered");
        assertNotNull(plugin.getCommand("dppcdi"), "dppcdi command should be registered");
    }
}
