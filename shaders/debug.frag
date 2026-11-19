#version 430

in vec3 vertColor;
in vec3 vertPosition;
in vec3 vertPosWorld;
in vec3 vertPosView;
in vec2 vertTexCoord;
in vec3 vertNormal;
in vec3 vertTangent;
in vec3 vertBitangent;
in vec3 vertLightDir;
in float vertDepth;

uniform sampler2D textureColor;
uniform sampler2D textureNormal;
uniform vec3 uLightPos;
uniform bool uHasNormalMap;

// Mód vizualizace:
//  0 = výsledná barva (texture, bez osvětlení)
//  1 = pozice v object space (xyz → rgb)
//  2 = pozice ve world space
//  3 = pozice ve view space
//  4 = hloubka z pohledu kamery
//  5 = normála (world space)
//  6 = UV souřadnice
//  7 = vzdálenost od zdroje světla
//  8 = normal mapa (raw RGB), 9 = tangenta, 10 = bitangenta,
//  11 = normal map normal (world space)
uniform int uDebugMode;

out vec4 outColor;

void main() {
    switch (uDebugMode) {

        case 0: {
            // Základní textura bez osvětlení
            outColor = texture(textureColor, vertTexCoord);
            break;
        }

        case 1: {
            // Pozice v object space – mapujeme [-1,1] → [0,1]
            outColor = vec4(vertPosition * 0.5 + 0.5, 1.0);
            break;
        }

        case 2: {
            // Pozice ve world space – mapujeme rozsah [-3,3] → [0,1]
            outColor = vec4(vertPosWorld / 6.0 + 0.5, 1.0);
            break;
        }

        case 3: {
            // Pozice ve view space (camera space)
            outColor = vec4(abs(vertPosView) / 10.0, 1.0);
            break;
        }

        case 4: {
            // Hloubka z pohledu kamery
            // gl_FragCoord.z je v [0,1], kde 0=near, 1=far
            float d = gl_FragCoord.z;
            outColor = vec4(d, d, d, 1.0);
            break;
        }

        case 5: {
            // Normála ve world space → mapujeme [-1,1] → [0,1]
            vec3 n = normalize(vertNormal) * 0.5 + 0.5;
            outColor = vec4(n, 1.0);
            break;
        }

        case 6: {
            // UV souřadnice (texCoord)
            outColor = vec4(vertTexCoord, 0.0, 1.0);
            break;
        }

        case 7: {
            // Vzdálenost od zdroje světla – mapujeme [0,10] → [0,1] inverzně
            float dist = length(vertLightDir);
            float norm = 1.0 - clamp(dist / 10.0, 0.0, 1.0);
            outColor = vec4(norm, norm, norm, 1.0);
            break;
        }

        case 8: {
            vec3 sampledNormal = uHasNormalMap
                    ? texture(textureNormal, vertTexCoord).rgb
                    : vec3(0.5, 0.5, 1.0);
            outColor = vec4(sampledNormal, 1.0);
            break;
        }

        case 9: {
            outColor = vec4(normalize(vertTangent) * 0.5 + 0.5, 1.0);
            break;
        }

        case 10: {
            outColor = vec4(normalize(vertBitangent) * 0.5 + 0.5, 1.0);
            break;
        }

        case 11: {
            vec3 N = normalize(vertNormal);
            if (uHasNormalMap) {
                vec3 tangentNormal = texture(textureNormal, vertTexCoord).rgb * 2.0 - 1.0;
                vec3 T = normalize(vertTangent - N * dot(N, vertTangent));
                vec3 B = normalize(cross(N, T));
                N = normalize(mat3(T, B, N) * tangentNormal);
            }
            outColor = vec4(N * 0.5 + 0.5, 1.0);
            break;
        }

        default: {
            // Fallback – fialová barva = neznámý mód
            outColor = vec4(1.0, 0.0, 1.0, 1.0);
            break;
        }
    }
}
