package com.darksoldier1404.dppc.modbridge;

import com.darksoldier1404.dppmc.protocol.Direction;
import com.darksoldier1404.dppmc.protocol.ProtocolException;
import com.darksoldier1404.dppmc.protocol.ProtocolSpec;
import com.darksoldier1404.dppmc.protocol.frame.FrameDecoder;
import com.darksoldier1404.dppmc.protocol.frame.FrameEncoder;
import com.darksoldier1404.dppmc.protocol.frame.FrameLimits;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * One plugin's protocol, bound with {@link ModBridge#bind}. Handlers run on the main thread; a packet is only
 * accepted from a player whose client completed the handshake for this protocol.
 */
public final class ServerChannel {
    private static final Object GATE = new Object();

    private final ModBridge.Runtime runtime;
    private final Plugin plugin;
    private final ProtocolSpec spec;
    private final FrameDecoder<UUID> decoder = new FrameDecoder<>(FrameLimits.SERVERBOUND);
    private final FrameEncoder encoder = new FrameEncoder(FrameLimits.CLIENTBOUND);
    private final Map<Class<?>, BiConsumer<Player, Object>> handlers = new ConcurrentHashMap<>();
    private volatile ModRequirement requirement = ModRequirement.optional();
    private volatile BiPredicate<Player, Object> gate = (player, packet) -> true;
    /** The kinds of failure already logged as a warning; see {@link #reportFailure}. */
    private final Set<Object> reported = ConcurrentHashMap.newKeySet();

    ServerChannel(ModBridge.Runtime runtime, Plugin plugin, ProtocolSpec spec) {
        this.runtime = runtime;
        this.plugin = plugin;
        this.spec = spec;
    }

    public ProtocolSpec spec() {
        return spec;
    }

    public Plugin plugin() {
        return plugin;
    }

    /**
     * Handles a client-to-server packet type. Registering the same type again replaces the handler.
     *
     * @throws IllegalArgumentException if {@code type} is not a C2S packet of {@link #spec()}
     */
    public <T> void on(Class<T> type, BiConsumer<Player, ? super T> handler) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(handler, "handler");
        if (!spec.packetTypes(Direction.C2S).contains(type)) {
            throw new IllegalArgumentException(type.getName() + " is not a C2S packet of " + spec.channel());
        }
        handlers.put(type, (player, packet) -> handler.accept(player, type.cast(packet)));
    }

    /**
     * Sends a server-to-client packet, split into frames as needed.
     *
     * @return false if the player is offline, has not completed the handshake for this protocol, or the
     *         packet exceeds the message size (logged)
     * @throws IllegalArgumentException if {@code packet} is not an S2C packet of {@link #spec()}
     */
    public boolean send(Player player, Object packet) {
        Objects.requireNonNull(packet, "packet");
        byte[] message = spec.encode(Direction.S2C, packet);
        if (player == null || !player.isOnline() || !isReady(player)) {
            return false;
        }
        List<byte[]> frames;
        try {
            frames = encoder.encode(message);
        } catch (ProtocolException e) {
            plugin.getLogger().warning("not sending " + packet.getClass().getSimpleName() + " to " + player.getName()
                    + ": " + e.getMessage());
            return false;
        }
        // CraftPlayer drops a plugin message silently when the client has not registered the channel; the
        // handshake is what guarantees it has.
        for (byte[] frame : frames) {
            player.sendPluginMessage(plugin, spec.channel(), frame);
        }
        return true;
    }

    /** True once this player's client mod was accepted for this protocol on the current connection. */
    public boolean isReady(Player player) {
        return runtime.isReady(player.getUniqueId(), spec.namespace());
    }

    /**
     * Runs after a packet decodes and before its handler, for every C2S packet on this channel.
     * Returning false drops the packet silently.
     *
     * <p>This is where a rate limit belongs. A modified client can send as fast as it likes, and a
     * check inside each handler is one that a later handler forgets; one gate cannot be forgotten.
     * It runs on the main thread, so keep it cheap, and note that it sees the decoded packet — the
     * cost of decoding is already paid. (That cost has its own limit: DPP-Core drops frames past a
     * per-player decode budget before decoding them.)
     *
     * <p>A gate that throws drops the packet. Its first failure is logged as a warning, later ones at FINE,
     * and the same goes for each packet type's handler.
     */
    public void gate(BiPredicate<Player, Object> gate) {
        this.gate = Objects.requireNonNull(gate, "gate");
    }

    public ModRequirement requirement() {
        return requirement;
    }

    /** Applies from the next join; players already online are not re-checked. */
    public void require(ModRequirement requirement) {
        this.requirement = Objects.requireNonNull(requirement, "requirement");
    }

    // ------------------------------------------------------------------ called by ModBridge

    void receive(Player player, byte[] frame) {
        UUID id = player.getUniqueId();
        if (!runtime.isReady(id, spec.namespace())) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!runtime.charge(id, frame, now)) {
            plugin.getLogger().fine("dropped a " + spec.channel() + " frame from " + player.getName()
                    + ": over the decode budget");
            return;
        }
        Object packet;
        try {
            byte[] message = decoder.accept(id, frame, now);
            if (message == null) {
                return;
            }
            packet = spec.decode(Direction.C2S, message);
        } catch (ProtocolException e) {
            // Expected input: any client can open this channel and send arbitrary bytes.
            plugin.getLogger().fine("dropped a malformed " + spec.channel() + " frame from " + player.getName()
                    + ": " + e.getMessage());
            return;
        }
        boolean pass;
        try {
            pass = gate.test(player, packet);
        } catch (RuntimeException e) {
            // Dropped, as a gate saying no would drop it.
            reportFailure(GATE, "the gate on " + spec.channel() + " threw on " + packet.getClass().getSimpleName()
                    + " from " + player.getName() + "; the packet was dropped", e);
            return;
        }
        if (!pass) {
            return;
        }
        BiConsumer<Player, Object> handler = handlers.get(packet.getClass());
        if (handler == null) {
            plugin.getLogger().fine("no handler for " + packet.getClass().getSimpleName() + " on " + spec.channel());
            return;
        }
        try {
            handler.accept(player, packet);
        } catch (RuntimeException e) {
            reportFailure(packet.getClass(), "error handling " + packet.getClass().getSimpleName() + " from "
                    + player.getName(), e);
        }
    }

    /**
     * Logs the first failure of each kind (the gate, or a packet type's handler) as a warning and the rest at FINE.
     * A client that finds the input which makes plugin code throw can send it with every packet, and a stack trace
     * per packet would fill the log.
     */
    private void reportFailure(Object kind, String message, RuntimeException e) {
        if (reported.add(kind)) {
            plugin.getLogger().log(Level.WARNING, message + " (further failures of this kind are logged at FINE)", e);
        } else {
            plugin.getLogger().log(Level.FINE, message, e);
        }
    }

    void forget(UUID player) {
        decoder.forget(player);
    }

    void expire(long nowMillis) {
        decoder.expire(nowMillis);
    }
}
