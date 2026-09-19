import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8').replaceAll('\r\n','\n');
const bytes=p=>fs.readFileSync(path.join(root,p));
const json=p=>JSON.parse(read(p));
let checks=0;
const check=(v,msg)=>{checks++;assert.ok(v,msg)};
const specs=json('art/accessories-redrawn-v2/manifest.json').items;
check(specs.length===16 && new Set(specs.map(entry=>entry.id)).size===16,'16 uniquely mapped redrawn sprites');
check(JSON.stringify(specs.map(entry=>entry.id).sort())===JSON.stringify(
  json('art/accessories-draft-v1/manifest.json').items.map(entry=>entry.id).sort()),'all legacy accessory IDs preserved');
let referenceModels;
for(const loader of ['forge-1.20.1','neoforge-1.21.1']){
  const base=loader+'/src/main/';
  const java=n=>read(base+'java/cn/laowu/mod/'+n+'.java');
  const r=base+'resources/';
  const recipeDir=loader.startsWith('forge')?'recipes':'recipe';
  for(const entry of specs){
    const texture=bytes(r+'assets/laowu/textures/item/'+entry.id+'.png');
    check(texture.readUInt32BE(16)===16 && texture.readUInt32BE(20)===16,entry.id+' exact 16x16');
    check(createHash('sha256').update(texture).digest('hex')===entry.sha256,entry.id+' exact supplied artist pixels');
    check(json(r+'assets/laowu/lang/zh_cn.json')['item.laowu.'+entry.id]===entry.name,entry.id+' short name');
  }
  const outfit=java('CatOutfitType');
  const registry=java('LaoWuMod');
  const accessoryTab=registry.slice(registry.indexOf(' CAT_ACCESSORIES_TAB ='),registry.indexOf(' CAT_PACKAGE_MENU ='));
  const progressionTab=registry.slice(registry.indexOf(' CAT_PROGRESSION_TAB ='),registry.indexOf(' CAT_ACCESSORIES_TAB ='));
  check(accessoryTab.includes('register("cat_accessories"'),'dedicated accessory tab registered');
  check(accessoryTab.includes('CatAccessoryItems::tabIcon'),'accessory tab uses the redrawn bell icon');
  check(accessoryTab.includes('CatAccessoryItems.display(output)'),'all accessories in their own tab');
  check(!progressionTab.includes('CatAccessoryItems'),'no accessories duplicated in progression tab');
  check((registry.match(/CatAccessoryItems\.display\(output\)/g)??[]).length===1,'single creative insertion site');
  for(const lang of ['zh_cn','en_us']) {
    const messages=json(r+'assets/laowu/lang/'+lang+'.json');
    check(messages['itemGroup.laowu.cat_accessories']===(lang==='zh_cn'?'猫咪饰品':'Cat Accessories'),'localized accessory tab');
    for(const entry of specs.filter(entry=>entry.id.endsWith('_badge')))
      check(messages['item.laowu.'+entry.id].endsWith(lang==='zh_cn'?'挂饰':'Pendant'),'six attribute pendants renamed');
  }
  const tooltip=java('accessory/CatAccessoryTooltip');
  check(tooltip.indexOf('appendDetails(stack, def, details)')<tooltip.indexOf('details.add(marker)'),'effect block precedes accessory label');
  check(tooltip.includes('lines.addAll(afterName, details)'),'insert complete block before technical footers');
  check(tooltip.includes('lines.subList(afterName, lines.size()).removeIf'),'deduplicate existing accessory label without touching title');
  check(outfit.indexOf('ENGINEERING("engineering")')>outfit.indexOf('DYNAMITE("dynamite")'),'append enums; no shifted legacy network IDs');
  check(java('ServerConfig').includes('!outfit.isPreviewOnly()'),'no premature server config');
  check(java('CatSuitSetting').includes('!outfit.isPreviewOnly()'),'no suit setting UI');
  check(java('CareerCatBehavior').includes('if (outfit.isPreviewOnly())'),'no career combat AI installation');
  check(java('CareerCatBehavior').includes('&& !outfit.isPreviewOnly() && !isResting(cat)'),'combat explicitly disabled');
  check(java('client/CareerSuitTooltip').includes('if (outfit.isPreviewOnly())'),'no misleading Shift/Ctrl formulas');
  check(java('CatLaserCommands').includes('CatClothesData.getOutfit(cat).isPreviewOnly()'),'attack orders excluded');
  const models=[];
  for(const id of ['engineering','medical','music']){
    const modelPath=r+'assets/laowu/models/entity/'+id+'_suit.bbmodel';
    const model=json(modelPath);
    check(model.textures.length===2 && model.textures[1].uv_width===32, id+' suit texture index');
    const groups=new Map(model.groups.map(g=>[g.uuid,g]));
    const rootGroup=model.groups.find(g=>g.name==='group');
    check(JSON.stringify(rootGroup.origin)==='[0,5.4,-9.5]',id+' measured body root');
    check(java('client/HissingCatModel').includes('outfit.hasImportedModel() ? 18.6F'),id+' live body pivot Y');
    check(java('client/TerminatorPancakeModel').includes('outfit.hasImportedModel() ? 18.6F'),id+' pancake body pivot Y');
    check(model.outliner.some(n=>groups.get(n.uuid)?.name==='head'),id+' head group retains hat/headphones');
    const faces=model.elements.flatMap(e=>Object.values(e.faces));
    check(faces.some(f=>f.texture===1),id+' actual outfit faces');
    check(faces.every(f=>f.texture===0 || f.texture===1 || f.texture===null),id+' no unmapped texture');
    check(java('client/CatOutfitModels').includes('definition("'+id+'_suit", false, null)'),id+' renderer mapping');
    for(const prefix of ['','incomplete_']){
      const name=prefix+id+'_suit';
      check(java('LaoWuMod').includes('"'+name+'"'),name+' item registration');
      check(java('LaoWuMod').includes('output.accept('+name.toUpperCase()+'.get());') === (prefix===''),name+' only completed suits in creative tab');
      check(json(r+'assets/laowu/models/item/'+name+'.json').textures.layer0==='laowu:item/'+name,name+' correct supplied inventory icon');
    }
    for(const step of ['item_application','shearing']){
      const recipe=json(r+'data/laowu/'+recipeDir+'/'+id+'_suit_'+step+'.json');
      check(recipe.type==='create:item_application',id+' application/removal JEI');
      if(step==='item_application') {
        const data=recipe.results[0].nbt??recipe.results[0].components['minecraft:custom_data'];
        check(data.LaoWuCatOutfit===id,id+' loader-specific outfit data');
      }
    }
    for(const lang of ['zh_cn','en_us']){
      const messages=json(r+'assets/laowu/lang/'+lang+'.json');
      for(const key of ['item.laowu.'+id+'_suit','item.laowu.incomplete_'+id+'_suit','gui.laowu.cat_filter.career.'+id,'item.laowu.cat_pancake.prefix.'+id])
        check(!!messages[key],id+' localized '+key);
    }
    check(java('item/CatFilterRules').includes(id.toUpperCase()+'("'+id+'", CatOutfitType.'+id.toUpperCase()+')'),id+' filter option');
    models.push(read(modelPath));
  }
  if(referenceModels)check(JSON.stringify(models)===referenceModels,'identical imported models across loaders');
  referenceModels=JSON.stringify(models);
  check(java('entity/ButterCatBoss').includes('if (random.nextFloat() < 0.25F)'),'25% independent boss reward');
  check(java('entity/ButterCatBoss').includes('Mth.nextInt(random, 1, 3)'),'existing 1-3 reward range kept');
  check(java('accessory/CatAccessoryRarity').includes('Rarity.EPIC'),'boss rarity');
  const effects=json(r+'data/laowu/cat_accessories/cat_butter_cube.json').effects;
  check(effects.speed===10 && effects.moving_damage_reduction===25,'boss effect profile');
  check(!fs.existsSync(path.join(root,r+'data/laowu/'+recipeDir+'/accessory/cat_butter_cube.json')),'no boss crafting shortcut');
  const mechanic=java('accessory/CatAccessories');
  check(mechanic.includes('horizontalDistanceSqr() <= 1.0E-4D'),'ignore standing gravity');
  check(mechanic.includes('!(source.getEntity() instanceof LivingEntity)'),'no environmental mitigation');
  check(java('accessory/CatAccessoryEvents').includes('CatAccessories.mitigateMovingDamage'),'actual damage adapter');
}
console.log('PASS: '+checks+' accessory sprite, preview-career and boss wiring checks');
