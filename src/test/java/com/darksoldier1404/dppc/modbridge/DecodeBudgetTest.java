package com.darksoldier1404.dppc.modbridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darksoldier1404.dppmc.protocol.DBuf;
import com.darksoldier1404.dppmc.protocol.frame.FrameEncoder;
import com.darksoldier1404.dppmc.protocol.frame.FrameLimits;
import java.util.List;
import org.junit.jupiter.api.Test;

class DecodeBudgetTest {
    private static final int FULL = FrameLimits.SERVERBOUND.maxMessageBytes();

    @Test
    void aCompressedFrameCostsWhatItClaimsToInflateTo() {
        byte[] small = new FrameEncoder(FrameLimits.SERVERBOUND).encode(new byte[]{1, 2, 3}).get(0);
        assertEquals(small.length, DecodeBudget.costOf(small));

        List<byte[]> zeros = new FrameEncoder(FrameLimits.SERVERBOUND).encode(new byte[FULL]);
        assertEquals(1, zeros.size());
        assertTrue(zeros.get(0).length < 64 * 1024);
        assertEquals(FULL, DecodeBudget.costOf(zeros.get(0)));
    }

    @Test
    void aChunkedTransferPaysItsClaimOnTheFirstChunkOnly() {
        byte[] first = DBuf.writer().writeByte(0x03).writeVarInt(7).writeVarInt(0).writeVarInt(3)
                .writeVarInt(FULL).writeRaw(new byte[100]).toByteArray();
        byte[] second = DBuf.writer().writeByte(0x03).writeVarInt(7).writeVarInt(1).writeVarInt(3)
                .writeRaw(new byte[100]).toByteArray();
        assertEquals(FULL, DecodeBudget.costOf(first));
        assertEquals(second.length, DecodeBudget.costOf(second));
    }

    @Test
    void aHeaderThatDoesNotParseCostsItsBytes() {
        assertEquals(0, DecodeBudget.costOf(new byte[0]));
        assertEquals(1, DecodeBudget.costOf(new byte[]{0x01}));
        assertEquals(2, DecodeBudget.costOf(new byte[]{0x03, (byte) 0x80}));
    }

    @Test
    void theBucketRefillsOverTimeUpToItsCapacity() {
        DecodeBudget budget = new DecodeBudget(100, 10);
        assertTrue(budget.tryCharge(100, 0));
        assertFalse(budget.tryCharge(1, 0));
        assertFalse(budget.tryCharge(1, 99));
        assertTrue(budget.tryCharge(1, 100));

        assertFalse(budget.tryCharge(101, 1_000_000), "never more than the capacity");
        assertTrue(budget.tryCharge(100, 1_000_000));
    }

    @Test
    void aRefusedChargeTakesNothing() {
        DecodeBudget budget = new DecodeBudget(100, 10);
        assertTrue(budget.tryCharge(100, 0));
        assertFalse(budget.tryCharge(60, 5_000));
        assertTrue(budget.tryCharge(50, 5_000));
    }

    @Test
    void twoFullMessagesPassAtOnceAndThenOneEveryTwoSeconds() {
        DecodeBudget budget = new DecodeBudget();
        assertTrue(budget.tryCharge(FULL, 0));
        assertTrue(budget.tryCharge(FULL, 0));
        assertFalse(budget.tryCharge(FULL, 0));
        assertFalse(budget.tryCharge(FULL, 1_999));
        assertTrue(budget.tryCharge(FULL, 2_000));
    }

    @Test
    void aClientWithinItsPacerNeverRunsDry() {
        // 15 full frames a second for a minute, the most SendPacer lets through after its burst.
        DecodeBudget budget = new DecodeBudget();
        for (int frame = 0; frame < 15 * 60; frame++) {
            assertTrue(budget.tryCharge(FrameLimits.SERVERBOUND.maxFrameBytes(), frame * 1000L / 15), "frame " + frame);
        }
    }
}
