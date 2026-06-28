package com.darksoldier1404.dppc.support;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.darksoldier1404.dppc.DPPCore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for tests that need the DPP-Core plugin (and therefore a real
 * {@code DPlugin} instance, data folder, config and logger) fully loaded.
 */
public abstract class PluginTest {

    protected ServerMock server;
    protected DPPCore plugin;

    @BeforeEach
    void loadPlugin() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(DPPCore.class);
    }

    @AfterEach
    void unload() {
        MockBukkit.unmock();
    }
}
