import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const stats=['attack','health','speed','stamina','intelligence','luck'];
const expected=[
  'cat_ingot','cat_sheet','cat_strip','cat_powder','cat_component','cat_dough','cat_pancake',
  'basic_breeding_box','intermediate_breeding_box','advanced_breeding_box',
  'adoption_box','wish_adoption_box','cat_storage_box','cat_pouch',
  'cat_laser_pointer','cat_engineer_goggles','material_debug_wand','cat_ball',
  'cat_food','breeding_cat_food','mutation_cat_food','pheromone_cat_food',
  'cat_can','golden_cat_can','dried_fish','golden_dried_fish',
  ...stats.flatMap(s=>[s+'_breeding_cat_food',s+'_cat_can','golden_'+s+'_cat_can']),
  ...['terminator','fishing','flight','fire','honey','transport','dynamite','engineering','medical','music','agent','diving','cockroach'].map(s=>s+'_suit'),
  'butter_bread','giant_cat_treat'
];
assert.equal(expected.length,59);
const enabled=(condition,create)=>{
  switch(condition.type.split(':')[1]) {
    case 'mod_loaded': return condition.modid==='create'&&create;
    case 'not': return !enabled(condition.value,create);
    case 'and': return condition.values.every(c=>enabled(c,create));
    case 'or': return condition.values.some(c=>enabled(c,create));
    default: throw new Error('Unsupported condition '+JSON.stringify(condition));
  }
};
const outputs=j=>[j.result,...(j.results||[])].filter(Boolean).map(o=>typeof o==='string'?o:o.id||o.item).filter(Boolean);
const counts={cat_sheet:2,cat_strip:4,cat_powder:2,cat_food:2,breeding_cat_food:2,mutation_cat_food:8};
for(const [port,folder,key] of [
  ['forge-1.20.1','recipes','conditions'],['neoforge-1.21.1','recipe','neoforge:conditions']
]) {
  const recipes=path.join(root,port,'src/main/resources/data/laowu',folder);
  const dir=path.join(recipes,'standalone');
  assert.ok(fs.existsSync(dir),port+': missing no-Create survival recipes');
  const entries=fs.readdirSync(dir).filter(f=>f.endsWith('.json')).map(f=>({file:f,j:JSON.parse(fs.readFileSync(path.join(dir,f),'utf8'))}));
  assert.equal(entries.length,60,port+': all 59 outputs plus two dried-fish inputs');
  const produced=new Set();
  for(const {file,j}of entries) {
    assert.ok(j[key]?.length,file+': must have a loader condition');
    assert.ok(j[key].every(c=>enabled(c,false)),file+': must load without Create');
    assert.ok(!j[key].every(c=>enabled(c,true)),file+': must not load with Create');
    assert.ok(!JSON.stringify(j).includes('"create:'),file+': cannot require Create serializers or ingredients');
    for(const idof of outputs(j)) {
      produced.add(idof.slice(6));
      assert.equal(j.result?.count||1,counts[idof.slice(6)]||1,file+': expected batch size');
    }
    if(j.type==='laowu:pheromone_cat_food_crafting') produced.add('pheromone_cat_food');
  }
  assert.deepEqual([...produced].sort(),[...expected].sort(),port+': survival recipe coverage');
  const ingot=entries.find(e=>e.file==='cat_ingot.json').j;
  assert.deepEqual(ingot.ingredients.map(i=>i.item).sort(),['laowu:cat_fur','minecraft:iron_ingot','minecraft:sugar']);
  const strip=entries.find(e=>e.file==='cat_strip.json').j;
  assert.equal(strip.pattern.join(''),'II');
  assert.equal(strip.result.count,4,'two ingots make four strips without colliding with one-ingot sheets');
  const wish=entries.find(e=>e.file==='wish_adoption_box.json').j;
  assert.equal(wish.type,'minecraft:crafting_shaped','standalone wish recipe uses the vanilla shaped serializer, not the Create crafting inventory');
  const families={attack:'minecraft:blaze_powder',health:'minecraft:beetroot',speed:'minecraft:sugar',stamina:'minecraft:milk_bucket',intelligence:'minecraft:egg',luck:'minecraft:golden_carrot'};
  for(const [stat,ingredient]of Object.entries(families))for(const suffix of [stat+'_cat_can','golden_'+stat+'_cat_can',stat+'_breeding_cat_food']) {
    const j=entries.find(e=>e.file===suffix+'.json').j;
    assert.equal(j.ingredients.at(-1).item,ingredient,suffix+': correct stat ingredient');
  }
  // Prove the first ingot is obtainable, rather than treating block/nugget loops as a source.
  const active=[...fs.readdirSync(recipes).filter(f=>f.endsWith('.json')).map(f=>JSON.parse(fs.readFileSync(path.join(recipes,f),'utf8'))),...entries.map(e=>e.j)]
    .filter(j=>(j[key]||[]).every(c=>enabled(c,false)));
  const reachable=new Set(['laowu:cat_fur']);
  const ready=i=>Array.isArray(i)?i.some(ready):i.tag||i.item?.startsWith('minecraft:')||reachable.has(i.item);
  for(let step=0;step<active.length;step++)for(const j of active) {
    const ingredients=j.ingredients||(j.ingredient?[j.ingredient]:Object.values(j.key||{}));
    const inputs=j.type==='minecraft:smithing_transform'?[j.template,j.base,j.addition]:ingredients;
    if(inputs.length&&inputs.every(ready))outputs(j).forEach(o=>reachable.add(o));
  }
  for(const idof of ['cat_ingot','cat_component','cat_dough','cat_pancake','cat_scanner','cat_totem','cat_helmet','cat_chestplate','cat_leggings','cat_boots','cat_sword','cat_pickaxe','cat_axe','cat_shovel','cat_hoe','advanced_breeding_box'])
    assert.ok(reachable.has('laowu:'+idof),port+': no circular-only acquisition for '+idof);
  console.log('PASS: '+port+' 60 Create-absent recipes, batch sizes, food mapping and survival acquisition graph');
}
