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
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;

/**
 * Cupula de cielo con degradado vertical y un sol lejano con halo.
 * Se renderiza en el bucket Sky para no escribir profundidad.
 */
public class SkyDome extends Node {

    private static final Vector3f CENTER = new Vector3f(23f, 0f, 10f);

    private final Node sunNode;
    private final Geometry sunCore;
    private float time = 0f;

    public SkyDome(AssetManager assetManager) {
        super("SkyDome");

        Sphere sphere = new Sphere(32, 64, 620f);
        Geometry dome = new Geometry("SkySphere", sphere);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture skyTex = TextureGenerator.createSkyGradientTexture(256);
        skyTex.setWrap(Texture.WrapMode.Clamp);
        mat.setTexture("ColorMap", skyTex);
        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Front);
        mat.getAdditionalRenderState().setDepthWrite(false);
        dome.setMaterial(mat);
        dome.setQueueBucket(RenderQueue.Bucket.Sky);
        dome.setLocalTranslation(CENTER);
        attachChild(dome);

        sunNode = new Node("SunNode");
        sunNode.setLocalTranslation(130f, 165f, -430f);

        sunCore = addGlow(assetManager, 46f, 1f);
        addGlow(assetManager, 130f, 0.55f);
        addGlow(assetManager, 320f, 0.22f);

        attachChild(sunNode);
    }

    private Geometry addGlow(AssetManager assetManager, float size, float alphaScale) {
        Quad quad = new Quad(size, size);
        Geometry geom = new Geometry("SunGlow", quad);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", TextureGenerator.createGlowTexture(128));
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);
        mat.getAdditionalRenderState().setDepthWrite(false);
        geom.setMaterial(mat);
        geom.setQueueBucket(RenderQueue.Bucket.Transparent);
        geom.setLocalTranslation(-size * 0.5f, -size * 0.5f, 0f);
        geom.setLocalScale(alphaScale);

        BillboardControl billboard = new BillboardControl();
        billboard.setAlignment(BillboardControl.Alignment.Screen);
        geom.addControl(billboard);

        sunNode.attachChild(geom);
        return geom;
    }

    public void update(float tpf) {
        time += tpf;
        float pulse = 1f + 0.05f * FastMath.sin(time * 0.7f);
        sunCore.setLocalScale(pulse);
    }
}
