package com.darksoldier1404.dppc.builder.command;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import com.darksoldier1404.dppc.api.logger.DLogManager;
import com.darksoldier1404.dppc.data.DPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

@DPPCoreVersion(since = "5.3.0")
public class CommandBuilder implements CommandExecutor, TabCompleter {
    private final Map<String, SubCommand> subCommands = new HashMap<>();
    private final DPlugin plugin;
    private final List<String> subCommandNames = new ArrayList<>();
    private BiConsumer<CommandSender, String[]> defaultAction;
    private String noSubCommandsMessage;

    public List<String> getSubCommandNames() {
        return subCommandNames;
    }

    public CommandBuilder(DPlugin plugin) {
        this.plugin = plugin;
        this.noSubCommandsMessage = plugin.getPrefix() + "No available commands.";
        this.defaultAction = (sender, args) -> {
            StringBuilder helpMessage = new StringBuilder();
            boolean hasCommands = false;
            for (String cmd : subCommandNames) {
                SubCommand sub = subCommands.get(cmd);
                if (sub.permission == null || sender.hasPermission(sub.permission)) {
                    if (!hasCommands) {
                        helpMessage.append(plugin.getPrefix()).append("Available commands:\n");
                        hasCommands = true;
                    }
                    helpMessage.append(plugin.getPrefix()).append(sub.usage).append("\n");
                }
            }
            if (hasCommands) {
                sender.sendMessage(helpMessage.toString().trim());
            } else {
                sender.sendMessage(noSubCommandsMessage);
            }
        };
    }

    public void build(@NotNull String command) {
        plugin.getCommand(command).setExecutor(this);
    }

    public void addSubCommand(String name, String usage, BiFunction<CommandSender, String[], Boolean> action) {
        addSubCommand(name, null, usage, false, action);
    }

    public void addSubCommand(String name, String usage, boolean isPlayerOnly, BiFunction<CommandSender, String[], Boolean> action) {
        addSubCommand(name, null, usage, isPlayerOnly, action);
    }

    public void addSubCommand(String name, String permission, String usage, BiFunction<CommandSender, String[], Boolean> action) {
        addSubCommand(name, permission, usage, false, action);
    }

    public void addSubCommand(String name, String permission, String usage, boolean isPlayerOnly, BiFunction<CommandSender, String[], Boolean> action) {
        subCommands.put(name.toLowerCase(), new SubCommand(name, permission, usage, isPlayerOnly, action));
        subCommandNames.add(name.toLowerCase());
    }

    public void addTabCompletion(String subCommand, Function<String[], List<String>> completion) {
        SubCommand cmd = this.subCommands.get(subCommand.toLowerCase());
        if (cmd != null) {
            cmd.setTabCompletion(completion);
        }
    }

    // Overloaded method to support CommandSender in tab completion
    public void addTabCompletion(String subCommand, BiFunction<CommandSender, String[], List<String>> completion) {
        SubCommand cmd = this.subCommands.get(subCommand.toLowerCase());
        if (cmd != null) {
            cmd.setTabCompletionWithSender(completion);
        }
    }

    public void setDefaultAction(BiConsumer<CommandSender, String[]> action) {
        this.defaultAction = action;
    }

    public void setNoSubCommandsMessage(String message) {
        this.noSubCommandsMessage = message;
    }

    public Map<String, SubCommand> getSubCommands() {
        return subCommands;
    }

    public DPlugin getPlugin() {
        return plugin;
    }

    public BiConsumer<CommandSender, String[]> getDefaultAction() {
        return defaultAction;
    }

    public String getNoSubCommandsMessage() {
        return noSubCommandsMessage;
    }

    private static class SubCommand {
        private final String name;
        private final String permission; // Nullable for no permission check
        private final String usage;
        private final boolean isPlayerOnly;
        private final BiFunction<CommandSender, String[], Boolean> action;
        private Function<String[], List<String>> tabCompletion;
        private BiFunction<CommandSender, String[], List<String>> tabCompletionWithSender;

        public SubCommand(String name, String permission, String usage, boolean isPlayerOnly, BiFunction<CommandSender, String[], Boolean> action) {
            this.name = name;
            this.permission = permission;
            this.usage = usage;
            this.isPlayerOnly = isPlayerOnly;
            this.action = action;
        }

        public void setTabCompletion(Function<String[], List<String>> tabCompletion) {
            this.tabCompletion = tabCompletion;
        }

        public void setTabCompletionWithSender(BiFunction<CommandSender, String[], List<String>> tabCompletionWithSender) {
            this.tabCompletionWithSender = tabCompletionWithSender;
        }

        public String getName() {
            return name;
        }

        public String getPermission() {
            return permission;
        }

        public String getUsage() {
            return usage;
        }

        public boolean isPlayerOnly() {
            return isPlayerOnly;
        }

        public BiFunction<CommandSender, String[], Boolean> getAction() {
            return action;
        }

        public Function<String[], List<String>> getTabCompletion() {
            return tabCompletion;
        }

        public BiFunction<CommandSender, String[], List<String>> getTabCompletionWithSender() {
            return tabCompletionWithSender;
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        plugin.getLog().info("Command executed: " + command.getName() + " by " + sender.getName() + " with args: " + String.join(", ", args), DLogManager.printCommandLogs);
        if (args.length == 0) {
            defaultAction.accept(sender, args);
            return false;
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand == null) {
            sender.sendMessage(plugin.getPrefix() + "Unknown subcommand.");
            return false;
        }

        if (subCommand.isPlayerOnly && !(sender instanceof Player)) {
            sender.sendMessage(plugin.getPrefix() + "This command can only be used by players.");
            return false;
        }

        if (subCommand.permission != null && !sender.hasPermission(subCommand.permission)) {
            sender.sendMessage(plugin.getPrefix() + "You do not have permission to use this command.");
            return false;
        }

        if (!subCommand.action.apply(sender, args)) {
            sender.sendMessage(plugin.getPrefix() + "Usage: " + subCommand.usage);
        }
        return false;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            for (String cmd : subCommandNames) {
                SubCommand sub = subCommands.get(cmd);
                if ((sub.permission == null || sender.hasPermission(sub.permission)) &&
                        (!sub.isPlayerOnly || sender instanceof Player)) {
                    completions.add(cmd);
                }
            }
            return completions;
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand != null &&
                (subCommand.permission == null || sender.hasPermission(subCommand.permission)) &&
                (!subCommand.isPlayerOnly || sender instanceof Player)) {
            if (subCommand.tabCompletionWithSender != null) {
                return subCommand.tabCompletionWithSender.apply(sender, args);
            } else if (subCommand.tabCompletion != null) {
                return subCommand.tabCompletion.apply(args);
            }
        }
        return null;
    }
}
