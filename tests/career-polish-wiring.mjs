import assert from 'node:assert/strict';
import fs from 'node:fs';
const root=new URL('../',import.meta.url),read=p=>fs.readFileSync(new URL(p,root),'utf8').replaceAll('\r\n','\n');
let checks=0;const check=(v,m)=>{checks++;assert.ok(v,m);};
const ids=['terminator','fishing','flight','fire','honey','transport','dynamite','engineering','medical','music','agent','diving','cockroach'];
for(const port of ['forge-1.20.1','neoforge-1.21.1']){
 const j=f=>read(port+'/src/main/java/cn/laowu/mod/'+f+'.java');
 check(!j('CatCockroachSwarm').includes('stat == CatStat.STAMINA'),'swarm never increases stamina');
 const farm=j('CatCockroachFarming');
 for(const token of ['INTERVAL = 100, HALF_SIZE = 4','health) / 200.0','instanceof FarmBlock','isValidBonemealTarget','BoneMealItem.growCrop','hasChunkAt','CatProfileData.isBeingViewed','getTarget()'])
   check(farm.includes(token),'farm guard '+token);
 check(!farm.includes('ItemEntity')&&!farm.includes('discard()'),'discarded scavenger proposal is not implemented');
 const water=j('CatDivingAttack');
 for(const token of ['isBeneficial()', 'new ArrayList<>(other.getActiveEffects())','CatTeamRules.canHarm','hasLineOfSight','ParticleTypes.SPLASH'])
   check(water.includes(token),'spray guard '+token);
 check(j('CareerCatBehavior').includes('CatDivingAttack.spray(cat, target)'),'real attack goal calls spray');
 check(j('CatAgentCombatGoal').includes('CatAgentMeleeMotion.begin(cat)'),'real attack starts cosmetic animation');
 check(j('network/ModNetwork').includes('AgentMeleePacket')&&j('CommonEvents').includes('CatAgentMeleeMotion.syncTo(cat, player)'),'new tracking and live attacks synced');
 check(j('client/ClientPacketHandler').includes('cat.getUUID().equals(packet.uuid())'),'reused entity IDs are checked');
 const tooltip=j('client/CareerSuitTooltip');
 check(tooltip.includes('List.of("combat", "work")'),'default tooltip exactly battle and work');
 check(tooltip.includes('Screen.hasControlDown()')&&tooltip.includes('Screen.hasShiftDown()'),'details retain both modifier keys');
 for(const lang of ['zh_cn','en_us']){
  const t=JSON.parse(read(port+'/src/main/resources/assets/laowu/lang/'+lang+'.json'));
  for(const id of ids){
   const key='item.laowu.'+id+'_suit.tooltip.';
   check(!!t[key+'combat']&&!!t[key+'work'],'two concise sections '+id);
   check(!/[×=]|_K|寻路|pathfind|SU|tick|128|120°/.test(t[key+'combat']),'no formulas, ranges or technical internals in default combat '+id);
   check(!/[=]|_K|寻路|pathfind|SU|tick/.test(t[key+'work']),'no formulas or technical internals in default work '+id);
   for(let n=1;n<=3;n++)check(t[key+'condition'+n]===t['item.laowu.honey_suit.tooltip.condition'+n],'consistent Ctrl headings '+id);
  }
  const f=t['item.laowu.fishing_suit.tooltip.work'];
  check(lang==='zh_cn'?f.includes('坐垫旁有容器')&&f.includes('坐垫底部方块周围有水域'):f.includes('container')&&f.includes('beneath the cushion'),'fisher placement conditions retained');
  check(!t['item.laowu.cockroach_suit.tooltip.combat'].includes('Stamina')&&!t['item.laowu.cockroach_suit.tooltip.combat'].includes('耐力'),'default swarm description matches two stats');
 }
}
console.log('PASS: '+checks+' farming, spray, attack-sync and thirteen concise dual-language suit descriptions');
