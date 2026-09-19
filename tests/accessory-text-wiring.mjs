import assert from 'node:assert/strict';
import fs from 'node:fs';
const root=new URL('../',import.meta.url);
const read=p=>fs.readFileSync(new URL(p,root),'utf8');
const json=p=>JSON.parse(read(p));
let count=0;
const check=(v,m)=>{count++;assert.ok(v,m);};
for(const loader of ['forge-1.20.1','neoforge-1.21.1']){
 const res=loader+'/src/main/resources/';
 for(const lang of ['zh_cn','en_us']){
  const strings=json(res+'assets/laowu/lang/'+lang+'.json');
  for(const [key,value]of Object.entries(strings)){
   if(!key.startsWith('cat_accessory.laowu.effect.'))continue;
   for(let i=0;i<value.length;i++)if(value[i]==='%'){
    const token=value.slice(i).match(/^%(?:%|(?:[1-9][0-9]*\$)?s)/);
    check(token,loader+' '+lang+' invalid Minecraft translation escape: '+key+' / '+value);
    i+=token[0].length-1;
   }
  }
  const effect=k=>strings['cat_accessory.laowu.effect.'+k];
  check(effect('pilot_dodge_per_speed').includes('80%%'),'Literal cap is escaped so earlier placeholder is not discarded');
  check(!/冷却|cooldown/i.test(effect('opening_damage')),'Plush text no longer claims a cooldown');
  check(!effect('emergency_shield').includes('不倒翁')&&!effect('emergency_shield').includes('Roly-Poly'),'Current mutual exclusion name');
  check(effect('heavy_hit_cap').match(/%1\$s%%/g)?.length===2,'Both cap numbers remain configurable');
 }
 const zh=json(res+'assets/laowu/lang/zh_cn.json'),v=k=>zh['cat_accessory.laowu.effect.'+k];
 check(v('fishing_pull')==='钓鱼钩命中后改为将敌人拉向自身。','Concise hook');
 check(v('super_flame_multiplier')==='喷射蓝色超级火焰，喷火伤害×%s。','Concise super flame');
 check(v('honey_patch')==='命中后在敌人脚底留下蜂蜜渍，持续5秒，减速接触到的生物。','Concise honey');
 check(v('melee_reflect')==='近身受击后反弹实际伤害的%s%%。','Concise reflected actual damage');
 check(v('owner_attack_bonus')==='主人在4格范围内时，战斗力属性+%s。','Concise owner range');
 check(v('sample_pickup')==='只自动拾取自身9格物品栏内已有同类物品的掉落物；可搭配磁铁。','Concise sample pickup');
 check(zh['item.laowu.cat_roly_poly']==='金基咪'&&v('emergency_shield').includes('金基咪'),'Canonical Kimi name');
}
console.log('PASS: '+count+' accessory translation escapes, concise text and consistent names (real rendering checked separately)');
