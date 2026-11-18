package com.batallanaval.ui;

import com.batallanaval.audio.SoundManager;
import com.batallanaval.view.TextureGenerator;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

public class GuiButton extends Node {

    private final String id;
    private final float width;
    private final float height;
    private Runnable action;
    private final Geometry bgGeom;
    private final BitmapText labelText;

    private final Material matNormal;
    private final Material matHover;
    private final Material matDisabled;

    private boolean enabled = true;
    private boolean hovered = false;

    public GuiButton(String id, String text, float width, float height, AssetManager assetManager, BitmapFont font,
                     ColorRGBA normalBg, ColorRGBA normalBorder, Runnable action) {
        super("Button_" + id);
        this.id = id;
        this.width = width;
        this.height = height;
        this.action = action;

        // Normal material
        Texture normalTex = TextureGenerator.createButtonTexture((int) width, (int) height, normalBg, normalBorder);
        matNormal = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matNormal.setTexture("ColorMap", normalTex);
        matNormal.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        // Hover material (brighter)
        ColorRGBA hoverBg = normalBg.mult(1.4f);
        hoverBg.a = Math.min(1.0f, normalBg.a + 0.15f);
        ColorRGBA hoverBorder = ColorRGBA.White;
        Texture hoverTex = TextureGenerator.createButtonTexture((int) width, (int) height, hoverBg, hoverBorder);
        matHover = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matHover.setTexture("ColorMap", hoverTex);
        matHover.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        // Disabled material (greyed out)
        ColorRGBA disBg = new ColorRGBA(0.18f, 0.20f, 0.22f, 0.6f);
        ColorRGBA disBorder = new ColorRGBA(0.35f, 0.35f, 0.35f, 0.6f);
        Texture disTex = TextureGenerator.createButtonTexture((int) width, (int) height, disBg, disBorder);
        matDisabled = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matDisabled.setTexture("ColorMap", disTex);
        matDisabled.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        // Background Quad
        Quad quad = new Quad(width, height);
        bgGeom = new Geometry("BgQuad", quad);
        bgGeom.setMaterial(matNormal);
        attachChild(bgGeom);

        // Centered Text Label
        labelText = new BitmapText(font, false);
        labelText.setSize(font.getCharSet().getRenderedSize() * 0.85f);
        labelText.setText(text);
        labelText.setColor(ColorRGBA.White);
        labelText.setQueueBucket(RenderQueue.Bucket.Gui);

        updateTextPosition();
        attachChild(labelText);

        setQueueBucket(RenderQueue.Bucket.Gui);
    }

    public void setText(String newText) {
        labelText.setText(newText);
        updateTextPosition();
    }

    public void setOnClickListener(Runnable action) {
        this.action = action;
    }

    private void updateTextPosition() {
        float textWidth = labelText.getLineWidth();
        float textHeight = labelText.getLineHeight();
        float tx = (width - textWidth) * 0.5f;
        float ty = (height + textHeight * 0.7f) * 0.5f;
        labelText.setLocalTranslation(tx, ty, 1f);
    }

    public boolean contains(float screenX, float screenY) {
        Vector3f worldPos = getWorldTranslation();
        return screenX >= worldPos.x && screenX <= worldPos.x + width &&
               screenY >= worldPos.y && screenY <= worldPos.y + height;
    }

    public void updateHover(float mouseX, float mouseY) {
        if (!enabled) return;
        boolean nowHovered = contains(mouseX, mouseY);
        if (nowHovered != hovered) {
            hovered = nowHovered;
            bgGeom.setMaterial(hovered ? matHover : matNormal);
        }
    }

    public boolean onClick(float mouseX, float mouseY) {
        if (!enabled) return false;
        if (contains(mouseX, mouseY)) {
            SoundManager.getInstance().play(SoundManager.SoundType.BUTTON_CLICK);
            if (action != null) {
                action.run();
            }
            return true;
        }
        return false;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            hovered = false;
            bgGeom.setMaterial(matDisabled);
            labelText.setColor(new ColorRGBA(0.5f, 0.5f, 0.5f, 0.8f));
        } else {
            bgGeom.setMaterial(matNormal);
            labelText.setColor(ColorRGBA.White);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getId() {
        return id;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
