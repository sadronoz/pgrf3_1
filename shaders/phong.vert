#version 430

in vec3 inPosition;
in vec3 inColor;
in vec2 inTexCoord;
in vec3 inNormal;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProj;
uniform vec3 uLightPos;   // pozice světla ve world space

out vec3 vertColor;
out vec3 vertPosition;
out vec2 vertTexCoord;
out vec3 vertNormal;      // world space
out vec3 vertLightDir;    // world space: světlo - fragment
out vec3 vertViewDir;     // world space: kamera - fragment

void main() {
    vertColor    = inColor;
    vertPosition = inPosition;
    vertTexCoord = inTexCoord;

    // Normála do world space
    vertNormal = normalize(transpose(inverse(mat3(uModel))) * inNormal);

    vec4 posWorld = uModel * vec4(inPosition, 1.0);

    // Vektor ke světlu
    vertLightDir = uLightPos - posWorld.xyz;

    // Vektor ke kameře (eye = pozice kamery ve world space)
    // Eye pozici rekonstruujeme z view matice: eye = -R^T * t
    vec3 eyePos = -(mat3(uView[0].xyz, uView[1].xyz, uView[2].xyz)) * vec3(uView[3]);
    vertViewDir = eyePos - posWorld.xyz;

    gl_Position = uProj * uView * posWorld;
}
