package com.darksoldier1404.dppc.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TupleTest {

    @Test
    void constructorStoresValues() {
        Tuple<String, Integer> t = new Tuple<>("a", 1);
        assertEquals("a", t.getA());
        assertEquals(1, t.getB());
    }

    @Test
    void ofFactoryMatchesConstructor() {
        Tuple<String, Integer> t = Tuple.of("x", 42);
        assertEquals("x", t.getA());
        assertEquals(42, t.getB());
    }

    @Test
    void settersUpdateValues() {
        Tuple<String, Integer> t = Tuple.of("a", 1);
        t.setA("b");
        t.setB(2);
        assertEquals("b", t.getA());
        assertEquals(2, t.getB());
    }

    @Test
    void supportsNullValues() {
        Tuple<String, Integer> t = Tuple.of(null, null);
        assertNull(t.getA());
        assertNull(t.getB());
    }

    @Test
    void toStringFormat() {
        assertEquals("[a, 1]", Tuple.of("a", 1).toString());
    }
}
