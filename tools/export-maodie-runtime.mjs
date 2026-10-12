import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import path from 'node:path';
const root=path.resolve(import.meta.dirname,'..');
const source=JSON.parse(readFileSync(path.join(root,'art/maodie-fighter-v11/maodie_fighter_v11.bbmodel')));
const model={meta:source.meta,resolution:source.resolution,elements:source.elements,groups:source.groups,outliner:source.outliner,textures:[{id:'0',uv_width:64,uv_height:32}]};
const clips={};
for(const a of source.animations.filter(a=>!a.name.endsWith('.combat_flow'))){
 const bones={};for(const track of Object.values(a.animators)){
  const channels={};for(const c of ['position','rotation','scale'])channels[c]=track.keyframes.filter(k=>k.channel===c).map(k=>[k.time,...['x','y','z'].map(n=>+k.data_points[0][n])]);
  bones[track.name]=channels;
 }
 clips[a.name.replace('animation.maodie.','')]={length:a.length,loop:a.loop==='loop',bones};
}
for(const port of ['forge-1.20.1','neoforge-1.21.1']){
 const assets=path.join(root,port,'src/main/resources/assets/laowu');
 for(const dir of ['models/entity','cat_animation_clips','textures/entity'])mkdirSync(path.join(assets,dir),{recursive:true});
 writeFileSync(path.join(assets,'models/entity/maodie_fighter.bbmodel'),JSON.stringify(model));
 writeFileSync(path.join(assets,'cat_animation_clips/maodie_fighter.json'),JSON.stringify({version:1,source:'maodie-fighter-v11',clips}));
 writeFileSync(path.join(assets,'textures/entity/maodie_fighter.png'),Buffer.from(source.textures[0].source.split(',')[1],'base64'));
}
console.log('Exported approved v11 geometry, atlas and eight gameplay clips to both loaders');
