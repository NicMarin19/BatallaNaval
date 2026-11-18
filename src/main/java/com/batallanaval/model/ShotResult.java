package com.batallanaval.model;

public class ShotResult {
    public enum Outcome {
        MISS,
        HIT,
        SUNK,
        ALREADY_SHOT,
        OUT_OF_BOUNDS
    }

    private final Outcome outcome;
    private final int x;
    private final int y;
    private final Ship hitShip;

    public ShotResult(Outcome outcome, int x, int y, Ship hitShip) {
        this.outcome = outcome;
        this.x = x;
        this.y = y;
        this.hitShip = hitShip;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Ship getHitShip() {
        return hitShip;
    }

    public Coordinate getCoordinate() {
        return new Coordinate(x, y);
    }
}
