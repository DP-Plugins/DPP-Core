package com.darksoldier1404.dppc.modbridge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.darksoldier1404.dppmc.protocol.Direction;
import com.darksoldier1404.dppmc.protocol.ProtocolSpec;
import com.darksoldier1404.dppmc.protocol.core.CoreProtocol;
import com.darksoldier1404.dppmc.protocol.core.Verdict;
import com.darksoldier1404.dppmc.protocol.fixtures.WireFixtures;
import com.darksoldier1404.dppmc.protocol.frame.FrameDecoder;
import com.darksoldier1404.dppmc.protocol.frame.FrameEncoder;
import com.darksoldier1404.dppmc.protocol.frame.FrameLimits;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The hand-computed frames DPP-ModCore commits in its protocol test fixtures, read and written by the server side. */
class WireFixtureCompatibilityTest {
    @Test
    void theServerDecodesTheClientHelloFixture() {
        byte[] message = new FrameDecoder<UUID>(FrameLimits.SERVERBOUND)
                .accept(UUID.randomUUID(), WireFixtures.bytes(WireFixtures.HELLO_FRAME_HEX), 0);
        assertEquals(WireFixtures.helloSample(), CoreProtocol.SPEC.decode(Direction.C2S, message));
    }

    @Test
    void theHandshakeAcceptsTheHelloFixture() {
        ServerHandshake handshake = new ServerHandshake(
                () -> Map.of("dp_hudshop", ProtocolSpec.builder("dp_hudshop", 3).minCompatible(2).build()), "5.4.2");
        UUID player = UUID.randomUUID();
        handshake.accept(player, WireFixtures.bytes(WireFixtures.HELLO_FRAME_HEX), 0);
        assertEquals(Verdict.ACCEPTED, handshake.session(player).orElseThrow().verdicts().get("dp_hudshop"));
    }

    @Test
    void theServerChunksExactlyLikeTheFixture() {
        List<String> frames = new FrameEncoder(WireFixtures.chunkLimits()).encode(WireFixtures.chunkedMessage())
                .stream().map(HexFormat.of()::formatHex).toList();
        assertEquals(WireFixtures.chunkedFramesHex(), frames);
    }
}
