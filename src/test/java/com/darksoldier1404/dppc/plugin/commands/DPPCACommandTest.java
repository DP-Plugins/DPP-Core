package com.darksoldier1404.dppc.plugin.commands;

import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.darksoldier1404.dppc.DPPCore;
import com.darksoldier1404.dppc.builder.action.ActionBuilder;
import com.darksoldier1404.dppc.support.PluginTest;
import org.bukkit.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code /dppca} is op-only, except for {@code testsudo} which every player may run.
 */
class DPPCACommandTest extends PluginTest {

    private final DPPCACommand executor = new DPPCACommand();
    private PlayerMock player;

    @BeforeEach
    void registerAction() {
        player = server.addPlayer("Steve");
        player.teleport(new Location(server.addSimpleWorld("world"), 0, 64, 0));
        player.setOp(false);
        DPPCore.actions.put("greet", new ActionBuilder(plugin, "greet").sendMessage("hello"));
    }

    private void run(String... args) {
        executor.onCommand(player, plugin.getCommand("dppca"), "dppca", args);
        server.getScheduler().performTicks(5L);
    }

    @Test
    void testsudoRunsTheActionForANonOpPlayer() {
        run("testsudo", "greet");
        player.assertSaid("hello");
    }

    @Test
    void testIsStillOpOnly() {
        run("test", "greet");
        assertEquals(plugin.getLang().get("g.cmd.permission.denied"), player.nextMessage());
        player.assertNoMoreSaid();
    }

    @Test
    void testStillWorksForOps() {
        player.setOp(true);
        run("test", "greet");
        player.assertSaid("hello");
    }

    @Test
    void testsudoReportsUnknownAction() {
        run("testsudo", "nope");
        assertEquals(plugin.getLang().get("ab.cmd.not_found"), player.nextMessage());
    }

    @Test
    void testsudoWithoutAnActionNameShowsItsOwnUsage() {
        run("testsudo");
        String usage = plugin.getLang().get("ab.cmd.usage.testsudo");
        assertFalse(usage.contains("Error: Language key not found"), "usage key must exist in the lang files");
        assertEquals(usage, player.nextMessage());
    }

    @Test
    void testsudoDoesNotUnlockOtherSubcommands() {
        run("reload");
        assertEquals(plugin.getLang().get("g.cmd.permission.denied"), player.nextMessage());
    }

    @Test
    void nonOpTabCompleteOnlyOffersTestsudo() {
        List<String> completions = executor.onTabComplete(player, plugin.getCommand("dppca"), "dppca", new String[]{""});
        assertEquals(List.of("testsudo"), completions);
    }

    @Test
    void opTabCompleteOffersEverything() {
        player.setOp(true);
        List<String> completions = executor.onTabComplete(player, plugin.getCommand("dppca"), "dppca", new String[]{""});
        assertNotNull(completions);
        assertTrue(completions.containsAll(List.of("create", "edit", "delete", "list", "view", "test", "testsudo", "reload")));
    }

    @Test
    void helpListsTestsudo() {
        player.setOp(true);
        run();
        String help = plugin.getLang().get("ab.cmd.help.testsudo");
        assertFalse(help.contains("Error: Language key not found"), "help key must exist in the lang files");
        boolean seen = false;
        String message;
        while ((message = player.nextMessage()) != null) {
            if (message.equals(help)) seen = true;
        }
        assertTrue(seen, "the help output must mention testsudo");
    }
}
