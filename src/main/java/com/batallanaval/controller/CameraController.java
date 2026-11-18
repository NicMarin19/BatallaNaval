package com.batallanaval.controller;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

public class CameraController {

    public enum CameraMode {
        GENERAL("General", new Vector3f(24f, 40f, 54f), new Vector3f(23f, 0f, 11f)),
        ENEMIGO("Ataque", new Vector3f(36f, 34f, 44f), new Vector3f(36f, 0f, 10f)),
        DEFENSA("Defensa", new Vector3f(10f, 34f, 44f), new Vector3f(10f, 0f, 10f)),
        CENITAL("Cenital", new Vector3f(23f, 62f, 10.1f), new Vector3f(23f, 0f, 10f));

        public final String label;
        public final Vector3f position;
        public final Vector3f lookTarget;

        CameraMode(String label, Vector3f position, Vector3f lookTarget) {
            this.label = label;
            this.position = position;
            this.lookTarget = lookTarget;
        }
    }

    private final Camera camera;
    private CameraMode currentMode = CameraMode.GENERAL;

    private Vector3f targetPosition = new Vector3f();
    private Vector3f targetLookAt = new Vector3f();
    private Vector3f currentLookAt = new Vector3f();

    private boolean transitioning = false;
    private float transitionSpeed = 5.0f;

    private Vector3f savedPosition = new Vector3f();
    private Vector3f savedLookTarget = new Vector3f();
    private CameraMode savedMode = CameraMode.GENERAL;
    private boolean isFocusedOnDice = false;

    public CameraController(Camera camera) {
        this.camera = camera;
        applyModeInstant(CameraMode.GENERAL);
        saveCurrentView();
    }

    public void saveCurrentView() {
        this.savedPosition.set(camera.getLocation());
        this.savedLookTarget.set(this.currentLookAt);
        this.savedMode = this.currentMode;
    }

    /**
     * Coloca automáticamente la cámara cerca de los dados para capturar
     * la tirada cinemática 3D en primer plano.
     */
    public void focusOnDice() {
        saveCurrentView();
        isFocusedOnDice = true;
        this.targetPosition.set(23.0f, 4.2f, 15.2f);
        this.targetLookAt.set(23.0f, 0.5f, 9.8f);
        this.transitionSpeed = 6.5f;
        this.transitioning = true;
    }

    /**
     * Restaura la cámara suavemente a la perspectiva de combate previa.
     */
    public void restoreSavedView() {
        isFocusedOnDice = false;
        if (savedMode != null) {
            this.currentMode = savedMode;
            this.targetPosition.set(savedMode.position);
            this.targetLookAt.set(savedMode.lookTarget);
        } else {
            this.targetPosition.set(savedPosition);
            this.targetLookAt.set(savedLookTarget);
        }
        this.transitionSpeed = 5.0f;
        this.transitioning = true;
    }

    public boolean isFocusedOnDice() {
        return isFocusedOnDice;
    }

    public void setMode(CameraMode mode) {
        this.currentMode = mode;
        this.targetPosition.set(mode.position);
        this.targetLookAt.set(mode.lookTarget);
        this.transitioning = true;
    }

    public void cycleMode() {
        CameraMode[] modes = CameraMode.values();
        int next = (currentMode.ordinal() + 1) % modes.length;
        setMode(modes[next]);
    }

    public void applyModeInstant(CameraMode mode) {
        this.currentMode = mode;
        this.targetPosition.set(mode.position);
        this.targetLookAt.set(mode.lookTarget);
        this.currentLookAt.set(mode.lookTarget);
        camera.setLocation(mode.position);
        camera.lookAt(mode.lookTarget, Vector3f.UNIT_Y);
        this.transitioning = false;
    }

    public void update(float tpf) {
        if (transitioning) {
            Vector3f curPos = camera.getLocation();
            curPos.interpolateLocal(targetPosition, transitionSpeed * tpf);
            camera.setLocation(curPos);

            currentLookAt.interpolateLocal(targetLookAt, transitionSpeed * tpf);
            camera.lookAt(currentLookAt, Vector3f.UNIT_Y);

            if (curPos.distanceSquared(targetPosition) < 0.05f &&
                currentLookAt.distanceSquared(targetLookAt) < 0.05f) {
                camera.setLocation(targetPosition);
                camera.lookAt(targetLookAt, Vector3f.UNIT_Y);
                transitioning = false;
            }
        }
    }

    public void pan(float dx, float dz) {
        transitioning = false;
        Vector3f loc = camera.getLocation();
        loc.x += dx;
        loc.z += dz;
        camera.setLocation(loc);

        targetLookAt.x += dx;
        targetLookAt.z += dz;
        currentLookAt.set(targetLookAt);
        camera.lookAt(currentLookAt, Vector3f.UNIT_Y);
    }

    public void zoom(float delta) {
        transitioning = false;
        Vector3f dir = camera.getDirection();
        Vector3f loc = camera.getLocation();
        Vector3f newLoc = loc.add(dir.mult(delta));
        if (newLoc.y > 6.0f && newLoc.y < 65.0f) {
            camera.setLocation(newLoc);
        }
    }

    public CameraMode getCurrentMode() {
        return currentMode;
    }
}
