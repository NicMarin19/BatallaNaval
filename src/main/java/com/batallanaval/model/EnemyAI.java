package com.batallanaval.model;

import java.util.*;

public class EnemyAI {
    private final Board targetBoard;
    private final Random random;
    private final boolean[][] alreadyAttacked;

    // AI targeting memory
    private final Stack<Coordinate> targetQueue;
    private final List<Coordinate> currentHits;

    public EnemyAI(Board targetBoard) {
        this.targetBoard = targetBoard;
        this.random = new Random();
        this.alreadyAttacked = new boolean[Board.SIZE][Board.SIZE];
        this.targetQueue = new Stack<>();
        this.currentHits = new ArrayList<>();
    }

    public void reset() {
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                alreadyAttacked[y][x] = false;
            }
        }
        targetQueue.clear();
        currentHits.clear();
    }

    public Coordinate chooseTarget() {
        // 1. If we have targeted coordinates in queue, pop one that has not been attacked yet
        while (!targetQueue.isEmpty()) {
            Coordinate candidate = targetQueue.pop();
            if (isValidUnattacked(candidate.x, candidate.y)) {
                return candidate;
            }
        }

        // 2. Parity hunting: try checkerboard cells first
        List<Coordinate> parityCandidates = new ArrayList<>();
        List<Coordinate> allCandidates = new ArrayList<>();

        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                if (!alreadyAttacked[y][x]) {
                    allCandidates.add(new Coordinate(x, y));
                    if ((x + y) % 2 == 0) {
                        parityCandidates.add(new Coordinate(x, y));
                    }
                }
            }
        }

        if (!parityCandidates.isEmpty()) {
            return parityCandidates.get(random.nextInt(parityCandidates.size()));
        } else if (!allCandidates.isEmpty()) {
            return allCandidates.get(random.nextInt(allCandidates.size()));
        }

        return new Coordinate(0, 0); // fallback
    }

    public void processShotResult(ShotResult result) {
        int x = result.getX();
        int y = result.getY();
        alreadyAttacked[y][x] = true;

        if (result.getOutcome() == ShotResult.Outcome.HIT) {
            currentHits.add(new Coordinate(x, y));
            generateTargetsAroundHits();
        } else if (result.getOutcome() == ShotResult.Outcome.SUNK) {
            currentHits.add(new Coordinate(x, y));
            // When a ship is sunk, remove its coordinates from currentHits
            if (result.getHitShip() != null) {
                List<Coordinate> sunkCoords = result.getHitShip().getOccupiedCoordinates();
                currentHits.removeAll(sunkCoords);
            } else {
                currentHits.clear();
            }

            targetQueue.clear();
            // If any leftover hits from another wounded ship remain, re-target around them
            if (!currentHits.isEmpty()) {
                generateTargetsAroundHits();
            }
        }
    }

    private void generateTargetsAroundHits() {
        if (currentHits.isEmpty()) return;

        if (currentHits.size() == 1) {
            // One hit: add all 4 cardinal directions
            Coordinate hit = currentHits.get(0);
            addCandidateIfValid(hit.x + 1, hit.y);
            addCandidateIfValid(hit.x - 1, hit.y);
            addCandidateIfValid(hit.x, hit.y + 1);
            addCandidateIfValid(hit.x, hit.y - 1);
        } else {
            // Multiple hits: determine line of alignment
            Coordinate first = currentHits.get(0);
            Coordinate second = currentHits.get(1);
            boolean isHorizontal = (first.y == second.y);

            if (isHorizontal) {
                // Find min and max X
                int minX = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;
                int y = first.y;
                for (Coordinate c : currentHits) {
                    if (c.y == y) {
                        if (c.x < minX) minX = c.x;
                        if (c.x > maxX) maxX = c.x;
                    }
                }
                addCandidateIfValid(maxX + 1, y);
                addCandidateIfValid(minX - 1, y);
            } else {
                // Vertical alignment
                int minY = Integer.MAX_VALUE;
                int maxY = Integer.MIN_VALUE;
                int x = first.x;
                for (Coordinate c : currentHits) {
                    if (c.x == x) {
                        if (c.y < minY) minY = c.y;
                        if (c.y > maxY) maxY = c.y;
                    }
                }
                addCandidateIfValid(x, maxY + 1);
                addCandidateIfValid(x, minY - 1);
            }
        }
    }

    private void addCandidateIfValid(int x, int y) {
        if (isValidUnattacked(x, y)) {
            Coordinate c = new Coordinate(x, y);
            if (!targetQueue.contains(c)) {
                targetQueue.push(c);
            }
        }
    }

    private boolean isValidUnattacked(int x, int y) {
        return x >= 0 && x < Board.SIZE && y >= 0 && y < Board.SIZE && !alreadyAttacked[y][x];
    }

    public boolean[][] getAlreadyAttacked() {
        return alreadyAttacked;
    }

    public List<Coordinate> getTargetQueueList() {
        return new ArrayList<>(targetQueue);
    }

    public List<Coordinate> getCurrentHits() {
        return new ArrayList<>(currentHits);
    }

    public void restore(boolean[][] attacked, List<Coordinate> queue, List<Coordinate> hits) {
        reset();
        if (attacked != null) {
            for (int y = 0; y < Board.SIZE && y < attacked.length; y++) {
                for (int x = 0; x < Board.SIZE && x < attacked[y].length; x++) {
                    alreadyAttacked[y][x] = attacked[y][x];
                }
            }
        }
        if (queue != null) {
            for (Coordinate c : queue) {
                targetQueue.push(c);
            }
        }
        if (hits != null) {
            currentHits.addAll(hits);
        }
    }
}
