#version 430

in vec3 vertColor;
in vec3 vertPosition;
in vec2 vertTexCoord;
in vec3 vertNormal;
in vec3 vertLightDir;
in vec3 vertViewDir;

uniform sampler2D textureColor;
uniform sampler2D textureNormal;
uniform vec3 uColor;       // tint barva (highlight při výběru)

out vec4 outColor;

void main() {
    // === Základní barva z textury ===
    vec3 baseColor = texture(textureColor, vertTexCoord).rgb;

    // Pokud je tint barva žlutá (vybraný objekt), přimícháme ji
    // uColor == (0.5,0.5,0.5) = normální, (1,1,0) = vybraný
    bool isSelected = (uColor.r > 0.9 && uColor.g > 0.9 && uColor.b < 0.1);
    if (isSelected) {
        baseColor = mix(baseColor, vec3(1.0, 1.0, 0.0), 0.3);
    }

    // === Normála (použijeme vertex normálu, bez normal mappingu) ===
    vec3 N = normalize(vertNormal);

    // === Vektory ===
    vec3 L = normalize(vertLightDir);
    vec3 V = normalize(vertViewDir);
    vec3 H = normalize(L + V);   // half-vector pro Blinn-Phong

    // === Phong/Blinn-Phong složky ===
    vec3 ambientColor  = vec3(0.15, 0.15, 0.15);
    vec3 diffuseColor  = vec3(1.0,  1.0,  1.0);
    vec3 specularColor = vec3(1.0,  1.0,  1.0);
    float shininess    = 64.0;

    float nDotL  = max(dot(N, L), 0.0);
    float nDotH  = max(dot(N, H), 0.0);

    vec3 ambient  = ambientColor * baseColor;
    vec3 diffuse  = diffuseColor * baseColor * nDotL;
    vec3 specular = specularColor * pow(nDotH, shininess) * step(0.001, nDotL);

    outColor = vec4(ambient + diffuse + specular, 1.0);
}
