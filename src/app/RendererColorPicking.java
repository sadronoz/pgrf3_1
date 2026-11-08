package app;

import app.objLoader.ObjLoader;
import app.object.Axis;
import app.object.Quad;
import lwjglutils.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import transforms.*;

import java.io.IOException;
import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30C.glBindFramebuffer;


public class RendererColorPicking extends AbstractRenderer {
    private OGLBuffers buffersAxis, buffersFloor, buffersDragon;
    private int shaderProgramPhong, shaderProgramAxis, shaderProgramColorPicking;
    private OGLTexture2D textureColor, textureNormal;

    private Camera camera;
    private boolean mousePressed = false;
    private double lastX, lastY;
    private Mat4PerspRH proj;

    private OGLRenderTarget renderTarget;
    private OGLTexture2D.Viewer viewer;

    public void init() {
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        renderTarget = new OGLRenderTarget(width, height);
        viewer = new OGLTexture2D.Viewer();

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
//            textureColor = new OGLTexture2D("./textures/bricksa.jpg");
//            textureNormal = new OGLTexture2D("./textures/bricksn.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // ======== Scene objects ========
        buffersAxis = new Axis().getBuffers();
        buffersFloor = new Quad().getBuffers();
        buffersDragon = ObjLoader.load("./obj/dragon.obj");

        // ======== Shaderové programy ========
        shaderProgramPhong = ShaderUtils.loadProgram("/phong");
        shaderProgramAxis = ShaderUtils.loadProgram("/axis");
        shaderProgramColorPicking = ShaderUtils.loadProgram("/colorPicking");
        // TODO: shaderový program, který obarvuje ID barvou

        // Tady říkáme, že chceme použít konkrétní shaderový program
        //glUseProgram(shaderProgram);
        // Textury je nutné bindovat
        //texture.bind(shaderProgram, "textureWood");
    }

    public void display() {
        glEnable(GL_DEPTH_TEST);

        // TODO: Vykreslit scénu do textury
        // TODO: shaderovým program, který obarvuje ID barvou
        renderTarget.bind();
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        renderScene(shaderProgramColorPicking);

        // Kresli na obrazovku
        // TODO: tady už renderuju phongem
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        renderScene(shaderProgramPhong);

        glDisable(GL_DEPTH_TEST);
        viewer.view(renderTarget.getColorTexture(), -1, -1, 0.5);
    }

    private void renderScene(int shaderProgram) {
        // ======== Render Axis ========
        // TODO: axis nechceme renderovat do textury (nejdou myší vybrat)
        {
            glUseProgram(shaderProgramAxis);
            setGlobalUniforms(shaderProgramAxis);
            buffersAxis.draw(GL_LINES, shaderProgramAxis);
        }

        // ======== Render Floor ========
        // TODO: do shaderu, který vykresluje do textury, musíme přidat ID (barva)
        {
            glUseProgram(shaderProgram);
            setGlobalUniforms(shaderProgram);
            int locUModel = glGetUniformLocation(shaderProgram, "uModel");
            glUniformMatrix4fv(locUModel, false, new Mat4RotY(Math.toRadians(90)).floatArray());
            int locUColor = glGetUniformLocation(shaderProgram, "uColor");
            glUniform3f(locUColor, 30.0f / 255, .0f, .0f);

            textureColor.bind(shaderProgram, "textureColor", 0);
            textureNormal.bind(shaderProgram, "textureNormal", 1);
            buffersFloor.draw(GL_TRIANGLES, shaderProgram);
        }

        // ======== Render Dragon ========
        {
            glUseProgram(shaderProgram);
            setGlobalUniforms(shaderProgram);
            int locUModel = glGetUniformLocation(shaderProgram, "uModel");
            glUniformMatrix4fv(locUModel, false, new Mat4Scale(0.05f).mul(new Mat4RotX(Math.toRadians(90))).mul(new Mat4RotZ(Math.toRadians(90))).mul(new Mat4Transl(-0.25, 0, 0)).floatArray());
            int locUColor = glGetUniformLocation(shaderProgram, "uColor");
            glUniform3f(locUColor, 70.0f / 255, .0f, .0f);
            buffersDragon.draw(GL_TRIANGLES, shaderProgram);
        }
    }

    private void setGlobalUniforms(int shaderProgram) {
        // View matice - posíláme do shaderu jako uniform
        int locUView = glGetUniformLocation(shaderProgram, "uView");
        glUniformMatrix4fv(locUView, false, camera.getViewMatrix().floatArray());
        // Proj matice - posíláme do shaderu jako uniform
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

            DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
            DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
            glfwGetCursorPos(window, xBuffer, yBuffer);
            double x = xBuffer.get(0);
            double y = yBuffer.get(0);

            OGLTexImageFloat image = renderTarget.getColorTexture().getTexImage(new OGLTexImageFloat.Format(4));
            System.out.println("x: " + x);
            System.out.println("y: " + y);

            int texY = renderTarget.getHeight() - (int)Math.round(y) - 1;
            float id = image.getPixel((int)Math.round(x), texY, 0) * 255;
            System.out.println("id: " + id);
            if((int)Math.round(id) == 30)
                System.out.println("Floor");
            else if((int)Math.round(id) == 70)
                System.out.println("Dragon");
            else
                System.out.println("Nic nevybráno");

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