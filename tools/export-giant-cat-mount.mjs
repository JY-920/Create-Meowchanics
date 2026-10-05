import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const args = process.argv.slice(2);
if (args.length !== 0 && (args.length !== 2 || args[0] !== '--source')) {
  throw new Error('Usage: node tools/export-giant-cat-mount.mjs [--source path/to/model.bbmodel]');
}
const sourcePath = args.length === 2 ? path.resolve(args[1])
  : path.join(root, 'art/giant-cat-mount/source/vanilla_yellow_cat.bbmodel');
const source = JSON.parse(fs.readFileSync(sourcePath, 'utf8'));

// Frames are [seconds, x, y, z]. Positions use model pixels, rotations degrees.
// The exact same table is exported into the editable Blockbench project and runtime JSON.
const P = (length, ...frames) => [...frames, [length, ...frames[0].slice(1)]];
const R = (length, ...frames) => [...frames, [length, ...frames[0].slice(1)]];
const S = (length, ...frames) => [...frames, [length, ...frames[0].slice(1)]];
const clips = {
  idle: {
    length: 2, loop: true, bones: {
      // Shared ancestors include the legs: sway only the torso child, not the paws.
      body: {
        position: P(2, [0, 0, 0, 0], [.5, 0, .08, 0], [1, 0, .18, 0], [1.5, 0, .07, 0]),
        rotation: R(2, [0, 0, 0, 0], [1, 1.0, 0, .8]),
      },
      group3: { rotation: R(2, [0, 0, 0, 0], [1, 0, 0, .35]) },
      head: { rotation: R(2, [0, 0, 0, 0], [1, -1.2, 0, -.7]) },
      tail1: { rotation: R(2, [0, 0, 0, 0], [.5, 0, 8, 0], [1, 0, 0, 0], [1.5, 0, -8, 0]) },
      tail2: { rotation: R(2, [0, 0, 0, 0], [.5, 0, -5, 0], [1, 0, 0, 0], [1.5, 0, 5, 0]) },
    },
  },
  walk: {
    length: .8, loop: true, bones: {
      group2: {
        position: P(.8, [0, 0, 0, 0], [.2, 0, .48, 0], [.4, 0, 0, 0], [.6, 0, .48, 0]),
        scale: S(.8, [0, 1.02, .98, 1.01], [.2, .99, 1.02, .99], [.4, 1.02, .98, 1.01], [.6, .99, 1.02, .99]),
      },
      group5: { rotation: R(.8, [0, 1, 0, 1.6], [.2, -2, 0, -1.6], [.4, 1, 0, 1.6], [.6, -2, 0, -1.6]) },
      head: { rotation: R(.8, [0, -2, 0, -1], [.2, 2, 0, 1], [.4, -2, 0, -1], [.6, 2, 0, 1]) },
      left_front_leg: { rotation: R(.8, [0, 26, 0, 0], [.2, 0, 0, 0], [.4, -26, 0, 0], [.6, 0, 0, 0]) },
      right_front_leg: { rotation: R(.8, [0, -26, 0, 0], [.2, 0, 0, 0], [.4, 26, 0, 0], [.6, 0, 0, 0]) },
      left_hind_leg: { rotation: R(.8, [0, -21, 0, 0], [.2, 0, 0, 0], [.4, 21, 0, 0], [.6, 0, 0, 0]) },
      right_hind_leg: { rotation: R(.8, [0, 21, 0, 0], [.2, 0, 0, 0], [.4, -21, 0, 0], [.6, 0, 0, 0]) },
      tail1: { rotation: R(.8, [0, 0, 7, 0], [.2, 0, 0, 0], [.4, 0, -7, 0], [.6, 0, 0, 0]) },
      tail2: { rotation: R(.8, [0, 0, -9, 0], [.2, 0, 0, 0], [.4, 0, 9, 0], [.6, 0, 0, 0]) },
    },
  },
  run: {
    length: .52, loop: true, bones: {
      group2: {
        position: P(.52, [0, 0, 0, 0], [.13, 0, .18, -.08], [.26, 0, .03, 0], [.39, 0, .15, .05]),
        scale: S(.52, [0, 1.08, .90, .96], [.13, .96, 1.07, 1.09], [.26, 1.09, .89, .95], [.39, .97, 1.06, 1.08]),
      },
      group5: { rotation: R(.52, [0, -.8, 0, .35], [.13, 1, 0, -.35], [.26, -.8, 0, .35], [.39, .8, 0, -.35]) },
      head: { rotation: R(.52, [0, .6, 0, -.2], [.13, -.4, 0, .2], [.26, .6, 0, -.2], [.39, -.4, 0, .2]) },
      left_front_leg: { rotation: R(.52, [0, 43, 0, 0], [.13, 0, 0, 0], [.26, -43, 0, 0], [.39, 0, 0, 0]) },
      right_front_leg: { rotation: R(.52, [0, 37, 0, 0], [.13, 7, 0, 0], [.26, -40, 0, 0], [.39, -6, 0, 0]) },
      left_hind_leg: { rotation: R(.52, [0, -36, 0, 0], [.13, 0, 0, 0], [.26, 36, 0, 0], [.39, 0, 0, 0]) },
      right_hind_leg: { rotation: R(.52, [0, -31, 0, 0], [.13, -5, 0, 0], [.26, 33, 0, 0], [.39, 4, 0, 0]) },
      tail1: { rotation: R(.52, [0, 5, 12, 0], [.13, -5, 0, 0], [.26, 5, -12, 0], [.39, -5, 0, 0]) },
      tail2: { rotation: R(.52, [0, -6, -14, 0], [.13, 4, 0, 0], [.26, -6, 14, 0], [.39, 4, 0, 0]) },
    },
  },
  jump: {
    length: .9, loop: false, bones: {
      group2: {
        position: [[0, 0, 0, 0], [.12, 0, 0, 0], [.32, 0, .55, -.12], [.6, 0, .65, -.12], [.76, 0, 0, 0], [.9, 0, 0, 0]],
        scale: [[0, 1, 1, 1], [.12, 1.08, .87, 1.05], [.32, .96, 1.09, .96], [.6, .98, 1.04, .98], [.76, 1.1, .82, 1.08], [.9, 1, 1, 1]],
      },
      group5: { rotation: [[0, 0, 0, 0], [.12, -6, 0, 0], [.32, 9, 0, 0], [.6, 5, 0, 0], [.76, -7, 0, 0], [.9, 0, 0, 0]] },
      head: { rotation: [[0, 0, 0, 0], [.12, 5, 0, 0], [.32, -7, 0, 0], [.6, -3, 0, 0], [.76, 5, 0, 0], [.9, 0, 0, 0]] },
      left_front_leg: { rotation: [[0, 0, 0, 0], [.12, -18, 0, 0], [.32, 29, 0, 0], [.6, 21, 0, 0], [.76, -13, 0, 0], [.9, 0, 0, 0]] },
      right_front_leg: { rotation: [[0, 0, 0, 0], [.12, -18, 0, 0], [.32, 29, 0, 0], [.6, 21, 0, 0], [.76, -13, 0, 0], [.9, 0, 0, 0]] },
      left_hind_leg: { rotation: [[0, 0, 0, 0], [.12, 18, 0, 0], [.32, -29, 0, 0], [.6, -20, 0, 0], [.76, 17, 0, 0], [.9, 0, 0, 0]] },
      right_hind_leg: { rotation: [[0, 0, 0, 0], [.12, 18, 0, 0], [.32, -29, 0, 0], [.6, -20, 0, 0], [.76, 17, 0, 0], [.9, 0, 0, 0]] },
      tail1: { rotation: [[0, 0, 0, 0], [.12, -8, 0, 0], [.32, 15, 0, 0], [.6, 20, 0, 0], [.76, -14, 0, 0], [.9, 0, 0, 0]] },
      tail2: { rotation: [[0, 0, 0, 0], [.12, 9, 0, 0], [.32, -20, 0, 0], [.6, -24, 0, 0], [.76, 16, 0, 0], [.9, 0, 0, 0]] },
    },
  },
};

// Leg-local deformation adds soft compression without throwing the whole mount up/down.
for (const [bone, reverse] of [['left_front_leg', false], ['right_front_leg', false],
  ['left_hind_leg', true], ['right_hind_leg', true]]) {
  const compressed = [1.12, .82, 1.1], stretched = [.95, 1.14, .94];
  const a = reverse ? stretched : compressed, b = reverse ? compressed : stretched;
  clips.run.bones[bone].scale = S(.52, [0, ...a], [.13, 1, 1, 1], [.26, ...b], [.39, 1, 1, 1]);
  clips.walk.bones[bone].scale = S(.8, [0, 1.04, .94, 1.03], [.2, .98, 1.04, .98],
    [.4, 1.04, .94, 1.03], [.6, .98, 1.04, .98]);
}
// Rest endpoints. Expand these into a reversible lie-down curve below.
const hold = (...value) => [[0, ...value], [1, ...value]];
// Side-roll puts the tail attachment four pixels above the grounded torso side.
// A six-pixel proximal segment slopes to the thickened tip's .78-pixel centre;
// the child's local Z counter-bend then keeps the distal segment horizontal.
const restTailSlope = Math.asin((4 - .65 * 1.2) / 6) * 180 / Math.PI;
clips.rest = { length: 1, loop: false, bones: {
  group2: { position: hold(0, 0, 0), rotation: hold(0, 0, 90) },
  body: { scale: hold(1, 1.02, .98) },
  // A small neck lift leaves room for bounded world-facing head observation.
  head: { position: hold(.55, -.15, 0), rotation: hold(-3, 0, -18) },
  // Stretch the forelegs beside and slightly toward the muzzle, not under the
  // belly. Different reaches/rolls keep the upper and lower paws readable.
  left_front_leg: { position: hold(.2, -2, -.3), rotation: hold(38, 0, -15), scale: hold(1.02, .98, 1) },
  right_front_leg: { position: hold(.08, -1.8, .4), rotation: hold(25, 0, -8), scale: hold(1.02, .96, 1) },
  // Hind legs remain bent, with the lower paw reaching farther behind.
  left_hind_leg: { position: hold(.1, -1.5, .3), rotation: hold(-28, 0, -10), scale: hold(1.02, .95, 1) },
  right_hind_leg: { position: hold(-.1, -2, 1), rotation: hold(-42, 0, -10), scale: hold(1.02, 1, 1) },
  tail1: { rotation: hold(-38.4338, -restTailSlope, 0) },
  // Scale each segment's cross-section once, never the joint/child ancestry.
  tail1_volume: { scale: hold(1.2, 1, 1.2) },
  tail2: { rotation: hold(47.4338, 0, restTailSlope), scale: hold(1.2, 1, 1.2) },
} };
for (const [bone, channels] of Object.entries(clips.rest.bones)) {
  for (const [channel, frames] of Object.entries(channels)) {
    const neutral = channel === 'scale' ? 1 : 0;
    const end = frames.at(-1).slice(1);
    channels[channel] = Array.from({ length: 121 }, (_, index) => {
      const time = index / 120;
      // Roll onto the shoulder before extending the paws sideways. Moving the
      // leg pivots later avoids an abrupt change of support during the roll.
      const progress = channel === 'position' && bone.endsWith('_leg')
        ? time * time * (3 - 2 * time) : 1 - Math.pow(1 - time, 1.7);
      return [time, ...end.map(value => neutral + (value - neutral) * progress)];
    });
  }
}

const model = structuredClone(source);
model.name = 'giant_cat_mount';
model.model_identifier = 'giant_cat_mount';
// RuntimeBlockbenchModel uses resolution as a fallback; source atlas is 64x32.
// Correcting this metadata does not alter the original texture, pixels, UVs, or geometry.
model.resolution = { width: 64, height: 32 };
// The archive's tail2 is a sibling with a rounded world-space pivot, leaving a
// gap even at rest and separating during every independent tail1 rotation.
// Re-root ONLY the exported tip at the exact center of tail1's distal cap.
// The generated tail is deliberately 130% thick and 75% long; face UVs and
// the archived project stay unchanged. Shorten from each segment's top cap.
const tailBase = model.elements.find(element => element.name === 'tail_base_cube');
const tailTip = model.elements.find(element => element.name === 'tail_tip_cube');
for (const segment of [tailBase, tailTip]) {
  for (const axis of [0, 2]) {
    const center = (segment.from[axis] + segment.to[axis]) / 2;
    segment.from[axis] = center + (segment.from[axis] - center) * 1.3;
    segment.to[axis] = center + (segment.to[axis] - center) * 1.3;
  }
  segment.from[1] = segment.to[1] + (segment.from[1] - segment.to[1]) * .75;
}
const tail1 = model.groups.find(group => group.name === 'tail1');
const tail2 = model.groups.find(group => group.name === 'tail2');
const wrapper = model.groups.find(group => group.name === 'group4');
const joint = [(tailBase.from[0] + tailBase.to[0]) / 2, tailBase.from[1],
  (tailBase.from[2] + tailBase.to[2]) / 2];
const oldCap = [(tailTip.from[0] + tailTip.to[0]) / 2, tailTip.to[1],
  (tailTip.from[2] + tailTip.to[2]) / 2];
for (const key of ['from', 'to']) tailTip[key] = tailTip[key].map((v, i) => v + joint[i] - oldCap[i]);
tailTip.origin = [...joint];
tail2.origin = [...joint];
wrapper.origin = [...joint];
tail2.rotation[0] -= tail1.rotation[0];
function findNode(nodes, id) {
  for (const node of nodes) {
    if (typeof node !== 'object') continue;
    if (node.uuid === id) return node;
    const found = findNode(node.children, id);
    if (found) return found;
  }
}
const tailParent = findNode(model.outliner, model.groups.find(group => group.name === 'group3').uuid);
const tipNode = findNode(tailParent.children, wrapper.uuid);
tailParent.children = tailParent.children.filter(node => node !== tipNode);
findNode(tailParent.children, tail1.uuid).children.push(tipNode);
// A volume-only child keeps base thickening centered on its cap without
// moving the joint or compounding the tip's own 20% rest thickening.
const tailVolume = { ...structuredClone(tail1), name: 'tail1_volume',
  uuid: '5a000000-0000-4000-8000-000000000009',
  origin: [joint[0], tailBase.to[1], joint[2]], rotation: [0, 0, 0] };
model.groups.push(tailVolume);
const baseNode = findNode(model.outliner, tail1.uuid);
baseNode.children = baseNode.children.map(child => child === tailBase.uuid
  ? { uuid: tailVolume.uuid, isOpen: false, children: [tailBase.uuid] } : child);
// Ground the reversible side-roll against actual cube corners, including the
// corrected tail chain. Parent roll changes which corner supports the cat;
// a fixed root drop would bury the legs halfway through the transition.
const groupDefinitions = new Map(model.groups.map(group => [group.uuid, group]));
function floorHeight(index) {
  let minimum = Infinity;
  function visit(nodes, transforms) {
    for (const node of nodes) {
      if (typeof node === 'object') {
        visit(node.children, [...transforms, groupDefinitions.get(node.uuid)]);
        continue;
      }
      const element = model.elements.find(cube => cube.uuid === node);
      if (!element) continue;
      for (const x of [element.from[0], element.to[0]])
        for (const y of [element.from[1], element.to[1]])
          for (const z of [element.from[2], element.to[2]]) {
            let point = [x, y, z].map((value, axis) => value - transforms.at(-1).origin[axis]);
            for (let i = transforms.length - 1; i >= 0; i--) {
              const group = transforms[i], channels = clips.rest.bones[group.name] ?? {};
              const scale = channels.scale?.[index].slice(1) ?? [1, 1, 1];
              const rotation = channels.rotation?.[index].slice(1) ?? [0, 0, 0];
              const position = group.name === 'group2' ? [0, 0, 0]
                : channels.position?.[index].slice(1) ?? [0, 0, 0];
              let [px, py, pz] = point.map((value, axis) => value * scale[axis]);
              const [rx, ry, rz] = rotation.map((value, axis) => (value + group.rotation[axis]) * Math.PI / 180);
              [py, pz] = [py * Math.cos(rx) - pz * Math.sin(rx), py * Math.sin(rx) + pz * Math.cos(rx)];
              [px, pz] = [px * Math.cos(ry) + pz * Math.sin(ry), -px * Math.sin(ry) + pz * Math.cos(ry)];
              [px, py] = [px * Math.cos(rz) - py * Math.sin(rz), px * Math.sin(rz) + py * Math.cos(rz)];
              point = [px, py, pz].map((value, axis) => value + group.origin[axis]
                - (transforms[i - 1]?.origin[axis] ?? 0) + position[axis]);
            }
            minimum = Math.min(minimum, point[1]);
          }
    }
  }
  visit(model.outliner, []);
  return minimum;
}
const standingFloor = floorHeight(0);
clips.rest.bones.group2.position.forEach((frame, index) => {
  frame[2] = standingFloor * (1 - frame[0]) - floorHeight(index);
});
const uuids = new Map(model.groups.map(group => [group.name, group.uuid]));
function previewFrames(frames) {
  const dense = [];
  for (let index = 1; index < frames.length; index++) {
    const first = frames[index - 1];
    const last = frames[index];
    const steps = Math.ceil((last[0] - first[0]) * 60);
    for (let step = 0; step < steps; step++) {
      const u = step / steps;
      const smooth = u * u * (3 - 2 * u);
      dense.push([first[0] + (last[0] - first[0]) * u,
        ...[1, 2, 3].map(axis => first[axis] + (last[axis] - first[axis]) * smooth)]);
    }
  }
  dense.push(frames.at(-1));
  return dense;
}
model.animations = Object.entries(clips).map(([name, clip], clipIndex) => {
  const animators = {};
  let keyIndex = 1;
  for (const [bone, channels] of Object.entries(clip.bones)) {
    animators[uuids.get(bone)] = {
      name: bone, type: 'bone', rotation_global: false, quaternion_interpolation: false,
      keyframes: Object.entries(channels).flatMap(([channel, frames]) => previewFrames(frames).map(([time, x, y, z]) => ({
        channel, data_points: [{ x: String(x), y: String(y), z: String(z) }],
        uuid: `6ca70000-0000-4000-8000-${String(clipIndex * 1000 + keyIndex++).padStart(12, '0')}`,
        time, color: -1, interpolation: 'linear',
      }))),
    };
  }
  return {
    uuid: `6ca70000-0000-4000-8000-${String(clipIndex + 1).padStart(12, '0')}`,
    name, loop: clip.loop ? 'loop' : 'once', override: false, length: clip.length,
    snapping: 24, selected: false, group_name: '', scope: 0,
    anim_time_update: '', blend_weight: '', start_delay: '', loop_delay: '', animators,
  };
});

function write(relative, value) {
  const output = path.join(root, relative);
  fs.mkdirSync(path.dirname(output), { recursive: true });
  fs.writeFileSync(output, JSON.stringify(value, null, 2) + '\n');
}
write('art/giant-cat-mount/giant_cat_mount.bbmodel', model);
const runtime = { version: 1, units: { position: 'pixels', rotation: 'degrees', time: 'seconds' }, clips };
for (const loader of ['forge-1.20.1', 'neoforge-1.21.1']) {
  write(`${loader}/src/main/resources/assets/laowu/models/entity/giant_cat_mount.bbmodel`, model);
  write(`${loader}/src/main/resources/assets/laowu/cat_animation_clips/giant_cat_mount.json`, runtime);
}
console.log('Exported giant cat mount model and animation curves for Forge and NeoForge.');
