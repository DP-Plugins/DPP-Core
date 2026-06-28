package com.darksoldier1404.dppc.lang;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link DLang} lookup logic with manually registered language contexts,
 * avoiding the resource/file-backed {@code initPluginLang} path.
 */
class DLangTest {

    private DLang langWithEntry(String key, String value) {
        DLang lang = new DLang();
        DLangContext ctx = new DLangContext(null, Locale.forLanguageTag("en-US"));
        ctx.initDefaultValues(key, value);
        lang.getLangContexts().add(ctx);
        lang.setCurrentLang(Locale.forLanguageTag("en-US"));
        return lang;
    }

    @Test
    void findReturnsValueForCurrentLanguage() {
        DLang lang = langWithEntry("greeting", "hello");
        assertEquals("hello", lang.find("greeting"));
        assertTrue(lang.has("greeting"));
    }

    @Test
    void findMissingKeyReturnsErrorString() {
        DLang lang = langWithEntry("greeting", "hello");
        assertTrue(lang.find("absent").contains("Language key not found"));
        assertFalse(lang.has("absent"));
    }

    @Test
    void getAppliesColor() {
        DLang lang = langWithEntry("c", "&aGreen");
        assertEquals("§aGreen", lang.get("c"));
    }

    @Test
    void getWithArgsSubstitutesPlaceholders() {
        DLang lang = langWithEntry("welcome", "Hi {0}, you have {1} mail");
        assertEquals("Hi Steve, you have 3 mail", lang.getWithArgs("welcome", "Steve", "3"));
    }

    @Test
    void differentLanguageIsNotMatched() {
        DLang lang = langWithEntry("greeting", "hello");
        lang.setCurrentLang(Locale.forLanguageTag("ko-KR"));
        assertFalse(lang.has("greeting"));
    }

    @Test
    void defaultCurrentLanguageIsEnglish() {
        assertEquals("en", new DLang().getCurrentLang().getLanguage());
    }
}
