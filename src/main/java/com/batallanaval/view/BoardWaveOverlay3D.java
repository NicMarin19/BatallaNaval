package com.batallanaval.view;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

/**
 * Capa de olas dinámicas suaves que cubre el sector del tablero.
 * Genera pequeñas ondulaciones marinas translúcidas y estelas de espuma.
 */
public class BoardWaveOverlay3D extends Node {

    private final Geometry waveGeom;
    private final Geometry foamGeom;
    private final Quad waveQuad;
    private final Quad foamQuad;
    private float waveTime = 0f;

    public BoardWaveOverlay3D(AssetManager assetManager, float boardWidth, float boardDepth) {
        super("BoardWaveOverlay");

        float margin = 1.2f;
        float totalW = boardWidth + margin * 2f;
        float totalD = boardDepth + margin * 2f;

        // Wave Layer
        waveQuad = new Quad(totalW, totalD);
        waveQuad.scaleTextureCoordinates(new Vector2f(6.0f, 6.0f));

        waveGeom = new Geometry("BoardWaveLayer", waveQuad);
        Material matWave = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture waveTex = TextureGenerator.createWaterTexture(256, 256,
                new ColorRGBA(0.02f, 0.12f, 0.22f, 0.40f),
                new ColorRGBA(0.08f, 0.32f, 0.50f, 0.65f),
                new ColorRGBA(0.35f, 0.70f, 0.88f, 0.80f),
                65, 0.5);
        waveTex.setWrap(Texture.WrapMode.Repeat);
        matWave.setTexture("ColorMap", waveTex);
        matWave.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        waveGeom.setMaterial(matWave);

        // Lay flat
        waveGeom.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        waveGeom.setLocalTranslation(-margin, 0.035f, boardDepth + margin);
        attachChild(waveGeom);

        // Subtle foam shimmer layer
        foamQuad = new Quad(totalW, totalD);
        foamQuad.scaleTextureCoordinates(new Vector2f(8.0f, 8.0f));

        foamGeom = new Geometry("BoardFoamLayer", foamQuad);
        Material matFoam = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture foamTex = TextureGenerator.createFoamTexture(128, 45);
        foamTex.setWrap(Texture.WrapMode.Repeat);
        matFoam.setTexture("ColorMap", foamTex);
        matFoam.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);
        foamGeom.setMaterial(matFoam);

        foamGeom.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        foamGeom.setLocalTranslation(-margin, 0.040f, boardDepth + margin);
        attachChild(foamGeom);

        setQueueBucket(RenderQueue.Bucket.Transparent);
    }

    public void update(float tpf) {
        waveTime += tpf;

        // Animate wave vertical undulating breathing
        float waveHeave = 0.035f + 0.015f * FastMath.sin(waveTime * 1.8f);
        Vector3f loc = waveGeom.getLocalTranslation();
        loc.y = waveHeave;
        waveGeom.setLocalTranslation(loc);

        // Foam layer offset
        Vector3f foamLoc = foamGeom.getLocalTranslation();
        foamLoc.y = waveHeave + 0.005f;
        foamGeom.setLocalTranslation(foamLoc);
    }
}
