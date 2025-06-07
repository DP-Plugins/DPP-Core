package com.darksoldier1404.dppc.datacontainer;

import com.darksoldier1404.dppc.datacontainer.obj.YamlData;
import com.darksoldier1404.dppc.utils.ConfigUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Handles language files with support for placeholders and color codes.
 */
public class LangData extends YamlData {
    private final String language;
    private final String prefix;

    /**
     * Creates a new LangData instance for the specified language.
     * @param plugin The plugin instance
     * @param language The language code (e.g., "en", "ko")
     */
    public LangData(@NotNull JavaPlugin plugin, @NotNull String language) {
        super(plugin, "lang/" + language + ".yml", DataType.LANG);
        this.language = language;
        this.prefix = data.getString("prefix", "&8[&b" + plugin.getName() + "&8]&r ");
    }

    /**
     * Creates a new LangData instance with existing YAML data.
     * @param plugin The plugin instance
     * @param language The language code
     * @param data The YAML configuration data
     */
    public LangData(@NotNull JavaPlugin plugin, @NotNull String language, @NotNull YamlConfiguration data) {
        super(plugin, data, DataType.LANG);
        this.language = language;
        this.prefix = data.getString("prefix", "&8[&b" + plugin.getName() + "&8]&r ");
    }

    /**
     * Gets the language code of this language file.
     * @return The language code (e.g., "en", "ko")
     */
    @NotNull
    public String getLanguage() {
        return language;
    }

    /**
     * Gets the prefix used for messages.
     * @return The message prefix
     */
    @NotNull
    public String getPrefix() {
        return prefix;
    }

    /**
     * Gets a formatted message with color codes and placeholders.
     * @param path The path to the message
     * @param placeholders The placeholder values
     * @return The formatted message, or an error message if not found
     */
    @NotNull
    public String getMessage(@NotNull String path, @Nullable Object... placeholders) {
        String message = data.getString(path);
        if (message == null) {
            return "&cMissing message: &7" + path;
        }
        return format(message, placeholders);
    }

    /**
     * Gets a prefixed and formatted message with color codes and placeholders.
     * @param path The path to the message
     * @param placeholders The placeholder values
     * @return The formatted message with prefix, or an error message if not found
     */
    @NotNull
    public String getPrefixedMessage(@NotNull String path, @Nullable Object... placeholders) {
        return prefix + getMessage(path, placeholders);
    }

    /**
     * Gets a list of formatted messages with color codes and placeholders.
     * @param path The path to the message list
     * @param placeholders The placeholder values
     * @return The list of formatted messages, or a list with an error message if not found
     */
    @NotNull
    public List<String> getMessageList(@NotNull String path, @Nullable Object... placeholders) {
        List<String> messages = data.getStringList(path);
        if (messages.isEmpty()) {
            return java.util.Collections.singletonList("&cMissing message list: &7" + path);
        }
        
        List<String> formatted = new ArrayList<>();
        for (String message : messages) {
            formatted.add(format(message, placeholders));
        }
        return formatted;
    }

    /**
     * Formats a message with color codes and placeholders.
     * @param message The message to format
     * @param placeholders The placeholder values
     * @return The formatted message
     */
    @NotNull
    private String format(@NotNull String message, @Nullable Object... placeholders) {
        // Replace color codes
        String formatted = ChatColor.translateAlternateColorCodes('&', message);
        
        // Replace placeholders
        if (placeholders != null) {
            for (int i = 0; i < placeholders.length; i++) {
                formatted = formatted.replace("{" + i + "}", 
                    Objects.toString(placeholders[i] != null ? placeholders[i] : "null"));
            }
        }
        
        return formatted;
    }

    /**
     * Checks if a message exists in the language file.
     * @param path The path to check
     * @return true if the message exists
     */
    public boolean hasMessage(@NotNull String path) {
        return data.isSet(path);
    }

    /**
     * Gets a configuration value with type safety.
     * @param path The path to the value
     * @param type The expected type
     * @param def The default value
     * @param <T> The type of the value
     * @return The value, or the default if not found or invalid type
     */
    @Nullable
    public <T> T get(@NotNull String path, @NotNull Class<T> type, @Nullable T def) {
        return super.get(path, type, def);
    }
    
    @Override
    public void loadData() {
        // Get the file name without extension
        String fileNameWithoutExt = fileName.endsWith(".yml") ? 
            fileName.substring(0, fileName.length() - 4) : fileName;
            
        // Try to load from file system first
        this.data = ConfigUtils.loadCustomData(plugin, fileNameWithoutExt, "lang");
        
        if (this.data == null) {
            // If not found, try to save default from resources
            plugin.saveResource("lang/" + fileName, false);
            this.data = ConfigUtils.loadCustomData(plugin, fileNameWithoutExt, "lang");
            
            if (this.data == null) {
                this.data = new YamlConfiguration();
                plugin.getLogger().warning("Could not load language file: " + fileName);
                return;
            }
        }
        
        plugin.getLogger().info("Loaded language file: " + fileName);
    }
    
    @Override
    public void exportData() {
        synchronized (lock) {
            String fileNameWithoutExt = fileName.endsWith(".yml") ? 
                fileName.substring(0, fileName.length() - 4) : fileName;
            ConfigUtils.saveCustomData(plugin, data, fileNameWithoutExt, "lang");
        }
    }
}
