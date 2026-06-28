package com.darksoldier1404.dppc.builder.command;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommandArgumentsTest {

    private CommandArguments args(Map<ArgumentIndex, Object> map) {
        return new CommandArguments(map);
    }

    @Test
    void typedGettersReturnMatchingValues() {
        Map<ArgumentIndex, Object> map = new EnumMap<>(ArgumentIndex.class);
        map.put(ArgumentIndex.ARG_0, "hello");
        map.put(ArgumentIndex.ARG_1, 42);
        map.put(ArgumentIndex.ARG_2, true);
        map.put(ArgumentIndex.ARG_3, 3.14d);
        map.put(ArgumentIndex.ARG_4, Material.DIAMOND);

        CommandArguments a = args(map);
        assertEquals("hello", a.getString(ArgumentIndex.ARG_0));
        assertEquals(42, a.getInteger(ArgumentIndex.ARG_1));
        assertEquals(true, a.getBoolean(ArgumentIndex.ARG_2));
        assertEquals(3.14d, a.getDouble(ArgumentIndex.ARG_3));
        assertEquals(Material.DIAMOND, a.getMaterial(ArgumentIndex.ARG_4));
    }

    @Test
    void wrongTypeReturnsNull() {
        Map<ArgumentIndex, Object> map = new EnumMap<>(ArgumentIndex.class);
        map.put(ArgumentIndex.ARG_0, "not-an-int");
        CommandArguments a = args(map);
        assertNull(a.getInteger(ArgumentIndex.ARG_0));
        assertNull(a.getBoolean(ArgumentIndex.ARG_0));
        assertNull(a.getMaterial(ArgumentIndex.ARG_0));
    }

    @Test
    void missingKeyReturnsNull() {
        CommandArguments a = args(new EnumMap<>(ArgumentIndex.class));
        assertNull(a.getString(ArgumentIndex.ARG_5));
        assertNull(a.get(ArgumentIndex.ARG_5));
    }

    @Test
    void getStringArrayReturnsArray() {
        Map<ArgumentIndex, Object> map = new EnumMap<>(ArgumentIndex.class);
        String[] value = {"a", "b"};
        map.put(ArgumentIndex.ARG_0, value);
        assertArrayEquals(value, args(map).getStringArray(ArgumentIndex.ARG_0));
    }

    @Test
    void genericGetReturnsRawObject() {
        Map<ArgumentIndex, Object> map = new EnumMap<>(ArgumentIndex.class);
        Object value = new Object();
        map.put(ArgumentIndex.ARG_0, value);
        assertEquals(value, args(map).get(ArgumentIndex.ARG_0));
    }
}
