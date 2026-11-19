package app.object;

import lwjglutils.OGLBuffers;

/**
 * UV sféra generovaná procedurálně.
 * Používá se jako vizualizace zdroje světla – nelze ji vybrat (ID = 0).
 */
public class Sphere extends SceneObject {

    public Sphere(int stacks, int slices) {
        super(0); // ID 0 = nelze vybrat

        int vertCount = (stacks + 1) * (slices + 1);
        // pos(3) + color(3) + texCoord(2) + normal(3) = 11 floatů na vrchol
        float[] vb = new float[vertCount * 11];

        int idx = 0;
        for (int i = 0; i <= stacks; i++) {
            double phi = Math.PI / 2 - i * Math.PI / stacks; // [-π/2, π/2]
            float y   = (float) Math.sin(phi);
            float r   = (float) Math.cos(phi);

            for (int j = 0; j <= slices; j++) {
                double theta = j * 2 * Math.PI / slices;
                float x = r * (float) Math.cos(theta);
                float z = r * (float) Math.sin(theta);

                // Pozice
                vb[idx++] = x;
                vb[idx++] = y;
                vb[idx++] = z;
                // Barva (teplá žluté-bílá = světlo)
                vb[idx++] = 1.0f;
                vb[idx++] = 1.0f;
                vb[idx++] = 0.8f;
                // UV
                vb[idx++] = (float) j / slices;
                vb[idx++] = (float) i / stacks;
                // Normála (na jednotkové sféře = pozice)
                vb[idx++] = x;
                vb[idx++] = y;
                vb[idx++] = z;
            }
        }

        // Index buffer – trojúhelníky
        int triCount = stacks * slices * 2;
        int[] ib = new int[triCount * 3];
        int ibIdx = 0;
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                int topLeft     = i * (slices + 1) + j;
                int topRight    = topLeft + 1;
                int bottomLeft  = topLeft + (slices + 1);
                int bottomRight = bottomLeft + 1;

                // Trojúhelník 1
                ib[ibIdx++] = topLeft;
                ib[ibIdx++] = bottomLeft;
                ib[ibIdx++] = topRight;
                // Trojúhelník 2
                ib[ibIdx++] = topRight;
                ib[ibIdx++] = bottomLeft;
                ib[ibIdx++] = bottomRight;
            }
        }

        OGLBuffers.Attrib[] attribs = {
                new OGLBuffers.Attrib("inPosition", 3),
                new OGLBuffers.Attrib("inColor",    3),
                new OGLBuffers.Attrib("inTexCoord", 2),
                new OGLBuffers.Attrib("inNormal",   3),
        };

        buffers = new OGLBuffers(vb, attribs, ib);
    }
}
