package com.darksoldier1404.dppc.modbridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darksoldier1404.dppmc.protocol.Direction;
import com.darksoldier1404.dppmc.protocol.ProtocolException;
import com.darksoldier1404.dppmc.protocol.ProtocolSpec;
import com.darksoldier1404.dppmc.protocol.core.CoreProtocol;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServerHandshakeTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final ModOffer SHOP = new ModOffer("dp_hudshop", "1.2.0", 4, 2);
    private static final ModOffer PAINT = new ModOffer("dp_paint", "1.0.0", 1, 1);

    private final Map<String, ProtocolSpec> bound = new HashMap<>(
            Map.of("dp_hudshop", ProtocolSpec.builder("dp_hudshop", 5).minCompatible(3).build()));
    private final ServerHandshake handshake = new ServerHandshake(() -> bound, "5.4.2");

    private static byte[] helloFrame(ModOffer... offers) {
        byte[] message = CoreProtocol.SPEC.encode(Direction.C2S, new Hello(CoreProtocol.VERSION, "0.1.0", "1.21.11", List.of(offers)));
        return new FrameEncoder(FrameLimits.SERVERBOUND).encode(message).get(0);
    }

    private static HelloAck ack(ServerHandshake.Answer answer) {
        FrameDecoder<String> decoder = new FrameDecoder<>(CoreProtocol.CLIENTBOUND_LIMITS);   // as the client reads it
        byte[] message = null;
        for (byte[] frame : answer.ackFrames()) {
            message = decoder.accept("server", frame, 0);
        }
        return (HelloAck) CoreProtocol.SPEC.decode(Direction.S2C, message);
    }

    @Test
    void helloIsAnsweredWithVerdictsAndLimits() {
        ServerHandshake.Answer answer = handshake.accept(PLAYER, helloFrame(SHOP, PAINT), 0).orElseThrow();

        HelloAck ack = ack(answer);
        assertEquals("5.4.2", ack.coreVersion());
        assertEquals(List.of(new ModVerdict("dp_hudshop", Verdict.ACCEPTED), new ModVerdict("dp_paint", Verdict.NOT_ON_SERVER)),
                ack.verdicts());
        assertEquals(FrameLimits.SERVERBOUND.maxFrameBytes(), ack.maxServerboundFrame());
        assertEquals(List.of(SHOP), answer.newlyReady());
        assertEquals(Map.of(), answer.newlyRefused());
        assertTrue(handshake.isReady(PLAYER, "dp_hudshop"));
        assertFalse(handshake.isReady(PLAYER, "dp_paint"));
        assertEquals("1.21.11", handshake.session(PLAYER).orElseThrow().minecraftVersion());
    }

    @Test
    void aRepeatedHelloIsAnsweredAgainButIsNotNews() {
        handshake.accept(PLAYER, helloFrame(SHOP), 0);
        ServerHandshake.Answer again = handshake.accept(PLAYER, helloFrame(SHOP), 1_000).orElseThrow();
        assertFalse(again.ackFrames().isEmpty());
        assertEquals(List.of(), again.newlyReady());
    }

    @Test
    void incompatibleVersionsAreReportedOnce() {
        ModOffer old = new ModOffer("dp_hudshop", "0.9.0", 2, 1);
        ServerHandshake.Answer answer = handshake.accept(PLAYER, helloFrame(old), 0).orElseThrow();
        assertEquals(Map.of(old, Verdict.CLIENT_TOO_OLD), answer.newlyRefused());
        assertEquals(Map.of(), handshake.accept(PLAYER, helloFrame(old), 1_000).orElseThrow().newlyRefused());
        assertFalse(handshake.isReady(PLAYER, "dp_hudshop"));
    }

    @Test
    void aProtocolBoundLaterIsNewsOnTheNextHello() {
        handshake.accept(PLAYER, helloFrame(SHOP, PAINT), 0);
        bound.put("dp_paint", ProtocolSpec.builder("dp_paint", 1).build());
        assertEquals(List.of(PAINT), handshake.accept(PLAYER, helloFrame(SHOP, PAINT), 1_000).orElseThrow().newlyReady());
    }

    @Test
    void malformedInputIsRejectedAndForgetClearsTheSession() {
        assertThrows(ProtocolException.class, () -> handshake.accept(PLAYER, new byte[]{0x04}, 0));
        byte[] ackAsHello = new FrameEncoder(FrameLimits.SERVERBOUND).encode(new byte[]{0x05, 0x00}).get(0);
        assertThrows(ProtocolException.class, () -> handshake.accept(PLAYER, ackAsHello, 0));

        handshake.accept(PLAYER, helloFrame(SHOP), 0);
        handshake.forget(PLAYER);
        assertTrue(handshake.session(PLAYER).isEmpty());
    }

    @Test
    void helloesFasterThanTheClientRetriesAreNotAnswered() {
        ModOffer old = new ModOffer("dp_hudshop", "0.9.0", 2, 1);
        assertEquals(List.of(SHOP), handshake.accept(PLAYER, helloFrame(SHOP), 10_000).orElseThrow().newlyReady());
        // Flipping versions to fire a refused event, then a ready one, as fast as frames arrive:
        assertTrue(handshake.accept(PLAYER, helloFrame(old), 10_100).isEmpty());
        assertTrue(handshake.isReady(PLAYER, "dp_hudshop"));
        // A client's own retry, a second later, is answered.
        assertEquals(Map.of(old, Verdict.CLIENT_TOO_OLD),
                handshake.accept(PLAYER, helloFrame(old), 11_000).orElseThrow().newlyRefused());

        handshake.forget(PLAYER);
        assertTrue(handshake.accept(PLAYER, helloFrame(SHOP), 11_000).isPresent(), "a new connection is answered at once");
    }

    @Test
    void theCoreChannelReassemblesTheLargestHelloButNoMore() {
        String longest = "x".repeat(250);
        List<ModOffer> offers = new ArrayList<>();
        for (int i = 0; i < Hello.MAX_MODS; i++) {
            offers.add(new ModOffer(String.format("%03d", i) + longest, longest, Integer.MAX_VALUE, Integer.MAX_VALUE));
        }
        byte[] largest = CoreProtocol.SPEC.encode(Direction.C2S,
                new Hello(CoreProtocol.VERSION, longest, longest, offers));
        ServerHandshake.Answer answer = null;
        for (byte[] frame : new FrameEncoder(FrameLimits.SERVERBOUND).encode(largest)) {
            answer = handshake.accept(PLAYER, frame, 0).orElse(null);
        }
        assertEquals(Hello.MAX_MODS, answer.session().mods().size());
        assertEquals(Hello.MAX_MODS, ack(answer).verdicts().size(), "and the answer fits the client's core limits");

        // A frame of a few hundred bytes that inflates to one byte past the limit is refused before inflating.
        FrameEncoder generous = new FrameEncoder(FrameLimits.SERVERBOUND);
        byte[] fits = generous.encode(new byte[CoreProtocol.MAX_MESSAGE_BYTES]).get(0);
        byte[] over = generous.encode(new byte[CoreProtocol.MAX_MESSAGE_BYTES + 1]).get(0);
        FrameDecoder<UUID> decoder = new FrameDecoder<>(CoreProtocol.SERVERBOUND_LIMITS);
        assertEquals(CoreProtocol.MAX_MESSAGE_BYTES, decoder.accept(PLAYER, fits, 0).length);
        assertTrue(over.length < 1024, "over is " + over.length);
        ProtocolException refused = assertThrows(ProtocolException.class, () -> handshake.accept(PLAYER, over, 1_000));
        assertTrue(refused.getMessage().contains("out of range 0.." + CoreProtocol.MAX_MESSAGE_BYTES), refused.getMessage());
    }
}
