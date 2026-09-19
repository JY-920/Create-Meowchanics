import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8').replaceAll('\r\n','\n');
let checks=0;
const check=(v,m)=>{checks++;assert(v,m)};
const clips=[];
for(const port of ['forge-1.20.1','neoforge-1.21.1']){
  const java=name=>read(port+'/src/main/java/cn/laowu/mod/'+name+'.java');
  const work=java('CatEngineeringBehavior');
  check(work.includes('instanceof SeatEntity'),'real seat passenger only');
  check(work.includes('seatPos.above()'),'only directly above the seat');
  check(work.includes('AllBlocks.HAND_CRANK.has(state)'),'not valve handles or other kinetic blocks');
  check(work.includes('getAxis().isHorizontal()'),'wall mount only');
  check(work.includes('hasChunkAt(crankPos)'),'no forced chunk loading');
  check(work.includes('crank.turn(crank.backwards)'),'Create owns direction/speed/stress');
  check(!work.includes('setSpeed(') && !work.includes('setBlock('),'do not manufacture cosmetic power');
  check(work.includes('getClockWise().toYRot()'),'side-on orientation');
  check(work.includes('record Work(long position, int identity, long started)'),'weak work cache does not retain worlds');
  const career=java('CareerCatBehavior');
  check(career.indexOf('CatEngineeringBehavior.tick(cat)') < career.indexOf('if (outfit.isPreviewOnly())'),'work allowed without combat');
  check(java('CommonEvents').includes('if (CatEngineeringBehavior.findCrank(cat) != null) return;'),'work takes priority over idle hissing');
  const render=java('client/HissingCatRenderer'), model=java('client/HissingCatModel'), anim=java('client/CatEngineeringAnimation');
  check(render.includes('CatEngineeringAnimation.prepare(cat, bodyYaw, partialTick)'),'same render yaw for pose and crank');
  check(render.includes('if (CatEngineeringAnimation.isPosing(cat)) return;'),'oiiai does not spin paws away');
  check(model.includes('CatEngineeringAnimation.apply(cat, head, body, leftHindLeg, rightHindLeg,'),'correct bone order');
  check(!model.includes('transforms.put("group3"'),'near-wall tool box is not suppressed');
  check(!model.includes('transforms.put("group2"'),'outer tool box is not suppressed');
  check(anim.includes('crank.getIndependentAngle(partialTick)'),'same smooth angle as both Create renderers');
  check(anim.includes('Math.atan2(canonical.y, canonical.z)'),'phase handles all directions and reversal');
  check(anim.includes('state.seatY = center.y - 16'),'live rider height; not a fixed Forge/Neo offset');
  check(anim.includes('state.baby = cat.isBaby()'),'kitten head/body size adaptation');
  check(anim.includes('ease(state.blend)') && anim.includes('q.slerp'),'smooth entry and exit');
  check(!anim.includes('getEntities') && !work.includes('getEntities'),'no whole-world or neighborhood scan');
  const clipPath=port+'/src/main/resources/assets/laowu/cat_animation_clips/cat_engineering_crank.json';
  const clip=JSON.parse(read(clipPath));clips.push(read(clipPath));
  check(clip.frames.length===181 && clip.bone_order.length===8,'8 single-bone limbs, 181 source poses');
  check(Math.abs(clip.frames[45][11]-13.3)<.00001 && Math.abs(clip.frames[135][11]-13.3)<.00001,
    'torso is 4 model pixels farther back at both middle phases');
  check(clip.frames.every(f=>f[15]===1 && f[16]===.72 && f[17]===1),'torso never stretches during the cycle');
  check(Math.abs(clip.frames[45][10]-23.15)<.00001,'raise torso 4px at high crank position');
  check(clip.frames[45][2]<8 && clip.frames[45][11]>13,'head does not follow the torso rearward offset');
  check(clip.frames[45][43]<1.12 && clip.frames[45][52]<1.12,'high crank pose uses shorter arms');
  check(Math.abs(clip.frames[0][10]-20.3)<.00001 && Math.abs(clip.frames[90][10]-20.3)<.00001,
    'standing blends across both half-cycles, not only the upper quarter-turn');
  let bodyStep=0,legStep=0,legAcceleration=0;
  for(let i=0;i<180;i++){
    const current=clip.frames[i], previous=clip.frames[(i+179)%180], before=clip.frames[(i+178)%180];
    bodyStep=Math.max(bodyStep,Math.abs(current[10]-previous[10]));
    for(const k of [25,34]){
      legStep=Math.max(legStep,6*Math.abs(current[k]-previous[k]));
      legAcceleration=Math.max(legAcceleration,6*Math.abs(current[k]-2*previous[k]+before[k]));
    }
  }
  check(bodyStep<.12 && legStep<.15 && legAcceleration<.014,'smooth rise/retraction including the loop seam');
  check(model.includes('transforms.put("group", bodyAttachedAccessoryDelta(pivotX, pivotY, pivotZ))'),
    'both tool boxes use the live torso attachment in game');
  for(const lang of ['zh_cn','en_us']){
    const text=JSON.parse(read(port+'/src/main/resources/assets/laowu/lang/'+lang+'.json'))['item.laowu.engineering_suit.tooltip.summary'];
    check(lang==='zh_cn'?text.includes('曲柄'):text.includes('Hand Crank'),'work tooltip');
  }
}
check(clips[0]===clips[1],'identical client animation resources');
const project=JSON.parse(read('docs/blockbench/cat_engineering_crank.bbmodel'));
check(project.groups.some(g=>g.name==='reference_crank_turn'),'real reference pivot');
const pack=project.groups.find(g=>g.name==='group');
const animation=project.animations.find(a=>a.name==='animation.cat.engineering_crank');
const packScales=animation.animators[pack.uuid].keyframes.filter(k=>k.channel==='scale');
check(packScales.length===181 && packScales.every(k=>{
  const p=k.data_points[0];return Number(p.x)===1 && Number(p.y)===1 && Number(p.z)===.72;
}),'tool-box dimensions stay fixed during stand-up');
for(const name of ['group2','group3']){
  const box=project.groups.find(g=>g.name===name);
  const node=project.outliner.find(g=>g.uuid===project.groups.find(g=>g.name==='group').uuid)
    .children.find(g=>g.uuid===box.uuid);
  check(box.visibility!==false && node.visibility!==false,'MCP preview keeps '+name+' group visible');
  check(node.children.every(id=>project.elements.find(e=>e.uuid===id).visibility!==false),
    'MCP preview keeps all '+name+' meshes visible');
}
console.log('PASS: '+checks+' engineering work, rendering, translation and integration checks');
