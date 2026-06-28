package com.darksoldier1404.dppc.builder.command;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArgumentTest {

    @Test
    void requiredFlagIsStored() {
        Argument<String> a = new Argument<>(ArgumentIndex.ARG_0, ArgumentType.STRING, true, Collections.emptyList());
        assertTrue(a.isRequired());
        Argument<String> b = new Argument<>(ArgumentIndex.ARG_0, ArgumentType.STRING, false, Collections.emptyList());
        assertFalse(b.isRequired());
    }

    @Test
    void stringSuggestionsPassThrough() {
        Argument<String> a = new Argument<>(ArgumentIndex.ARG_0, ArgumentType.STRING, true, Arrays.asList("one", "two"));
        assertEquals(Arrays.asList("one", "two"), a.getSuggestionsAsStringList());
    }

    @Test
    void integerSuggestionsAreStringified() {
        Argument<Integer> a = new Argument<>(ArgumentIndex.ARG_0, ArgumentType.INTEGER, true, Arrays.asList(1, 2, 3));
        assertEquals(Arrays.asList("1", "2", "3"), a.getSuggestionsAsStringList());
    }

    @Test
    void booleanSuggestionsAreStringified() {
        Argument<Boolean> a = new Argument<>(ArgumentIndex.ARG_0, ArgumentType.BOOLEAN, true, Arrays.asList(true, false));
        assertEquals(Arrays.asList("true", "false"), a.getSuggestionsAsStringList());
    }

    @Test
    void materialSuggestionsUseEnumName() {
        Argument<Material> a = new Argument<>(ArgumentIndex.ARG_0, ArgumentType.MATERIAL, true, Arrays.asList(Material.STONE, Material.DIRT));
        assertEquals(Arrays.asList("STONE", "DIRT"), a.getSuggestionsAsStringList());
    }

    @Test
    void nullStaticSuggestionsYieldEmptyList() {
        Argument<String> a = new Argument<>(ArgumentIndex.ARG_0, ArgumentType.STRING, true, (java.util.Collection<String>) null);
        List<String> result = a.getSuggestionsAsStringList();
        assertTrue(result.isEmpty());
    }
}
