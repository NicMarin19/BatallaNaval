package com.batallanaval.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class EnemyAITest {

    @Test
    public void testAIEveryShotIsUnique() {
        Board playerBoard = new Board();
        EnemyAI ai = new EnemyAI(playerBoard);
        Set<Coordinate> shots = new HashSet<>();

        // AI should be able to pick 100 distinct coordinates without crashing or repeating
        for (int i = 0; i < 100; i++) {
            Coordinate target = ai.chooseTarget();
            assertNotNull(target);
            assertTrue(target.x >= 0 && target.x < Board.SIZE);
            assertTrue(target.y >= 0 && target.y < Board.SIZE);
            assertFalse(shots.contains(target), "AI targeted already shot coordinate: " + target);

            shots.add(target);
            ShotResult result = playerBoard.receiveShot(target.x, target.y);
            ai.processShotResult(result);
        }

        assertEquals(100, shots.size());
    }

    @Test
    public void testAIHuntsAdjacentWhenHit() {
        Board playerBoard = new Board();
        Ship destroyer = playerBoard.getShips().get(4); // Size 2
        playerBoard.placeShip(destroyer, 3, 3, true); // (3,3) and (4,3)

        EnemyAI ai = new EnemyAI(playerBoard);

        // Simulate AI scoring a hit at (3,3)
        ShotResult hit = new ShotResult(ShotResult.Outcome.HIT, 3, 3, destroyer);
        ai.processShotResult(hit);

        // Next target chosen by AI must be adjacent to (3,3)
        Coordinate next = ai.chooseTarget();
        assertNotNull(next);
        int dx = Math.abs(next.x - 3);
        int dy = Math.abs(next.y - 3);
        assertTrue((dx == 1 && dy == 0) || (dx == 0 && dy == 1),
                "Expected adjacent coordinate to (3,3), but got: " + next);
    }
}
