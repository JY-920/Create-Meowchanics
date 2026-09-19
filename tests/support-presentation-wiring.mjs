import assert from 'node:assert/strict';
import fs from 'node:fs';
const root=new URL('../',import.meta.url);
const read=p=>fs.readFileSync(new URL(p,root),'utf8').replaceAll('\r\n','\n');
let count=0,shared;
const check=(ok,message)=>{count++;assert.ok(ok,message);};
for(const port of ['forge-1.20.1','neoforge-1.21.1']){
 const java=p=>read(port+'/src/main/java/cn/laowu/mod/'+p+'.java');
 const appearance=java('client/CatAppearanceLayer'),medical=java('client/CatMedicalAnimation'),agent=java('client/CatAgentAttackAnimation');
 check(!appearance.slice(0,appearance.indexOf('model.isPlayingPipa()')).includes('traits.traits().isEmpty()'),'Career instrument cannot be gated on an appearance trait');
 check(appearance.includes('model.isPlayingPipa() && !cat.isInvisible()'),'Pipa still depends on live performance and visibility');
 const standing=medical.slice(0,medical.indexOf('public static void applyStationed'));
 check(standing.includes('Mth.HALF_PI + breath, 12, -10')&&standing.includes('pose(leftHind, .18F, 18, 5, w)'),'Combat recovery stands with open limbs on original anchors');
 check(medical.includes('pose(body, Mth.PI / 4, 8, -5, w)'),'Separate cushion pose retained');
 check(agent.includes('body.setPos(0,12,-10);body.xRot=Mth.HALF_PI')&&!agent.includes('lh.xRot=rh.xRot=-Mth.HALF_PI'),'Agent attacks use standing, not seated, bones');
 const smoke=java('client/CatHealingSmokeParticle');
 check(smoke.includes('extends TextureSheetParticle')&&smoke.includes('PARTICLE_SHEET_TRANSLUCENT')&&smoke.includes('setAlpha(1F)')&&smoke.includes('scale(4)'),'Dense full-opacity campfire puffs with soft sprite edges, no vanilla access widening');
 check(smoke.includes('random.nextFloat() / 5000')&&smoke.includes('(lifetime - age) / 10F'),'Campfire drift with short smooth end fade');
 check(java('CatAgentSmoke').includes('LaoWuMod.CAT_AGENT_SMOKE.get()'),'Normal retreat emits custom translucent puffs');
 check(java('client/ClientModEvents').includes('CatHealingSmokeParticle.SmokeProvider::new'),'Normal smoke provider registered');
 const expected=Array.from({length:12},(_,i)=>'minecraft:big_smoke_'+i);
 for(const kind of ['cat_agent_smoke','cat_healing_smoke']){
  const list=JSON.parse(read(port+'/src/main/resources/assets/laowu/particles/'+kind+'.json')).textures;
  assert.deepEqual(list,expected);count++;
 }
 check(java('CatMusicRules').includes('Math.min(24, 5 + .03 * finite(intelligence))')&&java('CatMusicSupport').includes('Math.min(24, radius)'),'Server radius and synchronized client radius have matching reduced bounds');
 const same=[medical,agent,smoke,java('CatMusicRules')].join('\n');
 if(shared)check(shared===same,'Presentation logic agrees across loaders');shared=same;
}
console.log('PASS: '+count+' pipa eligibility, standing/cushion pose, campfire transparency and reduced-range guards');
