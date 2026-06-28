package com.darksoldier1404.dppc.data;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Minimal {@link DataCargo} implementation with a public no-arg constructor,
 * used by the CUSTOM-type data container tests.
 */
public class TestCargo implements DataCargo {

    public String value = "default";

    public TestCargo() {
    }

    @Override
    public Object serialize() {
        YamlConfiguration data = new YamlConfiguration();
        data.set("value", value);
        return data;
    }

    @Override
    public Object deserialize(YamlConfiguration data) {
        this.value = data.getString("value", "default");
        return this;
    }
}
