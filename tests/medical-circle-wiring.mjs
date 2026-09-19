import assert from 'node:assert/strict';
import fs from 'node:fs';
const root=new URL('../',import.meta.url);
const read=p=>fs.readFileSync(new URL(p,root),'utf8').replaceAll('\r\n','\n').trim();
let count=0,shared;
const check=(value,message)=>{count++;assert.ok(value,message);};
for(const port of ['forge-1.20.1','neoforge-1.21.1']){
 const java=name=>read(port+'/src/main/java/cn/laowu/mod/'+name+'.java');
 const goal=java('CatMedicalSupportGoal'),state=java('CatMedicalHealing'),rules=java('CatSupportRules'),work=java('CatMedicalWork');
 check(rules.includes('1 + .03 * finiteIntelligence')&&rules.includes('3 + .03 * finiteIntelligence'),'intelligence scaling for rate and radius');
 check(rules.includes('HEAL_TICKS = 5')&&rules.includes('MEDICAL_WINDUP_TICKS = 20'),'quarter-second pulses after one-second preparation');
 check(goal.includes('getEntitiesOfClass')&&goal.includes('CatMedicalHealing.treat(cat, ally'),'all circle recipients, including caster');
 check(goal.includes('ally == healer')&&goal.includes('recipient != cat'),'self treatment is eligible without looking or navigating to self');
 check(goal.includes('boolean aerial')&&goal.includes('recipient.isPassenger()')&&goal.includes('if (aerial) cat.getNavigation().moveTo'),'airborne target uses actual sphere, ground-projected navigation');
 check(goal.includes('cat.hasLineOfSight(candidate)')&&goal.includes('radius * radius'),'line of sight and sphere, not entire search box');
 check(state.includes('now(healer) - channelStart >= CatSupportRules.MEDICAL_WINDUP_TICKS'),'channel warmup prevents redeployment burst');
 check(state.includes('setWantedPosition(cat.getX(), cat.getY(), cat.getZ(), 0)')&&state.includes('cat.setZza(0)'),'root only caster and clear residual locomotion');
 check(!goal.includes('ally.set')&&!state.includes('patient.setNoAi')&&!state.includes('patient.setDeltaMovement'),'patients retain AI and movement');
 check(java('CommonEvents').includes('CatMedicalHealing.holdStill(cat)')&&java('CommonEvents').includes('CatMedicalHealing.flush(level)'),'caster pose isolation and end-of-tick aggregation are wired');
 check(state.includes('Math.max(previous.rate, rate)')&&state.includes('data.getLong(NEXT_HEAL) > tick'),'strongest source wins with one shared cooldown');
 check(state.includes('VISUAL_TTL = 30')&&state.includes('v.nextSync = time + 10'),'heartbeats tolerate network delay but expire');
 check(java('client/ClientPacketHandler').includes('patient.getUUID().equals(p.uuid())'),'UUID rejects stale entity reuse');
 check(java('client/ClientPacketHandler').includes('LivingEntity patient'),'players and other creatures receive the effect');
 check(java('network/ModNetwork').includes('MedicalHealingPacket::handle'),'effect packet is registered');
 check(work.includes('AABB.ofSize')&&work.includes('box.contains(patient.getBoundingBox().getCenter())'),'fixed cube, including corners and bounded height');
 check(work.includes('LivingEntity.class')&&work.includes('CareerCatBehavior.findSeat(cat) != null'),'Create Seat clinic treats living entities');
 check(work.includes('CatMedicalHealing.cast(cat, CatSupportRules.MEDICAL_WORK_SIZE / 2, true)'),'clinic channel preserves seated passenger mode');
 const model=java('client/HissingCatModel');
 check(model.includes('boolean healing = cn.laowu.mod.CatMedicalHealing.casting(cat)'),'only caster changes pose');
 check(model.includes('!healing && !music && traits')&&model.includes('part.resetPose()'),'no dance/pose leakage to next cat');
 const outline=java('client/CatPerformanceOutline'),effect=java('client/CatMedicalEffects');
 check(outline.includes('capture.healing!=null?.55F:0')&&outline.includes('new State()'),'existing state-guarded outline palette');
 check(java('client/ClientModEvents').includes('MedicalPatientLayer')&&java('client/ClientModEvents').includes('getSkins()'),'both player skins and creature renderers receive a layer');
 check(effect.includes('GL11.GL_LEQUAL')&&effect.includes('depthMask(false)'),'terrain depth tested, never overwrite scene depth');
 check(effect.includes('i < 5')&&effect.includes('target.height + .28'),'five rising crosses plus persistent overhead cross');
 check(effect.includes('drawGlyphs')&&effect.includes('target.radius')&&effect.includes('target.stationed'),'testable actual projection and synchronized range/work footprint');
 const fragment=read(port+'/src/main/resources/assets/laowu/shaders/core/performance_boundary.fsh');
 check(fragment.includes('healing ? green : purple')&&fragment.includes('SceneDepth'),'green contour preserves purple performance and occlusion');
 for(const lang of ['zh_cn','en_us']){
  const t=JSON.parse(read(port+'/src/main/resources/assets/laowu/lang/'+lang+'.json'));
  check(!!t['item.laowu.career_suit.snapshot.healing_radius'],'live healing-radius snapshot');
  check(t['item.laowu.medical_suit.tooltip.behaviour2'].includes('3×3×3'),'localized clinic volume');
  check(!!t['key.laowu.pilot_descend'],'pilot binding preserved');
 }
 const sources=[goal,state,rules,work,java('client/CatMedicalAnimation'),fragment].join('\n');
 if(shared)check(sources===shared,'medical rules, work, animation and shader remain identical across loaders');
 shared=sources;
}
console.log('PASS: '+count+' medical intelligence, clinic, networking, pose and visual wiring checks');
