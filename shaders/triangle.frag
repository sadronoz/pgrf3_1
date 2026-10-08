#version 430

in vec3 vertColor;
in vec3 vertPosition;
in vec2 vertTexCoord;
in vec3 vertNormal;
in vec3 lightDirection;

uniform vec3 uColor;
uniform sampler2D textureWood;

out vec4 outColor;

void main() {
//    outColor = vec4(vertTexCoord, 0, 1.0);
//    outColor = texture(textureWood, vertTexCoord);
//    outColor = vec4(vertNormal, 1);

    // Základní barva pixelu
    vec3 baseColor = vec3(0.5, 0.5, 0.5);
    // Barvy jednotlivých složek
    vec3 ambientColor = vec3(0.9, 0.9, 0.9);
    vec3 diffuseColor = vec3(1, 0, 0);
    // Normalizujeme
    vec3 ld = normalize(lightDirection);
    vec3 nd = normalize(vertNormal);
    // Skalární součin - může být záporný, to nechceme
    float nDotL = max(dot(nd, ld), 0);

    // Výsledná barva - baseColor * phong
    outColor = vec4(baseColor * (ambientColor + (diffuseColor * nDotL)), 1);
    //outColor = vec4(ld, 1);
}
