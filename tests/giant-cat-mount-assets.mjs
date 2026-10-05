import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';
import { createRequire } from 'node:module';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const source = JSON.parse(fs.readFileSync(path.join(root, 'art/giant-cat-mount/source/vanilla_yellow_cat.bbmodel'), 'utf8'));
const exporter = path.join(root, 'tools/export-giant-cat-mount.mjs');
execFileSync(process.execPath, [exporter], { cwd: root, stdio: 'pipe' });

const editable = JSON.parse(fs.readFileSync(path.join(root, 'art/giant-cat-mount/giant_cat_mount.bbmodel'), 'utf8'));
// Exercise geometry endpoints through the runtime's T * Rz * Ry * Rx * S hierarchy.
// A sibling tail tip or an off-center tip pivot separates these two contact points.
const seamData = JSON.parse(fs.readFileSync(path.join(root,
    'forge-1.20.1/src/main/resources/assets/laowu/cat_animation_clips/giant_cat_mount.json')));
function sampleChannel(frames, time, fallback) {
    if (!frames) return fallback;
    const index = frames.findIndex(frame => frame[0] >= time);
    if (index <= 0) return (index < 0 ? frames.at(-1) : frames[0]).slice(1);
    const a = frames[index - 1], b = frames[index];
    const u = (time - a[0]) / (b[0] - a[0]), h = u * u * (3 - 2 * u);
    return a.slice(1).map((v, i) => v + (b[i + 1] - v) * h);
}
function groupChain(model, elementId) {
    const groups = new Map(model.groups.map(group => [group.uuid, group]));
    function visit(nodes, chain) {
        for (const node of nodes) {
            if (node === elementId) return chain;
            if (typeof node === 'object') {
                const found = visit(node.children, [...chain, groups.get(node.uuid)]);
                if (found) return found;
            }
        }
    }
    return visit(model.outliner, []);
}
function worldPoint(model, element, point, clip, time) {
    const chain = groupChain(model, element.uuid);
    let p = point.map((v, i) => v - chain.at(-1).origin[i]);
    for (let index = chain.length - 1; index >= 0; index--) {
        const group = chain[index], channels = clip.bones[group.name] ?? {};
        const scale = sampleChannel(channels.scale, time, [1, 1, 1]);
        const rotation = sampleChannel(channels.rotation, time, [0, 0, 0]);
        const position = sampleChannel(channels.position, time, [0, 0, 0]);
        let [x, y, z] = p.map((v, i) => v * scale[i]);
        const [rx, ry, rz] = rotation.map((v, i) => (v + group.rotation[i]) * Math.PI / 180);
        [y, z] = [y * Math.cos(rx) - z * Math.sin(rx), y * Math.sin(rx) + z * Math.cos(rx)];
        [x, z] = [x * Math.cos(ry) + z * Math.sin(ry), -x * Math.sin(ry) + z * Math.cos(ry)];
        [x, y] = [x * Math.cos(rz) - y * Math.sin(rz), x * Math.sin(rz) + y * Math.cos(rz)];
        p = [x, y, z].map((v, i) => v + group.origin[i] - (chain[index - 1]?.origin[i] ?? 0) + position[i]);
    }
    return p;
}
const base = editable.elements.find(element => element.name === 'tail_base_cube');
const tip = editable.elements.find(element => element.name === 'tail_tip_cube');
// Catches omitted/compounded transverse resizing or shortening the wrong axis.
// The archive has two 1 x 8 x 1 pixel tail segments: requested output is 1.3 x 6 x 1.3.
for (const segment of [base, tip]) {
    for (const [axis, expected] of [[0, 1.3], [1, 6], [2, 1.3]]) {
        assert.ok(Math.abs(segment.to[axis] - segment.from[axis] - expected) < 1e-9,
            `${segment.name} axis ${axis} must render 130% thickness / 75% length`);
    }
    const at = (point, time) => worldPoint(editable, segment, point, seamData.clips.rest, time);
    const edgeLength = (axis, time) => {
        const a = [...segment.from], b = [...a]; b[axis] = segment.to[axis];
        return Math.hypot(...at(a, time).map((v, i) => v - at(b, time)[i]));
    };
    for (const axis of [0, 2]) {
        assert.ok(Math.abs(edgeLength(axis, 0) - 1.3) < 1e-9, 'standing tail keeps base thickness');
        assert.ok(Math.abs(edgeLength(axis, 1) - 1.56) < 1e-9,
            `${segment.name} thickens by another 120% at rest, exactly once`);
        let previous = edgeLength(axis, 0);
        for (let frame = 1; frame <= 120; frame++) {
            const current = edgeLength(axis, frame / 120);
            assert.ok(current >= previous - 1e-9 && current - previous < .01,
                `${segment.name} rest thickness approaches smoothly without overshoot`);
            previous = current;
        }
    }
    for (const time of [0, .25, .5, .75, 1]) {
        assert.ok(Math.abs(edgeLength(1, time) - 6) < 1e-9,
            `${segment.name} rest thickening never stretches tail length`);
    }
}
const corners = element => [element.from[0], element.to[0]].flatMap(x =>
    [element.from[1], element.to[1]].flatMap(y => [element.from[2], element.to[2]].map(z => [x, y, z])));
const legs = editable.elements.filter(element => element.name.endsWith('_leg_cube'));
const torso = editable.elements.find(element => element.name === 'body_cube');
// Moving either shared ancestor during idle lifts/rocks the paws, even without leg keys.
for (let frame = 0; frame <= 240; frame++) {
    const time = seamData.clips.idle.length * frame / 240;
    for (const leg of legs) for (const corner of corners(leg)) {
        const neutral = worldPoint(editable, leg, corner, { bones: {} }, 0);
        const idle = worldPoint(editable, leg, corner, seamData.clips.idle, time);
        assert.ok(Math.hypot(...idle.map((v, i) => v - neutral[i])) < 1e-6,
            `${leg.name} must remain planted during idle at ${time}`);
    }
}
const torsoCenter = torso.from.map((v, i) => (v + torso.to[i]) / 2);
assert.ok(Math.hypot(...worldPoint(editable, torso, torsoCenter, seamData.clips.idle, 1)
    .map((v, i) => v - worldPoint(editable, torso, torsoCenter, seamData.clips.idle, 0)[i])) > .1,
    'idle torso still visibly breathes while paws stay planted');
assert.equal(sampleChannel(seamData.clips.rest.bones.group2.position, 0, [0, 0, 0])[1], 0,
    'lie-down starts standing, so folded limbs can reach the floor before the body settles');
for (let frame = 0; frame <= 1200; frame++) {
    const time = frame / 1200;
    for (const element of editable.elements.filter(element => element.name.endsWith('_leg_cube'))) {
        const heights = [];
        for (const x of [element.from[0], element.to[0]])
            for (const y of [element.from[1], element.to[1]])
                for (const z of [element.from[2], element.to[2]])
                    heights.push(worldPoint(editable, element, [x, y, z], seamData.clips.rest, time)[1]);
        assert.ok(Math.min(...heights) >= -.15, `${element.name} sinks into floor during lie-down at ${time}`);
    }
    const heights = editable.elements.flatMap(element => corners(element)
        .map(point => worldPoint(editable, element, point, seamData.clips.rest, time)[1]));
    assert.ok(Math.min(...heights) >= -.15, `rest model sinks into floor at ${time}`);
    assert.ok(Math.min(...heights) < .08, `rest model floats above floor at ${time}`);
}
// A side lie turns the torso's left/right axis vertical, unlike a belly-down crouch.
const sideA = worldPoint(editable, torso, [-4, 1, -15], seamData.clips.rest, 1);
const sideB = worldPoint(editable, torso, [4, 1, -15], seamData.clips.rest, 1);
assert.ok(Math.abs(sideB[1] - sideA[1]) > 7.5, 'rest torso is rolled onto its side');
assert.ok(Math.abs(sideB[0] - sideA[0]) < .5, 'rest is not a belly-down folded pose');
assert.ok(Math.abs(Math.min(...corners(torso).map(point =>
    worldPoint(editable, torso, point, seamData.clips.rest, 1)[1]))) < .05,
    'the torso side contacts the floor at rest');
// A globally grounded body can still leave the whole tail floating. Check the
// actual distal segment, including its rest thickening, rather than bone keys.
const tipHeights = corners(tip).map(point => worldPoint(editable, tip, point, seamData.clips.rest, 1)[1]);
assert.ok(Math.abs(Math.min(...tipHeights)) < .08,
    `side-rest tail tip must touch the floor, not float ${Math.min(...tipHeights)} model pixels above it`);
const tipStart = worldPoint(editable, tip, [0, tip.to[1], 8.5], seamData.clips.rest, 1);
const tipEnd = worldPoint(editable, tip, [0, tip.from[1], 8.5], seamData.clips.rest, 1);
assert.ok(Math.abs(tipStart[1] - tipEnd[1]) < .08,
    'the relaxed distal tail lies along the floor, not pointing up or penetrating it');
// The old equal 55-degree fold leaves paws hidden in the torso footprint.
// Check the actual distal paw faces, not just bone angles or authored keys.
const sideFootprint = Math.max(...corners(torso).map(point =>
    worldPoint(editable, torso, point, seamData.clips.rest, 1)[0]));
const restPaws = Object.fromEntries(legs.map(leg => {
    const paw = [leg.from[0], leg.to[0]].flatMap(x => [leg.from[2], leg.to[2]]
        .map(z => worldPoint(editable, leg, [x, leg.from[1], z], seamData.clips.rest, 1)));
    const center = [0, 1, 2].map(axis => paw.reduce((total, point) => total + point[axis], 0) / paw.length);
    assert.ok(center[0] > sideFootprint + .4, `${leg.name} paw extends visibly beside, not beneath, the torso`);
    const sideReach = Math.max(...corners(leg).map(point =>
        worldPoint(editable, leg, point, seamData.clips.rest, 1)[0]));
    assert.ok(sideReach > sideFootprint + 2.25,
        `${leg.name} exposes a leg segment beside the belly, not just a tiny paw tip`);
    return [leg.name, { paw, center }];
}));
for (const side of ['left', 'right']) {
    assert.ok(restPaws[`${side}_front_leg_cube`].center[2] < -5.5,
        `${side} forepaw stretches side-forward toward the head`);
    assert.ok(restPaws[`${side}_hind_leg_cube`].center[2] > 7,
        `${side} hind paw relaxes beside the haunch`);
}
assert.ok(Math.abs(restPaws.left_front_leg_cube.center[2] - restPaws.right_front_leg_cube.center[2]) > .75,
    'front paws are staggered so both remain readable');
assert.ok(Math.abs(restPaws.left_hind_leg_cube.center[2] - restPaws.right_hind_leg_cube.center[2]) > 1,
    'hind paws have distinct relaxed reach instead of one overlapping folded pose');
for (const region of ['front', 'hind']) {
    assert.ok(Math.hypot(...restPaws[`left_${region}_leg_cube`].center
        .map((value, axis) => value - restPaws[`right_${region}_leg_cube`].center[axis])) > 1.75,
        `${region} paws have a visibly separated side-lying silhouette`);
}
for (const [name, clip] of Object.entries(seamData.clips)) {
    for (let frame = 0; frame <= 120; frame++) {
        const time = clip.length * frame / 120;
        const a = worldPoint(editable, base, [0, base.from[1], (base.from[2] + base.to[2]) / 2], clip, time);
        const b = worldPoint(editable, tip, [0, tip.to[1], (tip.from[2] + tip.to[2]) / 2], clip, time);
        for (const size of [.6, 1.5, 6]) {
            const gap = Math.hypot(...a.map((v, i) => (v - b[i]) * size));
            assert.ok(gap < 1e-6, `${name} tail joint opens ${gap} pixels at ${time}s, size ${size}`);
        }
    }
}
const correctedElements = structuredClone(source.elements);
const correctedBase = correctedElements.find(element => element.name === 'tail_base_cube');
correctedBase.from = [-.65, 3, 7.85]; correctedBase.to = [.65, 9, 9.15];
const correctedTip = correctedElements.find(element => element.name === 'tail_tip_cube');
correctedTip.from = [-.65, -3, 7.85]; correctedTip.to = [.65, 3, 9.15]; correctedTip.origin = [0, 3, 8.5];
// Resize/reparent arithmetic can differ by an ULP; only tail coordinates get
// tolerance. All metadata, UVs and non-tail geometry remain exact comparisons.
const comparableElements = elements => elements.map(element => element.name.startsWith('tail_')
    ? { ...element, ...Object.fromEntries(['from', 'to', 'origin'].map(key =>
        [key, element[key].map(value => Math.round(value * 1e10) / 1e10)])) } : element);
assert.deepEqual(comparableElements(editable.elements), correctedElements,
    'only intentional tail resize/joint changes; all other cubes and every UV remain archived');
const correctedGroups = structuredClone(source.groups);
correctedGroups.find(group => group.name === 'group4').origin = [0, 3, 8.5];
const correctedTail = correctedGroups.find(group => group.name === 'tail2');
correctedTail.origin = [0, 3, 8.5]; correctedTail.rotation = [-47.4338, 0, 0];
assert.deepEqual(editable.groups.filter(group => group.name !== 'tail1_volume'), correctedGroups,
    'existing groups only adjust distal tail pivots and local rest angle');
const volume = editable.groups.find(group => group.name === 'tail1_volume');
assert.ok(volume, 'base-only volume group isolates thickness from the child tail joint');
assert.deepEqual(volume.origin, [0, 9, 8.5]);
assert.deepEqual(volume.rotation, [0, 0, 0]);
const correctedOutliner = structuredClone(source.outliner);
const oldTailParent = correctedOutliner[0].children[0].children.at(-1);
oldTailParent.children[0].children.push(oldTailParent.children.pop());
oldTailParent.children[0].children[0] = {
    uuid: volume.uuid, isOpen: false, children: [base.uuid],
};
assert.deepEqual(editable.outliner, correctedOutliner,
    'only distal-tail reparenting and base-only volume wrapper change the source hierarchy');
assert.deepEqual(editable.textures, source.textures, 'embedded 64x32 texture stays untouched');
assert.deepEqual(editable.resolution, { width: 64, height: 32 }, 'runtime UV fallback matches atlas');
assert.deepEqual(editable.animations.map(clip => clip.name), ['idle', 'walk', 'run', 'jump', 'rest']);

const runtimePath = 'src/main/resources/assets/laowu/cat_animation_clips/giant_cat_mount.json';
const modelPath = 'src/main/resources/assets/laowu/models/entity/giant_cat_mount.bbmodel';
const forge = 'forge-1.20.1/';
const neo = 'neoforge-1.21.1/';
const data = JSON.parse(fs.readFileSync(path.join(root, forge, runtimePath), 'utf8'));
assert.equal(fs.readFileSync(path.join(root, forge, runtimePath), 'utf8'),
    fs.readFileSync(path.join(root, neo, runtimePath), 'utf8'), 'loader curves are identical');
assert.equal(fs.readFileSync(path.join(root, forge, modelPath), 'utf8'),
    fs.readFileSync(path.join(root, neo, modelPath), 'utf8'), 'loader models are identical');
assert.deepEqual(comparableElements(JSON.parse(fs.readFileSync(path.join(root, forge, modelPath), 'utf8')).elements),
    correctedElements, 'runtime model only changes the requested tail dimensions and corrected seam');

const allowed = new Set([...source.groups.map(group => group.name), 'tail1_volume']);
for (const [name, clip] of Object.entries(data.clips)) {
    assert.ok(clip.length > 0, `${name} has a duration`);
    assert.ok(Object.keys(clip.bones).length >= 5, `${name} animates a meaningful portion of the cat`);
    for (const [bone, channels] of Object.entries(clip.bones)) {
        assert.ok(allowed.has(bone), `${name} only uses source bones`);
        for (const [channel, frames] of Object.entries(channels)) {
            assert.ok(['position', 'rotation', 'scale'].includes(channel));
            assert.equal(frames[0][0], 0, `${name}/${bone}/${channel} starts at zero`);
            assert.equal(frames.at(-1)[0], clip.length, `${name}/${bone}/${channel} reaches clip end`);
            for (const frame of frames) {
                assert.equal(frame.length, 4);
                assert.ok(frame.every(Number.isFinite));
                if (channel === 'scale') assert.ok(frame.slice(1).every(v => v > 0.45 && v < 1.6));
            }
            for (let index = 1; index < frames.length; index++) {
                assert.ok(frames[index][0] > frames[index - 1][0], 'keyframe times increase');
            }
        }
    }
}
assert.ok(data.clips.run.bones.group2.position.every(frame => Math.abs(frame[2]) <= .22),
    'run root lift stays below .22 pixels: spring comes from deformation, not bouncing');
assert.ok(data.clips.run.bones.group5.rotation.every(frame => Math.abs(frame[1]) <= 2),
    'run body pitch stays subtle');
for (const bone of ['left_front_leg', 'right_front_leg', 'left_hind_leg', 'right_hind_leg']) {
    assert.ok(data.clips.run.bones[bone].scale?.some(frame => frame[2] < .86), `${bone} visibly compresses`);
    assert.ok(data.clips.run.bones[bone].scale?.some(frame => frame[2] > 1.1), `${bone} visibly stretches`);
}
assert.ok(data.clips.jump.bones.group2.position.some(frame => frame[2] > .3), 'jump has a visible secondary lift');
assert.ok(data.clips.jump.bones.group2.position.every(frame => frame[2] <= .75),
    'jump clip does not add a second physical jump');
assert.ok(data.clips.jump.bones.group2.scale.some(frame => frame[2] < 0.9), 'jump lands with squash');
assert.ok(data.clips.walk.bones.left_front_leg.rotation.some(frame => frame[1] > 0.2));
assert.ok(data.clips.walk.bones.right_front_leg.rotation.some(frame => frame[1] < -0.2));
for (const time of [0, .26]) {
    const angle = bone => data.clips.run.bones[bone].rotation.find(frame => frame[0] === time)[1];
    assert.ok(angle('left_front_leg') * angle('right_front_leg') > 0,
        'run front legs propel together, unlike the walk');
    assert.ok(angle('left_hind_leg') * angle('right_hind_leg') > 0,
        'run hind legs push together');
    assert.ok(angle('left_front_leg') * angle('left_hind_leg') < 0,
        'run front and hind strides oppose');
}
const frontUuid = source.groups.find(group => group.name === 'left_front_leg').uuid;
const walkPreview = editable.animations.find(clip => clip.name === 'walk');
const frontKeys = walkPreview.animators[frontUuid].keyframes
    .filter(key => key.channel === 'rotation').sort((a, b) => a.time - b.time);
assert.ok(frontKeys.length >= 48, 'editable walk curve is densely sampled for runtime parity');
assert.ok(frontKeys.every(key => key.interpolation === 'linear'), 'Blockbench uses linear between dense samples');
const samplePreview = time => {
    const end = frontKeys.findIndex(key => key.time >= time);
    const before = frontKeys[end - 1];
    const after = frontKeys[end];
    if (!before) return Number(after.data_points[0].x);
    const alpha = (time - before.time) / (after.time - before.time);
    return Number(before.data_points[0].x) * (1 - alpha) + Number(after.data_points[0].x) * alpha;
};
assert.ok(Math.abs(samplePreview(.05) - 21.9375) < .02,
    'Blockbench walk quarter-segment matches runtime Hermite rather than sparse linear');
const ids = new Map(editable.groups.map(group => [group.name, group.uuid]));
const hermite = (frames, time, axis) => {
    const end = frames.findIndex(frame => frame[0] >= time);
    if (end <= 0) return frames[0][axis];
    const first = frames[end - 1], last = frames[end];
    const u = (time - first[0]) / (last[0] - first[0]);
    return first[axis] + (last[axis] - first[axis]) * u * u * (3 - 2 * u);
};
const linear = (frames, time, axis) => {
    const end = frames.findIndex(frame => frame.time >= time);
    if (end <= 0) return Number(frames[0].data_points[0][axis]);
    const first = frames[end - 1], last = frames[end];
    const u = (time - first.time) / (last.time - first.time);
    return Number(first.data_points[0][axis]) * (1 - u) + Number(last.data_points[0][axis]) * u;
};
for (const [clipName, clip] of Object.entries(data.clips)) {
    const preview = editable.animations.find(animation => animation.name === clipName);
    for (const [bone, channels] of Object.entries(clip.bones)) {
        for (const [channel, runtimeFrames] of Object.entries(channels)) {
            const previewFrames = preview.animators[ids.get(bone)].keyframes
                .filter(frame => frame.channel === channel).sort((a, b) => a.time - b.time);
            for (let sample = 0; sample <= Math.ceil(clip.length * 120); sample++) {
                const time = Math.min(clip.length, sample / 120);
                for (const [axis, index] of [['x', 1], ['y', 2], ['z', 3]]) {
                    const error = Math.abs(hermite(runtimeFrames, time, index) - linear(previewFrames, time, axis));
                    const tolerance = channel === 'rotation' ? .65 : channel === 'scale' ? .006 : .03;
                    assert.ok(error < tolerance, `${clipName}/${bone}/${channel}/${axis} preview error ${error}`);
                }
            }
        }
    }
}
console.log('giant cat mount assets: OK');

// Optional diagnostic render uses exactly the transformed exported cube corners.
// The bundled canvas dependency is supplied through NODE_PATH, never production.
if (process.argv.includes('--preview')) {
    const { createCanvas } = createRequire(import.meta.url)('@napi-rs/canvas');
    const canvas = createCanvas(1500, 720), ctx = canvas.getContext('2d');
    ctx.fillStyle = '#eee8de'; ctx.fillRect(0, 0, 1500, 720);
    const dot = (a, b) => a.reduce((sum, value, axis) => sum + value * b[axis], 0);
    const unit = vector => vector.map(value => value / Math.hypot(...vector));
    const colors = { body_cube: '#daa944', head_main: '#e6bb61', nose: '#dbae60',
        left_front_leg_cube: '#f3e4bc', right_front_leg_cube: '#f0c7a5',
        left_hind_leg_cube: '#d6dfb4', right_hind_leg_cube: '#b2c89b' };
    const faces = [[0, 1, 3, 2], [4, 6, 7, 5], [0, 4, 5, 1],
        [2, 3, 7, 6], [0, 2, 6, 4], [1, 5, 7, 3]];
    const views = [{ label: 'Side lie - side view', camera: [28, 7, -5], time: 1 },
        { label: 'Side lie - above', camera: [4, 32, -4], time: 1 },
        { label: 'Lie / get-up transition', camera: [24, 16, -24], time: .5 }];
    views.forEach((view, panel) => {
        const depth = unit(view.camera), right = unit([depth[2], 0, -depth[0]]);
        const up = [depth[1] * right[2], depth[2] * right[0] - depth[0] * right[2], -depth[1] * right[0]];
        const project = point => [panel * 500 + 250 + dot(point, right) * 14,
            385 - dot(point, up) * 14];
        ctx.fillStyle = '#584d3c'; ctx.font = 'bold 22px sans-serif'; ctx.fillText(view.label, panel * 500 + 25, 40);
        const polygons = editable.elements.flatMap(element => {
            const points = corners(element).map(point => worldPoint(editable, element, point, seamData.clips.rest, view.time));
            return faces.map((indices, index) => ({ points: indices.map(i => points[i]),
                color: colors[element.name] ?? '#e0b65b', shade: .75 + index * .045 }));
        }).sort((a, b) => a.points.reduce((sum, point) => sum + dot(point, depth), 0)
            - b.points.reduce((sum, point) => sum + dot(point, depth), 0));
        for (const polygon of polygons) {
            ctx.beginPath(); polygon.points.forEach((point, i) => {
                const [x, y] = project(point); if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
            }); ctx.closePath();
            ctx.fillStyle = polygon.color; ctx.fill();
            ctx.fillStyle = `rgba(70, 40, 10, ${1 - polygon.shade})`; ctx.fill();
            ctx.strokeStyle = '#68533b'; ctx.lineWidth = .8; ctx.stroke();
        }
        ctx.fillStyle = '#584d3c'; ctx.font = '18px sans-serif';
        ctx.fillText('Amber = torso / head', panel * 500 + 25, 590);
        ctx.fillText('Cream / peach = separated front paws', panel * 500 + 25, 620);
        ctx.fillText('Light / dark green = relaxed hind paws', panel * 500 + 25, 650);
    });
    const output = path.join(root, 'art/giant-cat-mount/rest-pose-diagnostic.png');
    fs.writeFileSync(output, canvas.toBuffer('image/png')); console.log(`pose diagnostic: ${output}`);
}
