#version 150
uniform float Phase;
in vec2 windUV;
in float strength;
out vec4 fragColor;
void main() {
 float r=length(windUV);
 if(r>1.0 || r<0.035)discard;
 float theta=atan(windUV.y,windUV.x);
 float opacity=0.0;
 float tone=0.0;
 for(int i=0;i<3;i++){
  float seed=float(i);
  float burst=fract(Phase*1.3+seed/3.0);
  float radius=0.08+0.92*burst;
  float spiral=theta-Phase*14.0+r*2.2+seed*0.55;
  float tail=fract(spiral*0.7957747);
  float streak=smoothstep(0.015,0.1,tail)*(1.0-smoothstep(0.64,0.96,tail));
  float thickness=0.012+0.032*tail;
  float radial=1.0-smoothstep(thickness,thickness+0.013,abs(r-radius));
  float air=radial*streak*(1.0-smoothstep(0.75,1.0,burst));
  opacity+=air;
  tone+=air*(i==1?0.63:0.98);
 }
 float fade=smoothstep(0.0,0.16,Phase)*(1.0-smoothstep(2.68,3.0,Phase));
 float alpha=clamp(opacity*0.78,0.0,0.82)*fade*strength;
 if(alpha<0.004)discard;
 float gray=tone/max(opacity,0.0001);
 fragColor=vec4(vec3(gray)*alpha,alpha);
}
