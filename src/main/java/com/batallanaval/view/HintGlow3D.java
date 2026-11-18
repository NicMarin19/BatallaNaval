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
import com.jme3.scene.shape.Sphere;

/**
 * Baliza luminosa que marca una casilla enemiga recomendada por el sistema
 * de ayudas. Combina anillos pulsantes, un haz vertical y un nucleo flotante.
 */
public class HintGlow3D extends Node {

    private final Geometry outerRing;
    private final Geometry innerRing;
    private final Geometry beam;
    private final Geometry core;
    private final Geometry[] brackets = new Geometry[4];

    private final Material outerMat;
    private final Material innerMat;
    private final Material beamMat;
    private final Material coreMat;

    private float time = 0f;

    public HintGlow3D(AssetManager assetManager) {
        super("HintGlow3D");

        outerMat = createGlowMaterial(assetManager, new ColorRGBA(0.15f, 1.0f, 0.75f, 0.75f));
        innerMat = createGlowMaterial(assetManager, new ColorRGBA(0.35f, 1.0f, 0.95f, 0.55f));
        beamMat = createGlowMaterial(assetManager, new ColorRGBA(0.15f, 0.85f, 1.0f, 0.28f));
        coreMat = createGlowMaterial(assetManager, new ColorRGBA(0.75f, 1.0f, 0.95f, 0.95f));

        Cylinder outer = new Cylinder(32, 32, 0.86f, 0.035f, true);
        outerRing = new Geometry("HintOuterRing", outer);
        outerRing.setMaterial(outerMat);
        outerRing.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        outerRing.setLocalTranslation(0f, 0.07f, 0f);
        attachChild(outerRing);

        Cylinder inner = new Cylinder(28, 28, 0.58f, 0.045f, true);
        innerRing = new Geometry("HintInnerRing", inner);
        innerRing.setMaterial(innerMat);
        innerRing.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        innerRing.setLocalTranslation(0f, 0.11f, 0f);
        attachChild(innerRing);

        Cylinder beamCylinder = new Cylinder(12, 16, 0.11f, 2.4f, true);
        beam = new Geometry("HintBeam", beamCylinder);
        beam.setMaterial(beamMat);
        beam.setLocalTranslation(0f, 1.25f, 0f);
        attachChild(beam);

        Sphere coreSphere = new Sphere(16, 16, 0.17f);
        core = new Geometry("HintCore", coreSphere);
        core.setMaterial(coreMat);
        core.setLocalTranslation(0f, 2.45f, 0f);
        attachChild(core);

        Box bracketBox = new Box(0.035f, 0.45f, 0.035f);
        float[][] bracketPositions = {
                {-0.74f, 0.50f, -0.74f},
                {0.74f, 0.50f, -0.74f},
                {-0.74f, 0.50f, 0.74f},
                {0.74f, 0.50f, 0.74f}
        };
        for (int i = 0; i < bracketPositions.length; i++) {
            Geometry bracket = new Geometry("HintBracket_" + i, bracketBox);
            bracket.setMaterial(coreMat);
            bracket.setLocalTranslation(bracketPositions[i][0], bracketPositions[i][1], bracketPositions[i][2]);
            attachChild(bracket);
            brackets[i] = bracket;
        }

        setQueueBucket(RenderQueue.Bucket.Transparent);
    }

    private static Material createGlowMaterial(AssetManager assetManager, ColorRGBA color) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Additive);
        material.getAdditionalRenderState().setDepthWrite(false);
        return material;
    }

    public void update(float tpf) {
        time += tpf;

        float pulse = 0.5f + 0.5f * FastMath.sin(time * 4.2f);
        float fastPulse = 0.5f + 0.5f * FastMath.sin(time * 9.0f);

        float outerScale = 1.0f + 0.22f * pulse;
        outerRing.setLocalScale(outerScale, 1f, outerScale);

        float innerScale = 1.15f - 0.25f * pulse;
        innerRing.setLocalScale(innerScale, 1f, innerScale);

        float beamScale = 1.0f + 0.15f * fastPulse;
        beam.setLocalScale(beamScale, 1f, beamScale);

        core.setLocalScale(0.85f + 0.45f * fastPulse);
        core.setLocalTranslation(0f, 2.45f + 0.16f * FastMath.sin(time * 3.4f), 0f);

        for (Geometry bracket : brackets) {
            bracket.setLocalScale(1f, 0.85f + 0.30f * fastPulse, 1f);
        }

        outerMat.setColor("Color", new ColorRGBA(0.15f, 1.0f, 0.75f, 0.45f + 0.40f * pulse));
        innerMat.setColor("Color", new ColorRGBA(0.35f, 1.0f, 0.95f, 0.35f + 0.35f * pulse));
        beamMat.setColor("Color", new ColorRGBA(0.15f, 0.85f, 1.0f, 0.14f + 0.22f * fastPulse));
        coreMat.setColor("Color", new ColorRGBA(0.75f, 1.0f, 0.95f, 0.75f + 0.25f * fastPulse));
    }
}
