package com.batallanaval.view;

import com.batallanaval.model.ShipType;
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
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

import java.util.ArrayList;
import java.util.List;

/**
 * Flota decorativa que patrulla ambos flancos del mapa. Usa los modelos
 * detallados de {@link ShipMeshFactory}, con estela de espuma, balanceo,
 * cabeceo y luces de navegacion intermitentes.
 */
public class DecorativeFleet extends Node {

    private static final class Escort {
        Node node;
        Vector3f home;
        Quaternion baseRotation;
        Vector3f heading;
        float bobPhase;
        float bobSpeed;
        float rollPhase;
        float pitchPhase;
        float driftPhase;
        float driftAmp;
        float driftSpeed;
        final List<Geometry> lights = new ArrayList<>();
        float lightSpeed;
        float lightPhase;
    }

    private final AssetManager assetManager;
    private final ShipMeshFactory shipFactory;
    private final List<Escort> escorts = new ArrayList<>();
    private float time = 0f;

    public DecorativeFleet(AssetManager assetManager, ShipMeshFactory shipFactory) {
        super("DecorativeFleet");
        this.assetManager = assetManager;
        this.shipFactory = shipFactory;
        buildFleet();
    }

    private void buildFleet() {
        // x, z, escala, rumbo (grados), tipo
        Object[][] specs = {
                // Flanco occidental
                {-26f, 2f, 1.20f, 12f, ShipType.ACORAZADO},
                {-36f, 16f, 0.90f, -8f, ShipType.CRUCERO},
                {-22f, 27f, 0.78f, 26f, ShipType.DESTRUCTOR},
                {-48f, 8f, 0.85f, 3f, ShipType.CRUCERO},
                {-41f, 33f, 0.68f, -16f, ShipType.DESTRUCTOR},
                {-15f, -24f, 0.68f, 8f, ShipType.DESTRUCTOR},
                {-31f, 42f, 1.25f, 20f, ShipType.ACORAZADO},
                {-17f, 36f, 0.85f, 4f, ShipType.DESTRUCTOR},

                // Flanco oriental
                {60f, -3f, 1.30f, 186f, ShipType.PORTAVIONES},
                {72f, 18f, 0.95f, 174f, ShipType.ACORAZADO},
                {57f, 30f, 0.78f, 167f, ShipType.DESTRUCTOR},
                {85f, 9f, 0.78f, 191f, ShipType.CRUCERO},
                {70f, 34f, 0.65f, 171f, ShipType.DESTRUCTOR},
                {73f, 46f, 1.20f, 172f, ShipType.ACORAZADO},
                {56f, 44f, 0.85f, 160f, ShipType.DESTRUCTOR},

                // Linea de horizonte
                {8f, -38f, 0.78f, 6f, ShipType.CRUCERO},
                {28f, -45f, 0.70f, 2f, ShipType.DESTRUCTOR},
                {46f, -39f, 0.72f, 11f, ShipType.ACORAZADO},
                {-18f, -35f, 0.62f, 4f, ShipType.CRUCERO},
                {66f, -33f, 0.60f, 8f, ShipType.ACORAZADO}
        };

        for (int i = 0; i < specs.length; i++) {
            Object[] spec = specs[i];
            float x = (Float) spec[0];
            float z = (Float) spec[1];
            float scale = (Float) spec[2];
            float headingDeg = (Float) spec[3];
            ShipType type = (ShipType) spec[4];

            buildEscort(i, type, new Vector3f(x, 0f, z), scale, headingDeg);
        }
    }

    private void buildEscort(int index, ShipType type, Vector3f home, float scale, float headingDeg) {
        Escort escort = new Escort();
        escort.home = home;
        escort.heading = new Vector3f(FastMath.cos(headingDeg * FastMath.DEG_TO_RAD), 0f,
                -FastMath.sin(headingDeg * FastMath.DEG_TO_RAD)).normalizeLocal();

        float headingRad = headingDeg * FastMath.DEG_TO_RAD;
        escort.baseRotation = new Quaternion().fromAngleAxis(headingRad, Vector3f.UNIT_Y);

        float length = type.getSize() * ShipMeshFactory.CELL_SIZE;

        Node pivot = new Node("Escort_" + index + "_" + type.name());
        pivot.setLocalScale(scale);

        Node model = shipFactory.createShipNode(type);
        model.setLocalTranslation(-length * 0.5f, 0f, 0f);
        pivot.attachChild(model);

        // Estela de espuma (detras de la popa)
        pivot.attachChild(createWake(length));

        // Luces de navegacion (roja a babor, verde a estribor)
        float halfBeam = length * 0.14f;
        escort.lights.add(addNavLight(pivot, new Vector3f(length * 0.34f, 0.55f, halfBeam),
                new ColorRGBA(1.0f, 0.18f, 0.12f, 1f)));
        escort.lights.add(addNavLight(pivot, new Vector3f(length * 0.34f, 0.55f, -halfBeam),
                new ColorRGBA(0.20f, 1.0f, 0.35f, 1f)));
        escort.lights.add(addNavLight(pivot, new Vector3f(-length * 0.10f, 1.35f, 0f),
                new ColorRGBA(1.0f, 0.95f, 0.75f, 1f)));

        escort.node = pivot;
        escort.bobPhase = (index * 1.37f) % FastMath.TWO_PI;
        escort.bobSpeed = 0.55f + (index % 5) * 0.08f;
        escort.rollPhase = (index * 2.11f) % FastMath.TWO_PI;
        escort.pitchPhase = (index * 0.77f) % FastMath.TWO_PI;
        escort.driftPhase = (index * 1.91f) % FastMath.TWO_PI;
        escort.driftAmp = 1.2f + (index % 4) * 0.6f;
        escort.driftSpeed = 0.10f + (index % 3) * 0.035f;
        escort.lightSpeed = 0.7f + (index % 4) * 0.18f;
        escort.lightPhase = (index * 0.9f) % FastMath.TWO_PI;

        applyPose(escort, 0f);

        attachChild(pivot);
        escorts.add(escort);
    }

    private Geometry addNavLight(Node pivot, Vector3f position, ColorRGBA color) {
        Box box = new Box(0.06f, 0.06f, 0.06f);
        Geometry geom = new Geometry("NavLight", box);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        geom.setMaterial(mat);
        geom.setLocalTranslation(position);
        pivot.attachChild(geom);
        return geom;
    }

    private Geometry createWake(float shipLength) {
        float wakeWidth = shipLength * 0.50f;
        float wakeLength = shipLength * 0.95f;

        Quad quad = new Quad(wakeWidth, wakeLength);
        Geometry wake = new Geometry("EnemyWake", quad);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture tex = TextureGenerator.createWakeTexture(128, 256);
        tex.setWrap(Texture.WrapMode.Clamp);
        mat.setTexture("ColorMap", tex);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        mat.getAdditionalRenderState().setDepthWrite(false);
        wake.setMaterial(mat);
        wake.setQueueBucket(RenderQueue.Bucket.Transparent);

        // Orientar la estela: ancho local -> +Z, largo local -> -X (hacia popa)
        Quaternion qz = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Z);
        Quaternion qy = new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_Y);
        wake.setLocalRotation(qz.mult(qy));
        wake.setLocalTranslation(-shipLength * 0.5f, 0.03f, -wakeWidth * 0.5f);
        return wake;
    }

    private void applyPose(Escort escort, float lateralOffset) {
        Vector3f position = escort.home.add(escort.heading.mult(lateralOffset));

        float bob = FastMath.sin(time * escort.bobSpeed + escort.bobPhase) * 0.11f;
        position.y += bob - 0.06f;

        float roll = FastMath.sin(time * escort.bobSpeed * 0.8f + escort.rollPhase) * 0.035f;
        float pitch = FastMath.sin(time * escort.bobSpeed * 0.6f + escort.pitchPhase) * 0.020f;

        Quaternion rollQ = new Quaternion().fromAngleAxis(roll, Vector3f.UNIT_X);
        Quaternion pitchQ = new Quaternion().fromAngleAxis(pitch, Vector3f.UNIT_Z);

        escort.node.setLocalTranslation(position);
        escort.node.setLocalRotation(escort.baseRotation.mult(rollQ).mult(pitchQ));
    }

    public void update(float tpf) {
        time += tpf;

        for (Escort escort : escorts) {
            float drift = FastMath.sin(time * escort.driftSpeed * FastMath.TWO_PI + escort.driftPhase)
                    * escort.driftAmp;
            applyPose(escort, drift);

            // Parpadeo de luces: las rojas/verdes alternan con la blanca
            for (int i = 0; i < escort.lights.size(); i++) {
                Geometry light = escort.lights.get(i);
                float phaseOffset = i == 2 ? FastMath.PI : 0f;
                boolean on = FastMath.sin(time * escort.lightSpeed * FastMath.TWO_PI
                        + escort.lightPhase + phaseOffset) > -0.15f;
                light.setCullHint(on ? CullHint.Inherit : CullHint.Always);
            }
        }
    }
}
