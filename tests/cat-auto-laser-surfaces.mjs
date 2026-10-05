import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import path from 'node:path';
const root=path.resolve(import.meta.dirname,'..');
import {inflateSync} from 'node:zlib';
// Read the real supplied PNG, including transparency; geometry-only coverage
// missed the complementary one-pixel teeth and incorrectly justified cropping.
function readRgba(file){
  const png=readFileSync(file);assert.equal(png[24],8);assert.equal(png[25],6);
  const w=png.readUInt32BE(16),h=png.readUInt32BE(20),chunks=[];
  for(let p=8;p<png.length;){const n=png.readUInt32BE(p),type=png.toString('ascii',p+4,p+8);if(type==='IDAT')chunks.push(png.subarray(p+8,p+8+n));p+=n+12;}
  const raw=inflateSync(Buffer.concat(chunks)),out=Buffer.alloc(w*h*4),stride=w*4;
  const paeth=(a,b,c)=>{const p=a+b-c,pa=Math.abs(p-a),pb=Math.abs(p-b),pc=Math.abs(p-c);return pa<=pb&&pa<=pc?a:pb<=pc?b:c;};
  for(let y=0;y<h;y++)for(let x=0;x<stride;x++){
    const filter=raw[y*(stride+1)],i=y*stride+x,a=x>=4?out[i-4]:0,b=y?out[i-stride]:0,c=y&&x>=4?out[i-stride-4]:0;
    const predictor=[0,a,b,Math.floor((a+b)/2),paeth(a,b,c)][filter];
    assert.notEqual(predictor,undefined);out[i]=(raw[y*(stride+1)+1+x]+predictor)&255;
  }
  return (u,v)=>[...out.subarray((Math.floor(v)*w+Math.floor(u))*4,(Math.floor(v)*w+Math.floor(u))*4+4)];
}
const authored=JSON.parse(readFileSync(path.join(root,'assets-source/cat-auto-laser/model.bbmodel'))).elements.filter(e=>e.from?.[1]===11.9);
const sourceLids=authored.map(e=>({origin:e.from,size:e.to.map((v,i)=>v-e.from[i]),uv:Object.fromEntries(['up','down'].map(side=>{
  const [u,v,u2,v2]=e.faces[side].uv;return [side,{uv:[u,v],uv_size:[u2-u,v2-v]}];
}))}));
function surfacePixels(lids,pixel,x,z,side){
  return lids.filter(c=>x>c.origin[0]&&x<c.origin[0]+c.size[0]&&z>c.origin[2]&&z<c.origin[2]+c.size[2]).map(c=>{
    const f=c.uv[side],tx=(x-c.origin[0])/c.size[0],tz=(z-c.origin[2])/c.size[2];
    return pixel(f.uv[0]+tx*f.uv_size[0],f.uv[1]+(side==='up'?tz:1-tz)*f.uv_size[1]);
  }).filter(rgba=>rgba[3]>=128);
}
for(const port of ['forge-1.20.1','neoforge-1.21.1']){
  const assets=path.join(root,port,'src/main/resources/assets/laowu');
  const bones=JSON.parse(readFileSync(path.join(assets,'models/entity/cat_auto_laser.geo.json')))['minecraft:geometry'][0].bones;
  const lids=bones.filter(b=>['bone4','bone5'].includes(b.name)).flatMap(b=>b.cubes);
  for(const texture of ['texture.png','不亮.png'])for(const side of ['up','down']){
    const pixel=readRgba(path.join(root,'assets-source/cat-auto-laser',texture));
    test(port+' '+texture+' '+side+' interlocks with exactly one opaque surface at every pixel',()=>{
      for(let z=-5.5;z<6;z++)for(let x=-5.5;x<6;x++){
        assert.equal(surfacePixels(sourceLids,pixel,x,z,side).length,1,'Authored fixture must itself interlock');
        assert.equal(surfacePixels(lids,pixel,x,z,side).length,1,'Gap or overlapping opaque teeth at '+x+','+z);
      }
    });
    test(port+' '+texture+' '+side+' matches the full authored artwork, including teeth',()=>{
      for(let z=-5.5;z<6;z++)for(let x=-5.5;x<6;x++)
        assert.deepEqual(surfacePixels(lids,pixel,x,z,side),surfacePixels(sourceLids,pixel,x,z,side),'Artwork mirrored/cropped at '+x+','+z);
    });
  }
  test(port+' interface has no coplanar dynamic liner over its four baked walls',()=>{
    const body=bones.find(b=>b.name==='bb_main2');
    for(const c of body.cubes){
      const loY=Math.min(c.origin[1],c.origin[1]+c.size[1]);
      const hiY=Math.max(c.origin[1],c.origin[1]+c.size[1]);
      if(Math.min(hiY,.95)-Math.max(loY,0)<=1e-8)continue;
      for(const [name,axis,end] of [['west',0,0],['east',0,1],['north',2,0],['south',2,1]]){
        if(!c.uv[name])continue;
        const plane=c.origin[axis]+end*c.size[axis];
        assert.ok(Math.abs(Math.abs(plane)-6)>1e-8,'Duplicate inner wall at '+name+' causes z-fighting');
      }
    }
  });
}
