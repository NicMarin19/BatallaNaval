package com.batallanaval.ui;

import com.batallanaval.view.TextureGenerator;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de inicio. Presenta la escena 3D de fondo y un panel de comando
 * con: Nueva Partida, Reanudar Partida, Ajustes, Creditos y Salir.
 */
public class MainMenuScreen extends Node {

    private static final float PANEL_X = 100f;
    private static final float PANEL_Y = 86f;
    private static final float PANEL_W = 450f;
    private static final float PANEL_H = 548f;

    private final float screenWidth;
    private final float screenHeight;
    private final AssetManager assetManager;
    private final BitmapFont font;
    private final GameSettings settings;

    private final List<GuiButton> buttons = new ArrayList<>();
    private final SettingsPanel settingsPanel;
    private final Node contentNode = new Node("MenuContent");

    private Runnable onNewGame;
    private Runnable onResume;
    private Runnable onQuit;

    private GuiButton btnResume;
    private BitmapText statusText;

    private Node creditsNode;
    private boolean creditsVisible = false;

    private BitmapText titleLine1;
    private BitmapText titleLine2;
    private Node radarNode;
    private float pulseTime = 0f;
    private float radarAngle = 0f;
    private static final float RADAR_SIZE = 320f;

    public MainMenuScreen(float screenWidth, float screenHeight,
                          AssetManager assetManager, GameSettings settings) {
        super("MainMenuScreen");
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.assetManager = assetManager;
        this.settings = settings;
        this.font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        buildBackdrop();
        buildRadarDecoration();
        buildCommandPanel();
        attachChild(contentNode);
        buildCreditsPanel();

        settingsPanel = new SettingsPanel(assetManager, font, settings, 540f, 340f);
        settingsPanel.setLocalTranslation(screenWidth * 0.5f, screenHeight * 0.5f, 30f);
        settingsPanel.setOnChange(settingsPanel::refreshLabels);
        settingsPanel.setOnClose(this::updateContentVisibility);
        attachChild(settingsPanel);

        setQueueBucket(RenderQueue.Bucket.Gui);
    }

    // ------------------------------------------------------------------
    //  CONSTRUCCION
    // ------------------------------------------------------------------

    private void buildBackdrop() {
        Quad veil = new Quad(screenWidth, screenHeight);
        Geometry veilGeom = new Geometry("MenuVeil", veil);
        Material matVeil = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matVeil.setColor("Color", new ColorRGBA(0.008f, 0.030f, 0.060f, 0.35f));
        matVeil.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        veilGeom.setMaterial(matVeil);
        contentNode.attachChild(veilGeom);

        // Degradado lateral: oscurece la izquierda (panel) y deja ver el mar a la derecha.
        Quad gradient = new Quad(screenWidth, screenHeight);
        Geometry gradientGeom = new Geometry("MenuGradient", gradient);
        Material matGrad = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture gradTex = TextureGenerator.createHorizontalGradientTexture(512, 64,
                new ColorRGBA(0.010f, 0.028f, 0.055f, 0.95f),
                new ColorRGBA(0.010f, 0.028f, 0.055f, 0.02f));
        matGrad.setTexture("ColorMap", gradTex);
        matGrad.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        gradientGeom.setMaterial(matGrad);
        gradientGeom.setLocalTranslation(0, 0, 0.2f);
        contentNode.attachChild(gradientGeom);

        // Barras de acento superior e inferior
        contentNode.attachChild(UiKit.rect(assetManager, screenWidth, 2f,
                new ColorRGBA(0.25f, 0.65f, 1f, 0.45f), 0, screenHeight - 2f, 0.5f));
        contentNode.attachChild(UiKit.rect(assetManager, screenWidth, 2f,
                new ColorRGBA(0.25f, 0.65f, 1f, 0.45f), 0, 0f, 0.5f));

        // Version
        contentNode.attachChild(UiKit.label(assetManager, font,
                "v1.1.0  |  jMonkeyEngine 3.7",
                0.62f, new ColorRGBA(0.42f, 0.55f, 0.66f, 0.9f), 16f, 26f, 1f));

        // Pista inferior derecha
        contentNode.attachChild(UiKit.labelRight(assetManager, font,
                "ESC: PAUSA / RESUMEN DE PARTIDA",
                0.62f, new ColorRGBA(0.42f, 0.62f, 0.78f, 0.9f),
                screenWidth - 18f, 26f, 1f));
    }

    private void buildRadarDecoration() {
        float size = RADAR_SIZE;
        float x = screenWidth - size - 96f;
        float y = 168f;

        radarNode = new Node("MenuRadarNode");
        radarNode.setLocalTranslation(x + size * 0.5f, y + size * 0.5f, 1f);

        Quad q = new Quad(size, size);
        Geometry radarGeometry = new Geometry("MenuRadar", q);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", TextureGenerator.createRadarTexture(256));
        mat.setColor("Color", new ColorRGBA(0.62f, 0.88f, 1f, 0.62f));
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        radarGeometry.setMaterial(mat);
        radarGeometry.setLocalTranslation(-size * 0.5f, -size * 0.5f, 0f);
        radarGeometry.setQueueBucket(RenderQueue.Bucket.Gui);
        radarNode.attachChild(radarGeometry);
        contentNode.attachChild(radarNode);

        contentNode.attachChild(UiKit.labelCentered(assetManager, font,
                "MAPEO TACTICO // EN ESPERA",
                0.62f, new ColorRGBA(0.35f, 0.62f, 0.80f, 0.85f),
                x + size * 0.5f, y - 14f, 1f));
    }

    private void buildCommandPanel() {
        // Panel de mando
        contentNode.attachChild(UiKit.panelAt(assetManager, PANEL_W, PANEL_H,
                new ColorRGBA(0.020f, 0.055f, 0.100f, 0.93f),
                new ColorRGBA(0.30f, 0.75f, 1.00f, 0.95f),
                PANEL_X, PANEL_Y, 2f));

        float left = PANEL_X + 30f;
        float top = PANEL_Y + PANEL_H;

        contentNode.attachChild(UiKit.label(assetManager, font, "// SIMULADOR TACTICO NAVAL",
                0.66f, new ColorRGBA(0.42f, 0.78f, 1f, 0.9f), left, top - 30f, 3f));

        titleLine1 = UiKit.label(assetManager, font, "BATALLA", 2.20f,
                new ColorRGBA(0.55f, 0.92f, 1f, 1f), left, top - 86f, 3f);
        contentNode.attachChild(titleLine1);

        titleLine2 = UiKit.label(assetManager, font, "NAVAL 3D", 2.20f,
                new ColorRGBA(0.30f, 0.78f, 1f, 1f), left, top - 140f, 3f);
        contentNode.attachChild(titleLine2);

        contentNode.attachChild(UiKit.hLine(assetManager, PANEL_W - 60f,
                new ColorRGBA(0.25f, 0.62f, 0.92f, 0.55f),
                left, top - 158f, 3f));

        contentNode.attachChild(UiKit.label(assetManager, font,
                "COMANDO LA FLOTA. DOMINA EL OCEANO.",
                0.60f, new ColorRGBA(0.62f, 0.72f, 0.82f, 0.95f), left, top - 180f, 3f));

        // Botones
        float btnW = PANEL_W - 60f;
        float btnH = 48f;
        float btnX = left;
        float rowTop = top - 246f;

        GuiButton btnNew = new GuiButton("mmNew", "NUEVA PARTIDA", btnW, btnH,
                assetManager, font,
                new ColorRGBA(0.06f, 0.20f, 0.34f, 0.97f),
                new ColorRGBA(0.28f, 0.80f, 1.0f, 1f),
                () -> { if (onNewGame != null) onNewGame.run(); });
        btnNew.setLocalTranslation(btnX, rowTop - btnH, 3f);
        contentNode.attachChild(btnNew);
        buttons.add(btnNew);

        btnResume = new GuiButton("mmResume", "REANUDAR (CARPETA)", btnW, btnH,
                assetManager, font,
                new ColorRGBA(0.05f, 0.16f, 0.12f, 0.95f),
                new ColorRGBA(0.30f, 0.95f, 0.55f, 1f),
                () -> { if (onResume != null) onResume.run(); });
        btnResume.setLocalTranslation(btnX, rowTop - btnH - 62f, 3f);
        btnResume.setEnabled(false);
        contentNode.attachChild(btnResume);
        buttons.add(btnResume);

        GuiButton btnSettings = new GuiButton("mmSettings", "AJUSTES", btnW, btnH,
                assetManager, font,
                UiKit.BTN_BG, UiKit.BTN_BORDER,
                this::openSettings);
        btnSettings.setLocalTranslation(btnX, rowTop - btnH - 124f, 3f);
        contentNode.attachChild(btnSettings);
        buttons.add(btnSettings);

        GuiButton btnCredits = new GuiButton("mmCredits", "CREDITOS", btnW, btnH,
                assetManager, font,
                UiKit.BTN_BG, UiKit.BTN_BORDER,
                this::toggleCredits);
        btnCredits.setLocalTranslation(btnX, rowTop - btnH - 186f, 3f);
        contentNode.attachChild(btnCredits);
        buttons.add(btnCredits);

        GuiButton btnQuit = new GuiButton("mmQuit", "SALIR DEL JUEGO", btnW, btnH,
                assetManager, font,
                UiKit.BTN_RED_BG, UiKit.BTN_RED_BD,
                () -> { if (onQuit != null) onQuit.run(); });
        btnQuit.setLocalTranslation(btnX, rowTop - btnH - 248f, 3f);
        contentNode.attachChild(btnQuit);
        buttons.add(btnQuit);

        statusText = UiKit.label(assetManager, font, "ESTADO: SIN PARTIDA ACTIVA",
                0.60f, new ColorRGBA(0.55f, 0.65f, 0.75f, 0.9f),
                PANEL_X, PANEL_Y - 26f, 3f);
        contentNode.attachChild(statusText);
    }

    private void buildCreditsPanel() {
        creditsNode = new Node("CreditsPanel");

        float w = 580f;
        float h = 360f;
        float x = (screenWidth - w) * 0.5f;
        float y = (screenHeight - h) * 0.5f;

        creditsNode.attachChild(UiKit.panelAt(assetManager, w, h,
                new ColorRGBA(0.020f, 0.055f, 0.100f, 1f),
                new ColorRGBA(0.30f, 0.78f, 1.00f, 1f), x, y, 20f));

        creditsNode.attachChild(UiKit.label(assetManager, font, "CREDITOS", 1.5f,
                UiKit.ACCENT, x + 28f, y + h - 26f, 21f));
        creditsNode.attachChild(UiKit.label(assetManager, font,
                "BATALLA NAVAL 3D // SISTEMA DE COMANDO TACTICO",
                0.62f, UiKit.TEXT_DIM, x + 28f, y + h - 54f, 21f));
        creditsNode.attachChild(UiKit.hLine(assetManager, w - 56f,
                new ColorRGBA(0.25f, 0.62f, 0.92f, 0.55f),
                x + 28f, y + h - 66f, 21f));

        String[] lines = {
                "Desarrollado con jMonkeyEngine 3.7 y Java 17",
                "",
                "Diseno y programacion: Equipo Naval",
                "Modelos 3D: construccion procedural por primitivas",
                "Texturas y agua: generacion procedural en tiempo real",
                "Audio: sintesis PCM procedural (javax.sound)",
                "",
                "17 celdas de flota por bando  //  IA con busqueda y objetivo",
                "",
                "Gracias por jugar."
        };

        float cy = y + h - 98f;
        for (String line : lines) {
            ColorRGBA color = line.isEmpty()
                    ? UiKit.TEXT_DIM
                    : (line.startsWith("Desarrollado") ? UiKit.ACCENT : UiKit.TEXT);
            creditsNode.attachChild(UiKit.label(assetManager, font, line,
                    line.startsWith("Desarrollado") ? 0.76f : 0.68f,
                    color, x + 30f, cy, 21f));
            cy -= 20f;
        }

        GuiButton btnClose = new GuiButton("creditsClose", "CERRAR", 180f, 42f,
                assetManager, font, UiKit.BTN_BG, UiKit.BTN_BORDER, this::toggleCredits);
        btnClose.setLocalTranslation(x + (w - 180f) * 0.5f, y + 24f, 21f);
        creditsNode.attachChild(btnClose);

        creditsNode.setCullHint(CullHint.Always);
        attachChild(creditsNode);
    }

    // ------------------------------------------------------------------
    //  INTERACCION
    // ------------------------------------------------------------------

    public void update(float tpf) {
        pulseTime += tpf * 1.6f;
        float pulse = 0.90f + 0.10f * FastMath.sin(pulseTime);
        titleLine2.setColor(new ColorRGBA(0.30f * pulse, 0.78f * pulse, pulse, 1f));

        // Rotacion lenta del radar decorativo
        radarAngle += tpf * 0.45f;
        radarNode.setLocalRotation(new Quaternion().fromAngleAxis(radarAngle, Vector3f.UNIT_Z));
    }

    public boolean handleMouseClick(float x, float y) {
        if (!isVisible()) return false;

        if (settingsPanel.isVisible()) {
            settingsPanel.handleMouseClick(x, y);
            return true; // modal: consume siempre
        }

        if (creditsVisible) {
            for (com.jme3.scene.Spatial child : creditsNode.getChildren()) {
                if (child instanceof GuiButton) {
                    ((GuiButton) child).onClick(x, y);
                }
            }
            // Clic fuera del panel cierra los creditos
            if (!insideCreditsPanel(x, y)) toggleCredits();
            return true;
        }

        for (GuiButton btn : buttons) {
            if (btn.onClick(x, y)) return true;
        }
        return false;
    }

    private boolean insideCreditsPanel(float x, float y) {
        float w = 580f;
        float h = 360f;
        float px = (screenWidth - w) * 0.5f;
        float py = (screenHeight - h) * 0.5f;
        return x >= px && x <= px + w && y >= py && y <= py + h;
    }

    public void handleMouseMove(float x, float y) {
        if (!isVisible()) return;
        if (settingsPanel.isVisible()) {
            settingsPanel.handleMouseMove(x, y);
            return;
        }
        if (creditsVisible) {
            for (com.jme3.scene.Spatial child : creditsNode.getChildren()) {
                if (child instanceof GuiButton) {
                    ((GuiButton) child).updateHover(x, y);
                }
            }
            return;
        }
        for (GuiButton btn : buttons) {
            btn.updateHover(x, y);
        }
    }

    /** @return true si ESC fue consumido (cerrando ajustes/creditos). */
    public boolean handleEscape() {
        if (!isVisible()) return false;
        if (settingsPanel.isVisible()) {
            settingsPanel.hide();
            updateContentVisibility();
            return true;
        }
        if (creditsVisible) {
            toggleCredits();
            return true;
        }
        return false;
    }

    private void openSettings() {
        creditsVisible = false;
        creditsNode.setCullHint(CullHint.Always);
        settingsPanel.show();
        updateContentVisibility();
    }

    private void toggleCredits() {
        if (settingsPanel.isVisible()) settingsPanel.hide();
        creditsVisible = !creditsVisible;
        creditsNode.setCullHint(creditsVisible ? CullHint.Inherit : CullHint.Always);
        updateContentVisibility();
    }

    /** Oculta el menu de fondo mientras hay un panel modal abierto. */
    private void updateContentVisibility() {
        boolean modal = creditsVisible || settingsPanel.isVisible();
        contentNode.setCullHint(modal ? CullHint.Always : CullHint.Inherit);
    }

    public void setOnNewGame(Runnable action) {
        this.onNewGame = action;
    }

    public void setOnResume(Runnable action) {
        this.onResume = action;
    }

    public void setOnQuit(Runnable action) {
        this.onQuit = action;
    }

    public void setResumeEnabled(boolean enabled) {
        btnResume.setEnabled(enabled);
    }

    public void setGameStatus(String status) {
        statusText.setText("ESTADO: " + status);
    }

    public void updateFolderStatus(int savedCount, boolean gameInProgress, String stateLabel) {
        if (gameInProgress) {
            setResumeEnabled(true);
            setGameStatus("EN CURSO (" + stateLabel + ") | " + savedCount + " GUARDADA(S)");
        } else if (savedCount > 0) {
            setResumeEnabled(true);
            setGameStatus(savedCount + " PARTIDA(S) EN CARPETA 'partidas'");
        } else {
            setResumeEnabled(false);
            setGameStatus("SIN PARTIDA ACTIVA (Carpeta vacía)");
        }
    }

    public void show() {
        setCullHint(CullHint.Inherit);
        creditsVisible = false;
        creditsNode.setCullHint(CullHint.Always);
        settingsPanel.hide();
        updateContentVisibility();
    }

    public void hide() {
        setCullHint(CullHint.Always);
        creditsVisible = false;
        creditsNode.setCullHint(CullHint.Always);
        settingsPanel.hide();
        updateContentVisibility();
    }

    public boolean isVisible() {
        return getCullHint() != CullHint.Always;
    }
}
