package com.batallanaval.ui;

import com.batallanaval.view.TextureGenerator;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

/**
 * Paleta y helpers de UI compartidos por el HUD y los menus.
 */
public final class UiKit {

    public static final ColorRGBA ACCENT      = new ColorRGBA(0.32f, 0.84f, 1.00f, 1f);
    public static final ColorRGBA ACCENT_SOFT = new ColorRGBA(0.18f, 0.46f, 0.66f, 1f);
    public static final ColorRGBA TEXT        = new ColorRGBA(0.88f, 0.93f, 0.98f, 1f);
    public static final ColorRGBA TEXT_DIM    = new ColorRGBA(0.56f, 0.66f, 0.75f, 1f);
    public static final ColorRGBA WARN        = new ColorRGBA(1.00f, 0.74f, 0.26f, 1f);
    public static final ColorRGBA DANGER      = new ColorRGBA(1.00f, 0.38f, 0.32f, 1f);
    public static final ColorRGBA OK          = new ColorRGBA(0.36f, 1.00f, 0.55f, 1f);

    public static final ColorRGBA BTN_BG      = new ColorRGBA(0.055f, 0.13f, 0.22f, 0.96f);
    public static final ColorRGBA BTN_BORDER  = new ColorRGBA(0.22f, 0.62f, 0.92f, 0.95f);
    public static final ColorRGBA BTN_GREEN_BG  = new ColorRGBA(0.05f, 0.24f, 0.12f, 0.96f);
    public static final ColorRGBA BTN_GREEN_BD  = new ColorRGBA(0.30f, 0.95f, 0.50f, 0.95f);
    public static final ColorRGBA BTN_ORANGE_BG = new ColorRGBA(0.26f, 0.14f, 0.04f, 0.96f);
    public static final ColorRGBA BTN_ORANGE_BD = new ColorRGBA(1.00f, 0.66f, 0.20f, 0.95f);
    public static final ColorRGBA BTN_RED_BG    = new ColorRGBA(0.26f, 0.06f, 0.06f, 0.96f);
    public static final ColorRGBA BTN_RED_BD    = new ColorRGBA(1.00f, 0.34f, 0.34f, 0.95f);

    private UiKit() {
    }

    /** Panel con relleno, borde y esquinas biseladas, anclado en su esquina inferior izquierda. */
    public static Geometry panel(AssetManager assetManager, float width, float height,
                                 ColorRGBA fill, ColorRGBA border) {
        return panelAt(assetManager, width, height, fill, border, 0f, 0f, 0f);
    }

    public static Geometry panelAt(AssetManager assetManager, float width, float height,
                                   ColorRGBA fill, ColorRGBA border,
                                   float x, float y, float z) {
        Quad quad = new Quad(width, height);
        Geometry geom = new Geometry("UiPanel", quad);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture tex = TextureGenerator.createButtonTexture(
                Math.max(2, (int) width), Math.max(2, (int) height), fill, border);
        mat.setTexture("ColorMap", tex);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        geom.setMaterial(mat);
        geom.setLocalTranslation(x, y, z);
        return geom;
    }

    /** Rectangulo plano de color (sin textura), util para lineas y velos. */
    public static Geometry rect(AssetManager assetManager, float width, float height,
                                ColorRGBA color, float x, float y, float z) {
        Quad quad = new Quad(width, height);
        Geometry geom = new Geometry("UiRect", quad);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        geom.setMaterial(mat);
        geom.setLocalTranslation(x, y, z);
        return geom;
    }

    public static Geometry hLine(AssetManager assetManager, float width,
                                 ColorRGBA color, float x, float y, float z) {
        return rect(assetManager, width, 1.5f, color, x, y, z);
    }

    public static BitmapText text(AssetManager assetManager, BitmapFont font, String content,
                                  float scale, ColorRGBA color) {
        BitmapText text = new BitmapText(font, false);
        text.setSize(font.getCharSet().getRenderedSize() * scale);
        text.setText(content);
        text.setColor(color);
        text.setQueueBucket(RenderQueue.Bucket.Gui);
        return text;
    }

    public static BitmapText label(AssetManager assetManager, BitmapFont font, String content,
                                   float scale, ColorRGBA color, float x, float y, float z) {
        BitmapText text = text(assetManager, font, content, scale, color);
        text.setLocalTranslation(x, y, z);
        return text;
    }

    public static BitmapText labelRight(AssetManager assetManager, BitmapFont font, String content,
                                        float scale, ColorRGBA color, float rightX, float y, float z) {
        BitmapText text = text(assetManager, font, content, scale, color);
        text.setLocalTranslation(rightX - text.getLineWidth(), y, z);
        return text;
    }

    public static BitmapText labelCentered(AssetManager assetManager, BitmapFont font, String content,
                                           float scale, ColorRGBA color, float centerX, float y, float z) {
        BitmapText text = text(assetManager, font, content, scale, color);
        text.setLocalTranslation(centerX - text.getLineWidth() * 0.5f, y, z);
        return text;
    }

    public static void setText(BitmapText text, String content) {
        text.setText(content);
    }

    public static String formatTime(float seconds) {
        int total = Math.max(0, (int) seconds);
        int minutes = total / 60;
        int secs = total % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    public static String hpBar(int current, int max) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < max; i++) {
            sb.append(i < current ? '#' : '-');
        }
        sb.append(']');
        return sb.toString();
    }
}
