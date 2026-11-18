package com.batallanaval.ui;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel de ajustes reutilizable por el menu de inicio y el menu de pausa.
 * Se construye centrado en su propio origen.
 */
public class SettingsPanel extends Node {

    private final float panelWidth;
    private final float panelHeight;
    private final GameSettings settings;

    private final List<GuiButton> buttons = new ArrayList<>();
    private GuiButton btnSound;
    private GuiButton btnWater;
    private GuiButton btnHints;

    private Runnable onClose;
    private Runnable onChange;

    public SettingsPanel(AssetManager assetManager, BitmapFont font, GameSettings settings,
                         float width, float height) {
        super("SettingsPanel");
        this.panelWidth = width;
        this.panelHeight = height;
        this.settings = settings;

        build(assetManager, font);
        setQueueBucket(RenderQueue.Bucket.Gui);
        setLocalTranslation(0, 0, 0);
        hide();
    }

    private void build(AssetManager assetManager, BitmapFont font) {
        float x0 = -panelWidth * 0.5f;
        float y0 = -panelHeight * 0.5f;

        // Fondo
        attachChild(UiKit.panelAt(assetManager, panelWidth, panelHeight,
                new ColorRGBA(0.030f, 0.065f, 0.115f, 1f),
                new ColorRGBA(0.28f, 0.72f, 1.0f, 1f), x0, y0, 0f));

        // Cabecera
        attachChild(UiKit.label(assetManager, font, "AJUSTES", 1.45f, UiKit.ACCENT,
                x0 + 26f, panelHeight * 0.5f - 22f, 1f));
        attachChild(UiKit.label(assetManager, font, "CONFIGURACION DEL SISTEMA NAVAL",
                0.62f, UiKit.TEXT_DIM, x0 + 26f, panelHeight * 0.5f - 46f, 1f));
        attachChild(UiKit.hLine(assetManager, panelWidth - 52f,
                new ColorRGBA(0.24f, 0.60f, 0.90f, 0.65f), x0 + 26f,
                panelHeight * 0.5f - 58f, 1f));

        float rowY = panelHeight * 0.5f - 96f;
        float btnW = 176f;
        float btnH = 38f;

        attachChild(UiKit.label(assetManager, font, "SONIDO", 0.85f, UiKit.TEXT,
                x0 + 30f, rowY, 1f));
        attachChild(UiKit.label(assetManager, font, "Efectos de canones, alarmas y ambiente",
                0.58f, UiKit.TEXT_DIM, x0 + 30f, rowY - 17f, 1f));
        btnSound = new GuiButton("setSound", "", btnW, btnH, assetManager, font,
                UiKit.BTN_BG, UiKit.BTN_BORDER, () -> {
                    settings.toggleSound();
                    refreshLabels();
                    notifyChanged();
                });
        btnSound.setLocalTranslation(-x0 - btnW - 26f, rowY - 29f, 1.2f);
        attachChild(btnSound);
        buttons.add(btnSound);

        rowY -= 58f;
        attachChild(UiKit.label(assetManager, font, "CALIDAD DEL AGUA", 0.85f, UiKit.TEXT,
                x0 + 30f, rowY, 1f));
        attachChild(UiKit.label(assetManager, font, "Espuma, reflejos y oleaje adicional",
                0.58f, UiKit.TEXT_DIM, x0 + 30f, rowY - 17f, 1f));
        btnWater = new GuiButton("setWater", "", btnW, btnH, assetManager, font,
                UiKit.BTN_BG, UiKit.BTN_BORDER, () -> {
                    settings.toggleWaterQuality();
                    refreshLabels();
                    notifyChanged();
                });
        btnWater.setLocalTranslation(-x0 - btnW - 26f, rowY - 29f, 1.2f);
        attachChild(btnWater);
        buttons.add(btnWater);

        rowY -= 58f;
        attachChild(UiKit.label(assetManager, font, "AYUDAS EN PANTALLA", 0.85f, UiKit.TEXT,
                x0 + 30f, rowY, 1f));
        attachChild(UiKit.label(assetManager, font, "Textos de control y referencias del HUD",
                0.58f, UiKit.TEXT_DIM, x0 + 30f, rowY - 17f, 1f));
        btnHints = new GuiButton("setHints", "", btnW, btnH, assetManager, font,
                UiKit.BTN_BG, UiKit.BTN_BORDER, () -> {
                    settings.toggleShowHints();
                    refreshLabels();
                    notifyChanged();
                });
        btnHints.setLocalTranslation(-x0 - btnW - 26f, rowY - 29f, 1.2f);
        attachChild(btnHints);
        buttons.add(btnHints);

        // Cerrar
        GuiButton btnClose = new GuiButton("setClose", "CERRAR", 190f, 42f, assetManager, font,
                UiKit.BTN_BG, UiKit.BTN_BORDER, () -> {
                    hide();
                    if (onClose != null) onClose.run();
                });
        btnClose.setLocalTranslation(-95f, y0 + 24f, 1.2f);
        attachChild(btnClose);
        buttons.add(btnClose);

        refreshLabels();
    }

    public void refreshLabels() {
        btnSound.setText(settings.isSoundEnabled() ? "SONIDO: ACTIVADO" : "SONIDO: SILENCIO");
        btnWater.setText(settings.isHighQualityWater() ? "AGUA: ALTA" : "AGUA: MEDIA");
        btnHints.setText(settings.isShowHints() ? "AYUDAS: VISIBLES" : "AYUDAS: OCULTAS");
    }

    private void notifyChanged() {
        if (onChange != null) onChange.run();
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public boolean handleMouseClick(float x, float y) {
        if (!isVisible()) return false;
        for (GuiButton btn : buttons) {
            if (btn.onClick(x, y)) return true;
        }
        return containsPoint(x, y);
    }

    public void handleMouseMove(float x, float y) {
        if (!isVisible()) return;
        for (GuiButton btn : buttons) {
            btn.updateHover(x, y);
        }
    }

    public boolean containsPoint(float x, float y) {
        Vector3f origin = getWorldTranslation();
        return x >= origin.x - panelWidth * 0.5f && x <= origin.x + panelWidth * 0.5f
                && y >= origin.y - panelHeight * 0.5f && y <= origin.y + panelHeight * 0.5f;
    }

    public void show() {
        setCullHint(CullHint.Inherit);
        refreshLabels();
    }

    public void hide() {
        setCullHint(CullHint.Always);
    }

    public boolean isVisible() {
        return getCullHint() != CullHint.Always;
    }
}
