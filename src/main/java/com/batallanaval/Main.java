package com.batallanaval;

import com.batallanaval.audio.SoundManager;
import com.batallanaval.controller.BattleController;
import com.batallanaval.controller.CameraController;
import com.batallanaval.model.Board;
import com.batallanaval.model.GameSaveData;
import com.batallanaval.model.GameSaveManager;
import com.batallanaval.ui.*;
import com.batallanaval.view.Board3DView;
import com.batallanaval.view.DecorativeFleet;
import com.batallanaval.view.ShipMeshFactory;
import com.batallanaval.view.SkyDome;
import com.batallanaval.view.WaterSurface;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FogFilter;
import com.jme3.system.AppSettings;

public class Main extends SimpleApplication {

    private WaterSurface waterSurface;
    private SkyDome skyDome;
    private DecorativeFleet decorativeFleet;

    private Board3DView playerView;
    private Board3DView enemyView;

    private TacticalHud hud;
    private DiceRollOverlay diceOverlay;
    private DiceResultTableOverlay diceTableOverlay;
    private SavedGamesModal savedGamesModal;
    private MainMenuScreen mainMenu;
    private PauseMenuScreen pauseMenu;
    private GameSettings gameSettings;

    private CameraController cameraController;
    private BattleController battleController;
    private ScreenshotAppState screenshotState;

    private boolean inMainMenu = true;
    private boolean paused = true;
    private boolean gameInProgress = false;
    private float menuOrbitTime = 0f;

    public static void main(String[] args) {
        Main app = new Main();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Batalla Naval 3D - jMonkeyEngine");
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setVSync(true);
        settings.setSamples(4);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true);
        setDisplayStatView(false);
        setDisplayFps(false);

        // Horizonte marino de fondo
        viewPort.setBackgroundColor(new ColorRGBA(0.24f, 0.40f, 0.54f, 1f));

        setupLighting();

        ShipMeshFactory shipFactory = new ShipMeshFactory(assetManager);
        setupEnvironment(shipFactory);
        setupPostProcessing();
        setupBoardsAndControllers(shipFactory);
        setupMenus();
        setupScreenshots();
        setupInputHandlers();

        enterMainMenu();
    }

    private void setupScreenshots() {
        new java.io.File("screenshots").mkdirs();
        screenshotState = new ScreenshotAppState();
        screenshotState.setFilePath("screenshots/");
        screenshotState.setFileName("batalla");
        screenshotState.setIsNumbered(true);
        stateManager.attach(screenshotState);
    }

    private void setupLighting() {
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.35f, -0.85f, -0.30f).normalizeLocal());
        sun.setColor(new ColorRGBA(1.0f, 0.97f, 0.90f, 1.0f).mult(1.35f));
        rootNode.addLight(sun);

        // Rebote azul del oceano
        DirectionalLight bounce = new DirectionalLight();
        bounce.setDirection(new Vector3f(0.4f, 0.25f, 0.45f).normalizeLocal());
        bounce.setColor(new ColorRGBA(0.20f, 0.42f, 0.66f, 1.0f).mult(0.75f));
        rootNode.addLight(bounce);

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(new ColorRGBA(0.38f, 0.46f, 0.56f, 1.0f));
        rootNode.addLight(ambient);
    }

    private void setupEnvironment(ShipMeshFactory shipFactory) {
        skyDome = new SkyDome(assetManager);
        rootNode.attachChild(skyDome);

        waterSurface = new WaterSurface(assetManager);
        rootNode.attachChild(waterSurface);

        decorativeFleet = new DecorativeFleet(assetManager, shipFactory);
        rootNode.attachChild(decorativeFleet);
    }

    private void setupPostProcessing() {
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        // Niebla exponencial suave: solo tiñe el horizonte, no la escena cercana.
        FogFilter fog = new FogFilter(new ColorRGBA(0.34f, 0.52f, 0.66f, 1f), 0.85f, 1600f);
        fpp.addFilter(fog);
        viewPort.addProcessor(fpp);
    }

    private void setupBoardsAndControllers(ShipMeshFactory shipFactory) {
        Board playerBoard = new Board();
        Board enemyBoard = new Board();

        playerView = new Board3DView("player", true, assetManager, shipFactory);
        playerView.setLocalTranslation(0f, 0f, 0f);
        rootNode.attachChild(playerView);

        enemyView = new Board3DView("enemy", false, assetManager, shipFactory);
        enemyView.setLocalTranslation(26f, 0f, 0f);
        rootNode.attachChild(enemyView);

        playerView.setActiveCamera(cam);
        enemyView.setActiveCamera(cam);

        cameraController = new CameraController(cam);

        hud = new TacticalHud(settings.getWidth(), settings.getHeight(), assetManager);
        guiNode.attachChild(hud);
        hud.setVisible(false);

        diceOverlay = new DiceRollOverlay(settings.getWidth(), settings.getHeight(), assetManager);
        diceOverlay.setLocalTranslation(0f, 0f, 60f);
        guiNode.attachChild(diceOverlay);
        diceOverlay.hide();

        diceTableOverlay = new DiceResultTableOverlay(settings.getWidth(), settings.getHeight(), assetManager);
        diceTableOverlay.setLocalTranslation(0f, 0f, 70f);
        guiNode.attachChild(diceTableOverlay);
        diceTableOverlay.hide();

        battleController = new BattleController(
                playerBoard, enemyBoard,
                playerView, enemyView,
                hud, cameraController,
                assetManager, cam, inputManager, rootNode,
                diceOverlay
        );
        battleController.setDiceTableOverlay(diceTableOverlay);
    }

    private void setupMenus() {
        gameSettings = new GameSettings();

        mainMenu = new MainMenuScreen(settings.getWidth(), settings.getHeight(),
                assetManager, gameSettings);
        guiNode.attachChild(mainMenu);
        mainMenu.setOnNewGame(this::startNewGame);
        mainMenu.setOnResume(this::openSavedGamesModal);
        mainMenu.setOnQuit(this::stop);

        pauseMenu = new PauseMenuScreen(settings.getWidth(), settings.getHeight(),
                assetManager, gameSettings);
        guiNode.attachChild(pauseMenu);
        pauseMenu.setOnResume(() -> resumeGame(false));
        pauseMenu.setOnRestart(this::restartFromPause);
        pauseMenu.setOnMainMenu(this::enterMainMenu);
        pauseMenu.setOnSave(this::saveCurrentGame);
        pauseMenu.setOnOpenSavesFolder(this::openSavedGamesModal);

        savedGamesModal = new SavedGamesModal(settings.getWidth(), settings.getHeight(), assetManager);
        savedGamesModal.setLocalTranslation(0f, 0f, 85f);
        savedGamesModal.setOnLoadGame(this::loadAndResumeGame);
        savedGamesModal.setOnClose(this::onSavedGamesModalClosed);
        guiNode.attachChild(savedGamesModal);

        hud.setPauseAction(this::togglePause);

        gameSettings.addListener(this::applySettings);
        applySettings();
    }

    private void applySettings() {
        waterSurface.setHighQuality(gameSettings.isHighQualityWater());
        hud.setHintsVisible(gameSettings.isShowHints());
    }

    // ------------------------------------------------------------------
    //  FLUJO DE PANTALLAS
    // ------------------------------------------------------------------

    private void enterMainMenu() {
        inMainMenu = true;
        paused = true;
        if (diceOverlay != null) {
            diceOverlay.hide();
        }
        if (diceTableOverlay != null) {
            diceTableOverlay.hide();
        }
        if (savedGamesModal != null) {
            savedGamesModal.hide();
        }

        int savesCount = GameSaveManager.listSaves().size();
        mainMenu.updateFolderStatus(savesCount, gameInProgress, battleController.getStateLabel());
        mainMenu.show();

        pauseMenu.hide();
        hud.setVisible(false);
    }

    private void openSavedGamesModal() {
        if (savedGamesModal != null) {
            savedGamesModal.show();
        }
    }

    private void onSavedGamesModalClosed() {
        if (inMainMenu) {
            int savesCount = GameSaveManager.listSaves().size();
            mainMenu.updateFolderStatus(savesCount, gameInProgress, battleController.getStateLabel());
        }
    }

    private void saveCurrentGame() {
        if (!gameInProgress) {
            pauseMenu.showSaveStatus("No hay ninguna partida activa para guardar.", false);
            return;
        }
        GameSaveData save = battleController.createSaveData(null);
        String fileName = GameSaveManager.generateDefaultFileName();
        boolean success = GameSaveManager.saveGame(save, fileName);
        if (success) {
            pauseMenu.showSaveStatus("✓ Partida guardada: " + fileName, true);
            hud.showAlert("PARTIDA GUARDADA EN CARPETA 'partidas'", ColorRGBA.Green, 2.5f);
            SoundManager.getInstance().play(SoundManager.SoundType.SONAR_PING);
        } else {
            pauseMenu.showSaveStatus("Error al guardar en carpeta 'partidas'.", false);
        }
    }

    private void loadAndResumeGame(GameSaveData saveData) {
        if (saveData == null) return;
        boolean loaded = battleController.loadFromSave(saveData);
        if (loaded) {
            gameInProgress = true;
            inMainMenu = false;
            paused = false;

            if (savedGamesModal != null) savedGamesModal.hide();
            mainMenu.hide();
            pauseMenu.hide();
            hud.setVisible(true);

            cameraController.applyModeInstant(CameraController.CameraMode.GENERAL);
            SoundManager.getInstance().play(SoundManager.SoundType.SONAR_PING);
        }
    }

    private void startNewGame() {
        battleController.restartGame();
        gameInProgress = true;

        inMainMenu = false;
        paused = false;

        mainMenu.hide();
        pauseMenu.hide();
        hud.setVisible(true);

        cameraController.applyModeInstant(CameraController.CameraMode.GENERAL);
    }

    private void resumeFromMainMenu() {
        openSavedGamesModal();
    }

    private void resumeGame(boolean fromMenu) {
        if (!gameInProgress) {
            startNewGame();
            return;
        }
        inMainMenu = false;
        paused = false;
        mainMenu.hide();
        pauseMenu.hide();
        hud.setVisible(true);
        if (fromMenu) {
            cameraController.applyModeInstant(cameraController.getCurrentMode());
        }
    }

    private void restartFromPause() {
        battleController.restartGame();
        resumeGame(false);
        cameraController.applyModeInstant(CameraController.CameraMode.GENERAL);
    }

    private void togglePause() {
        if (inMainMenu) return;
        if (diceOverlay != null && diceOverlay.isVisible()) {
            return;
        }
        if (diceTableOverlay != null && diceTableOverlay.isVisible()) {
            return;
        }
        if (savedGamesModal != null && savedGamesModal.isVisible()) {
            savedGamesModal.hide();
            return;
        }

        if (paused) {
            resumeGame(false);
        } else {
            paused = true;
            pauseMenu.refreshSummary(battleController);
            pauseMenu.show();
            hud.setVisible(false);
        }
    }

    // ------------------------------------------------------------------
    //  ENTRADA
    // ------------------------------------------------------------------

    private void setupInputHandlers() {
        inputManager.addMapping("LeftClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("RightClick", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addMapping("RotateShip", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("CycleCamera", new KeyTrigger(KeyInput.KEY_C));
        inputManager.addMapping("AutoPlaceKey", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("RollDiceKey", new KeyTrigger(KeyInput.KEY_X));
        inputManager.addMapping("Pause", new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addMapping("ScreenshotKey", new KeyTrigger(KeyInput.KEY_F12));

        inputManager.addMapping("PanLeft", new KeyTrigger(KeyInput.KEY_A), new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("PanRight", new KeyTrigger(KeyInput.KEY_D), new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("PanUp", new KeyTrigger(KeyInput.KEY_W), new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("PanDown", new KeyTrigger(KeyInput.KEY_S), new KeyTrigger(KeyInput.KEY_DOWN));

        inputManager.addMapping("ZoomIn", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping("ZoomOut", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));

        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (!isPressed) return;

                Vector2f mouse = inputManager.getCursorPosition();

                if ("ScreenshotKey".equals(name)) {
                    screenshotState.takeScreenshot();
                    return;
                }

                // Modal de partidas guardadas tiene prioridad absoluta de entrada
                if (savedGamesModal != null && savedGamesModal.isVisible()) {
                    if ("LeftClick".equals(name)) {
                        savedGamesModal.handleMouseClick(mouse.x, mouse.y);
                    } else if ("Pause".equals(name)) {
                        savedGamesModal.hide();
                        onSavedGamesModalClosed();
                    }
                    return;
                }

                // Tabla modal de resultados de dados tiene prioridad de entrada
                if (diceTableOverlay != null && diceTableOverlay.isVisible()) {
                    if ("LeftClick".equals(name)) {
                        diceTableOverlay.handleMouseClick(mouse.x, mouse.y);
                    }
                    return;
                }

                // La habilidad de dados es modal: absorbe toda la entrada hasta confirmar.
                if (!inMainMenu && !paused && diceOverlay.isVisible()) {
                    if ("LeftClick".equals(name)) {
                        diceOverlay.handleMouseClick(mouse.x, mouse.y);
                    }
                    return;
                }

                if ("Pause".equals(name)) {
                    if (inMainMenu) {
                        mainMenu.handleEscape();
                    } else if (paused) {
                        if (!pauseMenu.handleEscape()) {
                            resumeGame(false);
                        }
                    } else {
                        togglePause();
                    }
                    return;
                }

                if ("LeftClick".equals(name)) {
                    if (inMainMenu) {
                        mainMenu.handleMouseClick(mouse.x, mouse.y);
                    } else if (paused) {
                        pauseMenu.handleMouseClick(mouse.x, mouse.y);
                    } else {
                        battleController.handleLeftClick();
                    }
                    return;
                }

                if (inMainMenu || paused) return;

                if ("RightClick".equals(name) || "RotateShip".equals(name)) {
                    battleController.toggleRotation();
                } else if ("CycleCamera".equals(name)) {
                    cameraController.cycleMode();
                } else if ("AutoPlaceKey".equals(name)) {
                    battleController.autoPlacePlayerShips();
                } else if ("RollDiceKey".equals(name)) {
                    battleController.launchDiceRoll();
                }
            }
        }, "LeftClick", "RightClick", "RotateShip", "CycleCamera", "AutoPlaceKey", "RollDiceKey", "Pause", "ScreenshotKey");

        inputManager.addListener(new AnalogListener() {
            @Override
            public void onAnalog(String name, float value, float tpf) {
                if (inMainMenu || paused) return;

                float panSpeed = 22f * tpf;
                if ("PanLeft".equals(name)) {
                    cameraController.pan(-panSpeed, 0);
                } else if ("PanRight".equals(name)) {
                    cameraController.pan(panSpeed, 0);
                } else if ("PanUp".equals(name)) {
                    cameraController.pan(0, -panSpeed);
                } else if ("PanDown".equals(name)) {
                    cameraController.pan(0, panSpeed);
                } else if ("ZoomIn".equals(name)) {
                    cameraController.zoom(3.5f);
                } else if ("ZoomOut".equals(name)) {
                    cameraController.zoom(-3.5f);
                }
            }
        }, "PanLeft", "PanRight", "PanUp", "PanDown", "ZoomIn", "ZoomOut");
    }

    // ------------------------------------------------------------------
    //  BUCLE PRINCIPAL
    // ------------------------------------------------------------------

    @Override
    public void simpleUpdate(float tpf) {
        // La escena siempre respira: mar, cielo y flota decorativa.
        waterSurface.update(tpf);
        skyDome.update(tpf);
        decorativeFleet.update(tpf);
        hud.update(tpf);

        Vector2f mousePos = inputManager.getCursorPosition();

        if (savedGamesModal != null && savedGamesModal.isVisible()) {
            savedGamesModal.update(tpf);
            savedGamesModal.handleMouseMove(mousePos.x, mousePos.y);
            return;
        }

        if (inMainMenu) {
            menuOrbitTime += tpf;
            updateMenuCamera();
            mainMenu.update(tpf);
            mainMenu.handleMouseMove(mousePos.x, mousePos.y);
        } else if (paused) {
            pauseMenu.update(tpf);
            pauseMenu.handleMouseMove(mousePos.x, mousePos.y);
        } else {
            hud.handleMouseMove(mousePos.x, mousePos.y);
            cameraController.update(tpf);
            battleController.update(tpf);

            if (diceTableOverlay != null && diceTableOverlay.isVisible()) {
                diceTableOverlay.update(tpf);
                diceTableOverlay.handleMouseMove(mousePos.x, mousePos.y);
            }

            if (diceOverlay.isVisible()) {
                diceOverlay.update(tpf);
                diceOverlay.handleMouseMove(mousePos.x, mousePos.y);
            }
        }
    }

    private void updateMenuCamera() {
        float angle = 0.55f + menuOrbitTime * 0.045f;
        float radius = 42f;
        float x = 23f + FastMath.cos(angle) * radius;
        float z = 10f + FastMath.sin(angle) * radius;

        cam.setLocation(new Vector3f(x, 19f, z));
        cam.lookAt(new Vector3f(23f, 2.5f, 10f), Vector3f.UNIT_Y);
    }
}
