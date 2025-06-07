package com.darksoldier1404.dppc.example;

import com.darksoldier1404.dppc.datacontainer.LangData;
import com.darksoldier1404.dppc.datacontainer.UserData;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 데이터 컨테이너 사용 예제 클래스
 * DataContainerManager, YamlData, UserData, LangData, MapData의 주요 기능을 보여줍니다.
 */
public class DataContainerExample {
    private final JavaPlugin plugin;
    private LangData langData;
    private final Map<String, UserData> userDataMap = new ConcurrentHashMap<>();

    public DataContainerExample(JavaPlugin plugin) {
        this.plugin = plugin;
        
        // 1. 언어 데이터 초기화
        initializeLanguage();
        
        // 2. 유저 데이터 맵 초기화
        initializeUserDataMap();
    }
    

    
    /**
     * LangData 사용 예제
     */
    private void initializeLanguage() {
        // 1. 언어 파일 로드 (예: en.yml)
        langData = new LangData(plugin, "en");
        
        // 2. Get and log welcome message
        String welcomeMsg = langData.getData().getString("messages.welcome", "Welcome, %player%!")
                .replace("%player%", "Player");
        plugin.getLogger().info(welcomeMsg);
        
        // 3. Get and log help messages
        List<String> helpMessages = langData.getData().getStringList("messages.help");
        if (helpMessages.isEmpty()) {
            helpMessages = Arrays.asList("Available commands:", "/help - Show this help");
        }
        for (String line : helpMessages) {
            plugin.getLogger().info(line);
        }
        
        // 4. Log settings
        int maxHomes = langData.getData().getInt("settings.max-homes", 3);
        boolean useColor = langData.getData().getBoolean("settings.use-color", true);
        plugin.getLogger().info("Max homes: " + maxHomes + ", Use color: " + useColor);
    }
    
    /**
     * UserData 및 MapData 사용 예제
     */
    private void initializeUserDataMap() {
        // 1. Load all existing user data
        File userDataDir = new File(plugin.getDataFolder(), "userdata");
        if (userDataDir.exists() && userDataDir.isDirectory()) {
            File[] files = userDataDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    String uuidStr = file.getName().replace(".yml", "");
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        UserData userData = new UserData(plugin, uuid, "Unknown");
                        userData.loadData();
                        userDataMap.put(uuidStr, userData);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in filename: " + file.getName());
                    }
                }
            }
        }
        plugin.getLogger().info("Loaded " + userDataMap.size() + " user data files");
    }
    
    /**
     * 플레이어 데이터 조회 및 수정 예제
     */
    public void handlePlayerData(Player player) {
        // 1. Get user data (create if not exists)
        UserData userData = getUserData(player);
        
        // 2. Set data using the configuration
        userData.getData().set("last-login", System.currentTimeMillis());
        userData.getData().set("ip-address", player.getAddress().getAddress().getHostAddress());
        
        // 3. Get settings (using the values for demonstration)
        int coins = userData.getData().getInt("coins", 0);
        boolean isVIP = userData.getData().getBoolean("vip", false);
        // Use the variables to avoid unused variable warnings
        plugin.getLogger().info(player.getName() + " has " + coins + " coins and " + 
                (isVIP ? "is a VIP" : "is not a VIP"));
        
        // 4. Save data
        userData.exportData();
    }
    
    /**
     * 오프라인 플레이어 데이터 조회 예제
     */
    public void handleOfflinePlayer(OfflinePlayer offlinePlayer) {
        UserData userData = getOfflineUserData(offlinePlayer);
        
        // Check last seen time
        long lastSeen = userData.getData().getLong("last-seen", 0L);
        String lastSeenStr = lastSeen > 0 ? 
            new java.util.Date(lastSeen).toString() : "Never";
            
        plugin.getLogger().info(offlinePlayer.getName() + " last seen: " + lastSeenStr);
    }
    
    /**
     * 모든 유저 데이터 저장
     */
    public void saveAllUserData() {
        int saved = 0;
        for (UserData userData : userDataMap.values()) {
            try {
                userData.exportData();
                saved++;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save user data for " + userData.getPlayerId() + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Saved " + saved + "/" + userDataMap.size() + " user data files");
    }
    
    // Utility methods
    private UserData getUserData(Player player) {
        String uuid = player.getUniqueId().toString();
        return userDataMap.computeIfAbsent(uuid, k -> {
            UserData userData = new UserData(plugin, player);
            userData.loadData();
            return userData;
        });
    }
    
    private UserData getOfflineUserData(OfflinePlayer offlinePlayer) {
        String uuid = offlinePlayer.getUniqueId().toString();
        return userDataMap.computeIfAbsent(uuid, k -> {
            UserData userData = new UserData(
                plugin, 
                offlinePlayer.getUniqueId(), 
                offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown"
            );
            userData.loadData();
            return userData;
        });
    }
}
