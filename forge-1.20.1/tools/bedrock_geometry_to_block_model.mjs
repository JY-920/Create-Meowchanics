import fs from 'node:fs';

const [inputPath, outputPath, texture] = process.argv.slice(2);
if (!inputPath || !outputPath || !texture) {
  throw new Error('usage: node bedrock_geometry_to_block_model.mjs <geo.json> <model.json> <texture>');
}

const source = JSON.parse(fs.readFileSync(inputPath, 'utf8'));
const geometry = source['minecraft:geometry'][0];
const textureWidth = geometry.description.texture_width;
const textureHeight = geometry.description.texture_height;
const directions = ['north', 'east', 'south', 'west', 'up', 'down'];
const y180Direction = {
  north: 'south', east: 'west', south: 'north', west: 'east', up: 'up', down: 'down'
};

const clean = value => Math.abs(value) < 1e-9 ? 0 : Number(value.toFixed(6));
const point = values => [clean(values[0] + 8), clean(values[1]), clean(values[2] + 8)];

const elements = [];
for (const bone of geometry.bones) {
  for (const cube of bone.cubes ?? []) {
    const origin = cube.origin;
    const size = cube.size;
    let from = point(origin);
    let to = point([origin[0] + size[0], origin[1] + size[1], origin[2] + size[2]]);
    let faceRemap = Object.fromEntries(directions.map(direction => [direction, direction]));
    let uvRotation = 0;
    let elementRotation;

    const rotation = cube.rotation ?? [0, 0, 0];
    const pivot = cube.pivot ?? [0, 0, 0];
    if (Math.abs(rotation[1]) === 180 && rotation[0] === 0 && rotation[2] === 0) {
      const x1 = 2 * pivot[0] - origin[0];
      const x2 = 2 * pivot[0] - (origin[0] + size[0]);
      const z1 = 2 * pivot[2] - origin[2];
      const z2 = 2 * pivot[2] - (origin[2] + size[2]);
      from = point([Math.min(x1, x2), origin[1], Math.min(z1, z2)]);
      to = point([Math.max(x1, x2), origin[1] + size[1], Math.max(z1, z2)]);
      faceRemap = y180Direction;
      uvRotation = 180;
    } else {
      const activeAxes = rotation
        .map((angle, index) => ({angle, axis: ['x', 'y', 'z'][index]}))
        .filter(value => value.angle !== 0);
      if (activeAxes.length === 1) {
        elementRotation = {
          origin: point(pivot),
          axis: activeAxes[0].axis,
          angle: activeAxes[0].angle,
          rescale: false
        };
      } else if (activeAxes.length > 1) {
        throw new Error(`cube rotation cannot be represented by a vanilla block model: ${rotation}`);
      }
    }

    const faces = {};
    for (const direction of directions) {
      const sourceFace = cube.uv?.[faceRemap[direction]];
      if (!sourceFace) continue;
      const [u, v] = sourceFace.uv;
      const [width, height] = sourceFace.uv_size;
      faces[direction] = {
        uv: [
          clean(u * 16 / textureWidth),
          clean(v * 16 / textureHeight),
          clean((u + width) * 16 / textureWidth),
          clean((v + height) * 16 / textureHeight)
        ],
        texture: '#texture'
      };
      if (uvRotation) faces[direction].rotation = uvRotation;
    }

    const element = {from, to, faces};
    if (elementRotation) element.rotation = elementRotation;
    elements.push(element);
  }
}

const result = {
  credit: 'Converted deterministically from the supplied Blockbench Bedrock model.',
  parent: 'minecraft:block/block',
  ambientocclusion: false,
  textures: {texture, particle: texture},
  elements
};
fs.writeFileSync(outputPath, JSON.stringify(result, null, 2) + '\n', 'utf8');
