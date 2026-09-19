import assert from 'node:assert/strict';
import fs from 'node:fs';
import {createHash} from 'node:crypto';
const root=new URL('../',import.meta.url);
const bytes=p=>fs.readFileSync(new URL(p,root));
const read=p=>bytes(p).toString('utf8').replaceAll('\r\n','\n');
const json=p=>JSON.parse(read(p));
let checks=0;
const check=(value,label)=>{checks++;assert.ok(value,label);};
const materials={
 engineering:['create:goggles','create:brown_toolbox'],
 medical:['minecraft:white_wool','minecraft:golden_apple'],
 music:['minecraft:light_gray_wool','minecraft:jukebox'],
 agent:['minecraft:tinted_glass','minecraft:flint'],
 diving:['create:copper_diving_helmet','create:mechanical_pump'],
 cockroach:['minecraft:bread','minecraft:sugar']
};
const images=new Map();
for(const port of ['forge-1.20.1','neoforge-1.21.1']){
 const java=n=>read(port+'/src/main/java/cn/laowu/mod/'+n+'.java');
 const res=port+'/src/main/resources/',dir=port.startsWith('forge')?'recipes':'recipe';
 const neo=dir==='recipe',registry=java('LaoWuMod');
 const names=[...java('CatOutfitType').matchAll(/^\s+[A-Z]+\("([a-z]+)"\)/gm)].map(m=>m[1]).filter(n=>n!=='none').map(n=>n+'_suit');
 names.push('cat_component','cat_grenade');
 check(names.length===15,'13 careers + two component recipes');
 check((registry.match(/new com\.simibubi\.create\.content\.processing\.sequenced\.SequencedAssemblyItem\(/g)||[]).length===15,'All intermediates use native progress/stack limit');
 let steps=0;
 for(const name of names){
  const half='incomplete_'+name,r=json(res+'data/laowu/'+dir+'/'+name+'_sequenced_assembly.json');
  check(r.type==='create:sequenced_assembly'&&r.loops===1,'One-loop Create assembly '+name);
  check((neo?r.transitional_item.id:r.transitionalItem.item)==='laowu:'+half,'Distinct transitional item '+name);
  for(const stage of r.sequence){
   check(stage.type==='create:deploying'&&stage.ingredients[0].item==='laowu:'+half,'Real deploying stage '+name);
   check(stage.results.length===1&&(neo?stage.results[0].id:stage.results[0].item)==='laowu:'+half,'Next stage preserves item '+name);steps++;
  }
  const texture=name==='cat_grenade'?'cat_shell':half;
  const icon=json(res+'assets/laowu/models/item/'+half+'.json');
  check(icon.parent==='minecraft:item/generated'&&icon.textures.layer0==='laowu:item/'+texture,'Artist sprite mapping '+half);
  const png=bytes(res+'assets/laowu/textures/item/'+texture+'.png');
  check(png.subarray(1,4).toString()==='PNG'&&png.readUInt32BE(16)>0&&png.readUInt32BE(20)>0,'Real PNG '+half);
  const digest=createHash('sha256').update(png).digest('hex');
  if(images.has(half))check(images.get(half)===digest,'Both ports use identical artwork '+half);else images.set(half,digest);
  const career=name.replace(/_suit$/,'');
  if(materials[career]){
   check(r.ingredient.item==='laowu:cat_component','New suits all start with cat component');
   check(JSON.stringify(r.sequence.map(s=>s.ingredients[1].item))===JSON.stringify(materials[career]),'Exact requested material order '+career);
   check(r.results.length===1&&(neo?r.results[0].id:r.results[0].item)==='laowu:'+name
     &&(r.results[0].chance??1)===1&&(r.results[0].count??1)===1,'Guaranteed one new suit '+career);
  }
 }
 check(steps===31,'All 31 assembly stages');
 const filter=java('item/CatFilterItem'),rules=java('item/CatFilterRules'),menu=java('CatFilterMenu');
 check(filter.includes('WishAdoptionBoxBlockEntity box')&&filter.includes('return super.useOn(context);'),'Scoped to wish box');
 check(filter.indexOf('if (level.isClientSide)')<filter.indexOf('rules.write(stack)'),'Server is sole data writer');
 check(filter.includes('List.of()')&&filter.includes('withBaseCurrent(!offer.maximum())'),'Fresh predicates, exact raw NOW semantics');
 check(filter.includes('offer.maximum() ? 0 : mask, offer.maximum() ? mask : 0, 0'),'Clears other page and logic flags');
 check(java('CommonEvents').includes('filter.useOn(')&&java('CommonEvents').includes('event.setCanceled(true)'),'Block event intercepts before opening GUI');
 check(rules.includes('filter.getBoolean("BaseCurrent")')&&rules.includes('filter.putBoolean("BaseCurrent", true)'),'Saved imported mode');
 check(rules.includes('baseCurrent ? profile.current(stat)')&&rules.includes('CatAttributeEffects.effectiveValue'),'Imported base vs ordinary effective values');
 check(menu.includes('BASE_CURRENT_INDEX')&&menu.includes('.withBaseCurrent(baseCurrent())'),'Synced menu preserves imported semantics');
 check(java('client/CatFilterScreen').includes('gui.laowu.cat_filter.page.base_current'),'UI labels raw base mode');
 for(const lang of ['zh_cn','en_us']){
  const t=json(res+'assets/laowu/lang/'+lang+'.json');
  for(const key of ['gui.laowu.cat_filter.base_current','gui.laowu.cat_filter.page.base_current','item.laowu.cat_filter.wish_import','message.laowu.cat_filter.wish_imported'])
   check(typeof t[key]==='string'&&t[key].length>0,'Localized '+key);
 }
}
console.log('PASS: '+checks+' wish-filter import wiring and 15 artist intermediates / six new assembly recipes across both ports');
