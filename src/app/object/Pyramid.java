package app.object;

import lwjglutils.OGLBuffers;
import transforms.Vec2D;
import transforms.Vec3D;

public class Pyramid extends SceneObject {
    private static final int VERTEX_COUNT = 18;
    private static final int FLOATS_PER_VERTEX = 14;

    public Pyramid(int id) {
        super(id);

        float[] vertexData = new float[VERTEX_COUNT * FLOATS_PER_VERTEX];
        int[] indexData = new int[VERTEX_COUNT];
        int[] cursor = {0};

        Vec3D frontLeft = new Vec3D(-0.7, 0, 0.7);
        Vec3D frontRight = new Vec3D(0.7, 0, 0.7);
        Vec3D backRight = new Vec3D(0.7, 0, -0.7);
        Vec3D backLeft = new Vec3D(-0.7, 0, -0.7);
        Vec3D apex = new Vec3D(0, 1.4, 0);

        Vec2D bottomLeft = new Vec2D(0, 0);
        Vec2D bottomRight = new Vec2D(1, 0);
        Vec2D top = new Vec2D(0.5, 1);
        addTriangle(vertexData, cursor, frontLeft, frontRight, apex, bottomLeft, bottomRight, top);
        addTriangle(vertexData, cursor, frontRight, backRight, apex, bottomLeft, bottomRight, top);
        addTriangle(vertexData, cursor, backRight, backLeft, apex, bottomLeft, bottomRight, top);
        addTriangle(vertexData, cursor, backLeft, frontLeft, apex, bottomLeft, bottomRight, top);

        addTriangle(vertexData, cursor, frontLeft, backRight, frontRight,
                new Vec2D(0, 0), new Vec2D(1, 1), new Vec2D(1, 0));
        addTriangle(vertexData, cursor, frontLeft, backLeft, backRight,
                new Vec2D(0, 0), new Vec2D(0, 1), new Vec2D(1, 1));

        for (int i = 0; i < indexData.length; i++) {
            indexData[i] = i;
        }

        OGLBuffers.Attrib[] attribs = {
                new OGLBuffers.Attrib("inPosition", 3),
                new OGLBuffers.Attrib("inColor", 3),
                new OGLBuffers.Attrib("inTexCoord", 2),
                new OGLBuffers.Attrib("inNormal", 3),
                new OGLBuffers.Attrib("inTangent", 3)
        };
        buffers = new OGLBuffers(vertexData, attribs, indexData);
    }

    private static void addTriangle(float[] data, int[] cursor,
                                    Vec3D p1, Vec3D p2, Vec3D p3,
                                    Vec2D uv1, Vec2D uv2, Vec2D uv3) {
        Vec3D edge1 = p2.sub(p1);
        Vec3D edge2 = p3.sub(p1);
        Vec3D normal = edge1.cross(edge2).normalized().orElse(new Vec3D(0, 1, 0));

        double deltaU1 = uv2.getX() - uv1.getX();
        double deltaV1 = uv2.getY() - uv1.getY();
        double deltaU2 = uv3.getX() - uv1.getX();
        double deltaV2 = uv3.getY() - uv1.getY();
        double determinant = deltaU1 * deltaV2 - deltaU2 * deltaV1;
        Vec3D tangent = Math.abs(determinant) < 1e-8
                ? new Vec3D(1, 0, 0)
                : edge1.mul(deltaV2).sub(edge2.mul(deltaV1)).mul(1.0 / determinant);
        tangent = tangent.sub(normal.mul(tangent.dot(normal)))
                .normalized().orElse(new Vec3D(1, 0, 0));

        writeVertex(data, cursor, p1, uv1, normal, tangent);
        writeVertex(data, cursor, p2, uv2, normal, tangent);
        writeVertex(data, cursor, p3, uv3, normal, tangent);
    }

    private static void writeVertex(float[] data, int[] cursor, Vec3D position,
                                    Vec2D uv, Vec3D normal, Vec3D tangent) {
        data[cursor[0]++] = (float) position.getX();
        data[cursor[0]++] = (float) position.getY();
        data[cursor[0]++] = (float) position.getZ();
        data[cursor[0]++] = 1;
        data[cursor[0]++] = 1;
        data[cursor[0]++] = 1;
        data[cursor[0]++] = (float) uv.getX();
        data[cursor[0]++] = (float) uv.getY();
        data[cursor[0]++] = (float) normal.getX();
        data[cursor[0]++] = (float) normal.getY();
        data[cursor[0]++] = (float) normal.getZ();
        data[cursor[0]++] = (float) tangent.getX();
        data[cursor[0]++] = (float) tangent.getY();
        data[cursor[0]++] = (float) tangent.getZ();
    }
}
