package com.darksoldier1404.dppc.api.entity;

import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.darksoldier1404.dppc.support.MockServerTest;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class TargetAPITest extends MockServerTest {

    private WorldMock world;

    private PlayerMock at(String name, double x, double y, double z) {
        if (world == null) {
            world = server.addSimpleWorld("world");
        }
        PlayerMock p = server.addPlayer(name);
        p.teleport(new Location(world, x, y, z));
        return p;
    }

    @Test
    void distanceBetweenEntities() {
        PlayerMock a = at("A", 0, 64, 0);
        PlayerMock b = at("B", 3, 64, 0);
        assertEquals(3.0, TargetAPI.getDistanceBTAC(a, b), 1e-9);
    }

    @Test
    void distanceWithNullIsZero() {
        PlayerMock a = at("A", 0, 64, 0);
        assertEquals(0.0, TargetAPI.getDistanceBTAC(a, null));
        assertEquals(0.0, TargetAPI.getDistanceBTAC(null, a));
    }

    @Test
    void nearestTargetFromListPicksClosestWithinRange() {
        PlayerMock center = at("C", 0, 64, 0);
        PlayerMock near = at("Near", 3, 64, 0);
        PlayerMock far = at("Far", 50, 64, 0);
        assertSame(near, TargetAPI.getNearestTargetFromList(center, Arrays.asList(far, near), 10));
    }

    @Test
    void nearestTargetReturnsNullWhenNoneInRange() {
        PlayerMock center = at("C", 0, 64, 0);
        PlayerMock far = at("Far", 50, 64, 0);
        assertNull(TargetAPI.getNearestTargetFromList(center, Collections.singletonList(far), 10));
    }

    @Test
    void nearestTargetNullCenterOrEmptyListReturnsNull() {
        PlayerMock center = at("C", 0, 64, 0);
        assertNull(TargetAPI.getNearestTargetFromList(null, Collections.emptyList(), 10));
        assertNull(TargetAPI.getNearestTargetFromList(center, Collections.emptyList(), 10));
    }
}
