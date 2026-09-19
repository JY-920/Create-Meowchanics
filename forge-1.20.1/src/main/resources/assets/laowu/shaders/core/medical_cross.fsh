#version 150
in vec2 uv;
in float strength;
out vec4 fragColor;
float boxDistance(vec2 p,vec2 extent){
    vec2 q=abs(p)-extent;
    return length(max(q,vec2(0.0)))+min(max(q.x,q.y),0.0);
}
void vfxMaterialMain(){
    float d=min(boxDistance(uv,vec2(0.17,0.63)),boxDistance(uv,vec2(0.63,0.17)));
    float aa=max(fwidth(d),0.012);
    float core=1.0-smoothstep(-aa,aa,d);
    float halo=exp(-max(d,0.0)*12.0)*(1.0-core);
    float edgeFade=1.0-smoothstep(0.76,1.0,max(abs(uv.x),abs(uv.y)));
    float alpha=(core*0.9+halo*0.45)*strength*edgeFade;
    if(alpha<0.005)discard;
    vec3 color=mix(vec3(0.015,0.9,0.17),vec3(0.70,1.0,0.80),core);
    fragColor=vec4(color*alpha,alpha);
}

// Never allow an invalid material sample to cover the game image.
void main() {
    vfxMaterialMain();
    if (any(isnan(fragColor)) || any(isinf(fragColor))) { discard; }
}
