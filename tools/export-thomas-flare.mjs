import { readFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import assert from 'node:assert/strict';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const source = 'docs/blockbench/cat_thomas_flare.bbmodel';
const bytes = readFileSync(path.join(root, source));
const model = JSON.parse(bytes);
const names = ['head', 'body', 'left_hind_leg', 'right_hind_leg',
  'left_front_leg', 'right_front_leg', 'tail1', 'tail2'];
const animation = model.animations.find(a => a.name === 'animation.cat.thomas_flare');
assert(animation && animation.loop === 'loop', 'Missing approved looping animation');
assert.equal(animation.length, 2.4);
assert.equal(model.groups.length, names.length, 'Only the approved single-leg rig is supported');
const times = [];
const tracks = names.map(name => {
  const group = model.groups.find(g => g.name === name);
  assert(group && group.rotation.every(v => v === 0), 'Unexpected rest pose: ' + name);
  const animator = animation.animators[group.uuid];
  assert.equal(animator.name, name);
  const channels = {};
  for (const channel of ['position', 'rotation', 'scale']) {
    const keys = animator.keyframes.filter(k => k.channel === channel).sort((a, b) => a.time - b.time);
    assert.equal(keys.length, 145, 'Unexpected key count: ' + name + '/' + channel);
    keys.forEach((key, i) => {
      assert.equal(key.interpolation, 'linear');
      assert.equal(key.data_points.length, 1);
      assert(Math.abs(key.time - i / 60) < 5e-6, 'Uneven key times');
      if (times[i] === undefined) times[i] = key.time;
      else assert.equal(times[i], key.time, 'Bone channels have different sample times');
    });
    channels[channel] = keys.map(k => ['x', 'y', 'z'].map(axis => {
      const value = Number(k.data_points[0][axis]);
      assert(Number.isFinite(value), 'Non-numeric animation channel');
      return value;
    }));
  }
  return { group, channels };
});
const round = value => Number(value.toFixed(7));
const frames = times.map((_, i) => tracks.flatMap(({ group, channels: c }) => {
  const p = c.position[i].map((v, axis) => v + group.origin[axis]);
  const r = c.rotation[i].map(v => v * Math.PI / 180);
  assert(c.scale[i].every(v => v > 0));
  // BB -> ModelPart is (-x, 24-y, z). Conjugating rotation by that basis
  // changes the signs of X/Y angles while preserving Z and local scales.
  return [-p[0], 24 - p[1], p[2], -r[0], -r[1], r[2], ...c.scale[i]].map(round);
}));
const result = {
  format_version: 1,
  source,
  source_sha256: createHash('sha256').update(bytes).digest('hex'),
  duration_seconds: animation.length,
  sample_rate: 60,
  bone_order: names,
  frame_times: times,
  frames
};
if (process.argv.includes('--check')) {
  for (const loader of ['forge-1.20.1', 'neoforge-1.21.1']) {
    const file = path.join(root, loader, 'src/main/resources/assets/laowu/cat_animation_clips/cat_thomas_flare.json');
    assert.deepEqual(JSON.parse(readFileSync(file, 'utf8')), result, loader + ' animation is stale');
  }
  process.stdout.write('PASS: both baked clips match the approved Blockbench project\n');
} else {
  // Output only; callers materialize the generated asset through apply_patch.
  process.stdout.write(JSON.stringify(result) + '\n');
}
