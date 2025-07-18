package com.darksoldier1404.dppc.lang;

import com.darksoldier1404.dppc.DPPCore;
import com.darksoldier1404.dppc.utils.ConfigUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class DLang {
    private final JavaPlugin plugin;
    private final Logger log = DPPCore.getInstance().log;
    private YamlConfiguration currentLang;
    private Map<String, YamlConfiguration> langFiles = new HashMap<>();

    public DLang(@NotNull String langKey, JavaPlugin plugin) {
        this.plugin = plugin;
        loadDefaultLangFiles();
        try {
            currentLang = langFiles.get(langKey);
        } catch (Exception e) {
            log.warning("[DLang] Error: Language file not found!");
        }
    }

    public void setLangFile(YamlConfiguration lang) {
        currentLang = lang;
    }

    public YamlConfiguration getCurrentLang() {
        return currentLang;
    }

    public void setCurrentLang(YamlConfiguration currentLang) {
        this.currentLang = currentLang;
    }

    public Map<String, YamlConfiguration> getLangFiles() {
        return langFiles;
    }

    public void setLangFiles(Map<String, YamlConfiguration> langFiles) {
        this.langFiles = langFiles;
    }

    public void setLang(String lang) {
        try {
            currentLang = langFiles.get(lang);
        } catch (Exception e) {
            log.warning("[DLang] Error: Language file not found!");
            log.warning("[DLang] input: " + lang);
        }
    }

    @NotNull
    public String get(String key) {
        String s = currentLang.getString(key);
        if (s == null) {
            return "[DLang] Error: Language key not found: " + key;
        }
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @NotNull
    public String getWithArgs(String key, String... args) {
        String s = currentLang.getString(key);
        if (s != null) {
            for (int i = 0; i < args.length; i++) {
                s = s.replace("{" + i + "}", args[i]);
            }
            return ChatColor.translateAlternateColorCodes('&', s);
        }
        return "[DLang] Error: Language key not found: " + key;
    }

    public void loadDefaultLangFiles() {
        File f = new File(plugin.getDataFolder() + "/lang", "English.yml");
        if (!f.exists()) {
            plugin.saveResource("lang/English.yml", false);
            plugin.saveResource("lang/Korean.yml", false);
        }
        for (YamlConfiguration data : ConfigUtils.loadCustomDataList(plugin, "lang")) {
            try {
                langFiles.put(data.getString("Lang"), data);
            } catch (Exception e) {
                log.warning("[DLang] Error loading lang file: " + data.getName());
            }
        }
    }
}
