#version 430

in vec2 vertTexCoord;

uniform sampler2D textureScene;

out vec4 outColor;

void main() {
    vec3 baseColor = texture(textureScene, vertTexCoord).rgb;

    //if(gl_FragCoord.x < 300)
    baseColor.r = 0.1f;

   // outColor = vec4(vertTexCoord, 0, 1);
    outColor = vec4(baseColor, 1);
}
