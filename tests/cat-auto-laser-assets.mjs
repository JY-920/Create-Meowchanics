import assert from 'node:assert/strict';
import {readFileSync,existsSync} from 'node:fs';
import path from 'node:path';
const root=path.resolve(import.meta.dirname,'..');
const source=path.join(root,'assets-source/cat-auto-laser');
// RuntimeBlockbenchModel keeps signed origin/size: "up" lies at originY+sizeY,
// not maxY. The inverted [6,2,6]+[-12,-2,-12] liner's up face is on Y=0.
// Reintroducing that face covers the baked [-3,3] shaft mouth even after the
// outer shell's conventional down cap has been removed.
function horizontalFaces(bone) {
  return bone.cubes.flatMap(c=>['up','down'].filter(side=>c.uv?.[side]).map(side=>({
    y:c.origin[1]+(side==='up'?c.size[1]:0),
    minX:Math.min(c.origin[0],c.origin[0]+c.size[0]),
    maxX:Math.max(c.origin[0],c.origin[0]+c.size[0]),
    minZ:Math.min(c.origin[2],c.origin[2]+c.size[2]),
    maxZ:Math.max(c.origin[2],c.origin[2]+c.size[2]),
  })));
}
for(const port of ['forge-1.20.1','neoforge-1.21.1']){
  const assets=path.join(root,port,'src/main/resources/assets/laowu');
  const geometryPath=path.join(assets,'models/entity/cat_auto_laser.geo.json');
  assert.ok(existsSync(geometryPath),'Automatic laser author geometry has not been installed');
  const geometry=JSON.parse(readFileSync(geometryPath));
  const bones=geometry['minecraft:geometry'][0].bones;
  const baseFaces=horizontalFaces(bones.find(b=>b.name==='bb_main2'));
  const mouthOverlays=baseFaces.filter(f=>Math.abs(f.y)<1e-8
    &&Math.min(f.maxX,3)>Math.max(f.minX,-3)&&Math.min(f.maxZ,3)>Math.max(f.minZ,-3));
  assert.deepEqual(mouthOverlays,[],`${port}: signed liner face covers the bottom shaft opening at Y=0`);
  assert.deepEqual(baseFaces.filter(f=>Math.abs(f.y-12)<1e-8
    &&Math.min(f.maxX,6)>Math.max(f.minX,-6)&&Math.min(f.maxZ,6)>Math.max(f.minZ,-6)),[],
    'Upper inverted cavity cap covers moving lids at Y=11.9');
  for(const [x,z]of [[-2.5,-2.5],[0,0],[2.5,2.5]])
    assert.ok(baseFaces.some(f=>f.y===2&&f.minX<x&&f.maxX>x&&f.minZ<z&&f.maxZ>z),
      'Removing the Y=0 overlay must not remove the real interior floor at Y=2');
  assert.ok(bones.find(b=>b.name==='bb_main').cubes.every(c=>Math.max(c.origin[2],c.origin[2]+c.size[2])<=8),'Authored solid light beam still protrudes beyond the pen mouth');
  const original=JSON.parse(readFileSync(path.join(source,'model.bbmodel')));
  for(const name of ['bb_main','bone6'])for(const cube of bones.find(b=>b.name===name).cubes){
    const faces=['up','down'].filter(face=>cube.uv[face].uv_size.every(v=>v!==0));
    if(!faces.length)continue;
    const authored=original.elements.find(e=>e.from&&e.from.every((v,i)=>Math.abs(v-cube.origin[i])<.0001)&&e.to.every((v,i)=>Math.abs(v-cube.origin[i]-cube.size[i])<.0001));
    assert.ok(authored,'Cannot locate original laser pen cube');
    for(const face of faces){
      const uv=cube.uv[face];
      assert.deepEqual([...uv.uv,uv.uv[0]+uv.uv_size[0],uv.uv[1]+uv.uv_size[1]],authored.faces[face].uv,'Pen top/bottom UVs disagree with original Blockbench corners');
    }
  }
  assert.equal(bones.length,9);
  assert.equal(bones.find(b=>b.name==='bb_main').parent,'bone3');
  assert.equal(bones.find(b=>b.name==='bone3').parent,'bone');
  assert.ok(!bones.find(b=>b.name==='bb_main2').cubes[0].uv.down,'Original bottom cap would cover shaft mouth');
  assert.ok(!bones.find(b=>b.name==='bb_main2').cubes[0].uv.up,'Opaque source top would hide both moving lids');
  assert.equal(bones.find(b=>b.name==='bb_main2').cubes.length,7,'Missing original-UV rim strips around moving lids');
  for(const [file,original]of [['cat_auto_laser.png','texture.png'],['cat_auto_laser_off.png','不亮.png']])
    assert.deepEqual(readFileSync(path.join(assets,'textures/block',file)),readFileSync(path.join(source,original)));
  const animation=JSON.parse(readFileSync(path.join(assets,'animations/cat_auto_laser.animation.json')));
  assert.equal(animation.animations['animation.model.new'].animation_length,1.25);
  const model=JSON.parse(readFileSync(path.join(assets,'models/block/cat_auto_laser.json')));
  assert.equal(model.textures.port,'laowu:block/cat_casing_shaft_opening');
  // Native Create encased ports recess the whole 12x12 opening panel, not
  // just the central 6x6 dark spot, leaving a two-pixel structural border.
  assert.ok(model.elements.some(e=>e.from[0]===2&&e.to[0]===14&&e.from[2]===2&&e.to[2]===14
    &&e.from[1]>.9&&e.from[1]<1.1&&e.to[1]===e.from[1]&&e.faces.down?.texture==='#port'),
    'Missing Create-style full inset interface panel');
  assert.equal(Object.keys(JSON.parse(readFileSync(path.join(assets,'blockstates/cat_auto_laser.json'))).variants).length,6);
}
console.log('PASS: automatic laser author textures, hierarchy, 25-tick animation, six frames and recessed shaft mouth');
