package app.object;

import lwjglutils.OGLBuffers;

public class Quad extends SceneObject {
    public Quad() {
        // vb
        float[] vb = {
                -1f, -1f, -0.1f, 1, 0, 0, 0, 0, 0, 0, 1,
                1f, -1f, -0.1f, 0, 1, 0, 1, 0, 0, 0, 1,
                1f, 1f, -0.1f, 0, 0, 1, 1, 1, 0, 0, 1,
                -1f, 1f, -0.1f, 0, 1, 0, 0, 1, 0, 0, 1,
        };

        // ib
        int[] ib = {
                0, 1, 2,
                3, 0, 2
        };

        // popsat grafice, co je to za data
        OGLBuffers.Attrib[] attribs = {
                new OGLBuffers.Attrib("inPosition", 3),
                new OGLBuffers.Attrib("inColor", 3),
                new OGLBuffers.Attrib("inTexCoord", 2),
                new OGLBuffers.Attrib("inNormal", 3)
        };

        // dát do dohromady
        buffers = new OGLBuffers(vb, attribs, ib);
    }
}
