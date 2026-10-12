#version 150
in vec3 Position;
in vec2 UV0;
in vec4 Color;
uniform mat4 WindProjection;
out vec2 windUV;
out float strength;
void main() {
 gl_Position = WindProjection * vec4(Position, 1.0);
 windUV = UV0;
 strength = Color.a;
}
