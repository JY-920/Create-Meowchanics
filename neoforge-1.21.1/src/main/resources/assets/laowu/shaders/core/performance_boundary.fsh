#version 150
uniform sampler2D Silhouette;
uniform sampler2D SilhouetteDepth;
uniform sampler2D SceneDepth;
uniform vec2 MaskSize;
uniform float Time;
in vec2 texCoord;
in float vertexAlpha;
out vec4 fragColor;

void main() {
    vec4 inside = texture(Silhouette, texCoord);
    // The body interior is never shaded by this effect, even at overlapping skin faces.
    if (inside.r > 0.03) discard;
    float nearest = 100.0;
    vec4 payload = vec4(0.0);
    float closeEdge = 0.0, middleEdge = 0.0, farEdge = 0.0;
    float worldHere = texture(SceneDepth, texCoord).r;
    for (int ring = 0; ring < 3; ring++) {
        float radius = ring == 0 ? 1.35 : ring == 1 ? 3.4 : 6.5;
        float coverage = 0.0;
        for (int i = 0; i < 8; i++) {
            float angle = float(i) * 0.785398163 + float(ring) * 0.18;
            vec2 offset = vec2(cos(angle), sin(angle)) * radius;
            vec2 uv = clamp(texCoord + offset / MaskSize, vec2(0.0), vec2(1.0));
            // Read color and depth at the same covered texel, without mixing clear depth into its edge.
            uv = (clamp(floor(uv * MaskSize), vec2(0.0), MaskSize - 1.0) + 0.5) / MaskSize;
            vec4 sampleMask = texture(Silhouette, uv);
            if (sampleMask.r < 0.5) continue;
            float surface = texture(SilhouetteDepth, uv).r;
            float worldEdge = texture(SceneDepth, uv).r;
            if (surface > worldEdge + 0.000012 || surface > worldHere + 0.000012) continue;
            coverage = max(coverage, sampleMask.r);
            float distance = length((texCoord - uv) * MaskSize);
            if (distance < nearest) { nearest = distance; payload = sampleMask / sampleMask.r; }
        }
        if (ring == 0) closeEdge = coverage;
        else if (ring == 1) middleEdge = coverage;
        else farEdge = coverage;
    }
    if (payload.g < 0.01) discard;
    float width = clamp(payload.b * 2.0, 0.5, 1.5);
    float seed = payload.a;
    // Low-frequency breathing in the outer glow, with a steady lavender rim.
    float breath = 0.88 + 0.12 * sin(Time * 3.2 + seed * 6.283185);
    float core = closeEdge * 0.80;
    float glow = (middleEdge * 0.24 + farEdge * 0.085) * breath;
    float falloff = 1.0 - smoothstep(3.2 * width, 7.0 * width, nearest);
    float alpha = (core + glow) * (1.0 - inside.r) * falloff * payload.g * vertexAlpha;
    if (alpha < 0.008) discard;
    vec3 purple = mix(vec3(0.42, 0.10, 0.88), vec3(0.82, 0.52, 1.0), closeEdge);
    fragColor = vec4(purple, clamp(alpha, 0.0, 0.94));
}
