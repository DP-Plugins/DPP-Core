package com.darksoldier1404.dppc.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuadrupleTest {

    @Test
    void ofStoresAllValues() {
        Quadruple<String, Integer, Boolean, Double> q = Quadruple.of("a", 1, true, 2.5);
        assertEquals("a", q.getA());
        assertEquals(1, q.getB());
        assertEquals(true, q.getC());
        assertEquals(2.5, q.getD());
    }

    @Test
    void settersUpdateValues() {
        Quadruple<String, Integer, Boolean, Double> q = Quadruple.of("a", 1, true, 2.5);
        q.setA("b");
        q.setB(2);
        q.setC(false);
        q.setD(9.9);
        assertEquals("b", q.getA());
        assertEquals(2, q.getB());
        assertEquals(false, q.getC());
        assertEquals(9.9, q.getD());
    }

    @Test
    void toStringFormat() {
        assertEquals("[a, 1, true, 2.5]", Quadruple.of("a", 1, true, 2.5).toString());
    }
}
