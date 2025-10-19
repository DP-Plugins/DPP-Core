package com.darksoldier1404.dppc.lang;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import com.darksoldier1404.dppc.data.DPlugin;
import com.darksoldier1404.dppc.utils.ColorUtils;
import com.darksoldier1404.dppc.utils.ConfigUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@DPPCoreVersion(since = "5.3.0")
public class DLang {
    private final Set<DLangContext> langContexts = new HashSet<>();
    public Locale currentLang = Locale.forLanguageTag("en-US");

    public Set<DLangContext> getLangContexts() {
        return langContexts;
    }

    public void setCurrentLang(Locale lang) {
        currentLang = lang;
    }

    public Locale getCurrentLang() {
        return currentLang;
    }

    public void initPluginLang(DPlugin plugin) {
        File f = new File(plugin.getDataFolder() + "/lang", "en_US.yml");
        if (!f.exists()) {
            plugin.saveResource("lang/en_US.yml", false);
            plugin.saveResource("lang/ko_KR.yml", false);
        }
        ConfigUtils.loadCustomDataMap(plugin, "lang").forEach((name, data) -> {
            try {
                loadDefaultLangFiles(plugin, data, name);
            } catch (Exception e) {
                plugin.getLog().warning("[DLang] Error loading lang file: " + data.getName(), true);
            }
        });
    }

    public void loadDefaultLangFiles(DPlugin plugin, YamlConfiguration data, String fileName) {
        DLangContext context = new DLangContext(plugin, Locale.forLanguageTag(fileName.split("\\.")[0].replace("_", "-")));
        for (String key : data.getKeys(false)) {
            String value = data.getString(key);
            if (value != null) {
                if (context.hasValue(key)) {
                    plugin.getLog().warning("Language key '" + key + "' already exists in the context.", true);
                } else {
                    context.initDefaultValues(key, value);
                }
            } else {
                plugin.getLog().warning("Language key '" + key + "' has no value in the configuration file. Please ensure it is set correctly.", true);
            }
        }
        langContexts.add(context);
    }

    public String find(String key) {
        for (DLangContext context : langContexts) {
            if (context.getLang().getLanguage().equals(currentLang.getLanguage())) {
                if (context.hasValue(key)) {
                    return context.getValue(key);
                }
            }
        }
        return "[DLang] Error: Language key not found: " + key;
    }

    @NotNull
    public String get(String key) {
        return ColorUtils.applyColor(find(key));
    }

    @NotNull
    public String getWithArgs(String key, String... args) {
        String s = find(key);
        for (int i = 0; i < args.length; i++) {
            s = s.replace("{" + i + "}", args[i]);
        }
        return ColorUtils.applyColor(s);
    }
}
