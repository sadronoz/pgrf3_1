package app;

import transforms.Vec2D;
import transforms.Vec3D;

public class Vertex {
    private final Vec3D position;
    private final Vec2D uv;
    private final Vec3D normal;
    private final Vec3D tangent;

    public Vertex(Vec3D position, Vec2D uv, Vec3D normal, Vec3D tangent) {
        this.position = position;
        this.uv = uv;
        this.normal = normal;
        this.tangent = tangent;
    }

    public Vec3D getPosition() {
        return position;
    }

    public Vec2D getUv() {
        return uv;
    }

    public Vec3D getNormal() {
        return normal;
    }

    public Vec3D getTangent() {
        return tangent;
    }
}
