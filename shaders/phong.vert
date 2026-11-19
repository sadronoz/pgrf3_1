#version 430

in vec3 inPosition;
in vec3 inColor;
in vec2 inTexCoord;
in vec3 inNormal;
in vec3 inTangent;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProj;
uniform vec3 uLightPos;   // pozice světla ve world space

out vec3 vertColor;
out vec3 vertPosition;
out vec2 vertTexCoord;
out vec3 vertNormal;      // world space
out mat3 vertTBN;         // tangent-to-world basis
out vec3 vertLightDir;    // world space: světlo - fragment
out vec3 vertViewDir;     // world space: kamera - fragment

void main() {
    vertColor    = inColor;
    vertPosition = inPosition;
    vertTexCoord = inTexCoord;

    vec4 posWorld = uModel * vec4(inPosition, 1.0);
    mat3 normalMatrix = transpose(inverse(mat3(uModel)));
    vec3 N = normalize(normalMatrix * inNormal);
    vec3 T = normalize(mat3(uModel) * inTangent);
    T = normalize(T - N * dot(N, T));
    vec3 B = normalize(cross(N, T));
    vertNormal = N;
    vertTBN = mat3(T, B, N);

    // Vektor ke světlu
    vertLightDir = uLightPos - posWorld.xyz;

    // Vektor ke kameře (eye = pozice kamery ve world space)
    mat3 viewRotation = mat3(uView);
    vec3 eyePos = transpose(viewRotation) * (-uView[3].xyz);
    vertViewDir = eyePos - posWorld.xyz;

    gl_Position = uProj * uView * posWorld;
}
