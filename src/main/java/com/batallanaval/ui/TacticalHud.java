package com.batallanaval.ui;

import com.batallanaval.model.Board;
import com.batallanaval.model.PhraseBank;
import com.batallanaval.model.Ship;
import com.batallanaval.view.TextureGenerator;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD tactico limpio. Solo muestra la informacion esencial de la partida:
 * estado, flotas, telemetria de tiro y los botones de la fase actual.
 * El resto de opciones vive en el menu de pausa (ESC).
 */
public class TacticalHud extends Node {

    private static final float TOP_BAR_H = 60f;
    private static final float BOTTOM_ZONE_H = 62f;
    private static final float PANEL_W_LEFT = 256f;
    private static final float PANEL_W_RIGHT = 292f;
    private static final float PANEL_H = 168f;
    private static final float PANEL_MARGIN = 18f;

    private final float screenWidth;
    private final float screenHeight;
    private final AssetManager assetManager;
    private final BitmapFont font;

    private final List<GuiButton> buttons = new ArrayList<>();

    private BitmapText statusBannerText;
    private BitmapText hintText;
    private BitmapText subtitleText;

    private final List<BitmapText> playerShipTexts = new ArrayList<>();
    private final List<BitmapText> enemyShipTexts = new ArrayList<>();
    private BitmapText statsText;

    private Node alertNode;
    private BitmapText alertText;
    private Geometry alertBorder;
    private float alertTimer = 0f;

    private Node phraseNode;
    private BitmapText phraseTitle;
    private BitmapText phraseText;
    private float phraseTimer = 0f;
    private float phraseX;
    private float phraseY;
    private float phraseWidth;
    private float phraseHeight;

    private Node placementBar;
    private GuiButton btnAutoPlace;
    private GuiButton btnRotate;
    private GuiButton btnStartBattle;
    private GuiButton btnPause;
    private GuiButton btnRollDice;

    private float pulseTime = 0f;

    public TacticalHud(float screenWidth, float screenHeight, AssetManager assetManager) {
        super("TacticalHud");
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.assetManager = assetManager;
        this.font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        buildTopBar();
        buildFleetPanels();
        buildBottomBar();
        buildCenterAlert();
        buildPhraseCard();

        setQueueBucket(RenderQueue.Bucket.Gui);
    }

    // ------------------------------------------------------------------
    //  CONSTRUCCION
    // ------------------------------------------------------------------

    private void buildTopBar() {
        attachChild(UiKit.panelAt(assetManager, screenWidth, TOP_BAR_H,
                new ColorRGBA(0.020f, 0.052f, 0.096f, 0.90f),
                new ColorRGBA(0.18f, 0.52f, 0.82f, 0.85f),
                0f, screenHeight - TOP_BAR_H, 0f));

        attachChild(UiKit.hLine(assetManager, screenWidth,
                new ColorRGBA(0.25f, 0.72f, 1f, 0.35f), 0f, screenHeight - TOP_BAR_H, 1f));

        attachChild(UiKit.label(assetManager, font, "BATALLA NAVAL 3D",
                1.02f, new ColorRGBA(0.48f, 0.90f, 1f, 1f), 20f, screenHeight - 14f, 1f));

        subtitleText = UiKit.label(assetManager, font, "SISTEMA DE COMANDO TACTICO",
                0.58f, new ColorRGBA(0.50f, 0.64f, 0.76f, 0.9f), 20f, screenHeight - 38f, 1f);
        attachChild(subtitleText);

        statusBannerText = UiKit.labelRight(assetManager, font,
                "DESPLIEGUE: coloca tu flota",
                0.92f, UiKit.WARN, screenWidth - 20f, screenHeight - 24f, 1f);
        attachChild(statusBannerText);

        hintText = UiKit.labelRight(assetManager, font,
                "[ESC] RESUMEN   |   CAMARA: GENERAL [C]",
                0.60f, new ColorRGBA(0.56f, 0.68f, 0.80f, 0.9f),
                screenWidth - 20f, screenHeight - 44f, 1f);
        attachChild(hintText);
    }

    private void buildFleetPanels() {
        float yPos = screenHeight - TOP_BAR_H - 12f - PANEL_H;

        // --- Panel izquierdo: flota aliada ---
        attachChild(UiKit.panelAt(assetManager, PANEL_W_LEFT, PANEL_H,
                new ColorRGBA(0.030f, 0.085f, 0.150f, 0.86f),
                new ColorRGBA(0.20f, 0.62f, 0.92f, 0.80f),
                PANEL_MARGIN, yPos, 0f));

        attachChild(UiKit.label(assetManager, font, "FLOTA ALIADA",
                0.80f, new ColorRGBA(0.40f, 0.90f, 1f, 1f),
                PANEL_MARGIN + 14f, yPos + PANEL_H - 14f, 1f));

        attachChild(UiKit.hLine(assetManager, PANEL_W_LEFT - 28f,
                new ColorRGBA(0.22f, 0.58f, 0.88f, 0.45f),
                PANEL_MARGIN + 14f, yPos + PANEL_H - 30f, 1f));

        for (int i = 0; i < 5; i++) {
            BitmapText shipTxt = UiKit.label(assetManager, font, "Unidad " + (i + 1),
                    0.68f, ColorRGBA.White,
                    PANEL_MARGIN + 14f, yPos + PANEL_H - 50f - i * 21f, 1f);
            attachChild(shipTxt);
            playerShipTexts.add(shipTxt);
        }

        // --- Panel derecho: radar enemigo + telemetria ---
        float rightX = screenWidth - PANEL_W_RIGHT - PANEL_MARGIN;
        attachChild(UiKit.panelAt(assetManager, PANEL_W_RIGHT, PANEL_H,
                new ColorRGBA(0.120f, 0.048f, 0.042f, 0.86f),
                new ColorRGBA(0.92f, 0.42f, 0.16f, 0.80f),
                rightX, yPos, 0f));

        attachChild(UiKit.label(assetManager, font, "RADAR ENEMIGO",
                0.80f, new ColorRGBA(1f, 0.58f, 0.24f, 1f),
                rightX + 14f, yPos + PANEL_H - 14f, 1f));

        attachChild(UiKit.hLine(assetManager, PANEL_W_RIGHT - 28f,
                new ColorRGBA(0.90f, 0.42f, 0.18f, 0.45f),
                rightX + 14f, yPos + PANEL_H - 30f, 1f));

        for (int i = 0; i < 5; i++) {
            BitmapText enemyTxt = UiKit.label(assetManager, font, "Contacto " + (i + 1),
                    0.68f, new ColorRGBA(0.82f, 0.84f, 0.88f, 1f),
                    rightX + 14f, yPos + PANEL_H - 50f - i * 21f, 1f);
            attachChild(enemyTxt);
            enemyShipTexts.add(enemyTxt);
        }

        statsText = UiKit.label(assetManager, font, "Tiros 0 | Impactos 0 | Precision 0.0%",
                0.58f, new ColorRGBA(0.92f, 0.96f, 1f, 1f),
                rightX + 14f, yPos + 16f, 1f);
        attachChild(statsText);
    }

    private void buildBottomBar() {
        float yPos = 18f;

        // Barra de despliegue (solo visible durante la colocacion)
        placementBar = new Node("PlacementBar");

        btnAutoPlace = new GuiButton("autoPlace", "AUTO-COLOCAR", 152f, 40f,
                assetManager, font, UiKit.BTN_BG, UiKit.BTN_BORDER, null);
        btnAutoPlace.setLocalTranslation(0f, 0f, 1f);
        placementBar.attachChild(btnAutoPlace);
        buttons.add(btnAutoPlace);

        btnRotate = new GuiButton("rotate", "ROTAR: HORIZ [R]", 178f, 40f,
                assetManager, font, UiKit.BTN_ORANGE_BG, UiKit.BTN_ORANGE_BD, null);
        btnRotate.setLocalTranslation(164f, 0f, 1f);
        placementBar.attachChild(btnRotate);
        buttons.add(btnRotate);

        btnStartBattle = new GuiButton("startBattle", "INICIAR COMBATE", 196f, 40f,
                assetManager, font, UiKit.BTN_GREEN_BG, UiKit.BTN_GREEN_BD, null);
        btnStartBattle.setLocalTranslation(354f, 0f, 1f);
        btnStartBattle.setEnabled(false);
        placementBar.attachChild(btnStartBattle);
        buttons.add(btnStartBattle);

        placementBar.setLocalTranslation(PANEL_MARGIN, yPos, 1f);
        attachChild(placementBar);

        // Boton táctico para lanzar los dados cuando estén listos
        btnRollDice = new GuiButton("rollDice", "DADOS EN 8", 220f, 40f,
                assetManager, font,
                new ColorRGBA(0.12f, 0.08f, 0.02f, 0.95f),
                new ColorRGBA(1.0f, 0.80f, 0.15f, 0.95f),
                null);
        btnRollDice.setLocalTranslation(PANEL_MARGIN, yPos, 1f);
        btnRollDice.setEnabled(false);
        btnRollDice.setCullHint(CullHint.Always);
        attachChild(btnRollDice);
        buttons.add(btnRollDice);

        // Boton permanente de pausa
        btnPause = new GuiButton("pause", "[ESC] MENU", 124f, 40f,
                assetManager, font, UiKit.BTN_BG, UiKit.BTN_BORDER, null);
        btnPause.setLocalTranslation(screenWidth - 124f - PANEL_MARGIN, yPos, 1f);
        attachChild(btnPause);
        buttons.add(btnPause);
    }

    private void buildCenterAlert() {
        alertNode = new Node("CenterAlert");
        float w = 620f;
        float h = 70f;

        Quad q = new Quad(w, h);
        Geometry bg = new Geometry("AlertBg", q);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture tex = TextureGenerator.createButtonTexture((int) w, (int) h,
                new ColorRGBA(0.030f, 0.060f, 0.105f, 0.96f),
                new ColorRGBA(1.0f, 0.80f, 0.20f, 1f));
        mat.setTexture("ColorMap", tex);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        bg.setMaterial(mat);
        bg.setLocalTranslation(-w * 0.5f, -h * 0.5f, 0f);
        alertNode.attachChild(bg);

        alertBorder = UiKit.hLine(assetManager, w - 40f,
                new ColorRGBA(1f, 0.82f, 0.22f, 0.65f), -w * 0.5f + 20f, -h * 0.5f + 8f, 1f);
        alertNode.attachChild(alertBorder);

        alertText = UiKit.text(assetManager, font, "IMPACTO CONFIRMADO", 1.35f, ColorRGBA.Yellow);
        alertNode.attachChild(alertText);

        alertNode.setLocalTranslation(screenWidth * 0.5f, screenHeight * 0.74f, 12f);
        alertNode.setQueueBucket(RenderQueue.Bucket.Gui);
        alertNode.setCullHint(CullHint.Always);
        attachChild(alertNode);
    }

    private void buildPhraseCard() {
        phraseWidth = Math.min(780f, screenWidth - 80f);
        phraseHeight = 94f;
        phraseX = (screenWidth - phraseWidth) * 0.5f;
        phraseY = 92f;

        phraseNode = new Node("PhraseCard");
        phraseNode.attachChild(UiKit.panelAt(assetManager, phraseWidth, phraseHeight,
                new ColorRGBA(0.035f, 0.075f, 0.130f, 0.97f),
                new ColorRGBA(1.0f, 0.78f, 0.20f, 0.95f),
                phraseX, phraseY, 4f));

        // Franja dorada de comunicaciones.
        phraseNode.attachChild(UiKit.rect(assetManager, 5f, phraseHeight - 10f,
                new ColorRGBA(1.0f, 0.78f, 0.20f, 0.95f),
                phraseX + 6f, phraseY + 5f, 5f));

        // Icono de voz (bocadillo + ondas).
        float iconSize = 54f;
        Geometry icon = new Geometry("PhraseVoiceIcon", new Quad(iconSize, iconSize));
        Material iconMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        iconMat.setTexture("ColorMap", TextureGenerator.createVoiceIconTexture(96));
        iconMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        icon.setMaterial(iconMat);
        icon.setLocalTranslation(phraseX + 20f, phraseY + (phraseHeight - iconSize) * 0.5f, 5f);
        phraseNode.attachChild(icon);

        phraseTitle = UiKit.label(assetManager, font, "COMUNICADO DE GUERRA",
                0.52f, new ColorRGBA(1.0f, 0.78f, 0.20f, 0.95f),
                phraseX + 88f, phraseY + phraseHeight - 18f, 5f);
        phraseNode.attachChild(phraseTitle);

        phraseText = UiKit.text(assetManager, font, "", 1.0f, ColorRGBA.White);
        phraseText.setLocalTranslation(phraseX + 88f, phraseY + 30f, 5f);
        phraseNode.attachChild(phraseText);

        phraseNode.setQueueBucket(RenderQueue.Bucket.Gui);
        phraseNode.setCullHint(CullHint.Always);
        attachChild(phraseNode);
    }

    /** Muestra una frase de hundimiento en la tarjeta de comunicaciones. */
    public void showPhrase(String phrase) {
        String display = PhraseBank.toDisplayText(phrase);
        if (display.isEmpty()) {
            display = "...";
        }

        phraseText.setText(display);
        float maxWidth = phraseWidth - 88f - 24f;
        float scale = Math.min(1f, maxWidth / Math.max(1f, phraseText.getLineWidth()));
        phraseText.setLocalScale(scale);

        float textWidth = phraseText.getLineWidth() * scale;
        phraseText.setLocalTranslation(phraseX + 88f + (maxWidth - textWidth) * 0.5f,
                phraseY + 30f, 5f);
        phraseText.setColor(new ColorRGBA(1.0f, 0.94f, 0.74f, 1f));

        phraseTimer = 5.5f;
        phraseNode.setCullHint(CullHint.Never);
    }

    /** Actualiza la ronda actual y las rondas que faltan para la habilidad de dados. */
    public void setRoundInfo(int round, int untilDice) {
        if (round <= 0) {
            subtitleText.setText("SISTEMA DE COMANDO TACTICO");
            return;
        }
        String diceInfo = untilDice > 0 ? "DADOS EN " + untilDice : "DADOS LISTOS";
        subtitleText.setText("COMANDO TACTICO // RONDA " + round + " // " + diceInfo);
    }

    // ------------------------------------------------------------------
    //  ACTUALIZACION
    // ------------------------------------------------------------------

    public void update(float tpf) {
        pulseTime += tpf * 3.2f;
        if (alertTimer > 0) {
            alertTimer -= tpf;
            if (alertTimer <= 0) {
                alertNode.setCullHint(CullHint.Always);
            }
        }

        if (phraseTimer > 0f) {
            phraseTimer -= tpf;
            if (phraseTimer <= 0f) {
                phraseNode.setCullHint(CullHint.Always);
            } else {
                float pulse = 0.85f + 0.15f * FastMath.sin(phraseTimer * 6f);
                phraseTitle.setColor(new ColorRGBA(pulse, 0.78f * pulse, 0.20f * pulse, 0.95f));
            }
        }
    }

    public void showAlert(String message, ColorRGBA color, float duration) {
        alertText.setText(message);
        alertText.setColor(color);
        alertText.setLocalTranslation(-alertText.getLineWidth() * 0.5f,
                alertText.getLineHeight() * 0.4f, 1f);

        alertTimer = duration;
        alertNode.setCullHint(CullHint.Never);
    }

    public void updateStatusBanner(String text, ColorRGBA color) {
        statusBannerText.setText(text);
        statusBannerText.setColor(color);
        statusBannerText.setLocalTranslation(Math.max(310f, screenWidth - 20f - statusBannerText.getLineWidth()),
                screenHeight - 24f, 1f);
    }

    public void updateFleetStatus(Board playerBoard, Board enemyBoard, int shots, int hits) {
        List<Ship> playerShips = playerBoard.getShips();
        for (int i = 0; i < playerShips.size() && i < playerShipTexts.size(); i++) {
            Ship s = playerShips.get(i);
            BitmapText txt = playerShipTexts.get(i);

            int size = s.getType().getSize();
            int hp = s.getRemainingHealth();
            String bar = UiKit.hpBar(hp, size);

            if (!s.isPlaced()) {
                txt.setText(s.getType().getDisplayName().toUpperCase() + "  sin desplegar");
                txt.setColor(new ColorRGBA(0.52f, 0.56f, 0.60f, 1f));
            } else if (s.isSunk()) {
                txt.setText(s.getType().getDisplayName().toUpperCase() + " " + bar + " HUNDIDO");
                txt.setColor(new ColorRGBA(1.0f, 0.28f, 0.24f, 1f));
            } else if (s.getHits() > 0) {
                txt.setText(s.getType().getDisplayName().toUpperCase() + " " + bar + " " + hp + "/" + size);
                txt.setColor(new ColorRGBA(1.0f, 0.82f, 0.28f, 1f));
            } else {
                txt.setText(s.getType().getDisplayName().toUpperCase() + " " + bar);
                txt.setColor(new ColorRGBA(0.42f, 0.98f, 0.58f, 1f));
            }
        }

        List<Ship> enemyShips = enemyBoard.getShips();
        for (int i = 0; i < enemyShips.size() && i < enemyShipTexts.size(); i++) {
            Ship s = enemyShips.get(i);
            BitmapText txt = enemyShipTexts.get(i);

            if (s.isSunk()) {
                txt.setText("Contacto " + (i + 1) + ": HUNDIDO");
                txt.setColor(new ColorRGBA(1.0f, 0.42f, 0.28f, 1f));
            } else {
                txt.setText("Contacto " + (i + 1) + " [" + s.getType().getSize() + "]: desconocido");
                txt.setColor(new ColorRGBA(0.74f, 0.78f, 0.84f, 0.95f));
            }
        }

        float accuracy = shots > 0 ? (hits * 100f / shots) : 0f;
        statsText.setText(String.format("Tiros %d | Impactos %d | Precision %.1f%%", shots, hits, accuracy));
    }

    // ------------------------------------------------------------------
    //  INTERACCION
    // ------------------------------------------------------------------

    public boolean handleMouseClick(float mouseX, float mouseY) {
        for (GuiButton btn : buttons) {
            if (btn.isEnabled() && btn.onClick(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    public void handleMouseMove(float mouseX, float mouseY) {
        for (GuiButton btn : buttons) {
            btn.updateHover(mouseX, mouseY);
        }
    }

    public boolean isMouseOverGui(float mouseX, float mouseY) {
        if (mouseY >= screenHeight - TOP_BAR_H) return true;
        if (mouseY <= BOTTOM_ZONE_H) return true;

        float panelsY = screenHeight - TOP_BAR_H - 12f - PANEL_H;
        if (mouseY >= panelsY && mouseY <= panelsY + PANEL_H) {
            if (mouseX <= PANEL_MARGIN + PANEL_W_LEFT) return true;
            if (mouseX >= screenWidth - PANEL_MARGIN - PANEL_W_RIGHT) return true;
        }

        if (phraseNode != null && phraseNode.getCullHint() != CullHint.Always) {
            if (mouseX >= phraseX && mouseX <= phraseX + phraseWidth
                    && mouseY >= phraseY && mouseY <= phraseY + phraseHeight) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    //  API PARA EL CONTROLADOR / VENTANA PRINCIPAL
    // ------------------------------------------------------------------

    public void setAutoPlaceAction(Runnable action) {
        btnAutoPlace.setOnClickListener(action);
    }

    public void setRotateAction(Runnable action) {
        btnRotate.setOnClickListener(action);
    }

    public void setStartBattleAction(Runnable action) {
        btnStartBattle.setOnClickListener(action);
    }

    public void setRollDiceAction(Runnable action) {
        if (btnRollDice != null) {
            btnRollDice.setOnClickListener(action);
        }
    }

    public void setDiceButtonState(boolean available, int roundsUntil) {
        if (btnRollDice == null) return;
        if (placementBar.getCullHint() != CullHint.Always) {
            btnRollDice.setCullHint(CullHint.Always);
            btnRollDice.setEnabled(false);
            return;
        }

        btnRollDice.setCullHint(CullHint.Inherit);
        if (available) {
            btnRollDice.setText("🎲 LANZAR DADOS [X]");
            btnRollDice.setEnabled(true);
        } else if (roundsUntil > 0) {
            btnRollDice.setText("DADOS EN " + roundsUntil + " RONDAS");
            btnRollDice.setEnabled(false);
        } else {
            btnRollDice.setText("DADOS: YA USADOS");
            btnRollDice.setEnabled(false);
        }
    }

    public void setPauseAction(Runnable action) {
        btnPause.setOnClickListener(action);
    }

    public void setPlacementControlsEnabled(boolean enabled) {
        btnAutoPlace.setEnabled(enabled);
        btnRotate.setEnabled(enabled);
        placementBar.setCullHint(enabled ? CullHint.Inherit : CullHint.Always);
    }

    public void setStartBattleEnabled(boolean enabled) {
        btnStartBattle.setEnabled(enabled);
    }

    public void setRotationButtonText(boolean horizontal) {
        btnRotate.setText(horizontal ? "ROTAR: HORIZ [R]" : "ROTAR: VERT [R]");
    }

    public void setCameraLabel(String mode) {
        hintText.setText("[ESC] RESUMEN   |   CAMARA: " + mode.toUpperCase() + " [C]");
        hintText.setLocalTranslation(screenWidth - 20f - hintText.getLineWidth(),
                screenHeight - 44f, 1f);
    }

    public void setHintsVisible(boolean visible) {
        hintText.setCullHint(visible ? CullHint.Inherit : CullHint.Always);
    }

    public void setVisible(boolean visible) {
        setCullHint(visible ? CullHint.Inherit : CullHint.Always);
    }

    public boolean isHudVisible() {
        return getCullHint() != CullHint.Always;
    }
}
