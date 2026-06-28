package com.darksoldier1404.dppc.api.essentials.economy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NullProviderTest {

    @Test
    void reportsDisabledNamedNone() {
        NullProvider p = new NullProvider();
        assertEquals("None", p.getName());
        assertFalse(p.isEnabled());
    }

    @Test
    void balanceQueriesReturnZeroAndFalse() {
        NullProvider p = new NullProvider();
        assertEquals(BigDecimal.ZERO, p.getMoney(null));
        assertFalse(p.has(null, BigDecimal.TEN));
    }

    @Test
    void mutationsAreNoOpsAndDoNotThrow() {
        NullProvider p = new NullProvider();
        p.add(null, BigDecimal.TEN);
        p.take(null, BigDecimal.TEN);
        p.set(null, BigDecimal.TEN);
        // Still zero / disabled afterwards.
        assertEquals(BigDecimal.ZERO, p.getMoney(null));
    }
}
