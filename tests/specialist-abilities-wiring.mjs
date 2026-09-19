import assert from 'node:assert/strict';
import fs from 'node:fs';
const root=new URL('../',import.meta.url),read=p=>fs.readFileSync(new URL(p,root),'utf8').replaceAll('\r\n','\n');
let n=0;const check=(v,m)=>{n++;assert.ok(v,m);};
for(const p of ['forge-1.20.1','neoforge-1.21.1']){
 const j=f=>read(p+'/src/main/java/cn/laowu/mod/'+f+'.java');
 check(j('CatCockroachSwarm').includes('PER_ALLY = 6'),'each nearby friendly cockroach grants six to health and power');
 for(const lang of ['zh_cn','en_us']){
  const tooltip=JSON.parse(read(p+'/src/main/resources/assets/laowu/lang/'+lang+'.json'))['item.laowu.cockroach_suit.tooltip.behaviour1'];
  check(tooltip.includes('+6')&&!tooltip.includes('+5'),'localized swarm bonus matches live six-point rule');
 }
 const outline=j('client/CatPerformanceOutline');
 check(outline.includes('Stage.AFTER_ENTITIES')&&outline.includes('CatMedicalEffects.draw(event,worldView,worldProjection')&&outline.includes('CatMusicEffects.draw(event,worldView,worldProjection'),'glyphs use saved entity-stage matrices');
 check(outline.includes(p.startsWith('forge')?'new Matrix4f(event.getPoseStack().last().pose())':'new Matrix4f(event.getModelViewMatrix())'),'loader-specific actual world view');
 check(!j('CatMusicRecords').includes('findSeat('),'nine-slot records do not require cushion');
 const audio=j('client/CatMusicRecordClient');
 check(audio.includes('PENDING')&&audio.includes('canStartSilent() { return true; }'),'late-tracking retry and muted-start sound');
 check(j('genetics/CatAttributeEffects').includes('CatOutfitType outfit)')&&j('genetics/CatAttributeEffects').includes('CatCockroachSwarm.statBonus'),'all real suit bonuses plus transient swarm stats');
 const model=j('client/HissingCatModel');
 check(model.includes('CatMedicalAnimation.applyStationed')&&model.includes('CatDivingMount.swimming(cat)')&&model.includes('CatRideAnimation.apply'),'seat-specific pose and water-only diver pose');
 const smoke=j('CatAgentSmoke');
 check(smoke.includes('hasMemoryValue(MemoryModuleType.ATTACK_TARGET)')&&smoke.includes('hasMemoryValue(MemoryModuleType.ANGRY_AT)'),'safe goal/brain mobs');
 check(j('CatAgentCombatGoal').includes('flankPosition(target)')&&j('CatAgentCombatGoal').includes('hasNearbyAlly(cat)')&&j('CatAgentCombatGoal').includes('path.canReach()'),'flank around shoulders, alone gate and real path');
 const mount=j('entity/CatDivingCarrier');
 check(mount.includes('sender.getVehicle()!=this')&&mount.includes('validInput')&&mount.includes('hasChunksAt')&&mount.includes('move(MoverType.SELF,motion)'),'server-owned bounded packet validation, loaded chunks and real collisions');
 check(mount.includes('setAirSupply')&&!mount.includes('addEffect('),'powered air only, no lingering potion');
 check(j('CatDivingRules').includes('1.30')&&j('CatDivingRules').includes('.20'),'water boost and land penalty');
 const clip=name=>JSON.parse(read(p+'/src/main/resources/assets/laowu/cat_animation_clips/'+name+'.json'));
 const dash=clip('cockroach_dash'),wings=clip('cockroach_wings');
 check(dash.loop&&!wings.loop&&dash.length===.5&&wings.length===.5,'source animation timing and one-shot hurt');
 check(Object.keys(wings.tracks).sort().join(',')==='group10,group11','hurt clip contains no limb tracks');
 for(const bone of ['group10','group11'])check(JSON.stringify(dash.tracks[bone])===JSON.stringify(wings.tracks[bone]),'same authored wing keys');
 for(const f of ['CatDivingRules','CatDivingMount','CatAgentCombatGoal','CatAgentSmoke','CatCockroachSwarm','client/CatCockroachAnimation','client/CatRideAnimation']){
  if(p.startsWith('neoforge'))check(j(f)===read('forge-1.20.1/src/main/java/cn/laowu/mod/'+f+'.java'),'dual-port shared '+f);
 }
}
console.log('PASS: '+n+' specialist ability, animation split, glyph matrix and mobile audio wiring checks');
