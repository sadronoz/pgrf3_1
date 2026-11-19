#version 430

in vec3 inPosition;
in vec3 inColor;
in vec2 inTexCoord;
in vec3 inNormal;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProj;

out vec3 vertColor;

void main() {
    vertColor   = inColor;
    gl_Position = uProj * uView * uModel * vec4(inPosition, 1.0);
}
