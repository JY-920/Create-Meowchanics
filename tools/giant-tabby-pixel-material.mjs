// Mesh-native UV baking: 16 model units = one block; one atlas texel = one unit.
// Discrete vanilla colour clusters only. No filtered enlargement, fur, or image generation.
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
const {PNG}=require('C:/Users/16611/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/pngjs');
const round=n=>Math.round(n*1e6)/1e6;
const hash=(x,y)=>((Math.imul(x+131,1597334677)^Math.imul(y+41,3812015801))>>>0);
const clamp=(v,a,b)=>Math.min(b,Math.max(a,v));

export function pixelMaterial(elements,source,bytes){
 const base=PNG.sync.read(bytes),original=new Map(source.elements.map(e=>[e.name,e]));
 const patches=[];
 for(const e of elements)for(const [direction,face] of Object.entries(e.faces)){
  const [dx,dy,dz]=e.to.map((v,i)=>round(v-e.from[i]));
  const [w,h]=['up','down'].includes(direction)?[dx,dz]:['east','west'].includes(direction)?[dz,dy]:[dx,dy];
  patches.push({e,direction,face,w,h,pw:Math.ceil(w),ph:Math.ceil(h)});
 }
 // Two opaque gutter texels prevent atlas bleed without shrinking any surface UV.
 const width=256,gap=2;let x=gap,y=gap,row=0;
 patches.sort((a,b)=>b.ph-a.ph||b.pw-a.pw);
 for(const p of patches){
  if(x+p.pw+gap>width){x=gap;y+=row+gap*2;row=0;}
  p.x=x;p.y=y;x+=p.pw+gap*2;row=Math.max(row,p.ph);
 }
 const height=2**Math.ceil(Math.log2(y+row+gap)),atlas=new PNG({width,height});
 function colour(p,i,j){
  const old=original.get(p.e.name)?.faces[p.direction];
  let rgb,sx=i/4,sy=j/4;
  if(old){
   const uv=old.uv;
   sx=uv[0]+(i+.5)/p.w*(uv[2]-uv[0]);
   sy=uv[1]+(j+.5)/p.h*(uv[3]-uv[1]);
   const k=(clamp(Math.floor(sy),0,base.height-1)*base.width+clamp(Math.floor(sx),0,base.width-1))*4;
   rgb=base.data[k+3]?Array.from(base.data.slice(k,k+3)):[234,169,57];
  }else if(p.e.name==='collar')rgb=[142,48,32];
  else rgb=p.direction==='north'?[234,234,234]:[234,169,57];
  // Keep eyes and pink nose clean. Each original colour patch gets at most one
  // small stepped highlight/shadow, never a hair stroke or per-pixel noise field.
  const skin=rgb[0]>rgb[1]*1.1&&rgb[1]>rgb[2]*1.3;
  const cream=Math.max(...rgb)-Math.min(...rgb)<15;
  if(skin||cream||p.e.name==='collar'){
   const cx=Math.floor(sx),cy=Math.floor(sy),seed=hash(cx,cy);
   let px=Math.floor((sx-cx)*4),py=Math.floor((sy-cy)*4);
   if(seed&1)px=3-px;if(seed&2)py=3-py;
   const mask=(px===1&&py===1)||(px===2&&py===1)||(px===1&&py===2);
   if(mask&&seed%4!==0){const delta=(seed&4)?6:-6;rgb=rgb.map(v=>clamp(v+delta,0,255));}
  }
  return [...rgb,255];
 }
 for(const p of patches){
  for(let j=-gap;j<p.ph+gap;j++)for(let i=-gap;i<p.pw+gap;i++){
   const rgba=colour(p,clamp(i,0,p.w-1e-6),clamp(j,0,p.h-1e-6));
   const k=((p.y+j)*width+p.x+i)*4;atlas.data.set(rgba,k);
  }
  p.face.uv=[p.x,p.y,round(p.x+p.w),round(p.y+p.h)];p.face.texture=0;
 }
 return {png:atlas,texture:PNG.sync.write(atlas),layout:patches.map(p=>({cube:p.e.name,face:p.direction,uv:p.face.uv,model_units:[p.w,p.h]}))};
}
