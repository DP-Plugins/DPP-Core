package com.darksoldier1404.dppc.events;

import com.darksoldier1404.dppc.DPPCore;
import com.darksoldier1404.dppc.plugin.functions.UpdateStatus;
import com.darksoldier1404.dppc.utils.PluginUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Notifies admins of outdated plugins shortly after they join.
 *
 * <p>Five seconds after a player with {@code dppc.admin} joins, any plugins whose installed version
 * is behind the latest known version are listed in chat (with their GitHub link). The feature can be
 * turned off via {@code Settings.notify_admins_on_join} in config.yml. Cached versions are used when
 * available; if no check has run yet, one is triggered first.</p>
 */
public class AdminUpdateNotifyListener implements Listener {

    private static final String CONFIG_KEY = "Settings.notify_admins_on_join";
    private static final long DELAY_TICKS = 100L; // 5 seconds

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!DPPCore.getInstance().getConfig().getBoolean(CONFIG_KEY, true)) return;
        if (!p.hasPermission("dppc.admin")) return;

        Bukkit.getScheduler().runTaskLater(DPPCore.getInstance(), () -> {
            if (!p.isOnline()) return;
            if (PluginUtil.hasCachedVersions()) {
                notifyOutdated(p);
            } else {
                // No update check has run yet: fetch first, then notify on completion.
                PluginUtil.checkAllUpdatesAsync(() -> notifyOutdated(p));
            }
        }, DELAY_TICKS);
    }

    private void notifyOutdated(Player p) {
        if (!p.isOnline()) return;
        List<JavaPlugin> outdated = getOutdatedPlugins();
        if (outdated.isEmpty()) return;

        String prefix = DPPCore.getInstance().getPrefix();
        p.sendMessage(prefix + "§e" + outdated.size() + " plugin(s) have a new version available:");
        for (JavaPlugin plugin : outdated) {
            String latest = PluginUtil.getCachedLatestVersion(plugin.getName());
            p.sendMessage(prefix + "§c" + plugin.getName() + " §7"
                    + plugin.getDescription().getVersion() + " §f→ §a" + latest);
            p.sendMessage(prefix + "§fGitHub: §e" + PluginUtil.getGithubUrl(plugin.getName()));
        }
    }

    /**
     * @return the registered plugins whose installed version is behind the cached latest version.
     */
    public static List<JavaPlugin> getOutdatedPlugins() {
        List<JavaPlugin> outdated = new ArrayList<>();
        for (JavaPlugin plugin : PluginUtil.getLoadedPlugins().keySet()) {
            if (plugin == null) continue;
            UpdateStatus status = UpdateStatus.of(
                    plugin.getDescription().getVersion(),
                    PluginUtil.getCachedLatestVersion(plugin.getName()));
            if (status == UpdateStatus.OUTDATED) {
                outdated.add(plugin);
            }
        }
        return outdated;
    }
}
