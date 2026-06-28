package com.darksoldier1404.dppc.utils;

import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.darksoldier1404.dppc.support.MockServerTest;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemStackUtilsTest extends MockServerTest {

    @Test
    void getPlayerHeadReturnsSinglePlayerHead() {
        PlayerMock player = server.addPlayer("Steve");
        ItemStack head = ItemStackUtils.getPlayerHead(player);
        assertEquals(Material.PLAYER_HEAD, head.getType());
        assertEquals(1, head.getAmount());
    }
}
