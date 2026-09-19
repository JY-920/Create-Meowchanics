import assert from 'node:assert/strict';
import {readFileSync,readdirSync} from 'node:fs';
import {createHash} from 'node:crypto';
const root=new URL('../',import.meta.url);
const read=p=>readFileSync(new URL(p,root),'utf8').replaceAll('\r\n','\n');
const fresh=JSON.parse(read('art/accessories-art-v3/manifest.json')).items;
const original=JSON.parse(read('art/accessories-redrawn-v2/manifest.json')).items;
const career=JSON.parse(read('art/career-accessories-v1/manifest.json')).items;
const general=JSON.parse(read('art/general-accessories-v1/manifest.json')).items;
let checks=0;
const check=(ok,m)=>{checks++;assert.ok(ok,m);};
check(fresh.length===20&&original.length===16,'20 delivered icons plus 16 previous originals');
check(new Set([...fresh,...original].map(x=>x.id)).size===36,'Distinct supplied icons, no accidental double mapping');
check([...career,...general].filter(x=>!x.source).map(x=>x.id).join(',')==='','All accessories have supplied artwork');
for(const port of ['forge-1.20.1','neoforge-1.21.1']){
 const assets=port+'/src/main/resources/assets/laowu/';
 const names=JSON.parse(read(assets+'lang/zh_cn.json'));
 for(const item of [...original,...fresh]){
  const bytes=readFileSync(new URL(assets+'textures/item/'+item.id+'.png',root));
  check(bytes.readUInt32BE(16)===16&&bytes.readUInt32BE(20)===16,'16x16 original '+item.id);
  check(createHash('sha256').update(bytes).digest('hex')===item.sha256,'Unmodified artist bytes '+item.id);
  check(names['item.laowu.'+item.id]===item.name,'Final short name '+item.id);
  const model=JSON.parse(read(assets+'models/item/'+item.id+'.json'));
  check(model.textures.layer0==='laowu:item/'+item.id,'Stable ID points at its new icon '+item.id);
 }
 const def=JSON.parse(read(port+'/src/main/resources/data/laowu/cat_accessories/cat_rhythm_tambourine.json'));
 assert.deepEqual(def.effects,{health:-10,music_speed_bonus:20});checks++;
 const java=n=>read(port+'/src/main/java/cn/laowu/mod/'+n+'.java');
 check(java('accessory/CatAccessories').includes('CatStat.SPEED ? CatAccessoryAuras.speedBonus(cat)'),'Speed uses the six-stat path');
 check(java('accessory/CatAccessories').includes('state.putInt("MusicSpeedBonus"'),'Temporary state sent to clients');
 const auras=java('accessory/CatAccessoryAuras');
 check(auras.includes('CatAttributeEffects.refresh(cat)')&&auras.includes('CatAccessories.syncDynamic(cat)'),'State transitions refresh mechanics and panel');
 check(auras.includes('Math.max(best,offer.amount)')&&auras.includes('source.isRemoved()')&&auras.includes('offer.until<='),'Source removal, expiration and no stacking');
 const unchanged=['genetics/CatAttributeData','genetics/CatAttributeProfile'].map(java).join('\n');
 check(!unchanged.includes('music_speed_bonus'),'No music aura in persisted genes');
}
console.log('PASS: '+checks+' supplied icon hashes, final names, complete art audit and music Speed state wiring checks');
