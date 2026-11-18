package com.batallanaval.view;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

/**
 * Cartel táctico flotante 3D que aparece directamente sobre el barco destruido.
 * Muestra el comentario del combate en tiempo real mientras el barco se hunde.
 */
public class FloatingShipComment3D extends Node {

    private final Vector3f basePosition;
    private final Geometry backgroundGeom;
    private final BitmapText titleText;
    private final BitmapText phraseText;
    private final Material bgMat;

    private float timer = 0f;
    private static final float DURATION = 5.2f;
    private boolean finished = false;

    public FloatingShipComment3D(AssetManager assetManager, Vector3f position, String phrase) {
        super("FloatingShipComment");
        this.basePosition = position.clone();

        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        // Format clean natural text
        String cleanPhrase = phrase.replace("🗣️", "").trim();

        titleText = new BitmapText(font, false);
        titleText.setSize(0.52f);
        titleText.setText("[RADIO DE COMBATE]");
        titleText.setColor(new ColorRGBA(1.0f, 0.82f, 0.20f, 1f));
        titleText.setQueueBucket(RenderQueue.Bucket.Transparent);

        phraseText = new BitmapText(font, false);
        phraseText.setSize(0.68f);
        phraseText.setText(cleanPhrase);
        phraseText.setColor(new ColorRGBA(1.0f, 0.98f, 0.90f, 1f));
        phraseText.setQueueBucket(RenderQueue.Bucket.Transparent);

        float textWidth = Math.max(titleText.getLineWidth(), phraseText.getLineWidth());
        float padX = 1.0f;
        float padY = 0.55f;
        float plateW = textWidth + padX * 2.0f;
        float plateH = 1.8f;

        // Background plate
        Quad plateQuad = new Quad(plateW, plateH);
        backgroundGeom = new Geometry("CommentPlate", plateQuad);
        bgMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture bgTex = TextureGenerator.createButtonTexture((int) (plateW * 40), (int) (plateH * 40),
                new ColorRGBA(0.02f, 0.05f, 0.10f, 0.92f),
                new ColorRGBA(1.0f, 0.75f, 0.20f, 0.95f));
        bgMat.setTexture("ColorMap", bgTex);
        bgMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        backgroundGeom.setMaterial(bgMat);
        backgroundGeom.setLocalTranslation(-plateW * 0.5f, -plateH * 0.5f, -0.02f);
        backgroundGeom.setQueueBucket(RenderQueue.Bucket.Transparent);
        attachChild(backgroundGeom);

        // Center texts on plate
        titleText.setLocalTranslation(-titleText.getLineWidth() * 0.5f, plateH * 0.5f - 0.25f, 0.02f);
        attachChild(titleText);

        phraseText.setLocalTranslation(-phraseText.getLineWidth() * 0.5f, -0.10f, 0.02f);
        attachChild(phraseText);

        setLocalTranslation(basePosition.x, basePosition.y + 2.4f, basePosition.z);
        setLocalScale(0.1f);
        setQueueBucket(RenderQueue.Bucket.Transparent);
    }

    public void update(float tpf, Camera camera) {
        if (finished) return;

        timer += tpf;
        if (timer >= DURATION) {
            finished = true;
            removeFromParent();
            return;
        }

        // Smooth pop-in scale
        float scale;
        if (timer < 0.25f) {
            float s = timer / 0.25f;
            scale = 0.1f + 0.9f * (s * s * (3f - 2f * s));
        } else if (timer > DURATION - 0.8f) {
            float s = (DURATION - timer) / 0.8f;
            scale = Math.max(0.01f, s);
        } else {
            scale = 1.0f;
        }
        setLocalScale(scale);

        // Gentle float upwards
        float floatY = basePosition.y + 2.4f + timer * 0.22f;
        setLocalTranslation(basePosition.x, floatY, basePosition.z);

        // Billboard rotation: face the active camera
        if (camera != null) {
            lookAt(camera.getLocation(), Vector3f.UNIT_Y);
        }

        // Subtle pulse on title
        float pulse = 0.85f + 0.15f * FastMath.sin(timer * 5f);
        titleText.setColor(new ColorRGBA(1.0f, 0.82f * pulse, 0.20f * pulse, Math.min(1f, scale * 1.5f)));
        phraseText.setColor(new ColorRGBA(1.0f, 0.98f, 0.90f, Math.min(1f, scale * 1.5f)));
    }

    public boolean isFinished() {
        return finished;
    }
}
