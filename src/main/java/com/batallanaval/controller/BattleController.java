package com.batallanaval.controller;

import com.batallanaval.audio.SoundManager;
import com.batallanaval.model.*;
import com.batallanaval.ui.DiceResultTableOverlay;
import com.batallanaval.ui.DiceRollOverlay;
import com.batallanaval.ui.TacticalHud;
import com.batallanaval.view.Board3DView;
import com.batallanaval.view.CannonProjectile;
import com.batallanaval.view.Dice3DThrowAnimation;
import com.jme3.asset.AssetManager;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.InputManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BattleController {

    private final Board playerBoard;
    private final Board enemyBoard;
    private final Board3DView playerView;
    private final Board3DView enemyView;
    private final TacticalHud hud;
    private final CameraController cameraController;
    private final EnemyAI enemyAI;
    private final DiceRollOverlay diceOverlay;
    private DiceResultTableOverlay diceTableOverlay;

    private final AssetManager assetManager;
    private final Camera camera;
    private final InputManager inputManager;
    private final Node rootNode;

    private GameState state = GameState.PLACEMENT;
    private boolean horizontalPlacement = true;
    private int currentShipToPlaceIndex = 0;

    private int totalPlayerShots = 0;
    private int totalPlayerHits = 0;
    private float elapsedSeconds = 0f;

    private float aiTurnDelay = 0f;
    private final List<CannonProjectile> activeProjectiles = new ArrayList<>();
    private final List<Dice3DThrowAnimation> activeDiceAnimations = new ArrayList<>();
    private final Random random = new Random();

    // Habilidad de dados y sistema de ayudas
    private int roundNumber = 0;
    private int consecutiveMisses = 0;
    private double doubleShotChance = 0.0;
    private boolean doubleShotArmed = false;
    private boolean diceResolvedThisRound = false;

    private int hintX = -1;
    private int hintY = -1;
    private float hintTimer = 0f;
    private static final float HINT_DURATION = 8.0f;

    public BattleController(Board playerBoard, Board enemyBoard, Board3DView playerView, Board3DView enemyView,
                            TacticalHud hud, CameraController cameraController, AssetManager assetManager,
                            Camera camera, InputManager inputManager, Node rootNode,
                            DiceRollOverlay diceOverlay) {
        this.playerBoard = playerBoard;
        this.enemyBoard = enemyBoard;
        this.playerView = playerView;
        this.enemyView = enemyView;
        this.hud = hud;
        this.cameraController = cameraController;
        this.assetManager = assetManager;
        this.camera = camera;
        this.inputManager = inputManager;
        this.rootNode = rootNode;
        this.diceOverlay = diceOverlay;
        this.enemyAI = new EnemyAI(playerBoard);

        if (diceOverlay != null) {
            diceOverlay.setOnRollComplete(this::onDiceRollComplete);
        }

        setupHudCallbacks();
        initPlacementPhase();
    }

    public void setDiceTableOverlay(DiceResultTableOverlay diceTableOverlay) {
        this.diceTableOverlay = diceTableOverlay;
    }

    private void setupHudCallbacks() {
        hud.setAutoPlaceAction(this::autoPlacePlayerShips);
        hud.setRotateAction(this::toggleRotation);
        hud.setStartBattleAction(this::startBattle);
        hud.setRollDiceAction(this::launchDiceRoll);
    }

    public void initPlacementPhase() {
        state = GameState.PLACEMENT;
        currentShipToPlaceIndex = 0;
        playerBoard.clear();
        enemyBoard.clear();
        playerView.clearAllShips();
        playerView.clearAllMarkers();
        enemyView.clearAllShips();
        enemyView.clearAllMarkers();
        enemyAI.reset();

        totalPlayerShots = 0;
        totalPlayerHits = 0;
        elapsedSeconds = 0f;

        roundNumber = 0;
        consecutiveMisses = 0;
        doubleShotChance = 0.0;
        doubleShotArmed = false;
        diceResolvedThisRound = false;
        clearHint();
        if (diceOverlay != null) {
            diceOverlay.hide();
        }

        hud.setPlacementControlsEnabled(true);
        hud.setStartBattleEnabled(false);
        hud.setRotationButtonText(horizontalPlacement);
        hud.setRoundInfo(0, 8);
        hud.setDiceButtonState(false, 8);
        hud.updateStatusBanner("DESPLIEGUE: coloca tu flota en el tablero aliado", ColorRGBA.Yellow);
        hud.updateFleetStatus(playerBoard, enemyBoard, totalPlayerShots, totalPlayerHits);
        cameraController.setMode(CameraController.CameraMode.GENERAL);
    }

    public void autoPlacePlayerShips() {
        if (state != GameState.PLACEMENT) return;
        playerBoard.autoPlaceAllShips(random);
        playerView.clearAllShips();
        for (Ship s : playerBoard.getShips()) {
            playerView.showShip(s);
        }
        playerView.hideGhostShip();
        currentShipToPlaceIndex = playerBoard.getShips().size();
        hud.setStartBattleEnabled(true);
        hud.updateStatusBanner("Flota desplegada. Pulsa [INICIAR COMBATE]", new ColorRGBA(0.3f, 1.0f, 0.4f, 1f));
        hud.updateFleetStatus(playerBoard, enemyBoard, totalPlayerShots, totalPlayerHits);
        SoundManager.getInstance().play(SoundManager.SoundType.SONAR_PING);
    }

    public void clearPlayerShips() {
        if (state != GameState.PLACEMENT) return;
        playerBoard.clear();
        playerView.clearAllShips();
        playerView.hideGhostShip();
        currentShipToPlaceIndex = 0;
        hud.setStartBattleEnabled(false);
        hud.updateStatusBanner("Tablero limpio. Ubica tus barcos.", ColorRGBA.Yellow);
        hud.updateFleetStatus(playerBoard, enemyBoard, totalPlayerShots, totalPlayerHits);
    }

    public void toggleRotation() {
        horizontalPlacement = !horizontalPlacement;
        hud.setRotationButtonText(horizontalPlacement);
        SoundManager.getInstance().play(SoundManager.SoundType.BUTTON_CLICK);
    }

    public void startBattle() {
        if (!playerBoard.allShipsPlaced()) {
            hud.showAlert("Debes colocar todos los barcos primero!", ColorRGBA.Red, 2.0f);
            return;
        }

        // Deploy enemy fleet randomly
        enemyBoard.autoPlaceAllShips(random);
        playerView.hideGhostShip();

        hud.setPlacementControlsEnabled(false);
        hud.setStartBattleEnabled(false);
        hud.setDiceButtonState(false, 8);
        hud.showAlert("ZAFARRANCHO DE COMBATE", new ColorRGBA(0.2f, 1.0f, 0.4f, 1f), 2.2f);
        SoundManager.getInstance().play(SoundManager.SoundType.SONAR_PING);

        roundNumber = 0;
        consecutiveMisses = 0;
        doubleShotChance = 0.0;
        doubleShotArmed = false;
        diceResolvedThisRound = false;
        beginPlayerTurn();
    }

    public void restartGame() {
        initPlacementPhase();
        hud.showAlert("PARTIDA REINICIADA", ColorRGBA.Cyan, 1.5f);
    }

    public void update(float tpf) {
        hud.setCameraLabel(cameraController.getCurrentMode().label);
        playerView.update(tpf, camera);
        enemyView.update(tpf, camera);

        if (state != GameState.PLACEMENT) {
            elapsedSeconds += tpf;
        }

        if (hintTimer > 0f) {
            hintTimer -= tpf;
            if (hintTimer <= 0f) {
                clearHint();
            }
        }

        // Update active projectiles
        for (int i = activeProjectiles.size() - 1; i >= 0; i--) {
            CannonProjectile proj = activeProjectiles.get(i);
            proj.update(tpf);
            if (proj.isFinished()) {
                activeProjectiles.remove(i);
            }
        }

        // Update active 3D dice throw animations
        for (int i = activeDiceAnimations.size() - 1; i >= 0; i--) {
            Dice3DThrowAnimation anim = activeDiceAnimations.get(i);
            anim.update(tpf);
            if (anim.isCompleted()) {
                activeDiceAnimations.remove(i);
            }
        }

        // Process AI turn timing
        if (state == GameState.AI_TURN) {
            aiTurnDelay -= tpf;
            if (aiTurnDelay <= 0) {
                executeAiShot();
            }
        }

        // Handle mouse raycasting and hovering
        handleRaycasting();
    }

    private void handleRaycasting() {
        Vector2f mousePos = inputManager.getCursorPosition();

        if (hud.isMouseOverGui(mousePos.x, mousePos.y)) {
            playerView.clearHover();
            enemyView.clearHover();
            playerView.hideGhostShip();
            return;
        }

        CollisionResult hit = getSceneIntersection(mousePos);
        if (hit == null) {
            playerView.clearHover();
            enemyView.clearHover();
            playerView.hideGhostShip();
            return;
        }

        Geometry geom = hit.getGeometry();
        String boardId = geom.getUserData("boardId");
        Integer gridX = geom.getUserData("gridX");
        Integer gridY = geom.getUserData("gridY");

        if (boardId == null || gridX == null || gridY == null) {
            playerView.clearHover();
            enemyView.clearHover();
            playerView.hideGhostShip();
            return;
        }

        if (state == GameState.PLACEMENT && "player".equals(boardId)) {
            playerView.setHoverCell(gridX, gridY);
            enemyView.clearHover();

            if (currentShipToPlaceIndex < playerBoard.getShips().size()) {
                Ship curShip = playerBoard.getShips().get(currentShipToPlaceIndex);
                boolean valid = playerBoard.canPlaceShip(curShip, gridX, gridY, horizontalPlacement);
                playerView.showGhostShip(curShip.getType(), gridX, gridY, horizontalPlacement, valid);
            }
        } else if (state == GameState.PLAYER_TURN && "enemy".equals(boardId)) {
            playerView.clearHover();
            // Mira apuntando directamente en la posición exacta del puntero
            enemyView.setCursorPointer(hit.getContactPoint(), gridX, gridY);
        } else {
            playerView.clearHover();
            enemyView.clearHover();
            playerView.hideGhostShip();
        }
    }

    public void handleLeftClick() {
        if (state == GameState.DICE_ROLL) {
            return;
        }

        Vector2f mousePos = inputManager.getCursorPosition();

        // Check if clicked GUI button
        if (hud.handleMouseClick(mousePos.x, mousePos.y)) {
            return;
        }

        if (hud.isMouseOverGui(mousePos.x, mousePos.y)) {
            return;
        }

        CollisionResult hit = getSceneIntersection(mousePos);
        if (hit == null) return;

        Geometry geom = hit.getGeometry();
        String boardId = geom.getUserData("boardId");
        Integer gridX = geom.getUserData("gridX");
        Integer gridY = geom.getUserData("gridY");

        if (boardId == null || gridX == null || gridY == null) return;

        if (state == GameState.PLACEMENT && "player".equals(boardId)) {
            handleShipPlacementClick(gridX, gridY);
        } else if (state == GameState.PLAYER_TURN && "enemy".equals(boardId)) {
            handlePlayerAttackClick(gridX, gridY);
        }
    }

    private void handleShipPlacementClick(int x, int y) {
        if (currentShipToPlaceIndex >= playerBoard.getShips().size()) {
            return;
        }

        Ship ship = playerBoard.getShips().get(currentShipToPlaceIndex);
        if (playerBoard.canPlaceShip(ship, x, y, horizontalPlacement)) {
            playerBoard.placeShip(ship, x, y, horizontalPlacement);
            playerView.showShip(ship);
            SoundManager.getInstance().play(SoundManager.SoundType.BUTTON_CLICK);

            currentShipToPlaceIndex++;
            playerView.hideGhostShip();

            if (currentShipToPlaceIndex >= playerBoard.getShips().size()) {
                hud.setStartBattleEnabled(true);
                hud.updateStatusBanner("Todos los barcos ubicados. Pulsa [INICIAR COMBATE]", new ColorRGBA(0.2f, 1.0f, 0.4f, 1f));
            } else {
                Ship next = playerBoard.getShips().get(currentShipToPlaceIndex);
                hud.updateStatusBanner("Ubica tu " + next.getType().getDisplayName() + " [" + next.getType().getSize() + " celdas]", ColorRGBA.Yellow);
            }
            hud.updateFleetStatus(playerBoard, enemyBoard, totalPlayerShots, totalPlayerHits);
        } else {
            hud.showAlert("Posicion invalida: el barco no cabe o se cruza", ColorRGBA.Red, 1.2f);
        }
    }

    private void handlePlayerAttackClick(int x, int y) {
        if (state != GameState.PLAYER_TURN) {
            return;
        }

        CellState cell = enemyBoard.getCell(x, y);
        if (cell == CellState.HIT || cell == CellState.MISS || cell == CellState.SUNK) {
            hud.showAlert("Coordenada ya atacada", ColorRGBA.Orange, 1.0f);
            return;
        }

        enemyView.clearHover();
        if (x == hintX && y == hintY) {
            clearHint();
        }

        state = GameState.ANIMATING_PLAYER_SHOT;
        totalPlayerShots++;

        Vector3f startPos = playerView.getWorldCellCenter(random.nextInt(Board.SIZE), random.nextInt(Board.SIZE)).add(0, 2f, 0);
        Vector3f targetPos = enemyView.getWorldCellCenter(x, y);

        SoundManager.getInstance().play(SoundManager.SoundType.CANNON_FIRE);

        CannonProjectile proj = new CannonProjectile(assetManager, startPos, targetPos, 0.7f, 9.0f, () -> {
            ShotResult result = enemyBoard.receiveShot(x, y);
            enemyView.updateCellMarker(x, y, enemyBoard.getCell(x, y));

            String coordName = result.getCoordinate().toNavalNotation();

            if (result.getOutcome() == ShotResult.Outcome.MISS) {
                SoundManager.getInstance().play(SoundManager.SoundType.WATER_SPLASH);
                hud.showAlert("AGUA (" + coordName + ")", ColorRGBA.Cyan, 1.3f);
                hud.updateFleetStatus(playerBoard, enemyBoard, totalPlayerShots, totalPlayerHits);

                Coordinate hint = registerMissAndMaybeHint();

                if (doubleShotArmed) {
                    doubleShotArmed = false;
                    state = GameState.PLAYER_TURN;

                    if (hint != null) {
                        hud.showAlert("DOBLE DISPARO + PISTA EN " + hint.toNavalNotation(),
                                new ColorRGBA(1.0f, 0.82f, 0.20f, 1f), 3.0f);
                        hud.updateStatusBanner("DADOS DE GUERRA: doble disparo y pista activa en "
                                + hint.toNavalNotation(), new ColorRGBA(1.0f, 0.82f, 0.20f, 1f));
                    } else {
                        hud.showAlert("DOBLE DISPARO: vuelve a disparar",
                                new ColorRGBA(1.0f, 0.82f, 0.20f, 1f), 2.4f);
                        hud.updateStatusBanner("DADOS DE GUERRA: doble disparo activo. Dispara de nuevo",
                                new ColorRGBA(1.0f, 0.82f, 0.20f, 1f));
                    }
                } else {
                    if (hint != null) {
                        hud.showAlert("PISTA DE INTELIGENCIA: " + hint.toNavalNotation(),
                                new ColorRGBA(0.30f, 1.0f, 0.90f, 1f), 3.0f);
                        hud.updateStatusBanner("AYUDA ACTIVA: sigue el brillo en el radar enemigo",
                                new ColorRGBA(0.30f, 1.0f, 0.90f, 1f));
                    } else {
                        hud.updateStatusBanner("Turno del enemigo... preparando fuego", ColorRGBA.Orange);
                    }

                    state = GameState.AI_TURN;
                    aiTurnDelay = 0.9f;
                }
            } else if (result.getOutcome() == ShotResult.Outcome.HIT) {
                totalPlayerHits++;
                consecutiveMisses = 0;
                SoundManager.getInstance().play(SoundManager.SoundType.EXPLOSION);
                SoundManager.getInstance().play(SoundManager.SoundType.HIT_ALERT);

                // Casilla en negro de impacto
                enemyView.markCellExploded(x, y);

                hud.showAlert("IMPACTO CONFIRMADO EN " + coordName, ColorRGBA.Yellow, 1.6f);
                hud.updateStatusBanner("Impacto certero. Puedes disparar de nuevo", ColorRGBA.Yellow);
                hud.updateFleetStatus(playerBoard, enemyBoard, totalPlayerShots, totalPlayerHits);

                state = GameState.PLAYER_TURN;
            } else if (result.getOutcome() == ShotResult.Outcome.SUNK) {
                totalPlayerHits++;
                consecutiveMisses = 0;
                SoundManager.getInstance().play(SoundManager.SoundType.EXPLOSION);
                SoundManager.getInstance().play(SoundManager.SoundType.HIT_ALERT);

                Ship sunk = result.getHitShip();

                // 1. Casillas en negro del lugar donde explotó
                enemyView.markShipExploded(sunk);

                // 2. Actualizar marcadores de fuego
                for (Coordinate c : sunk.getOccupiedCoordinates()) {
                    enemyView.updateCellMarker(c.x, c.y, CellState.SUNK);
                }

                // 3. Mostrar barco y ejecutar animación de hundimiento en el agua
                enemyView.showShip(sunk);
                enemyView.startShipSinking(sunk);

                // 4. Comentario natural que aparece directamente sobre el barco destruido
                String phrase = PhraseBank.randomPhrase(random);
                enemyView.showFloatingComment(sunk, phrase);
                hud.showPhrase(phrase);

                hud.showAlert("HUNDISTE EL " + sunk.getType().getDisplayName().toUpperCase() + " ENEMIGO",
                        new ColorRGBA(1.0f, 0.3f, 0.1f, 1f), 2.2f);
                hud.updateFleetStatus(playerBoard, enemyBoard, totalPlayerShots, totalPlayerHits);

                if (enemyBoard.areAllShipsSunk()) {
                    triggerVictory();
                } else {
                    hud.updateStatusBanner("Barco enemigo destruido. Dispara otra vez", ColorRGBA.Yellow);
                    state = GameState.PLAYER_TURN;
                }
            }
        });

        rootNode.attachChild(proj);
        activeProjectiles.add(proj);
    }

    private Coordinate registerMissAndMaybeHint() {
        consecutiveMisses++;
        if (consecutiveMisses <= 3) {
            return null;
        }

        Coordinate hint = chooseHintCoordinate();
        if (hint == null) {
            return null;
        }

        consecutiveMisses = 0;
        hintX = hint.x;
        hintY = hint.y;
        hintTimer = HINT_DURATION;
        enemyView.showHintGlow(hint.x, hint.y);
        return hint;
    }

    private Coordinate chooseHintCoordinate() {
        List<Coordinate> candidates = enemyBoard.getShootableShipCells();
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private void clearHint() {
        hintX = -1;
        hintY = -1;
        hintTimer = 0f;
        enemyView.clearHintGlow();
    }

    private void beginPlayerTurn() {
        roundNumber++;
        doubleShotChance = 0.0;
        doubleShotArmed = false;
        diceResolvedThisRound = false;
        hud.setRoundInfo(roundNumber, roundsUntilDice());
        state = GameState.PLAYER_TURN;

        boolean canRoll = (roundNumber >= 8 && roundNumber % 8 == 0 && !diceResolvedThisRound);
        hud.setDiceButtonState(canRoll, roundsUntilDice());

        if (canRoll) {
            hud.showAlert("★ ¡DADOS DE GUERRA LISTOS! ★ Pulsa [LANZAR DADOS]",
                    new ColorRGBA(1.0f, 0.85f, 0.20f, 1f), 3.2f);
            hud.updateStatusBanner("HABILIDAD LISTA: Pulsa [LANZAR DADOS] o pulsa [X] para tirar al centro",
                    new ColorRGBA(1.0f, 0.85f, 0.20f, 1f));
            SoundManager.getInstance().play(SoundManager.SoundType.SONAR_PING);
        } else {
            hud.updateStatusBanner("RONDA " + roundNumber + ": tu turno. Selecciona un objetivo",
                    new ColorRGBA(0.3f, 0.9f, 1.0f, 1f));
        }
    }

    /**
     * El jugador activa manualmente la tirada de dados cuando esté disponible.
     * Coloca automáticamente la cámara cerca de los dados, lanza físicamente los dados con
     * animación 3D, y al asentarse despliega la tabla táctica de resultados y probabilidades aumentadas.
     */
    public void launchDiceRoll() {
        if (state != GameState.PLAYER_TURN) {
            return;
        }

        if (roundsUntilDice() != 0 || diceResolvedThisRound) {
            hud.showAlert("Los dados aún no están listos (Faltan " + roundsUntilDice() + " rondas)",
                    ColorRGBA.Orange, 1.6f);
            return;
        }

        state = GameState.DICE_ROLL;
        hud.setDiceButtonState(false, 8);
        hud.showAlert("¡LANZANDO DADOS // CÁMARA ACERCADA!", new ColorRGBA(1.0f, 0.85f, 0.20f, 1f), 2.2f);
        hud.updateStatusBanner("Tirando dados de guerra al centro del mar entre los tableros...",
                new ColorRGBA(1.0f, 0.85f, 0.20f, 1f));
        SoundManager.getInstance().play(SoundManager.SoundType.SONAR_PING);

        // 1. La cámara se coloca automáticamente cerca de los dados
        cameraController.focusOnDice();

        // 2. Generar tirada aleatoria y calcular probabilidades
        int d1 = random.nextInt(6) + 1;
        int d2 = random.nextInt(6) + 1;
        int sum = d1 + d2;
        double newProb = sum / 12.0;
        double oldProb = doubleShotChance;
        boolean doubleArmed = random.nextDouble() < newProb;

        // 3. Disparar la animación cinemática 3D en el canal central entre ambos tableros
        Dice3DThrowAnimation anim = new Dice3DThrowAnimation(assetManager, camera, d1, d2, (p) -> {});

        // 4. Cuando los dados se asienten en el agua, mostrar la tabla de resultados y probabilidades
        anim.setSettledListener((val1, val2, probability) -> {
            if (diceTableOverlay != null) {
                diceTableOverlay.showResults(val1, val2, oldProb, newProb, doubleArmed, roundNumber, () -> {
                    // Al pulsar "CONTINUAR AL COMBATE" en la tabla:
                    anim.finishAndRemove();
                    cameraController.restoreSavedView();
                    applyDiceResult(newProb, doubleArmed);
                });
            } else {
                anim.finishAndRemove();
                cameraController.restoreSavedView();
                applyDiceResult(newProb, doubleArmed);
            }
        });

        rootNode.attachChild(anim);
        activeDiceAnimations.add(anim);
    }

    public void showDiceRoll() {
        launchDiceRoll();
    }

    private void onDiceRollComplete(double probability) {
        boolean doubleArmed = random.nextDouble() < probability;
        applyDiceResult(probability, doubleArmed);
    }

    private void applyDiceResult(double probability, boolean doubleArmed) {
        doubleShotChance = probability;
        doubleShotArmed = doubleArmed;
        diceResolvedThisRound = true;
        state = GameState.PLAYER_TURN;
        hud.setRoundInfo(roundNumber, roundsUntilDice());
        hud.setDiceButtonState(false, roundsUntilDice());

        int percent = (int) Math.round(probability * 100.0);
        if (doubleShotArmed) {
            hud.showAlert("★ ¡MEJORA DESBLOQUEADA: DOBLE DISPARO AL " + percent + "%! ★",
                    new ColorRGBA(1.0f, 0.85f, 0.20f, 1f), 3.5f);
            hud.updateStatusBanner("★ DADOS DE GUERRA: ¡DOBLE DISPARO ACTIVO ESTE TURNO! ★",
                    new ColorRGBA(1.0f, 0.85f, 0.20f, 1f));
            SoundManager.getInstance().play(SoundManager.SoundType.HIT_ALERT);
        } else {
            hud.showAlert("★ MEJORA DE DADOS (" + percent + "%): SIN DOBLE DISPARO ★",
                    new ColorRGBA(0.80f, 0.85f, 0.90f, 1f), 3.0f);
            hud.updateStatusBanner("RONDA " + roundNumber + ": tu turno. Selecciona un objetivo",
                    new ColorRGBA(0.3f, 0.9f, 1.0f, 1f));
            SoundManager.getInstance().play(SoundManager.SoundType.BUTTON_CLICK);
        }
    }

    private int roundsUntilDice() {
        if (roundNumber % 8 == 0 && !diceResolvedThisRound) {
            return 0;
        }
        int mod = roundNumber % 8;
        return mod == 0 ? 8 : 8 - mod;
    }

    private void executeAiShot() {
        if (state != GameState.AI_TURN) return;

        state = GameState.ANIMATING_AI_SHOT;
        Coordinate target = enemyAI.chooseTarget();

        Vector3f startPos = enemyView.getWorldCellCenter(random.nextInt(Board.SIZE), random.nextInt(Board.SIZE)).add(0, 2f, 0);
        Vector3f targetPos = playerView.getWorldCellCenter(target.x, target.y);

        SoundManager.getInstance().play(SoundManager.SoundType.CANNON_FIRE);

        CannonProjectile proj = new CannonProjectile(assetManager, startPos, targetPos, 0.7f, 9.0f, () -> {
            ShotResult result = playerBoard.receiveShot(target.x, target.y);
            enemyAI.processShotResult(result);
            playerView.updateCellMarker(target.x, target.y, playerBoard.getCell(target.x, target.y));

            String coordName = target.toNavalNotation();

            if (result.getOutcome() == ShotResult.Outcome.MISS) {
                SoundManager.getInstance().play(SoundManager.SoundType.WATER_SPLASH);
                hud.showAlert("ENEMIGO DISPARO EN " + coordName + ": AGUA", ColorRGBA.Cyan, 1.4f);
                hud.updateFleetStatus(playerBoard, enemyBoard, totalPlayerShots, totalPlayerHits);
                beginPlayerTurn();
            } else if (result.getOutcome() == ShotResult.Outcome.HIT) {
                SoundManager.getInstance().play(SoundManager.SoundType.EXPLOSION);
                playerView.markCellExploded(target.x, target.y);
                hud.showAlert("ALERTA: EL ENEMIGO IMPACTO TU BARCO EN " + coordName, ColorRGBA.Orange, 1.6f);
                hud.updateFleetStatus(playerBoard, enemyBoard, totalPlayerShots, totalPlayerHits);

                // AI shoots again on hit!
                state = GameState.AI_TURN;
                aiTurnDelay = 0.9f;
            } else if (result.getOutcome() == ShotResult.Outcome.SUNK) {
                SoundManager.getInstance().play(SoundManager.SoundType.EXPLOSION);
                Ship sunk = result.getHitShip();

                // Casillas en negro del barco destruido
                playerView.markShipExploded(sunk);

                for (Coordinate c : sunk.getOccupiedCoordinates()) {
                    playerView.updateCellMarker(c.x, c.y, CellState.SUNK);
                }

                // Animación de hundimiento en agua
                playerView.startShipSinking(sunk);

                // Comentario natural flotante sobre el barco destruido
                String phrase = PhraseBank.randomPhrase(random);
                playerView.showFloatingComment(sunk, phrase);

                hud.showAlert("TU " + sunk.getType().getDisplayName().toUpperCase() + " FUE HUNDIDO", ColorRGBA.Red, 2.2f);
                hud.updateFleetStatus(playerBoard, enemyBoard, totalPlayerShots, totalPlayerHits);

                if (playerBoard.areAllShipsSunk()) {
                    triggerDefeat();
                } else {
                    // AI continues turn after sinking
                    state = GameState.AI_TURN;
                    aiTurnDelay = 0.9f;
                }
            }
        });

        rootNode.attachChild(proj);
        activeProjectiles.add(proj);
    }

    private void triggerVictory() {
        state = GameState.VICTORY;
        clearHint();
        if (diceOverlay != null) {
            diceOverlay.hide();
        }
        SoundManager.getInstance().play(SoundManager.SoundType.VICTORY);
        hud.showAlert("VICTORIA NAVAL ABSOLUTA", new ColorRGBA(0.2f, 1.0f, 0.3f, 1f), 10.0f);
        hud.updateStatusBanner("MISION CUMPLIDA. Has aniquilado la flota enemiga.", new ColorRGBA(0.2f, 1.0f, 0.3f, 1f));
    }

    private void triggerDefeat() {
        state = GameState.DEFEAT;
        clearHint();
        if (diceOverlay != null) {
            diceOverlay.hide();
        }
        SoundManager.getInstance().play(SoundManager.SoundType.DEFEAT);
        hud.showAlert("FLOTA ANIQUILADA - DERROTA", new ColorRGBA(1.0f, 0.2f, 0.2f, 1f), 10.0f);
        hud.updateStatusBanner("Tu flota fue hundida. Reinicia o revisa el resumen [ESC].", new ColorRGBA(1.0f, 0.2f, 0.2f, 1f));
    }

    private CollisionResult getSceneIntersection(Vector2f mousePos) {
        CollisionResults results = new CollisionResults();
        Vector3f click3d = camera.getWorldCoordinates(mousePos, 0f).clone();
        Vector3f dir = camera.getWorldCoordinates(mousePos, 1f).subtractLocal(click3d).normalizeLocal();
        Ray ray = new Ray(click3d, dir);
        rootNode.collideWith(ray, results);

        for (int i = 0; i < results.size(); i++) {
            CollisionResult cr = results.getCollision(i);
            if (cr.getGeometry() != null && cr.getGeometry().getUserData("boardId") != null) {
                return cr;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    //  CONSULTAS PARA EL HUD Y EL MENU DE PAUSA
    // ------------------------------------------------------------------

    public GameState getState() {
        return state;
    }

    public float getElapsedSeconds() {
        return elapsedSeconds;
    }

    public int getTotalPlayerShots() {
        return totalPlayerShots;
    }

    public int getTotalPlayerHits() {
        return totalPlayerHits;
    }

    public float getAccuracy() {
        return totalPlayerShots > 0 ? (totalPlayerHits * 100f / totalPlayerShots) : 0f;
    }

    public int getPlayerShipsAlive() {
        int alive = 0;
        for (Ship s : playerBoard.getShips()) {
            if (s.isPlaced() && !s.isSunk()) alive++;
        }
        return alive;
    }

    public int getFleetSize() {
        return playerBoard.getShips().size();
    }

    public int getEnemyShipsSunk() {
        return enemyBoard.getSunkShipsCount();
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public int getRoundsUntilDice() {
        return roundsUntilDice();
    }

    public double getDoubleShotChance() {
        return doubleShotChance;
    }

    public String getStateLabel() {
        switch (state) {
            case PLACEMENT:
                return "DESPLIEGUE DE FLOTA";
            case PLAYER_TURN:
                return "COMBATE - TU TURNO";
            case ANIMATING_PLAYER_SHOT:
            case PLAYER_DOUBLE_SHOT:
                return "COMBATE - DISPARO EN CURSO";
            case DICE_ROLL:
                return "HABILIDAD DADOS DE GUERRA";
            case AI_TURN:
            case ANIMATING_AI_SHOT:
                return "COMBATE - TURNO ENEMIGO";
            case VICTORY:
                return "VICTORIA NAVAL";
            case DEFEAT:
                return "DERROTA";
            case PAUSED:
                return "PARTIDA EN PAUSA";
            default:
                return "SIN PARTIDA";
        }
    }

    // ------------------------------------------------------------------
    //  PERSISTENCIA: GUARDAR Y REANUDAR PARTIDAS DESDE CARPETA
    // ------------------------------------------------------------------

    public GameSaveData createSaveData(String customTitle) {
        GameSaveData data = new GameSaveData();
        data.title = (customTitle != null && !customTitle.isEmpty())
                ? customTitle
                : "Ronda " + roundNumber + " - " + getStateLabel();
        data.gameState = state.name();
        data.elapsedSeconds = elapsedSeconds;
        data.totalPlayerShots = totalPlayerShots;
        data.totalPlayerHits = totalPlayerHits;
        data.roundNumber = roundNumber;
        data.consecutiveMisses = consecutiveMisses;
        data.doubleShotChance = doubleShotChance;
        data.doubleShotArmed = doubleShotArmed;
        data.diceResolvedThisRound = diceResolvedThisRound;

        // Tablero y barcos del jugador
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                data.playerGrid[y][x] = playerBoard.getCell(x, y).name();
            }
        }
        for (Ship s : playerBoard.getShips()) {
            data.playerShips.add(new GameSaveData.ShipSaveData(
                    s.getType().name(),
                    s.getStartX(),
                    s.getStartY(),
                    s.isHorizontal(),
                    s.getHits(),
                    s.isPlaced()
            ));
        }

        // Tablero y barcos del enemigo
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                data.enemyGrid[y][x] = enemyBoard.getCell(x, y).name();
            }
        }
        for (Ship s : enemyBoard.getShips()) {
            data.enemyShips.add(new GameSaveData.ShipSaveData(
                    s.getType().name(),
                    s.getStartX(),
                    s.getStartY(),
                    s.isHorizontal(),
                    s.getHits(),
                    s.isPlaced()
            ));
        }

        // IA enemiga
        boolean[][] aiAttacked = enemyAI.getAlreadyAttacked();
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                data.aiAlreadyAttacked[y][x] = aiAttacked[y][x];
            }
        }
        data.aiTargetQueue.addAll(enemyAI.getTargetQueueList());
        data.aiCurrentHits.addAll(enemyAI.getCurrentHits());

        return data;
    }

    public boolean loadFromSave(GameSaveData data) {
        if (data == null) return false;

        // 1. Limpiar efectos, pistas y elementos visuales
        clearHint();
        if (diceTableOverlay != null) {
            diceTableOverlay.hide();
        }
        if (diceOverlay != null) {
            diceOverlay.hide();
        }
        playerView.clearAllShips();
        playerView.clearAllMarkers();
        enemyView.clearAllShips();
        enemyView.clearAllMarkers();

        // 2. Restaurar variables numéricas y estado
        this.totalPlayerShots = data.totalPlayerShots;
        this.totalPlayerHits = data.totalPlayerHits;
        this.elapsedSeconds = data.elapsedSeconds;
        this.roundNumber = data.roundNumber;
        this.consecutiveMisses = data.consecutiveMisses;
        this.doubleShotChance = data.doubleShotChance;
        this.doubleShotArmed = data.doubleShotArmed;
        this.diceResolvedThisRound = data.diceResolvedThisRound;

        try {
            this.state = GameState.valueOf(data.gameState);
            if (this.state == GameState.DICE_ROLL || this.state == GameState.ANIMATING_PLAYER_SHOT || this.state == GameState.PLAYER_DOUBLE_SHOT) {
                this.state = GameState.PLAYER_TURN;
            } else if (this.state == GameState.ANIMATING_AI_SHOT) {
                this.state = GameState.AI_TURN;
            }
        } catch (Exception e) {
            this.state = GameState.PLAYER_TURN;
        }

        // 3. Restaurar tablero y barcos aliados
        playerBoard.clear();
        for (GameSaveData.ShipSaveData sd : data.playerShips) {
            for (Ship s : playerBoard.getShips()) {
                if (s.getType().name().equalsIgnoreCase(sd.shipType)) {
                    if (sd.placed) {
                        s.setPosition(sd.startX, sd.startY, sd.horizontal);
                        s.setHits(sd.hits);
                    }
                    break;
                }
            }
        }
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                try {
                    playerBoard.setCell(x, y, CellState.valueOf(data.playerGrid[y][x]));
                } catch (Exception ignored) {}
            }
        }

        // Mostrar barcos aliados desplegados y marcadores de impacto/agua/hundido
        for (Ship s : playerBoard.getShips()) {
            if (s.isPlaced()) {
                playerView.showShip(s);
            }
        }
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                CellState cell = playerBoard.getCell(x, y);
                if (cell == CellState.MISS || cell == CellState.HIT || cell == CellState.SUNK) {
                    playerView.updateCellMarker(x, y, cell);
                }
            }
        }

        // 4. Restaurar tablero y barcos enemigos
        enemyBoard.clear();
        for (GameSaveData.ShipSaveData sd : data.enemyShips) {
            for (Ship s : enemyBoard.getShips()) {
                if (s.getType().name().equalsIgnoreCase(sd.shipType)) {
                    if (sd.placed) {
                        s.setPosition(sd.startX, sd.startY, sd.horizontal);
                        s.setHits(sd.hits);
                    }
                    break;
                }
            }
        }
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                try {
                    enemyBoard.setCell(x, y, CellState.valueOf(data.enemyGrid[y][x]));
                } catch (Exception ignored) {}
            }
        }

        // Mostrar barcos enemigos que ya estén completamente hundidos y marcadores de tiro
        for (Ship s : enemyBoard.getShips()) {
            if (s.isSunk()) {
                enemyView.showShip(s);
                enemyView.markShipExploded(s);
            }
        }
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                CellState cell = enemyBoard.getCell(x, y);
                if (cell == CellState.MISS || cell == CellState.HIT || cell == CellState.SUNK) {
                    enemyView.updateCellMarker(x, y, cell);
                }
            }
        }

        // 5. Restaurar memoria de la IA
        enemyAI.restore(data.aiAlreadyAttacked, data.aiTargetQueue, data.aiCurrentHits);

        // 6. Actualizar el HUD
        hud.updateFleetStatus(playerBoard, enemyBoard, totalPlayerShots, totalPlayerHits);
        hud.setRoundInfo(roundNumber, roundsUntilDice());
        boolean canRoll = (roundNumber >= 8 && roundNumber % 8 == 0 && !diceResolvedThisRound);
        hud.setDiceButtonState(canRoll, roundsUntilDice());
        hud.setPlacementControlsEnabled(state == GameState.PLACEMENT);
        hud.setStartBattleEnabled(false);
        hud.updateStatusBanner("PARTIDA REANUDADA - " + getStateLabel(), ColorRGBA.Green);
        hud.showAlert("PARTIDA REANUDADA CORRECTAMENTE", new ColorRGBA(0.3f, 1.0f, 0.4f, 1f), 2.5f);

        return true;
    }
}
