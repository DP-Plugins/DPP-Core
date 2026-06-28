package com.darksoldier1404.dppc.builder.command;

import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.darksoldier1404.dppc.support.PluginTest;
import org.bukkit.command.Command;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandBuilderTest extends PluginTest {

    private Command dppcCommand() {
        return plugin.getCommand("dppc");
    }

    @Test
    void addSubCommandRegistersIt() {
        CommandBuilder cb = new CommandBuilder(plugin);
        cb.addSubCommand("give", "/give", (s, a) -> true);
        assertTrue(cb.getSubCommandNames().contains("give"));
        assertTrue(cb.getSubCommands().containsKey("give"));
    }

    @Test
    void noArgsRunsDefaultHelp() {
        CommandBuilder cb = new CommandBuilder(plugin);
        cb.addSubCommand("give", "/give help", (s, a) -> true);
        PlayerMock player = server.addPlayer("Steve");
        cb.onCommand(player, dppcCommand(), "dppc", new String[0]);
        String msg = player.nextMessage();
        assertTrue(msg.contains("Available commands"), msg);
    }

    @Test
    void unknownSubcommandIsReported() {
        CommandBuilder cb = new CommandBuilder(plugin);
        PlayerMock player = server.addPlayer("Steve");
        cb.onCommand(player, dppcCommand(), "dppc", new String[]{"doesnotexist"});
        assertTrue(player.nextMessage().contains("Unknown subcommand"));
    }

    @Test
    void legacyActionIsInvoked() {
        boolean[] called = {false};
        CommandBuilder cb = new CommandBuilder(plugin);
        cb.addSubCommand("run", "/run", (s, a) -> {
            called[0] = true;
            return true;
        });
        PlayerMock player = server.addPlayer("Steve");
        cb.onCommand(player, dppcCommand(), "dppc", new String[]{"run"});
        assertTrue(called[0]);
    }

    @Test
    void typedArgumentsAreParsedForExecutor() {
        int[] captured = {-1};
        CommandBuilder cb = new CommandBuilder(plugin);
        cb.beginSubCommand("num", "/num <n>")
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.INTEGER)
                .executes((sender, args) -> {
                    captured[0] = args.getInteger(ArgumentIndex.ARG_0);
                    return true;
                });
        PlayerMock player = server.addPlayer("Steve");
        cb.onCommand(player, dppcCommand(), "dppc", new String[]{"num", "42"});
        assertEquals(42, captured[0]);
    }

    @Test
    void missingRequiredArgumentShowsUsage() {
        CommandBuilder cb = new CommandBuilder(plugin);
        cb.beginSubCommand("num", "/num <n>")
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.INTEGER)
                .executes((sender, args) -> true);
        PlayerMock player = server.addPlayer("Steve");
        cb.onCommand(player, dppcCommand(), "dppc", new String[]{"num"});
        assertTrue(player.nextMessage().contains("/num <n>"));
    }

    @Test
    void tabCompleteSuggestsSubcommandsFilteredByToken() {
        CommandBuilder cb = new CommandBuilder(plugin);
        cb.addSubCommand("give", "/give", (s, a) -> true);
        cb.addSubCommand("get", "/get", (s, a) -> true);
        cb.addSubCommand("set", "/set", (s, a) -> true);
        PlayerMock player = server.addPlayer("Steve");

        List<String> all = cb.onTabComplete(player, dppcCommand(), "dppc", new String[]{"g"});
        assertTrue(all.contains("give"));
        assertTrue(all.contains("get"));
        assertFalse(all.contains("set"));

        List<String> narrowed = cb.onTabComplete(player, dppcCommand(), "dppc", new String[]{"gi"});
        assertEquals(List.of("give"), narrowed);
    }
}
