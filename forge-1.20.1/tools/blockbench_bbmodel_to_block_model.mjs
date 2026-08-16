import fs from 'node:fs';

const [inputPath, outputPath, texture] = process.argv.slice(2);
if (!inputPath || !outputPath || !texture) {
  throw new Error('usage: node blockbench_bbmodel_to_block_model.mjs <model.bbmodel> <model.json> <texture>');
}

const source = JSON.parse(fs.readFileSync(inputPath, 'utf8'));
if (!Array.isArray(source.elements) || !source.resolution) {
  throw new Error('input is not a cube-based Blockbench bbmodel');
}

const textureWidth = source.resolution.width;
const textureHeight = source.resolution.height;
const directions = ['north', 'east', 'south', 'west', 'up', 'down'];
const y180Direction = {
  north: 'south', east: 'west', south: 'north', west: 'east', up: 'up', down: 'down'
};
const clean = value => Math.abs(value) < 1e-9 ? 0 : Number(value.toFixed(6));
const point = values => [clean(values[0] + 8), clean(values[1]), clean(values[2] + 8)];

const elements = [];
for (const cube of source.elements) {
  if (cube.type !== 'cube' || cube.export === false) continue;

  let from = point(cube.from);
  let to = point(cube.to);
  let faceRemap = Object.fromEntries(directions.map(direction => [direction, direction]));
  let elementRotation;
  let bakedY180 = false;
  const rotation = cube.rotation ?? [0, 0, 0];
  const pivot = cube.origin ?? [0, 0, 0];

  if (Math.abs(rotation[1]) === 180 && rotation[0] === 0 && rotation[2] === 0) {
    const x1 = 2 * pivot[0] - cube.from[0];
    const x2 = 2 * pivot[0] - cube.to[0];
    const z1 = 2 * pivot[2] - cube.from[2];
    const z2 = 2 * pivot[2] - cube.to[2];
    from = point([Math.min(x1, x2), cube.from[1], Math.min(z1, z2)]);
    to = point([Math.max(x1, x2), cube.to[1], Math.max(z1, z2)]);
    faceRemap = y180Direction;
    bakedY180 = true;
  } else {
    const activeAxes = rotation
      .map((angle, index) => ({angle, axis: ['x', 'y', 'z'][index]}))
      .filter(value => value.angle !== 0);
    if (activeAxes.length === 1) {
      if (![ -45, -22.5, 22.5, 45 ].includes(activeAxes[0].angle)) {
        throw new Error(`unsupported vanilla element angle: ${activeAxes[0].angle}`);
      }
      elementRotation = {
        origin: point(pivot),
        axis: activeAxes[0].axis,
        angle: activeAxes[0].angle,
        rescale: false
      };
    } else if (activeAxes.length > 1) {
      throw new Error(`multi-axis cube rotation is unsupported: ${rotation}`);
    }
  }

  const faces = {};
  for (const direction of directions) {
    const sourceDirection = faceRemap[direction];
    const sourceFace = cube.faces?.[sourceDirection];
    if (!sourceFace || sourceFace.texture === null || sourceFace.enabled === false) continue;
    const [u1, v1, u2, v2] = sourceFace.uv;
    const face = {
      uv: [
        clean(u1 * 16 / textureWidth),
        clean(v1 * 16 / textureHeight),
        clean(u2 * 16 / textureWidth),
        clean(v2 * 16 / textureHeight)
      ],
      texture: '#texture'
    };
    const originalRotation = sourceFace.rotation ?? 0;
    const bakedRotation = bakedY180 && (direction === 'up' || direction === 'down') ? 180 : 0;
    const faceRotation = (originalRotation + bakedRotation) % 360;
    if (faceRotation) face.rotation = faceRotation;
    faces[direction] = face;
  }

  const element = {from, to, faces};
  if (elementRotation) element.rotation = elementRotation;
  elements.push(element);
}

const result = {
  credit: 'Converted deterministically from the supplied Blockbench bbmodel.',
  parent: 'minecraft:block/block',
  ambientocclusion: false,
  textures: {texture, particle: texture},
  elements
};
fs.writeFileSync(outputPath, JSON.stringify(result, null, 2) + '\n', 'utf8');
