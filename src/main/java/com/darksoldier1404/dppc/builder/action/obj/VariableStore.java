package com.darksoldier1404.dppc.builder.action.obj;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import com.darksoldier1404.dppc.data.DPlugin;
import com.darksoldier1404.dppc.utils.ConfigUtils;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Holds persisted action variables. Two scopes are supported:
 * <ul>
 *     <li>Global: one value shared by the whole server, stored in
 *     {@code variables/global.yml}.</li>
 *     <li>Player: a value per player UUID, stored in
 *     {@code variables/players/<uuid>.yml}; loaded on join, saved on quit.</li>
 * </ul>
 * Temporary (per-execution) variables are <b>not</b> handled here; they live in
 * {@link ActionContext}.
 *
 * <p>Each variable is serialized as a {@code "name=value"} line so that names
 * containing dots do not collide with YAML path separators.</p>
 *
 * <p>When constructed without a plugin the store is memory-only (no disk I/O),
 * which keeps unit tests free of a real data folder.</p>
 */
@DPPCoreVersion(since = "5.4.3")
public class VariableStore {
    private static final String FOLDER = "variables";
    private static final String PLAYER_FOLDER = "variables/players";
    private static final String GLOBAL_FILE = "global";
    private static final String KEY = "variables";

    private final DPlugin plugin;
    private final Map<String, String> global = new HashMap<>();
    private final Map<UUID, Map<String, String>> players = new HashMap<>();

    /** Memory-only store (used by unit tests). */
    public VariableStore() {
        this(null);
    }

    public VariableStore(DPlugin plugin) {
        this.plugin = plugin;
    }

    // --- Global ---

    public String getGlobal(String name) {
        return global.getOrDefault(name, "");
    }

    public boolean hasGlobal(String name) {
        return global.containsKey(name);
    }

    public void setGlobal(String name, String value) {
        global.put(name, value);
    }

    public Map<String, String> getGlobalMap() {
        return global;
    }

    // --- Player ---

    private Map<String, String> playerMap(UUID uuid) {
        return players.computeIfAbsent(uuid, k -> new HashMap<>());
    }

    public String getPlayer(UUID uuid, String name) {
        Map<String, String> m = players.get(uuid);
        return m == null ? "" : m.getOrDefault(name, "");
    }

    public boolean hasPlayer(UUID uuid, String name) {
        Map<String, String> m = players.get(uuid);
        return m != null && m.containsKey(name);
    }

    public void setPlayer(UUID uuid, String name, String value) {
        playerMap(uuid).put(name, value);
    }

    public Map<String, String> getPlayerMap(UUID uuid) {
        return players.getOrDefault(uuid, Collections.emptyMap());
    }

    // --- Persistence ---

    public void loadGlobal() {
        if (plugin == null) return;
        global.clear();
        global.putAll(read(GLOBAL_FILE, FOLDER));
    }

    public void saveGlobal() {
        if (plugin == null) return;
        write(GLOBAL_FILE, FOLDER, global);
    }

    public void loadPlayer(UUID uuid) {
        if (plugin == null) return;
        Map<String, String> m = playerMap(uuid);
        m.clear();
        m.putAll(read(uuid.toString(), PLAYER_FOLDER));
    }

    public void savePlayer(UUID uuid) {
        if (plugin == null) return;
        Map<String, String> m = players.get(uuid);
        if (m == null) return;
        write(uuid.toString(), PLAYER_FOLDER, m);
    }

    public void unloadPlayer(UUID uuid) {
        players.remove(uuid);
    }

    /** Persists the global store and every player store currently in memory. */
    public void saveAll() {
        if (plugin == null) return;
        saveGlobal();
        for (UUID uuid : players.keySet()) {
            savePlayer(uuid);
        }
    }

    private Map<String, String> read(String fileName, String path) {
        Map<String, String> result = new HashMap<>();
        YamlConfiguration data = ConfigUtils.loadCustomData(plugin, fileName, path);
        if (data == null) return result;
        List<String> lines = data.getStringList(KEY);
        for (String line : lines) {
            if (line == null) continue;
            int idx = line.indexOf('=');
            if (idx < 0) continue;
            result.put(line.substring(0, idx), line.substring(idx + 1));
        }
        return result;
    }

    private void write(String fileName, String path, Map<String, String> map) {
        YamlConfiguration data = new YamlConfiguration();
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (Map.Entry<String, String> e : map.entrySet()) {
            lines.add(e.getKey() + "=" + e.getValue());
        }
        data.set(KEY, lines);
        ConfigUtils.saveCustomData(plugin, data, fileName, path);
    }
}
