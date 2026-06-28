package com.darksoldier1404.dppc.api.logger;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link DLogNode} with {@code printToConsole = false} so no plugin
 * logger is touched; the node can therefore be built with a null plugin.
 */
class DLogNodeTest {

    @Test
    void logsAreAccumulated() {
        DLogNode node = new DLogNode(null);
        node.info("info-msg", false);
        node.warning("warn-msg", false);
        node.severe("severe-msg", false);
        assertEquals(3, node.getLogs().size());
    }

    @Test
    void clearEmptiesLogs() {
        DLogNode node = new DLogNode(null);
        node.info("x", false);
        node.clear();
        assertTrue(node.getLogs().isEmpty());
    }

    @Test
    void serializeWritesFormattedContext() {
        DLogNode node = new DLogNode(null);
        node.info("hello", false);
        YamlConfiguration data = node.serialize();
        String key = node.getLogs().get(0).getFormatedTimestamp();
        assertEquals("[INFO] hello", data.getString(key));
    }
}
