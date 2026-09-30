package com.darksoldier1404.dppc.data.sync;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A change announcement broadcast over the sync channel.
 * <p>
 * Wire format: {@code serverId|containerId|op|key}. The key is the final field and
 * is allowed to contain the {@code |} delimiter; the other three fields never do.
 */
@DPPCoreVersion(since = "5.4.4")
public final class SyncMessage {

    private static final char DELIM = '|';

    private final String serverId;
    private final String containerId;
    private final SyncOp op;
    private final String key;

    public SyncMessage(@NotNull String serverId, @NotNull String containerId, @NotNull SyncOp op, @NotNull String key) {
        this.serverId = serverId;
        this.containerId = containerId;
        this.op = op;
        this.key = key;
    }

    public String getServerId() {
        return serverId;
    }

    public String getContainerId() {
        return containerId;
    }

    public SyncOp getOp() {
        return op;
    }

    public String getKey() {
        return key;
    }

    @NotNull
    public String encode() {
        return serverId + DELIM + containerId + DELIM + op.name() + DELIM + key;
    }

    /**
     * Parses a wire message, or returns {@code null} if it is malformed.
     */
    @Nullable
    public static SyncMessage decode(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        // Limit to 4 so a key containing '|' survives intact in the last field.
        String[] parts = raw.split("\\" + DELIM, 4);
        if (parts.length < 4) {
            return null;
        }
        SyncOp op;
        try {
            op = SyncOp.valueOf(parts[2]);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return new SyncMessage(parts[0], parts[1], op, parts[3]);
    }
}
