package com.batallanaval.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Board {
    public static final int SIZE = 10;
    private final CellState[][] grid;
    private final List<Ship> ships;

    public Board() {
        this.grid = new CellState[SIZE][SIZE];
        this.ships = new ArrayList<>();
        initShips();
        clear();
    }

    private void initShips() {
        ships.clear();
        ships.add(new Ship(ShipType.PORTAVIONES));
        ships.add(new Ship(ShipType.ACORAZADO));
        ships.add(new Ship(ShipType.CRUCERO));
        ships.add(new Ship(ShipType.SUBMARINO));
        ships.add(new Ship(ShipType.DESTRUCTOR));
    }

    public void clear() {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                grid[y][x] = CellState.EMPTY;
            }
        }
        for (Ship ship : ships) {
            ship.resetPlacement();
        }
    }

    public boolean canPlaceShip(Ship ship, int startX, int startY, boolean horizontal) {
        int length = ship.getType().getSize();
        if (startX < 0 || startY < 0) return false;

        if (horizontal) {
            if (startX + length > SIZE || startY >= SIZE) return false;
            for (int i = 0; i < length; i++) {
                int cx = startX + i;
                int cy = startY;
                if (grid[cy][cx] != CellState.EMPTY) {
                    // Check if it's the same ship currently being moved
                    Ship existing = getShipAt(cx, cy);
                    if (existing != null && existing != ship) {
                        return false;
                    }
                }
            }
        } else {
            if (startX >= SIZE || startY + length > SIZE) return false;
            for (int i = 0; i < length; i++) {
                int cx = startX;
                int cy = startY + i;
                if (grid[cy][cx] != CellState.EMPTY) {
                    Ship existing = getShipAt(cx, cy);
                    if (existing != null && existing != ship) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean placeShip(Ship ship, int startX, int startY, boolean horizontal) {
        if (!canPlaceShip(ship, startX, startY, horizontal)) {
            return false;
        }

        // If previously placed, clear old cells
        if (ship.isPlaced()) {
            for (Coordinate coord : ship.getOccupiedCoordinates()) {
                grid[coord.y][coord.x] = CellState.EMPTY;
            }
        }

        ship.setPosition(startX, startY, horizontal);
        for (Coordinate coord : ship.getOccupiedCoordinates()) {
            grid[coord.y][coord.x] = CellState.SHIP;
        }
        return true;
    }

    public void autoPlaceAllShips(Random rnd) {
        clear();
        for (Ship ship : ships) {
            boolean placed = false;
            int attempts = 0;
            while (!placed && attempts < 500) {
                attempts++;
                boolean horizontal = rnd.nextBoolean();
                int x = rnd.nextInt(SIZE);
                int y = rnd.nextInt(SIZE);
                if (canPlaceShip(ship, x, y, horizontal)) {
                    placeShip(ship, x, y, horizontal);
                    placed = true;
                }
            }
        }
    }

    public ShotResult receiveShot(int x, int y) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) {
            return new ShotResult(ShotResult.Outcome.OUT_OF_BOUNDS, x, y, null);
        }

        CellState current = grid[y][x];
        if (current == CellState.HIT || current == CellState.MISS || current == CellState.SUNK) {
            return new ShotResult(ShotResult.Outcome.ALREADY_SHOT, x, y, null);
        }

        if (current == CellState.EMPTY) {
            grid[y][x] = CellState.MISS;
            return new ShotResult(ShotResult.Outcome.MISS, x, y, null);
        }

        if (current == CellState.SHIP) {
            grid[y][x] = CellState.HIT;
            Ship hitShip = getShipAt(x, y);
            if (hitShip != null) {
                hitShip.registerHit();
                if (hitShip.isSunk()) {
                    // Mark all coordinates of this ship as SUNK
                    for (Coordinate c : hitShip.getOccupiedCoordinates()) {
                        grid[c.y][c.x] = CellState.SUNK;
                    }
                    return new ShotResult(ShotResult.Outcome.SUNK, x, y, hitShip);
                }
            }
            return new ShotResult(ShotResult.Outcome.HIT, x, y, hitShip);
        }

        return new ShotResult(ShotResult.Outcome.MISS, x, y, null);
    }

    public Ship getShipAt(int x, int y) {
        for (Ship s : ships) {
            if (s.occupies(x, y)) {
                return s;
            }
        }
        return null;
    }

    public boolean allShipsPlaced() {
        for (Ship s : ships) {
            if (!s.isPlaced()) return false;
        }
        return true;
    }

    public boolean areAllShipsSunk() {
        for (Ship s : ships) {
            if (!s.isSunk()) return false;
        }
        return true;
    }

    public int getSunkShipsCount() {
        int count = 0;
        for (Ship s : ships) {
            if (s.isSunk()) count++;
        }
        return count;
    }

    public CellState getCell(int x, int y) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) return CellState.EMPTY;
        return grid[y][x];
    }

    public void setCell(int x, int y, CellState state) {
        if (x >= 0 && x < SIZE && y >= 0 && y < SIZE) {
            grid[y][x] = (state != null) ? state : CellState.EMPTY;
        }
    }

    public List<Ship> getShips() {
        return ships;
    }

    /**
     * Celdas de barcos que siguen intactas y, por tanto, se pueden seguir
     * disparando. Se usa para elegir las pistas de inteligencia.
     */
    public List<Coordinate> getShootableShipCells() {
        List<Coordinate> result = new ArrayList<>();
        for (Ship ship : ships) {
            if (ship.isSunk()) {
                continue;
            }
            for (Coordinate coord : ship.getOccupiedCoordinates()) {
                if (grid[coord.y][coord.x] == CellState.SHIP) {
                    result.add(coord);
                }
            }
        }
        return result;
    }
}
