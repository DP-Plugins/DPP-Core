package com.darksoldier1404.dppc.api.logger;

import org.junit.jupiter.api.Test;

import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DLogContextTest {

    @Test
    void ofStoresContextAndLevel() {
        DLogContext c = DLogContext.of("something happened", Level.INFO);
        assertEquals("something happened", c.getContext());
    }

    @Test
    void formattedContextIncludesLevel() {
        assertEquals("[WARNING] oops", DLogContext.of("oops", Level.WARNING).getFormatedContext());
    }

    @Test
    void formattedFullContextIncludesLevelTimestampAndMessage() {
        DLogContext c = DLogContext.of("msg", Level.SEVERE);
        String full = c.getFormatedFullContext();
        assertTrue(full.startsWith("[SEVERE] ["), full);
        assertTrue(full.endsWith("] msg"), full);
    }

    @Test
    void formattedTimestampHasExpectedShape() {
        // HH-mm-ss (8 chars) + '-' + 6 nano digits
        String ts = DLogContext.of("x", Level.INFO).getFormatedTimestamp();
        assertTrue(ts.matches("\\d{2}-\\d{2}-\\d{2}-\\d{6}"), ts);
    }
}
