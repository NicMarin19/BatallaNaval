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

import java.util.ArrayList;
import java.util.List;

/**
 * Oceano multicapa con scroll de UV a distintas velocidades, espuma aditiva,
 * brillo solar y oleaje suave. Se puede bajar la calidad desde los ajustes.
 */
public class WaterSurface extends Node {

    private static final float CENTER_X = 23f;
    private static final float CENTER_Z = 10f;

    private static final class Layer {
        Geometry geometry;
        float baseY;
        float width;
        float height;
        float tileW;
        float tileH;
        float speedX;
        float speedZ;
        float scrollX;
        float scrollZ;
        float bobPhase;
        float bobAmp;
    }

    private final List<Layer> layers = new ArrayList<>();
    private final Node detailNode = new Node("WaterDetail");
    private final Node sunGlintNode = new Node("SunGlint");

    private float time = 0f;
    private boolean highQuality = true;

    public WaterSurface(AssetManager assetManager) {
        super("WaterSurface");

        // Capa base opaca: oceano profundo
        Texture baseTex = TextureGenerator.createWaterTexture(512, 512);
        baseTex.setWrap(Texture.WrapMode.Repeat);
        baseTex.setAnisotropicFilter(8);
        addLayer(assetManager, "WaterBase", 4000f, -0.34f, 48, baseTex,
                RenderState.BlendMode.Off, false, 0.55f, 0.22f, 0f, 0f);

        // Capa de oleaje medio
        Texture swellTex = TextureGenerator.createWaterTexture(512, 512,
                new ColorRGBA(0.008f, 0.060f, 0.130f, 1f),
                new ColorRGBA(0.025f, 0.170f, 0.300f, 1f),
                new ColorRGBA(0.320f, 0.580f, 0.740f, 1f),
                78, 1.7);
        swellTex.setWrap(Texture.WrapMode.Repeat);
        addLayer(assetManager, "WaterSwellA", 2400f, -0.27f, 34, swellTex,
                RenderState.BlendMode.Alpha, false, 0.90f, -0.42f, 0.6f, 0.018f);

        // Capa de oleaje fino
        Texture rippleTex = TextureGenerator.createWaterTexture(512, 512,
                new ColorRGBA(0.012f, 0.080f, 0.160f, 1f),
                new ColorRGBA(0.045f, 0.240f, 0.380f, 1f),
                new ColorRGBA(0.520f, 0.780f, 0.920f, 1f),
                52, 4.2);
        rippleTex.setWrap(Texture.WrapMode.Repeat);
        addLayer(assetManager, "WaterSwellB", 2400f, -0.22f, 62, rippleTex,
                RenderState.BlendMode.Alpha, true, -0.72f, 0.88f, 1.8f, 0.014f);

        // Espuma capilar aditiva
        Texture foamTex = TextureGenerator.createFoamTexture(256, 44);
        foamTex.setWrap(Texture.WrapMode.Repeat);
        addLayer(assetManager, "WaterFoam", 2400f, -0.18f, 110, foamTex,
                RenderState.BlendMode.AlphaAdditive, true, 0.50f, 1.25f, 2.9f, 0.010f);

        attachChild(detailNode);
        buildSunGlint(assetManager);
        attachChild(sunGlintNode);
    }

    private void addLayer(AssetManager assetManager, String name, float size, float y,
                          int repeat, Texture texture, RenderState.BlendMode blendMode,
                          boolean detail, float speedX, float speedZ,
                          float bobPhase, float bobAmp) {
        Quad quad = new Quad(size, size);
        quad.scaleTextureCoordinates(new Vector2f(repeat, repeat));

        Geometry geom = new Geometry(name, quad);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", texture);
        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        if (blendMode != RenderState.BlendMode.Off) {
            mat.getAdditionalRenderState().setBlendMode(blendMode);
            mat.getAdditionalRenderState().setDepthWrite(false);
            geom.setQueueBucket(RenderQueue.Bucket.Transparent);
        }
        geom.setMaterial(mat);

        // Echar el quad sobre el plano X-Z
        geom.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));

        Layer layer = new Layer();
        layer.geometry = geom;
        layer.baseY = y;
        layer.width = size;
        layer.height = size;
        layer.tileW = size / repeat;
        layer.tileH = size / repeat;
        layer.speedX = speedX;
        layer.speedZ = speedZ;
        layer.bobPhase = bobPhase;
        layer.bobAmp = bobAmp;

        placeLayer(layer, 0f, 0f, 0f);
        layers.add(layer);

        if (detail) {
            detailNode.attachChild(geom);
        } else {
            attachChild(geom);
        }
    }

    private void placeLayer(Layer layer, float offsetX, float offsetZ, float bob) {
        layer.geometry.setLocalTranslation(
                CENTER_X - layer.width * 0.5f + offsetX,
                layer.baseY + bob,
                CENTER_Z + layer.height * 0.5f + offsetZ);
    }

    private void buildSunGlint(AssetManager assetManager) {
        Texture glow = TextureGenerator.createGlowTexture(256);

        // Reflejo lejano, alineado con la direccion del sol de la cupula.
        sunGlintNode.setLocalTranslation(75f, 0f, -180f);

        // Charco de luz ancho y suave.
        addGlintQuad(assetManager, glow, 280f, 62f, 0f, 0f, 0.13f);
        addGlintQuad(assetManager, glow, 150f, 34f, -6f, 12f, 0.16f);
        addGlintQuad(assetManager, glow, 70f, 16f, 4f, 22f, 0.20f);

        // Destellos finos: pequenas lineas horizontales de espuma iluminada.
        float[][] slivers = {
                {52f, 5f, -26f, 34f, 0.26f},
                {30f, 4f, 10f, 30f, 0.30f},
                {68f, 6f, 22f, 16f, 0.20f},
                {22f, 3f, -38f, 16f, 0.28f},
                {38f, 4f, 34f, 40f, 0.22f},
                {88f, 5f, -10f, 2f, 0.16f},
                {20f, 3f, 16f, -6f, 0.28f},
                {44f, 4f, -30f, -8f, 0.20f}
        };
        for (float[] s : slivers) {
            addGlintQuad(assetManager, glow, s[0], s[1], s[2], s[3], s[4]);
        }
    }

    private void addGlintQuad(AssetManager assetManager, Texture glow,
                              float w, float h, float x, float z, float alpha) {
        Quad quad = new Quad(w, h);
        Geometry geom = new Geometry("SunGlintQuad", quad);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", glow);
        mat.setColor("Color", new ColorRGBA(1f, 0.94f, 0.82f, alpha));
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);
        mat.getAdditionalRenderState().setDepthWrite(false);
        geom.setMaterial(mat);
        geom.setQueueBucket(RenderQueue.Bucket.Transparent);
        geom.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        geom.setLocalTranslation(-w * 0.5f + x, -0.155f, h * 0.5f + z);
        sunGlintNode.attachChild(geom);
    }

    public void update(float tpf) {
        time += tpf;

        for (Layer layer : layers) {
            layer.scrollX += layer.speedX * tpf;
            layer.scrollZ += layer.speedZ * tpf;

            // Desplazar exactamente un numero entero de tiles mantiene la
            // textura seamless (evita el salto visible del patron).
            float wrapX = layer.tileW * 8f;
            float wrapZ = layer.tileH * 8f;
            if (layer.scrollX > wrapX) layer.scrollX -= wrapX * 2f;
            if (layer.scrollX < -wrapX) layer.scrollX += wrapX * 2f;
            if (layer.scrollZ > wrapZ) layer.scrollZ -= wrapZ * 2f;
            if (layer.scrollZ < -wrapZ) layer.scrollZ += wrapZ * 2f;

            float bob = FastMath.sin(time * 0.55f + layer.bobPhase) * layer.bobAmp;
            placeLayer(layer, layer.scrollX, layer.scrollZ, bob);
        }

        // Latido muy sutil del brillo solar
        float pulse = 0.92f + 0.08f * FastMath.sin(time * 0.8f);
        sunGlintNode.setLocalScale(pulse, 1f, pulse);
    }

    public void setHighQuality(boolean highQuality) {
        this.highQuality = highQuality;
        detailNode.setCullHint(highQuality ? CullHint.Inherit : CullHint.Always);
        sunGlintNode.setCullHint(highQuality ? CullHint.Inherit : CullHint.Always);
    }

    public boolean isHighQuality() {
        return highQuality;
    }
}
