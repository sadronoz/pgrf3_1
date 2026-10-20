package app.object;

import lwjglutils.OGLBuffers;

public class Axis extends SceneObject {
    public Axis() {
        // vb
        float[] vb = {
                0, 0, 0,  1, 0, 0,
                1, 0, 0,  1, 0, 0,

                0, 0, 0,  0, 1, 0,
                0, 1, 0,  0, 1, 0,

                0, 0, 0,  0, 0, 1,
                0, 0, 1,  0, 0, 1,
        };

        // ib
        int[] ib = {
                0, 1,
                2, 3,
                4, 5
        };

        // popsat grafice, co je to za data
        OGLBuffers.Attrib[] attribs = {
                new OGLBuffers.Attrib("inPosition", 3),
                new OGLBuffers.Attrib("inColor", 3),
        };

        // dát do dohromady
        buffers = new OGLBuffers(vb, attribs, ib);
    }
}
