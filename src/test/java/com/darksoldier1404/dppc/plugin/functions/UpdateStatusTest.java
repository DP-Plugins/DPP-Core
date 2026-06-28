package com.darksoldier1404.dppc.plugin.functions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UpdateStatusTest {

    @Test
    void equalVersionsAreUpToDate() {
        assertEquals(UpdateStatus.UP_TO_DATE, UpdateStatus.of("1.0.0", "1.0.0"));
    }

    @Test
    void higherLatestIsOutdated() {
        assertEquals(UpdateStatus.OUTDATED, UpdateStatus.of("1.0.0", "1.0.1"));
        assertEquals(UpdateStatus.OUTDATED, UpdateStatus.of("1.2.9", "1.3.0"));
    }

    @Test
    void installedNewerThanLatestIsUpToDate() {
        assertEquals(UpdateStatus.UP_TO_DATE, UpdateStatus.of("1.2.0", "1.1.9"));
    }

    @Test
    void zeroVersionMeansUnverified() {
        assertEquals(UpdateStatus.UNKNOWN, UpdateStatus.of("1.0.0", "0.0.0"));
    }

    @Test
    void nullOrEmptyLatestIsUnknown() {
        assertEquals(UpdateStatus.UNKNOWN, UpdateStatus.of("1.0.0", null));
        assertEquals(UpdateStatus.UNKNOWN, UpdateStatus.of("1.0.0", ""));
    }

    @Test
    void nonNumericVersionsAreUnknown() {
        assertEquals(UpdateStatus.UNKNOWN, UpdateStatus.of("1.0.0", "1.0-SNAPSHOT"));
        assertEquals(UpdateStatus.UNKNOWN, UpdateStatus.of("abc", "1.0.0"));
        assertEquals(UpdateStatus.UNKNOWN, UpdateStatus.of(null, "1.0.0"));
    }
}
