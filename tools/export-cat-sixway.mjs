import assert from 'node:assert/strict';
import {readFileSync,writeFileSync,mkdirSync,copyFileSync,readdirSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {exportDepot} from './export-cat-depot.mjs';
import {exportBasin} from './export-cat-machines.mjs';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const write=process.argv.includes('--write');
const assets=new Map();
const add=(p,j)=>assets.set(p,Buffer.from(JSON.stringify(j,null,2)+'\n'));
// JSON block rotations have the opposite sign to the server/JOML frame.
export const frames={down:{},up:{x:180},north:{x:270},south:{x:90},east:{x:90,y:270},west:{x:90,y:90}};
const localAxis=(bottom,world)=>bottom==='east'||bottom==='west'?(world==='z'?'x':'z'):bottom==='north'||bottom==='south'?(world==='y'?'z':'x'):world;
const sideMap={north:'west',west:'south',south:'east',east:'north',up:'up',down:'down'};
const turnPoint=([x,y,z])=>[z,y,16-x];
function turnModel(model,turns) {
  model=structuredClone(model);
  for(let t=0;t<turns;t++)for(const e of model.elements){
    const a=turnPoint(e.from),b=turnPoint(e.to);
    // Swap only the axis whose sign changed. Min/max would erase authored
    // inward-facing (negative-extent) spout walls.
    e.from=[a[0],a[1],b[2]];
    e.to=[b[0],b[1],a[2]];
    e.faces=Object.fromEntries(Object.entries(e.faces).map(([side,face])=>{
      face={...face};
      if(side==='up'||side==='down')face.rotation=((face.rotation??0)+(side==='up'?270:90))%360;
      return [sideMap[side],face];
    }));
    if(e.rotation) {
      e.rotation.origin=turnPoint(e.rotation.origin);
      if(e.rotation.axis==='x'){e.rotation.axis='z';e.rotation.angle=-e.rotation.angle;}
      else if(e.rotation.axis==='z')e.rotation.axis='x';
    }
  }
  return model;
}
// A port occupies the authored 12 x 10 outline at side coordinates 2..14,
// height 4..14. Keep its surrounding pixels on the facade, but move the
// actual opening one model pixel inward and skin the four exposed jambs.
function recessPort(model,side,portFaces,resolution) {
  const source=model.elements[0].faces[side];
  assert(source,`Missing ${side} shell face`);
  delete model.elements[0].faces[side];
  const normalAxis=side==='east'||side==='west'?0:2;
  const lateralAxis=normalAxis===0?2:0;
  const outer=side==='east'||side==='south'?16:0;
  const inner=side==='east'||side==='south'?15:1;
  const uvPart=(lo,hi,y0,y1)=>{
    const [u0,v0,u1,v1]=source.uv;
    const reverse=side==='east'||side==='north';
    const a=reverse?16-hi:lo,b=reverse?16-lo:hi;
    const rounded=v=>Math.round(v*1e8)/1e8;
    return {uv:[rounded(u0+(u1-u0)*a/16),rounded(v0+(v1-v0)*(16-y1)/14),
      rounded(u0+(u1-u0)*b/16),rounded(v0+(v1-v0)*(16-y0)/14)],texture:'#opening'};
  };
  const portUv=face=>({uv:portFaces[face].uv.map((v,i)=>v*16/(i%2?resolution.height:resolution.width)),texture:'#opening'});
  const plane=(name,d0,d1,c0,c1,y0,y1,face,uv)=>{
    const from=[],to=[];
    from[normalAxis]=Math.min(d0,d1);to[normalAxis]=Math.max(d0,d1);
    from[lateralAxis]=c0;to[lateralAxis]=c1;
    from[1]=y0;to[1]=y1;
    model.elements.push({name,from,to,faces:{[face]:uv}});
  };
  for(const [name,c0,c1,y0,y1] of [
    ['lower_border',0,16,2,4],['upper_border',0,16,14,16],
    ['left_border',0,2,4,14],['right_border',14,16,4,14]
  ])plane(`${side}_${name}`,outer,outer,c0,c1,y0,y1,side,uvPart(c0,c1,y0,y1));
  // Authored cube_outline runs from Y=14 to Y=4. Our positive-extent
  // plane must invert V as well, otherwise the socket center moves to Y=10.
  const inset=portUv('north');
  [inset.uv[1],inset.uv[3]]=[inset.uv[3],inset.uv[1]];
  plane(`${side}_inset`,inner,inner,2,14,4,14,side,inset);
  const firstWall=normalAxis===0?'south':'east';
  const lastWall=normalAxis===0?'north':'west';
  plane(`${side}_left_jamb`,outer,inner,2,2,4,14,firstWall,portUv('east'));
  plane(`${side}_right_jamb`,outer,inner,14,14,4,14,lastWall,portUv('west'));
  plane(`${side}_lower_jamb`,outer,inner,2,14,4,4,'up',portUv('up'));
  plane(`${side}_upper_jamb`,outer,inner,2,14,14,14,'down',portUv('down'));
}
for(const [id,folder] of [['cat_press','哈基辊压'],['cat_mixer','哈基搅拌']]){
  const sourceDir=path.join(root,'art/cat-machines/sixway',id);
  if(process.argv.includes('--import')){
    assert(write,'--import requires --write');
    mkdirSync(sourceDir,{recursive:true});
    const artist='D:/Project_minecraft/哈基机器/'+folder;
    for(const file of readdirSync(artist))copyFileSync(path.join(artist,file),path.join(sourceDir,file));
  }
  const src=JSON.parse(readFileSync(path.join(sourceDir,'model.bbmodel'),'utf8'));
  const main=src.elements.find(e=>e.uuid==='94929427-fc70-88b8-a6f9-02a2b5302e20');
  assert(main,'Authored shell is missing');
  const port=src.elements.find(e=>e.name==='cube_outline');
  assert(port,'Authored port outline is missing');
  const shell=exportDepot({...src,elements:[main],outliner:[main.uuid]});
  shell.textures={'0':'laowu:block/'+id,opening:'laowu:block/'+id+'_opening',particle:'laowu:block/'+id};
  let itemShell;
  for(const axis of ['closed','x','z']){
    const model=structuredClone(shell);
    if(axis!=='closed')for(const face of axis==='x'?['east','west']:['north','south'])recessPort(model,face,port.faces,src.resolution);
    if(axis==='x')itemShell=model;
    add('models/block/'+id+(axis==='closed'?'':'_open_'+axis)+'.json',model);
  }
  const groups=new Map(src.groups.map(g=>[g.uuid,g]));
  const shellGroup=src.outliner.find(n=>typeof n!=='string'&&groups.get(n.uuid)?.name==='bone4');
  assert(shellGroup,'Missing static shell group');
  const statics=new Set(shellGroup.children);
  const moving=structuredClone(src);
  moving.elements=moving.elements.filter(e=>!statics.has(e.uuid));
  moving.outliner=moving.outliner.filter(n=>n.uuid!==shellGroup.uuid);
  moving.animations=[];moving.textures=[];moving.history=[];
  add('models/blockbench/'+id+'_moving.bbmodel',moving);
  for(const axis of ['x','z']){
    const ports=structuredClone(src);
    ports.elements=ports.elements.filter(e=>statics.has(e.uuid)&&e.uuid!==main.uuid&&((Math.abs(e.rotation?.[1]??0)%180===90)?'x':'z')===axis);
    assert.equal(ports.elements.length,2);
    ports.outliner=[{...shellGroup,children:ports.elements.map(e=>e.uuid)}];ports.animations=[];ports.textures=[];ports.history=[];
    add('models/blockbench/'+id+'_ports_'+axis+'.bbmodel',ports);
  }
  const variants={};
  for(const [bottom,rotation]of Object.entries(frames))for(const shaft_axis of ['x','y','z'])for(const shaft_open of [false,true]){
    const axis=localAxis(bottom,shaft_axis);
    const valid=!(bottom==='up'||bottom==='down'?shaft_axis==='y':bottom==='east'||bottom==='west'?shaft_axis==='x':shaft_axis==='z');
    variants['bottom='+bottom+',shaft_axis='+shaft_axis+',shaft_open='+shaft_open]={model:'laowu:block/'+id+(valid?'_open_'+axis:''),...rotation};
  }
  add('blockstates/'+id+'.json',{variants});
  // Item includes the retracted rod/head as static geometry, not an empty shell.
  const itemSrc=structuredClone(src);
  itemSrc.elements=itemSrc.elements.filter(e=>!statics.has(e.uuid));
  itemSrc.outliner=itemSrc.elements.map(e=>e.uuid);
  // Rotor plane quarter turns are baked in a dedicated two-sided Java plane.
  const quarter=itemSrc.elements.filter(e=>Math.abs(e.rotation?.[1]??0)===90);
  itemSrc.elements=itemSrc.elements.filter(e=>!quarter.includes(e));
  const item=exportDepot(itemSrc);item.textures={...shell.textures,shaft:'create:block/axis',shaft_end:'create:block/axis_top'};
  item.elements.unshift(...structuredClone(itemShell.elements));
  // Inventory has no block entity renderer. Bake the same 4px native shaft,
  // along the item's default X axis, so both recessed openings are occupied.
  item.elements.push({name:'Inventory shaft',from:[0,6,6],to:[16,10,10],faces:{
    east:{uv:[6,6,10,10],texture:'#shaft_end'},
    west:{uv:[6,6,10,10],texture:'#shaft_end'},
    up:{uv:[6,0,10,16],rotation:90,texture:'#shaft'},
    down:{uv:[6,0,10,16],rotation:90,texture:'#shaft'},
    north:{uv:[6,0,10,16],rotation:90,texture:'#shaft'},
    south:{uv:[6,0,10,16],rotation:90,texture:'#shaft'}
  }});
  for(const e of quarter){
    const temp=exportDepot({...src,elements:[{...e,rotation:[0,0,0]}],outliner:[e.uuid]});
    item.elements.push(...turnModel(temp,((e.rotation[1]/90)%4+4)%4).elements);
  }
  // Create's mixer uses GUI rotation [30,225,0] and 0.45 scale. Our head extends
  // below the shell rather than above its center, so center it with a positive Y offset.
  if(id==='cat_mixer')item.display={...item.display,gui:{rotation:[30,225,0],translation:[0,2.25,0],scale:[.45,.45,.45]}};
  add('models/item/'+id+'.json',item);
  assets.set('textures/block/'+id+'.png',readFileSync(path.join(sourceDir,'texture.png')));
  assets.set('textures/block/'+id+'_opening.png',readFileSync(path.join(sourceDir,'开口.png')));
}
const basin=JSON.parse(readFileSync(path.join(root,'art/cat-machines/哈基机器/哈基工作盆/basin_block_directional.bbmodel'),'utf8'));
const depotDir=path.join(root,'art/cat-machines/sixway/cat_depot');
if(process.argv.includes('--import')){
  mkdirSync(depotDir,{recursive:true});
  const artist='D:/Project_minecraft/哈基机器/哈基置物台';
  for(const file of readdirSync(artist))copyFileSync(path.join(artist,file),path.join(depotDir,file));
}
add('models/block/cat_depot.json',exportDepot(JSON.parse(readFileSync(path.join(depotDir,'model.bbmodel'),'utf8'))));
assets.set('textures/block/cat_depot.png',readFileSync(path.join(depotDir,'texture.png')));
const bvariants={};
for(const [facing,turns]of Object.entries({down:0,south:0,east:1,north:2,west:3})){
  const name=facing==='down'?'haji_basin':'haji_basin_spout_'+facing;
  add('models/block/'+name+'.json',turnModel(exportBasin(basin,facing!=='down'),turns));
  for(const [bottom,rot]of Object.entries(frames))bvariants['bottom='+bottom+',facing='+facing]={model:'laowu:block/'+name,...rot};
}
add('blockstates/haji_basin.json',{variants:bvariants});
add('blockstates/cat_depot.json',{variants:Object.fromEntries(Object.entries(frames).map(([bottom,rot])=>['bottom='+bottom,{model:'laowu:block/cat_depot',...rot}]))});
for(const loader of ['forge-1.20.1','neoforge-1.21.1'])for(const[p,bytes]of assets){
  const target=path.join(root,loader,'src/main/resources/assets/laowu',p);
  if(write){mkdirSync(path.dirname(target),{recursive:true});writeFileSync(target,bytes);}
  else assert.deepEqual(readFileSync(target),bytes,loader+'/'+p);
}
console.log('PASS: '+assets.size+' six-way assets per loader');
