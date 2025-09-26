/*
 * Special thanks to: tr7zw, SoSeDiK, Broken arrow
 * From TR's Mod Workshop
 */
package com.darksoldier1404.dppc.data;

import com.darksoldier1404.dppc.lang.DLang;
import com.darksoldier1404.dppc.utils.ColorUtils;
import com.darksoldier1404.dppc.utils.ConfigUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DPlugin extends JavaPlugin {
    public YamlConfiguration config;
    public String prefix;
    private final Map<String, DataContainer<?, ?>> data = new HashMap<>();
    private final boolean useDLang;
    private DLang lang;

    public DPlugin() {
        this(false);
    }

    public DPlugin(boolean useDLang) {
        this.useDLang = useDLang;
    }

    public void init() {
        this.config = ConfigUtils.loadDefaultPluginConfig(this);
        this.prefix = ColorUtils.applyColor(config.getString("Settings.prefix"));
        if (this.useDLang) {
            lang = new DLang();
            if (this.config.getString("Settings.Lang") == null) {
                this.config.set("Settings.Lang", "en_US");
            }

            lang.initPluginLang(this);
            lang.setCurrentLang(Locale.forLanguageTag(this.config.getString("Settings.Lang").replace("_", "-")));
        }
    }

    @Override
    public @NotNull YamlConfiguration getConfig() {
        return config;
    }

    public void setConfig(YamlConfiguration config) {
        this.config = config;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void set(String key, DataContainer value) {
        data.put(key, value);
    }

    public DataContainer get(String key) {
        return data.get(key);
    }

    public void reload() {
        init();
    }

    public void saveDataContainer() {
        ConfigUtils.savePluginConfig(this, config);
        for (Map.Entry<String, DataContainer<?, ?>> entry : data.entrySet()) {
            DataContainer<?, ?> data = entry.getValue();
            data.saveAll();
        }
    }

    public <K, V> DataContainer<K, V> loadDataContainer(DataContainer<K, V> container, Class<?> clazz) {
        try {
            data.put(container.getPath(), container);
            return container.loadAll(clazz);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isUseDLang() {
        return useDLang;
    }

    public DLang getLang() {
        return this.lang;
    }
}
