package com.darksoldier1404.dppc.modbridge;

import com.darksoldier1404.dppmc.protocol.core.ModOffer;
import com.darksoldier1404.dppmc.protocol.core.Verdict;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What a player's DPP-ModCore client said in its last hello, and what the server answered.
 *
 * @param coreVersion      the client's DPP-ModCore version
 * @param minecraftVersion the client's Minecraft version (may differ from the server's behind ViaVersion)
 * @param mods             every protocol the client offered, including ones this server does not bind
 * @param verdicts         the server's answer per protocol namespace
 */
public record ModSession(String coreVersion, String minecraftVersion, int protocolVersion,
                         List<ModOffer> mods, Map<String, Verdict> verdicts) {
    public ModSession {
        mods = List.copyOf(mods);
        verdicts = Map.copyOf(verdicts);
    }

    public Optional<ModOffer> mod(String namespace) {
        return mods.stream().filter(offer -> offer.modId().equals(namespace)).findFirst();
    }

    public boolean isReady(String namespace) {
        return verdicts.get(namespace) == Verdict.ACCEPTED;
    }
}
