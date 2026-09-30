package com.darksoldier1404.dppc.modbridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.darksoldier1404.dppc.support.PluginTest;
import com.darksoldier1404.dppmc.protocol.Direction;
import com.darksoldier1404.dppmc.protocol.PacketCodec;
import com.darksoldier1404.dppmc.protocol.ProtocolSpec;
import com.darksoldier1404.dppmc.protocol.core.CoreProtocol;
import com.darksoldier1404.dppmc.protocol.core.Hello;
import com.darksoldier1404.dppmc.protocol.core.ModOffer;
import com.darksoldier1404.dppmc.protocol.frame.FrameEncoder;
import com.darksoldier1404.dppmc.protocol.frame.FrameLimits;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** ModBridge on a mocked server: a bound channel, a player whose client said hello, and frames from that client. */
class ModBridgeTest extends PluginTest {
    record Ping(int nonce) {
    }

    private static final ProtocolSpec SPEC = ProtocolSpec.builder("dp_test", 1)
            .c2s(0, Ping.class, PacketCodec.of((buf, ping) -> buf.writeInt(ping.nonce()), buf -> new Ping(buf.readInt())))
            .build();

    private final List<Integer> handled = new ArrayList<>();
    private final List<LogRecord> logged = new ArrayList<>();
    private ServerChannel channel;
    private PlayerMock player;

    @BeforeEach
    void bindAndSayHello() {
        Plugin owner = MockBukkit.createMockPlugin();
        owner.getLogger().setLevel(Level.ALL);
        owner.getLogger().addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        channel = ModBridge.bind(owner, SPEC);
        channel.on(Ping.class, (who, ping) -> handled.add(ping.nonce()));
        player = server.addPlayer();
        byte[] hello = CoreProtocol.SPEC.encode(Direction.C2S, new Hello(CoreProtocol.VERSION, "1.1.0", "1.21.11",
                List.of(new ModOffer("dp_test", "1.0.0", 1, 1))));
        send(CoreProtocol.SPEC.channel(), new FrameEncoder(FrameLimits.SERVERBOUND).encode(hello));
        assertTrue(channel.isReady(player));
    }

    private void send(String channelName, List<byte[]> frames) {
        for (byte[] frame : frames) {
            server.getMessenger().dispatchIncomingMessage(player, channelName, frame);
        }
    }

    private void ping(int nonce) {
        send(SPEC.channel(), new FrameEncoder(FrameLimits.SERVERBOUND).encode(SPEC.encode(Direction.C2S, new Ping(nonce))));
    }

    private long count(Level level) {
        return logged.stream().filter(record -> record.getLevel() == level).count();
    }

    @Test
    void aPacketReachesItsHandler() {
        ping(7);
        assertEquals(List.of(7), handled);
    }

    @Test
    void aGateThatThrowsDropsThePacketAndWarnsOnce() {
        channel.gate((who, packet) -> {
            throw new IllegalStateException("a bug in the gate");
        });
        ping(1);
        ping(2);
        ping(3);
        assertEquals(List.of(), handled);
        assertEquals(1, count(Level.WARNING));
        assertEquals(2, count(Level.FINE));
    }

    @Test
    void aHandlerThatThrowsWarnsOnceForItsPacketType() {
        channel.on(Ping.class, (who, ping) -> {
            throw new IllegalStateException("a bug in the handler");
        });
        ping(1);
        ping(2);
        assertEquals(1, count(Level.WARNING));
        assertEquals(1, count(Level.FINE));
    }

    @Test
    void framesThatClaimFullInflationsRunThePlayersBudgetDry() {
        // A few hundred bytes each, and each makes the decoder allocate and inflate the full message size.
        byte[] bomb = new FrameEncoder(FrameLimits.SERVERBOUND).encode(new byte[FrameLimits.SERVERBOUND.maxMessageBytes()]).get(0);
        assertTrue(bomb.length < 64 * 1024);
        for (int i = 0; i < 3; i++) {
            send(SPEC.channel(), List.of(bomb));
        }
        List<String> fine = logged.stream().filter(record -> record.getLevel() == Level.FINE)
                .map(LogRecord::getMessage).toList();
        assertEquals(3, fine.size(), fine.toString());
        assertTrue(fine.get(0).contains("malformed"), fine.get(0));
        assertTrue(fine.get(1).contains("malformed"), fine.get(1));
        assertTrue(fine.get(2).contains("over the decode budget"), fine.get(2));
    }
}
