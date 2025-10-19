package com.darksoldier1404.dppc.api.luckperms;

import com.darksoldier1404.dppc.DPPCore;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class LuckpermAPI {
    private static final DPPCore plugin = DPPCore.getInstance();
    private static final LuckPerms lp = (LuckPerms) DPPCore.lp;
    private static final String PREFIX = "[DPP-Core.LuckpermAPI] ";
    private static final String MSG_USER_NOT_FOUND = PREFIX + "User not found.";
    private static final Logger logger = plugin.getLogger();

    @Nullable
    public static User getUser(OfflinePlayer p) {
        return getUserOrNotify(p);
    }

    @Nullable
    public static User getUserFromUUID(UUID uuid) {
        return lp.getUserManager().getUser(uuid);
    }

    @Nullable
    private static User getUserOrNotify(OfflinePlayer p) {
        try {
            User user = lp.getUserManager().getUser(p.getUniqueId());
            return user;
        } catch (Exception e) {
            logger.severe("Failed to get user for " + p.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private static <T extends Node> void addNode(OfflinePlayer p, T node) {
        User user = getUserOrNotify(p);
        if (user != null) {
            user.data().add(node);
            lp.getUserManager().saveUser(user);
            logger.info("Added node for player " + p.getName());
        }
    }

    private static <T extends Node> void removeNode(OfflinePlayer p, Class<T> nodeType, Predicate<T> predicate) {
        User user = getUserOrNotify(p);
        if (user != null) {
            boolean modified = user.getNodes().stream()
                    .filter(nodeType::isInstance)
                    .map(nodeType::cast)
                    .filter(predicate)
                    .findFirst()
                    .map(node -> {
                        user.data().remove(node);
                        return true;
                    })
                    .orElse(false);
            if (modified) {
                lp.getUserManager().saveUser(user);
                logger.info("Removed node for player " + p.getName());
            }
        }
    }

    private static <T extends Node> boolean hasNode(OfflinePlayer p, Class<T> nodeType, Predicate<T> predicate) {
        User user = getUserOrNotify(p);
        return user != null && user.getNodes().stream()
                .filter(nodeType::isInstance)
                .map(nodeType::cast)
                .anyMatch(predicate);
    }

    public static void setPrefix(OfflinePlayer p, String prefix, int priority) {
        if (prefix == null || prefix.isEmpty()) {
            return;
        }
        addNode(p, PrefixNode.builder(prefix, priority).build());
    }

    public static void setSuffix(OfflinePlayer p, String suffix, int priority) {
        if (suffix == null || suffix.isEmpty()) {
            return;
        }
        addNode(p, SuffixNode.builder(suffix, priority).build());
    }

    public static void delPrefix(OfflinePlayer p, String prefix) {
        removeNode(p, PrefixNode.class, node -> node.getKey().equals(prefix));
    }

    public static void delSuffix(OfflinePlayer p, String suffix) {
        removeNode(p, SuffixNode.class, node -> node.getKey().equals(suffix));
    }

    public static void delPrefix(OfflinePlayer p, int priority) {
        removeNode(p, PrefixNode.class, node -> node.getPriority() == priority);
    }

    public static void delSuffix(OfflinePlayer p, int priority) {
        removeNode(p, SuffixNode.class, node -> node.getPriority() == priority);
    }

    public static boolean isPrefixExists(OfflinePlayer p, String prefix) {
        return hasNode(p, PrefixNode.class, node -> node.getKey().equals(prefix));
    }

    public static boolean isSuffixExists(OfflinePlayer p, String suffix) {
        return hasNode(p, SuffixNode.class, node -> node.getKey().equals(suffix));
    }

    public static boolean isPrefixExists(OfflinePlayer p, int priority) {
        return hasNode(p, PrefixNode.class, node -> node.getPriority() == priority);
    }

    public static boolean isSuffixExists(OfflinePlayer p, int priority) {
        return hasNode(p, SuffixNode.class, node -> node.getPriority() == priority);
    }

    public static <T extends Node> void removeNodeByKey(OfflinePlayer p, Class<T> nodeType, String key) {
        removeNode(p, nodeType, node -> node.getKey().equals(key));
    }

    public static <T extends Node> void removeNodeByPriority(OfflinePlayer p, Class<T> nodeType, int priority, Function<T, Integer> priorityExtractor) {
        removeNode(p, nodeType, node -> priorityExtractor.apply(node) == priority);
    }

    public static <T extends Node> boolean hasNodeByKey(OfflinePlayer p, Class<T> nodeType, String key) {
        return hasNode(p, nodeType, node -> node.getKey().equals(key));
    }

    public static <T extends Node> boolean hasNodeByPriority(OfflinePlayer p, Class<T> nodeType, int priority, Function<T, Integer> priorityExtractor) {
        return hasNode(p, nodeType, node -> priorityExtractor.apply(node) == priority);
    }
}