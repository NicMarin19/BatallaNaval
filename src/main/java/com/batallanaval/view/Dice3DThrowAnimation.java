package com.batallanaval.view;

import com.batallanaval.audio.SoundManager;
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
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

import java.util.Random;
import java.util.function.Consumer;

/**
 * Animación cinemática 3D que lanza dos dados con física de parábola, giros
 * tridimensionales y rebotes hacia el centro exacto entre los dos tableros.
 * Al asentarse, revela las caras ganadoras y la mejora de combate en 3D.
 */
public class Dice3DThrowAnimation extends Node {

    private final AssetManager assetManager;
    private final Camera camera;
    private final int die1Value;
    private final int die2Value;
    private final double probability;
    private final int percent;
    private final Consumer<Double> onComplete;

    private final Die3DModel die1Model;
    private final Die3DModel die2Model;

    // Trayectorias
    private final Vector3f start1 = new Vector3f(11.0f, 13.5f, 22.0f);
    private final Vector3f landing1 = new Vector3f(21.8f, 0.45f, 9.4f);
    private final Vector3f bounce1 = new Vector3f(22.2f, 0.45f, 9.8f);

    private final Vector3f start2 = new Vector3f(13.5f, 14.5f, 23.5f);
    private final Vector3f landing2 = new Vector3f(24.2f, 0.45f, 10.6f);
    private final Vector3f bounce2 = new Vector3f(23.8f, 0.45f, 10.2f);

    // Efectos de agua y auras
    private Geometry splash1;
    private Geometry splash2;
    private Geometry aura1;
    private Geometry aura2;
    private Node bannerNode;
    private BitmapText titleText;
    private BitmapText resultText;
    private BitmapText buffText;

    private float timer = 0f;
    private static final float FLIGHT_END = 1.0f;
    private static final float BOUNCE1_END = 1.45f;
    private static final float BOUNCE2_END = 1.85f;
    private static final float TOTAL_DURATION = 4.2f;

    private boolean splashSoundPlayed = false;
    private boolean settleSoundPlayed = false;
    private boolean settledCallbackFired = false;
    private boolean completed = false;

    public interface DiceSettledListener {
        void onSettled(int die1, int die2, double probability);
    }
    private DiceSettledListener settledListener;

    public Dice3DThrowAnimation(AssetManager assetManager, Camera camera,
                                int die1, int die2, Consumer<Double> onComplete) {
        super("Dice3DThrowAnimation");
        this.assetManager = assetManager;
        this.camera = camera;
        this.die1Value = die1;
        this.die2Value = die2;
        this.probability = (die1 + die2) / 12.0;
        this.percent = (int) Math.round(probability * 100);
        this.onComplete = onComplete;

        die1Model = new Die3DModel(assetManager);
        die2Model = new Die3DModel(assetManager);

        attachChild(die1Model);
        attachChild(die2Model);

        buildWaterRipples();
        buildUpgradeBanner();

        setQueueBucket(RenderQueue.Bucket.Transparent);
    }

    private void buildWaterRipples() {
        Material matSplash = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matSplash.setColor("Color", new ColorRGBA(0.4f, 0.85f, 1.0f, 0.7f));
        matSplash.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        Cylinder c1 = new Cylinder(16, 16, 1.2f, 0.02f, true);
        splash1 = new Geometry("Splash1", c1);
        splash1.setMaterial(matSplash);
        splash1.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        splash1.setLocalTranslation(landing1.x, 0.08f, landing1.z);
        splash1.setCullHint(CullHint.Always);
        attachChild(splash1);

        splash2 = new Geometry("Splash2", c1);
        splash2.setMaterial(matSplash);
        splash2.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        splash2.setLocalTranslation(landing2.x, 0.08f, landing2.z);
        splash2.setCullHint(CullHint.Always);
        attachChild(splash2);

        // Auras doradas al asentarse
        Material matAura = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture auraTex = TextureGenerator.createAuraRingTexture(128);
        matAura.setTexture("ColorMap", auraTex);
        matAura.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);

        Quad auraQ = new Quad(2.8f, 2.8f);
        aura1 = new Geometry("DiceAura1", auraQ);
        aura1.setMaterial(matAura);
        aura1.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        aura1.setLocalTranslation(bounce1.x - 1.4f, 0.12f, bounce1.z + 1.4f);
        aura1.setCullHint(CullHint.Always);
        attachChild(aura1);

        aura2 = new Geometry("DiceAura2", auraQ);
        aura2.setMaterial(matAura);
        aura2.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        aura2.setLocalTranslation(bounce2.x - 1.4f, 0.12f, bounce2.z + 1.4f);
        aura2.setCullHint(CullHint.Always);
        attachChild(aura2);
    }

    private void buildUpgradeBanner() {
        bannerNode = new Node("DiceBanner3D");
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        titleText = new BitmapText(font, false);
        titleText.setSize(0.55f);
        titleText.setText("[HABILIDAD DADOS DE GUERRA]");
        titleText.setColor(new ColorRGBA(1.0f, 0.85f, 0.20f, 1f));
        titleText.setQueueBucket(RenderQueue.Bucket.Transparent);

        resultText = new BitmapText(font, false);
        resultText.setSize(0.85f);
        resultText.setText("TIRADA: " + die1Value + " + " + die2Value + " = " + (die1Value + die2Value));
        resultText.setColor(ColorRGBA.White);
        resultText.setQueueBucket(RenderQueue.Bucket.Transparent);

        buffText = new BitmapText(font, false);
        buffText.setSize(0.68f);
        buffText.setText("★ MEJORA: " + percent + "% PROBABILIDAD DE DOBLE DISPARO ★");
        buffText.setColor(new ColorRGBA(0.35f, 1.0f, 0.45f, 1f));
        buffText.setQueueBucket(RenderQueue.Bucket.Transparent);

        float textW = Math.max(titleText.getLineWidth(), Math.max(resultText.getLineWidth(), buffText.getLineWidth()));
        float plateW = textW + 1.6f;
        float plateH = 2.4f;

        Quad plateQuad = new Quad(plateW, plateH);
        Geometry plate = new Geometry("BannerPlate", plateQuad);
        Material matPlate = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture bgTex = TextureGenerator.createButtonTexture((int) (plateW * 40), (int) (plateH * 40),
                new ColorRGBA(0.02f, 0.05f, 0.12f, 0.94f),
                new ColorRGBA(1.0f, 0.80f, 0.15f, 0.98f));
        matPlate.setTexture("ColorMap", bgTex);
        matPlate.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        plate.setMaterial(matPlate);
        plate.setLocalTranslation(-plateW * 0.5f, -plateH * 0.5f, -0.02f);
        plate.setQueueBucket(RenderQueue.Bucket.Transparent);
        bannerNode.attachChild(plate);

        titleText.setLocalTranslation(-titleText.getLineWidth() * 0.5f, plateH * 0.5f - 0.22f, 0.02f);
        bannerNode.attachChild(titleText);

        resultText.setLocalTranslation(-resultText.getLineWidth() * 0.5f, 0.18f, 0.02f);
        bannerNode.attachChild(resultText);

        buffText.setLocalTranslation(-buffText.getLineWidth() * 0.5f, -0.42f, 0.02f);
        bannerNode.attachChild(buffText);

        // Posicionar en el centro de los dos tableros (X=23, Z=10) a 3.4m de altura
        bannerNode.setLocalTranslation(23.0f, 3.4f, 10.0f);
        bannerNode.setCullHint(CullHint.Always);
        attachChild(bannerNode);
    }

    public void update(float tpf) {
        timer += tpf;

        if (timer < FLIGHT_END) {
            // Fase 1: Vuelo parabólico hacia el centro de los tableros
            float t = timer / FLIGHT_END;
            updateDieFlight(die1Model, start1, landing1, t, 11.0f, 14.0f, 18.0f);
            updateDieFlight(die2Model, start2, landing2, t, 12.0f, 16.0f, 12.0f);

        } else if (timer < BOUNCE1_END) {
            if (!splashSoundPlayed) {
                splashSoundPlayed = true;
                SoundManager.getInstance().play(SoundManager.SoundType.WATER_SPLASH);
                splash1.setCullHint(CullHint.Never);
                splash2.setCullHint(CullHint.Never);
            }

            // Expandir anillos de salpicadura de agua
            float st = (timer - FLIGHT_END) / (BOUNCE1_END - FLIGHT_END);
            splash1.setLocalScale(1.0f + st * 1.8f);
            splash2.setLocalScale(1.0f + st * 1.8f);

            // Fase 2: Primer rebote
            float bt = (timer - FLIGHT_END) / (BOUNCE1_END - FLIGHT_END);
            updateDieFlight(die1Model, landing1, bounce1, bt, 1.8f, 5.0f, 6.0f);
            updateDieFlight(die2Model, landing2, bounce2, bt, 1.6f, 6.0f, 4.0f);

        } else if (timer < BOUNCE2_END) {
            // Fase 3: Segundo rebote pequeño y alineación final
            float bt2 = (timer - BOUNCE1_END) / (BOUNCE2_END - BOUNCE1_END);
            float yArc1 = bounce1.y + FastMath.sin(bt2 * FastMath.PI) * 0.45f;
            float yArc2 = bounce2.y + FastMath.sin(bt2 * FastMath.PI) * 0.38f;

            die1Model.setLocalTranslation(bounce1.x, yArc1, bounce1.z);
            die2Model.setLocalTranslation(bounce2.x, yArc2, bounce2.z);

            // Interpolación hacia la orientación que deja la cara ganadora arriba
            Quaternion finalRot1 = Die3DModel.getRotationForFace(die1Value);
            Quaternion finalRot2 = Die3DModel.getRotationForFace(die2Value);
            die1Model.setLocalRotation(new Quaternion().slerp(die1Model.getLocalRotation(), finalRot1, bt2));
            die2Model.setLocalRotation(new Quaternion().slerp(die2Model.getLocalRotation(), finalRot2, bt2));

        } else {
            // Fase 4: Asentados en el centro del canal marino con auras y cartel holográfico
            die1Model.setLocalTranslation(bounce1);
            die2Model.setLocalTranslation(bounce2);
            die1Model.setLocalRotation(Die3DModel.getRotationForFace(die1Value));
            die2Model.setLocalRotation(Die3DModel.getRotationForFace(die2Value));

            if (!settleSoundPlayed) {
                settleSoundPlayed = true;
                SoundManager.getInstance().play(SoundManager.SoundType.HIT_ALERT);
                bannerNode.setCullHint(CullHint.Never);
                aura1.setCullHint(CullHint.Never);
                aura2.setCullHint(CullHint.Never);
            }

            if (!settledCallbackFired) {
                settledCallbackFired = true;
                if (settledListener != null) {
                    settledListener.onSettled(die1Value, die2Value, probability);
                }
            }

            // Pulso y desvanecimiento de las auras
            float auraTime = timer - BOUNCE2_END;
            float auraScale = 1.0f + auraTime * 0.45f;
            aura1.setLocalScale(auraScale);
            aura2.setLocalScale(auraScale);

            // Orientar cartel hacia la cámara activa
            if (camera != null) {
                bannerNode.lookAt(camera.getLocation(), Vector3f.UNIT_Y);
            }

            // Pulso en el texto de mejora
            float pulse = 0.85f + 0.15f * FastMath.sin(auraTime * 6f);
            buffText.setColor(new ColorRGBA(0.35f * pulse, 1.0f * pulse, 0.45f * pulse, 1f));
        }

        // Si hay listener de tabla táctica, permanece en reposo hasta que el jugador pulse continuar
        float maxDuration = (settledListener != null) ? 180.0f : TOTAL_DURATION;
        if (timer >= maxDuration && !completed) {
            finishAndRemove();
        }
    }

    private void updateDieFlight(Die3DModel die, Vector3f from, Vector3f to, float t,
                                float arcH, float spinX, float spinZ) {
        float x = FastMath.interpolateLinear(t, from.x, to.x);
        float z = FastMath.interpolateLinear(t, from.z, to.z);
        float y = from.y + (to.y - from.y) * t + FastMath.sin(t * FastMath.PI) * arcH;

        die.setLocalTranslation(x, y, z);
        die.rotate(spinX * 0.05f, (spinX + spinZ) * 0.04f, spinZ * 0.05f);
    }

    public void setSettledListener(DiceSettledListener listener) {
        this.settledListener = listener;
        if (settledCallbackFired && listener != null) {
            listener.onSettled(die1Value, die2Value, probability);
        }
    }

    public void finishAndRemove() {
        if (!completed) {
            completed = true;
            if (onComplete != null) {
                onComplete.accept(probability);
            }
            removeFromParent();
        }
    }

    public boolean isCompleted() {
        return completed;
    }

    /**
     * Modelo 3D de un dado cúbico con las 6 caras texturizadas y rotaciones canónicas.
     */
    public static class Die3DModel extends Node {
        private final Geometry cubeBody;

        public Die3DModel(AssetManager assetManager) {
            super("Die3DModel");

            float half = 0.42f;
            Box box = new Box(half, half, half);
            cubeBody = new Geometry("DieCubeBody", box);
            Material matBody = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            matBody.setBoolean("UseMaterialColors", true);
            matBody.setColor("Diffuse", new ColorRGBA(0.96f, 0.95f, 0.92f, 1f));
            matBody.setColor("Ambient", new ColorRGBA(0.45f, 0.44f, 0.42f, 1f));
            matBody.setColor("Specular", ColorRGBA.White.mult(0.3f));
            matBody.setFloat("Shininess", 16f);
            cubeBody.setMaterial(matBody);
            attachChild(cubeBody);

            // Caras texturizadas (+Y:1, -Y:6, +Z:2, -Z:5, +X:3, -X:4)
            attachFaceQuad(assetManager, 1, new Vector3f(-half, half + 0.002f, half), -FastMath.HALF_PI, Vector3f.UNIT_X, half * 2f);
            attachFaceQuad(assetManager, 6, new Vector3f(-half, -half - 0.002f, -half), FastMath.HALF_PI, Vector3f.UNIT_X, half * 2f);
            attachFaceQuad(assetManager, 2, new Vector3f(-half, -half, half + 0.002f), 0f, Vector3f.UNIT_X, half * 2f);
            attachFaceQuad(assetManager, 5, new Vector3f(half, -half, -half - 0.002f), FastMath.PI, Vector3f.UNIT_Y, half * 2f);
            attachFaceQuad(assetManager, 3, new Vector3f(half + 0.002f, -half, half), FastMath.HALF_PI, Vector3f.UNIT_Y, half * 2f);
            attachFaceQuad(assetManager, 4, new Vector3f(-half - 0.002f, -half, -half), -FastMath.HALF_PI, Vector3f.UNIT_Y, half * 2f);
        }

        private void attachFaceQuad(AssetManager assetManager, int face, Vector3f pos, float angle, Vector3f axis, float size) {
            Quad q = new Quad(size, size);
            Geometry g = new Geometry("Face_" + face, q);
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            Texture tex = TextureGenerator.createDiceFaceTexture(face, 128);
            mat.setTexture("ColorMap", tex);
            mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            g.setMaterial(mat);

            if (angle != 0f) {
                g.setLocalRotation(new Quaternion().fromAngleAxis(angle, axis));
            }
            g.setLocalTranslation(pos);
            attachChild(g);
        }

        /**
         * Devuelve la rotación necesaria para que la cara deseada apunte exactamente hacia arriba (+Y).
         */
        public static Quaternion getRotationForFace(int face) {
            Quaternion q = new Quaternion();
            switch (face) {
                case 1:
                    return Quaternion.IDENTITY;
                case 6:
                    return q.fromAngleAxis(FastMath.PI, Vector3f.UNIT_X);
                case 2:
                    return q.fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);
                case 5:
                    return q.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X);
                case 3:
                    return q.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Z);
                case 4:
                    return q.fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_Z);
                default:
                    return Quaternion.IDENTITY;
            }
        }
    }
}
