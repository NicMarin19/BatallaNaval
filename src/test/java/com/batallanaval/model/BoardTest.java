package com.batallanaval.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class BoardTest {

    private Board board;

    @BeforeEach
    public void setUp() {
        board = new Board();
    }

    @Test
    public void testInitialState() {
        assertEquals(5, board.getShips().size());
        assertFalse(board.allShipsPlaced());
        assertFalse(board.areAllShipsSunk());

        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                assertEquals(CellState.EMPTY, board.getCell(x, y));
            }
        }
    }

    @Test
    public void testPlaceShipHorizontalValid() {
        Ship carrier = board.getShips().get(0); // Size 5
        boolean placed = board.placeShip(carrier, 0, 0, true);
        assertTrue(placed);
        assertTrue(carrier.isPlaced());

        for (int x = 0; x < 5; x++) {
            assertEquals(CellState.SHIP, board.getCell(x, 0));
            assertTrue(carrier.occupies(x, 0));
        }
        assertEquals(CellState.EMPTY, board.getCell(5, 0));
    }

    @Test
    public void testPlaceShipOutOfBounds() {
        Ship carrier = board.getShips().get(0); // Size 5
        // Cannot place horizontally at x=6 (needs 5 cells, 6+5=11 > 10)
        assertFalse(board.canPlaceShip(carrier, 6, 0, true));
        assertFalse(board.placeShip(carrier, 6, 0, true));

        // Cannot place vertically at y=7 (7+5=12 > 10)
        assertFalse(board.canPlaceShip(carrier, 0, 7, false));
    }

    @Test
    public void testShipCollision() {
        Ship carrier = board.getShips().get(0); // Size 5
        Ship battleship = board.getShips().get(1); // Size 4

        assertTrue(board.placeShip(carrier, 2, 2, true)); // Occupies (2,2) to (6,2)

        // Attempt placing battleship intersecting at (3,2) vertically from (3,0)
        assertFalse(board.canPlaceShip(battleship, 3, 0, false));
        assertFalse(board.placeShip(battleship, 3, 0, false));
    }

    @Test
    public void testAutoPlaceAllShips() {
        board.autoPlaceAllShips(new Random(12345));
        assertTrue(board.allShipsPlaced());

        int totalShipCells = 0;
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                if (board.getCell(x, y) == CellState.SHIP) {
                    totalShipCells++;
                }
            }
        }
        // Carrier (5) + Battleship (4) + Cruiser (3) + Sub (3) + Destroyer (2) = 17
        assertEquals(17, totalShipCells);
    }

    @Test
    public void testShootableShipCellsOnlyReturnsIntactShipCells() {
        board.autoPlaceAllShips(new Random(12345));
        assertEquals(17, board.getShootableShipCells().size());

        Ship destroyer = board.getShips().get(4); // Size 2
        Coordinate first = destroyer.getOccupiedCoordinates().get(0);
        Coordinate second = destroyer.getOccupiedCoordinates().get(1);

        assertEquals(ShotResult.Outcome.HIT, board.receiveShot(first.x, first.y).getOutcome());
        assertEquals(16, board.getShootableShipCells().size());

        assertEquals(ShotResult.Outcome.SUNK, board.receiveShot(second.x, second.y).getOutcome());
        assertEquals(15, board.getShootableShipCells().size());

        for (Coordinate coord : board.getShootableShipCells()) {
            assertEquals(CellState.SHIP, board.getCell(coord.x, coord.y));
        }
    }

    @Test
    public void testShootingMechanics() {
        Ship destroyer = board.getShips().get(4); // Size 2
        board.placeShip(destroyer, 4, 4, true); // (4,4) and (5,4)

        // Miss shot
        ShotResult miss = board.receiveShot(0, 0);
        assertEquals(ShotResult.Outcome.MISS, miss.getOutcome());
        assertEquals(CellState.MISS, board.getCell(0, 0));

        // Repeat shot
        ShotResult duplicate = board.receiveShot(0, 0);
        assertEquals(ShotResult.Outcome.ALREADY_SHOT, duplicate.getOutcome());

        // First hit
        ShotResult hit1 = board.receiveShot(4, 4);
        assertEquals(ShotResult.Outcome.HIT, hit1.getOutcome());
        assertEquals(CellState.HIT, board.getCell(4, 4));
        assertFalse(destroyer.isSunk());

        // Second hit -> Sunk
        ShotResult hit2 = board.receiveShot(5, 4);
        assertEquals(ShotResult.Outcome.SUNK, hit2.getOutcome());
        assertTrue(destroyer.isSunk());
        assertEquals(CellState.SUNK, board.getCell(4, 4));
        assertEquals(CellState.SUNK, board.getCell(5, 4));
    }
}
