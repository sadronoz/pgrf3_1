package app.object;

import lwjglutils.OGLBuffers;

/**
 * Souřadnicové osy – nelze vybrat myší (ID = 0 = žádný výběr).
 * X = červená, Y = zelená, Z = modrá.
 */
public class Axis extends SceneObject {
    public Axis() {
        super(0); // ID 0 = nelze vybrat
        float[] vb = {
                // X osa – červená
                0, 0, 0,   1, 0, 0,
                1, 0, 0,   1, 0, 0,
                // Y osa – zelená
                0, 0, 0,   0, 1, 0,
                0, 1, 0,   0, 1, 0,
                // Z osa – modrá
                0, 0, 0,   0, 0, 1,
                0, 0, 1,   0, 0, 1,
        };

        int[] ib = {
                0, 1,
                2, 3,
                4, 5
        };

        OGLBuffers.Attrib[] attribs = {
                new OGLBuffers.Attrib("inPosition", 3),
                new OGLBuffers.Attrib("inColor", 3),
        };

        buffers = new OGLBuffers(vb, attribs, ib);
    }
}
