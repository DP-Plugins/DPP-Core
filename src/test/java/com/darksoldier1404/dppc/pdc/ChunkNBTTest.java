package com.darksoldier1404.dppc.pdc;

import be.seeseemelk.mockbukkit.WorldMock;
import com.darksoldier1404.dppc.support.MockServerTest;
import org.bukkit.Chunk;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@link ChunkNBT} facade works on a real (mock) chunk.
 */
class ChunkNBTTest extends MockServerTest {

    @Test
    void stringTagRoundTripOnChunk() {
        WorldMock world = server.addSimpleWorld("world");
        Chunk chunk = world.getChunkAt(0, 0);
        ChunkNBT.setStringTag(chunk, "owner", "Steve");
        ChunkNBT.setBooleanTag(chunk, "claimed", true);
        assertEquals("Steve", ChunkNBT.getStringTag(chunk, "owner"));
        assertTrue(ChunkNBT.getBooleanTag(chunk, "claimed"));
        assertTrue(ChunkNBT.hasTagKey(chunk, "owner"));
    }
}
