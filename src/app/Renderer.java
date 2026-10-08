package app;

import lwjglutils.OGLBuffers;
import lwjglutils.OGLTexture2D;
import lwjglutils.ShaderUtils;
import org.lwjgl.glfw.GLFWKeyCallback;
import transforms.*;

import java.io.IOException;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;


public class Renderer extends AbstractRenderer {
    private OGLBuffers buffers;
    private int shaderProgram;
    private OGLTexture2D texture;

    private float rotZ = 0;

    private Camera camera;
    private Mat4PerspRH proj;

    public void init() {
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // ======== Kamera a projekce ========
        {
        camera = new Camera()
                .withPosition(new Vec3D(0.1, -1.5f, 1))
                .withAzimuth(Math.toRadians(90))
                .withZenith(Math.toRadians(-25))
                .withFirstPerson(true);

        proj = new Mat4PerspRH(Math.toRadians(90), height / (double)width, 0.1, 100);
        }

        // ======== Textury ========
        try {
            texture = new OGLTexture2D("./textures/wood.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // ======== Quad ========
        {
            // vb
            float[] vb = {
                    -1f, -1f, 0, 1, 0, 0, 0, 0, 0, 0, 1,
                    1f, -1f, 0, 0, 1, 0, 1, 0, 0, 0, 1,
                    1f, 1f, 0, 0, 0, 1, 1, 1, 0, 0, 1,
                    -1f, 1f, 0, 0, 1, 0, 0, 1, 0, 0, 1,
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

        // ======== Shaderové programy ========
        shaderProgram = ShaderUtils.loadProgram("/triangle");

        // Tady říkáme, že chceme použít konkrétní shaderový program
        glUseProgram(shaderProgram);
        // Textury je nutné bindovat
        texture.bind(shaderProgram, "textureWood");
    }

    public void display() {
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

//        rotZ += 0.001f;

        // Modelovací matice - posíláme do shaderu jako uniform
        Mat4 model = new Mat4RotZ(Math.toDegrees(rotZ));
        int locUModel = glGetUniformLocation(shaderProgram, "uModel");
        glUniformMatrix4fv(locUModel, false, model.floatArray());
        // View matice - posíláme do shaderu jako uniform
        int locUView = glGetUniformLocation(shaderProgram, "uView");
        glUniformMatrix4fv(locUView, false, camera.getViewMatrix().floatArray());
        // Proj matice - posíláme do shaderu jako uniform
        int locUProj = glGetUniformLocation(shaderProgram, "uProj");
        glUniformMatrix4fv(locUProj, false, proj.floatArray());

        // vykreslit
        buffers.draw(GL_TRIANGLES, shaderProgram);
    }



    protected GLFWKeyCallback keyCallback = new GLFWKeyCallback() {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
            if (action == GLFW_PRESS && key == GLFW_KEY_L){
                int locUColor = glGetUniformLocation(shaderProgram, "uColor");
                glUniform3f(locUColor, 0, 0, 1);
            }
        }
    };

    @Override
    public GLFWKeyCallback getKeyCallback() {
        return keyCallback;
    }


}