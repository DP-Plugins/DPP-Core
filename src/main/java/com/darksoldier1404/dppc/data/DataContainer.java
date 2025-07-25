package com.darksoldier1404.dppc.data;

import com.darksoldier1404.dppc.lang.DLang;
import com.darksoldier1404.dppc.utils.ColorUtils;
import com.darksoldier1404.dppc.utils.ConfigUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("all")
public class DataContainer {
    private final JavaPlugin plugin;
    private YamlConfiguration config;
    private DLang lang;
    private String prefix;
    private final Map<String, Object> data = new HashMap<>();
    private boolean useDLang = false;

    public DataContainer(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = ConfigUtils.loadDefaultPluginConfig(plugin);
        this.prefix = ColorUtils.applyColor(config.getString("Settings.prefix"));
        this.lang = new DLang(config.getString("Settings.Lang") == null ? "English" : config.getString("Settings.Lang"), plugin);
        if (this.useDLang) {
            if (config.getString("Settings.Lang") == null) {
                config.set("Settings.Lang", "English");
            }
        }
    }

    public DataContainer(JavaPlugin plugin, boolean useDLang) {
        this.plugin = plugin;
        this.config = ConfigUtils.loadDefaultPluginConfig(plugin);
        this.prefix = ColorUtils.applyColor(config.getString("Settings.prefix"));
        this.useDLang = useDLang;
        if (this.useDLang) {
            this.lang = new DLang(config.getString("Settings.Lang") == null ? "English" : config.getString("Settings.Lang"), plugin);
            if (config.getString("Settings.Lang") == null) {
                config.set("Settings.Lang", "English");
            }
        }
    }

    public YamlConfiguration getConfig() {
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

    public DLang getLang() {
        return lang;
    }

    public void setLang(DLang lang) {
        this.lang = lang;
    }

    public void initUserData(UUID uuid) {
        if (!hasUserData(uuid)) {
            YamlConfiguration data = ConfigUtils.loadCustomData(plugin, String.valueOf(uuid), "udata");
            addUserData(uuid, data);
        }
    }

    public void addUserData(UUID uuid, YamlConfiguration data) {
        if (this.data.containsKey("udata")) {
            Map<UUID, YamlConfiguration> udata = (Map<UUID, YamlConfiguration>) this.data.get("udata");
            udata.put(uuid, data);
        } else {
            Map<UUID, YamlConfiguration> udata = new HashMap<>();
            udata.put(uuid, data);
            this.data.put("udata", udata);
        }
    }

    public void removeUserData(UUID uuid) {
        if (this.data.containsKey("udata")) {
            Map<UUID, YamlConfiguration> udata = (Map<UUID, YamlConfiguration>) this.data.get("udata");
            udata.remove(uuid);
        }
    }

    @Nullable
    public YamlConfiguration getUserData(UUID uuid) {
        if (this.data.containsKey("udata")) {
            Map<UUID, YamlConfiguration> udata = (Map<UUID, YamlConfiguration>) this.data.get("udata");
            return udata.get(uuid);
        }
        return null;
    }

    public boolean hasUserData(UUID uuid) {
        if (this.data.containsKey("udata")) {
            Map<UUID, YamlConfiguration> udata = (Map<UUID, YamlConfiguration>) this.data.get("udata");
            return udata.containsKey(uuid);
        }
        return false;
    }

    public void clearUserData() {
        if (this.data.containsKey("udata")) {
            Map<UUID, YamlConfiguration> udata = (Map<UUID, YamlConfiguration>) this.data.get("udata");
            udata.clear();
        }
    }

    public void saveUserData(UUID uuid) {
        if (this.data.containsKey("udata")) {
            Map<UUID, YamlConfiguration> udata = (Map<UUID, YamlConfiguration>) this.data.get("udata");
            ConfigUtils.saveCustomData(plugin, udata.get(uuid), String.valueOf(uuid), "udata");
        }
    }

    public void saveAndLeave(UUID uuid) {
        if (this.data.containsKey("udata")) {
            Map<UUID, YamlConfiguration> udata = (Map<UUID, YamlConfiguration>) this.data.get("udata");
            ConfigUtils.saveCustomData(plugin, udata.get(uuid), String.valueOf(uuid), "udata");
            udata.remove(uuid);
        }
    }

    public void saveAllUserData() {
        if (this.data.containsKey("udata")) {
            Map<UUID, YamlConfiguration> udata = (Map<UUID, YamlConfiguration>) this.data.get("udata");
            for (Map.Entry<UUID, YamlConfiguration> entry : udata.entrySet()) {
                ConfigUtils.saveCustomData(plugin, entry.getValue(), String.valueOf(entry.getKey()), "udata");
            }
        }
    }

    public void set(String key, Object value) {
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public void reload() {
        config = ConfigUtils.reloadPluginConfig(plugin, config);
        prefix = ColorUtils.applyColor(config.getString("Settings.prefix"));
        if (useDLang) {
            lang = new DLang(config.getString("Settings.Lang") == null ? "English" : config.getString("Settings.Lang"), plugin);
            if (config.getString("Settings.Lang") == null) {
                config.set("Settings.Lang", "English");
            }
        }
    }

    public void save() {
        ConfigUtils.savePluginConfig(plugin, config);
        saveAllUserData();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() instanceof DataCargo) {
                DataCargo cargo = (DataCargo) entry.getValue();
                cargo.save();
            }
        }
    }
}
