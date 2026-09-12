#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
uniform mat4 CaptureView;
uniform mat4 CaptureProjection;
out vec2 texCoord;
out float skinAlpha;
void main() {
    gl_Position = CaptureProjection * CaptureView * vec4(Position, 1.0);
    texCoord = UV0;
    skinAlpha = Color.a;
}
