package app.object;

import lwjglutils.OGLBuffers;
import lwjglutils.OGLTexture2D;
import transforms.*;

/**
 * Základní třída pro objekt ve scéně.
 * Obsahuje geometrii, model matici, ID pro color picking, textury a příznak výběru.
 */
public class SceneObject {
    protected OGLBuffers buffers;

    // Jedinečné ID objektu pro color picking (hodnota 1–254, 0 = nelze vybrat)
    private final int id;

    // Transformace objektu
    private Vec3D position;
    private Vec3D rotation;  // rotace v radiánech (x, y, z)
    private Vec3D scale;

    // Je tento objekt právě aktivní (vybraný)?
    private boolean selected = false;

    // Textury (volitelné)
    private OGLTexture2D textureColor;
    private OGLTexture2D textureNormal;

    public SceneObject(int id) {
        this.id       = id;
        this.position = new Vec3D(0, 0, 0);
        this.rotation = new Vec3D(0, 0, 0);
        this.scale    = new Vec3D(1, 1, 1);
    }

    // ======== Model matice ========

    /** Scale → RotX → RotY → RotZ → Translate */
    public Mat4 getModelMatrix() {
        Mat4 s  = new Mat4Scale(scale.getX(), scale.getY(), scale.getZ());
        Mat4 rx = new Mat4RotX(rotation.getX());
        Mat4 ry = new Mat4RotY(rotation.getY());
        Mat4 rz = new Mat4RotZ(rotation.getZ());
        Mat4 t  = new Mat4Transl(position.getX(), position.getY(), position.getZ());
        return s.mul(rx).mul(ry).mul(rz).mul(t);
    }

    // ======== Color picking ID jako barva (R kanál) ========
    public float[] getPickingColor() {
        return new float[]{id / 255.0f, 0.0f, 0.0f};
    }

    // ======== Transformace ========

    public void translate(double dx, double dy, double dz) {
        position = position.add(new Vec3D(dx, dy, dz));
    }

    public void rotate(double drx, double dry, double drz) {
        rotation = rotation.add(new Vec3D(drx, dry, drz));
    }

    public void scaleBy(double factor) {
        scale = scale.mul(factor);
    }

    // ======== Gettery / Settery ========

    public OGLBuffers getBuffers()                      { return buffers; }
    public void setBuffers(OGLBuffers b)                { this.buffers = b; }

    public int getId()                                  { return id; }

    public boolean isSelected()                        { return selected; }
    public void setSelected(boolean selected)          { this.selected = selected; }

    public OGLTexture2D getTextureColor()              { return textureColor; }
    public void setTextureColor(OGLTexture2D t)        { this.textureColor = t; }

    public OGLTexture2D getTextureNormal()             { return textureNormal; }
    public void setTextureNormal(OGLTexture2D t)       { this.textureNormal = t; }

    public Vec3D getPosition()                         { return position; }
    public Vec3D getRotation()                         { return rotation; }
    public Vec3D getScale()                            { return scale; }
}
