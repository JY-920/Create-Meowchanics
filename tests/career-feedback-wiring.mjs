import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
let checks=0;
const check=(condition,why)=>{checks++;assert.ok(condition,why);};
const read=p=>fs.readFileSync(path.join(root,p),'utf8').replaceAll('\r\n','\n');
for(const port of ['forge-1.20.1','neoforge-1.21.1']){
 const java=n=>read(port+'/src/main/java/cn/laowu/mod/'+n+'.java');
 const cache=java('CatVisualStates'),medic=java('CatMedicalHealing'),music=java('CatMusicSupport');
 check(cache.includes('Map<Level, Bucket<T>>')&&cache.includes('WeakReference<Entity>')&&cache.includes('entry.entity.get() == entity'),'World, UUID, exact-entity and weak lifetime cache');
 check(medic.includes('CatVisualStates<Visual>')&&music.includes('CatVisualStates<State>'),'Both support visual state users are authority isolated');
 check(music.includes('visualRadius(Cat cat)'),'Synchronized music radius exposed for client circles');
 const outline=java('client/CatPerformanceOutline'),areas=java('client/CatSupportAreas');
 check(outline.includes('collectIndicators(')&&outline.includes('entitiesForRendering()'),'Indicators collected independently of mesh capture');
 check(outline.includes('AFTER_SOLID_BLOCKS')&&outline.includes('indicators.empty()'),'Solid-stage fallback and circle-only draw');
 for(const symbol of ['getProjectionMatrix()','getModelViewStack()','applyModelViewMatrix()','setProjectionMatrix(previousProjection,sorting)','setShaderColor(previousColor[0]'])
  check(areas.includes(symbol),'Ring camera and GUI state: '+symbol);
 check(java('client/CatMedicalEffects').includes('CatSupportAreas.draw')&&java('client/CatMusicEffects').includes('CatSupportAreas.draw'),'Both radius circles use corrected camera path');
 const leap=java('CatCockroachCombat'),career=java('CareerCatBehavior');
 for(const fragment of ['Flag.JUMP','CatCockroachSwarm.leap(cat,true)','NEXT_ATTACK','CatTeamRules.canHarm','cat.hasLineOfSight(target)','careerAttackIntervalTicks(cat)','if(!attacked)'])
  check(leap.includes(fragment),'Winged leap safety/pacing: '+fragment);
 check(career.includes('new CatCockroachCombat(cat)')&&career.includes('CatCockroachCombat.tryAttack(cat, target)'),'Jump and ordinary bite both wired');
 const attack=java('CatAgentMeleeMotion'),agent=java('CatAgentCombatGoal');
 check(attack.includes('if (current(cat) != null) return;')&&attack.includes('Math.max(12')&&attack.includes('(move + 1) % 3'),'Agent motions play fully and cycle');
 check(agent.includes('readyToRetreat()')&&agent.includes('candidatePath.canReach()')&&agent.includes('Vec3 point = cat.position()'),'Threat response retains smoke even without a path');
 const smokeGate=agent.slice(agent.indexOf('private boolean readyToRetreat()'),agent.indexOf('@Override public boolean canContinueToUse()'));
 check(agent.includes('CatAgentSmoke.burst(cat, cat.position())')&&!agent.includes('new AgentSmokeBomb'),'Retreat smoke is immediate with no thrown item');
 check(java('client/ClientModEvents').includes('LaoWuMod.AGENT_SMOKE_BOMB.get(), net.minecraft.client.renderer.entity.NoopRenderer::new'),'Legacy saved projectiles no longer show a fire-charge sprite');
 const smoke=java('CatAgentSmoke'),targetMixin=java('mixin/LivingEntityAgentSmokeTargetMixin');
 check(smoke.includes('getAvailableGoals().stream().filter(WrappedGoal::isRunning).toList().forEach(WrappedGoal::stop)')&&smoke.includes('setLastHurtByMob(null)')&&smoke.includes('setPersistentAngerTarget(null)'),'Clear cached target goals, revenge and neutral anger');
 for(const memory of ['ATTACK_TARGET','ANGRY_AT','HURT_BY_ENTITY','HURT_BY','LOOK_TARGET','WALK_TARGET'])
  check(smoke.includes('MemoryModuleType.'+memory),'Target-scoped Brain cleanup: '+memory);
 check(targetMixin.includes('canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z')&&targetMixin.includes('instanceof Mob observer')&&targetMixin.includes('require = 1'),'Fail-fast exact-overload target guard, not player damage immunity');
 check(java('CommonEvents').includes('!CatAgentSmoke.hiddenFrom(event.getEntity(), candidate)'),'Attention Magnet cannot undo concealment');
 check(!smokeGate.includes('CatStat.INTELLIGENCE')&&!smokeGate.includes('effectiveValue('),'Smoke retreat no longer checks Intelligence');
 check(smokeGate.includes('NEXT_SMOKE')&&smokeGate.includes('hasNearbyAlly(cat)')&&agent.includes('SMOKE_COOLDOWN = 200'),'Smoke retains ally and ten-second cooldown gates');
 const smokeAllies=agent.slice(agent.indexOf('public static boolean hasNearbyAlly(Cat cat)'),agent.indexOf('public static boolean behind('));
 check(smokeAllies.includes('!CatClothesData.getOutfit(other).isSupport()'),'Support allies never enable smoke concealment');
 check(smokeAllies.includes('other != cat')&&smokeAllies.includes('other.isAlive()')&&smokeAllies.includes('other.isTame()')&&smokeAllies.includes('CatTeamRules.friendly(cat, other)')&&smokeAllies.includes('cat.distanceToSqr(other) <= 144'),'Existing smoke ally identity, team and range rules preserved');
 check(agent.includes('intelligence >= SMART && !behind(cat, target) ? flankPosition(target)')&&agent.includes('SMART = 60'),'Intelligence still controls smart flanking');
 const spray=java('CatDivingAttack');
 check(spray.includes('NozzleFluidPuffData')&&spray.includes('Fluids.WATER')&&spray.includes('getEyeY() + .08'),'Raised water spray reuses collector flowers');
 const tooltip=java('client/CareerSuitTooltip');
 check(tooltip.indexOf('"item.laowu.career_suit.role"')<tooltip.indexOf('List.of("combat", "work")'),'Career role before default descriptions');
 check(tooltip.includes('"item.laowu.career_suit.damage_detail"'),'Combat damage receives its own subtitle');
 for(const lang of ['zh_cn','en_us']){
  const strings=JSON.parse(read(port+'/src/main/resources/assets/laowu/lang/'+lang+'.json'));
  check(strings['item.laowu.agent_suit.tooltip.behaviour1'].includes(lang==='zh_cn'?'烟雾隐蔽不受智力影响':'Smoke concealment has no Intelligence requirement'),'Localized smoke rule is separate from the smart-flanking threshold');
  for(const key of ['summary','combat','behaviour1'])check(strings['item.laowu.agent_suit.tooltip.'+key].includes(lang==='zh_cn'?'非辅助友方猫':'non-support friendly cats'),'Localized smoke ally role requirement: '+key);
  for(const role of ['melee','ranged','support'])check(!!strings['item.laowu.career_suit.role.'+role],'Localized role '+role);
  check(!!strings['item.laowu.career_suit.damage_detail'],'Localized damage subtitle');
  for(const suit of ['flight','diving']){
   const detail=strings['item.laowu.'+suit+'_suit.tooltip.behaviour2'];
   check(!/旧背包|新背包|legacy backpack|new backpack|无.*100.*封顶|not capped at 100/i.test(detail),'Removed migration/cap prose from '+suit);
  }
 }
}
console.log('PASS: '+checks+' career feedback authority, leap, smoke, water particle, ring-state and tooltip guards (runtime tests separate)');
