package com.darksoldier1404.dppc.builder.action.actions;

import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.support.MockServerTest;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Execute-behaviour tests for deterministic player-state actions.
 */
class ActionExecuteStateTest extends MockServerTest {

    private ActionContext ctx(PlayerMock player) {
        return new ActionContext(player);
    }

    @Test
    void setHunger() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        new SetHungerAction(7).execute(ctx(p));
        assertEquals(7, p.getFoodLevel());
    }

    @Test
    void setHungerClampsTo20() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        new SetHungerAction(999).execute(ctx(p));
        assertEquals(20, p.getFoodLevel());
    }

    @Test
    void setGamemode() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        new SetGamemodeAction("creative").execute(ctx(p));
        assertEquals(GameMode.CREATIVE, p.getGameMode());
    }

    @Test
    void setGamemodeIgnoresInvalidValue() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        GameMode before = p.getGameMode();
        new SetGamemodeAction("not_a_mode").execute(ctx(p));
        assertEquals(before, p.getGameMode());
    }

    @Test
    void giveItem() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        new GiveItemAction("STONE", 5).execute(ctx(p));
        assertTrue(p.getInventory().contains(Material.STONE, 5));
    }

    @Test
    void giveItemIgnoresInvalidMaterial() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        new GiveItemAction("NOT_A_MATERIAL", 5).execute(ctx(p));
        assertTrue(p.getInventory().isEmpty());
    }

    @Test
    void takeItemRemovesFromInventory() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        p.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.STONE, 10));
        new TakeItemAction("STONE", 4).execute(ctx(p));
        assertTrue(p.getInventory().contains(Material.STONE, 6));
    }

    @Test
    void randomNumberWithinRange() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        ActionContext context = ctx(p);
        new RandomTempNumberAction("roll", 1, 6).execute(context);
        int value = Integer.parseInt(context.getVariable("roll"));
        assertTrue(value >= 1 && value <= 6, "value=" + value);
    }

    @Test
    void closeInventoryDoesNotThrow() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        new CloseInventoryAction().execute(ctx(p));
    }

    @Test
    void delayExecuteIsNoOpAndExposesTicks() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        DelayAction delay = new DelayAction(40);
        delay.execute(ctx(p)); // no-op, must not throw
        assertEquals(40, delay.getTicks());
    }

    @Test
    void addAndRemovePotionEffect() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        new AddPotionEffectAction("SPEED", 10, 1).execute(ctx(p));
        assertTrue(p.hasPotionEffect(org.bukkit.potion.PotionEffectType.SPEED));
        new RemovePotionEffectAction("SPEED").execute(ctx(p));
        assertTrue(p.getActivePotionEffects().isEmpty());
    }

    @Test
    void clearEffectsRemovesAll() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        new AddPotionEffectAction("SPEED", 10, 1).execute(ctx(p));
        new ClearEffectsAction().execute(ctx(p));
        assertTrue(p.getActivePotionEffects().isEmpty());
    }

    @Test
    void teleportToExistingWorld() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        server.addSimpleWorld("dest");
        new TeleportAction("dest", 5, 70, 5).execute(ctx(p));
        assertEquals("dest", p.getLocation().getWorld().getName());
    }
}
