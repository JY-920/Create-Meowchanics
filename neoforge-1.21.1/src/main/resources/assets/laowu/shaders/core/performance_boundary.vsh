#version 150
in vec3 Position;
in vec2 UV0;
in vec4 Color;
out vec2 texCoord;
out float vertexAlpha;
void main() {
    gl_Position = vec4(Position, 1.0);
    texCoord = UV0;
    vertexAlpha = Color.a;
}
