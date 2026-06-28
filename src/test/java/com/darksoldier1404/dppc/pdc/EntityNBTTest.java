package com.darksoldier1404.dppc.pdc;

import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.darksoldier1404.dppc.support.MockServerTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@link EntityNBT} facade works on a real (mock) entity.
 */
class EntityNBTTest extends MockServerTest {

    @Test
    void stringAndIntTagsRoundTripOnEntity() {
        PlayerMock entity = server.addPlayer("Steve");
        EntityNBT.setStringTag(entity, "owner", "Steve");
        EntityNBT.setIntTag(entity, "level", 5);
        assertEquals("Steve", EntityNBT.getStringTag(entity, "owner"));
        assertEquals(5, EntityNBT.getIntegerTag(entity, "level"));
    }

    @Test
    void hasTagRemoveAndKeys() {
        PlayerMock entity = server.addPlayer("Steve");
        EntityNBT.setStringTag(entity, "owner", "Steve");
        assertTrue(EntityNBT.hasTagKey(entity, "owner"));
        assertTrue(EntityNBT.getKeys(entity).contains("owner"));
        EntityNBT.removeTag(entity, "owner");
        assertFalse(EntityNBT.hasTagKey(entity, "owner"));
    }
}
