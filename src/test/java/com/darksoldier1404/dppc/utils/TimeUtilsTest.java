package com.darksoldier1404.dppc.utils;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeUtilsTest {

    private static final long DAY_MS = 1000L * 60 * 60 * 24;

    @Test
    void parseFormatRoundTrip() throws Exception {
        Date d = TimeUtils.parseDate("2024-01-15 10:30:00");
        assertEquals("2024-01-15 10:30:00", TimeUtils.formatDate(d));
    }

    @Test
    void parseAcceptsDateOnlyFormat() throws Exception {
        Date d = TimeUtils.parseDate("2024-01-15");
        assertEquals("2024-01-15 00:00:00", TimeUtils.formatDate(d));
    }

    @Test
    void parseInvalidStringThrows() {
        assertThrows(Exception.class, () -> TimeUtils.parseDate("not-a-date"));
    }

    @Test
    void diffMethodsAreTimezoneIndependent() {
        Date start = new Date(0L);
        Date end = new Date(2 * DAY_MS);
        assertEquals(2, TimeUtils.getDaysBetween(start, end));
        assertEquals(48, TimeUtils.getHoursBetween(start, end));
        assertEquals(48 * 60, TimeUtils.getMinutesBetween(start, end));
        assertEquals(48 * 60 * 60, TimeUtils.getSecondsBetween(start, end));
    }

    @Test
    void diffCanBeNegative() {
        Date start = new Date(2 * DAY_MS);
        Date end = new Date(0L);
        assertEquals(-2, TimeUtils.getDaysBetween(start, end));
    }

    @Test
    void yearsAndMonthsBetween() throws Exception {
        Date start = TimeUtils.parseDate("2020-01-01 00:00:00");
        Date end = TimeUtils.parseDate("2022-03-01 00:00:00");
        assertEquals(2, TimeUtils.getYearsBetween(start, end));
        assertEquals(2 * 12 + 2, TimeUtils.getMonthsBetween(start, end));
    }

    @Test
    void passedHelpersUseNow() {
        Date longAgo = new Date(0L);
        assertTrue(TimeUtils.getDaysPassed(longAgo) > 0);
        assertTrue(TimeUtils.getYearsPassed(longAgo) > 0);
    }
}
