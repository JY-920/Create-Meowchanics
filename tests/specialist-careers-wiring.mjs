import assert from 'node:assert/strict';
import fs from 'node:fs';
import {createHash} from 'node:crypto';
const root=new URL('../',import.meta.url);
const bytes=p=>fs.readFileSync(new URL(p,root));
const read=p=>bytes(p).toString('utf8').replaceAll('\r\n','\n');
const json=p=>JSON.parse(read(p));
const manifest=json('art/specialist-suits-v1/manifest.json');
let checks=0;
const check=(ok,msg)=>{checks++;assert.ok(ok,msg);};
const original=['none','terminator','fishing','flight','fire','honey','transport','dynamite','engineering','medical','music'];
for(const port of ['forge-1.20.1','neoforge-1.21.1']) {
 const java=p=>read(port+'/src/main/java/cn/laowu/mod/'+p+'.java');
 const resources=port+'/src/main/resources/';
 const assets=resources+'assets/laowu/';
 const outfits=java('CatOutfitType'),registry=java('LaoWuMod');
 const ids=[...outfits.matchAll(/^\s+[A-Z]+\("([a-z]+)"\)/gm)].map(m=>m[1]);
 check(JSON.stringify(ids)===JSON.stringify([...original,...manifest.careers.map(c=>c.id)]),port+' stable old ordinals and append-only identities');
 check(/isPreviewOnly\(\)\s*\{\s*return false;/.test(outfits),port+' all thirteen careers active');
 check(java('client/CareerSuitTooltip').includes('outfit.isPreviewOnly()'),port+' clear preview-only descriptions');
 check(java('ServerConfig').includes('!outfit.isPreviewOnly()'),port+' exclude unimplemented suit settings');
 check(java('CareerCatBehavior').includes('outfit.isPreviewOnly()'),port+' no inherited career combat');
 check(java('CatCombatControl').includes('!CatClothesData.getOutfit(cat).isPreviewOnly()'),port+' no preview attack orders');
 const render=java('client/HissingCatModel');
 check(render.includes('diving ? 2.25F')&&render.includes('diving ? 17.5F')&&render.includes('diving ? 1.25F'),port+' authored diving body pivot');
 check(java('client/TerminatorPancakeModel').includes('CatOutfitType.DIVING ? 17.5F'),port+' diving pancake pivot');
 const filters=java('item/CatFilterRules'),definition=java('client/CatOutfitModels');
 const tips=port.startsWith('forge')?registry:java('client/ClientModEvents');
 for(const career of manifest.careers) {
  const id=career.id,symbol=id.toUpperCase();
  check(registry.includes('ITEMS.register("'+id+'_suit"')&&registry.includes('CatOutfitType.'+symbol),port+' usable '+id+' suit');
  check(registry.includes('INCOMPLETE_'+symbol+'_SUIT = ITEMS.register('),port+' incomplete component registered '+id);
  check(registry.includes('output.accept('+symbol+'_SUIT.get())')&&!registry.includes('output.accept(INCOMPLETE_'+symbol+'_SUIT.get())'),port+' complete creative entry, hidden component '+id);
  check(tips.includes('registerCareerSuitDescription('+(port.startsWith('forge')?'':'LaoWuMod.')+symbol+'_SUIT.get())'),port+' tooltip integration '+id);
  check(filters.includes(symbol+'("'+id+'"'),port+' career filter '+id);
  check(definition.includes('case '+symbol+' ->')&&definition.includes('definition("'+id+'_suit"'),port+' model selection '+id);
  for(const file of career.assets) {
   check(createHash('sha256').update(bytes(assets+file.path)).digest('hex')===file.sha256,port+' supplied asset '+file.path);
  }
  const model=json(assets+'models/entity/'+id+'_suit.bbmodel');
  check(model.textures.length===2,port+' native-cat and suit texture layers '+id);
  check(model.elements.some(e=>Object.values(e.faces??{}).some(f=>f.texture===1)),port+' actual suit faces '+id);
  for(const name of [id+'_suit','incomplete_'+id+'_suit']) {
   const item=json(assets+'models/item/'+name+'.json');
   check(item.parent==='minecraft:item/generated'&&item.textures.layer0==='laowu:item/'+name,port+' item icon mapping '+name);
  }
  const dir=port.startsWith('forge')?'recipes':'recipe';
  for(const mode of ['item_application','shearing']) {
   const recipe=json(resources+'data/laowu/'+dir+'/'+id+'_suit_'+mode+'.json');
   check(recipe.type==='create:item_application',port+' Create application '+id+' '+mode);
   check(recipe.ingredients[0].outfit===(mode==='shearing'?id:'none'),port+' recipe outfit predicate '+id+' '+mode);
  }
  for(const lang of ['zh_cn','en_us']) {
   const l=json(assets+'lang/'+lang+'.json');
   for(const key of ['item.laowu.'+id+'_suit','item.laowu.incomplete_'+id+'_suit','gui.laowu.cat_filter.career.'+id,'item.laowu.cat_pancake.prefix.'+id])
    check(typeof l[key]==='string'&&l[key].length>0,port+' '+lang+' '+key);
   const summary=l['item.laowu.'+id+'_suit.tooltip.summary'];
   check(typeof summary==='string'&&!/预览|preview/i.test(summary)&&!/27/.test(summary),port+' implemented ability summary '+id+' '+lang);
  }
 }
}
console.log('PASS: '+checks+' specialist identities, source assets, active abilities and dual-port wiring checks');
