package com.batallanaval.view;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;

public class CannonProjectile extends Node {

    private final Vector3f start;
    private final Vector3f target;
    private final float duration;
    private final float arcHeight;
    private final Runnable onImpact;

    private float elapsedTime = 0f;
    private boolean finished = false;

    public CannonProjectile(AssetManager assetManager, Vector3f start, Vector3f target, float duration, float arcHeight, Runnable onImpact) {
        super("CannonProjectile");
        this.start = new Vector3f(start);
        this.target = new Vector3f(target);
        this.duration = Math.max(0.1f, duration);
        this.arcHeight = arcHeight;
        this.onImpact = onImpact;

        // Visual shell
        Material matShell = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matShell.setColor("Color", new ColorRGBA(1.0f, 0.85f, 0.2f, 1f));

        Sphere shell = new Sphere(10, 10, 0.22f);
        Geometry geom = new Geometry("Shell", shell);
        geom.setMaterial(matShell);
        attachChild(geom);

        // Fiery tracer tail
        Material matTail = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matTail.setColor("Color", new ColorRGBA(1.0f, 0.4f, 0.05f, 0.8f));
        Cylinder tail = new Cylinder(8, 8, 0.12f, 0.6f, true);
        Geometry geomTail = new Geometry("TracerTail", tail);
        geomTail.setMaterial(matTail);
        geomTail.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        geomTail.setLocalTranslation(0, 0, -0.35f);
        attachChild(geomTail);

        setLocalTranslation(start);
    }

    public void update(float tpf) {
        if (finished) return;

        elapsedTime += tpf;
        float progress = Math.min(1.0f, elapsedTime / duration);

        // Linear horizontal interpolation
        Vector3f current = new Vector3f();
        current.interpolateLocal(start, target, progress);

        // Parabolic vertical arc: 4 * h * p * (1 - p)
        float parabola = 4.0f * arcHeight * progress * (1.0f - progress);
        current.y += parabola;

        setLocalTranslation(current);

        // Rotate shell towards direction of movement
        float futureProgress = Math.min(1.0f, progress + 0.05f);
        Vector3f nextPos = new Vector3f();
        nextPos.interpolateLocal(start, target, futureProgress);
        nextPos.y += 4.0f * arcHeight * futureProgress * (1.0f - futureProgress);
        Vector3f dir = nextPos.subtract(current);
        if (dir.lengthSquared() > 0.001f) {
            float yRatio = Math.abs(dir.y) / dir.length();
            Vector3f up = yRatio > 0.95f ? Vector3f.UNIT_Z : Vector3f.UNIT_Y;
            lookAt(current.add(dir), up);
        }

        if (progress >= 1.0f) {
            finished = true;
            if (onImpact != null) {
                onImpact.run();
            }
            removeFromParent();
        }
    }

    public boolean isFinished() {
        return finished;
    }
}
