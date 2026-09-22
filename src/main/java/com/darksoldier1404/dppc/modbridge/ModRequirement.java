package com.darksoldier1404.dppc.modbridge;

import java.time.Duration;
import java.util.Objects;

/**
 * Whether players must have a client mod (design §6.4), taken over from DP-HudShop's {@code client-mod.*}
 * settings. A player who lacks the mod, or whose mod speaks an incompatible protocol, is kicked once
 * {@link #grace()} has passed since joining; an incompatible mod is kicked at once. {@code {mod}} in a message
 * is replaced with the protocol namespace.
 *
 * @param bypassPermission players with this permission are never kicked; null for none
 */
public record ModRequirement(boolean required, Duration grace, String bypassPermission,
                             String missingMessage, String mismatchMessage) {
    public static final Duration DEFAULT_GRACE = Duration.ofSeconds(10);
    public static final String DEFAULT_MISSING = "This server requires the {mod} client mod.";
    public static final String DEFAULT_MISMATCH = "Your {mod} client mod does not match this server's version.";

    public ModRequirement {
        Objects.requireNonNull(grace, "grace");
        Objects.requireNonNull(missingMessage, "missingMessage");
        Objects.requireNonNull(mismatchMessage, "mismatchMessage");
        if (grace.isNegative() || grace.isZero()) {
            throw new IllegalArgumentException("grace must be positive: " + grace);
        }
    }

    /** Players without the mod may play. */
    public static ModRequirement optional() {
        return new ModRequirement(false, DEFAULT_GRACE, null, DEFAULT_MISSING, DEFAULT_MISMATCH);
    }

    /** Players without the mod, or with an incompatible one, are kicked. */
    public static ModRequirement mandatory() {
        return new ModRequirement(true, DEFAULT_GRACE, null, DEFAULT_MISSING, DEFAULT_MISMATCH);
    }

    public ModRequirement withGrace(Duration grace) {
        return new ModRequirement(required, grace, bypassPermission, missingMessage, mismatchMessage);
    }

    public ModRequirement withBypassPermission(String permission) {
        return new ModRequirement(required, grace, permission, missingMessage, mismatchMessage);
    }

    public ModRequirement withMessages(String missing, String mismatch) {
        return new ModRequirement(required, grace, bypassPermission, missing, mismatch);
    }
}
