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
import java.util.Random;
import java.util.function.Consumer;

/**
 * Overlay de lanzamiento de dados.
 * Aparece cada 8 rondas completas. El jugador lanza 2 dados y la suma
 * determina la probabilidad (sum/12) de disparar dos veces en el siguiente turno.
 */
public class DiceRollOverlay extends Node {

    private final float screenWidth;
    private final float screenHeight;
    private final AssetManager assetManager;
    private final BitmapFont font;
    private final Random rng = new Random();
    private final List<GuiButton> buttons = new ArrayList<>();
    private final Material[] dieFaceMaterials = new Material[7];

    // Dice state
    private int die1 = 1;
    private int die2 = 1;
    private boolean rolled = false;

    // Animation
    private float rollAnimTimer = 0f;
    private boolean isAnimating = false;
    private static final float ANIM_DURATION = 1.4f;

    // Result callback
    private Consumer<Double> onRollComplete; // receives probability 0.0–1.0

    // Die face displays
    private Geometry die1Geom;
    private Geometry die2Geom;
    private float die1BaseX;
    private float die2BaseX;
    private float dieBaseY;
    private float dieSize;

    private BitmapText resultText;
    private BitmapText probText;
    private GuiButton rollButton;

    // Upgrade animation
    private boolean isUpgradeAnimating = false;
    private float upgradeAnimTimer = 0f;
    private int targetProbPercent = 0;
    private float panelX;
    private float panelY;
    private float panelW;
    private float panelH;

    private Node upgradeEffectNode;
    private Geometry aura1Geom;
    private Geometry aura2Geom;
    private Material auraMat1;
    private Material auraMat2;
    private Geometry[] starGeoms;
    private BitmapText upgradeBannerText;
    private Geometry barBgGeom;
    private Geometry barFillGeom;
    private float maxBarWidth = 460f;

    public DiceRollOverlay(float screenWidth, float screenHeight, AssetManager assetManager) {
        super("DiceRollOverlay");
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.assetManager = assetManager;
        this.font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        for (int face = 1; face <= 6; face++) {
            dieFaceMaterials[face] = createDieFaceMaterial(face);
        }

        buildDimOverlay();
        buildPanel();
        setQueueBucket(RenderQueue.Bucket.Gui);
        hide();
    }

    private void buildDimOverlay() {
        Quad bg = new Quad(screenWidth, screenHeight);
        Geometry bgG = new Geometry("DiceDim", bg);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0f, 0.02f, 0.08f, 0.82f));
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        bgG.setMaterial(mat);
        bgG.setLocalTranslation(0, 0, 0);
        attachChild(bgG);
    }

    private void buildPanel() {
        this.panelW = 500f;
        this.panelH = 400f;
        this.panelX = (screenWidth - panelW) * 0.5f;
        this.panelY = (screenHeight - panelH) * 0.5f;

        // Panel background
        Quad panelQ = new Quad(panelW, panelH);
        Geometry panelG = new Geometry("DicePanelBg", panelQ);
        Material matP = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture panelTex = TextureGenerator.createButtonTexture((int) panelW, (int) panelH,
                new ColorRGBA(0.04f, 0.08f, 0.16f, 0.98f),
                new ColorRGBA(1.0f, 0.75f, 0.1f, 1f));
        matP.setTexture("ColorMap", panelTex);
        matP.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        panelG.setMaterial(matP);
        panelG.setLocalTranslation(panelX, panelY, 1f);
        attachChild(panelG);

        // Title
        BitmapText title = new BitmapText(font, false);
        title.setSize(font.getCharSet().getRenderedSize() * 1.5f);
        title.setText("HABILIDAD ESPECIAL: DADOS DE GUERRA");
        title.setColor(new ColorRGBA(1.0f, 0.80f, 0.1f, 1f));
        float tw = title.getLineWidth();
        float titleScale = Math.min(1f, (panelW - 20f) / tw);
        title.setLocalScale(titleScale);
        title.setLocalTranslation(panelX + (panelW - tw * titleScale) * 0.5f,
                panelY + panelH - 24f, 2f);
        attachChild(title);

        // Description
        BitmapText desc = new BitmapText(font, false);
        desc.setSize(font.getCharSet().getRenderedSize() * 0.70f);
        desc.setText("Cada 8 rondas: lanza los dados y suma sus caras.");
        desc.setColor(new ColorRGBA(0.85f, 0.90f, 0.95f, 0.90f));
        float dw = desc.getLineWidth();
        desc.setLocalTranslation(panelX + (panelW - dw) * 0.5f, panelY + panelH - 58f, 2f);
        attachChild(desc);

        BitmapText formula = new BitmapText(font, false);
        formula.setSize(font.getCharSet().getRenderedSize() * 0.58f);
        formula.setText("La suma / 12 es la probabilidad de doble disparo en este turno.");
        formula.setColor(new ColorRGBA(0.55f, 0.85f, 1.0f, 0.90f));
        float fw = formula.getLineWidth();
        formula.setLocalTranslation(panelX + (panelW - fw) * 0.5f, panelY + panelH - 78f, 2f);
        attachChild(formula);

        // Die coordinates
        dieSize = 90f;
        die1BaseX = panelX + panelW * 0.25f - dieSize * 0.5f;
        die2BaseX = panelX + panelW * 0.75f - dieSize * 0.5f;
        dieBaseY = panelY + panelH - 182f;

        // Die 1 (left)
        die1Geom = buildDieFace(die1BaseX, dieBaseY, dieSize, 1);
        attachChild(die1Geom);

        // Die 2 (right)
        die2Geom = buildDieFace(die2BaseX, dieBaseY, dieSize, 1);
        attachChild(die2Geom);

        // Die labels
        addLabel("+", panelX + panelW * 0.5f - 8f, dieBaseY + dieSize * 0.5f + 8f,
                new ColorRGBA(1f, 0.8f, 0.2f, 1f), 2.0f);

        buildUpgradeEffects();

        // Result text
        resultText = new BitmapText(font, false);
        resultText.setSize(font.getCharSet().getRenderedSize() * 1.0f);
        resultText.setText("Pulsa el boton para lanzar los dados");
        resultText.setColor(new ColorRGBA(0.9f, 0.85f, 0.5f, 1f));
        resultText.setLocalTranslation(panelX + 20f, panelY + 128f, 3f);
        attachChild(resultText);

        probText = new BitmapText(font, false);
        probText.setSize(font.getCharSet().getRenderedSize() * 1.1f);
        probText.setText("");
        probText.setColor(new ColorRGBA(0.3f, 1.0f, 0.4f, 1f));
        probText.setLocalTranslation(panelX + 20f, panelY + 104f, 3f);
        attachChild(probText);

        // Roll button
        float btnW = 260f;
        float btnH = 46f;
        rollButton = new GuiButton("diceRoll", "LANZAR DADOS", btnW, btnH,
                assetManager, font,
                new ColorRGBA(0.14f, 0.10f, 0.02f, 0.95f),
                new ColorRGBA(1.0f, 0.80f, 0.1f, 1f),
                () -> startRoll());
        rollButton.setLocalTranslation(panelX + (panelW - btnW) * 0.5f, panelY + 14f, 3f);
        buttons.add(rollButton);
        attachChild(rollButton);
    }

    private void buildUpgradeEffects() {
        upgradeEffectNode = new Node("UpgradeEffectNode");

        // Aura shockwave 1 (under Die 1)
        float auraSize = 130f;
        Quad auraQ = new Quad(auraSize, auraSize);
        aura1Geom = new Geometry("Aura1", auraQ);
        auraMat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture auraTex = TextureGenerator.createAuraRingTexture(128);
        auraMat1.setTexture("ColorMap", auraTex);
        auraMat1.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);
        aura1Geom.setMaterial(auraMat1);
        aura1Geom.setLocalTranslation(die1BaseX + (dieSize - auraSize) * 0.5f, dieBaseY + (dieSize - auraSize) * 0.5f, 1.5f);
        upgradeEffectNode.attachChild(aura1Geom);

        // Aura shockwave 2 (under Die 2)
        aura2Geom = new Geometry("Aura2", auraQ);
        auraMat2 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        auraMat2.setTexture("ColorMap", auraTex);
        auraMat2.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);
        aura2Geom.setMaterial(auraMat2);
        aura2Geom.setLocalTranslation(die2BaseX + (dieSize - auraSize) * 0.5f, dieBaseY + (dieSize - auraSize) * 0.5f, 1.5f);
        upgradeEffectNode.attachChild(aura2Geom);

        // Sparkle stars around dice
        float starSize = 36f;
        Quad starQ = new Quad(starSize, starSize);
        Texture starTex = TextureGenerator.createUpgradeStarTexture(64);
        Material starMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        starMat.setTexture("ColorMap", starTex);
        starMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);

        starGeoms = new Geometry[4];
        float[][] starOffsets = {{-22f, 70f}, {dieSize + 4f, 70f}, {-20f, -15f}, {dieSize + 6f, -15f}};
        for (int i = 0; i < 4; i++) {
            starGeoms[i] = new Geometry("Star_" + i, starQ);
            starGeoms[i].setMaterial(starMat);
            float bx = (i < 2) ? die1BaseX : die2BaseX;
            starGeoms[i].setLocalTranslation(bx + starOffsets[i][0], dieBaseY + starOffsets[i][1], 2.5f);
            upgradeEffectNode.attachChild(starGeoms[i]);
        }

        // Banner text for upgrade
        upgradeBannerText = new BitmapText(font, false);
        upgradeBannerText.setSize(font.getCharSet().getRenderedSize() * 1.15f);
        upgradeBannerText.setText("★ ¡MEJORA DE GUERRA DESBLOQUEADA! ★");
        upgradeBannerText.setColor(new ColorRGBA(1.0f, 0.85f, 0.15f, 1f));
        float bw = upgradeBannerText.getLineWidth();
        upgradeBannerText.setLocalTranslation(panelX + (panelW - bw) * 0.5f, panelY + panelH - 96f, 3f);
        upgradeEffectNode.attachChild(upgradeBannerText);

        // Progress bar background & fill
        maxBarWidth = panelW - 40f;
        Quad barBgQ = new Quad(maxBarWidth, 12f);
        barBgGeom = new Geometry("BarBg", barBgQ);
        Material barBgMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        barBgMat.setColor("Color", new ColorRGBA(0.1f, 0.15f, 0.22f, 0.9f));
        barBgGeom.setMaterial(barBgMat);
        barBgGeom.setLocalTranslation(panelX + 20f, panelY + 68f, 2.5f);
        upgradeEffectNode.attachChild(barBgGeom);

        Quad barFillQ = new Quad(1f, 12f);
        barFillGeom = new Geometry("BarFill", barFillQ);
        Material barFillMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        barFillMat.setColor("Color", new ColorRGBA(1.0f, 0.82f, 0.18f, 1.0f));
        barFillGeom.setMaterial(barFillMat);
        barFillGeom.setLocalTranslation(panelX + 20f, panelY + 68f, 2.8f);
        upgradeEffectNode.attachChild(barFillGeom);

        upgradeEffectNode.setCullHint(CullHint.Always);
        attachChild(upgradeEffectNode);
    }

    private Geometry buildDieFace(float x, float y, float size, int face) {
        Quad q = new Quad(size, size);
        Geometry g = new Geometry("DieFace", q);
        g.setMaterial(dieFaceMaterials[face]);
        g.setLocalTranslation(x, y, 2f);
        return g;
    }

    private Material createDieFaceMaterial(int face) {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture tex = TextureGenerator.createDiceFaceTexture(face, 90);
        mat.setTexture("ColorMap", tex);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        return mat;
    }

    private void addLabel(String text, float x, float y, ColorRGBA color, float scale) {
        BitmapText t = new BitmapText(font, false);
        t.setSize(font.getCharSet().getRenderedSize() * scale);
        t.setText(text);
        t.setColor(color);
        t.setLocalTranslation(x, y, 2f);
        attachChild(t);
    }

    private void startRoll() {
        if (isAnimating) return;
        rolled = false;
        isAnimating = true;
        rollAnimTimer = 0f;
        isUpgradeAnimating = false;
        upgradeAnimTimer = 0f;
        if (upgradeEffectNode != null) {
            upgradeEffectNode.setCullHint(CullHint.Always);
        }
        resetDiceTransforms();
        rollButton.setEnabled(false);
        probText.setText("");
        resultText.setText("Lanzando dados de guerra...");
        SoundManager.getInstance().play(SoundManager.SoundType.BUTTON_CLICK);
    }

    private void resetDiceTransforms() {
        die1Geom.setLocalScale(1f);
        die2Geom.setLocalScale(1f);
        die1Geom.setLocalTranslation(die1BaseX, dieBaseY, 2f);
        die2Geom.setLocalTranslation(die2BaseX, dieBaseY, 2f);
    }

    public void update(float tpf) {
        if (isAnimating) {
            rollAnimTimer += tpf;

            // Rapidly cycle random faces during animation
            int tempDie1 = rng.nextInt(6) + 1;
            int tempDie2 = rng.nextInt(6) + 1;
            updateDieFace(die1Geom, tempDie1);
            updateDieFace(die2Geom, tempDie2);

            // Subtle vibration during roll
            float jitterX = (rng.nextFloat() - 0.5f) * 4f;
            float jitterY = (rng.nextFloat() - 0.5f) * 4f;
            die1Geom.setLocalTranslation(die1BaseX + jitterX, dieBaseY + jitterY, 2f);
            die2Geom.setLocalTranslation(die2BaseX - jitterX, dieBaseY - jitterY, 2f);

            if (rollAnimTimer >= ANIM_DURATION) {
                // Finalize roll
                isAnimating = false;
                die1 = rng.nextInt(6) + 1;
                die2 = rng.nextInt(6) + 1;
                updateDieFace(die1Geom, die1);
                updateDieFace(die2Geom, die2);
                resetDiceTransforms();

                int sum = die1 + die2;
                double probability = sum / 12.0;
                targetProbPercent = (int) Math.round(probability * 100);

                resultText.setText(String.format("Resultado: %d + %d = %d", die1, die2, sum));
                probText.setText(String.format("Probabilidad de doble disparo: %d%%", targetProbPercent));
                probText.setColor(new ColorRGBA(1.0f, 0.85f, 0.2f, 1f));
                rolled = true;

                // Iniciar animación de mejora
                isUpgradeAnimating = true;
                upgradeAnimTimer = 0f;
                if (upgradeEffectNode != null) {
                    upgradeEffectNode.setCullHint(CullHint.Never);
                }
                SoundManager.getInstance().play(SoundManager.SoundType.HIT_ALERT);

                // Update button to close
                rollButton.setText("CONFIRMAR Y APLICAR MEJORA");
                rollButton.setEnabled(true);
                rollButton.setOnClickListener(() -> {
                    if (onRollComplete != null) onRollComplete.accept(probability);
                    hide();
                });
            }
        }

        // Animación dinámica de mejora (Auras, chispas doradas, barra de progreso y rebote de dados)
        if (isUpgradeAnimating) {
            upgradeAnimTimer += tpf;

            // 1. Expansión y desvanecimiento de las auras de choque
            float auraProgress = Math.min(1.0f, upgradeAnimTimer / 1.4f);
            float auraScale = 0.8f + auraProgress * 0.9f;
            float auraAlpha = Math.max(0f, 1.0f - auraProgress);
            aura1Geom.setLocalScale(auraScale);
            aura2Geom.setLocalScale(auraScale);
            auraMat1.setColor("Color", new ColorRGBA(1.0f, 0.82f, 0.2f, auraAlpha));
            auraMat2.setColor("Color", new ColorRGBA(1.0f, 0.82f, 0.2f, auraAlpha));

            // 2. Chispas / estrellas de mejora orbitando
            if (starGeoms != null) {
                float orbitAngle = upgradeAnimTimer * 3.5f;
                for (int i = 0; i < starGeoms.length; i++) {
                    float pulse = 0.8f + 0.35f * FastMath.sin(orbitAngle + i * 1.57f);
                    starGeoms[i].setLocalScale(pulse);
                }
            }

            // 3. Rebote elástico de los dados
            float bounce = 1.0f + 0.24f * FastMath.sin(upgradeAnimTimer * 8.5f) * FastMath.exp(-upgradeAnimTimer * 2.5f);
            die1Geom.setLocalScale(bounce);
            die2Geom.setLocalScale(bounce);
            float off = (dieSize * (bounce - 1f)) * 0.5f;
            die1Geom.setLocalTranslation(die1BaseX - off, dieBaseY - off, 2f);
            die2Geom.setLocalTranslation(die2BaseX - off, dieBaseY - off, 2f);

            // 4. Llenado animado de la barra de mejora
            float fillProgress = Math.min(1.0f, upgradeAnimTimer / 0.65f);
            // Smoothstep
            fillProgress = fillProgress * fillProgress * (3f - 2f * fillProgress);
            float currentBarW = Math.max(1f, fillProgress * (targetProbPercent / 100f) * maxBarWidth);
            barFillGeom.setLocalScale(currentBarW, 1f, 1f);

            // 5. Destello en el título de la mejora
            float bannerPulse = 0.85f + 0.15f * FastMath.sin(upgradeAnimTimer * 6.0f);
            upgradeBannerText.setColor(new ColorRGBA(1.0f, 0.85f * bannerPulse, 0.15f * bannerPulse, 1f));
        }
    }

    private void updateDieFace(Geometry geom, int face) {
        geom.setMaterial(dieFaceMaterials[face]);
    }

    public boolean handleMouseClick(float x, float y) {
        for (GuiButton btn : buttons) {
            if (btn.onClick(x, y)) return true;
        }
        return false;
    }

    public void handleMouseMove(float x, float y) {
        for (GuiButton btn : buttons) {
            btn.updateHover(x, y);
        }
    }

    /** Shows the dice overlay and resets its state. */
    public void show() {
        rolled = false;
        isAnimating = false;
        isUpgradeAnimating = false;
        rollAnimTimer = 0f;
        upgradeAnimTimer = 0f;
        if (upgradeEffectNode != null) {
            upgradeEffectNode.setCullHint(CullHint.Always);
        }
        resetDiceTransforms();
        die1 = 1;
        die2 = 1;
        updateDieFace(die1Geom, 1);
        updateDieFace(die2Geom, 1);
        resultText.setText("Pulsa el boton para lanzar los dados");
        probText.setText("");
        rollButton.setText("LANZAR DADOS");
        rollButton.setEnabled(true);
        rollButton.setOnClickListener(this::startRoll);
        setCullHint(CullHint.Inherit);
    }

    public void hide() {
        setCullHint(CullHint.Always);
    }

    public boolean isVisible() {
        return getCullHint() != CullHint.Always;
    }

    public void setOnRollComplete(Consumer<Double> callback) {
        this.onRollComplete = callback;
    }
}
