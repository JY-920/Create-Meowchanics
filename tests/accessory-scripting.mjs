import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';
const base = new URL('../', import.meta.url);
const read = p => readFileSync(new URL(p, base), 'utf8').replaceAll('\r\n', '\n');
let checks=0;
const check = (x,message) => { checks++; assert.ok(x,message); };
for (const loader of ['forge-1.20.1','neoforge-1.21.1']) {
 const java = n => read(loader+'/src/main/java/cn/laowu/mod/'+n+'.java');
 const hooks=java('accessory/CatAccessoryHooks'), handle=java('api/CatAccessoryHandle');
 for (const event of ['equip','unequip','tick','beforeAttack','afterAttack','beforeHurt','afterHurt','kill','projectile','beforeExplosion'])
   check(hooks.includes('"'+event+'"'),'event '+event);
 const bridge=java('compat/kubejs/CatAccessoriesKubePlugin');
 check(bridge.includes('GROUP.server') && bridge.includes('installBridge'), 'server-only optional bridge');
 const plugins=read(loader+'/src/main/resources/kubejs.plugins.txt').trim().split('\n');
 check(plugins.length===2 && new Set(plugins).size===2 &&
   plugins.includes('cn.laowu.mod.compat.kubejs.CatAccessoriesKubePlugin') &&
   plugins.includes('cn.laowu.mod.compat.kubejs.CatTraitsKubePlugin'),'both optional plugins discovered once');
 check(hooks.includes('AFTER_DAMAGE.size() < 2048') && hooks.includes('flushAfterDamage'), 'bounded post-damage queue');
 check(java('accessory/CatAccessoryEvents').includes('CatAccessoryHooks.flushAfterDamage()'), 'tick-end flush wired');
 check(hooks.includes('DEPTH.set(previous)') && java('api/CatAccessoryApi').includes('CatAccessoryHooks.withoutEvents'), 'recursion finally protection');
 check(handle.includes('tryActivate') && handle.includes('chargeCost') && handle.includes('now + ticks'), 'charge and cooldown reservation');
 check(handle.includes('container.setChanged()') && handle.includes('stack != identity'), 'live inventory persistence and stale-handle guard');
 check(handle.includes('setStatBonus') && java('accessory/CatAccessories').includes('StatBonuses'), 'dynamic bonuses reach effective stat pipeline');
 check(java('api/CatAccessoryApi').includes('CatTeamRules.canHarm') && java('api/CatAccessoryApi').includes('isSameThread()'), 'team and thread guards');
 check(java('CatProfileData').includes('preserveInPancake && (cat.isTame() || CatClothesData.getOutfit(cat) != CatOutfitType.NONE)) return'), 'tamed and career pancakes own inventory, unless split replaces the pancake');
 check(java('CareerCatBehavior').match(/CatAccessoryHooks\.projectile\(/g).length===5,'all five projectile launches wired');
 check(java('DynamiteCatLastStand').includes('CatAccessoryHooks.beforeExplosion'), 'final explosion hook');
 const build=read(loader+'/build.gradle');
 check(build.includes('compileOnly(') && !/implementation\([^\n]*kubejs/.test(build),'no mandatory implementation dependency');
 for (const shot of ['MechanicalLaserProjectile','HoneyMissileProjectile','DynamiteProjectile','FishingRodProjectile'])
   check(java('entity/'+shot).includes('implements cn.laowu.mod.api.CatAccessoryProjectile'),'projectile damage API '+shot);
}
for (const file of ['CatAccessoryHooks','CatAccessoryCharge','CatAccessoryScriptRules']) {
 check(read('forge-1.20.1/src/main/java/cn/laowu/mod/accessory/'+file+'.java') ===
       read('neoforge-1.21.1/src/main/java/cn/laowu/mod/accessory/'+file+'.java'), 'same common rules '+file);
}
const handlers={}, state=new Map(), stats=new Map();
let now=0, charge=20, healed=0, night=false, equipped=true, deadline=0;
const handle={
 tryActivate(key,ticks,cost){if(now<deadline||charge<cost)return false; charge-=cost;deadline=now+ticks;return true;},
 increment(key,n){state.set(key,(state.get(key)||0)+n);},
 setStatBonus(key,n){stats.set(key,n);}
};
const ctx={cat:{level:{isNight:()=>night}},accessory:()=>equipped?handle:null};
const api={nearbyAllies:()=>[{},{}],heal:()=>{healed++;return true;}};
vm.runInNewContext(read('docs/examples/cat-accessories/server_scripts/mechanisms.js'),{
 Java:{loadClass:name=>{check(name==='cn.laowu.mod.api.CatAccessoryApi','public facade');return api;}},
 CatAccessoryEvents:{afterAttack:fn=>handlers.hit=fn,tick:fn=>handlers.tick=fn}
});
handlers.hit({context:ctx}); handlers.hit({context:ctx});
check(charge===19&&healed===2,'heal-wave example costs exactly one charge per cooldown');
now=200;handlers.hit({context:ctx});check(charge===18&&healed===4,'cooldown expiry permits next wave');
handlers.tick({context:ctx});check(stats.get('attack')===0,'daytime bonus cleared');
night=true;handlers.tick({context:ctx});check(stats.get('attack')===8,'nighttime bonus applied');
equipped=false;now=400;handlers.hit({context:ctx});check(charge===18,'unequipped cannot activate');
console.log('PASS: '+checks+' accessory script wiring/example checks (real runtime checked separately)');
