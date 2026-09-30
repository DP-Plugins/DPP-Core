package com.darksoldier1404.dppc.data.sync;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SyncMessageTest {

    @Test
    void roundTripsAllFields() {
        SyncMessage msg = new SyncMessage("srv1", "Plugin:data", SyncOp.UPSERT, "abc-123");
        SyncMessage back = SyncMessage.decode(msg.encode());
        assertEquals("srv1", back.getServerId());
        assertEquals("Plugin:data", back.getContainerId());
        assertEquals(SyncOp.UPSERT, back.getOp());
        assertEquals("abc-123", back.getKey());
    }

    @Test
    void keyMayContainDelimiter() {
        SyncMessage msg = new SyncMessage("srv1", "Plugin:data", SyncOp.DELETE, "weird|key|name");
        SyncMessage back = SyncMessage.decode(msg.encode());
        assertEquals("weird|key|name", back.getKey());
        assertEquals(SyncOp.DELETE, back.getOp());
    }

    @Test
    void decodeRejectsMalformed() {
        assertNull(SyncMessage.decode(null));
        assertNull(SyncMessage.decode("only|three|parts"));
        assertNull(SyncMessage.decode("srv|cid|NOTANOP|key"));
    }
}
