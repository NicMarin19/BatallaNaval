package com.batallanaval.model;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Gestor de guardado y carga de partidas en la carpeta dedicada 'partidas'.
 * Implementa persistencia robusta y legible en formato de texto estructurado UTF-8.
 */
public class GameSaveManager {

    public static final String SAVES_FOLDER_NAME = "partidas";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    /**
     * Obtiene el directorio de partidas, creándolo si aún no existe.
     */
    public static File getSavesDirectory() {
        File dir = new File(SAVES_FOLDER_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Genera un nombre de archivo por defecto con la fecha y hora actual.
     */
    public static String generateDefaultFileName() {
        return "partida_" + FILE_DATE_FORMAT.format(new Date()) + ".sav";
    }

    /**
     * Guarda una partida en la carpeta 'partidas'.
     */
    public static boolean saveGame(GameSaveData data, String filename) {
        if (data == null) return false;
        if (filename == null || filename.trim().isEmpty()) {
            filename = generateDefaultFileName();
        }
        if (!filename.endsWith(".sav") && !filename.endsWith(".save")) {
            filename += ".sav";
        }

        File folder = getSavesDirectory();
        File saveFile = new File(folder, filename);

        data.fileName = filename;
        data.timestamp = System.currentTimeMillis();
        data.saveDate = DATE_FORMAT.format(new Date(data.timestamp));

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(saveFile), StandardCharsets.UTF_8))) {

            writer.println("[METADATA]");
            writer.println("fileName=" + data.fileName);
            writer.println("saveDate=" + data.saveDate);
            writer.println("timestamp=" + data.timestamp);
            writer.println("title=" + (data.title != null ? data.title : "Batalla Naval"));
            writer.println("gameState=" + data.gameState);
            writer.println("elapsedSeconds=" + data.elapsedSeconds);
            writer.println("totalPlayerShots=" + data.totalPlayerShots);
            writer.println("totalPlayerHits=" + data.totalPlayerHits);
            writer.println("roundNumber=" + data.roundNumber);
            writer.println("consecutiveMisses=" + data.consecutiveMisses);
            writer.println("doubleShotChance=" + data.doubleShotChance);
            writer.println("doubleShotArmed=" + data.doubleShotArmed);
            writer.println("diceResolvedThisRound=" + data.diceResolvedThisRound);
            writer.println();

            writer.println("[PLAYER_GRID]");
            for (int y = 0; y < Board.SIZE; y++) {
                StringBuilder sb = new StringBuilder();
                for (int x = 0; x < Board.SIZE; x++) {
                    if (x > 0) sb.append(",");
                    sb.append(data.playerGrid[y][x]);
                }
                writer.println(sb.toString());
            }
            writer.println();

            writer.println("[PLAYER_SHIPS]");
            for (GameSaveData.ShipSaveData s : data.playerShips) {
                writer.println(String.format("%s;%d;%d;%b;%d;%b",
                        s.shipType, s.startX, s.startY, s.horizontal, s.hits, s.placed));
            }
            writer.println();

            writer.println("[ENEMY_GRID]");
            for (int y = 0; y < Board.SIZE; y++) {
                StringBuilder sb = new StringBuilder();
                for (int x = 0; x < Board.SIZE; x++) {
                    if (x > 0) sb.append(",");
                    sb.append(data.enemyGrid[y][x]);
                }
                writer.println(sb.toString());
            }
            writer.println();

            writer.println("[ENEMY_SHIPS]");
            for (GameSaveData.ShipSaveData s : data.enemyShips) {
                writer.println(String.format("%s;%d;%d;%b;%d;%b",
                        s.shipType, s.startX, s.startY, s.horizontal, s.hits, s.placed));
            }
            writer.println();

            writer.println("[AI_ATTACKED]");
            for (int y = 0; y < Board.SIZE; y++) {
                StringBuilder sb = new StringBuilder();
                for (int x = 0; x < Board.SIZE; x++) {
                    if (x > 0) sb.append(",");
                    sb.append(data.aiAlreadyAttacked[y][x]);
                }
                writer.println(sb.toString());
            }
            writer.println();

            writer.println("[AI_QUEUE]");
            for (Coordinate c : data.aiTargetQueue) {
                writer.println(c.x + "," + c.y);
            }
            writer.println();

            writer.println("[AI_HITS]");
            for (Coordinate c : data.aiCurrentHits) {
                writer.println(c.x + "," + c.y);
            }
            writer.println();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Carga una partida por nombre de archivo.
     */
    public static GameSaveData loadGame(String filename) {
        if (filename == null) return null;
        File file = new File(getSavesDirectory(), filename);
        return loadGame(file);
    }

    /**
     * Carga una partida desde un archivo.
     */
    public static GameSaveData loadGame(File file) {
        if (file == null || !file.exists() || !file.canRead()) {
            return null;
        }

        GameSaveData data = new GameSaveData();
        data.fileName = file.getName();
        data.timestamp = file.lastModified();
        data.saveDate = DATE_FORMAT.format(new Date(data.timestamp));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            String currentSection = "";
            int playerGridRow = 0;
            int enemyGridRow = 0;
            int aiAttackedRow = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line.substring(1, line.length() - 1);
                    continue;
                }

                switch (currentSection) {
                    case "METADATA":
                        int eq = line.indexOf('=');
                        if (eq != -1) {
                            String k = line.substring(0, eq).trim();
                            String v = line.substring(eq + 1).trim();
                            parseMetadataKey(data, k, v);
                        }
                        break;

                    case "PLAYER_GRID":
                        if (playerGridRow < Board.SIZE) {
                            String[] cells = line.split(",");
                            for (int x = 0; x < Board.SIZE && x < cells.length; x++) {
                                data.playerGrid[playerGridRow][x] = cells[x].trim();
                            }
                            playerGridRow++;
                        }
                        break;

                    case "PLAYER_SHIPS":
                        GameSaveData.ShipSaveData ps = parseShipLine(line);
                        if (ps != null) {
                            data.playerShips.add(ps);
                        }
                        break;

                    case "ENEMY_GRID":
                        if (enemyGridRow < Board.SIZE) {
                            String[] cells = line.split(",");
                            for (int x = 0; x < Board.SIZE && x < cells.length; x++) {
                                data.enemyGrid[enemyGridRow][x] = cells[x].trim();
                            }
                            enemyGridRow++;
                        }
                        break;

                    case "ENEMY_SHIPS":
                        GameSaveData.ShipSaveData es = parseShipLine(line);
                        if (es != null) {
                            data.enemyShips.add(es);
                        }
                        break;

                    case "AI_ATTACKED":
                        if (aiAttackedRow < Board.SIZE) {
                            String[] cells = line.split(",");
                            for (int x = 0; x < Board.SIZE && x < cells.length; x++) {
                                data.aiAlreadyAttacked[aiAttackedRow][x] = Boolean.parseBoolean(cells[x].trim());
                            }
                            aiAttackedRow++;
                        }
                        break;

                    case "AI_QUEUE":
                        Coordinate qc = parseCoord(line);
                        if (qc != null) {
                            data.aiTargetQueue.add(qc);
                        }
                        break;

                    case "AI_HITS":
                        Coordinate hc = parseCoord(line);
                        if (hc != null) {
                            data.aiCurrentHits.add(hc);
                        }
                        break;
                }
            }

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void parseMetadataKey(GameSaveData data, String k, String v) {
        try {
            switch (k) {
                case "fileName": data.fileName = v; break;
                case "saveDate": data.saveDate = v; break;
                case "timestamp": data.timestamp = Long.parseLong(v); break;
                case "title": data.title = v; break;
                case "gameState": data.gameState = v; break;
                case "elapsedSeconds": data.elapsedSeconds = Float.parseFloat(v); break;
                case "totalPlayerShots": data.totalPlayerShots = Integer.parseInt(v); break;
                case "totalPlayerHits": data.totalPlayerHits = Integer.parseInt(v); break;
                case "roundNumber": data.roundNumber = Integer.parseInt(v); break;
                case "consecutiveMisses": data.consecutiveMisses = Integer.parseInt(v); break;
                case "doubleShotChance": data.doubleShotChance = Double.parseDouble(v); break;
                case "doubleShotArmed": data.doubleShotArmed = Boolean.parseBoolean(v); break;
                case "diceResolvedThisRound": data.diceResolvedThisRound = Boolean.parseBoolean(v); break;
            }
        } catch (Exception ignored) {}
    }

    private static GameSaveData.ShipSaveData parseShipLine(String line) {
        String[] parts = line.split(";");
        if (parts.length >= 6) {
            try {
                return new GameSaveData.ShipSaveData(
                        parts[0].trim(),
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim()),
                        Boolean.parseBoolean(parts[3].trim()),
                        Integer.parseInt(parts[4].trim()),
                        Boolean.parseBoolean(parts[5].trim())
                );
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static Coordinate parseCoord(String line) {
        String[] parts = line.split(",");
        if (parts.length >= 2) {
            try {
                return new Coordinate(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Lista todas las partidas guardadas en la carpeta 'partidas',
     * ordenadas cronológicamente de la más reciente a la más antigua.
     */
    public static List<GameSaveData> listSaves() {
        File dir = getSavesDirectory();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".sav") || name.endsWith(".save"));
        List<GameSaveData> list = new ArrayList<>();

        if (files != null) {
            for (File f : files) {
                GameSaveData s = loadGame(f);
                if (s != null) {
                    list.add(s);
                }
            }
        }

        // Ordenar por fecha descendente (más recientes primero)
        list.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        return list;
    }

    /**
     * Elimina un archivo de partida guardada.
     */
    public static boolean deleteSave(String filename) {
        if (filename == null) return false;
        File file = new File(getSavesDirectory(), filename);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }
}
