package com.darksoldier1404.dppc.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TripleTest {

    @Test
    void ofStoresAllValues() {
        Triple<String, Integer, Boolean> t = Triple.of("a", 1, true);
        assertEquals("a", t.getA());
        assertEquals(1, t.getB());
        assertEquals(true, t.getC());
    }

    @Test
    void settersUpdateValues() {
        Triple<String, Integer, Boolean> t = Triple.of("a", 1, true);
        t.setA("b");
        t.setB(2);
        t.setC(false);
        assertEquals("b", t.getA());
        assertEquals(2, t.getB());
        assertEquals(false, t.getC());
    }

    @Test
    void toStringFormat() {
        assertEquals("[a, 1, true]", Triple.of("a", 1, true).toString());
    }
}
