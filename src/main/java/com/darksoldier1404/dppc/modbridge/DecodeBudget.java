package com.darksoldier1404.dppc.modbridge;

import com.darksoldier1404.dppmc.protocol.DBuf;
import com.darksoldier1404.dppmc.protocol.ProtocolException;
import com.darksoldier1404.dppmc.protocol.frame.FrameLimits;

/**
 * How many bytes one player may make the server decode, charged before a frame is decoded. A compressed frame of
 * at most 32 KB may claim to inflate to the whole message limit (16 MB), which the decoder then allocates and
 * inflates on the main thread; charging the claim rather than the frame is what stops a client from repeating that.
 *
 * <p>A token bucket shared by all of a player's channels: {@link #CAPACITY} lets two messages of the full size
 * through back to back, and {@link #REFILL_PER_SECOND} is far above the frames a client's own pacer sends (15 a
 * second, about 480 KB). What runs a player dry is claiming full-size inflations over and over — more than one
 * every two seconds — which a real client does not do: a mod uploads a large model now and then, not twice a second.
 * Bukkit-free, so it is tested directly.
 */
final class DecodeBudget {
    /** Two full messages. */
    static final long CAPACITY = 2L * FrameLimits.SERVERBOUND.maxMessageBytes();
    /** Half a full message a second. */
    static final long REFILL_PER_SECOND = FrameLimits.SERVERBOUND.maxMessageBytes() / 2;

    private static final int COMPRESSED = 0x01;
    private static final int CHUNKED = 0x02;

    private final long capacity;
    private final double refillPerMilli;
    private double tokens;
    private long last;
    private boolean started;

    DecodeBudget() {
        this(CAPACITY, REFILL_PER_SECOND);
    }

    DecodeBudget(long capacity, long refillPerSecond) {
        this.capacity = capacity;
        this.refillPerMilli = refillPerSecond / 1000.0;
        this.tokens = capacity;
    }

    /** @return false when {@code cost} is more than is left; nothing is charged then */
    synchronized boolean tryCharge(long cost, long nowMillis) {
        if (!started) {
            started = true;
            last = nowMillis;
        } else if (nowMillis > last) {
            tokens = Math.min(capacity, tokens + (nowMillis - last) * refillPerMilli);
            last = nowMillis;
        }
        if (cost > tokens) {
            return false;
        }
        tokens -= cost;
        return true;
    }

    /**
     * What decoding {@code frame} can cost: the length a compressed message claims to inflate to, read from the
     * header of an unchunked frame or of a transfer's first chunk, and otherwise the frame's own bytes. A header
     * that does not parse costs its bytes, and the decoder then rejects it.
     */
    static long costOf(byte[] frame) {
        if (frame == null || frame.length == 0) {
            return 0;
        }
        try {
            DBuf in = DBuf.reader(frame);
            int flags = in.readUnsignedByte();
            if ((flags & COMPRESSED) == 0) {
                return frame.length;
            }
            if ((flags & CHUNKED) != 0) {
                in.readVarInt();
                int index = in.readVarInt();
                in.readVarInt();
                if (index != 0) {
                    return frame.length;
                }
            }
            // The compressed payload starts with the length it inflates to.
            return Math.max(frame.length, in.readVarInt() & 0xFFFFFFFFL);
        } catch (ProtocolException e) {
            return frame.length;
        }
    }
}
