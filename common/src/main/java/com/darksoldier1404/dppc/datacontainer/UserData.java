package com.darksoldier1404.dppc.datacontainer;

import com.darksoldier1404.dppc.datacontainer.obj.YamlData;
import com.darksoldier1404.dppc.utils.ConfigUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class UserData extends YamlData {
    private final UUID playerId;
    private final String playerName;

    public UserData(JavaPlugin plugin, OfflinePlayer player) {
        super(plugin, "userdata/" + player.getUniqueId() + ".yml", DataType.USERDATA);
        this.playerId = player.getUniqueId();
        this.playerName = player.getName();
        initializePlayerData();
    }

    public UserData(JavaPlugin plugin, UUID playerId, String playerName) {
        super(plugin, "userdata/" + playerId + ".yml", DataType.USERDATA);
        this.playerId = playerId;
        this.playerName = playerName;
        initializePlayerData();
    }

    @Override
    public void loadData() {
        // Use ConfigUtils to load or create the user data file
        String fileNameWithoutExt = fileName.endsWith(".yml") ? 
            fileName.substring(0, fileName.length() - 4) : fileName;
        
        // Get the path part if it exists
        String path = "userdata";
        if (fileName.contains("/")) {
            path = fileName.substring(0, fileName.lastIndexOf('/'));
            fileNameWithoutExt = fileName.substring(fileName.lastIndexOf('/') + 1).replace(".yml", "");
        } else {
            fileNameWithoutExt = fileNameWithoutExt.replace(".yml", "");
        }
        
        // Initialize the data using ConfigUtils
        this.data = ConfigUtils.initUserData(plugin, fileNameWithoutExt, path);
        
        if (this.data == null) {
            this.data = new YamlConfiguration();
            plugin.getLogger().warning("Failed to load user data from " + fileName);
        } else {
            plugin.getLogger().info("Loaded user data from " + fileName);
        }
        
        initializePlayerData();
    }
    
    @Override
    public void exportData() {
        synchronized (lock) {
            // Get the file name without extension
            String fileNameWithoutExt = fileName.endsWith(".yml") ? 
                fileName.substring(0, fileName.length() - 4) : fileName;
                
            // Get the path part if it exists
            String path = "userdata";
            if (fileName.contains("/")) {
                path = fileName.substring(0, fileName.lastIndexOf('/'));
                fileNameWithoutExt = fileName.substring(fileName.lastIndexOf('/') + 1).replace(".yml", "");
            } else {
                fileNameWithoutExt = fileNameWithoutExt.replace(".yml", "");
            }
            
            // Save using ConfigUtils
            ConfigUtils.saveCustomData(plugin, data, fileNameWithoutExt, path);
        }
    }
    
    private void initializePlayerData() {
        // Initialize default values if they don't exist
        if (!data.isSet("name")) {
            data.set("name", playerName);
            data.set("uuid", playerId.toString());
            data.set("first-join", System.currentTimeMillis());
            data.set("last-seen", System.currentTimeMillis());
            data.set("playtime", 0);
            data.set("last-location.world", "world");
            data.set("last-location.x", 0.0);
            data.set("last-location.y", 100.0);
            data.set("last-location.z", 0.0);
            data.set("last-location.yaw", 0.0f);
            data.set("last-location.pitch", 0.0f);
            data.set("settings.fly", false);
            data.set("settings.god", false);
            data.set("settings.vanished", false);
            exportData();
        }
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return data.getString("name", playerName);
    }

    public long getFirstJoinTime() {
        return data.getLong("first-join");
    }

    public long getLastSeenTime() {
        return data.getLong("last-seen");
    }

    public void setData(String path, Object value) {
        data.set(path, value);
        exportData();
    }

    public <T> T getData(String path, Class<T> type) {
        return type.cast(data.get(path));
    }

    public <T> T getData(String path, Class<T> type, T defaultValue) {
        T value = getData(path, type);
        return value != null ? value : defaultValue;
    }

    /**
     * Adds a value to a list in the configuration.
     * @param path The path to the list
     * @param value The value to add
     * @param <T> The type of the value
     */
    @SuppressWarnings("unchecked")
    public <T> void addToList(@NotNull String path, @NotNull T value) {
        List<T> list = getList(path, (Class<T>) value.getClass());
        list.add(value);
        set(path, list);
    }

    /**
     * Removes a value from a list in the configuration.
     * @param path The path to the list
     * @param value The value to remove
     * @param <T> The type of the value
     * @return true if the list contained the value
     */
    @SuppressWarnings("unchecked")
    public <T> boolean removeFromList(@NotNull String path, @NotNull T value) {
        List<T> list = getList(path, (Class<T>) value.getClass());
        boolean result = list.remove(value);
        if (result) {
            set(path, list);
        }
        return result;
    }

    /**
     * Gets a list of values from the configuration.
     * @param path The path to the list
     * @param type The type of elements in the list
     * @param <T> The type of elements in the list
     * @return The list of values, or an empty list if not found or invalid
     */
    @NotNull
    @Override
    public <T> List<T> getList(@NotNull String path, @NotNull Class<T> type) {
        return super.getList(path, type);
    }

    /**
     * Gets the keys under the specified path.
     * @param path The configuration path
     * @return The set of keys, or an empty set if none found
     */
    @NotNull
    @Override
    public Set<String> getKeys(@NotNull String path) {
        return super.getKeys(path);
    }
    
    /**
     * Gets all top-level keys in the configuration.
     * @return The set of top-level keys
     */
    @NotNull
    public Set<String> getKeys() {
        return getKeys("");
    }
    
    /**
     * Checks if the configuration contains the specified path.
     * @param path The path to check
     * @return true if the path exists
     */
    public boolean contains(@NotNull String path) {
        return data.contains(path);
    }
    
    /**
     * Removes a value from the configuration.
     * @param path The path to remove
     */
    public void remove(@NotNull String path) {
        set(path, null);
    }
}
