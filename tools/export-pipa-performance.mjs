import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { createHash } from 'node:crypto';
import assert from 'node:assert/strict';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// Mechanical export from the model and animation actually saved by Blockbench MCP.
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const source = 'docs/blockbench/cat_pipa_performance.bbmodel';
const bytes = readFileSync(path.join(root, source));
const model = JSON.parse(bytes);
const bones = ['head','body','left_hind_leg','right_hind_leg',
  'left_front_leg','right_front_leg','tail1','tail2','pipa','plectrum'];
const animation = model.animations.find(a => a.name === 'animation.cat.pipa_performance');
assert(animation && animation.loop === 'loop' && animation.length === 3.2);
assert.equal(model.groups.length, 10);
const times = [];
const tracks = bones.map(name => {
  const group = model.groups.find(g => g.name === name);
  assert(group && group.rotation.every(v => v === 0), name + ' rest rotation');
  const channels = {};
  for (const channel of ['position','rotation','scale']) {
    const keys = animation.animators[group.uuid].keyframes.filter(k => k.channel === channel).sort((a,b) => a.time-b.time);
    assert.equal(keys.length, 193);
    channels[channel] = keys.map((key,i) => {
      assert.equal(key.interpolation, 'linear');
      assert(Math.abs(key.time-i/60) < 0.00001);
      if (times[i] === undefined) times[i] = key.time;
      else assert.equal(times[i], key.time);
      const vector = ['x','y','z'].map(axis => Number(key.data_points[0][axis]));
      assert(vector.every(Number.isFinite));
      if (channel === 'scale') assert(vector.every(v => v > 0));
      return vector;
    });
  }
  return { group, channels };
});
const frames = times.map((time,i) => tracks.flatMap(({group,channels:c}) => {
  const p = c.position[i].map((v,j) => v+group.origin[j]);
  const r = c.rotation[i].map(v => v*Math.PI/180);
  return [-p[0],24-p[1],p[2],-r[0],-r[1],r[2],...c.scale[i]].map(v => Number(v.toFixed(7)));
}));
frames[0].forEach((v,i) => assert(Math.abs(v-frames.at(-1)[i]) < 0.00001, 'Loop seam '+i));
const clip = { format_version:1,source,source_sha256:createHash('sha256').update(bytes).digest('hex'),
  duration_seconds:3.2,sample_rate:60,bone_order:bones,frame_times:times,frames };
const props = model.groups.filter(g => ['pipa','plectrum'].includes(g.name));
const propIds = new Set(props.map(g => g.uuid));
const outliner = model.outliner.filter(g => propIds.has(g.uuid));
const elementIds = new Set(outliner.flatMap(g => g.children));
const elements = model.elements.filter(e => elementIds.has(e.uuid));
assert.equal(elements.length, 41);
const textureIndex = model.textures.findIndex(t => t.name === 'cat_pipa.png');
assert(textureIndex >= 0);
for (const e of elements) for (const f of Object.values(e.faces)) {
  assert.equal(f.texture, textureIndex, e.name + ' has wrong material');
  f.texture = 0;
}
const propModel = { meta:{format_version:'5.0',model_format:'free',box_uv:false},name:'cat_pipa',
  resolution:{width:64,height:32},elements,groups:props,outliner,
  textures:[{name:'cat_pipa.png',width:64,height:32}] };
const texture = Buffer.from(model.textures[textureIndex].source.split(',')[1], 'base64');
const assets = [
  ['cat_animation_clips/cat_pipa_performance.json',JSON.stringify(clip)+'\n'],
  ['models/entity/cat_pipa.bbmodel',JSON.stringify(propModel)+'\n'],
  ['textures/entity/cat_pipa.png',texture]
];
for (const loader of ['forge-1.20.1','neoforge-1.21.1']) for (const [name,content] of assets) {
  const file = path.join(root,loader,'src/main/resources/assets/laowu',name);
  if (process.argv.includes('--write')) {
    mkdirSync(path.dirname(file),{recursive:true}); writeFileSync(file,content);
  } else assert(Buffer.from(content).equals(readFileSync(file)), loader+' stale export: '+name);
}
console.log('PASS: Blockbench source, 193 loop frames, 10 bones, 41 prop cubes, palette and both loader exports');
