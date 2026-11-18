package com.batallanaval.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa todos los datos necesarios para guardar y reanudar
 * una partida de Batalla Naval 3D.
 */
public class GameSaveData implements Serializable {
    private static final long serialVersionUID = 1L;

    public String fileName;
    public String saveDate;
    public long timestamp;
    public String title;

    // Estado del juego y telemetría
    public String gameState = "PLAYER_TURN";
    public float elapsedSeconds = 0f;
    public int totalPlayerShots = 0;
    public int totalPlayerHits = 0;
    public int roundNumber = 0;
    public int consecutiveMisses = 0;
    public double doubleShotChance = 0.0;
    public boolean doubleShotArmed = false;
    public boolean diceResolvedThisRound = false;

    // Celdas del tablero aliado [y][x]
    public String[][] playerGrid = new String[Board.SIZE][Board.SIZE];
    public List<ShipSaveData> playerShips = new ArrayList<>();

    // Celdas del tablero enemigo [y][x]
    public String[][] enemyGrid = new String[Board.SIZE][Board.SIZE];
    public List<ShipSaveData> enemyShips = new ArrayList<>();

    // Estado de la Inteligencia Artificial
    public boolean[][] aiAlreadyAttacked = new boolean[Board.SIZE][Board.SIZE];
    public List<Coordinate> aiTargetQueue = new ArrayList<>();
    public List<Coordinate> aiCurrentHits = new ArrayList<>();

    public GameSaveData() {
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                playerGrid[y][x] = CellState.EMPTY.name();
                enemyGrid[y][x] = CellState.EMPTY.name();
                aiAlreadyAttacked[y][x] = false;
            }
        }
    }

    public static class ShipSaveData implements Serializable {
        private static final long serialVersionUID = 1L;
        public String shipType;
        public int startX;
        public int startY;
        public boolean horizontal;
        public int hits;
        public boolean placed;

        public ShipSaveData() {}

        public ShipSaveData(String shipType, int startX, int startY, boolean horizontal, int hits, boolean placed) {
            this.shipType = shipType;
            this.startX = startX;
            this.startY = startY;
            this.horizontal = horizontal;
            this.hits = hits;
            this.placed = placed;
        }
    }

    public float getAccuracy() {
        return totalPlayerShots > 0 ? (totalPlayerHits * 100f / totalPlayerShots) : 0f;
    }

    public int getPlayerAliveCount() {
        int count = 0;
        for (ShipSaveData s : playerShips) {
            ShipType st = ShipType.valueOf(s.shipType);
            if (s.placed && s.hits < st.getSize()) {
                count++;
            }
        }
        return count;
    }

    public int getEnemySunkCount() {
        int count = 0;
        for (ShipSaveData s : enemyShips) {
            ShipType st = ShipType.valueOf(s.shipType);
            if (s.placed && s.hits >= st.getSize()) {
                count++;
            }
        }
        return count;
    }
}
