import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=p=>readFileSync(path.join(root,p),'utf8').replaceAll('\r\n','\n');
const art=JSON.parse(read('art/general-accessories-v1/manifest.json')).items;
let checks=0;
const check=(x,m)=>{checks++;assert.ok(x,m);};
check(art.length===10,'Ten general accessories');
let shared;
for(const loader of ['forge-1.20.1','neoforge-1.21.1']){
 const base=loader+'/src/main/';
 const mechanics=read(base+'java/cn/laowu/mod/accessory/CatCommonAccessories.java');
 if(shared)check(mechanics===shared,'Shared mechanics exactly equivalent across loaders');shared=mechanics;
 for(const item of art){
   const def=JSON.parse(read(base+'resources/data/laowu/cat_accessories/'+item.id+'.json'));
   check(def.item==='laowu:'+item.id,'Stable ID: '+item.id);
   check(!def.required_outfit||def.required_outfit==='any','All careers can equip');
   check(Object.values(def.effects).every(v=>v>=0),'No negative costs');
   check(JSON.stringify(def.effects)===JSON.stringify(item.effects),'Art brief effect mapping');
 }
 check(!mechanics.includes('OpenerReady')&&!mechanics.includes('hit.opener'),'Plush has no new or legacy cooldown gate');
 check(read(base+'java/cn/laowu/mod/accessory/CatAccessoryRarity.java').includes('"cat_roly_poly", "cat_chew_bone"'),'Native Epic chew bone');
 check(JSON.parse(read(base+'resources/data/laowu/cat_accessories/cat_spiked_collar.json')).effects.melee_reflect===50,'Fifty percent reflection');
 check(mechanics.includes('owner.distanceToSqr(cat) <= 16'),'Four blocks inclusive, three-dimensional');
 check(mechanics.includes('owner.level() == cat.level()'),'Owner dimension guard');
 check(mechanics.includes('hit.eligible = true')&&mechanics.includes('!hit.eligible'),'Kill retains pre-hit eligibility');
 check(mechanics.includes('hit.prevented')&&mechanics.includes('hit.allowed.getAsBoolean()'),'Death prevention and late cancellation guard');
 check(mechanics.includes('hit.beforeHealth - victim.getHealth()'),'Actual health loss, not raw hurt amount');
 check(mechanics.includes('now(cat) - s.lastStack >= 10')&&mechanics.includes('now(cat) - s.lastHit >= 80'),'Combo limits');
 check(mechanics.includes('Math.min(5, s.stacks + 1)'),'Five stack maximum');
 check(mechanics.includes('putLong("MintUntil", now(cat) + 80)')&&mechanics.includes('putLong("MintReady", now(cat) + 160)'),'Mint separate duration/cooldown');
 check(mechanics.includes('putLong("ShieldReady", now(cat) + 300)')&&mechanics.includes('putLong("RolyReady", now(cat) + 300)'),'Saved fifteen-second cooldowns');
 check(!mechanics.includes('setAbsorptionAmount(')&&!mechanics.includes('addEffect(new MobEffectInstance(MobEffects.ABSORPTION'),'Does not overwrite external absorption');
 check(mechanics.includes('REFLECTING.remove()')&&mechanics.includes('damageSources().thorns(cat)'),'Reflection recursion guard');
 check(mechanics.includes('CatProfileData.ACCESSORY_SLOTS; slot < CatProfileData.SLOT_COUNT'),'Samples in nine-slot cargo only');
 check(read(base+'java/cn/laowu/mod/CatMusicSupport.java').includes('CatCommonAccessories.haste(cat)'),'Haste routed to existing career attack controllers');
 check(read(base+'java/cn/laowu/mod/accessory/CatAccessoryEvents.java').includes('LivingHealEvent'),'All normal healing shares one received-healing hook');
 const mix=read(base+'java/cn/laowu/mod/mixin/LivingEntityCatAccessoryDamageMixin.java');
 check(mix.includes('@At("RETURN")')&&!mix.includes('@Redirect')&&!mix.includes('cancellable'),'Read-only post-health injection');
}
console.log('PASS: '+checks+' general accessory definitions, no-penalty balance, timing, save, team and loader phase guards (gameplay checked separately)');
