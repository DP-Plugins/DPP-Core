package com.darksoldier1404.dppc.datacontainer;

import com.darksoldier1404.dppc.datacontainer.obj.DData;
import com.darksoldier1404.dppc.datacontainer.obj.YamlData;
import com.darksoldier1404.dppc.utils.ConfigUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all data containers for the plugin.
 * Handles loading, saving, and accessing different types of data.
 */
public class DataContainerManager {
    private final JavaPlugin plugin;
    private final Map<DataType, DData> dataContainers = new EnumMap<>(DataType.class);
    private final Map<UUID, UserData> userDataCache = new ConcurrentHashMap<>();
    private final String defaultLanguage;

    /**
     * Creates a new DataContainerManager with the default language "en_US".
     * @param plugin The plugin instance
     */
    public DataContainerManager(@NotNull JavaPlugin plugin) {
        this(plugin, "en_US");
    }

    /**
     * Creates a new DataContainerManager with the specified default language.
     * @param plugin The plugin instance
     * @param defaultLanguage The default language code (e.g., "en_US", "ko_KR")
     */
    public DataContainerManager(@NotNull JavaPlugin plugin, @NotNull String defaultLanguage) {
        this.plugin = plugin;
        this.defaultLanguage = defaultLanguage;
        initializeDataContainers();
    }

    private void initializeDataContainers() {
        // Initialize default data containers
        dataContainers.put(DataType.CONFIG, new ConfigData(plugin));
        dataContainers.put(DataType.LANG, new LangData(plugin, defaultLanguage));
    }

    /**
     * Gets the configuration data container.
     * @return The ConfigData instance
     */
    @NotNull
    public ConfigData getConfig() {
        return (ConfigData) dataContainers.computeIfAbsent(
            DataType.CONFIG,
            k -> new ConfigData(plugin)
        );
    }

    /**
     * Gets the language data container.
     * @return The LangData instance
     */
    @NotNull
    public LangData getLang() {
        return (LangData) dataContainers.computeIfAbsent(
            DataType.LANG,
            k -> new LangData(plugin, defaultLanguage)
        );
    }

    /**
     * Changes the current language and reloads the language file.
     * @param languageCode The language code (e.g., "en_US", "ko_KR")
     */
    public void setLang(@NotNull String languageCode) {
        LangData newLangData = new LangData(plugin, languageCode);
        dataContainers.put(DataType.LANG, newLangData);
    }

    /**
     * Gets or creates user data for the specified player.
     * @param player The player
     * @return The UserData instance for the player
     */
    @NotNull
    public UserData getUserData(@NotNull OfflinePlayer player) {
        return userDataCache.computeIfAbsent(
            player.getUniqueId(),
            uuid -> new UserData(plugin, player)
        );
    }

    /**
     * Gets or creates user data for the specified player ID and name.
     * @param playerId The player's UUID
     * @param playerName The player's name
     * @return The UserData instance for the player
     */
    @NotNull
    public UserData getUserData(@NotNull UUID playerId, @NotNull String playerName) {
        return userDataCache.computeIfAbsent(
            playerId,
            uuid -> new UserData(plugin, playerId, playerName)
        );
    }

    /**
     * Unloads and saves the specified player's data.
     * @param playerId The player's UUID
     */
    public void unloadUserData(@NotNull UUID playerId) {
        UserData userData = userDataCache.remove(playerId);
        if (userData != null) {
            userData.exportData();
        }
    }

    /**
     * Unloads and saves all user data.
     */
    public void unloadAllUserData() {
        userDataCache.values().forEach(DData::exportData);
        userDataCache.clear();
    }

    /**
     * Saves all data containers and user data.
     */
    public void saveAll() {
        // Save all data containers
        dataContainers.values().forEach(DData::exportData);
        
        // Save all user data
        userDataCache.values().forEach(DData::exportData);
    }

    /**
     * Reloads all data containers from disk.
     */
    public void reloadAll() {
        // Reload all data containers
        dataContainers.values().forEach(data -> {
            if (data instanceof YamlData) {
                ((YamlData) data).loadData();
            }
        });
    }

    /**
     * Gets a list of all data files for a specific data type.
     * @param dataType The data type
     * @return Array of data files
     */
    @NotNull
    public File[] getDataFiles(@NotNull DataType dataType) {
        File dataFolder = new File(plugin.getDataFolder(), dataType.name().toLowerCase());
        if (!dataFolder.exists()) {
            return new File[0];
        }
        return dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
    }
    
    /**
     * Loads all user data files from the userdata directory.
     * @return List of loaded user data configurations
     */
    @NotNull
    public List<YamlConfiguration> loadAllUserData() {
        return ConfigUtils.loadCustomDataList(plugin, "userdata");
    }
    
    /**
     * Initializes a new user data file.
     * @param fileName The name of the user data file (without .yml extension)
     * @return The initialized YamlConfiguration
     */
    @NotNull
    public YamlConfiguration initUserData(@NotNull String fileName) {
        return ConfigUtils.initUserData(plugin, fileName, "userdata");
    }

    /**
     * Gets a list of all user data files.
     * @return Array of user data files
     */
    @NotNull
    public File[] getUserDataFiles() {
        return getDataFiles(DataType.USERDATA);
    }

    /**
     * Gets the number of cached user data entries.
     * @return Number of cached users
     */
    public int getCachedUserCount() {
        return userDataCache.size();
    }
}
