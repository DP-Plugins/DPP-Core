package com.darksoldier1404.dppc.utils;

import be.seeseemelk.mockbukkit.WorldMock;
import com.darksoldier1404.dppc.support.MockServerTest;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocationUtilTest extends MockServerTest {

    private static final double DELTA = 1.0E-6;
    private WorldMock world;
    private Location center;

    @BeforeEach
    void setUpWorld() {
        world = server.addSimpleWorld("world");
        center = new Location(world, 0, 64, 0);
    }

    @Test
    void getCircleReturnsEvenlySpacedPointsOnRadius() {
        List<Location> circle = LocationUtil.getCircle(center, 4.0, 8);
        assertEquals(8, circle.size());
        for (Location loc : circle) {
            assertEquals(world, loc.getWorld());
            assertEquals(64.0, loc.getY(), DELTA);
            // same Y as center, so straight-line distance is the horizontal radius
            assertEquals(4.0, loc.distance(center), 1.0E-9);
        }
    }

    @Test
    void getArcKeepsRadiusAndPointCount() {
        List<Location> arc = LocationUtil.getArc(center, 4.0, 5, 0.0, 90.0);
        assertEquals(5, arc.size());
        for (Location loc : arc) {
            assertEquals(4.0, loc.distance(center), 1.0E-9);
        }
    }

    @Test
    void getSphereDistributesPointsOnSurface() {
        List<Location> sphere = LocationUtil.getSphere(center, 5.0, 50);
        assertEquals(50, sphere.size());
        for (Location loc : sphere) {
            assertEquals(5.0, loc.distance(center), 1.0E-6);
        }
    }

    @Test
    void getLineByCountIncludesBothEndpoints() {
        Location from = new Location(world, 0, 64, 0);
        Location to = new Location(world, 0, 64, 10);
        List<Location> line = LocationUtil.getLine(from, to, 5);
        assertEquals(5, line.size());
        assertEquals(0.0, line.get(0).distance(from), DELTA);
        assertEquals(0.0, line.get(4).distance(to), DELTA);
    }

    @Test
    void getLineBySpacingFillsSegment() {
        Location from = new Location(world, 0, 64, 0);
        Location to = new Location(world, 0, 64, 10);
        List<Location> line = LocationUtil.getLine(from, to, 2.0);
        // 0,2,4,6,8,10 -> 6 points
        assertEquals(6, line.size());
        assertEquals(0.0, line.get(0).distance(from), DELTA);
        assertEquals(10.0, line.get(5).distance(from), 1.0E-9);
    }

    @Test
    void getHelixClimbsToHeight() {
        List<Location> helix = LocationUtil.getHelix(center, 3.0, 10.0, 20, 2.0);
        assertEquals(20, helix.size());
        assertEquals(64.0, helix.get(0).getY(), DELTA);
        assertEquals(74.0, helix.get(19).getY(), DELTA);
        for (Location loc : helix) {
            double dx = loc.getX() - center.getX();
            double dz = loc.getZ() - center.getZ();
            assertEquals(3.0, Math.sqrt(dx * dx + dz * dz), 1.0E-9);
        }
    }

    @Test
    void getLocationInFrontMovesAlongFacing() {
        Location origin = new Location(world, 0, 64, 0, 0.0f, 0.0f); // yaw 0 -> +Z
        Location front = LocationUtil.getLocationInFront(origin, 5.0);
        assertEquals(5.0, front.distance(origin), 1.0E-9);
        assertEquals(5.0, front.getZ(), 1.0E-9);
    }

    @Test
    void getRelativeForwardEqualsInFront() {
        Location origin = new Location(world, 0, 64, 0, 30.0f, 10.0f);
        Location front = LocationUtil.getLocationInFront(origin, 5.0);
        Location relative = LocationUtil.getRelativeLocation(origin, 5.0, 0.0, 0.0);
        assertEquals(0.0, front.distance(relative), 1.0E-9);
    }

    @Test
    void getMidpointIsHalfway() {
        Location a = new Location(world, 0, 64, 0);
        Location b = new Location(world, 0, 64, 10);
        Location mid = LocationUtil.getMidpoint(a, b);
        assertEquals(0.0, mid.getX(), DELTA);
        assertEquals(64.0, mid.getY(), DELTA);
        assertEquals(5.0, mid.getZ(), DELTA);
    }

    @Test
    void nullAndDegenerateInputsAreSafe() {
        assertTrue(LocationUtil.getCircle(null, 4.0, 8).isEmpty());
        assertTrue(LocationUtil.getCircle(center, 4.0, 0).isEmpty());
        assertEquals(1, LocationUtil.getArc(center, 4.0, 1, 0.0, 90.0).size());
    }

    // ---- new shape generators ----

    @Test
    void getFilledCircleStaysWithinRadiusOnPlane() {
        List<Location> filled = LocationUtil.getFilledCircle(center, 4.0, 1.0);
        assertTrue(filled.size() > 1);
        for (Location loc : filled) {
            assertEquals(64.0, loc.getY(), DELTA);
            assertTrue(loc.distance(center) <= 4.0 + DELTA);
        }
    }

    @Test
    void getFilledSphereStaysWithinRadius() {
        List<Location> filled = LocationUtil.getFilledSphere(center, 4.0, 1.0);
        assertTrue(filled.size() > 1);
        for (Location loc : filled) {
            assertTrue(loc.distance(center) <= 4.0 + DELTA);
        }
    }

    @Test
    void getCylinderStacksRingsAtRadius() {
        List<Location> cyl = LocationUtil.getCylinder(center, 3.0, 6.0, 10, 4);
        assertEquals(40, cyl.size());
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Location loc : cyl) {
            double dx = loc.getX() - center.getX();
            double dz = loc.getZ() - center.getZ();
            assertEquals(3.0, Math.sqrt(dx * dx + dz * dz), 1.0E-9);
            minY = Math.min(minY, loc.getY());
            maxY = Math.max(maxY, loc.getY());
        }
        assertEquals(64.0, minY, DELTA);
        assertEquals(70.0, maxY, DELTA);
    }

    @Test
    void getConeShrinksFromBaseToApex() {
        List<Location> cone = LocationUtil.getCone(center, 3.0, 6.0, 8, 4);
        assertEquals(32, cone.size());
        Location base = cone.get(0);
        Location apex = cone.get(cone.size() - 1);
        double baseR = Math.hypot(base.getX() - center.getX(), base.getZ() - center.getZ());
        double apexR = Math.hypot(apex.getX() - center.getX(), apex.getZ() - center.getZ());
        assertEquals(3.0, baseR, 1.0E-9);
        assertEquals(0.0, apexR, 1.0E-9);
        assertEquals(64.0, base.getY(), DELTA);
        assertEquals(70.0, apex.getY(), DELTA);
    }

    @Test
    void getRectangleAndOutlineStayWithinBounds() {
        List<Location> filled = LocationUtil.getRectangle(center, 6.0, 4.0, 1.0);
        assertTrue(filled.size() > 1);
        for (Location loc : filled) {
            assertEquals(64.0, loc.getY(), DELTA);
            assertTrue(Math.abs(loc.getX() - center.getX()) <= 3.0 + DELTA);
            assertTrue(Math.abs(loc.getZ() - center.getZ()) <= 2.0 + DELTA);
        }
        List<Location> outline = LocationUtil.getRectangleOutline(center, 6.0, 4.0, 1.0);
        assertTrue(outline.size() > 1);
        for (Location loc : outline) {
            boolean onEdgeX = Math.abs(Math.abs(loc.getX() - center.getX()) - 3.0) < DELTA;
            boolean onEdgeZ = Math.abs(Math.abs(loc.getZ() - center.getZ()) - 2.0) < DELTA;
            assertTrue(onEdgeX || onEdgeZ);
        }
    }

    @Test
    void getBoxAndCubeStayWithinBounds() {
        List<Location> box = LocationUtil.getBox(center, 4.0, 6.0, 2.0, 1.0);
        assertTrue(box.size() > 1);
        for (Location loc : box) {
            assertTrue(Math.abs(loc.getX() - center.getX()) <= 2.0 + DELTA);
            assertTrue(Math.abs(loc.getY() - center.getY()) <= 3.0 + DELTA);
            assertTrue(Math.abs(loc.getZ() - center.getZ()) <= 1.0 + DELTA);
        }
        List<Location> cube = LocationUtil.getCube(center, 4.0, 1.0);
        for (Location loc : cube) {
            assertTrue(Math.abs(loc.getX() - center.getX()) <= 2.0 + DELTA);
            assertTrue(Math.abs(loc.getY() - center.getY()) <= 2.0 + DELTA);
            assertTrue(Math.abs(loc.getZ() - center.getZ()) <= 2.0 + DELTA);
        }
    }

    @Test
    void getPolygonHasExpectedPointCount() {
        List<Location> poly = LocationUtil.getPolygon(center, 5.0, 6, 4);
        assertEquals(24, poly.size());
        for (Location loc : poly) {
            assertTrue(loc.distance(center) <= 5.0 + DELTA);
        }
    }

    @Test
    void getStarAlternatesRadii() {
        List<Location> star = LocationUtil.getStar(center, 5.0, 2.0, 5, 3);
        assertEquals(30, star.size());
        double maxR = 0, minR = Double.MAX_VALUE;
        for (Location loc : star) {
            double r = Math.hypot(loc.getX() - center.getX(), loc.getZ() - center.getZ());
            maxR = Math.max(maxR, r);
            minR = Math.min(minR, r);
        }
        assertEquals(5.0, maxR, 1.0E-9);
        assertTrue(minR <= 2.0 + DELTA);
    }

    // ---- directional helpers ----

    @Test
    void directionalHelpersMoveCorrectAxis() {
        Location origin = new Location(world, 0, 64, 0, 0.0f, 0.0f); // yaw 0 -> +Z, right -> -X
        assertEquals(5.0, LocationUtil.getLocationInFront(origin, 5.0).getZ(), 1.0E-9);
        assertEquals(-5.0, LocationUtil.getLocationBehind(origin, 5.0).getZ(), 1.0E-9);
        assertEquals(-5.0, LocationUtil.getLocationRight(origin, 5.0).getX(), 1.0E-9);
        assertEquals(5.0, LocationUtil.getLocationLeft(origin, 5.0).getX(), 1.0E-9);
        assertEquals(69.0, LocationUtil.getLocationAbove(origin, 5.0).getY(), 1.0E-9);
        assertEquals(59.0, LocationUtil.getLocationBelow(origin, 5.0).getY(), 1.0E-9);
    }

    // ---- transforms ----

    @Test
    void lookAtFacesTarget() {
        Location from = new Location(world, 0, 64, 0);
        Location target = new Location(world, 0, 64, 10);
        Location looking = LocationUtil.lookAt(from, target);
        assertEquals(0.0, looking.getDirection().getX(), 1.0E-9);
        assertEquals(1.0, looking.getDirection().getZ(), 1.0E-6);
    }

    @Test
    void rotateAroundKeepsDistanceAndReturnsAfterFullTurn() {
        Location point = new Location(world, 4, 64, 0);
        Location rotated = LocationUtil.rotateAround(point, center, 90.0);
        assertEquals(4.0, rotated.distance(center), 1.0E-9);
        Location full = LocationUtil.rotateAround(point, center, 360.0);
        assertEquals(0.0, full.distance(point), 1.0E-9);
    }

    @Test
    void rotateVectorPreservesLength() {
        Vector v = new Vector(1, 0, 0);
        Vector rotated = LocationUtil.rotateVector(v, new Vector(0, 1, 0), 90.0);
        assertEquals(1.0, rotated.length(), 1.0E-9);
        assertEquals(0.0, rotated.getX(), 1.0E-9);
    }

    // ---- interpolation / offset ----

    @Test
    void lerpInterpolatesEndpoints() {
        Location a = new Location(world, 0, 64, 0);
        Location b = new Location(world, 0, 64, 10);
        assertEquals(0.0, LocationUtil.lerp(a, b, 0.0).distance(a), DELTA);
        assertEquals(0.0, LocationUtil.lerp(a, b, 1.0).distance(b), DELTA);
        assertEquals(5.0, LocationUtil.lerp(a, b, 0.5).getZ(), DELTA);
    }

    @Test
    void offsetAddsWorldAxes() {
        Location off = LocationUtil.offset(center, 1.0, 2.0, 3.0);
        assertEquals(1.0, off.getX(), DELTA);
        assertEquals(66.0, off.getY(), DELTA);
        assertEquals(3.0, off.getZ(), DELTA);
    }

    // ---- math / query ----

    @Test
    void getDirectionIsNormalized() {
        Location from = new Location(world, 0, 64, 0);
        Location to = new Location(world, 0, 64, 10);
        Vector dir = LocationUtil.getDirection(from, to);
        assertEquals(1.0, dir.length(), 1.0E-9);
        assertEquals(1.0, dir.getZ(), 1.0E-9);
    }

    @Test
    void distanceSquaredMatchesDistance() {
        Location a = new Location(world, 0, 64, 0);
        Location b = new Location(world, 3, 64, 4);
        assertEquals(25.0, LocationUtil.distanceSquared(a, b), DELTA);
    }

    @Test
    void isWithinRadiusChecksDistanceAndWorld() {
        Location target = new Location(world, 3, 64, 0);
        assertTrue(LocationUtil.isWithinRadius(center, target, 4.0));
        assertTrue(!LocationUtil.isWithinRadius(center, target, 2.0));
        WorldMock other = server.addSimpleWorld("other");
        assertTrue(!LocationUtil.isWithinRadius(center, new Location(other, 0, 64, 0), 10.0));
    }

    // ---- random ----

    @Test
    void randomCircleAndSphereRespectRadius() {
        for (int i = 0; i < 50; i++) {
            assertEquals(4.0, LocationUtil.randomPointOnCircle(center, 4.0).distance(center), 1.0E-6);
            assertTrue(LocationUtil.randomPointInCircle(center, 4.0).distance(center) <= 4.0 + DELTA);
            assertEquals(4.0, LocationUtil.randomPointOnSphere(center, 4.0).distance(center), 1.0E-6);
            assertTrue(LocationUtil.randomPointInSphere(center, 4.0).distance(center) <= 4.0 + DELTA);
        }
    }
}
