package app;

import app.objLoader.ObjLoader;
import app.object.Axis;
import app.object.Quad;
import lwjglutils.OGLBuffers;
import lwjglutils.OGLRenderTarget;
import lwjglutils.OGLTexture2D;
import lwjglutils.ShaderUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import transforms.*;

import java.io.IOException;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;


public class Renderer extends AbstractRenderer {
    private OGLBuffers buffersAxis, buffersFloor, buffersDragon;
    private int shaderProgramPhong, shaderProgramAxis;
    private OGLTexture2D textureColor, textureNormal;

    private Camera camera;
    private boolean mousePressed = false;
    private double lastX, lastY;
    private Mat4PerspRH proj;

    public void init() {
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // ======== Kamera a projekce ========
        {
            camera = new Camera()
                    .withPosition(new Vec3D(0, 0, 0))
                    .withAzimuth(Math.toRadians(135))
                    .withZenith(Math.toRadians(-45))
                    .withRadius(5)
                    .withFirstPerson(false);
            proj = new Mat4PerspRH(Math.toRadians(45), height / (double) width, 0.1f, 100f);
        }

        // ======== Textury ========
        try {
            textureColor = new OGLTexture2D("./textures/brickwall.jpg");
            textureNormal = new OGLTexture2D("./textures/brickwall_normal.jpg");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // ======== Scene objects ========
        buffersAxis = new Axis().getBuffers();
        buffersFloor = new Quad(1).getBuffers(); // ID 1
        buffersDragon = ObjLoader.load("./obj/dragon.obj");

        // ======== Shaderové programy ========
        shaderProgramPhong = ShaderUtils.loadProgram("/phong");
        shaderProgramAxis = ShaderUtils.loadProgram("/axis");
    }

    public void display() {
        glEnable(GL_DEPTH_TEST);
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // ======== Render Axis ========
        {
            glUseProgram(shaderProgramAxis);
            setGlobalUniforms(shaderProgramAxis);
            buffersAxis.draw(GL_LINES, shaderProgramAxis);
        }

        // ======== Render Floor ========
        {
            glUseProgram(shaderProgramPhong);
            setGlobalUniforms(shaderProgramPhong);
            int locUModel = glGetUniformLocation(shaderProgramPhong, "uModel");
            glUniformMatrix4fv(locUModel, false, new Mat4RotY(Math.toRadians(90)).floatArray());
            textureColor.bind(shaderProgramPhong, "textureColor", 0);
            textureNormal.bind(shaderProgramPhong, "textureNormal", 1);
            buffersFloor.draw(GL_TRIANGLES, shaderProgramPhong);
        }
    }


    private void setGlobalUniforms(int shaderProgram) {
        int locUView = glGetUniformLocation(shaderProgram, "uView");
        glUniformMatrix4fv(locUView, false, camera.getViewMatrix().floatArray());
        int locUProj = glGetUniformLocation(shaderProgram, "uProj");
        glUniformMatrix4fv(locUProj, false, proj.floatArray());
    }

    // ====== Callbacks ======
    private GLFWKeyCallback keyCallback = new GLFWKeyCallback() {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
        }
    };

    private GLFWMouseButtonCallback mbCallback = new GLFWMouseButtonCallback() {
        @Override
        public void invoke(long window, int button, int action, int mods) {
            if (button == 0) {
                mousePressed = action == 1;
            }
        }
    };

    private GLFWCursorPosCallback cpCallbacknew = new GLFWCursorPosCallback() {
        @Override
        public void invoke(long window, double x, double y) {
            if (mousePressed) {
                camera = camera.addAzimuth(Math.toRadians(lastX - x) * 0.3);
                camera = camera.addZenith(Math.toRadians(lastY - y) * 0.3);
            }
            lastX = x;
            lastY = y;
        }
    };

    private GLFWScrollCallback scrollCallback = new GLFWScrollCallback() {
        @Override
        public void invoke(long window, double dx, double dy) {
            camera = camera.mulRadius(Math.pow(0.8, dy));
        }
    };

    @Override
    public GLFWKeyCallback getKeyCallback() {
        return keyCallback;
    }

    @Override
    public GLFWMouseButtonCallback getMouseCallback() {
        return mbCallback;
    }

    @Override
    public GLFWCursorPosCallback getCursorCallback() {
        return cpCallbacknew;
    }

    @Override
    public GLFWScrollCallback getScrollCallback() {
        return scrollCallback;
    }
}
