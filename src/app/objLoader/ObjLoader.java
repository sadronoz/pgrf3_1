package app.objLoader;

import app.Vertex;
import lwjglutils.OGLBuffers;
import transforms.Vec2D;
import transforms.Vec3D;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ObjLoader {
    public static OGLBuffers load(String resource) {
        // Loader neřeší "o", "g" a "materiál"
        FileReader fr = null;
        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
            fr = new FileReader(url.getFile());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        List<Vec3D> positions = new ArrayList<>();
        List<Vec2D> uvs = new ArrayList<>();
        List<Vec3D> normals = new ArrayList<>();
        List<Vertex> vertices = new ArrayList<>();

        BufferedReader br = new BufferedReader(fr);
        String line;
        try {
            while ((line = br.readLine()) != null) {
                String[] currentLine = line.split(" ");

                if (line.startsWith("v ")) {
                    double x = Double.parseDouble(currentLine[1]);
                    double y = Double.parseDouble(currentLine[2]);
                    double z = Double.parseDouble(currentLine[3]);
                    Vec3D position = new Vec3D(x, y, z);
                    positions.add(position);
                } else if (line.startsWith("vt ")) {
                    double u = Double.parseDouble(currentLine[1]);
                    double v = Double.parseDouble(currentLine[2]);
                    Vec2D uv = new Vec2D(u, v);
                    uvs.add(uv);
                } else if (line.startsWith("vn ")) {
                    double x = Double.parseDouble(currentLine[1]);
                    double y = Double.parseDouble(currentLine[2]);
                    double z = Double.parseDouble(currentLine[3]);
                    Vec3D normal = new Vec3D(x, y, z);
                    normals.add(normal);
                } else if (line.startsWith("f ")) {
                    // Pozor: faces nemusí být jen 3 vrcholy, nutná triangulace
                    // Pokud je se v různých faces objevuje stejný vrchol, zbytečně ho znovu přidáváme do seznamu vrcholů
                    // Vrchol nemusí mít uv
                    // Vrchol nemusí mít normálu
                    String[] v1Indices = currentLine[1].split("/");
                    String[] v2Indices = currentLine[2].split("/");
                    String[] v3Indices = currentLine[3].split("/");

                    Vertex v1 = new Vertex(
                            positions.get(Integer.parseInt(v1Indices[0]) - 1),
                            uvs.get(Integer.parseInt(v1Indices[1]) - 1),
                            normals.get(Integer.parseInt(v1Indices[2]) - 1),
                            null
                    );

                    Vertex v2 = new Vertex(
                            positions.get(Integer.parseInt(v2Indices[0]) - 1),
                            uvs.get(Integer.parseInt(v2Indices[1]) - 1),
                            normals.get(Integer.parseInt(v2Indices[2]) - 1),
                            null
                    );

                    Vertex v3 = new Vertex(
                            positions.get(Integer.parseInt(v3Indices[0]) - 1),
                            uvs.get(Integer.parseInt(v3Indices[1]) - 1),
                            normals.get(Integer.parseInt(v3Indices[2]) - 1),
                            null
                    );

                    Vec3D tangent = calculateTangent(v1, v2, v3);
                    v1 = new Vertex(v1.getPosition(), v1.getUv(), v1.getNormal(),
                            orthogonalizeTangent(tangent, v1.getNormal()));
                    v2 = new Vertex(v2.getPosition(), v2.getUv(), v2.getNormal(),
                            orthogonalizeTangent(tangent, v2.getNormal()));
                    v3 = new Vertex(v3.getPosition(), v3.getUv(), v3.getNormal(),
                            orthogonalizeTangent(tangent, v3.getNormal()));
                    vertices.add(v1);
                    vertices.add(v2);
                    vertices.add(v3);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        float[] vb = new float[vertices.size() * 11];
        int[] ib = new int[vertices.size()];

        // Pozor: nyní si tohle můžeme dovolit, protože máme jednoduché trojúhelníky, které jdou za sebou
        for (int i = 0; i < vertices.size(); i++) {
            ib[i] = i;
        }

        int index = 0;
        for(Vertex v : vertices) {
            vb[index++] = (float)v.getPosition().getX();
            vb[index++] = (float)v.getPosition().getY();
            vb[index++] = (float)v.getPosition().getZ();

            vb[index++] = (float)v.getUv().getX();
            vb[index++] = (float)v.getUv().getY();

            vb[index++] = (float)v.getNormal().getX();
            vb[index++] = (float)v.getNormal().getY();
            vb[index++] = (float)v.getNormal().getZ();
            vb[index++] = (float)v.getTangent().getX();
            vb[index++] = (float)v.getTangent().getY();
            vb[index++] = (float)v.getTangent().getZ();
        }


        OGLBuffers.Attrib[] attribs = {
                new OGLBuffers.Attrib("inPosition", 3),
                new OGLBuffers.Attrib("inTexCoord", 2),
                new OGLBuffers.Attrib("inNormal", 3),
                new OGLBuffers.Attrib("inTangent", 3)
        };


        return new OGLBuffers(vb, attribs, ib);
    }

    private static Vec3D calculateTangent(Vertex v1, Vertex v2, Vertex v3) {
        Vec3D edge1 = v2.getPosition().sub(v1.getPosition());
        Vec3D edge2 = v3.getPosition().sub(v1.getPosition());
        double deltaU1 = v2.getUv().getX() - v1.getUv().getX();
        double deltaV1 = v2.getUv().getY() - v1.getUv().getY();
        double deltaU2 = v3.getUv().getX() - v1.getUv().getX();
        double deltaV2 = v3.getUv().getY() - v1.getUv().getY();
        double determinant = deltaU1 * deltaV2 - deltaU2 * deltaV1;

        if (Math.abs(determinant) < 1e-8) {
            return fallbackTangent(v1.getNormal());
        }

        return edge1.mul(deltaV2).sub(edge2.mul(deltaV1)).mul(1.0 / determinant);
    }

    private static Vec3D orthogonalizeTangent(Vec3D tangent, Vec3D normal) {
        Vec3D normalizedNormal = normal.normalized().orElse(new Vec3D(0, 0, 1));
        Vec3D orthogonalTangent = tangent.sub(normalizedNormal.mul(tangent.dot(normalizedNormal)));
        return orthogonalTangent.normalized().orElse(fallbackTangent(normalizedNormal));
    }

    private static Vec3D fallbackTangent(Vec3D normal) {
        Vec3D normalizedNormal = normal.normalized().orElse(new Vec3D(0, 0, 1));
        Vec3D axis = Math.abs(normalizedNormal.getX()) < 0.9
                ? new Vec3D(1, 0, 0)
                : new Vec3D(0, 1, 0);
        Vec3D tangent = axis.sub(normalizedNormal.mul(axis.dot(normalizedNormal)));
        return tangent.normalized().orElse(new Vec3D(1, 0, 0));
    }
}
