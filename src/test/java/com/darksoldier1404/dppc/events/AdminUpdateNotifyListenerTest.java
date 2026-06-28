package com.darksoldier1404.dppc.events;

import com.darksoldier1404.dppc.support.PluginTest;
import com.darksoldier1404.dppc.utils.PluginUtil;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminUpdateNotifyListenerTest extends PluginTest {

    private boolean containsByName(java.util.List<JavaPlugin> plugins, String name) {
        return plugins.stream().anyMatch(pl -> name.equals(pl.getName()));
    }

    @Test
    void outdatedPluginIsCollectedWhenNewerVersionCached() {
        String name = plugin.getName();
        PluginUtil.cacheLatestVersion(name, "999.0.0");
        assertTrue(containsByName(AdminUpdateNotifyListener.getOutdatedPlugins(), name));
    }

    @Test
    void upToDatePluginIsNotCollected() {
        String name = plugin.getName();
        PluginUtil.cacheLatestVersion(name, plugin.getDescription().getVersion());
        assertFalse(containsByName(AdminUpdateNotifyListener.getOutdatedPlugins(), name));
    }

    @Test
    void unverifiedPluginIsNotCollected() {
        String name = plugin.getName();
        PluginUtil.cacheLatestVersion(name, "0.0.0");
        assertFalse(containsByName(AdminUpdateNotifyListener.getOutdatedPlugins(), name));
    }
}
