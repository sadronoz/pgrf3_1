package app.object;

import lwjglutils.OGLBuffers;

public class Quad extends SceneObject {
    public Quad(int id) {
        super(id);
        // pos(3) + color(3) + texCoord(2) + normal(3) = 11 floatů na vrchol
        float[] vb = {
                -1f, -1f, 0f,   1, 0, 0,   0, 0,   0, 0, 1,
                 1f, -1f, 0f,   0, 1, 0,   1, 0,   0, 0, 1,
                 1f,  1f, 0f,   0, 0, 1,   1, 1,   0, 0, 1,
                -1f,  1f, 0f,   0, 1, 0,   0, 1,   0, 0, 1,
        };

        int[] ib = {
                0, 1, 2,
                3, 0, 2
        };

        OGLBuffers.Attrib[] attribs = {
                new OGLBuffers.Attrib("inPosition", 3),
                new OGLBuffers.Attrib("inColor", 3),
                new OGLBuffers.Attrib("inTexCoord", 2),
                new OGLBuffers.Attrib("inNormal", 3)
        };

        buffers = new OGLBuffers(vb, attribs, ib);
    }
}
