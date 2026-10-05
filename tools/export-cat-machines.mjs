import assert from 'node:assert/strict';
import {readFileSync, writeFileSync, mkdirSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const loaders = ['forge-1.20.1', 'neoforge-1.21.1'];

/** Export the authored Java-block geometry without normalizing inverted cube bounds.
 * In particular the Basin Interior's negative Y extent is intentional: it reverses
 * the wall winding, exactly as Create's own basin/block model does.
 */
export function exportBasin(source, directional) {
  const groups = new Map(source.groups.map(group => [group.uuid, group]));
  const included = new Set();
  function collect(nodes) {
    for (const node of nodes) {
      if (typeof node === 'string') { included.add(node); continue; }
      const group = groups.get(node.uuid);
      if (group?.export === false || (!directional && group?.name === 'Spoutput')) continue;
      assert(!group?.rotation?.some(value => value !== 0), 'group rotations must be baked before export');
      collect(node.children ?? []);
    }
  }
  collect(source.outliner);
  const elements = source.elements.filter(element => element.export !== false && included.has(element.uuid)).map(element => {
    const result = {name: element.name, from: [...element.from], to: [...element.to], faces: {}};
    if (element.shade === false) result.shade = false;
    const axes = (element.rotation ?? [0, 0, 0]).map((angle, index) => ({angle, index})).filter(axis => axis.angle !== 0);
    assert(axes.length <= 1, 'Java block elements support only a single axis rotation');
    if (axes.length) {
      const {angle, index} = axes[0];
      assert([-45, -22.5, 22.5, 45].includes(angle), 'unsupported Java block rotation angle');
      result.rotation = {angle, axis: 'xyz'[index], origin: [...element.origin], rescale: element.rescale === true};
    }
    for (const [direction, face] of Object.entries(element.faces)) {
      if (face.texture === null || face.texture === undefined) continue;
      assert(face.texture === 0 || face.texture === 1, 'unexpected basin texture index');
      result.faces[direction] = {
        uv: face.uv.map((value, index) => value * 16 / (index % 2 ? source.resolution.height : source.resolution.width)),
        texture: `#${face.texture}`,
      };
      if (face.rotation) result.faces[direction].rotation = face.rotation;
      if (face.cullface) result.faces[direction].cullface = face.cullface;
      if (face.tint !== undefined && face.tint >= 0) result.faces[direction].tintindex = face.tint;
    }
    return result;
  });
  return {
    parent: 'minecraft:block/block', ambientocclusion: source.ambientocclusion !== false,
    textures: {'0': 'laowu:block/haji_basin', '1': 'laowu:block/haji_basin_spout', particle: 'laowu:block/haji_basin'},
    elements,
  };
}

function buildAssets(source) {
  const assets = new Map();
  const add = (name, value) => assets.set(name, Buffer.from(JSON.stringify(value, null, 2) + '\n'));
  const casing = 'laowu:block/cat_casing';
  add('models/block/cat_casing.json', {parent: 'minecraft:block/cube_all', textures: {all: casing}});
  add('models/item/cat_casing.json', {parent: 'laowu:block/cat_casing'});
  add('blockstates/cat_casing.json', {variants: {'': {model: 'laowu:block/cat_casing'}}});
  const rotations = {x: {x: 90, y: 90}, y: {}, z: {x: 90, y: 180}};
  const shaftTextures = {casing, opening: 'laowu:block/cat_casing_shaft_opening'};
  add('models/block/cat_encased_shaft.json', {parent: 'create:block/encased_shaft/block', textures: shaftTextures});
  add('models/item/cat_encased_shaft.json', {parent: 'create:block/encased_shaft/item', textures: shaftTextures});
  add('blockstates/cat_encased_shaft.json', {variants: Object.fromEntries(Object.entries(rotations).map(([axis, rotation]) =>
    [`axis=${axis}`, {model: 'laowu:block/cat_encased_shaft', uvlock: true, ...rotation}]))});
  for (const size of ['', '_large']) {
    const id = `cat_encased${size}_cogwheel`;
    // User-authored PNGs retain Create's original slot alpha and CT tile layout.
    const textures = {'1': 'minecraft:block/stripped_spruce_log_top', '4': 'laowu:block/cat_casing_shaft_opening', casing, particle: casing,
      side: `laowu:block/cat_encased_cogwheel_side${size ? '_connected' : ''}`};
    const variants = {};
    for (const top of [false, true]) for (const bottom of [false, true]) {
      const suffix = `${top ? '_top' : ''}${bottom ? '_bottom' : ''}`;
      add(`models/block/${id}${suffix}.json`, {parent: `create:block/encased${size}_cogwheel/block${suffix}`, textures});
      for (const [axis, rotation] of Object.entries(rotations)) {
        variants[`axis=${axis},bottom_shaft=${bottom},top_shaft=${top}`] = {model: `laowu:block/${id}${suffix}`, ...rotation};
      }
    }
    add(`blockstates/${id}.json`, {variants});
    add(`models/item/${id}.json`, {parent: `create:block/encased${size}_cogwheel/item`,
      textures:size?{...textures,'4':'create:block/large_cogwheel'}:textures});
  }
  add('models/block/haji_basin.json', exportBasin(source, false));
  add('models/block/haji_basin_directional.json', exportBasin(source, true));
  add('models/item/haji_basin.json', {parent: 'laowu:block/haji_basin'});
  add('blockstates/haji_basin.json', {variants: {
    'facing=down': {model: 'laowu:block/haji_basin'},
    'facing=east': {model: 'laowu:block/haji_basin_directional', y: 270},
    'facing=north': {model: 'laowu:block/haji_basin_directional', y: 180},
    'facing=south': {model: 'laowu:block/haji_basin_directional'},
    'facing=west': {model: 'laowu:block/haji_basin_directional', y: 90},
  }});
  return assets;
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  // External artist sources remain untouched. Override their root with --source <directory>.
  const sourceIndex = process.argv.indexOf('--source');
  const sourceRoot = sourceIndex < 0 ? path.join(root, 'art/cat-machines') : process.argv[sourceIndex + 1];
  assert(sourceRoot, '--source requires a directory');
  const basinRoot = path.join(sourceRoot, '哈基机器/哈基工作盆');
  const assets = buildAssets(JSON.parse(readFileSync(path.join(basinRoot, 'basin_block_directional.bbmodel'), 'utf8')));
  for (const [name, file] of [
    ['cat_casing', path.join(sourceRoot, '猫机壳 (1).png')],
    ['cat_casing_connected', path.join(sourceRoot, '猫机壳-纹理链接 (1).png')],
    ['cat_casing_shaft_opening', path.join(sourceRoot, 'encased-user-20260926/gearbox.png')],
    ['cat_encased_cogwheel_side', path.join(sourceRoot, 'encased-user-20260926/andesite_encased_cogwheel_side.png')],
    ['cat_encased_cogwheel_side_connected', path.join(sourceRoot, 'encased-user-20260926/andesite_encased_cogwheel_side_connected.png')],
    ['haji_basin', path.join(basinRoot, '1.png')],
    ['haji_basin_spout', path.join(basinRoot, '2.png')],
  ]) assets.set(`textures/block/${name}.png`, readFileSync(file));
  for (const loader of loaders) for (const [name, bytes] of assets) {
    if(name==='blockstates/haji_basin.json')continue; // Owned by export-cat-sixway.mjs.
    const target = path.join(root, loader, 'src/main/resources/assets/laowu', name);
    if (process.argv.includes('--write')) {
      mkdirSync(path.dirname(target), {recursive: true});
      writeFileSync(target, bytes);
    } else assert.deepEqual(readFileSync(target), bytes, `${loader}/${name} differs from authored source export`);
  }
  console.log(`PASS: ${assets.size} cat machine assets per loader; original PNG bytes preserved`);
}
