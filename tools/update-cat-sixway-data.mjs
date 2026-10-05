import {readFileSync,writeFileSync} from 'node:fs';
import path from 'node:path';
const root=process.cwd();
for(const loader of ['forge-1.20.1','neoforge-1.21.1']){
 const resources=path.join(root,loader,'src/main/resources');
 const neo=loader.startsWith('neo');
 for(const locale of ['zh_cn','en_us']){
  const file=path.join(resources,'assets/laowu/lang',locale+'.json');
  const j=JSON.parse(readFileSync(file,'utf8'));
  j['block.laowu.cat_press']=locale==='zh_cn'?'猫冲压机':'Cat Mechanical Press';
  j['block.laowu.cat_mixer']=locale==='zh_cn'?'猫搅拌器':'Cat Mechanical Mixer';
  writeFileSync(file,JSON.stringify(j,null,2)+'\n');
 }
 const file=path.join(resources,'data/minecraft/tags',neo?'block':'blocks','mineable/pickaxe.json');
 const j=JSON.parse(readFileSync(file,'utf8'));
 for(const id of ['cat_press','cat_mixer']){
  if(!j.values.includes('laowu:'+id))j.values.push('laowu:'+id);
  const loot={type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:'laowu:'+id}],conditions:[{condition:'minecraft:survives_explosion'}]}]};
  writeFileSync(path.join(resources,'data/laowu',neo?'loot_table':'loot_tables','blocks',id+'.json'),JSON.stringify(loot,null,2)+'\n');
 }
 writeFileSync(file,JSON.stringify(j,null,2)+'\n');
}

