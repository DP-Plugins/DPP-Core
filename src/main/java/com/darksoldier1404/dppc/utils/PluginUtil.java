package com.darksoldier1404.dppc.utils;

import com.darksoldier1404.dppc.DPPCore;
import com.darksoldier1404.dppc.api.logger.DLogManager;
import com.darksoldier1404.dppc.api.placeholder.PlaceholderBuilder;
import com.darksoldier1404.dppc.builder.action.ActionBuilder;
import com.darksoldier1404.dppc.utils.enums.DependPlugin;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sk89q.worldguard.WorldGuard;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.bukkit.Bukkit.getServer;

@SuppressWarnings("all")
public class PluginUtil {
    private static final DPPCore plugin = DPPCore.getInstance();
    private static final Map<JavaPlugin, Integer> loadedPlugins = new HashMap<>();
    private static final Set<DependPlugin> dependPlugins = new HashSet<>();

    public static void addPlugin(JavaPlugin plugin, int id) {
        loadedPlugins.put(plugin, id);
    }

    public static Map<JavaPlugin, Integer> getLoadedPlugins() {
        return loadedPlugins;
    }

    @Nullable
    public static JavaPlugin getPluginByName(String name) {
        return loadedPlugins.keySet().stream()
                .filter(plugin -> plugin.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public static void loadALLPlugins() {
        for (JavaPlugin pl : loadedPlugins.keySet()) {
            if (pl != null) {
                if (isMetricsEnabled(pl.getName())) {
                    new Metrics((JavaPlugin) pl, loadedPlugins.get(pl));
                    DPPCore.getInstance().log.info(pl.getName() + " plugin metrics enabled.", DLogManager.printPluginUtilsLogs);
                }
            }
        }
    }

    public static void loadALLAction() {
        for (YamlConfiguration raw : ConfigUtils.loadCustomDataList(plugin, "actions")) {
            String actionName = raw.getString("ACTION_NAME");
            if (actionName == null) {
                plugin.getLog().warning("Action name is null. Skipping...", DLogManager.printPluginUtilsLogs);
                continue;
            }
            plugin.actions.put(actionName, new ActionBuilder(plugin, actionName).importFromYaml(raw));
        }
    }

    @Nullable
    public static boolean isMetricsEnabled(String name) {
        return DPPCore.getInstance().config.getStringList("Settings.metrics-excluded") == null ? true : !DPPCore.getInstance().config.getStringList("Settings.metrics-excluded").contains(name);
    }

    public static void initPlaceholders() {
        if (dependPlugins.contains(DependPlugin.PlaceholderAPI)) {
            for (PlaceholderBuilder.InternalExpansion pb : plugin.placeholders) {
                pb.register();
                plugin.getLog().info("PlaceholderAPI registered: " + pb.getIdentifier(), DLogManager.printPluginUtilsLogs);
            }
        } else {
            plugin.getLog().warning("PlaceholderAPI plugin is not installed.", DLogManager.printPluginUtilsLogs);
            plugin.getLog().warning("PlaceholderAPI is disabled.", DLogManager.printPluginUtilsLogs);
        }
    }

    public static void initializeSoftDependPlugins() {
        DPPCore.ess = getPluginInstance("Essentials", "MoneyAPI", DependPlugin.Essentials);
        DPPCore.lp = getPluginInstance("LuckPerms", "PermissionAPI", DependPlugin.LuckPerms);
        getPluginInstance("WorldGuard", "WorldGuardAPI", DependPlugin.WorldGuard);
        getPluginInstance("PlaceholderAPI", "PlaceholderUtils", DependPlugin.PlaceholderAPI);
    }

    public static boolean isDependPluginLoaded(DependPlugin dependPlugin) {
        return dependPlugins.contains(dependPlugin);
    }

    @Nullable
    public static Plugin getPluginInstance(String pluginName, String apiName, DependPlugin dependPlugin) {
        Plugin instance = getServer().getPluginManager().getPlugin(pluginName);
        if (instance == null) {
            plugin.getLog().warning(pluginName + " plugin is not installed.", DLogManager.printPluginUtilsLogs);
            plugin.getLog().warning(apiName + " is disabled.", DLogManager.printPluginUtilsLogs);
            return null;
        }
        dependPlugins.add(dependPlugin);
        return instance;
    }

    @Deprecated
    @Nullable
    public static <T> T getPluginInstance(String pluginName, Class<T> pluginClass, String apiName) {
        Plugin instance = getServer().getPluginManager().getPlugin(pluginName);
        if (instance == null) {
            plugin.getLog().warning(pluginName + " plugin is not installed.", DLogManager.printPluginUtilsLogs);
            plugin.getLog().warning(apiName + " is disabled.", DLogManager.printPluginUtilsLogs);
            return null;
        }
        if (!pluginClass.isInstance(instance)) {
            plugin.getLog().warning(pluginName + " plugin does not match the required class.", DLogManager.printPluginUtilsLogs);
            return null;
        }
        return pluginClass.cast(instance);
    }

    @Nullable
    public static LuckPerms getLuckPermsInstance() {
        Plugin lpPlugin = getServer().getPluginManager().getPlugin("LuckPerms");
        if (lpPlugin == null) {
            plugin.getLog().warning("LuckPerms plugin is not installed.", DLogManager.printPluginUtilsLogs);
            plugin.getLog().warning("PermissionAPI is disabled.", DLogManager.printPluginUtilsLogs);
            return null;
        }
        return LuckPermsProvider.get();
    }

    @Nullable
    public static WorldGuard getWorldGuardInstance() {
        Plugin wgPlugin = getServer().getPluginManager().getPlugin("WorldGuard");
        if (wgPlugin == null) {
            plugin.getLog().warning("WorldGuard plugin is not installed.", DLogManager.printPluginUtilsLogs);
            plugin.getLog().warning("WorldGuardAPI is disabled.", DLogManager.printPluginUtilsLogs);
            return null;
        }
        return WorldGuard.getInstance();
    }


    private static final String API_URL = "https://raw.githubusercontent.com/darksoldier1404/DPP-Releases/main/releases.json";

    @NotNull
    public static String getLatestVersion(String pluginName) {
        CompletableFuture<String> future = new CompletableFuture<>();

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(API_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);

                    int responseCode = connection.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        plugin.getLog().warning("Warning: Unable to get version data for " + pluginName + ". HTTP Response Code: " + responseCode, DLogManager.printPluginUtilsLogs);
                        future.complete("0.0.0");
                        return;
                    }

                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }

                        JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();
                        JsonObject releases = jsonObject.getAsJsonObject("releases");
                        if (releases == null || !releases.has(pluginName)) {
                            plugin.getLog().warning("Warning: Plugin " + pluginName + " not found in API response.", DLogManager.printPluginUtilsLogs);
                            future.complete("0.0.0");
                            return;
                        }

                        JsonArray pluginReleases = releases.getAsJsonArray(pluginName);
                        if (pluginReleases.isEmpty()) {
                            plugin.getLog().warning("Warning: No releases found for plugin " + pluginName, DLogManager.printPluginUtilsLogs);
                            future.complete("0.0.0");
                            return;
                        }

                        JsonObject latestRelease = pluginReleases.get(0).getAsJsonObject();
                        String tag = latestRelease.get("tag").getAsString();
                        future.complete(Objects.requireNonNull(tag, "0.0.0"));
                    }
                } catch (Exception e) {
                    plugin.getLog().warning("Error fetching version for " + pluginName + ": " + e.getMessage(), DLogManager.printPluginUtilsLogs);
                    e.printStackTrace();
                    future.complete("0.0.0");
                }
            }
        }.runTaskAsynchronously(plugin);
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            plugin.getLog().warning("Error waiting for version result for " + pluginName + ": " + e.getMessage(), DLogManager.printPluginUtilsLogs);
            e.printStackTrace();
            return "0.0.0";
        }
    }

    private static boolean isNewVersion(String currentVersion, String latestVersion) {
        try {
            String[] currentParts = currentVersion.split("\\.");
            String[] latestParts = latestVersion.split("\\.");

            for (int i = 0; i < Math.min(currentParts.length, latestParts.length); i++) {
                int currentPart = Integer.parseInt(currentParts[i]);
                int latestPart = Integer.parseInt(latestParts[i]);

                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            plugin.getLog().warning("Error comparing versions: " + e.getMessage(), DLogManager.printPluginUtilsLogs);
            return false;
        }
        return false;
    }

    public static void updateCheck() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            updateCheck(getServer().getConsoleSender());
        });
    }

    public static void updateCheck(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (JavaPlugin plugin : loadedPlugins.keySet()) {
                String latestVersion = getLatestVersion(plugin.getName());
                if (latestVersion != null) {
                    String currentVersion = plugin.getDescription().getVersion();
                    if (isNewVersion(currentVersion, latestVersion)) {
                        sender.sendMessage("§f[ §bDPP-Core §f] §e" + plugin.getName() + " §f| §cA new version of " + plugin.getName() + " is available: " + latestVersion + ". You are running version " + currentVersion);
                        sender.sendMessage("§fDownload: §ehttps://dpnw.site/");
                    } else {
                        sender.sendMessage("§f[ §bDPP-Core §f] §e" + plugin.getName() + " §f| §aYou are running the latest version §f(§a" + currentVersion + "§f)");
                    }
                }
            }
        });
    }

    public static void updateCheck(CommandSender sender, String name) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (loadedPlugins.keySet().stream().noneMatch(plugin -> plugin.getName().equalsIgnoreCase(name))) {
                sender.sendMessage("Plugin " + name + " not found.");
                return;
            }
            String latestVersion = getLatestVersion(name);
            if (latestVersion != null) {
                JavaPlugin plugin = loadedPlugins.keySet().stream().filter(p -> p.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
                String currentVersion = plugin.getDescription().getVersion();
                if (isNewVersion(currentVersion, latestVersion)) {
                    sender.sendMessage("§f[ §bDPP-Core §f] §e" + name + " §f| §cA new version of " + name + " is available: " + latestVersion + ". You are running version " + currentVersion);
                    sender.sendMessage("§fDownload: §ehttps://dpnw.site/");
                } else {
                    sender.sendMessage("§f[ §bDPP-Core §f] §e" + name + " §f| §aYou are running the latest version §f(§a" + currentVersion + "§f)");
                }
            }
        });
    }

    public static void showBanner() {
        StringBuilder loadedPlugins = new StringBuilder();
        getLoadedPlugins().keySet().stream()
                .forEach(pl -> {
                    if (pl != null) {
                        loadedPlugins.append(String.format(
                                "   §aName §f: §b%-25s §7| §aVersion §f: §e%-15s%n",
                                pl.getName(),
                                pl.getDescription().getVersion()
                        ));
                    }
                });

        String result = loadedPlugins.toString();
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            getServer().getConsoleSender().sendMessage("\n" +
                    "─────────────────────────────────────────────────────────────────\n" +
                    "  §b    ____  ____  ____        ______              \n" +
                    "     / __ ⧵/ __ ⧵/ __ ⧵      / ____/___  ________ \n" +
                    "    / / / / /_/ / /_/ /_____/ /   / __ ⧵/ ___/ _ ⧵\n" +
                    "   / /_/ / ____/ ____/_____/ /___/ /_/ / /  /  __/\n" +
                    "  /_____/_/   /_/          ⧵____/⧵____/_/   ⧵___/ \n\n" +
                    "§7》》》》》》》 §aVERSION §f: §e" + plugin.getDescription().getVersion() + "\n\n" +
                    "§7》》》》》》》 §cAPI-VERSION §f: §e" + plugin.getDescription().getAPIVersion() + "\n\n" +
                    "§7》》》》》》》 §dSERVER-VERSION §f: §e" + getServer().getBukkitVersion() + " (" + getServer().getVersion() + ")" + "\n\n\n" +
                    "§7》》》》》》》 §bCheck out our new plugins! §f: §ehttps://dpnw.site\n\n" +
                    "§7》》》》》》》 §bJoin our Discord Server to get latest update! §f: §ehttps://discord.com/invite/JnMCqkn2FX\n\n\n" +
                    "§7》》》》》》》 §6Plugin installed with DPP-Core §7《《《《《《《\n\n" +
                    loadedPlugins +
                    "§f─────────────────────────────────────────────────────────────────\n");
        }, 80L);
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            updateCheck();
        }, 90L);
    }
}
