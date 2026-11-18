package com.batallanaval.view;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class WedgeMeshBuilder {

    /**
     * Creates a pointed naval ship bow mesh.
     * The bow starts at x = 0 with base width (-halfWidth to +halfWidth) and height (0 to height),
     * and tapers forward to a sharp vertical cutwater prow at x = length.
     */
    public static Mesh createBowMesh(float length, float height, float halfWidth) {
        Mesh mesh = new Mesh();

        // 6 Vertices:
        // 0: Bow tip bottom (length, 0, 0)
        // 1: Bow tip top    (length, height, 0)
        // 2: Port bottom    (0, 0, -halfWidth)
        // 3: Port top       (0, height, -halfWidth)
        // 4: Starboard bot  (0, 0, halfWidth)
        // 5: Starboard top  (0, height, halfWidth)

        float[] pos = new float[] {
            // Front tip
            length, 0f, 0f,           // 0
            length, height, 0f,       // 1
            // Port (Left / -Z)
            0f, 0f, -halfWidth,       // 2
            0f, height, -halfWidth,   // 3
            // Starboard (Right / +Z)
            0f, 0f, halfWidth,        // 4
            0f, height, halfWidth     // 5
        };

        // Normals (approximate outwardly pointing)
        float[] norm = new float[] {
            0.7f, 0f, 0f,
            0.7f, 0.4f, 0f,
            -0.3f, 0f, -0.9f,
            -0.3f, 0.4f, -0.9f,
            -0.3f, 0f, 0.9f,
            -0.3f, 0.4f, 0.9f
        };

        float[] tex = new float[] {
            1f, 0f,
            1f, 1f,
            0f, 0f,
            0f, 1f,
            0f, 0f,
            0f, 1f
        };

        // Triangles
        short[] indices = new short[] {
            // Port side (0, 1, 3, 2)
            0, 1, 3,
            0, 3, 2,

            // Starboard side (4, 5, 1, 0)
            4, 5, 1,
            4, 1, 0,

            // Top deck triangle (1, 5, 3)
            1, 5, 3,

            // Bottom hull triangle (0, 2, 4)
            0, 2, 4,

            // Aft joint back face (2, 3, 5, 4)
            2, 3, 5,
            2, 5, 4
        };

        FloatBuffer pb = BufferUtils.createFloatBuffer(pos);
        FloatBuffer nb = BufferUtils.createFloatBuffer(norm);
        FloatBuffer tb = BufferUtils.createFloatBuffer(tex);
        ShortBuffer ib = BufferUtils.createShortBuffer(indices);

        mesh.setBuffer(Type.Position, 3, pb);
        mesh.setBuffer(Type.Normal, 3, nb);
        mesh.setBuffer(Type.TexCoord, 2, tb);
        mesh.setBuffer(Type.Index, 3, ib);

        mesh.updateBound();
        return mesh;
    }
}
