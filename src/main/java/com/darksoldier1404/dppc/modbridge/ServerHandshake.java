package com.darksoldier1404.dppc.modbridge;

import com.darksoldier1404.dppmc.protocol.Direction;
import com.darksoldier1404.dppmc.protocol.ProtocolException;
import com.darksoldier1404.dppmc.protocol.ProtocolSpec;
import com.darksoldier1404.dppmc.protocol.core.CoreProtocol;
import com.darksoldier1404.dppmc.protocol.core.HandshakeRules;
import com.darksoldier1404.dppmc.protocol.core.Hello;
import com.darksoldier1404.dppmc.protocol.core.HelloAck;
import com.darksoldier1404.dppmc.protocol.core.ModOffer;
import com.darksoldier1404.dppmc.protocol.core.ModVerdict;
import com.darksoldier1404.dppmc.protocol.core.Verdict;
import com.darksoldier1404.dppmc.protocol.frame.FrameDecoder;
import com.darksoldier1404.dppmc.protocol.frame.FrameEncoder;
import com.darksoldier1404.dppmc.protocol.frame.FrameLimits;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The server half of the {@code dppmc:core} handshake (design §6.2), with no Bukkit types so it is tested
 * directly. A client repeats its hello until answered, so a hello is answered again, but only a changed
 * verdict counts as news for events and kicks.
 *
 * <p>This channel is read before any handshake, so anyone can send on it: it reassembles at most
 * {@link CoreProtocol#MAX_MESSAGE_BYTES} one transfer at a time ({@link CoreProtocol#SERVERBOUND_LIMITS}; the
 * largest hello, 256 mods with the longest names, is about 135 KB), and answers a player at most once per
 * {@link #MIN_HELLO_GAP_MILLIS}. A client retries once a second; a faster stream of hellos, alternating versions
 * to flip a verdict back and forth, would otherwise fire a ready event, and whatever a plugin sends on it, each time.
 */
final class ServerHandshake {
    /**
     * @param ackFrames     the HelloAck frames to send back on {@code dppmc:core}
     * @param newlyReady    offers accepted for the first time on this connection
     * @param newlyRefused  offers for protocols this server binds that were refused for the first time
     */
    record Answer(List<byte[]> ackFrames, ModSession session, List<ModOffer> newlyReady,
                  Map<ModOffer, Verdict> newlyRefused) {
    }

    /** Half the client's retry interval ({@code CoreProtocol.HELLO_INTERVAL_TICKS}, one second). */
    static final long MIN_HELLO_GAP_MILLIS = 500;

    private final Supplier<Map<String, ProtocolSpec>> boundSpecs;
    private final String bridgeVersion;
    private final FrameDecoder<UUID> decoder = new FrameDecoder<>(CoreProtocol.SERVERBOUND_LIMITS);
    private final Map<UUID, Long> lastAnswer = new ConcurrentHashMap<>();
    private final FrameEncoder encoder = new FrameEncoder(CoreProtocol.CLIENTBOUND_LIMITS);
    private final Map<UUID, ModSession> sessions = new ConcurrentHashMap<>();

    /** @param boundSpecs the specs bound on this server right now, keyed by namespace */
    ServerHandshake(Supplier<Map<String, ProtocolSpec>> boundSpecs, String bridgeVersion) {
        this.boundSpecs = boundSpecs;
        this.bridgeVersion = bridgeVersion;
    }

    /**
     * @return the answer, or empty while a chunked hello is still arriving or when the player's last hello was
     *         answered less than {@link #MIN_HELLO_GAP_MILLIS} ago
     * @throws ProtocolException on a malformed frame or message; the caller drops it
     */
    Optional<Answer> accept(UUID player, byte[] frame, long nowMillis) {
        byte[] message = decoder.accept(player, frame, nowMillis);
        if (message == null) {
            return Optional.empty();
        }
        Long last = lastAnswer.get(player);
        if (last != null && nowMillis - last < MIN_HELLO_GAP_MILLIS) {
            return Optional.empty();
        }
        if (!(CoreProtocol.SPEC.decode(Direction.C2S, message) instanceof Hello hello)) {
            throw new ProtocolException("expected a Hello on " + CoreProtocol.SPEC.channel());
        }
        Map<String, ProtocolSpec> specs = boundSpecs.get();
        HelloAck ack = HandshakeRules.answer(hello, specs, bridgeVersion, FrameLimits.SERVERBOUND, FrameLimits.CLIENTBOUND);
        Map<String, Verdict> verdicts = new LinkedHashMap<>();
        for (ModVerdict verdict : ack.verdicts()) {
            verdicts.put(verdict.modId(), verdict.verdict());
        }
        ModSession session = new ModSession(hello.coreVersion(), hello.minecraftVersion(), hello.protocolVersion(),
                hello.mods(), verdicts);
        ModSession previous = sessions.put(player, session);
        Map<String, Verdict> before = previous == null ? Map.of() : previous.verdicts();

        List<ModOffer> newlyReady = new ArrayList<>();
        Map<ModOffer, Verdict> newlyRefused = new HashMap<>();
        for (ModOffer offer : hello.mods()) {
            Verdict verdict = verdicts.get(offer.modId());
            if (verdict == before.get(offer.modId())) {
                continue;
            }
            if (verdict == Verdict.ACCEPTED) {
                newlyReady.add(offer);
            } else if (verdict != Verdict.NOT_ON_SERVER) {
                newlyRefused.put(offer, verdict);
            }
        }
        List<byte[]> frames = encoder.encode(CoreProtocol.SPEC.encode(Direction.S2C, ack));
        lastAnswer.put(player, nowMillis);
        return Optional.of(new Answer(frames, session, newlyReady, newlyRefused));
    }

    Optional<ModSession> session(UUID player) {
        return Optional.ofNullable(sessions.get(player));
    }

    boolean isReady(UUID player, String namespace) {
        ModSession session = sessions.get(player);
        return session != null && session.isReady(namespace);
    }

    void forget(UUID player) {
        sessions.remove(player);
        lastAnswer.remove(player);
        decoder.forget(player);
    }

    void expire(long nowMillis) {
        decoder.expire(nowMillis);
    }
}
