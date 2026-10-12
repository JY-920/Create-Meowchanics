import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {test} from 'node:test';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const suits={
  terminator:'minecraft:iron_block',fishing:'minecraft:fishing_rod',flight:'minecraft:phantom_membrane',
  fire:'minecraft:blaze_rod',honey:'minecraft:honey_block',transport:'minecraft:chest',dynamite:'minecraft:tnt',
  engineering:'laowu:cat_engineer_goggles',medical:'minecraft:white_wool',music:'minecraft:note_block',
  agent:'minecraft:tinted_glass',diving:'minecraft:glass',cockroach:'minecraft:bread'
};
// Hand-checked layouts; these are the grids players see and must actually craft.
const grids={
  cat_engineer_goggles:[['','minecraft:string',''],['minecraft:glass_pane','laowu:cat_sheet','minecraft:glass_pane']],
  wish_adoption_box:[['','minecraft:paper',''],['minecraft:emerald','laowu:adoption_box','minecraft:emerald'],['','minecraft:paper','']],
  intermediate_breeding_box:[['minecraft:iron_ingot','laowu:cat_fur','minecraft:iron_ingot'],['laowu:cat_fur','laowu:basic_breeding_box','laowu:cat_fur'],['minecraft:iron_ingot','laowu:cat_fur','minecraft:iron_ingot']],
  advanced_breeding_box:[['minecraft:paper','laowu:cat_component','minecraft:paper'],['minecraft:diamond','laowu:intermediate_breeding_box','minecraft:diamond'],['minecraft:paper','','minecraft:paper']],
  cat_component:[['','laowu:cat_gear',''],['minecraft:iron_ingot','minecraft:redstone','minecraft:iron_ingot'],['','laowu:cat_spring','']],
  cat_pouch:[['minecraft:leather','','minecraft:leather'],['minecraft:string','laowu:cat_ingot','minecraft:string'],['','minecraft:leather','']],
  cat_ball:[['','laowu:cat_fur',''],['laowu:cat_fur','minecraft:slime_ball','laowu:cat_fur'],['','laowu:cat_fur','']],
  cat_laser_pointer:[['minecraft:red_stained_glass'],['laowu:cat_ingot'],['minecraft:redstone']],
  material_debug_wand:[['minecraft:amethyst_shard'],['laowu:cat_fur'],['minecraft:stick']],
  golden_cat_can:[['minecraft:gold_ingot','laowu:cat_can','minecraft:gold_ingot']],
  golden_dried_fish:[['','minecraft:gold_ingot',''],['minecraft:gold_ingot','laowu:dried_fish','minecraft:gold_ingot'],['','minecraft:gold_ingot','']],
  mutation_cat_food:[['laowu:breeding_cat_food','laowu:breeding_cat_food','laowu:breeding_cat_food'],['laowu:breeding_cat_food','minecraft:poisonous_potato','laowu:breeding_cat_food'],['laowu:breeding_cat_food','laowu:breeding_cat_food','laowu:breeding_cat_food']]
};
for(const [name,material] of Object.entries(suits))grids[name+'_suit']=[['','laowu:cat_fur',''],[material,'laowu:cat_component',material],['','laowu:cat_fur','']];

for(const [port,folder] of [['forge-1.20.1','recipes'],['neoforge-1.21.1','recipe']]) {
  const read=name=>JSON.parse(fs.readFileSync(path.join(root,port,'src/main/resources/data/laowu',folder,'standalone',name+'.json'),'utf8'));
  test(port+': ingot requires exactly one added sugar without honey',()=>{
    const recipe=read('cat_ingot');
    assert.deepEqual(recipe.ingredients.map(i=>i.item).sort(),['laowu:cat_fur','minecraft:iron_ingot','minecraft:sugar']);
  });
  for(const [name,grid] of Object.entries(grids))test(port+': '+name+' has the intended symmetric craft',()=>{
    const recipe=read(name);
    assert.equal(recipe.type,'minecraft:crafting_shaped');
    const actual=recipe.pattern.map(row=>[...row].map(symbol=>symbol===' '?'':recipe.key[symbol]?.item||'#'+recipe.key[symbol]?.tag));
    assert.deepEqual(actual,grid);
    assert.equal(recipe.result.count||1,name==='mutation_cat_food'?8:1);
    for(const row of actual)assert.deepEqual(row,[...row].reverse(),'materials mirror around the centre column');
    assert.equal(recipe.pattern.length<=3&&recipe.pattern.every(row=>row.length<=3),true,'fits the vanilla crafting grid');
  });
}
