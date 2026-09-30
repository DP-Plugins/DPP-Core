package com.darksoldier1404.dppc.plugin.commands;

import com.darksoldier1404.dppc.DPPCore;
import com.darksoldier1404.dppc.utils.LocationUtil;
import com.darksoldier1404.dppc.utils.ParticleUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Developer command for visually testing every {@link LocationUtil} function.
 * <p>
 * Usage: {@code /dppcloc <function> [numeric args...] [PARTICLE]}
 * <p>
 * The function is evaluated relative to the player's current location (and the point 8 blocks
 * ahead of their eyes, used as the "to"/target for two-point functions). The resulting locations
 * are drawn with the chosen particle (default {@code FLAME}) every 2 ticks for ~3 seconds so the
 * shape stays visible. Numeric arguments map to each function's parameters in order; any
 * non-numeric argument is treated as the particle name.
 */
public class DPPCLocTestCommand implements CommandExecutor, TabCompleter {
    private final DPPCore plugin = DPPCore.getInstance();

    private static final long DRAW_TICKS = 60L;
    private static final long DRAW_PERIOD = 2L;

    private static final List<String> FUNCTIONS = Arrays.asList(
            "circle", "arc", "sphere", "filledcircle", "filledsphere", "cylinder", "cone",
            "line", "linespacing", "helix", "rectangle", "rectangleoutline", "box", "cube",
            "polygon", "star", "front", "behind", "left", "right", "above", "below", "relative",
            "midpoint", "lookat", "rotatearound", "lerp", "rotatevector", "direction",
            "distancesquared", "iswithinradius", "randomoncircle", "randomincircle",
            "randomonsphere", "randominsphere"
    );

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cYou must be op to use this command.");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by a player.");
            return true;
        }
        Player p = (Player) sender;
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String fn = args[0].toLowerCase();

        // Parse remaining args: numbers become ordered parameters, a non-number becomes the particle.
        List<Double> nums = new ArrayList<>();
        Particle particle = Particle.CRIT;
        for (int i = 1; i < args.length; i++) {
            try {
                nums.add(Double.parseDouble(args[i]));
            } catch (NumberFormatException e) {
                try {
                    particle = Particle.valueOf(args[i].toUpperCase());
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage("§eIgnoring unknown argument/particle: §f" + args[i]);
                }
            }
        }

        final Location center = p.getLocation();
        final Location look = LocationUtil.getLocationInFront(p.getEyeLocation(), 8.0);
        final Particle fp = particle;
        Supplier<List<Location>> supplier;

        switch (fn) {
            // ---- shape generators ----
            case "circle": {
                double r = d(nums, 0, 4);
                int pts = (int) d(nums, 1, 40);
                supplier = () -> LocationUtil.getCircle(center, r, pts);
                break;
            }
            case "arc": {
                double r = d(nums, 0, 4);
                int pts = (int) d(nums, 1, 20);
                double start = d(nums, 2, 0);
                double sweep = d(nums, 3, 180);
                supplier = () -> LocationUtil.getArc(center, r, pts, start, sweep);
                break;
            }
            case "sphere": {
                double r = d(nums, 0, 4);
                int pts = (int) d(nums, 1, 80);
                supplier = () -> LocationUtil.getSphere(center, r, pts);
                break;
            }
            case "filledcircle": {
                double r = d(nums, 0, 4);
                double sp = d(nums, 1, 0.5);
                supplier = () -> LocationUtil.getFilledCircle(center, r, sp);
                break;
            }
            case "filledsphere": {
                double r = d(nums, 0, 4);
                double sp = d(nums, 1, 0.6);
                supplier = () -> LocationUtil.getFilledSphere(center, r, sp);
                break;
            }
            case "cylinder": {
                double r = d(nums, 0, 3);
                double h = d(nums, 1, 5);
                int ppr = (int) d(nums, 2, 20);
                int rings = (int) d(nums, 3, 6);
                supplier = () -> LocationUtil.getCylinder(center, r, h, ppr, rings);
                break;
            }
            case "cone": {
                double r = d(nums, 0, 3);
                double h = d(nums, 1, 5);
                int ppr = (int) d(nums, 2, 16);
                int rings = (int) d(nums, 3, 6);
                supplier = () -> LocationUtil.getCone(center, r, h, ppr, rings);
                break;
            }
            case "line": {
                int pts = (int) d(nums, 0, 20);
                supplier = () -> LocationUtil.getLine(center, look, pts);
                break;
            }
            case "linespacing": {
                double sp = d(nums, 0, 0.5);
                supplier = () -> LocationUtil.getLine(center, look, sp);
                break;
            }
            case "helix": {
                double r = d(nums, 0, 2);
                double h = d(nums, 1, 6);
                int pts = (int) d(nums, 2, 80);
                double turns = d(nums, 3, 3);
                supplier = () -> LocationUtil.getHelix(center, r, h, pts, turns);
                break;
            }
            case "rectangle": {
                double w = d(nums, 0, 6);
                double l = d(nums, 1, 4);
                double sp = d(nums, 2, 0.5);
                supplier = () -> LocationUtil.getRectangle(center, w, l, sp);
                break;
            }
            case "rectangleoutline": {
                double w = d(nums, 0, 6);
                double l = d(nums, 1, 4);
                double sp = d(nums, 2, 0.5);
                supplier = () -> LocationUtil.getRectangleOutline(center, w, l, sp);
                break;
            }
            case "box": {
                double w = d(nums, 0, 4);
                double h = d(nums, 1, 3);
                double l = d(nums, 2, 4);
                double sp = d(nums, 3, 0.5);
                supplier = () -> LocationUtil.getBox(center, w, h, l, sp);
                break;
            }
            case "cube": {
                double size = d(nums, 0, 4);
                double sp = d(nums, 1, 0.5);
                supplier = () -> LocationUtil.getCube(center, size, sp);
                break;
            }
            case "polygon": {
                double r = d(nums, 0, 4);
                int sides = (int) d(nums, 1, 5);
                int pps = (int) d(nums, 2, 10);
                supplier = () -> LocationUtil.getPolygon(center, r, sides, pps);
                break;
            }
            case "star": {
                double outer = d(nums, 0, 4);
                double inner = d(nums, 1, 2);
                int spikes = (int) d(nums, 2, 5);
                int ppe = (int) d(nums, 3, 8);
                supplier = () -> LocationUtil.getStar(center, outer, inner, spikes, ppe);
                break;
            }
            // ---- directional helpers (drawn as a small marker ball) ----
            case "front": {
                Location l = LocationUtil.getLocationInFront(center, d(nums, 0, 5));
                supplier = () -> marker(l);
                break;
            }
            case "behind": {
                Location l = LocationUtil.getLocationBehind(center, d(nums, 0, 5));
                supplier = () -> marker(l);
                break;
            }
            case "left": {
                Location l = LocationUtil.getLocationLeft(center, d(nums, 0, 5));
                supplier = () -> marker(l);
                break;
            }
            case "right": {
                Location l = LocationUtil.getLocationRight(center, d(nums, 0, 5));
                supplier = () -> marker(l);
                break;
            }
            case "above": {
                Location l = LocationUtil.getLocationAbove(center, d(nums, 0, 3));
                supplier = () -> marker(l);
                break;
            }
            case "below": {
                Location l = LocationUtil.getLocationBelow(center, d(nums, 0, 3));
                supplier = () -> marker(l);
                break;
            }
            case "relative": {
                double fwd = d(nums, 0, 3);
                double up = d(nums, 1, 1);
                double right = d(nums, 2, 2);
                Location l = LocationUtil.getRelativeLocation(center, fwd, up, right);
                supplier = () -> marker(l);
                break;
            }
            case "midpoint": {
                Location l = LocationUtil.getMidpoint(center, look);
                supplier = () -> marker(l);
                break;
            }
            case "lerp": {
                double t = d(nums, 0, 0.5);
                Location l = LocationUtil.lerp(center, look, t);
                supplier = () -> marker(l);
                break;
            }
            case "rotatearound": {
                double ang = d(nums, 0, 45);
                Location l = LocationUtil.rotateAround(look, center, ang);
                supplier = () -> marker(l);
                break;
            }
            // ---- random points (recomputed each draw to show distribution) ----
            case "randomoncircle": {
                double r = d(nums, 0, 4);
                supplier = () -> randomCloud(() -> LocationUtil.randomPointOnCircle(center, r), 80);
                break;
            }
            case "randomincircle": {
                double r = d(nums, 0, 4);
                supplier = () -> randomCloud(() -> LocationUtil.randomPointInCircle(center, r), 80);
                break;
            }
            case "randomonsphere": {
                double r = d(nums, 0, 4);
                supplier = () -> randomCloud(() -> LocationUtil.randomPointOnSphere(center, r), 80);
                break;
            }
            case "randominsphere": {
                double r = d(nums, 0, 4);
                supplier = () -> randomCloud(() -> LocationUtil.randomPointInSphere(center, r), 80);
                break;
            }
            // ---- vector / scalar functions (reported as a message) ----
            case "lookat": {
                Location l = LocationUtil.lookAt(center, look);
                sender.sendMessage("§alookAt → yaw=§f" + String.format("%.1f", l.getYaw()) + " §apitch=§f" + String.format("%.1f", l.getPitch()));
                return true;
            }
            case "direction": {
                Vector v = LocationUtil.getDirection(center, look);
                sender.sendMessage("§agetDirection → §f" + fmt(v));
                return true;
            }
            case "rotatevector": {
                double ang = d(nums, 0, 90);
                Vector v = LocationUtil.rotateVector(center.getDirection(), new Vector(0, 1, 0), ang);
                sender.sendMessage("§arotateVector(facing, Y, " + ang + "°) → §f" + fmt(v));
                return true;
            }
            case "distancesquared": {
                double ds = LocationUtil.distanceSquared(center, look);
                sender.sendMessage("§adistanceSquared(you, look@8) → §f" + String.format("%.3f", ds) + " §7(≈ " + String.format("%.3f", Math.sqrt(ds)) + " blocks)");
                return true;
            }
            case "iswithinradius": {
                double r = d(nums, 0, 10);
                boolean within = LocationUtil.isWithinRadius(center, look, r);
                sender.sendMessage("§aisWithinRadius(you, look@8, " + r + ") → §f" + within);
                return true;
            }
            default:
                sender.sendMessage("§cUnknown function: §f" + fn);
                sender.sendMessage("§7Type §f/" + label + "§7 to see the list.");
                return true;
        }

        final Supplier<List<Location>> sup = supplier;
        int initial = sup.get().size();
        sender.sendMessage("§aDrawing §e" + fn + "§a (§f" + initial + "§a points) with §e" + fp + "§a for " + (DRAW_TICKS / 20.0) + "s.");
        new BukkitRunnable() {
            long elapsed = 0;

            @Override
            public void run() {
                if (elapsed >= DRAW_TICKS || !p.isOnline()) {
                    cancel();
                    return;
                }
                ParticleUtil.spawnAll(sup.get(), fp);
                elapsed += DRAW_PERIOD;
            }
        }.runTaskTimer(plugin, 0L, DRAW_PERIOD);
        return true;
    }

    /** Draw a small visible ball around a single location so directional results are easy to see. */
    private static List<Location> marker(@Nullable Location loc) {
        if (loc == null) return Collections.emptyList();
        return LocationUtil.getSphere(loc, 0.3, 12);
    }

    /** Build a cloud of {@code count} random points from the supplier (skips nulls). */
    private static List<Location> randomCloud(Supplier<Location> point, int count) {
        List<Location> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Location l = point.get();
            if (l != null) result.add(l);
        }
        return result;
    }

    private static double d(List<Double> nums, int idx, double def) {
        return idx < nums.size() ? nums.get(idx) : def;
    }

    private static String fmt(Vector v) {
        return String.format("(%.3f, %.3f, %.3f)", v.getX(), v.getY(), v.getZ());
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== DPP-Core LocationUtil Test ===");
        sender.sendMessage("§e/dppcloc <function> [args...] [PARTICLE]");
        sender.sendMessage("§7Center = your location, target = 8 blocks ahead. Default particle FLAME.");
        sender.sendMessage("§bShapes: §fcircle <r> <pts> §7| §farc <r> <pts> <startDeg> <sweepDeg> §7| §fsphere <r> <pts>");
        sender.sendMessage("§f  filledcircle <r> <sp> §7| §ffilledsphere <r> <sp> §7| §fcylinder <r> <h> <ppr> <rings>");
        sender.sendMessage("§f  cone <r> <h> <ppr> <rings> §7| §fline <pts> §7| §flinespacing <sp> §7| §fhelix <r> <h> <pts> <turns>");
        sender.sendMessage("§f  rectangle <w> <l> <sp> §7| §frectangleoutline <w> <l> <sp> §7| §fbox <w> <h> <l> <sp> §7| §fcube <size> <sp>");
        sender.sendMessage("§f  polygon <r> <sides> <pps> §7| §fstar <outerR> <innerR> <spikes> <ppe>");
        sender.sendMessage("§bDirection: §ffront/behind/left/right <dist>, above/below <dist>, relative <fwd> <up> <right>");
        sender.sendMessage("§f  midpoint, lerp <t>, rotatearound <deg>");
        sender.sendMessage("§bRandom: §frandomoncircle/randomincircle/randomonsphere/randominsphere <r>");
        sender.sendMessage("§bReports (message only): §flookat, direction, rotatevector <deg>, distancesquared, iswithinradius <r>");
        sender.sendMessage("§7Example: §f/dppcloc circle 5 64 HEART");
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> out = new ArrayList<>();
            for (String f : FUNCTIONS) {
                if (f.startsWith(prefix)) out.add(f);
            }
            return out;
        }
        return null;
    }
}
