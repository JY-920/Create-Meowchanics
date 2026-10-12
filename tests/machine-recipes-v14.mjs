import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const recipes={
  cat_editor:{pattern:[' B ','III'],key:{B:'create:empty_schematic',I:'laowu:cat_ingot'}},
  cat_deployment_platform:{pattern:['I','C'],key:{I:'minecraft:iron_bars',C:'laowu:cat_casing'}},
  cat_ejecting_deployment_platform:{pattern:['P','I','C'],key:{P:'create:cardboard',I:'minecraft:iron_bars',C:'laowu:cat_casing'}},
  cat_auto_laser:{pattern:['L','B','G'],key:{L:'laowu:cat_laser_pointer',B:'laowu:cat_block',G:'create:cogwheel'}},
  creature_transmitter:{pattern:['T','C','R'],key:{T:'create:electron_tube',C:'laowu:cat_casing',R:'minecraft:comparator'}}
};
for(const [loader,folder,condition] of [
  ['forge-1.20.1','recipes','forge:mod_loaded'],
  ['neoforge-1.21.1','recipe','neoforge:mod_loaded']]){
  for(const [id,expected] of Object.entries(recipes)){
    const file=path.join(root,loader,'src/main/resources/data/laowu',folder,id+'.json');
    const json=JSON.parse(readFileSync(file,'utf8'));
    assert.equal(json.type,'minecraft:crafting_shaped',`${loader}/${id}: crafting type`);
    assert.deepEqual(json.pattern,expected.pattern,`${loader}/${id}: visual layout`);
    for(const [symbol,item] of Object.entries(expected.key))assert.equal(json.key[symbol]?.item,item,`${loader}/${id}: ${symbol}`);
    assert.equal(json.result.item??json.result.id,'laowu:'+id,`${loader}/${id}: output`);
    assert.deepEqual(json[loader.startsWith('forge')?'conditions':'neoforge:conditions'],[{type:condition,modid:'create'}],`${loader}/${id}: Create-only`);
  }
}
console.log('10 Create-only machine recipes match the requested layouts.');
