import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const source='D:/Project_minecraft/待实现/生物转信器';
const bb=JSON.parse(fs.readFileSync(path.join(source,'model.bbmodel')));
const e=bb.elements[0];
const faces=Object.fromEntries(Object.entries(e.faces).map(([side,f])=>[side,{uv:f.uv.map(v=>v/2),texture:'#all',cullface:side}]));
const model={parent:'minecraft:block/block',textures:{all:'laowu:block/creature_transmitter',particle:'#all'},
 elements:[{from:[0,0,0],to:[16,16,16],faces}]};
for(const loader of ['forge-1.20.1','neoforge-1.21.1']){
 const r=path.join(root,loader,'src/main/resources');
 const write=(name,data)=>{const p=path.join(r,name);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(data,null,2)+'\n');};
 write('assets/laowu/models/block/creature_transmitter.json',model);
 write('assets/laowu/models/item/creature_transmitter.json',{parent:'laowu:block/creature_transmitter'});
 write('assets/laowu/blockstates/creature_transmitter.json',{variants:{'':{model:'laowu:block/creature_transmitter'}}});
 fs.copyFileSync(path.join(source,'texture.png'),path.join(r,'assets/laowu/textures/block/creature_transmitter.png'));
 write('data/laowu/'+(loader.startsWith('neo')?'loot_table':'loot_tables')+'/blocks/creature_transmitter.json',
 {type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:'laowu:creature_transmitter'}],conditions:[{condition:'minecraft:survives_explosion'}]}]});
}

