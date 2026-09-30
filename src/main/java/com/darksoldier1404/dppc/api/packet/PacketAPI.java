package com.darksoldier1404.dppc.api.packet;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import com.darksoldier1404.dppc.utils.PluginUtil;
import com.darksoldier1404.dppc.utils.enums.DependPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

@DPPCoreVersion(since = "5.5.0")
public class PacketAPI {
    // Real world entity IDs never get anywhere near this range, so fake IDs handed out
    // from here can't collide with a legitimately spawned entity.
    private static final AtomicInteger FAKE_ENTITY_ID = new AtomicInteger(2_000_000_000);

    public static boolean isEnabled() {
        return PluginUtil.isDependPluginLoaded(DependPlugin.ProtocolLib);
    }

    @Nullable
    public static ProtocolManager manager() {
        return isEnabled() ? ProtocolLibrary.getProtocolManager() : null;
    }

    public static int nextFakeEntityId() {
        return FAKE_ENTITY_ID.incrementAndGet();
    }
}
