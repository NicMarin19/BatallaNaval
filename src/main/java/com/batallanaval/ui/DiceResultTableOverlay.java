package com.batallanaval.ui;

import com.batallanaval.audio.SoundManager;
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
 * Tabla táctica de resultados de los dados y probabilidades aumentadas.
 * Se despliega al terminar de rodar los dados con la cámara colocada cerca de ellos,
 * mostrando de forma clara los números obtenidos y el incremento porcentual de las probabilidades.
 */
public class DiceResultTableOverlay extends Node {

    private static final float PANEL_W = 820f;
    private static final float PANEL_H = 500f;

    private final float screenWidth;
    private final float screenHeight;
    private final AssetManager assetManager;
    private final BitmapFont font;

    private final List<GuiButton> buttons = new ArrayList<>();
    private GuiButton btnContinue;
    private Runnable onContinueCallback;

    // Elementos dinámicos de texto
    private BitmapText titleText;
    private BitmapText die1BadgeText;
    private BitmapText die2BadgeText;
    private BitmapText totalBadgeText;

    // Celdas de la tabla
    private final List<BitmapText[]> tableRowTexts = new ArrayList<>();
    private BitmapText summaryCardTitle;
    private BitmapText summaryCardDetail;
    private Geometry summaryCardBorder;

    private float pulseTime = 0f;

    public DiceResultTableOverlay(float screenWidth, float screenHeight, AssetManager assetManager) {
        super("DiceResultTableOverlay");
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.assetManager = assetManager;
        this.font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        build();
        setQueueBucket(RenderQueue.Bucket.Gui);
        hide();
    }

    private void build() {
        float panelX = (screenWidth - PANEL_W) * 0.5f;
        float panelY = (screenHeight - PANEL_H) * 0.5f;
        float top = panelY + PANEL_H;

        // Velo translúcido que permite apreciar los dados 3D en el fondo
        Quad veilQ = new Quad(screenWidth, screenHeight);
        Geometry veil = new Geometry("DiceTableVeil", veilQ);
        Material matVeil = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matVeil.setColor("Color", new ColorRGBA(0.01f, 0.03f, 0.07f, 0.72f));
        matVeil.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        veil.setMaterial(matVeil);
        veil.setLocalTranslation(0f, 0f, 0f);
        attachChild(veil);

        // Panel principal con textura y borde dorado militar
        Quad panelQ = new Quad(PANEL_W, PANEL_H);
        Geometry panelBg = new Geometry("DiceTablePanelBg", panelQ);
        Material matPanel = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture panelTex = TextureGenerator.createButtonTexture((int) PANEL_W, (int) PANEL_H,
                new ColorRGBA(0.025f, 0.055f, 0.105f, 0.96f),
                new ColorRGBA(1.0f, 0.82f, 0.20f, 1.0f));
        matPanel.setTexture("ColorMap", panelTex);
        matPanel.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        panelBg.setMaterial(matPanel);
        panelBg.setLocalTranslation(panelX, panelY, 1f);
        attachChild(panelBg);

        // Cabecera
        titleText = UiKit.label(assetManager, font, "★ TELEMETRÍA DE TIRO // DADOS DE GUERRA ★",
                1.30f, new ColorRGBA(1.0f, 0.85f, 0.20f, 1f), panelX + 30f, top - 24f, 2f);
        attachChild(titleText);

        attachChild(UiKit.label(assetManager, font, "INFORME TÁCTICO DE NÚMEROS OBTENIDOS Y PROBABILIDADES AUMENTADAS",
                0.60f, new ColorRGBA(0.45f, 0.80f, 1.0f, 0.95f), panelX + 30f, top - 48f, 2f));

        attachChild(UiKit.hLine(assetManager, PANEL_W - 60f,
                new ColorRGBA(1.0f, 0.80f, 0.25f, 0.65f), panelX + 30f, top - 58f, 2f));

        // Insignias visuales de los dados arriba de la tabla
        float badgeY = top - 88f;
        die1BadgeText = UiKit.label(assetManager, font, "DADO 1: [ - ]", 0.90f,
                new ColorRGBA(0.35f, 0.95f, 1.0f, 1f), panelX + 40f, badgeY, 2f);
        attachChild(die1BadgeText);

        die2BadgeText = UiKit.label(assetManager, font, "DADO 2: [ - ]", 0.90f,
                new ColorRGBA(0.35f, 0.95f, 1.0f, 1f), panelX + 280f, badgeY, 2f);
        attachChild(die2BadgeText);

        totalBadgeText = UiKit.label(assetManager, font, "SUMA TOTAL: [ - / 12 ]", 1.0f,
                new ColorRGBA(1.0f, 0.88f, 0.25f, 1f), panelX + 520f, badgeY, 2f);
        attachChild(totalBadgeText);

        // Construir la tabla con columnas formateadas
        buildTableSection(panelX + 30f, top - 110f);

        // Tarjeta resumen inferior
        buildSummaryCard(panelX + 30f, panelY + 70f);

        // Botón de continuar
        float btnW = 320f;
        float btnH = 44f;
        btnContinue = new GuiButton("diceTableContinue", "CONTINUAR AL COMBATE", btnW, btnH,
                assetManager, font,
                new ColorRGBA(0.08f, 0.24f, 0.14f, 0.98f),
                new ColorRGBA(0.30f, 1.0f, 0.45f, 1.0f),
                this::onContinueClicked);
        btnContinue.setLocalTranslation(panelX + (PANEL_W - btnW) * 0.5f, panelY + 16f, 3f);
        attachChild(btnContinue);
        buttons.add(btnContinue);
    }

    private void buildTableSection(float x, float startY) {
        float tableW = PANEL_W - 60f;
        float headerH = 26f;

        // Fondo cabecera de tabla
        attachChild(UiKit.rect(assetManager, tableW, headerH,
                new ColorRGBA(0.04f, 0.12f, 0.22f, 0.95f), x, startY - headerH, 2f));
        attachChild(UiKit.hLine(assetManager, tableW,
                new ColorRGBA(0.30f, 0.75f, 1.0f, 0.80f), x, startY - headerH, 2.2f));

        // Títulos de columnas
        float colConceptX = x + 14f;
        float colPrevX = x + 250f;
        float colResultX = x + 410f;
        float colIncreaseX = x + 580f;

        attachChild(UiKit.label(assetManager, font, "PARÁMETRO / CONCEPTO", 0.65f,
                new ColorRGBA(0.55f, 0.90f, 1.0f, 1f), colConceptX, startY - 8f, 2.5f));
        attachChild(UiKit.label(assetManager, font, "VALOR BASE", 0.65f,
                new ColorRGBA(0.55f, 0.90f, 1.0f, 1f), colPrevX, startY - 8f, 2.5f));
        attachChild(UiKit.label(assetManager, font, "VALOR TIRO", 0.65f,
                new ColorRGBA(0.55f, 0.90f, 1.0f, 1f), colResultX, startY - 8f, 2.5f));
        attachChild(UiKit.label(assetManager, font, "INCREMENTO Y ESTADO", 0.65f,
                new ColorRGBA(0.55f, 0.90f, 1.0f, 1f), colIncreaseX, startY - 8f, 2.5f));

        // Filas de datos
        String[] concepts = {
                "Dado 1 (Babor)",
                "Dado 2 (Estribor)",
                "Puntuación Combinada",
                "Probabilidad Doble Tiro",
                "Habilidad de Fuego Extra",
                "Ciclo de Recarga"
        };

        float curY = startY - headerH - 24f;
        float rowH = 24f;

        for (int i = 0; i < concepts.length; i++) {
            // Fondo alternado para filas
            if (i % 2 == 1) {
                attachChild(UiKit.rect(assetManager, tableW, rowH,
                        new ColorRGBA(0.04f, 0.08f, 0.16f, 0.50f), x, curY - 5f, 1.8f));
            }

            BitmapText txtConcept = UiKit.label(assetManager, font, concepts[i], 0.64f,
                    new ColorRGBA(0.85f, 0.90f, 0.96f, 1f), colConceptX, curY + 12f, 2.5f);
            attachChild(txtConcept);

            BitmapText txtPrev = UiKit.label(assetManager, font, "-", 0.64f,
                    UiKit.TEXT_DIM, colPrevX, curY + 12f, 2.5f);
            attachChild(txtPrev);

            BitmapText txtResult = UiKit.label(assetManager, font, "-", 0.68f,
                    ColorRGBA.White, colResultX, curY + 12f, 2.5f);
            attachChild(txtResult);

            BitmapText txtIncrease = UiKit.label(assetManager, font, "-", 0.68f,
                    new ColorRGBA(0.35f, 1.0f, 0.45f, 1f), colIncreaseX, curY + 12f, 2.5f);
            attachChild(txtIncrease);

            tableRowTexts.add(new BitmapText[]{txtConcept, txtPrev, txtResult, txtIncrease});

            attachChild(UiKit.hLine(assetManager, tableW,
                    new ColorRGBA(0.18f, 0.40f, 0.65f, 0.25f), x, curY - 5f, 2.0f));

            curY -= rowH;
        }
    }

    private void buildSummaryCard(float x, float y) {
        float w = PANEL_W - 60f;
        float h = 64f;

        attachChild(UiKit.rect(assetManager, w, h,
                new ColorRGBA(0.02f, 0.08f, 0.14f, 0.90f), x, y, 2f));

        summaryCardBorder = UiKit.rect(assetManager, 5f, h,
                new ColorRGBA(1.0f, 0.85f, 0.20f, 1f), x, y, 2.5f);
        attachChild(summaryCardBorder);

        summaryCardTitle = UiKit.label(assetManager, font,
                "★ ANÁLISIS DE MEJORA: PROBABILIDAD DE COMBATE INCREMENTADA ★",
                0.68f, new ColorRGBA(1.0f, 0.85f, 0.20f, 1f), x + 16f, y + h - 14f, 2.5f);
        attachChild(summaryCardTitle);

        summaryCardDetail = UiKit.label(assetManager, font,
                "Fórmula: (Dado 1 + Dado 2) / 12  =>  Probabilidad ganada este turno.",
                0.60f, new ColorRGBA(0.75f, 0.90f, 1.0f, 0.95f), x + 16f, y + 22f, 2.5f);
        attachChild(summaryCardDetail);
    }

    /**
     * Muestra la tabla con los resultados exactos del tiro y el desglose de probabilidades.
     */
    public void showResults(int die1, int die2, double oldProb, double newProb,
                            boolean doubleShotArmed, int roundNumber, Runnable onContinue) {
        this.onContinueCallback = onContinue;

        int sum = die1 + die2;
        int oldPercent = (int) Math.round(oldProb * 100);
        int newPercent = (int) Math.round(newProb * 100);
        int increasePercent = newPercent - oldPercent;

        // Actualizar insignias superiores
        die1BadgeText.setText("DADO 1 (BABOR): [ " + die1 + " ]");
        die2BadgeText.setText("DADO 2 (ESTRIBOR): [ " + die2 + " ]");
        totalBadgeText.setText(String.format("SUMA TOTAL: [ %d / 12 ]", sum));

        // Fila 0: Dado 1
        tableRowTexts.get(0)[1].setText("1 (Mínimo)");
        tableRowTexts.get(0)[2].setText("Cara " + die1);
        tableRowTexts.get(0)[3].setText("+" + die1 + " pts de tiro");
        tableRowTexts.get(0)[3].setColor(new ColorRGBA(0.40f, 0.85f, 1.0f, 1f));

        // Fila 1: Dado 2
        tableRowTexts.get(1)[1].setText("1 (Mínimo)");
        tableRowTexts.get(1)[2].setText("Cara " + die2);
        tableRowTexts.get(1)[3].setText("+" + die2 + " pts de tiro");
        tableRowTexts.get(1)[3].setColor(new ColorRGBA(0.40f, 0.85f, 1.0f, 1f));

        // Fila 2: Puntuación Combinada
        tableRowTexts.get(2)[1].setText("2 / 12 (Base)");
        tableRowTexts.get(2)[2].setText(String.format("%d / 12", sum));
        tableRowTexts.get(2)[3].setText(String.format("%.1f%% del máximo", (sum / 12.0) * 100));
        tableRowTexts.get(2)[3].setColor(new ColorRGBA(1.0f, 0.85f, 0.25f, 1f));

        // Fila 3: Probabilidad Doble Tiro
        tableRowTexts.get(3)[1].setText(oldPercent + "%");
        tableRowTexts.get(3)[2].setText(newPercent + "%");
        tableRowTexts.get(3)[3].setText("+" + newPercent + "% AUMENTO");
        tableRowTexts.get(3)[3].setColor(new ColorRGBA(0.35f, 1.0f, 0.45f, 1f));

        // Fila 4: Habilidad Fuego Extra
        tableRowTexts.get(4)[1].setText("Inactiva");
        if (doubleShotArmed) {
            tableRowTexts.get(4)[2].setText("¡ARMADO / ÉXITO!");
            tableRowTexts.get(4)[2].setColor(new ColorRGBA(0.30f, 1.0f, 0.45f, 1f));
            tableRowTexts.get(4)[3].setText("✓ Disparo extra al fallar");
            tableRowTexts.get(4)[3].setColor(new ColorRGBA(0.30f, 1.0f, 0.45f, 1f));
        } else {
            tableRowTexts.get(4)[2].setText("NO ACTIVADO (" + newPercent + "%)");
            tableRowTexts.get(4)[2].setColor(new ColorRGBA(0.80f, 0.85f, 0.90f, 1f));
            tableRowTexts.get(4)[3].setText("Sin tiro adicional este turno");
            tableRowTexts.get(4)[3].setColor(new ColorRGBA(0.70f, 0.75f, 0.80f, 1f));
        }

        // Fila 5: Ciclo de Recarga
        tableRowTexts.get(5)[1].setText("Lista");
        tableRowTexts.get(5)[2].setText("Ejecutada");
        tableRowTexts.get(5)[3].setText("Próxima tirada en 8 rondas");
        tableRowTexts.get(5)[3].setColor(UiKit.TEXT_DIM);

        // Tarjeta resumen
        if (doubleShotArmed) {
            summaryCardTitle.setText("★ ¡DOBLE DISPARO DESBLOQUEADO Y ARMADO AL " + newPercent + "%! ★");
            summaryCardTitle.setColor(new ColorRGBA(0.35f, 1.0f, 0.45f, 1f));
            summaryCardDetail.setText(String.format("Tirada: %d + %d = %d. Aumento de probabilidad: +%d%%. ¡Conservarás un disparo extra si fallas!",
                    die1, die2, sum, newPercent));
        } else {
            summaryCardTitle.setText("★ TIRADA DE DADOS: +" + newPercent + "% DE PROBABILIDAD ★");
            summaryCardTitle.setColor(new ColorRGBA(1.0f, 0.85f, 0.25f, 1f));
            summaryCardDetail.setText(String.format("Tirada: %d + %d = %d (Aumento: +%d%%). En esta ocasión no se activó el disparo extra.",
                    die1, die2, sum, newPercent));
        }

        setCullHint(CullHint.Never);
        SoundManager.getInstance().play(SoundManager.SoundType.HIT_ALERT);
    }

    private void onContinueClicked() {
        hide();
        SoundManager.getInstance().play(SoundManager.SoundType.BUTTON_CLICK);
        if (onContinueCallback != null) {
            onContinueCallback.run();
        }
    }

    public void update(float tpf) {
        if (!isVisible()) return;
        pulseTime += tpf * 3.0f;
        float pulse = 0.88f + 0.12f * FastMath.sin(pulseTime);
        titleText.setColor(new ColorRGBA(1.0f * pulse, 0.85f * pulse, 0.20f * pulse, 1f));
    }

    public boolean handleMouseClick(float x, float y) {
        if (!isVisible()) return false;
        for (GuiButton btn : buttons) {
            if (btn.onClick(x, y)) {
                return true;
            }
        }
        return true; // Modal: consume clics
    }

    public void handleMouseMove(float x, float y) {
        if (!isVisible()) return;
        for (GuiButton btn : buttons) {
            btn.updateHover(x, y);
        }
    }

    public void hide() {
        setCullHint(CullHint.Always);
    }

    public boolean isVisible() {
        return getCullHint() != CullHint.Always;
    }
}
