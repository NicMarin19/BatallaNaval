package com.batallanaval.ui;

import com.batallanaval.controller.BattleController;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu de pausa (ESC). Concentra el resumen de la partida, los controles y
 * las acciones de reinicio / ajustes / salida al menu principal.
 */
public class PauseMenuScreen extends Node {

    private static final float PANEL_W = 940f;
    private static final float PANEL_H = 560f;

    private static final int ROW_STATE = 0;
    private static final int ROW_TIME = 1;
    private static final int ROW_SHOTS = 2;
    private static final int ROW_HITS = 3;
    private static final int ROW_ACCURACY = 4;
    private static final int ROW_ALLY = 5;
    private static final int ROW_ENEMY = 6;

    private final float screenWidth;
    private final float screenHeight;
    private final AssetManager assetManager;
    private final BitmapFont font;

    private final List<GuiButton> buttons = new ArrayList<>();
    private final BitmapText[] valueTexts = new BitmapText[7];
    private final SettingsPanel settingsPanel;

    private Runnable onResume;
    private Runnable onRestart;
    private Runnable onMainMenu;
    private Runnable onSave;
    private Runnable onOpenSavesFolder;

    private BitmapText titleText;
    private BitmapText saveStatusText;
    private float pulseTime = 0f;

    public PauseMenuScreen(float screenWidth, float screenHeight,
                           AssetManager assetManager, GameSettings settings) {
        super("PauseMenuScreen");
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.assetManager = assetManager;
        this.font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        build();
        settingsPanel = new SettingsPanel(assetManager, font, settings, 540f, 340f);
        settingsPanel.setLocalTranslation(screenWidth * 0.5f, screenHeight * 0.5f, 40f);
        settingsPanel.setOnChange(settingsPanel::refreshLabels);
        attachChild(settingsPanel);

        setQueueBucket(RenderQueue.Bucket.Gui);
        hide();
    }

    private void build() {
        float panelX = (screenWidth - PANEL_W) * 0.5f;
        float panelY = (screenHeight - PANEL_H) * 0.5f;
        float top = panelY + PANEL_H;

        // Velo oscuro
        attachChild(UiKit.rect(assetManager, screenWidth, screenHeight,
                new ColorRGBA(0.005f, 0.015f, 0.035f, 0.76f), 0f, 0f, 5f));

        // Panel principal
        attachChild(UiKit.panelAt(assetManager, PANEL_W, PANEL_H,
                new ColorRGBA(0.020f, 0.050f, 0.090f, 1f),
                new ColorRGBA(0.26f, 0.70f, 1.00f, 1f),
                panelX, panelY, 6f));

        titleText = UiKit.label(assetManager, font, "MENU DE PAUSA",
                1.42f, UiKit.ACCENT, panelX + 30f, top - 26f, 7f);
        attachChild(titleText);

        attachChild(UiKit.labelRight(assetManager, font,
                "RESUMEN DE PARTIDA EN CURSO",
                0.62f, UiKit.TEXT_DIM, panelX + PANEL_W - 30f, top - 44f, 7f));

        attachChild(UiKit.hLine(assetManager, PANEL_W - 60f,
                new ColorRGBA(0.24f, 0.62f, 0.92f, 0.55f),
                panelX + 30f, top - 58f, 7f));

        buildSummaryColumn(panelX + 30f, top);
        buildControlsColumn(panelX + 500f, top);
        buildButtons(panelX, panelY);
    }

    private void buildSummaryColumn(float x, float top) {
        attachChild(UiKit.label(assetManager, font, "RESUMEN DE PARTIDA",
                0.82f, UiKit.ACCENT, x, top - 86f, 7f));

        attachChild(UiKit.hLine(assetManager, 400f,
                new ColorRGBA(0.22f, 0.55f, 0.85f, 0.35f), x, top - 96f, 7f));

        String[] labels = {
                "ESTADO",
                "TIEMPO DE PARTIDA",
                "DISPAROS REALIZADOS",
                "IMPACTOS CONFIRMADOS",
                "PRECISION DE TIRO",
                "FLOTA ALIADA",
                "FLOTA ENEMIGA"
        };

        float y = top - 126f;
        for (int i = 0; i < labels.length; i++) {
            attachChild(UiKit.label(assetManager, font, labels[i],
                    0.72f, UiKit.TEXT_DIM, x, y, 7f));

            valueTexts[i] = UiKit.label(assetManager, font, "---",
                    0.72f, UiKit.TEXT, x + 205f, y, 7f);
            attachChild(valueTexts[i]);

            y -= 32f;
        }

        attachChild(UiKit.hLine(assetManager, 400f,
                new ColorRGBA(0.22f, 0.55f, 0.85f, 0.35f), x, y + 6f, 7f));

        attachChild(UiKit.label(assetManager, font,
                "La partida queda en pausa mientras este menu esta abierto.",
                0.60f, new ColorRGBA(0.55f, 0.66f, 0.78f, 0.9f), x, y - 18f, 7f));
        attachChild(UiKit.label(assetManager, font,
                "Pulsa [ESC] para reanudar de inmediato.",
                0.60f, new ColorRGBA(0.55f, 0.66f, 0.78f, 0.9f), x, y - 38f, 7f));
    }

    private void buildControlsColumn(float x, float top) {
        attachChild(UiKit.label(assetManager, font, "CONTROLES",
                0.82f, UiKit.ACCENT, x, top - 86f, 7f));

        attachChild(UiKit.hLine(assetManager, 410f,
                new ColorRGBA(0.22f, 0.55f, 0.85f, 0.35f), x, top - 96f, 7f));

        String[][] controls = {
                {"CLIC IZQUIERDO", "Disparar / colocar barco"},
                {"CLIC DERECHO / R", "Rotar orientacion del barco"},
                {"W A S D / FLECHAS", "Mover camara"},
                {"RUEDA DEL RATON", "Zoom de camara"},
                {"ESPACIO", "Auto-colocar la flota"},
                {"C", "Cambiar modo de camara"},
                {"ESC", "Abrir / cerrar este resumen"}
        };

        float y = top - 128f;
        for (String[] row : controls) {
            attachChild(UiKit.label(assetManager, font, row[0],
                    0.66f, new ColorRGBA(0.55f, 0.86f, 1f, 1f), x, y, 7f));
            attachChild(UiKit.label(assetManager, font, row[1],
                    0.66f, UiKit.TEXT, x + 170f, y, 7f));
            y -= 29f;
        }
    }

    private void buildButtons(float panelX, float panelY) {
        float btnW = 270f;
        float btnH = 42f;
        float gap = 20f;
        float total = btnW * 3 + gap * 2;
        float startX = panelX + (PANEL_W - total) * 0.5f;

        // Fila Superior: Reanudar, Guardar Partida y Reanudar desde Carpeta
        float row2Y = panelY + 68f;

        GuiButton btnResume = new GuiButton("pauseResume", "REANUDAR", btnW, btnH,
                assetManager, font, UiKit.BTN_GREEN_BG, UiKit.BTN_GREEN_BD,
                () -> { if (onResume != null) onResume.run(); });
        btnResume.setLocalTranslation(startX, row2Y, 8f);
        attachChild(btnResume);
        buttons.add(btnResume);

        GuiButton btnSave = new GuiButton("pauseSave", "GUARDAR PARTIDA", btnW, btnH,
                assetManager, font,
                new ColorRGBA(0.18f, 0.14f, 0.03f, 0.95f),
                new ColorRGBA(1.0f, 0.82f, 0.20f, 1f),
                () -> { if (onSave != null) onSave.run(); });
        btnSave.setLocalTranslation(startX + btnW + gap, row2Y, 8f);
        attachChild(btnSave);
        buttons.add(btnSave);

        GuiButton btnFolder = new GuiButton("pauseFolder", "REANUDAR CARPETA", btnW, btnH,
                assetManager, font,
                new ColorRGBA(0.04f, 0.15f, 0.25f, 0.95f),
                new ColorRGBA(0.30f, 0.85f, 1.0f, 1f),
                () -> { if (onOpenSavesFolder != null) onOpenSavesFolder.run(); });
        btnFolder.setLocalTranslation(startX + (btnW + gap) * 2f, row2Y, 8f);
        attachChild(btnFolder);
        buttons.add(btnFolder);

        // Fila Inferior: Reiniciar, Ajustes y Menú Principal
        float row1Y = panelY + 18f;

        GuiButton btnRestart = new GuiButton("pauseRestart", "REINICIAR PARTIDA", btnW, btnH,
                assetManager, font, UiKit.BTN_ORANGE_BG, UiKit.BTN_ORANGE_BD,
                () -> { if (onRestart != null) onRestart.run(); });
        btnRestart.setLocalTranslation(startX, row1Y, 8f);
        attachChild(btnRestart);
        buttons.add(btnRestart);

        GuiButton btnSettings = new GuiButton("pauseSettings", "AJUSTES", btnW, btnH,
                assetManager, font, UiKit.BTN_BG, UiKit.BTN_BORDER,
                () -> settingsPanel.show());
        btnSettings.setLocalTranslation(startX + btnW + gap, row1Y, 8f);
        attachChild(btnSettings);
        buttons.add(btnSettings);

        GuiButton btnMainMenu = new GuiButton("pauseMainMenu", "MENU PRINCIPAL", btnW, btnH,
                assetManager, font, UiKit.BTN_RED_BG, UiKit.BTN_RED_BD,
                () -> { if (onMainMenu != null) onMainMenu.run(); });
        btnMainMenu.setLocalTranslation(startX + (btnW + gap) * 2f, row1Y, 8f);
        attachChild(btnMainMenu);
        buttons.add(btnMainMenu);

        saveStatusText = UiKit.label(assetManager, font, "",
                0.62f, new ColorRGBA(0.35f, 1.0f, 0.45f, 1f), panelX + 30f, panelY + 120f, 8f);
        attachChild(saveStatusText);
    }

    // ------------------------------------------------------------------
    //  DATOS
    // ------------------------------------------------------------------

    public void refreshSummary(BattleController battle) {
        valueTexts[ROW_STATE].setText(battle.getStateLabel());
        valueTexts[ROW_STATE].setColor(colorForState(battle));
        valueTexts[ROW_TIME].setText(UiKit.formatTime(battle.getElapsedSeconds()));
        valueTexts[ROW_SHOTS].setText(String.valueOf(battle.getTotalPlayerShots()));
        valueTexts[ROW_HITS].setText(String.valueOf(battle.getTotalPlayerHits()));
        valueTexts[ROW_ACCURACY].setText(String.format("%.1f%%", battle.getAccuracy()));
        valueTexts[ROW_ALLY].setText(battle.getPlayerShipsAlive() + " / " + battle.getFleetSize() + " operativos");
        valueTexts[ROW_ENEMY].setText(battle.getEnemyShipsSunk() + " / " + battle.getFleetSize() + " hundidos");
    }

    private ColorRGBA colorForState(BattleController battle) {
        switch (battle.getState()) {
            case VICTORY:
                return UiKit.OK;
            case DEFEAT:
                return UiKit.DANGER;
            case AI_TURN:
            case ANIMATING_AI_SHOT:
                return UiKit.WARN;
            default:
                return UiKit.ACCENT;
        }
    }

    // ------------------------------------------------------------------
    //  INTERACCION
    // ------------------------------------------------------------------

    public void update(float tpf) {
        pulseTime += tpf * 1.4f;
        float pulse = 0.90f + 0.10f * FastMath.sin(pulseTime);
        titleText.setColor(new ColorRGBA(0.30f * pulse, 0.82f * pulse, pulse, 1f));
    }

    public boolean handleMouseClick(float x, float y) {
        if (!isVisible()) return false;
        if (settingsPanel.isVisible()) {
            settingsPanel.handleMouseClick(x, y);
            return true;
        }
        for (GuiButton btn : buttons) {
            if (btn.onClick(x, y)) return true;
        }
        return true; // modal
    }

    public void handleMouseMove(float x, float y) {
        if (!isVisible()) return;
        if (settingsPanel.isVisible()) {
            settingsPanel.handleMouseMove(x, y);
            return;
        }
        for (GuiButton btn : buttons) {
            btn.updateHover(x, y);
        }
    }

    /** @return true si ESC cerro los ajustes y no debe reanudarse la partida. */
    public boolean handleEscape() {
        if (settingsPanel.isVisible()) {
            settingsPanel.hide();
            return true;
        }
        return false;
    }

    public void show() {
        setCullHint(CullHint.Inherit);
        settingsPanel.hide();
    }

    public void hide() {
        setCullHint(CullHint.Always);
        settingsPanel.hide();
    }

    public boolean isVisible() {
        return getCullHint() != CullHint.Always;
    }

    public void toggle() {
        if (isVisible()) hide();
        else show();
    }

    public void setOnResume(Runnable action) {
        this.onResume = action;
    }

    public void setOnRestart(Runnable action) {
        this.onRestart = action;
    }

    public void setOnMainMenu(Runnable action) {
        this.onMainMenu = action;
    }

    public void setOnSave(Runnable action) {
        this.onSave = action;
    }

    public void setOnOpenSavesFolder(Runnable action) {
        this.onOpenSavesFolder = action;
    }

    public void showSaveStatus(String message, boolean success) {
        if (saveStatusText != null) {
            saveStatusText.setText(message);
            saveStatusText.setColor(success ? new ColorRGBA(0.35f, 1.0f, 0.45f, 1f) : UiKit.DANGER);
        }
    }
}
