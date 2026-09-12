#version 150
uniform sampler2D SkinTexture;
uniform vec3 OutlineData;
in vec2 texCoord;
in float skinAlpha;
out vec4 fragColor;
void main() {
    if (texture(SkinTexture, texCoord).a * skinAlpha < 0.12) discard;
    fragColor = vec4(1.0, OutlineData);
}
