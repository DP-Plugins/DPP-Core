package com.darksoldier1404.dppc.modbridge;

import com.darksoldier1404.dppmc.protocol.core.Verdict;

/** {@link ModRequirement} decisions, free of Bukkit so they are tested directly (from DP-HudShop's HandshakePolicy). */
final class RequirementCheck {
    enum Kick {
        NONE,
        /** Required, and no accepted hello for this protocol. */
        MISSING,
        /** Required, and the client's protocol version is incompatible. */
        MISMATCH
    }

    private RequirementCheck() {
    }

    /** @param verdict the server's verdict for this protocol, or null when no hello arrived */
    static Kick evaluate(ModRequirement requirement, boolean bypass, Verdict verdict) {
        if (!requirement.required() || bypass || verdict == Verdict.ACCEPTED) {
            return Kick.NONE;
        }
        return verdict == Verdict.CLIENT_TOO_OLD || verdict == Verdict.SERVER_TOO_OLD ? Kick.MISMATCH : Kick.MISSING;
    }
}
