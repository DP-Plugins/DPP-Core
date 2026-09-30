package com.darksoldier1404.dppc.utils;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Geometry helpers that build {@link Location} points without touching the world or entities.
 * <p>
 * Shape generators return a {@link List} of {@code Location}s (the world of the supplied
 * center/origin is preserved) that can be fed directly into {@code ParticleUtil} for drawing,
 * or reused for RPG skill positioning. All methods are null-safe and return an empty list
 * (or, for single-location helpers, {@code null}) on invalid input rather than throwing.
 * <p>
 * These methods are written for hot paths (potentially called dozens of times per tick):
 * lists are pre-sized, points are built with {@code new Location(...)} instead of
 * {@code clone().add(...)}, transcendental calls are hoisted, and circular shapes use
 * incremental rotation so a ring of N points costs two trig calls instead of {@code 2N}.
 */
@SuppressWarnings("all")
@DPPCoreVersion(since = "5.4.5")
public class LocationUtil {

    private static final double EPS = 1.0E-9;

    private LocationUtil() {
    }

    /**
     * Build an evenly spaced horizontal circle (XZ plane) around the center.
     *
     * @param center center of the circle (its Y is kept for every point)
     * @param radius circle radius in blocks
     * @param points number of points to generate
     * @return list of points on the circle, or an empty list on invalid input
     */
    @NotNull
    public static List<Location> getCircle(@Nullable Location center, double radius, int points) {
        if (center == null || points <= 0) return new ArrayList<>();
        List<Location> result = new ArrayList<>(points);
        World w = center.getWorld();
        double cx = center.getX(), cy = center.getY(), cz = center.getZ();
        // incremental rotation: rotate (radius, 0) by 2pi/points each step
        double step = 2 * Math.PI / points;
        double cos = Math.cos(step), sin = Math.sin(step);
        double px = radius, pz = 0;
        for (int i = 0; i < points; i++) {
            result.add(new Location(w, cx + px, cy, cz + pz));
            double nx = px * cos - pz * sin;
            pz = px * sin + pz * cos;
            px = nx;
        }
        return result;
    }

    /**
     * Build a horizontal arc (partial circle) around the center, useful for sweep effects.
     *
     * @param center   center of the arc (its Y is kept for every point)
     * @param radius   arc radius in blocks
     * @param points   number of points to generate
     * @param startDeg starting angle in degrees
     * @param sweepDeg total angle the arc spans in degrees
     * @return list of points along the arc, or an empty list on invalid input
     */
    @NotNull
    public static List<Location> getArc(@Nullable Location center, double radius, int points, double startDeg, double sweepDeg) {
        if (center == null || points <= 0) return new ArrayList<>();
        List<Location> result = new ArrayList<>(points);
        World w = center.getWorld();
        double cx = center.getX(), cy = center.getY(), cz = center.getZ();
        double startRad = Math.toRadians(startDeg);
        double px = radius * Math.cos(startRad), pz = radius * Math.sin(startRad);
        if (points == 1) {
            result.add(new Location(w, cx + px, cy, cz + pz));
            return result;
        }
        double step = Math.toRadians(sweepDeg) / (points - 1);
        double cos = Math.cos(step), sin = Math.sin(step);
        for (int i = 0; i < points; i++) {
            result.add(new Location(w, cx + px, cy, cz + pz));
            double nx = px * cos - pz * sin;
            pz = px * sin + pz * cos;
            px = nx;
        }
        return result;
    }

    /**
     * Distribute points roughly evenly over the surface of a sphere (Fibonacci spiral).
     *
     * @param center center of the sphere
     * @param radius sphere radius in blocks
     * @param points number of points to generate
     * @return list of points on the sphere surface, or an empty list on invalid input
     */
    @NotNull
    public static List<Location> getSphere(@Nullable Location center, double radius, int points) {
        if (center == null || points <= 0) return new ArrayList<>();
        List<Location> result = new ArrayList<>(points);
        World w = center.getWorld();
        double cx = center.getX(), cy = center.getY(), cz = center.getZ();
        double goldenAngle = Math.PI * (3 - Math.sqrt(5));
        double cosG = Math.cos(goldenAngle), sinG = Math.sin(goldenAngle);
        double ux = 1, uz = 0; // unit (cos theta, sin theta), theta starts at 0
        double inv = 2.0 / points;
        for (int i = 0; i < points; i++) {
            double y = 1 - (i + 0.5) * inv; // from ~1 to ~-1
            double ringRadius = Math.sqrt(1 - y * y) * radius;
            result.add(new Location(w, cx + ux * ringRadius, cy + y * radius, cz + uz * ringRadius));
            double nx = ux * cosG - uz * sinG;
            uz = ux * sinG + uz * cosG;
            ux = nx;
        }
        return result;
    }

    /**
     * Build a straight line between two points using a fixed number of points (endpoints included).
     *
     * @param from   start location
     * @param to     end location
     * @param points number of points to generate (1 returns only {@code from})
     * @return list of points along the segment, or an empty list on invalid input
     */
    @NotNull
    public static List<Location> getLine(@Nullable Location from, @Nullable Location to, int points) {
        if (from == null || to == null || points <= 0) return new ArrayList<>();
        List<Location> result = new ArrayList<>(points);
        World w = from.getWorld();
        double fx = from.getX(), fy = from.getY(), fz = from.getZ();
        double dx = to.getX() - fx, dy = to.getY() - fy, dz = to.getZ() - fz;
        if (points == 1) {
            result.add(new Location(w, fx, fy, fz));
            return result;
        }
        double invLast = 1.0 / (points - 1);
        for (int i = 0; i < points; i++) {
            double t = i * invLast;
            result.add(new Location(w, fx + dx * t, fy + dy * t, fz + dz * t));
        }
        return result;
    }

    /**
     * Build a straight line between two points spaced by a fixed distance (endpoints included).
     *
     * @param from    start location
     * @param to      end location
     * @param spacing distance between consecutive points in blocks
     * @return list of points along the segment, or an empty list on invalid input
     */
    @NotNull
    public static List<Location> getLine(@Nullable Location from, @Nullable Location to, double spacing) {
        if (from == null || to == null || spacing <= 0) return new ArrayList<>();
        World w = from.getWorld();
        double fx = from.getX(), fy = from.getY(), fz = from.getZ();
        double dx = to.getX() - fx, dy = to.getY() - fy, dz = to.getZ() - fz;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0) {
            List<Location> single = new ArrayList<>(1);
            single.add(new Location(w, fx, fy, fz));
            return single;
        }
        int steps = (int) Math.floor(len / spacing);
        List<Location> result = new ArrayList<>(steps + 1);
        double sx = dx / len * spacing, sy = dy / len * spacing, sz = dz / len * spacing;
        for (int i = 0; i <= steps; i++) {
            result.add(new Location(w, fx + sx * i, fy + sy * i, fz + sz * i));
        }
        return result;
    }

    /**
     * Build a vertical helix (spiral) rising from the center.
     *
     * @param center center of the base of the helix
     * @param radius horizontal radius in blocks
     * @param height total height the helix climbs in blocks
     * @param points number of points to generate
     * @param turns  number of full revolutions over the height
     * @return list of points along the helix, or an empty list on invalid input
     */
    @NotNull
    public static List<Location> getHelix(@Nullable Location center, double radius, double height, int points, double turns) {
        if (center == null || points <= 0) return new ArrayList<>();
        List<Location> result = new ArrayList<>(points);
        World w = center.getWorld();
        double cx = center.getX(), cy = center.getY(), cz = center.getZ();
        double px = radius, pz = 0; // angle starts at 0
        if (points == 1) {
            result.add(new Location(w, cx + px, cy, cz + pz));
            return result;
        }
        double step = 2 * Math.PI * turns / (points - 1);
        double cos = Math.cos(step), sin = Math.sin(step);
        for (int i = 0; i < points; i++) {
            double y = height * i / (points - 1);
            result.add(new Location(w, cx + px, cy + y, cz + pz));
            double nx = px * cos - pz * sin;
            pz = px * sin + pz * cos;
            px = nx;
        }
        return result;
    }

    /**
     * Build a horizontal disc (filled circle, XZ plane) as a grid of points.
     *
     * @param center  center of the disc (its Y is kept for every point)
     * @param radius  disc radius in blocks
     * @param spacing distance between grid points in blocks
     * @return list of points inside the disc, or an empty list on invalid input
     */
    @NotNull
    public static List<Location> getFilledCircle(@Nullable Location center, double radius, double spacing) {
        if (center == null || radius < 0 || spacing <= 0) return new ArrayList<>();
        int perAxis = (int) (2 * radius / spacing) + 2;
        List<Location> result = new ArrayList<>(perAxis * perAxis);
        World w = center.getWorld();
        double cx = center.getX(), cy = center.getY(), cz = center.getZ();
        double r2 = radius * radius + EPS;
        for (double x = -radius; x <= radius + EPS; x += spacing) {
            double x2 = x * x;
            for (double z = -radius; z <= radius + EPS; z += spacing) {
                if (x2 + z * z <= r2) {
                    result.add(new Location(w, cx + x, cy, cz + z));
                }
            }
        }
        return result;
    }

    /**
     * Build a solid ball (filled sphere) as a 3D grid of points.
     *
     * @param center  center of the sphere
     * @param radius  sphere radius in blocks
     * @param spacing distance between grid points in blocks
     * @return list of points inside the sphere, or an empty list on invalid input
     */
    @NotNull
    public static List<Location> getFilledSphere(@Nullable Location center, double radius, double spacing) {
        if (center == null || radius < 0 || spacing <= 0) return new ArrayList<>();
        int perAxis = (int) (2 * radius / spacing) + 2;
        List<Location> result = new ArrayList<>(perAxis * perAxis * perAxis);
        World w = center.getWorld();
        double cx = center.getX(), cy = center.getY(), cz = center.getZ();
        double r2 = radius * radius + EPS;
        for (double x = -radius; x <= radius + EPS; x += spacing) {
            double x2 = x * x;
            for (double y = -radius; y <= radius + EPS; y += spacing) {
                double xy2 = x2 + y * y;
                for (double z = -radius; z <= radius + EPS; z += spacing) {
                    if (xy2 + z * z <= r2) {
                        result.add(new Location(w, cx + x, cy + y, cz + z));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Build the side surface of a vertical cylinder as a set of stacked rings.
     *
     * @param center        center of the bottom face
     * @param radius        cylinder radius in blocks
     * @param height        cylinder height in blocks
     * @param pointsPerRing number of points on each ring
     * @param rings         number of stacked rings from bottom to top
     * @return list of points on the cylinder surface, or an empty list on invalid input
     */
    @NotNull
    public static List<Location> getCylinder(@Nullable Location center, double radius, double height, int pointsPerRing, int rings) {
        if (center == null || pointsPerRing <= 0 || rings <= 0) return new ArrayList<>();
        World w = center.getWorld();
        double cx = center.getX(), cy = center.getY(), cz = center.getZ();
        double[] ux = new double[pointsPerRing];
        double[] uz = new double[pointsPerRing];
        fillUnitCircle(ux, uz);
        List<Location> result = new ArrayList<>(pointsPerRing * rings);
        for (int i = 0; i < rings; i++) {
            double yOff = rings == 1 ? 0 : height * i / (rings - 1);
            double y = cy + yOff;
            for (int j = 0; j < pointsPerRing; j++) {
                result.add(new Location(w, cx + ux[j] * radius, y, cz + uz[j] * radius));
            }
        }
        return result;
    }

    /**
     * Build the surface of a vertical cone (base at the center, apex above) as stacked rings.
     *
     * @param center        center of the base
     * @param radius        base radius in blocks
     * @param height        cone height in blocks
     * @param pointsPerRing number of points on each ring
     * @param rings         number of stacked rings from base to apex
     * @return list of points on the cone surface, or an empty list on invalid input
     */
    @NotNull
    public static List<Location> getCone(@Nullable Location center, double radius, double height, int pointsPerRing, int rings) {
        if (center == null || pointsPerRing <= 0 || rings <= 0) return new ArrayList<>();
        World w = center.getWorld();
        double cx = center.getX(), cy = center.getY(), cz = center.getZ();
        double[] ux = new double[pointsPerRing];
        double[] uz = new double[pointsPerRing];
        fillUnitCircle(ux, uz);
        List<Location> result = new ArrayList<>(pointsPerRing * rings);
        for (int i = 0; i < rings; i++) {
            double t = rings == 1 ? 0 : (double) i / (rings - 1);
            double rr = radius * (1 - t);
            double y = cy + height * t;
            for (int j = 0; j < pointsPerRing; j++) {
                result.add(new Location(w, cx + ux[j] * rr, y, cz + uz[j] * rr));
            }
        }
        return result;
    }

    /**
     * Build a filled horizontal rectangle (XZ plane) as a grid of points.
     *
     * @param center  center of the rectangle (its Y is kept for every point)
     * @param width   size along the X axis in blocks
     * @param length  size along the Z axis in blocks
     * @param spacing distance between grid points in blocks
     * @return list of points inside the rectangle, or an empty list on invalid input
     */
    @NotNull
    public static List<Location> getRectangle(@Nullable Location center, double width, double length, double spacing) {
        if (center == null || width < 0 || length < 0 || spacing <= 0) return new ArrayList<>();
        double hw = width / 2, hl = length / 2;
        int nx = (int) (width / spacing) + 2, nz = (int) (length / spacing) + 2;
        List<Location> result = new ArrayList<>(nx * nz);
        World w = center.getWorld();
        double cx = center.getX(), cy = center.getY(), cz = center.getZ();
        for (double x = -hw; x <= hw + EPS; x += spacing) {
            for (double z = -hl; z <= hl + EPS; z += spacing) {
                result.add(new Location(w, cx + x, cy, cz + z));
            }
        }
        return result;
    }

    /**
     * Build the outline (perimeter) of a horizontal rectangle (XZ plane).
     *
     * @param center  center of the rectangle (its Y is kept for every point)
     * @param width   size along the X axis in blocks
     * @param length  size along the Z axis in blocks
     * @param spacing distance between points along the edges in blocks
     * @return list of points on the rectangle's edges, or an empty list on invalid input
     */
    @NotNull
    public static List<Location> getRectangleOutline(@Nullable Location center, double width, double length, double spacing) {
        if (center == null || width < 0 || length < 0 || spacing <= 0) return new ArrayList<>();
        List<Location> result = new ArrayList<>();
        World w = center.getWorld();
        double cx = center.getX(), cy = center.getY(), cz = center.getZ();
        double hw = width / 2, hl = length / 2;
        double x0 = cx - hw, x1 = cx + hw, z0 = cz - hl, z1 = cz + hl;
        appendLine(result, w, x0, cy, z0, x1, cy, z0, spacing, false);
        appendLine(result, w, x1, cy, z0, x1, cy, z1, spacing, false);
        appendLine(result, w, x1, cy, z1, x0, cy, z1, spacing, false);
        appendLine(result, w, x0, cy, z1, x0, cy, z0, spacing, false);
        return result;
    }

    /**
     * Build the wireframe (12 edges) of an axis-aligned cuboid.
     *
     * @param center  center of the box
     * @param width   size along the X axis in blocks
     * @param height  size along the Y axis in blocks
     * @param length  size along the Z axis in blocks
     * @param spacing distance between points along the edges in blocks
     * @return list of points on the box edges, or an empty list on invalid input
     */
    @NotNull
    public static List<Location> getBox(@Nullable Location center, double width, double height, double length, double spacing) {
        if (center == null || width < 0 || height < 0 || length < 0 || spacing <= 0) return new ArrayList<>();
        List<Location> result = new ArrayList<>();
        World w = center.getWorld();
        double cx = center.getX(), cy = center.getY(), cz = center.getZ();
        double hw = width / 2, hh = height / 2, hl = length / 2;
        double x0 = cx - hw, x1 = cx + hw, y0 = cy - hh, y1 = cy + hh, z0 = cz - hl, z1 = cz + hl;
        // bottom ring
        appendLine(result, w, x0, y0, z0, x1, y0, z0, spacing, false);
        appendLine(result, w, x1, y0, z0, x1, y0, z1, spacing, false);
        appendLine(result, w, x1, y0, z1, x0, y0, z1, spacing, false);
        appendLine(result, w, x0, y0, z1, x0, y0, z0, spacing, false);
        // top ring
        appendLine(result, w, x0, y1, z0, x1, y1, z0, spacing, false);
        appendLine(result, w, x1, y1, z0, x1, y1, z1, spacing, false);
        appendLine(result, w, x1, y1, z1, x0, y1, z1, spacing, false);
        appendLine(result, w, x0, y1, z1, x0, y1, z0, spacing, false);
        // vertical pillars
        appendLine(result, w, x0, y0, z0, x0, y1, z0, spacing, false);
        appendLine(result, w, x1, y0, z0, x1, y1, z0, spacing, false);
        appendLine(result, w, x1, y0, z1, x1, y1, z1, spacing, false);
        appendLine(result, w, x0, y0, z1, x0, y1, z1, spacing, false);
        return result;
    }

    /**
     * Build the wireframe of an axis-aligned cube (equal sides).
     *
     * @param center  center of the cube
     * @param size    edge length in blocks
     * @param spacing distance between points along the edges in blocks
     * @return list of points on the cube edges, or an empty list on invalid input
     */
    @NotNull
    public static List<Location> getCube(@Nullable Location center, double size, double spacing) {
        return getBox(center, size, size, size, spacing);
    }

    /**
     * Build a regular polygon (horizontal, XZ plane) by connecting its vertices.
     *
     * @param center        center of the polygon (its Y is kept for every point)
     * @param radius        circumradius (center to vertex) in blocks
     * @param sides         number of sides (>= 2)
     * @param pointsPerSide number of points along each side
     * @return list of points on the polygon edges, or an empty list on invalid input
     */
    @NotNull
    public static List<Location> getPolygon(@Nullable Location center, double radius, int sides, int pointsPerSide) {
        if (center == null || sides < 2 || pointsPerSide <= 0) return new ArrayList<>();
        World w = center.getWorld();
        double cx = center.getX(), cy = center.getY(), cz = center.getZ();
        // precompute vertex offsets via incremental rotation
        double[] ox = new double[sides];
        double[] oz = new double[sides];
        double step = 2 * Math.PI / sides;
        double cos = Math.cos(step), sin = Math.sin(step);
        double px = radius, pz = 0;
        for (int s = 0; s < sides; s++) {
            ox[s] = px;
            oz[s] = pz;
            double nx = px * cos - pz * sin;
            pz = px * sin + pz * cos;
            px = nx;
        }
        List<Location> result = new ArrayList<>(sides * pointsPerSide);
        double invSide = 1.0 / pointsPerSide;
        for (int s = 0; s < sides; s++) {
            int n = (s + 1) % sides;
            double ax = ox[s], az = oz[s];
            double dx = ox[n] - ax, dz = oz[n] - az;
            for (int i = 0; i < pointsPerSide; i++) {
                double t = i * invSide;
                result.add(new Location(w, cx + ax + dx * t, cy, cz + az + dz * t));
            }
        }
        return result;
    }

    /**
     * Build a star outline (horizontal, XZ plane) alternating between an outer and inner radius.
     *
     * @param center        center of the star (its Y is kept for every point)
     * @param outerRadius   distance from center to the outer spikes in blocks
     * @param innerRadius   distance from center to the inner vertices in blocks
     * @param spikes        number of spikes (>= 2)
     * @param pointsPerEdge number of points along each edge
     * @return list of points on the star edges, or an empty list on invalid input
     */
    @NotNull
    public static List<Location> getStar(@Nullable Location center, double outerRadius, double innerRadius, int spikes, int pointsPerEdge) {
        if (center == null || spikes < 2 || pointsPerEdge <= 0) return new ArrayList<>();
        World w = center.getWorld();
        double cx = center.getX(), cy = center.getY(), cz = center.getZ();
        int vertices = spikes * 2;
        double[] ox = new double[vertices];
        double[] oz = new double[vertices];
        // unit vector rotated by pi/spikes each step, scaled by alternating radius
        double step = Math.PI / spikes;
        double cos = Math.cos(step), sin = Math.sin(step);
        double ux = 1, uz = 0;
        for (int k = 0; k < vertices; k++) {
            double r = (k % 2 == 0) ? outerRadius : innerRadius;
            ox[k] = ux * r;
            oz[k] = uz * r;
            double nx = ux * cos - uz * sin;
            uz = ux * sin + uz * cos;
            ux = nx;
        }
        List<Location> result = new ArrayList<>(vertices * pointsPerEdge);
        double invEdge = 1.0 / pointsPerEdge;
        for (int k = 0; k < vertices; k++) {
            int n = (k + 1) % vertices;
            double ax = ox[k], az = oz[k];
            double dx = ox[n] - ax, dz = oz[n] - az;
            for (int i = 0; i < pointsPerEdge; i++) {
                double t = i * invEdge;
                result.add(new Location(w, cx + ax + dx * t, cy, cz + az + dz * t));
            }
        }
        return result;
    }

    /**
     * Fill the supplied arrays with one full circle of unit (cos, sin) offsets using incremental
     * rotation (two trig calls regardless of size). The two arrays must be the same length.
     */
    private static void fillUnitCircle(double[] cosOut, double[] sinOut) {
        int n = cosOut.length;
        double step = 2 * Math.PI / n;
        double cos = Math.cos(step), sin = Math.sin(step);
        double px = 1, pz = 0;
        for (int i = 0; i < n; i++) {
            cosOut[i] = px;
            sinOut[i] = pz;
            double nx = px * cos - pz * sin;
            pz = px * sin + pz * cos;
            px = nx;
        }
    }

    /**
     * Append the points of a straight segment (spaced by {@code spacing}) directly into {@code out}.
     * When {@code includeLast} is false the final generated point is dropped so abutting edges of a
     * polyline do not duplicate the shared corner.
     */
    private static void appendLine(List<Location> out, World w,
                                   double fx, double fy, double fz,
                                   double tx, double ty, double tz,
                                   double spacing, boolean includeLast) {
        double dx = tx - fx, dy = ty - fy, dz = tz - fz;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0) {
            out.add(new Location(w, fx, fy, fz));
            return;
        }
        int steps = (int) Math.floor(len / spacing);
        double sx = dx / len * spacing, sy = dy / len * spacing, sz = dz / len * spacing;
        int limit = (steps >= 1 && !includeLast) ? steps - 1 : steps;
        for (int i = 0; i <= limit; i++) {
            out.add(new Location(w, fx + sx * i, fy + sy * i, fz + sz * i));
        }
    }

    /**
     * Get the location directly in front of an origin along the way it is facing.
     *
     * @param origin   origin location (its yaw/pitch determine the direction)
     * @param distance how far in front, in blocks
     * @return the location in front, preserving the origin's facing, or {@code null} if origin is null
     */
    @Nullable
    public static Location getLocationInFront(@Nullable Location origin, double distance) {
        if (origin == null) return null;
        Vector d = origin.getDirection();
        Location result = origin.clone();
        result.add(d.getX() * distance, d.getY() * distance, d.getZ() * distance);
        return result;
    }

    /**
     * Get the location directly behind an origin (opposite of its facing).
     *
     * @param origin   origin location
     * @param distance how far behind, in blocks
     * @return the location behind, or {@code null} if origin is null
     */
    @Nullable
    public static Location getLocationBehind(@Nullable Location origin, double distance) {
        return getLocationInFront(origin, -distance);
    }

    /**
     * Get the location to the origin's left (relative to its facing).
     *
     * @param origin   origin location
     * @param distance how far left, in blocks
     * @return the location to the left, or {@code null} if origin is null
     */
    @Nullable
    public static Location getLocationLeft(@Nullable Location origin, double distance) {
        return getRelativeLocation(origin, 0, 0, -distance);
    }

    /**
     * Get the location to the origin's right (relative to its facing).
     *
     * @param origin   origin location
     * @param distance how far right, in blocks
     * @return the location to the right, or {@code null} if origin is null
     */
    @Nullable
    public static Location getLocationRight(@Nullable Location origin, double distance) {
        return getRelativeLocation(origin, 0, 0, distance);
    }

    /**
     * Get the location directly above an origin (world up).
     *
     * @param origin   origin location
     * @param distance how far up, in blocks
     * @return the location above, or {@code null} if origin is null
     */
    @Nullable
    public static Location getLocationAbove(@Nullable Location origin, double distance) {
        if (origin == null) return null;
        return origin.clone().add(0, distance, 0);
    }

    /**
     * Get the location directly below an origin (world down).
     *
     * @param origin   origin location
     * @param distance how far down, in blocks
     * @return the location below, or {@code null} if origin is null
     */
    @Nullable
    public static Location getLocationBelow(@Nullable Location origin, double distance) {
        if (origin == null) return null;
        return origin.clone().add(0, -distance, 0);
    }

    /**
     * Get a location relative to the origin's facing direction.
     * <p>
     * {@code forward} follows the look direction, {@code up} is world-up, and {@code right} is
     * perpendicular to both (positive = to the origin's right). Handy for offsetting projectiles
     * or effect spawns from a caster.
     *
     * @param origin  origin location (its yaw/pitch determine the basis)
     * @param forward distance along the look direction
     * @param up      distance along world up
     * @param right   distance to the origin's right
     * @return the offset location, preserving the origin's facing, or {@code null} if origin is null
     */
    @Nullable
    public static Location getRelativeLocation(@Nullable Location origin, double forward, double up, double right) {
        if (origin == null) return null;
        Vector f = origin.getDirection();
        double fx = f.getX(), fy = f.getY(), fz = f.getZ();
        // right = normalize(f x (0,1,0)) = normalize(-fz, 0, fx)
        double rx = -fz, rz = fx;
        double rlen = Math.sqrt(rx * rx + rz * rz);
        if (rlen > EPS) {
            rx /= rlen;
            rz /= rlen;
        } else {
            rx = 0;
            rz = 0;
        }
        Location result = origin.clone();
        result.add(fx * forward + rx * right, fy * forward + up, fz * forward + rz * right);
        return result;
    }

    /**
     * Get the midpoint between two locations.
     *
     * @param a first location
     * @param b second location
     * @return the midpoint (world taken from {@code a}), or {@code null} if either is null
     */
    @Nullable
    public static Location getMidpoint(@Nullable Location a, @Nullable Location b) {
        if (a == null || b == null) return null;
        return new Location(a.getWorld(),
                (a.getX() + b.getX()) / 2,
                (a.getY() + b.getY()) / 2,
                (a.getZ() + b.getZ()) / 2);
    }

    /**
     * Return a copy of {@code from} oriented to look at {@code target}.
     *
     * @param from   the location to reorient (its position is kept)
     * @param target the location to look at
     * @return a copy of {@code from} whose yaw/pitch face {@code target}, or {@code null} on invalid input
     */
    @Nullable
    public static Location lookAt(@Nullable Location from, @Nullable Location target) {
        if (from == null || target == null) return null;
        double dx = target.getX() - from.getX();
        double dy = target.getY() - from.getY();
        double dz = target.getZ() - from.getZ();
        Location result = from.clone();
        if (dx == 0 && dy == 0 && dz == 0) return result;
        result.setDirection(new Vector(dx, dy, dz));
        return result;
    }

    /**
     * Rotate a location around a center point on the horizontal (Y) axis.
     *
     * @param point    the location to rotate
     * @param center   the pivot
     * @param angleDeg rotation angle in degrees
     * @return the rotated location (world taken from {@code center}), or {@code null} on invalid input
     */
    @Nullable
    public static Location rotateAround(@Nullable Location point, @Nullable Location center, double angleDeg) {
        if (point == null || center == null) return null;
        double relx = point.getX() - center.getX();
        double rely = point.getY() - center.getY();
        double relz = point.getZ() - center.getZ();
        double angle = Math.toRadians(angleDeg);
        double cos = Math.cos(angle), sin = Math.sin(angle);
        // rotation about world Y axis
        double rx = relx * cos + relz * sin;
        double rz = relz * cos - relx * sin;
        return new Location(center.getWorld(), center.getX() + rx, center.getY() + rely, center.getZ() + rz);
    }

    /**
     * Rotate a vector around an arbitrary axis (Rodrigues' rotation formula).
     *
     * @param vector   the vector to rotate (not modified)
     * @param axis     the rotation axis (need not be normalized)
     * @param angleDeg rotation angle in degrees
     * @return a new rotated vector, or a zero vector on invalid input
     */
    @NotNull
    public static Vector rotateVector(@Nullable Vector vector, @Nullable Vector axis, double angleDeg) {
        if (vector == null || axis == null) return new Vector(0, 0, 0);
        double ax = axis.getX(), ay = axis.getY(), az = axis.getZ();
        double alen = Math.sqrt(ax * ax + ay * ay + az * az);
        if (alen == 0) return new Vector(0, 0, 0);
        double kx = ax / alen, ky = ay / alen, kz = az / alen;
        double vx = vector.getX(), vy = vector.getY(), vz = vector.getZ();
        double angle = Math.toRadians(angleDeg);
        double cos = Math.cos(angle), sin = Math.sin(angle);
        double dot = kx * vx + ky * vy + kz * vz;
        // k x v
        double cxv = ky * vz - kz * vy;
        double cyv = kz * vx - kx * vz;
        double czv = kx * vy - ky * vx;
        double oneMinusCos = 1 - cos;
        return new Vector(
                vx * cos + cxv * sin + kx * dot * oneMinusCos,
                vy * cos + cyv * sin + ky * dot * oneMinusCos,
                vz * cos + czv * sin + kz * dot * oneMinusCos);
    }

    /**
     * Linearly interpolate between two locations.
     *
     * @param from start location (returned when {@code t} is 0)
     * @param to   end location (returned when {@code t} is 1)
     * @param t    interpolation factor (typically 0..1)
     * @return the interpolated location (world taken from {@code from}), or {@code null} on invalid input
     */
    @Nullable
    public static Location lerp(@Nullable Location from, @Nullable Location to, double t) {
        if (from == null || to == null) return null;
        return new Location(from.getWorld(),
                from.getX() + (to.getX() - from.getX()) * t,
                from.getY() + (to.getY() - from.getY()) * t,
                from.getZ() + (to.getZ() - from.getZ()) * t);
    }

    /**
     * Apply a world-axis offset to a location.
     *
     * @param loc the base location
     * @param x   offset along X
     * @param y   offset along Y
     * @param z   offset along Z
     * @return the offset location, or {@code null} if {@code loc} is null
     */
    @Nullable
    public static Location offset(@Nullable Location loc, double x, double y, double z) {
        if (loc == null) return null;
        return loc.clone().add(x, y, z);
    }

    /**
     * Get the normalized direction vector pointing from one location to another.
     *
     * @param from start location
     * @param to   target location
     * @return the unit direction vector, or a zero vector on invalid input
     */
    @NotNull
    public static Vector getDirection(@Nullable Location from, @Nullable Location to) {
        if (from == null || to == null) return new Vector(0, 0, 0);
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0) return new Vector(0, 0, 0);
        return new Vector(dx / len, dy / len, dz / len);
    }

    /**
     * Compute the squared distance between two locations (cheaper than {@code distance}, world-agnostic).
     *
     * @param a first location
     * @param b second location
     * @return the squared distance, or {@code -1} on invalid input
     */
    public static double distanceSquared(@Nullable Location a, @Nullable Location b) {
        if (a == null || b == null) return -1;
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Check whether a target location is within a radius of a center (same world required).
     *
     * @param center the center location
     * @param target the location to test
     * @param radius the radius in blocks
     * @return {@code true} if {@code target} is in the same world and within the radius
     */
    public static boolean isWithinRadius(@Nullable Location center, @Nullable Location target, double radius) {
        if (center == null || target == null) return false;
        World cw = center.getWorld(), tw = target.getWorld();
        if (cw != null && tw != null && cw != tw) {
            return false;
        }
        return distanceSquared(center, target) <= radius * radius + EPS;
    }

    /**
     * Get a random point on the circumference of a horizontal circle.
     *
     * @param center center of the circle (its Y is kept)
     * @param radius circle radius in blocks
     * @return a random point on the circle, or {@code null} if center is null
     */
    @Nullable
    public static Location randomPointOnCircle(@Nullable Location center, double radius) {
        if (center == null) return null;
        double angle = ThreadLocalRandom.current().nextDouble(2 * Math.PI);
        return new Location(center.getWorld(),
                center.getX() + radius * Math.cos(angle),
                center.getY(),
                center.getZ() + radius * Math.sin(angle));
    }

    /**
     * Get a uniformly distributed random point inside a horizontal circle.
     *
     * @param center center of the circle (its Y is kept)
     * @param radius circle radius in blocks
     * @return a random point inside the circle, or {@code null} if center is null
     */
    @Nullable
    public static Location randomPointInCircle(@Nullable Location center, double radius) {
        if (center == null) return null;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double angle = rng.nextDouble(2 * Math.PI);
        double r = radius * Math.sqrt(rng.nextDouble());
        return new Location(center.getWorld(),
                center.getX() + r * Math.cos(angle),
                center.getY(),
                center.getZ() + r * Math.sin(angle));
    }

    /**
     * Get a random point on the surface of a sphere.
     *
     * @param center center of the sphere
     * @param radius sphere radius in blocks
     * @return a random point on the sphere surface, or {@code null} if center is null
     */
    @Nullable
    public static Location randomPointOnSphere(@Nullable Location center, double radius) {
        if (center == null) return null;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double y = rng.nextDouble(-1, 1);
        double theta = rng.nextDouble(2 * Math.PI);
        double ring = Math.sqrt(1 - y * y);
        return new Location(center.getWorld(),
                center.getX() + ring * Math.cos(theta) * radius,
                center.getY() + y * radius,
                center.getZ() + ring * Math.sin(theta) * radius);
    }

    /**
     * Get a uniformly distributed random point inside a sphere.
     *
     * @param center center of the sphere
     * @param radius sphere radius in blocks
     * @return a random point inside the sphere, or {@code null} if center is null
     */
    @Nullable
    public static Location randomPointInSphere(@Nullable Location center, double radius) {
        if (center == null) return null;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double y = rng.nextDouble(-1, 1);
        double theta = rng.nextDouble(2 * Math.PI);
        double ring = Math.sqrt(1 - y * y);
        double r = radius * Math.cbrt(rng.nextDouble());
        return new Location(center.getWorld(),
                center.getX() + ring * Math.cos(theta) * r,
                center.getY() + y * r,
                center.getZ() + ring * Math.sin(theta) * r);
    }
}
