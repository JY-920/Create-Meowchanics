import { createRequire } from 'node:module';
import { createHash } from 'node:crypto';
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';

// Standalone art source. This tool does not write to either mod's resources,
// register an entity, or alter the existing Butter Cat boss.
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const require = createRequire(import.meta.url);
const dependencyRoot = process.env.MEOW_ART_NODE_MODULES ||
  'C:/Users/16611/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules';
const { PNG } = require(path.join(dependencyRoot, 'pngjs'));
const outDir = path.join(root, 'art/giant-tabby-boss');
const writing = process.argv.includes('--write');
const uuid = name => {
  const s = createHash('sha256').update('laowu.giant_tabby.art.v1/' + name).digest('hex');
  return `${s.slice(0, 8)}-${s.slice(8, 12)}-4${s.slice(13, 16)}-a${s.slice(17, 20)}-${s.slice(20, 32)}`;
};
const round = n => Number(n.toFixed(6));
const colors = {
  fur: '#DBAF58', light: '#E4BB67', warm: '#D7A64B', shade: '#CEA04A',
  stripe: '#C68F39', darkStripe: '#BC8231', softStripe: '#D09D44',
  white: '#EAE7D5', whiteShade: '#D8D7C6', cream: '#E9C77E',
  eyeWhite: '#F6F5DD', eyeGreen: '#72B52B', eyeDark: '#407B24',
  nose: '#D99B91', noseShade: '#B97974', mouth: '#674338',
  collar: '#943A2B', collarLight: '#AA4931', collarDark: '#7C2D24',
};
const rgb = name => colors[name].match(/[0-9a-f]{2}/gi).map(n => parseInt(n, 16));
const groups = [], elements = [], nodes = new Map(), faceJobs = [];
function bone(name, origin, parent = null) {
  const id = uuid('bone/' + name);
  const group = { name, uuid: id, origin, rotation: [0, 0, 0], export: true,
    visibility: true, autouv: 0, shade: true, mirror_uv: false, color: groups.length % 8 };
  groups.push(group);
  const node = { uuid: id, isOpen: name === 'root' || name === 'torso', children: [] };
  nodes.set(name, node);
  if (parent) nodes.get(parent).children.push(node);
  return group;
}
function pointOnFace(face, from, to, u, v) {
  const lerp = (a, b, f) => a + (b - a) * f;
  const x = lerp(from[0], to[0], u), y = lerp(to[1], from[1], v), z = lerp(from[2], to[2], u);
  if (face === 'north') return [x, y, from[2]];
  if (face === 'south') return [lerp(to[0], from[0], u), y, to[2]];
  if (face === 'east') return [to[0], y, lerp(to[2], from[2], u)];
  if (face === 'west') return [from[0], y, z];
  if (face === 'up') return [x, to[1], lerp(from[2], to[2], v)];
  return [x, from[1], lerp(to[2], from[2], v)];
}
const bands = [[-20, -15, 23], [-9, -4, 21], [3, 9, 24], [16, 21, 20], [28, 33, 23]];
function fur(kind, face, p) {
  const [x, y, z] = p;
  const light = face === 'up';
  if (kind === 'body') {
    if (face === 'down' || y < 18) return face === 'down' ? 'whiteShade' : 'white';
    const band = bands.find(([a, b]) => z >= a && z < b);
    if (band && (light || y >= band[2])) return light ? 'stripe' : 'stripe';
    if (face === 'north' || face === 'south') {
      if (Math.abs(x) > 10 && y > 25) return 'softStripe';
      if (Math.abs(x) < 3 && y > 39) return 'stripe';
    }
    if (y < 24) return 'warm';
    return light ? 'light' : 'fur';
  }
  if (kind === 'neck') return face === 'down' ? 'cream' : 'fur';
  if (kind === 'head') {
    if (face === 'north') {
      // Three broad texel columns per eye; no raised geometry or glow.
      const ax = Math.abs(x);
      if (y >= 47.5 && y < 53.5 && ax >= 4) {
        return ax >= 8 ? 'eyeWhite' : ax >= 6 ? 'eyeGreen' : 'eyeDark';
      }
      if (y >= 53.5 && (ax < 2 || ax >= 6 && ax < 8)) return 'stripe';
      if (y >= 51.5 && y < 53.5 && ax >= 2 && ax < 4) return 'softStripe';
      if (y < 39.5 && ax > 6) return 'light';
      return 'fur';
    }
    if (face === 'east' || face === 'west') {
      if (z > -42 && z < -38 && y > 41 || z > -33 && z < -29 && y > 39) return 'stripe';
      if (z < -43 && y < 45) return 'light';
      return y < 39 ? 'light' : 'fur';
    }
    if (face === 'up') return Math.abs(x) < 2 || z > -33 && z < -29 ? 'stripe' : 'light';
    return face === 'down' ? 'cream' : 'fur';
  }
  if (kind === 'muzzle') {
    if (face === 'north' && Math.abs(x) < 2.1 && y > 42) return 'nose';
    return face === 'up' ? 'light' : 'cream';
  }
  if (kind === 'jaw') return face === 'up' ? 'mouth' : y < 37.2 ? 'cream' : 'fur';
  if (kind === 'ear') {
    if (face === 'north') return Math.abs(x) > 5.4 && Math.abs(x) < 9.3 ? 'white' : 'cream';
    return face === 'up' ? 'white' : 'fur';
  }
  if (kind === 'leg') {
    if (y < 5.1) return face === 'down' ? 'whiteShade' : 'white';
    return (Math.abs(x) > 11.8 || z % 10 < 3) && y > 9 ? 'softStripe' : 'fur';
  }
  if (kind === 'tail') {
    if (z >= 69) return light ? 'white' : 'whiteShade';
    if (z > 46 && z < 51 || z > 60 && z < 65) return 'stripe';
    return light ? 'light' : 'fur';
  }
  if (kind === 'collar') return light ? 'collarLight' : face === 'down' ? 'collarDark' : 'collar';
  return kind;
}
function cube(name, group, from, to, paint, density = 2) {
  const e = { name, type: 'cube', uuid: uuid('cube/' + name), box_uv: false, autouv: 0,
    from, to, origin: [0, 0, 0], rotation: [0, 0, 0], color: 1, export: true,
    visibility: true, render_order: 'default', faces: {} };
  const [dx, dy, dz] = to.map((n, i) => n - from[i]);
  for (const face of ['north', 'east', 'south', 'west', 'up', 'down']) {
    const w = Math.max(1, Math.ceil((face === 'east' || face === 'west' ? dz : dx) / density));
    const h = Math.max(1, Math.ceil((face === 'up' || face === 'down' ? dz : dy) / density));
    faceJobs.push({ e, face, w, h, paint });
  }
  elements.push(e);
  nodes.get(group).children.push(e.uuid);
  return e;
}

bone('root', [0, 0, 0]);
bone('torso', [0, 33, 6], 'root');
bone('body_mass', [0, 33, 6], 'torso');
cube('long_block_body', 'body_mass', [-16, 16, -23], [16, 50, 37], 'body');
bone('neck', [0, 43, -22], 'torso');
cube('short_neck', 'neck', [-10, 30, -27], [10, 51, -21], 'neck');
bone('head', [0, 46, -25], 'neck');
cube('square_head', 'head', [-10, 39, -46], [10, 56, -26], 'head');
cube('lower_cheeks', 'head', [-10, 36, -43], [10, 39, -26], 'head');
cube('block_muzzle', 'head', [-6, 39, -49], [6, 45, -44], 'muzzle');
bone('jaw', [0, 39, -39], 'head');
cube('lower_jaw', 'jaw', [-6, 36, -48.7], [6, 39, -38.8], 'jaw');
for (const sign of [-1, 1]) {
  const name = sign < 0 ? 'right_ear' : 'left_ear';
  bone(name, [sign * 7.5, 55.8, -36], 'head');
  cube(name, name, [sign * 7.5 - 2.5, 55.8, -38], [sign * 7.5 + 2.5, 61, -34], 'ear');
}
bone('collar', [0, 46, -25.3], 'head');
cube('collar_top', 'collar', [-10.5, 55.9, -28], [10.5, 56.5, -24.9], 'collar', 1);
cube('collar_bottom', 'collar', [-10.5, 35.5, -28], [10.5, 36.1, -24.9], 'collar', 1);
cube('collar_left', 'collar', [10, 36.1, -28], [10.5, 55.9, -24.9], 'collar', 1);
cube('collar_right', 'collar', [-10.5, 36.1, -28], [-10, 55.9, -24.9], 'collar', 1);
for (const front of [true, false]) for (const left of [true, false]) {
  const name = `${left ? 'left' : 'right'}_${front ? 'front' : 'hind'}_leg`;
  const x = left ? 11 : -11, z = front ? -17 : 29;
  bone(name, [x, 20, z], 'torso');
  cube(name + '_upper', name, [x - 3, 9, z - 3], [x + 3, 21, z + 3], 'leg');
  bone(name + '_paw', [x, 9, z], name);
  cube(name + '_lower', name + '_paw', [x - 3, 0, z - 3], [x + 3, 9, z + 3], 'leg');
}
bone('tail_base', [0, 44, 36], 'torso');
cube('tail_proximal', 'tail_base', [-2, 42, 36], [2, 46, 54], 'tail');
bone('tail_middle', [0, 44, 54], 'tail_base');
cube('tail_distal', 'tail_middle', [-2, 42, 54], [2, 46, 69], 'tail');
bone('tail_tip', [0, 44, 69], 'tail_middle');
cube('white_tail_tip', 'tail_tip', [-2, 42, 69], [2, 46, 74], 'tail');

// Pack real per-face UV islands with a 1 px extruded gutter. Nearest sampling
// is retained in the project and the eventual glTF export.
const atlasSize = 128, atlas = new PNG({ width: atlasSize, height: atlasSize });
const ordered = [...faceJobs].sort((a, b) => b.h - a.h || b.w - a.w || a.e.name.localeCompare(b.e.name) || a.face.localeCompare(b.face));
let cursorX = 0, cursorY = 0, rowHeight = 0;
function put(x, y, color) {
  const k = (y * atlasSize + x) * 4;
  atlas.data.set([...rgb(color), 255], k);
}
for (const job of ordered) {
  if (cursorX + job.w + 2 > atlasSize) { cursorY += rowHeight; cursorX = 0; rowHeight = 0; }
  assert(cursorY + job.h + 2 <= atlasSize, 'UV atlas overflow');
  const u = cursorX + 1, v = cursorY + 1;
  job.e.faces[job.face] = { uv: [u, v, u + job.w, v + job.h], texture: 0 };
  for (let y = -1; y <= job.h; y++) for (let x = -1; x <= job.w; x++) {
    const sx = Math.max(0, Math.min(job.w - 1, x)), sy = Math.max(0, Math.min(job.h - 1, y));
    const point = pointOnFace(job.face, job.e.from, job.e.to, (sx + .5) / job.w, (sy + .5) / job.h);
    put(u + x, v + y, fur(job.paint, job.face, point));
  }
  cursorX += job.w + 2;
  rowHeight = Math.max(rowHeight, job.h + 2);
}
const texture = PNG.sync.write(atlas);
const animations = [];
function animation(name, length, loop = false) {
  const a = { uuid: uuid('animation/' + name), name: 'animation.giant_tabby.' + name,
    loop: loop ? 'loop' : 'once', length, snapping: 30, override: false,
    anim_time_update: '', blend_weight: '', start_delay: '', loop_delay: '', animators: {} };
  animations.push(a);
  return a;
}
function track(a, name, channel, keys) {
  const id = uuid('bone/' + name);
  assert(groups.some(g => g.uuid === id), 'Unknown bone ' + name);
  const animator = a.animators[id] ||= { name, type: 'bone', rotation_global: false, keyframes: [] };
  for (const [t, value] of keys) animator.keyframes.push({
    channel, data_points: [Object.fromEntries(['x', 'y', 'z'].map((k, i) =>
      [k, String(round(value[i] * (channel === 'rotation' && i === 0 ? -1 : 1)))]))],
    uuid: uuid(`${a.name}/${name}/${channel}/${t}`), time: round(t), color: -1, interpolation: 'linear',
  });
}
function sampled(a, name, channel, fn) {
  const n = Math.round(a.length * 30);
  track(a, name, channel, Array.from({ length: n + 1 }, (_, i) => [a.length * i / n, fn(i / n, a.length * i / n)]));
}
function easedTrack(a, name, channel, keys) {
  sampled(a, name, channel, (_, time) => {
    let i = 0;
    while (i < keys.length - 2 && time > keys[i + 1][0]) i++;
    const [ta, va] = keys[i], [tb, vb] = keys[i + 1];
    let f = Math.max(0, Math.min(1, (time - ta) / (tb - ta)));
    f = f * f * (3 - 2 * f);
    return va.map((n, c) => n + (vb[c] - n) * f);
  });
}
const tau = Math.PI * 2;
const idle = animation('idle', 4, true);
sampled(idle, 'body_mass', 'scale', p => [1 + .007 * Math.sin(p * tau), 1 + .009 * Math.sin(p * tau), 1]);
sampled(idle, 'head', 'rotation', p => [2 + 1.5 * Math.sin(p * tau), 2 * Math.sin(p * tau), 0]);
sampled(idle, 'tail_base', 'rotation', p => [-5, 4 * Math.sin(p * tau), 0]);
sampled(idle, 'tail_middle', 'rotation', p => [-8, 7 * Math.sin(p * tau + .5), 0]);
sampled(idle, 'tail_tip', 'rotation', p => [-3, 9 * Math.sin(p * tau + 1), 0]);
easedTrack(idle, 'left_ear', 'rotation', [[0, [0, 0, 0]], [1.7, [0, 0, 0]], [1.87, [0, -7, -5]], [2.13, [0, 0, 0]], [4, [0, 0, 0]]]);
for (const running of [false, true]) {
  const a = animation(running ? 'run' : 'walk', running ? .8 : 1.6, true);
  for (const front of [true, false]) for (const left of [true, false]) {
    const name = `${left ? 'left' : 'right'}_${front ? 'front' : 'hind'}_leg`;
    const phase = front === left ? 0 : Math.PI;
    sampled(a, name, 'rotation', p => [(running ? 32 : 18) * Math.sin(p * tau + phase), 0, 0]);
    sampled(a, name + '_paw', 'rotation', p => [-(running ? 23 : 13) * Math.max(0, Math.sin(p * tau + phase)), 0, 0]);
  }
  sampled(a, 'torso', 'position', p => [0, (running ? 1.2 : .35) * (1 - Math.cos(p * tau * 2)), 0]);
  sampled(a, 'head', 'rotation', p => [2 + (running ? 3 : 1.2) * Math.sin(p * tau * 2), 0, 0]);
  sampled(a, 'tail_base', 'rotation', p => [-5, (running ? 10 : 5) * Math.sin(p * tau), 0]);
  sampled(a, 'tail_middle', 'rotation', p => [-8, 7 * Math.sin(p * tau + .7), 0]);
}
const bite = animation('bite', 1.2);
easedTrack(bite, 'head', 'rotation', [[0, [0, 0, 0]], [.32, [-15, 0, 0]], [.6, [18, 0, 0]], [.78, [14, 0, 0]], [1.2, [0, 0, 0]]]);
easedTrack(bite, 'neck', 'position', [[0, [0, 0, 0]], [.3, [0, 1, 2]], [.6, [0, 0, -4]], [.8, [0, 0, -2]], [1.2, [0, 0, 0]]]);
easedTrack(bite, 'jaw', 'rotation', [[0, [0, 0, 0]], [.3, [32, 0, 0]], [.5, [32, 0, 0]], [.63, [2, 0, 0]], [.9, [8, 0, 0]], [1.2, [0, 0, 0]]]);
const swipe = animation('paw_swipe', 1.5);
easedTrack(swipe, 'left_front_leg', 'rotation', [[0, [0, 0, 0]], [.45, [-65, -12, 15]], [.68, [-45, 25, -25]], [.84, [22, 12, -9]], [1.5, [0, 0, 0]]]);
easedTrack(swipe, 'left_front_leg_paw', 'rotation', [[0, [0, 0, 0]], [.4, [25, 0, 0]], [.72, [-8, 0, 0]], [1.5, [0, 0, 0]]]);
easedTrack(swipe, 'head', 'rotation', [[0, [0, 0, 0]], [.45, [-5, -12, 0]], [.85, [7, 14, 0]], [1.5, [0, 0, 0]]]);
const leap = animation('pounce', 1.8);
easedTrack(leap, 'torso', 'position', [[0, [0, 0, 0]], [.45, [0, -4, 0]], [.85, [0, 13, 0]], [1.05, [0, 11, 0]], [1.4, [0, -3, 0]], [1.8, [0, 0, 0]]]);
easedTrack(leap, 'torso', 'rotation', [[0, [0, 0, 0]], [.45, [-5, 0, 0]], [.85, [-10, 0, 0]], [1.25, [8, 0, 0]], [1.8, [0, 0, 0]]]);
for (const name of ['left_front_leg', 'right_front_leg', 'left_hind_leg', 'right_hind_leg']) {
  const front = name.includes('front');
  easedTrack(leap, name, 'rotation', [[0, [0, 0, 0]], [.45, [front ? 25 : -28, 0, 0]], [.85, [front ? -65 : 55, 0, 0]], [1.2, [front ? -30 : 15, 0, 0]], [1.4, [front ? 16 : -20, 0, 0]], [1.8, [0, 0, 0]]]);
}
const hurt = animation('hurt', .6);
easedTrack(hurt, 'head', 'rotation', [[0, [0, 0, 0]], [.14, [-9, 9, -5]], [.35, [3, -3, 2]], [.6, [0, 0, 0]]]);
easedTrack(hurt, 'body_mass', 'scale', [[0, [1, 1, 1]], [.14, [1.025, .97, 1.015]], [.6, [1, 1, 1]]]);
const death = animation('death', 2.4);
death.loop = 'hold';
easedTrack(death, 'root', 'rotation', [[0, [0, 0, 0]], [.4, [0, 0, 6]], [1.3, [0, 0, 90]], [1.6, [0, 0, 86]], [1.95, [0, 0, 90]], [2.4, [0, 0, 90]]]);
easedTrack(death, 'root', 'position', [[0, [0, 0, 0]], [.4, [0, 0, 0]], [1.3, [0, 16, 0]], [1.6, [0, 17, 0]], [1.95, [0, 16, 0]], [2.4, [0, 16, 0]]]);
easedTrack(death, 'head', 'rotation', [[0, [0, 0, 0]], [1.3, [0, 0, -9]], [2.4, [0, 0, -9]]]);
for (const name of ['left_front_leg', 'right_front_leg', 'left_hind_leg', 'right_hind_leg'])
  easedTrack(death, name, 'rotation', [[0, [0, 0, 0]], [1.3, [name.includes('front') ? -18 : 20, 0, 0]], [2.4, [name.includes('front') ? -18 : 20, 0, 0]]]);

// Contact correction uses the same ZYX Euler order as Blockbench. Do not
// stretch the legs or bake mesh deformations into the model to hide clipping.
const parentOf = new Map(), ownerOf = new Map(), groupOf = new Map(groups.map(g => [g.uuid, g]));
function indexParents(node, parent = null) {
  if (parent) parentOf.set(node.uuid, parent);
  for (const child of node.children) {
    if (typeof child === 'string') ownerOf.set(child, node.uuid);
    else indexParents(child, node.uuid);
  }
}
indexParents(nodes.get('root'));
function valueAt(a, id, channel, time) {
  const keys = a.animators[id]?.keyframes.filter(k => k.channel === channel);
  if (!keys?.length) return channel === 'scale' ? [1, 1, 1] : [0, 0, 0];
  let i = 0;
  while (i < keys.length - 2 && keys[i + 1].time < time) i++;
  const first = keys[i], last = keys[Math.min(i + 1, keys.length - 1)];
  const f = Math.max(0, Math.min(1, (time - first.time) / (last.time - first.time || 1)));
  return ['x', 'y', 'z'].map(axis => Number(first.data_points[0][axis]) * (1 - f) + Number(last.data_points[0][axis]) * f);
}
function poseMinimum(a, time) {
  const transforms = new Map(groups.map(g => [g.uuid, {
    g, pos: valueAt(a, g.uuid, 'position', time), scale: valueAt(a, g.uuid, 'scale', time),
    rot: valueAt(a, g.uuid, 'rotation', time).map(n => n * Math.PI / 180),
  }]));
  let min = Infinity;
  for (const e of elements) for (let i = 0; i < 8; i++) {
    let p = e.from.map((n, axis) => i & (1 << axis) ? e.to[axis] : n);
    let id = ownerOf.get(e.uuid);
    while (id) {
      const t = transforms.get(id);
      let [x, y, z] = p.map((n, axis) => (n - t.g.origin[axis]) * t.scale[axis]);
      const [rx, ry, rz] = t.rot;
      [y, z] = [y * Math.cos(rx) - z * Math.sin(rx), y * Math.sin(rx) + z * Math.cos(rx)];
      [x, z] = [x * Math.cos(ry) + z * Math.sin(ry), -x * Math.sin(ry) + z * Math.cos(ry)];
      [x, y] = [x * Math.cos(rz) - y * Math.sin(rz), x * Math.sin(rz) + y * Math.cos(rz)];
      p = [x, y, z].map((n, axis) => n + t.g.origin[axis] + t.pos[axis]);
      id = parentOf.get(id);
    }
    min = Math.min(min, p[1]);
  }
  return min;
}
const rootId = uuid('bone/root');
for (const a of animations) {
  const n = Math.round(a.length * 30), grounded = a !== leap;
  const keys = Array.from({ length: n + 1 }, (_, i) => {
    const time = round(a.length * i / n), p = valueAt(a, rootId, 'position', time);
    const min = poseMinimum(a, time);
    p[1] -= grounded ? min : Math.min(0, min);
    return [time, p.map(round)];
  });
  if (a.animators[rootId]) a.animators[rootId].keyframes = a.animators[rootId].keyframes.filter(k => k.channel !== 'position');
  track(a, 'root', 'position', keys);
  for (const [time] of keys) assert(poseMinimum(a, time) > -.0001, a.name + ' floor clipping');
}

const model = {
  meta: { format_version: '5.0', model_format: 'free', box_uv: false },
  name: '巨型橘猫 Boss · 参考复刻', model_identifier: 'giant_tabby_boss', geometry_name: 'giant_tabby_boss',
  resolution: { width: atlasSize, height: atlasSize },
  elements, groups, outliner: [nodes.get('root')], animations,
  textures: [{ name: 'giant_tabby_boss.png', id: '0', uuid: uuid('texture'), path: '', folder: '', namespace: '',
    width: atlasSize, height: atlasSize, uv_width: atlasSize, uv_height: atlasSize,
    render_mode: 'default', render_sides: 'auto', visible: true, internal: true,
    source: 'data:image/png;base64,' + texture.toString('base64') }],
  animation_variable_placeholders: '',
};
const modelBytes = Buffer.from(JSON.stringify(model) + '\n');
const manifest = {
  schema: 1, kind: 'standalone-boss-art-not-gameplay', id: 'giant_tabby_boss',
  title: '巨型橘猫（暂定名）',
  reference: {
    supplied_image: '42ae2f7b699ece60dfc22b2cd889728b.jpg',
    episode: 'Alan Becker — Creeper Clan / Animation vs. Minecraft Shorts Ep 40',
    bilibili: 'https://www.bilibili.com/video/BV163426vE3s/?t=625',
    youtube: 'https://www.youtube.com/watch?v=FcQlQAs5EDM',
    screenshot_time_seconds: 625,
    notes: 'Observed side, rear and running thumbnail views. Red line figure is Red, not part of the collar. Unseen front/underside details are inferred. Newly authored reconstruction, not an official extracted model.',
  },
  scale: { units_per_block: 16, neutral_ear_height_blocks: 61 / 16, body_width_blocks: 2,
    nose_to_tail_blocks: 123 / 16, forward: '-Z', up: '+Y', floor_y: 0,
    notes: 'Editable reconstruction scale, not a measured canonical world size.' },
  counts: { cubes: elements.length, bones: groups.length, animations: animations.length },
  texture: { width: atlasSize, height: atlasSize, filtering: 'nearest', extruded_gutter_pixels: 1 },
  animations: animations.map(a => ({ name: a.name, seconds: a.length, loop: a.loop,
    purpose: a.name.split('.').at(-1) === 'pounce' ? 'in-place visual clip; entity controls horizontal movement' : 'visual only; no damage events' })),
  files: Object.fromEntries([['giant_tabby_boss.bbmodel', modelBytes], ['giant_tabby_boss.png', texture]].map(([name, bytes]) =>
    [name, { bytes: bytes.length, sha256: createHash('sha256').update(bytes).digest('hex') }])),
};
function emit(name, bytes) {
  const p = path.join(outDir, name);
  if (writing) { mkdirSync(outDir, { recursive: true }); writeFileSync(p, bytes); }
  else assert.deepEqual(readFileSync(p), Buffer.from(bytes), name + ' is stale');
}
emit('giant_tabby_boss.bbmodel', modelBytes);
emit('giant_tabby_boss.png', texture);
emit('manifest.json', JSON.stringify(manifest, null, 2) + '\n');

// Structural and animation boundary checks, independent of Blockbench UI.
let checks = 0;
const allIds = [...groups, ...elements, ...animations].map(e => e.uuid);
assert.equal(new Set(allIds).size, allIds.length); checks++;
for (const e of elements) {
  assert(e.to.every((n, i) => n > e.from[i])); checks++;
  for (const f of Object.values(e.faces)) {
    assert.equal(f.texture, 0);
    assert(f.uv.every(n => n >= 1 && n <= atlasSize - 1)); checks += 2;
  }
}
for (const a of animations) for (const animator of Object.values(a.animators)) {
  for (const k of animator.keyframes) {
    assert(k.time >= 0 && k.time <= a.length);
    assert(Object.values(k.data_points[0]).every(n => Number.isFinite(Number(n)))); checks += 2;
  }
  for (const channel of new Set(animator.keyframes.map(k => k.channel))) {
    const k = animator.keyframes.filter(k => k.channel === channel);
    assert.equal(k[0].time, 0); assert.equal(k.at(-1).time, a.length); checks += 2;
    if (a.loop === 'loop') { assert.deepEqual(k[0].data_points, k.at(-1).data_points); checks++; }
  }
}
assert.equal(Math.min(...elements.map(e => e.from[1])), 0); checks++;
console.log(JSON.stringify({ mode: writing ? 'write' : 'verify', output: outDir, ...manifest.counts,
  atlas_used_height: cursorY + rowHeight, checks, native_blockbench_validation: 'separate step required' }, null, 2));
