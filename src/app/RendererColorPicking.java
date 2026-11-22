package app;

import app.objLoader.ObjLoader;
import app.object.Axis;
import app.object.Pyramid;
import app.object.Quad;
import app.object.SceneObject;
import app.object.Sphere;
import lwjglutils.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import transforms.*;

import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30C.glBindFramebuffer;


public class RendererColorPicking extends AbstractRenderer {

    // ======== Shadery ========
    private int shaderProgramPhong, shaderProgramAxis, shaderProgramColorPicking, shaderProgramDebug, shaderProgramLight;

    // ======== Scéna ========
    private Axis axisObject;
    private final List<SceneObject> sceneObjects = new ArrayList<>();
    private SceneObject selectedObject = null;
    private Sphere lightSource;

    // ======== Kamera ========
    private Camera camera;
    private boolean mousePressed = false;
    private double lastX, lastY;
    private Mat4PerspRH proj;

    // ======== Color picking FBO ========
    private OGLRenderTarget renderTarget;
    private OGLTexture2D.Viewer viewer;

    // ======== Klávesy ========
    private final Set<Integer> pressedKeys = new HashSet<>();
    private static final double MOVE_SPEED  = 0.05;
    private static final double TRANS_STEP  = 0.05;
    private static final double ROT_STEP    = Math.toRadians(3);
    private static final double SCALE_STEP  = 1.05;

    private enum TransformAxis { X, Y, Z }
    private TransformAxis activeAxis = TransformAxis.X;

    // ======== Debug mód ========
    // 0=texture, 1=pos object, 2=pos world, 3=pos view, 4=depth, 5=normal,
    // 6=UV, 7=light dist, 8=normal map RGB, 9=tangent, 10=bitangent, 11=mapped normal
    private static final int DEBUG_MODE_COUNT = 12;
    private static final String[] DEBUG_MODE_NAMES = {
        "Textura (bez osvětlení)",
        "Pozice – object space",
        "Pozice – world space",
        "Pozice – view space",
        "Hloubka (depth)",
        "Normála – world space",
        "UV souřadnice",
        "Vzdálenost od světla",
        "Normal mapa (raw)",
        "Tangenta – world space",
        "Bitangenta – world space",
        "Normála po normal mappingu – world space"
    };
    private int debugMode = -1; // -1 = Phong (normální render), 0–8 = debug mód

    // ======== Zdroj světla ========
    private Vec3D lightPos = new Vec3D(2.0, 2.0, 2.0);
    private static final double LIGHT_STEP = 0.1;

    @Override
    public void init() {
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        renderTarget = new OGLRenderTarget(width, height);
        viewer = new OGLTexture2D.Viewer();

        // ======== Kamera ========
        camera = new Camera()
                .withPosition(new Vec3D(0, 0, 0))
                .withAzimuth(Math.toRadians(135))
                .withZenith(Math.toRadians(-45))
                .withRadius(5)
                .withFirstPerson(false);
        proj = new Mat4PerspRH(Math.toRadians(45), height / (double) width, 0.1f, 100f);

        // ======== Textury ========
        OGLTexture2D texBrickColor, texBrickNormal, texWood;
        try {
            texBrickColor  = new OGLTexture2D("./textures/brickwall.jpg");
            texBrickNormal = new OGLTexture2D("./textures/brickwall_normal.jpg");
            texWood        = new OGLTexture2D("./textures/wood.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // ======== Osy ========
        axisObject = new Axis();
        lightSource = new Sphere(24, 32);

        // ======== Floor (Quad) – ID 1 ========
        SceneObject floor = new Quad(1);
        floor.setTextureColor(texBrickColor);
        floor.setTextureNormal(texBrickNormal);
        floor.rotate(Math.toRadians(-90), 0, 0);
        floor.translate(0, -0.36, 0);
        sceneObjects.add(floor);

        // ======== Dragon (OBJ) – ID 2 ========
        SceneObject dragon = new SceneObject(2);
        dragon.setBuffers(ObjLoader.load("./obj/Shrimp-bl.obj"));
        dragon.setTextureColor(texWood);
        dragon.scaleBy(0.4);
        dragon.rotate(Math.toRadians(90), 0, Math.toRadians(90));
        dragon.translate(-0.25, 0, 0);
        sceneObjects.add(dragon);

        // ======== Pyramid (ID 3) ========
        SceneObject pyramid = new Pyramid(3);
        pyramid.setTextureColor(texWood);
        pyramid.scaleBy(0.7);
        pyramid.translate(0.45, -0.36, -0.4);
        sceneObjects.add(pyramid);

        // ======== Shadery ========
        shaderProgramPhong        = ShaderUtils.loadProgram("/phong");
        shaderProgramAxis         = ShaderUtils.loadProgram("/axis");
        shaderProgramColorPicking = ShaderUtils.loadProgram("/colorPicking");
        shaderProgramDebug        = ShaderUtils.loadProgram("/debug");
        shaderProgramLight        = ShaderUtils.loadProgram("/light");

        printHelp();
    }

    @Override
    public void display() {
        // WASD pohyb kamery
        if (pressedKeys.contains(GLFW_KEY_W)) camera = camera.forward(MOVE_SPEED);
        if (pressedKeys.contains(GLFW_KEY_S)) camera = camera.backward(MOVE_SPEED);
        if (pressedKeys.contains(GLFW_KEY_A)) camera = camera.left(MOVE_SPEED);
        if (pressedKeys.contains(GLFW_KEY_D)) camera = camera.right(MOVE_SPEED);

        glEnable(GL_DEPTH_TEST);

        // 1. průchod: color picking do FBO (vždy, bez ohledu na debug mód)
        renderTarget.bind();
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        renderPickingPass();

        // 2. průchod: finální render
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (debugMode >= 0) {
            renderDebugPass();
        } else {
            renderMainPass();
        }

        // Náhled picking textury (levý dolní roh)
        glDisable(GL_DEPTH_TEST);
        viewer.view(renderTarget.getColorTexture(), -1, -1, 0.5);
    }

    // ================================================================
    // Picking průchod – jen objekty scény, osy se nekresli
    // ================================================================
    private void renderPickingPass() {
        glUseProgram(shaderProgramColorPicking);
        setGlobalUniforms(shaderProgramColorPicking);

        for (SceneObject obj : sceneObjects) {
            glUniformMatrix4fv(
                    glGetUniformLocation(shaderProgramColorPicking, "uModel"),
                    false, obj.getModelMatrix().floatArray());
            float[] c = obj.getPickingColor();
            glUniform3f(glGetUniformLocation(shaderProgramColorPicking, "uColor"), c[0], c[1], c[2]);
            obj.getBuffers().draw(GL_TRIANGLES, shaderProgramColorPicking);
        }
    }

    // ================================================================
    // Hlavní průchod – Phong + highlight
    // ================================================================
    private void renderMainPass() {
        // Osy
        glUseProgram(shaderProgramAxis);
        setGlobalUniforms(shaderProgramAxis);
        axisObject.getBuffers().draw(GL_LINES, shaderProgramAxis);

        // Objekty
        glUseProgram(shaderProgramPhong);
        setGlobalUniforms(shaderProgramPhong);
        glUniform3f(glGetUniformLocation(shaderProgramPhong, "uLightPos"),
                (float) lightPos.getX(), (float) lightPos.getY(), (float) lightPos.getZ());

        for (SceneObject obj : sceneObjects) {
            glUniformMatrix4fv(
                    glGetUniformLocation(shaderProgramPhong, "uModel"),
                    false, obj.getModelMatrix().floatArray());

            if (obj.isSelected()) {
                glUniform3f(glGetUniformLocation(shaderProgramPhong, "uColor"), 1.0f, 1.0f, 0.0f);
            } else {
                glUniform3f(glGetUniformLocation(shaderProgramPhong, "uColor"), 0.5f, 0.5f, 0.5f);
            }

            bindTextures(shaderProgramPhong, obj);
            glUniform1i(glGetUniformLocation(shaderProgramPhong, "uHasNormalMap"),
                    obj.getTextureNormal() != null ? 1 : 0);
            obj.getBuffers().draw(GL_TRIANGLES, shaderProgramPhong);
        }

        renderLightSource();
    }

    // ================================================================
    // Debug průchod – všechny objekty debug shaderem, osy normálně
    // ================================================================
    private void renderDebugPass() {
        // Osy vždy normálním shaderem
        glUseProgram(shaderProgramAxis);
        setGlobalUniforms(shaderProgramAxis);
        axisObject.getBuffers().draw(GL_LINES, shaderProgramAxis);

        // Objekty debug shaderem
        glUseProgram(shaderProgramDebug);
        setGlobalUniforms(shaderProgramDebug);
        glUniform3f(glGetUniformLocation(shaderProgramDebug, "uLightPos"),
                (float) lightPos.getX(), (float) lightPos.getY(), (float) lightPos.getZ());
        glUniform1i(glGetUniformLocation(shaderProgramDebug, "uDebugMode"), debugMode);

        for (SceneObject obj : sceneObjects) {
            glUniformMatrix4fv(
                    glGetUniformLocation(shaderProgramDebug, "uModel"),
                    false, obj.getModelMatrix().floatArray());
            bindTextures(shaderProgramDebug, obj);
            glUniform1i(glGetUniformLocation(shaderProgramDebug, "uHasNormalMap"),
                    obj.getTextureNormal() != null ? 1 : 0);
            obj.getBuffers().draw(GL_TRIANGLES, shaderProgramDebug);
        }

        renderLightSource();
    }

    private void renderLightSource() {
        glUseProgram(shaderProgramLight);
        setGlobalUniforms(shaderProgramLight);
        Mat4 model = new Mat4Scale(0.12).mul(new Mat4Transl(lightPos));
        glUniformMatrix4fv(
                glGetUniformLocation(shaderProgramLight, "uModel"),
                false, model.floatArray());
        lightSource.getBuffers().draw(GL_TRIANGLES, shaderProgramLight);
    }

    // ================================================================
    private void bindTextures(int prog, SceneObject obj) {
        if (obj.getTextureColor()  != null) obj.getTextureColor().bind(prog, "textureColor", 0);
        if (obj.getTextureNormal() != null) obj.getTextureNormal().bind(prog, "textureNormal", 1);
    }

    private void setGlobalUniforms(int prog) {
        glUniformMatrix4fv(glGetUniformLocation(prog, "uView"), false, camera.getViewMatrix().floatArray());
        glUniformMatrix4fv(glGetUniformLocation(prog, "uProj"), false, proj.floatArray());
    }

    private SceneObject findObjectById(int id) {
        if (id == 0) return null;
        for (SceneObject obj : sceneObjects)
            if (obj.getId() == id) return obj;
        return null;
    }

    // ================================================================
    // Transformace aktivního objektu
    // ================================================================
    private void applyTranslation(double delta) {
        if (selectedObject == null) return;
        switch (activeAxis) {
            case X: selectedObject.translate(delta, 0, 0); break;
            case Y: selectedObject.translate(0, delta, 0); break;
            case Z: selectedObject.translate(0, 0, delta); break;
        }
    }

    private void applyRotation(double delta) {
        if (selectedObject == null) return;
        switch (activeAxis) {
            case X: selectedObject.rotate(delta, 0, 0); break;
            case Y: selectedObject.rotate(0, delta, 0); break;
            case Z: selectedObject.rotate(0, 0, delta); break;
        }
    }

    private void applyScale(double factor) {
        if (selectedObject == null) return;
        selectedObject.scaleBy(factor);
    }

    private void printHelp() {
        System.out.println("=== OVLÁDÁNÍ ===");
        System.out.println("WASD          - pohyb kamery");
        System.out.println("Myš + drag    - rozhlížení");
        System.out.println("Scroll        - zoom");
        System.out.println("Levý klik     - vybrat objekt (color picking)");
        System.out.println("Tab           - přepnout výběr na další objekt");
        System.out.println("Escape        - zrušit výběr / zavřít");
        System.out.println("1/2/3         - aktivní osa X/Y/Z");
        System.out.println("I/K           - translace +/-");
        System.out.println("U/O           - rotace +/-");
        System.out.println("+/-           - scale +/-");
        System.out.println("F             - přepnout debug mód (cyklicky)");
        System.out.println("J/L           - posun světla X +/-");
        System.out.println("Y/H           - posun světla Y +/-");
        System.out.println("N/M           - posun světla Z +/-");
        System.out.println("================");
    }

    // ================================================================
    // Callbacks
    // ================================================================

    private final GLFWKeyCallback keyCallback = new GLFWKeyCallback() {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                if (selectedObject != null) {
                    selectedObject.setSelected(false);
                    selectedObject = null;
                    System.out.println("Výběr zrušen");
                } else {
                    glfwSetWindowShouldClose(window, true);
                }
            }

            if (action == GLFW_PRESS)   pressedKeys.add(key);
            if (action == GLFW_RELEASE) pressedKeys.remove(key);

            if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                // --- Osa transformace ---
                if (key == GLFW_KEY_1) { activeAxis = TransformAxis.X; System.out.println("Osa: X"); }
                if (key == GLFW_KEY_2) { activeAxis = TransformAxis.Y; System.out.println("Osa: Y"); }
                if (key == GLFW_KEY_3) { activeAxis = TransformAxis.Z; System.out.println("Osa: Z"); }

                // --- Tab: přepnout objekt ---
                if (key == GLFW_KEY_TAB && action == GLFW_PRESS) {
                    if (sceneObjects.isEmpty()) return;
                    if (selectedObject != null) selectedObject.setSelected(false);
                    int idx = selectedObject == null ? 0
                            : (sceneObjects.indexOf(selectedObject) + 1) % sceneObjects.size();
                    selectedObject = sceneObjects.get(idx);
                    selectedObject.setSelected(true);
                    System.out.println("Vybrán: ID=" + selectedObject.getId());
                }

                // --- Translace ---
                if (key == GLFW_KEY_I) applyTranslation( TRANS_STEP);
                if (key == GLFW_KEY_K) applyTranslation(-TRANS_STEP);

                // --- Rotace ---
                if (key == GLFW_KEY_U) applyRotation( ROT_STEP);
                if (key == GLFW_KEY_O) applyRotation(-ROT_STEP);

                // --- Scale ---
                if (key == GLFW_KEY_KP_ADD     || key == GLFW_KEY_EQUAL) applyScale(SCALE_STEP);
                if (key == GLFW_KEY_KP_SUBTRACT || key == GLFW_KEY_MINUS) applyScale(1.0 / SCALE_STEP);

                // --- Debug mód (F = cyklicky) ---
                if (key == GLFW_KEY_F && action == GLFW_PRESS) {
                    debugMode = (debugMode + 1 < DEBUG_MODE_COUNT) ? debugMode + 1 : -1;
                    if (debugMode < 0)
                        System.out.println("Debug mód: Phong (normální)");
                    else
                        System.out.println("Debug mód " + debugMode + ": " + DEBUG_MODE_NAMES[debugMode]);
                }

                // --- Pohyb světla ---
                if (key == GLFW_KEY_J) lightPos = lightPos.add(new Vec3D( LIGHT_STEP, 0, 0));
                if (key == GLFW_KEY_L) lightPos = lightPos.add(new Vec3D(-LIGHT_STEP, 0, 0));
                if (key == GLFW_KEY_Y) lightPos = lightPos.add(new Vec3D(0,  LIGHT_STEP, 0));
                if (key == GLFW_KEY_H) lightPos = lightPos.add(new Vec3D(0, -LIGHT_STEP, 0));
                if (key == GLFW_KEY_N) lightPos = lightPos.add(new Vec3D(0, 0,  LIGHT_STEP));
                if (key == GLFW_KEY_M) lightPos = lightPos.add(new Vec3D(0, 0, -LIGHT_STEP));
            }
        }
    };

    private final GLFWMouseButtonCallback mbCallback = new GLFWMouseButtonCallback() {
        @Override
        public void invoke(long window, int button, int action, int mods) {
            if (button == GLFW_MOUSE_BUTTON_LEFT)
                mousePressed = action == GLFW_PRESS;

            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                DoubleBuffer xBuf = BufferUtils.createDoubleBuffer(1);
                DoubleBuffer yBuf = BufferUtils.createDoubleBuffer(1);
                glfwGetCursorPos(window, xBuf, yBuf);
                int texX = (int) Math.round(xBuf.get(0));
                int texY = renderTarget.getHeight() - (int) Math.round(yBuf.get(0)) - 1;

                if (texX < 0 || texX >= renderTarget.getWidth() || texY < 0 || texY >= renderTarget.getHeight())
                    return;

                OGLTexImageFloat img = renderTarget.getColorTexture().getTexImage(new OGLTexImageFloat.Format(4));
                int pickedId = (int) Math.round(img.getPixel(texX, texY, 0) * 255);

                if (selectedObject != null) selectedObject.setSelected(false);
                selectedObject = findObjectById(pickedId);
                if (selectedObject != null) {
                    selectedObject.setSelected(true);
                    System.out.println("Vybráno: ID=" + pickedId);
                } else {
                    System.out.println("Nic nevybráno (ID=" + pickedId + ")");
                }
            }
        }
    };

    private final GLFWCursorPosCallback cpCallbacknew = new GLFWCursorPosCallback() {
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

    private final GLFWScrollCallback scrollCallback = new GLFWScrollCallback() {
        @Override
        public void invoke(long window, double dx, double dy) {
            camera = camera.mulRadius(Math.pow(0.8, dy));
        }
    };

    @Override public GLFWKeyCallback getKeyCallback()          { return keyCallback; }
    @Override public GLFWMouseButtonCallback getMouseCallback() { return mbCallback; }
    @Override public GLFWCursorPosCallback getCursorCallback() { return cpCallbacknew; }
    @Override public GLFWScrollCallback getScrollCallback()    { return scrollCallback; }
}
