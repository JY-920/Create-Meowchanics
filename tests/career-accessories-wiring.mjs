import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8').replaceAll('\r\n','\n');
const manifest=JSON.parse(read('art/career-accessories-v1/manifest.json')).items;
let checks=0;
const check=(ok,why)=>{checks++;assert.ok(ok,why);};
for(const port of ['forge-1.20.1','neoforge-1.21.1']){
 const java=n=>read(port+'/src/main/java/cn/laowu/mod/'+n+'.java');
 const assets=port+'/src/main/resources';
 const costs={cat_followup_gear:['terminator','attack',-20],cat_ace_feather:['flight','attack',-20],cat_blast_fuse:['dynamite','health',-20]};
 for(const item of manifest){const stat=Object.keys(item.effects).find(k=>['health','attack','speed','stamina','intelligence','luck'].includes(k));costs[item.id]=[item.outfit,stat,item.effects[stat]];}
 check(Object.keys(costs).length===13&&new Set(Object.values(costs).map(x=>x[0])).size===13,'One intended exclusive per career');
 for(const [id,[outfit,stat,value]] of Object.entries(costs)){
  const def=JSON.parse(read(assets+'/data/laowu/cat_accessories/'+id+'.json'));
  check(def.item==='laowu:'+id&&def.required_outfit===outfit&&def.effects[stat]===value,'Exact career restriction and attribute cost: '+id);
  for(const lang of ['zh_cn','en_us']){
   const strings=JSON.parse(read(assets+'/assets/laowu/lang/'+lang+'.json'));
   check(!!strings['item.laowu.'+id],'Localized exclusive name '+id);
  }
 }
 const events=java('CommonEvents');
 check((events.includes('if (!split && (cat.isTame() || outfit != CatOutfitType.NONE)')||events.includes('if ((cat.isTame() || outfit != CatOutfitType.NONE) && !split)'))&&events.includes('dropOnDeath(cat,!split)'),'Split never also creates a revivable third cat or copies contents, including the new tamed-cat path');
 check(events.indexOf('CatHealingSmoke.flush(level)')<events.indexOf('CatMedicalHealing.flush(level)'),'Smoke joins strongest shared healing flush');
 const split=java('CatCockroachSplit');
 for(const key of ['parent.getHealth()>0','parent.getOwnerUUID()','genes.current(stat)/2','CatTraitProfile.EMPTY','children[0].discard()','setAge(-24000)'])
  check(split.includes(key),'Split inheritance/rollback boundary: '+key);
 const auras=java('accessory/CatAccessoryAuras');
 for(const key of ['Math.max(best,offer.amount)','source.isRemoved()','offer.until<=','CatMedicalHealing.casting(source)','addTransientModifier','removeModifier'])
  check(auras.includes(key),'Nonstacking/removable aura '+key);
 const cannon=java('entity/EngineeringCannon'),ammo=java('CatArtilleryMunition');
 check(cannon.includes('CatArtilleryMunition.select(cat)')&&cannon.includes('munition.damageMultiplier()'),'Actual cannon selection/damage hook');
 check(ammo.includes('engineering_special_ammo')&&ammo.includes('SMALL_COG')&&ammo.includes('1.2'),'Exclusive ammo gate and shaft scaling');
 const defs=java('accessory/CatAccessoryDefinition');
 check(defs.includes('Math.min(0.8D'),'Pilot dodge caps at 80%');
 for(const effect of ['fishing_pull','super_flame_multiplier','honey_patch','enhanced_potions','engineering_special_ammo','medical_guard','music_movement_bonus','healing_smoke','diving_cleanse','cockroach_split'])
  check(defs.includes('"'+effect+'"'),'Additive schema mechanism '+effect);
 const honey=java('entity/CatHoneyPatch');
 check(!/setBlock|destroyBlock|explode\(/.test(honey),'Honey decal never mutates terrain');
 check(java('client/CatHoneyPatchRenderer').includes('honey_block_top.png'),'Vanilla ground texture');
 check(java('client/CatHealingSmokeParticle').includes('extends TextureSheetParticle')&&java('client/CatHealingSmokeParticle').includes('setColor(.20F, .82F, .30F)'),'Dedicated vanilla-style green smoke');
 check(java('CatAgentWatch').includes('MAX_RADIUS = 128')&&java('CatAgentWatch').includes('* .64'),'Cap raised without changing intelligence ratio');
 check(java('client/HissingCatModel').includes('CatPoseTransitions.sample')&&java('client/HissingCatModel').includes('CatPoseTransitions.apply'),'Production pose transitions wired');
 check(java('client/CatPoseTransitions').includes('CatVisualStates<State>')&&java('client/CatPoseTransitions').includes('active?6:8'),'Identity-isolated six/eight-tick transition');
 for(const item of manifest){
  const model=JSON.parse(read(assets+'/assets/laowu/models/item/'+item.id+'.json'));
  check(model.textures.layer0===(item.source?'laowu:item/'+item.id:'minecraft:item/'+item.icon),'Supplied icon or remaining placeholder '+item.id);
 }
}
console.log('PASS: '+checks+' career-exclusive costs, scope, original art placeholders, aura cleanup, split no-copy and transition source guards');
