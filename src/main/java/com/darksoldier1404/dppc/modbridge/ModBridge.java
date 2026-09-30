package com.darksoldier1404.dppc.modbridge;

import com.darksoldier1404.dppc.events.modbridge.ModClientReadyEvent;
import com.darksoldier1404.dppc.events.modbridge.ModClientRefusedEvent;
import com.darksoldier1404.dppmc.protocol.ProtocolException;
import com.darksoldier1404.dppmc.protocol.ProtocolSpec;
import com.darksoldier1404.dppmc.protocol.core.CoreProtocol;
import com.darksoldier1404.dppmc.protocol.core.ModOffer;
import com.darksoldier1404.dppmc.protocol.core.Verdict;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The server side of DPP-ModCore (design §6.4). DPP-Core answers the {@code dppmc:core} handshake for every
 * plugin, so a plugin only binds its protocol:
 *
 * <pre>{@code
 * ServerChannel ch = ModBridge.bind(this, HudShopProtocol.SPEC);   // in onEnable
 * ch.on(BuyRequest.class, (player, packet) -> ...);                // main thread
 * ch.send(player, new ShopOpenPacket(...));
 * ch.require(ModRequirement.mandatory().withBypassPermission("dphs.bypass.clientmod"));
 * }</pre>
 */
public final class ModBridge {
    private static volatile Runtime runtime;

    private ModBridge() {
    }

    /**
     * Binds a plugin's protocol and registers its channel for the plugin. The binding ends when the plugin is
     * disabled.
     *
     * @throws IllegalStateException    if DPP-Core is not enabled (add {@code depend: [DPP-Core]})
     * @throws IllegalArgumentException if the namespace is reserved or already bound by another plugin
     */
    public static ServerChannel bind(Plugin plugin, ProtocolSpec spec) {
        return running().bind(Objects.requireNonNull(plugin, "plugin"), Objects.requireNonNull(spec, "spec"));
    }

    /** What the player's DPP-ModCore client reported, if it said hello on this connection. */
    public static Optional<ModSession> session(Player player) {
        return running().handshake.session(player.getUniqueId());
    }

    /** Called by DPP-Core's onEnable. */
    public static synchronized void enable(JavaPlugin core) {
        if (runtime == null) {
            runtime = new Runtime(core);
            runtime.start();
        }
    }

    /** Called by DPP-Core's onDisable. */
    public static synchronized void disable() {
        if (runtime != null) {
            runtime.stop();
            runtime = null;
        }
    }

    private static Runtime running() {
        Runtime current = runtime;
        if (current == null) {
            throw new IllegalStateException("DPP-Core's mod bridge is not enabled; add depend: [DPP-Core] to plugin.yml");
        }
        return current;
    }

    /** Bukkit wiring around {@link ServerHandshake}; one instance while DPP-Core is enabled. */
    static final class Runtime implements Listener {
        private static final String CORE_CHANNEL = CoreProtocol.SPEC.channel();

        private final JavaPlugin core;
        private final Map<String, ServerChannel> channels = new ConcurrentHashMap<>();
        /** Per player, shared by every channel: binding more protocols must not multiply what one client may cost. */
        private final Map<UUID, DecodeBudget> budgets = new ConcurrentHashMap<>();
        private final ServerHandshake handshake;
        private int expireTask = -1;

        Runtime(JavaPlugin core) {
            this.core = core;
            this.handshake = new ServerHandshake(this::boundSpecs, core.getDescription().getVersion());
        }

        void start() {
            Bukkit.getMessenger().registerOutgoingPluginChannel(core, CORE_CHANNEL);
            Bukkit.getMessenger().registerIncomingPluginChannel(core, CORE_CHANNEL,
                    (channel, player, message) -> onMainThread(() -> onHello(player, message)));
            Bukkit.getPluginManager().registerEvents(this, core);
            expireTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(core, () -> {
                long now = System.currentTimeMillis();
                handshake.expire(now);
                channels.values().forEach(channel -> channel.expire(now));
            }, 20L, 20L);
        }

        void stop() {
            if (expireTask != -1) {
                Bukkit.getScheduler().cancelTask(expireTask);
            }
            Bukkit.getMessenger().unregisterIncomingPluginChannel(core, CORE_CHANNEL);
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(core, CORE_CHANNEL);
            channels.clear();
        }

        synchronized ServerChannel bind(Plugin plugin, ProtocolSpec spec) {
            if (spec.namespace().equals(CoreProtocol.SPEC.namespace())) {
                throw new IllegalArgumentException("namespace '" + spec.namespace() + "' is reserved for DPP-Core");
            }
            ServerChannel existing = channels.get(spec.namespace());
            if (existing != null) {
                throw new IllegalArgumentException(spec.channel() + " is already bound by " + existing.plugin().getName());
            }
            ServerChannel channel = new ServerChannel(this, plugin, spec);
            Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, spec.channel());
            Bukkit.getMessenger().registerIncomingPluginChannel(plugin, spec.channel(),
                    (name, player, message) -> onMainThread(() -> channel.receive(player, message)));
            channels.put(spec.namespace(), channel);
            core.getLogger().info("[ModBridge] " + plugin.getName() + " bound " + spec.channel() + " (protocol "
                    + spec.version() + ", accepts " + spec.minCompatible() + "+)");
            return channel;
        }

        boolean isReady(UUID player, String namespace) {
            return handshake.isReady(player, namespace);
        }

        /** Charges what decoding {@code frame} can cost to the player's budget; false when it is spent. */
        boolean charge(UUID player, byte[] frame, long nowMillis) {
            return budgets.computeIfAbsent(player, id -> new DecodeBudget())
                    .tryCharge(DecodeBudget.costOf(frame), nowMillis);
        }

        private Map<String, ProtocolSpec> boundSpecs() {
            Map<String, ProtocolSpec> specs = new LinkedHashMap<>();
            channels.forEach((namespace, channel) -> specs.put(namespace, channel.spec()));
            return specs;
        }

        private void onHello(Player player, byte[] frame) {
            if (!player.isOnline()) {
                return;
            }
            long now = System.currentTimeMillis();
            if (!charge(player.getUniqueId(), frame, now)) {
                core.getLogger().fine("[ModBridge] dropped a hello frame from " + player.getName() + ": over the decode budget");
                return;
            }
            Optional<ServerHandshake.Answer> answer;
            try {
                answer = handshake.accept(player.getUniqueId(), frame, now);
            } catch (ProtocolException e) {
                core.getLogger().fine("[ModBridge] dropped a malformed hello from " + player.getName() + ": " + e.getMessage());
                return;
            }
            if (answer.isEmpty()) {
                return;
            }
            for (byte[] ack : answer.get().ackFrames()) {
                player.sendPluginMessage(core, CORE_CHANNEL, ack);
            }
            for (ModOffer offer : answer.get().newlyReady()) {
                core.getLogger().info("[ModBridge] " + player.getName() + " is ready for " + offer.modId() + " "
                        + offer.modVersion() + " (DPP-ModCore " + answer.get().session().coreVersion() + ", Minecraft "
                        + answer.get().session().minecraftVersion() + ")");
                Bukkit.getPluginManager().callEvent(new ModClientReadyEvent(player, offer, answer.get().session()));
            }
            for (Map.Entry<ModOffer, Verdict> refused : answer.get().newlyRefused().entrySet()) {
                ModOffer offer = refused.getKey();
                core.getLogger().info("[ModBridge] " + player.getName() + " has " + offer.modId() + " " + offer.modVersion()
                        + " speaking protocol " + offer.specVersion() + ": " + refused.getValue());
                Bukkit.getPluginManager().callEvent(new ModClientRefusedEvent(player, offer, refused.getValue()));
                ServerChannel channel = channels.get(offer.modId());
                if (channel != null) {
                    enforce(player, channel);
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onJoin(PlayerJoinEvent event) {
            // Nothing is sent on join: the client says hello once its channels are registered.
            Player player = event.getPlayer();
            UUID id = player.getUniqueId();
            for (ServerChannel channel : channels.values()) {
                ModRequirement requirement = channel.requirement();
                if (!requirement.required()) {
                    continue;
                }
                long ticks = Math.max(1L, requirement.grace().toMillis() / 50L);
                Bukkit.getScheduler().runTaskLater(core, () -> {
                    Player online = Bukkit.getPlayer(id);
                    if (online != null && online.isOnline()) {
                        enforce(online, channel);
                    }
                }, ticks);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onQuit(PlayerQuitEvent event) {
            UUID id = event.getPlayer().getUniqueId();
            handshake.forget(id);
            budgets.remove(id);
            channels.values().forEach(channel -> channel.forget(id));
        }

        @EventHandler
        public void onPluginDisable(PluginDisableEvent event) {
            channels.values().removeIf(channel -> channel.plugin().equals(event.getPlugin()));
        }

        private void enforce(Player player, ServerChannel channel) {
            ModRequirement requirement = channel.requirement();
            boolean bypass = requirement.bypassPermission() != null && player.hasPermission(requirement.bypassPermission());
            Verdict verdict = handshake.session(player.getUniqueId())
                    .map(session -> session.verdicts().get(channel.spec().namespace()))
                    .orElse(null);
            String message = switch (RequirementCheck.evaluate(requirement, bypass, verdict)) {
                case NONE -> null;
                case MISSING -> requirement.missingMessage();
                case MISMATCH -> requirement.mismatchMessage();
            };
            if (message != null) {
                player.kickPlayer(message.replace("{mod}", channel.spec().namespace()));
            }
        }

        private void onMainThread(Runnable task) {
            // CraftBukkit delivers plugin messages on the main thread, but that is an implementation detail.
            if (Bukkit.isPrimaryThread()) {
                task.run();
            } else {
                Bukkit.getScheduler().runTask(core, task);
            }
        }
    }
}
