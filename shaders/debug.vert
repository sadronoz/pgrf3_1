#version 430

in vec3 inPosition;
in vec3 inColor;
in vec2 inTexCoord;
in vec3 inNormal;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProj;

// Pozice světla (world space) – pro výpočet vzdálenosti
uniform vec3 uLightPos;

out vec3 vertColor;
out vec3 vertPosition;      // pozice v object space
out vec3 vertPosWorld;      // pozice v world space
out vec3 vertPosView;       // pozice v view space (camera space)
out vec2 vertTexCoord;
out vec3 vertNormal;        // normála v world space
out vec3 vertLightDir;      // vektor ke světlu (world space)
out float vertDepth;        // hloubka z pohledu kamery (0=blízko, 1=daleko)

void main() {
    vertColor    = inColor;
    vertPosition = inPosition;
    vertTexCoord = inTexCoord;

    // Normála → world space (inverzní transponovaná model matice)
    vertNormal = normalize(transpose(inverse(mat3(uModel))) * inNormal);

    vec4 posWorld = uModel * vec4(inPosition, 1.0);
    vertPosWorld  = posWorld.xyz;

    vec4 posView  = uView * posWorld;
    vertPosView   = posView.xyz;

    // Hloubka: z view space, normalizovaná na [0,1] pomocí near/far
    // Jednoduše použijeme gl_FragCoord.z ve fragment shaderu.
    vertDepth = posView.z; // záporné číslo v RH systému

    // Vektor ke světlu
    vertLightDir = uLightPos - posWorld.xyz;

    gl_Position = uProj * posView;
}
