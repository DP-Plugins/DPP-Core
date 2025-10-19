/*
 * Special thanks to: tr7zw, SoSeDiK, Broken arrow
 * From TR's Mod Workshop
 */
package com.darksoldier1404.dppc.data;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import com.darksoldier1404.dppc.api.logger.DLogManager;
import com.darksoldier1404.dppc.api.logger.DLogNode;
import com.darksoldier1404.dppc.lang.DLang;
import com.darksoldier1404.dppc.utils.ColorUtils;
import com.darksoldier1404.dppc.utils.ConfigUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@DPPCoreVersion(since = "5.3.0")
public class DPlugin extends JavaPlugin {
    public YamlConfiguration config;
    public String prefix;
    private final Map<String, IDataHandler<?, ?>> data = new HashMap<>();
    private final boolean useDLang;
    private @Nullable DLang lang;
    public @NotNull DLogNode log;

    public DPlugin() {
        this(false);
    }

    public DPlugin(boolean useDLang) {
        this.useDLang = useDLang;
        log = DLogManager.init(this);
    }

    public void init() {
        this.config = ConfigUtils.loadDefaultPluginConfig(this);
        this.prefix = ColorUtils.applyColor(config.getString("Settings.prefix"));
        initDLang();
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

    public void set(String key, IDataHandler<?, ?> value) {
        data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T extends IDataHandler<?, ?>> T get(String key) {
        return (T) data.get(key);
    }

    public @NotNull DLogNode getLog() {
        return log;
    }

    public void reload() {
        init();
    }

    @Override
    public void saveConfig() {
        ConfigUtils.savePluginConfig(this, config);
    }

    public void initDLang() {
        if (this.useDLang) {
            if (this.config.getString("Settings.Lang") == null) {
                this.config.set("Settings.Lang", "en_US");
            }
            lang = new DLang();
            lang.initPluginLang(this);
            lang.setCurrentLang(Locale.forLanguageTag(this.config.getString("Settings.Lang").replace("_", "-")));
        } else {
            lang = null;
        }
    }

    public void saveAllData() {
        ConfigUtils.savePluginConfig(this, config);
        for (Map.Entry<String, IDataHandler<?, ?>> entry : data.entrySet()) {
            IDataHandler<?, ?> handler = entry.getValue();
            handler.saveAll();
        }
        saveLog();
    }

    public void saveDataContainer() {
        for (Map.Entry<String, IDataHandler<?, ?>> entry : data.entrySet()) {
            IDataHandler<?, ?> handler = entry.getValue();
            handler.saveAll();
        }
    }

    @Nullable
    public <K, V, T extends IDataHandler<K, V>> T loadDataContainer(T container) {
        return loadDataContainer(container, null);
    }

    @Nullable
    public <K, V, T extends IDataHandler<K, V>> T loadDataContainer(T container, Class<?> clazz) {
        try {
            IDataHandler<K, V> idh = container.loadAll(clazz);
            data.put(container.getPath(), idh);
            return (T) idh;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isUseDLang() {
        return useDLang;
    }

    public @Nullable DLang getLang() {
        return lang;
    }

    public void saveLog() {
        DLogManager.saveLogNode(this, false);
    }
}