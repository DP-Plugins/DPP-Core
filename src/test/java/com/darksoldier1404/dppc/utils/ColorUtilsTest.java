package com.darksoldier1404.dppc.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorUtilsTest {

    @Test
    void translatesAmpersandCodes() {
        assertEquals("§aHello", ColorUtils.applyColor("&aHello"));
    }

    @Test
    void leavesPlainTextUnchanged() {
        assertEquals("Hello world", ColorUtils.applyColor("Hello world"));
    }

    @Test
    void mapsKoreanJamoToColorLetters() {
        // 'ㅁ' maps to 'a', so "&ㅁ" becomes "&a" -> section-a
        assertEquals("§aHello", ColorUtils.applyColor("&ㅁHello"));
        // 'ㄱ' maps to 'r' (reset)
        assertEquals("§r", ColorUtils.applyColor("&ㄱ"));
    }

    @Test
    void convertsHexTags() {
        String result = ColorUtils.applyColor("<#FF00AA>Hi");
        // Hex is expanded into the section-x sequence; the literal tag must be gone.
        assertTrue(result.contains("Hi"));
        assertTrue(result.startsWith("§x"));
        assertEquals(-1, result.indexOf("<#"));
    }
}
