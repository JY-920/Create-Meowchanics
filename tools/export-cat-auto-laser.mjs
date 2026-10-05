import {readFileSync,writeFileSync,mkdirSync,copyFileSync} from 'node:fs';
import path from 'node:path';
const root=path.resolve(import.meta.dirname,'..');
const source=path.join(root,'assets-source/cat-auto-laser');
// Preserve the author's inside faces, zero-thickness lids and original PNG bytes.
const geo=JSON.parse(readFileSync(path.join(source,'model.geo.json')));
geo['minecraft:geometry'][0].description.identifier='geometry.cat_auto_laser';
// The two nested thin cuboids in front of the pen are authored light, not hardware.
// Runtime draws the beam; retain the pen body and its decorative front faces.
const pen=geo['minecraft:geometry'][0].bones.find(b=>b.name==='bb_main');
pen.cubes=pen.cubes.filter(c=>Math.max(c.origin[2],c.origin[2]+c.size[2])<=8);
// Bedrock exports reverse the two opposite UV corners of horizontal faces.
// The runtime reader uses Blockbench cube-face coordinates: restore original corners.
for(const bone of geo['minecraft:geometry'][0].bones)for(const cube of bone.cubes??[])
  for(const name of ['up','down']){const face=cube.uv?.[name];if(face){
    face.uv=face.uv.map((v,i)=>v+face.uv_size[i]);face.uv_size=face.uv_size.map(v=>-v);
  }}
// Bedrock mirrored the asymmetric lids across X, but this runtime does not.
// Restore their authored sides/pivots without swapping the two texture halves.
for(const name of ['bone4','bone5']){
  const lid=geo['minecraft:geometry'][0].bones.find(b=>b.name===name);
  lid.pivot[0]=-lid.pivot[0];
  for(const cube of lid.cubes)cube.origin[0]=-cube.origin[0]-cube.size[0];
}
// Keep the authored 7px right lid and its original UVs. Its 1px overlap is
// intentional: transparent texels form complementary teeth with the left lid.
// Cropping that strip removes the teeth; flipping V changes the source artwork.
delete geo['minecraft:geometry'][0].bones.find(b=>b.name==='bb_main2').cubes[0].uv.down;
// The source top is a solid black square above the lids. Cut its opening into four
// original-UV strips so the authored lids can actually close and uncover it.
const base=geo['minecraft:geometry'][0].bones.find(b=>b.name==='bb_main2');
// The inverted upper liner also has a down-facing ceiling at Y=12. It is
// above the lids (Y=11.9): with no culling its dark underside covered them.
// Keep its Y=10 cavity floor and side walls, remove only that closing cap.
delete base.cubes[1].uv.down;
// This cavity liner is inverted on Y: its "up" face is geometrically at Y=0,
// not Y=2. It still covered the shaft mouth after the outer down cap was removed.
// Keep the "down" face at Y=2 as the real interior floor; the baked model below
// already supplies the complete Y=0 outer rim and recessed shaft opening.
delete base.cubes[2].uv.up;
// The baked inset panel owns the four walls of this closed shaft socket.
// The old cavity liner lies on exactly those planes and causes z-fighting.
for(const name of ['north','east','south','west'])delete base.cubes[2].uv[name];
delete base.cubes[0].uv.up;
for(const [origin,size,uv,uv_size]of [
  [[-8,12,-8],[2,0,16],[0,0],[2,16]],
  [[6,12,-8],[2,0,16],[14,0],[2,16]],
  [[-6,12,-8],[12,0,2],[2,0],[12,2]],
  [[-6,12,6],[12,0,2],[2,14],[12,2]]
])base.cubes.push({origin,size,uv:{up:{uv,uv_size}}});
const frame=(from,to,faces)=>({from,to,faces});
const face=(texture,uv)=>({texture,uv});
const elements=[
  // Match Create's encased_shaft/block: 2px frame, whole 12px panel inset
  // 0.95px. The normal-size shaft terminates at the outer block boundary.
  frame([0,0,0],[2,0,16],{down:face('#side',[0,0,2,16])}),
  frame([14,0,0],[16,0,16],{down:face('#side',[14,0,16,16])}),
  frame([2,0,0],[14,0,2],{down:face('#side',[2,0,14,2])}),
  frame([2,0,14],[14,0,16],{down:face('#side',[2,14,14,16])}),
  frame([2,.95,2],[14,.95,14],{down:face('#port',[2,2,14,14])}),
  frame([2,0,2],[2,.95,14],{east:face('#side',[2,2,3,14])}),
  frame([14,0,2],[14,.95,14],{west:face('#side',[13,2,14,14])}),
  frame([2,0,2],[14,.95,2],{south:face('#side',[2,2,14,3])}),
  frame([2,0,14],[14,.95,14],{north:face('#side',[2,13,14,14])})
];
if(process.argv.includes('--import-filter'))
  copyFileSync('C:/Users/16611/AppData/Local/Temp/codex-clipboard-70c5aec8-6c6d-4e1f-a46b-71856cb0faa0.png',
    path.join(source,'creature_filter.png'));
for(const port of ['forge-1.20.1','neoforge-1.21.1']){
  const assets=path.join(root,port,'src/main/resources/assets/laowu');
  const json=(name,data)=>{const file=path.join(assets,name);mkdirSync(path.dirname(file),{recursive:true});writeFileSync(file,JSON.stringify(data,null,2)+'\n');};
  json('models/entity/cat_auto_laser.geo.json',geo);
  // Minecraft baked model rotations use the opposite sign to the BER pose frame.
  json('blockstates/cat_auto_laser.json',{variants:{
    'bottom=down':{model:'laowu:block/cat_auto_laser'},
    'bottom=up':{model:'laowu:block/cat_auto_laser',x:180},
    'bottom=north':{model:'laowu:block/cat_auto_laser',x:270},
    'bottom=south':{model:'laowu:block/cat_auto_laser',x:90},
    'bottom=east':{model:'laowu:block/cat_auto_laser',x:90,y:270},
    'bottom=west':{model:'laowu:block/cat_auto_laser',x:90,y:90}
  }});
  mkdirSync(path.join(assets,'animations'),{recursive:true});
  copyFileSync(path.join(source,'model.animation.json'),path.join(assets,'animations/cat_auto_laser.animation.json'));
  for(const [original,name]of [['texture.png','cat_auto_laser.png'],['不亮.png','cat_auto_laser_off.png']])
    copyFileSync(path.join(source,original),path.join(assets,'textures/block',name));
  json('models/block/cat_auto_laser.json',{textures:{particle:'laowu:block/cat_casing',side:'laowu:block/cat_casing',port:'laowu:block/cat_casing_shaft_opening'},elements});
  // Keep GUI fitting in the custom renderer; all other views use vanilla block transforms.
  json('models/item/cat_auto_laser.json',{parent:'builtin/entity',textures:{particle:'laowu:block/cat_casing'},gui_light:'side',display:{"ground":{"rotation":[0,0,0],"translation":[0,3,0],"scale":[0.25,0.25,0.25]},"fixed":{"rotation":[0,0,0],"translation":[0,0,0],"scale":[0.5,0.5,0.5]},"thirdperson_righthand":{"rotation":[75,45,0],"translation":[0,2.5,0],"scale":[0.375,0.375,0.375]},"firstperson_righthand":{"rotation":[0,45,0],"translation":[0,0,0],"scale":[0.4,0.4,0.4]},"firstperson_lefthand":{"rotation":[0,225,0],"translation":[0,0,0],"scale":[0.4,0.4,0.4]}}});
  mkdirSync(path.join(assets,'textures/item'),{recursive:true});
  copyFileSync(path.join(source,'creature_filter.png'),path.join(assets,'textures/item/creature_filter.png'));
  json('models/item/creature_filter.json',{parent:'minecraft:item/generated',textures:{layer0:'laowu:item/creature_filter'}});
}
console.log('Exported automatic laser geometry, bottom port, animation and original textures to both ports');
