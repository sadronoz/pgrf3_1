#version 430

in vec3 inPosition;
in vec2 inTexCoord;

out vec2 vertTexCoord;

void main() {
    vertTexCoord = inTexCoord;
    gl_Position = vec4(inPosition.xy, 0, 1);
}
