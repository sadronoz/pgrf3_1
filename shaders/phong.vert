#version 430

// Atributy vrcholu (vstup do shaderu)
in vec3 inPosition;
in vec3 inColor;
in vec2 inTexCoord;
in vec3 inNormal;

// Uniform proměnné
uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProj;

// Výstup ze shaderu - přechází do fragment shaderu jako interpolovaná hodnota
out vec3 vertColor;
out vec3 vertPosition;
out vec2 vertTexCoord;
out vec3 vertNormal;
out vec3 lightDirection;

void main() {
    vertColor = inColor;
    vertPosition = inPosition;
    vertTexCoord = inTexCoord;

    // Normála - pozor je nutné převést do stejného ss jako máme lightPos, pos apod. Matice musí být inverzní a transponovaná.
    vertNormal = transpose(inverse(mat3(uModel))) * inNormal;

    // Pozice světla. Nyní pevná bez možnosti měnit pozici.
    vec4 lightPosition = vec4(1, 0, 0.2, 1);
    // Pozice vrcholu v souřadnicích světa
    vec4 posWorldSpace = uModel * vec4(inPosition, 1);
    // Výpočet vektoru ke světlu (v souřadnicích světa)
    lightDirection = lightPosition.xyz - posWorldSpace.xyz;

    gl_Position = uProj * uView * posWorldSpace;
}
