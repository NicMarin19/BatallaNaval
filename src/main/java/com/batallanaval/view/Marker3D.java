package com.batallanaval.view;

import com.batallanaval.model.CellState;
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

public class Marker3D extends Node {

    private final CellState state;
    private float animationTime = 0f;

    // Animated nodes
    private Geometry pulseGeom;
    private Geometry rippleGeom;
    private Geometry flameCoreGeom;
    private Node smokeNode;

    public Marker3D(AssetManager assetManager, CellState state) {
        super("Marker_" + state.name());
        this.state = state;

        if (state == CellState.MISS) {
            buildMissMarker(assetManager);
        } else if (state == CellState.HIT || state == CellState.SUNK) {
            buildHitMarker(assetManager, state == CellState.SUNK);
        }

        setQueueBucket(RenderQueue.Bucket.Transparent);
    }

    private void buildMissMarker(AssetManager assetManager) {
        // Ocean Splash Water Ripple (flat on water surface)
        Material matRipple = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matRipple.setColor("Color", new ColorRGBA(0.4f, 0.85f, 1.0f, 0.65f));
        matRipple.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        Cylinder ripple = new Cylinder(24, 24, 0.65f, 0.02f, true);
        rippleGeom = new Geometry("WaterRipple", ripple);
        rippleGeom.setMaterial(matRipple);
        rippleGeom.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        rippleGeom.setLocalTranslation(0, 0.04f, 0);
        attachChild(rippleGeom);

        // Buoy Hull (White/Orange Marine Navigation Buoy)
        Material matBuoyWhite = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        matBuoyWhite.setBoolean("UseMaterialColors", true);
        matBuoyWhite.setColor("Diffuse", new ColorRGBA(0.95f, 0.95f, 0.98f, 1f));
        matBuoyWhite.setColor("Ambient", new ColorRGBA(0.4f, 0.4f, 0.45f, 1f));

        Cylinder buoyBase = new Cylinder(16, 16, 0.32f, 0.22f, true);
        Geometry geomBase = new Geometry("BuoyBase", buoyBase);
        geomBase.setMaterial(matBuoyWhite);
        geomBase.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        geomBase.setLocalTranslation(0, 0.12f, 0);
        attachChild(geomBase);

        // Orange Daymark Stripe
        Material matOrange = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matOrange.setColor("Color", new ColorRGBA(1.0f, 0.45f, 0.05f, 1f));
        Cylinder orangeStripe = new Cylinder(16, 16, 0.33f, 0.08f, true);
        Geometry gOrange = new Geometry("OrangeStripe", orangeStripe);
        gOrange.setMaterial(matOrange);
        gOrange.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        gOrange.setLocalTranslation(0, 0.18f, 0);
        attachChild(gOrange);

        // Mast Tower Cage
        Material matMetal = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matMetal.setColor("Color", new ColorRGBA(0.25f, 0.28f, 0.32f, 1f));
        Cylinder mast = new Cylinder(8, 8, 0.05f, 0.65f, true);
        Geometry gMast = new Geometry("BuoyMast", mast);
        gMast.setMaterial(matMetal);
        gMast.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        gMast.setLocalTranslation(0, 0.50f, 0);
        attachChild(gMast);

        // Flashing Cyan LED Navigation Lantern at top
        Material matLantern = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matLantern.setColor("Color", new ColorRGBA(0.2f, 0.9f, 1.0f, 1f));
        Sphere lantern = new Sphere(10, 10, 0.14f);
        pulseGeom = new Geometry("LanternTop", lantern);
        pulseGeom.setMaterial(matLantern);
        pulseGeom.setLocalTranslation(0, 0.88f, 0);
        attachChild(pulseGeom);
    }

    private void buildHitMarker(AssetManager assetManager, boolean isSunk) {
        // Charred Blast Crater Base
        Material matCharred = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matCharred.setColor("Color", new ColorRGBA(0.12f, 0.12f, 0.14f, 0.95f));

        Cylinder crater = new Cylinder(16, 16, 0.55f, 0.05f, true);
        Geometry gCrater = new Geometry("CraterBase", crater);
        gCrater.setMaterial(matCharred);
        gCrater.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        gCrater.setLocalTranslation(0, 0.06f, 0);
        attachChild(gCrater);

        // Fiery Base Flame Tongue (Red-Orange)
        Material matFlameBase = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matFlameBase.setColor("Color", new ColorRGBA(1.0f, 0.25f, 0.05f, 0.9f));
        matFlameBase.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        Cylinder flameBase = new Cylinder(12, 12, 0.28f, 0.08f, 0.85f, true, false);
        Geometry gFlameBase = new Geometry("FlameBase", flameBase);
        gFlameBase.setMaterial(matFlameBase);
        gFlameBase.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        gFlameBase.setLocalTranslation(0, 0.45f, 0);
        attachChild(gFlameBase);

        // Glowing Inner Flame Core (Brilliant Yellow)
        Material matCore = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matCore.setColor("Color", new ColorRGBA(1.0f, 0.85f, 0.2f, 1.0f));
        Sphere core = new Sphere(10, 10, 0.20f);
        flameCoreGeom = new Geometry("FlameCore", core);
        flameCoreGeom.setMaterial(matCore);
        flameCoreGeom.setLocalTranslation(0, 0.55f, 0);
        attachChild(flameCoreGeom);

        // Rising Smoke Column (Rising dark grey puffs)
        smokeNode = new Node("SmokeColumn");
        Material matSmoke = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matSmoke.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.22f, 0.75f));
        matSmoke.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        for (int i = 0; i < 3; i++) {
            Sphere smokePuff = new Sphere(8, 8, 0.16f + i * 0.08f);
            Geometry gSmoke = new Geometry("SmokePuff_" + i, smokePuff);
            gSmoke.setMaterial(matSmoke);
            gSmoke.setLocalTranslation(FastMath.sin(i * 1.5f) * 0.1f, 1.0f + i * 0.45f, FastMath.cos(i * 1.5f) * 0.1f);
            smokeNode.attachChild(gSmoke);
        }
        attachChild(smokeNode);

        // If sunk: catastrophic hazard cross on the beacon
        if (isSunk) {
            Material matCross = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            matCross.setColor("Color", new ColorRGBA(1.0f, 0.15f, 0.15f, 1f));
            Box bar1 = new Box(0.38f, 0.06f, 0.06f);
            Geometry gBar1 = new Geometry("SunkCross1", bar1);
            gBar1.setMaterial(matCross);
            gBar1.setLocalTranslation(0, 1.95f, 0);
            gBar1.rotate(0, 0, FastMath.QUARTER_PI);
            attachChild(gBar1);

            Geometry gBar2 = new Geometry("SunkCross2", bar1);
            gBar2.setMaterial(matCross);
            gBar2.setLocalTranslation(0, 1.95f, 0);
            gBar2.rotate(0, 0, -FastMath.QUARTER_PI);
            attachChild(gBar2);
        }

        pulseGeom = flameCoreGeom;
    }

    public void update(float tpf) {
        animationTime += tpf;

        // Animate water ripple
        if (rippleGeom != null) {
            float rippleScale = 1.0f + 0.35f * FastMath.sin(animationTime * 2.5f);
            rippleGeom.setLocalScale(rippleScale, 1.0f, rippleScale);
        }

        // Animate flame pulse
        if (flameCoreGeom != null) {
            float flicker = 1.0f + 0.18f * FastMath.sin(animationTime * 12.0f);
            flameCoreGeom.setLocalScale(flicker, flicker * 1.2f, flicker);
        }

        // Animate smoke drift
        if (smokeNode != null) {
            smokeNode.rotate(0, tpf * 0.6f, 0);
        }

        // Animate buoy pulse lantern
        if (pulseGeom != null && state == CellState.MISS) {
            float flash = FastMath.sin(animationTime * 5.0f);
            float scale = flash > 0.3f ? 1.25f : 0.85f;
            pulseGeom.setLocalScale(scale);
        }
    }

    public CellState getState() {
        return state;
    }
}
