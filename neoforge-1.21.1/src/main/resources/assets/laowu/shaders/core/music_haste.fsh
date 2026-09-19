#version 150
in vec2 uv;
in float strength;
out vec4 fragColor;
float segment(vec2 p, vec2 a, vec2 b) {
    vec2 v=b-a;
    return length(p-a-v*clamp(dot(p-a,v)/dot(v,v),0.0,1.0))-0.095;
}
void main() {
    // Two upward chevrons: an attack-tempo symbol, visually distinct from healing's +.
    float d=min(min(segment(uv,vec2(-0.49,0.14),vec2(0.0,0.58)),
                    segment(uv,vec2(0.0,0.58),vec2(0.49,0.14))),
                min(segment(uv,vec2(-0.49,-0.42),vec2(0.0,0.02)),
                    segment(uv,vec2(0.0,0.02),vec2(0.49,-0.42))));
    float aa=max(fwidth(d),0.012);
    float core=1.0-smoothstep(-aa,aa,d);
    float halo=exp(-max(d,0.0)*12.0)*(1.0-core);
    float edgeFade=1.0-smoothstep(0.76,1.0,max(abs(uv.x),abs(uv.y)));
    float alpha=(core*0.92+halo*0.4)*strength*edgeFade;
    if(alpha<0.005)discard;
    vec3 color=mix(vec3(0.46,0.10,0.92),vec3(0.89,0.68,1.0),core);
    fragColor=vec4(color*alpha,alpha);
    if(any(isnan(fragColor)) || any(isinf(fragColor)))discard;
}
