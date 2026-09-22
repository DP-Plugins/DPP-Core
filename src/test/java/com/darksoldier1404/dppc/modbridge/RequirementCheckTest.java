package com.darksoldier1404.dppc.modbridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.darksoldier1404.dppc.modbridge.RequirementCheck.Kick;
import com.darksoldier1404.dppmc.protocol.core.Verdict;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** The cases of DP-HudShop's HandshakePolicyTest, per protocol. */
class RequirementCheckTest {
    private static final ModRequirement REQUIRED = ModRequirement.mandatory();

    @Test
    void optionalNeverKicks() {
        assertEquals(Kick.NONE, RequirementCheck.evaluate(ModRequirement.optional(), false, null));
        assertEquals(Kick.NONE, RequirementCheck.evaluate(ModRequirement.optional(), false, Verdict.CLIENT_TOO_OLD));
    }

    @Test
    void requiredKicksMissingOrMismatchedClients() {
        assertEquals(Kick.MISSING, RequirementCheck.evaluate(REQUIRED, false, null));
        assertEquals(Kick.MISMATCH, RequirementCheck.evaluate(REQUIRED, false, Verdict.CLIENT_TOO_OLD));
        assertEquals(Kick.MISMATCH, RequirementCheck.evaluate(REQUIRED, false, Verdict.SERVER_TOO_OLD));
        assertEquals(Kick.NONE, RequirementCheck.evaluate(REQUIRED, false, Verdict.ACCEPTED));
    }

    @Test
    void bypassPermissionWins() {
        assertEquals(Kick.NONE, RequirementCheck.evaluate(REQUIRED, true, null));
        assertEquals(Kick.NONE, RequirementCheck.evaluate(REQUIRED, true, Verdict.CLIENT_TOO_OLD));
    }

    @Test
    void requirementValidatesAndCopies() {
        assertThrows(IllegalArgumentException.class, () -> REQUIRED.withGrace(Duration.ZERO));
        ModRequirement custom = REQUIRED.withGrace(Duration.ofSeconds(30)).withBypassPermission("dphs.bypass.clientmod")
                .withMessages("need {mod}", "update {mod}");
        assertEquals(Duration.ofSeconds(30), custom.grace());
        assertEquals("dphs.bypass.clientmod", custom.bypassPermission());
        assertEquals("update {mod}", custom.mismatchMessage());
    }
}
