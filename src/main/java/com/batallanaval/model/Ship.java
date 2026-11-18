package com.batallanaval.model;

import java.util.ArrayList;
import java.util.List;

public class Ship {
    private final ShipType type;
    private int startX;
    private int startY;
    private boolean horizontal;
    private int hits;
    private boolean placed;

    public Ship(ShipType type) {
        this.type = type;
        this.startX = -1;
        this.startY = -1;
        this.horizontal = true;
        this.hits = 0;
        this.placed = false;
    }

    public void setPosition(int x, int y, boolean horizontal) {
        this.startX = x;
        this.startY = y;
        this.horizontal = horizontal;
        this.placed = true;
    }

    public void resetPlacement() {
        this.startX = -1;
        this.startY = -1;
        this.placed = false;
        this.hits = 0;
    }

    public void registerHit() {
        if (hits < type.getSize()) {
            hits++;
        }
    }

    public void setHits(int hits) {
        this.hits = Math.max(0, Math.min(hits, type.getSize()));
    }

    public boolean isSunk() {
        return placed && hits >= type.getSize();
    }

    public boolean occupies(int x, int y) {
        if (!placed) return false;
        if (horizontal) {
            return y == startY && x >= startX && x < startX + type.getSize();
        } else {
            return x == startX && y >= startY && y < startY + type.getSize();
        }
    }

    public List<Coordinate> getOccupiedCoordinates() {
        List<Coordinate> coords = new ArrayList<>();
        if (!placed) return coords;
        for (int i = 0; i < type.getSize(); i++) {
            int cx = horizontal ? startX + i : startX;
            int cy = horizontal ? startY : startY + i;
            coords.add(new Coordinate(cx, cy));
        }
        return coords;
    }

    public ShipType getType() {
        return type;
    }

    public int getStartX() {
        return startX;
    }

    public int getStartY() {
        return startY;
    }

    public boolean isHorizontal() {
        return horizontal;
    }

    public int getHits() {
        return hits;
    }

    public int getRemainingHealth() {
        return Math.max(0, type.getSize() - hits);
    }

    public boolean isPlaced() {
        return placed;
    }
}
