package com.batallanaval.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameSaveManagerTest {

    private final String testSaveName = "test_partida_junit.sav";

    @BeforeEach
    @AfterEach
    public void cleanup() {
        GameSaveManager.deleteSave(testSaveName);
    }

    @Test
    public void testSaveAndLoadGame() {
        GameSaveData data = new GameSaveData();
        data.title = "Partida de Prueba";
        data.gameState = "PLAYER_TURN";
        data.elapsedSeconds = 120.5f;
        data.totalPlayerShots = 12;
        data.totalPlayerHits = 6;
        data.roundNumber = 8;
        data.consecutiveMisses = 2;
        data.doubleShotChance = 0.75;
        data.doubleShotArmed = true;
        data.diceResolvedThisRound = true;

        // Custom grid
        data.playerGrid[0][0] = CellState.SHIP.name();
        data.playerGrid[0][1] = CellState.HIT.name();
        data.playerGrid[5][5] = CellState.MISS.name();

        // Custom ship
        data.playerShips.add(new GameSaveData.ShipSaveData(
                ShipType.PORTAVIONES.name(), 0, 0, true, 1, true));

        // Enemy grid
        data.enemyGrid[2][2] = CellState.SUNK.name();
        data.enemyShips.add(new GameSaveData.ShipSaveData(
                ShipType.DESTRUCTOR.name(), 2, 2, false, 2, true));

        // AI state
        data.aiAlreadyAttacked[3][3] = true;
        data.aiTargetQueue.add(new Coordinate(3, 4));
        data.aiCurrentHits.add(new Coordinate(3, 3));

        // Save
        boolean saved = GameSaveManager.saveGame(data, testSaveName);
        assertTrue(saved, "La partida debe guardarse correctamente");

        // Verify file exists
        File savedFile = new File(GameSaveManager.getSavesDirectory(), testSaveName);
        assertTrue(savedFile.exists(), "El archivo guardado debe existir en la carpeta 'partidas'");

        // Load
        GameSaveData loaded = GameSaveManager.loadGame(testSaveName);
        assertNotNull(loaded, "La partida cargada no debe ser nula");
        assertEquals("Partida de Prueba", loaded.title);
        assertEquals("PLAYER_TURN", loaded.gameState);
        assertEquals(12, loaded.totalPlayerShots);
        assertEquals(6, loaded.totalPlayerHits);
        assertEquals(8, loaded.roundNumber);
        assertEquals(2, loaded.consecutiveMisses);
        assertEquals(0.75, loaded.doubleShotChance, 0.001);
        assertTrue(loaded.doubleShotArmed);
        assertTrue(loaded.diceResolvedThisRound);

        // Verify grids
        assertEquals(CellState.SHIP.name(), loaded.playerGrid[0][0]);
        assertEquals(CellState.HIT.name(), loaded.playerGrid[0][1]);
        assertEquals(CellState.MISS.name(), loaded.playerGrid[5][5]);
        assertEquals(CellState.SUNK.name(), loaded.enemyGrid[2][2]);

        // Verify ships
        assertEquals(1, loaded.playerShips.size());
        assertEquals(ShipType.PORTAVIONES.name(), loaded.playerShips.get(0).shipType);
        assertEquals(1, loaded.playerShips.get(0).hits);
        assertTrue(loaded.playerShips.get(0).placed);

        // Verify AI
        assertTrue(loaded.aiAlreadyAttacked[3][3]);
        assertEquals(1, loaded.aiTargetQueue.size());
        assertEquals(3, loaded.aiTargetQueue.get(0).x);
        assertEquals(4, loaded.aiTargetQueue.get(0).y);
        assertEquals(1, loaded.aiCurrentHits.size());
    }

    @Test
    public void testListSavesContainsSavedGame() {
        GameSaveData data = new GameSaveData();
        data.title = "Lista de Guardados Test";
        data.roundNumber = 5;

        GameSaveManager.saveGame(data, testSaveName);

        List<GameSaveData> saves = GameSaveManager.listSaves();
        assertNotNull(saves);
        assertFalse(saves.isEmpty(), "La lista de partidas no debe estar vacía tras guardar");

        boolean found = false;
        for (GameSaveData s : saves) {
            if (testSaveName.equals(s.fileName)) {
                found = true;
                break;
            }
        }
        assertTrue(found, "La partida guardada debe ser listada por GameSaveManager.listSaves()");
    }

    @Test
    public void testDeleteSave() {
        GameSaveData data = new GameSaveData();
        data.title = "Para Borrar";
        GameSaveManager.saveGame(data, testSaveName);

        File f = new File(GameSaveManager.getSavesDirectory(), testSaveName);
        assertTrue(f.exists());

        boolean deleted = GameSaveManager.deleteSave(testSaveName);
        assertTrue(deleted);
        assertFalse(f.exists(), "El archivo debe haber sido eliminado");
    }
}
