package com.darksoldier1404.dppc.lang;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DLangContextTest {

    private DLangContext ctx() {
        return new DLangContext(null, Locale.US);
    }

    @Test
    void storesLocale() {
        assertEquals(Locale.US, ctx().getLang());
    }

    @Test
    void initDefaultValuesPopulatesBothMaps() {
        DLangContext c = ctx();
        c.initDefaultValues("greeting", "hello");
        assertEquals("hello", c.getValue("greeting"));
        assertEquals("hello", c.getDefaultValue("greeting"));
        assertTrue(c.hasValue("greeting"));
    }

    @Test
    void valueOverridesDefault() {
        DLangContext c = ctx();
        c.setDefaultValue("k", "default");
        c.setValue("k", "override");
        assertEquals("override", c.getValue("k"));
        assertEquals("default", c.getDefaultValue("k"));
    }

    @Test
    void getValueFallsBackToDefault() {
        DLangContext c = ctx();
        c.setDefaultValue("k", "default");
        assertEquals("default", c.getValue("k"));
    }

    @Test
    void removeValueFallsBackToDefault() {
        DLangContext c = ctx();
        c.initDefaultValues("k", "v");
        c.setValue("k", "override");
        c.removeValue("k");
        assertEquals("v", c.getValue("k"));
    }

    @Test
    void clearEmptiesEverything() {
        DLangContext c = ctx();
        c.initDefaultValues("k", "v");
        c.clear();
        assertFalse(c.hasValue("k"));
        assertTrue(c.getValueMap().isEmpty());
        assertTrue(c.getDefaultValueMap().isEmpty());
    }
}
