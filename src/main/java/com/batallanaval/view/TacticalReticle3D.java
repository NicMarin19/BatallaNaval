package com.batallanaval.view;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

public class TacticalReticle3D extends Node {

    private final Geometry ringGeom;
    private final Geometry laserBeamGeom;
    private final Material matRing;
    private final Material matLaser;

    private float animTime = 0f;

    public TacticalReticle3D(AssetManager assetManager) {
        super("TacticalReticle3D");

        float reticleSize = Board3DView.CELL_SIZE * 0.95f;

        // Holographic Target Quad
        Quad quad = new Quad(reticleSize, reticleSize);
        ringGeom = new Geometry("ReticleRing", quad);

        matRing = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture reticleTex = TextureGenerator.createTargetReticleTexture(128);
        matRing.setTexture("ColorMap", reticleTex);
        matRing.setColor("Color", new ColorRGBA(0.2f, 1.0f, 0.5f, 0.95f));
        matRing.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        ringGeom.setMaterial(matRing);

        // Lay flat horizontally, centered
        ringGeom.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        ringGeom.setLocalTranslation(-reticleSize * 0.5f, 0.15f, reticleSize * 0.5f);
        attachChild(ringGeom);

        // Vertical Laser Targeting Beam
        matLaser = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matLaser.setColor("Color", new ColorRGBA(0.2f, 1.0f, 0.5f, 0.6f));
        matLaser.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        Cylinder laser = new Cylinder(8, 8, 0.04f, 4.0f, true);
        laserBeamGeom = new Geometry("LaserBeam", laser);
        laserBeamGeom.setMaterial(matLaser);
        laserBeamGeom.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        laserBeamGeom.setLocalTranslation(0, 2.15f, 0);
        attachChild(laserBeamGeom);

        // 4 Corner Brackets
        Material matBracket = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matBracket.setColor("Color", new ColorRGBA(0.4f, 1.0f, 0.8f, 0.85f));
        matBracket.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        float half = reticleSize * 0.52f;
        for (int i = 0; i < 4; i++) {
            float sx = (i % 2 == 0) ? -1 : 1;
            float sz = (i < 2) ? -1 : 1;
            Box bracket = new Box(0.12f, 0.02f, 0.03f);
            Geometry bGeom = new Geometry("Bracket_" + i, bracket);
            bGeom.setMaterial(matBracket);
            bGeom.setLocalTranslation(sx * half, 0.18f, sz * half);
            attachChild(bGeom);
        }

        // Central Precision Laser Dot / Point of Aim
        Material matDot = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matDot.setColor("Color", new ColorRGBA(1.0f, 0.2f, 0.2f, 1f));
        Cylinder dot = new Cylinder(8, 8, 0.045f, 0.02f, true);
        Geometry dotGeom = new Geometry("ReticleCenterDot", dot);
        dotGeom.setMaterial(matDot);
        dotGeom.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        dotGeom.setLocalTranslation(0, 0.17f, 0);
        attachChild(dotGeom);

        setQueueBucket(RenderQueue.Bucket.Transparent);
        setCullHint(CullHint.Always); // Initially hidden
    }

    public void showAt(Vector3f localPosition, boolean valid) {
        setLocalTranslation(localPosition);
        ColorRGBA col = valid ? new ColorRGBA(0.2f, 1.0f, 0.5f, 0.95f) : new ColorRGBA(1.0f, 0.4f, 0.1f, 0.95f);
        matRing.setColor("Color", col);
        matLaser.setColor("Color", new ColorRGBA(col.r, col.g, col.b, 0.5f));
        setCullHint(CullHint.Never);
    }

    public void hide() {
        setCullHint(CullHint.Always);
    }

    public void update(float tpf) {
        if (getCullHint() == CullHint.Always) return;

        animTime += tpf;
        // Rotating laser ring
        ringGeom.rotate(0, 0, tpf * 1.5f);

        // Pulsating height and laser brightness
        float pulse = FastMath.sin(animTime * 6.0f);
        float beamAlpha = 0.5f + 0.25f * pulse;
        ColorRGBA cur = (ColorRGBA) matLaser.getParam("Color").getValue();
        matLaser.setColor("Color", new ColorRGBA(cur.r, cur.g, cur.b, beamAlpha));
    }
}
