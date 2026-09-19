import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8').replaceAll('\r\n','\n');
let count=0;
const check=(value,why)=>{ count++; assert.ok(value,why); };
let previous;
for(const port of ['forge-1.20.1','neoforge-1.21.1']) {
  const java=name=>read(port+'/src/main/java/cn/laowu/mod/'+name+'.java');
  const goal=java('CatEngineeringCombat'), cannon=java('entity/EngineeringCannon');
  const gear=java('entity/EngineeringCogwheelProjectile'), render=java('client/EngineeringCannonRenderer');
  check(java('CatArtilleryTactics').includes('RANGE = 32') && goal.includes('DEPLOY_TICKS = 16'),'32-block range and deployed windup');
  check(goal.includes('CatArtilleryTactics.relocate') && goal.includes('repositionTicks >= 48') && !goal.includes('strafe'),'bounded intelligence-based redeployment with firing fallback');
  check(goal.includes('blockedTicks >= 20') && goal.includes('pathDelay = 8'),'bounded line-of-fire recovery / path updates');
  check(goal.includes('CatSuitSettings.current(CatOutfitType.ENGINEERING)'),'live configurable reload interval');
  check(!goal.slice(goal.indexOf('void stop()'),goal.indexOf('void tick()')).includes('cooldown ='),'target switches do not reset reload');
  check(cannon.includes('passenger.stopRiding()') && !cannon.includes('passenger.discard()'),'only the temporary cannon is discarded');
  check(!java('LaoWuMod').includes('.noSave()'),'passenger vehicle is persistable');
  check(cannon.includes('getBlockState')===false && !goal.includes('setBlock('),'deployment does not mutate terrain');
  check(cannon.includes('hasClearShot(target)') && cannon.includes('ClipContext.Block.COLLIDER'),'muzzle and sight use real block collision');
  check(cannon.includes('CatAccessoryHooks.projectile(cat, target, projectile)'),'KubeJS projectile callback participates');
  check(/CatAccessoryHooks\.projectile\(cat, target, projectile\)\) \{[\s\S]*?projectile\.discard\(\);[\s\S]*?return true;/.test(cannon),'script cancellation consumes reload instead of dispatching every tick');
  check(java('genetics/CatAttributeEffects').includes('activeBody ? outfit : CatOutfitType.NONE'),'live effective attributes include engineer suit bonuses');
  check(gear.includes('CatProjectileDamage.hurt') && gear.includes('CatTeamRules.canHarm'),'normal crit/hurt hooks and accessory knockback scope');
  check(gear.includes('bounds.contains(start)') && gear.includes('bounds.clip(start, end)'),'spawn-inside contacts and whole-segment multi-victim collision');
  check(gear.includes('travelled >= 34') && gear.includes('tickCount > 80'),'bounded 32-block projectile lifetime');
  check(cannon.includes('Vec3 start = pivot();'),'every shot originates from cannon centre');
  check(cannon.includes('SEAT_BACK = 1.05') && cannon.includes('SEAT_TOP = 0.25')
      && render.includes('INDUSTRIAL_IRON_BLOCK'),'rear seat with extended base');
  check(render.includes('SCHEMATICANNON_CONNECTOR') && render.includes('SCHEMATICANNON_PIPE'),'reuse actual Create cannon partials');
  check(render.includes('AllBlocks.SEATS.get(color)') && render.includes('getCollarColor()'),'team-colored cannon cushion');
  check(java('client/EngineeringCogwheelRenderer').includes('renderStatic(gear.getItem()'),'synchronized Create cog / large cog / shaft model');
  check(java('CatLaserCommands').includes('if (aim.target() == null) CatEngineeringCombat.release(cat)'),'move orders pack the cannon');
  check(java('CommonEvents').includes('if (CatEngineeringCombat.deployed(cat)) return;'),'autonomous movement / hissing cannot disturb mounted artillery');
  check(/isPreviewOnly\(\)\s*\{\s*return false;/.test(java('CatOutfitType')),'all implemented careers are active');
  check(java('client/HissingCatModel').includes('outfit.hasImportedModel()'),'engineer toolkit pivot survives combat activation');
  for(const lang of ['zh_cn','en_us']) {
    const text=JSON.parse(read(port+'/src/main/resources/assets/laowu/lang/'+lang+'.json'));
    for(const key of ['summary','condition1','behaviour1','condition2','behaviour2','condition3','behaviour3'])
      check(!!text['item.laowu.engineering_suit.tooltip.'+key],'complete suit formula/summary '+lang);
    check(text['item.laowu.engineering_suit.tooltip.behaviour1'].includes('_K'),'live K placeholder');
    check(!!text['entity.laowu.engineering_cannon'] && !!text['entity.laowu.engineering_cogwheel_projectile'],'entity names');
  }
  const shared=[goal,render,java('client/EngineeringCogwheelRenderer')].join('\n');
  if(previous)check(shared===previous,'identical shared artillery behavior and model math across loaders');
  previous=shared;
}
// Create pipe is +Y, pivot (8,15,8), mouth (8,32,8). Verify rendered mouth equals server muzzle.
const radians=deg=>deg*Math.PI/180;
const ry=(p,a)=>[Math.cos(a)*p[0]+Math.sin(a)*p[2],p[1],-Math.sin(a)*p[0]+Math.cos(a)*p[2]];
const rz=(p,a)=>[Math.cos(a)*p[0]-Math.sin(a)*p[1],Math.sin(a)*p[0]+Math.cos(a)*p[1],p[2]];
for(let yaw=-180;yaw<=180;yaw+=15) for(let pitch=-90;pitch<=90;pitch+=15) {
  const mouth=ry(rz([0,17/16*0.85,0],radians(-90-pitch)),radians(-yaw-90));
  const d=[-Math.sin(radians(yaw))*Math.cos(radians(pitch)),-Math.sin(radians(pitch)),Math.cos(radians(yaw))*Math.cos(radians(pitch))];
  check(mouth.every((v,i)=>Math.abs(v-d[i]*17/16*0.85)<1e-10),'barrel/muzzle alignment '+yaw+'/'+pitch);
  const seat=ry([0,0,-1.05],radians(-yaw));
  check(Math.abs(seat[0]-Math.sin(radians(yaw))*1.05)<1e-10 && Math.abs(seat[2]+Math.cos(radians(yaw))*1.05)<1e-10,'rear seat matches passenger anchor');
}
console.log('PASS: '+count+' artillery wiring, translations and barrel/seat alignment checks');
