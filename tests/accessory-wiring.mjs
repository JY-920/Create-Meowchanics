import assert from 'node:assert/strict';
import {readFileSync,readdirSync,existsSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
import vm from 'node:vm';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=(p)=>readFileSync(path.join(root,p),'utf8').replaceAll('\r\n','\n');
let checks=0;
function check(value,why){checks++;assert.ok(value,why);}
let reference;
for (const loader of ['forge-1.20.1','neoforge-1.21.1']) {
  const java=n=>read(loader+'/src/main/java/cn/laowu/mod/'+n+'.java');
  const resources=loader+'/src/main/resources/';
  const defs=readdirSync(path.join(root,resources,'data/laowu/cat_accessories')).sort()
    .map(file=>[file,JSON.parse(read(resources+'data/laowu/cat_accessories/'+file))]);
  check(defs.length===36,'36 definitions per loader, including thirteen career-exclusive effects');
  const temporary=[...JSON.parse(read('art/career-accessories-v1/manifest.json')).items,...JSON.parse(read('art/general-accessories-v1/manifest.json')).items];
  if(reference) check(JSON.stringify(defs)===reference,'byte-equivalent effects across loaders');
  reference=JSON.stringify(defs);
  for(const lang of ['zh_cn','en_us']){
    const messages=JSON.parse(read(resources+'assets/laowu/lang/'+lang+'.json'));
    check(messages['cat_accessory.laowu.label']===(lang==='zh_cn'?'[猫咪饰品]':'[Cat Accessory]'),'shared marker');
    for(const [file,def] of defs){
      const id=file.replace('.json','');
      check(!!messages['item.laowu.'+id],id+' name');
      for(const effect of Object.keys(def.effects)){
        const suffix=effect==='aggro_bias'?(def.effects[effect]>0?'.positive':'.negative'):'';
        check(!!messages['cat_accessory.laowu.effect.'+effect+suffix],effect+' tooltip');
      }
      const model=JSON.parse(read(resources+'assets/laowu/models/item/'+file));
      const placeholder=temporary.find(entry=>entry.id===id&&!entry.source);
      check(model.parent==='minecraft:item/generated' && model.textures.layer0===(placeholder?'minecraft:item/'+placeholder.icon:'laowu:item/'+id),'original or specified vanilla placeholder model');
      check(!existsSync(path.join(root,resources,'data/laowu',loader.startsWith('forge')?'recipes':'recipe','accessory',file)),id+' has no crafting recipe');
    }
  }
  const menu=java('CatProfileMenu'), inventory=java('CatProfileContainer'), effects=java('accessory/CatAccessories');
  check(menu.includes('CatAccessories.mayEquip') && menu.includes('getMaxStackSize() {\n                    return 1;'),'server-backed one-item slot guard');
  check(menu.includes('moveItemStackTo(stack, 0, CatProfileData.ACCESSORY_SLOTS, false)'), 'quick equip route');
  check(inventory.includes('putByte("Slot", (byte) slot)') && inventory.includes('CatAccessories.equipmentChanged(cat)'), 'slot persistence and removal refresh');
  check(effects.includes('slot < CatProfileData.ACCESSORY_SLOTS') && effects.includes('CatProfileData.ACCESSORY_SLOTS;'),'separate equipment and cargo ranges');
  check(effects.includes('EXTRA_STRIKE.get()') && effects.includes('EXTRA_STRIKE.remove()') && effects.includes('now + 2'),'bounded deferred follow-up with recursion guard');
  check(effects.includes('pending.size() < 4') && effects.includes('LAST_ROLL.get(cat)'),'attack and splash caps');
  check(effects.includes('target.level() != cat.level()') && effects.includes('CatTeamRules.canHarm(cat, target)'),'follow-up world/team guards');
  check(effects.includes('item.hasPickUpDelay()') && effects.includes('canPickUp(cat, item)') && effects.includes('if (!canStore(inventory, stack)) continue;'),'magnet ownership/delay/full-inventory guards');
  check(!effects.includes('item.getOwner().equals(cat.getOwnerUUID())'),'do not confuse item thrower with pickup owner');
  check(effects.includes('if (++processed > 32) break;') && effects.includes('item.setItem(remaining)'),'bounded magnet preserves remainders');
  check(java('CatProjectileDamage').includes('CatAccessories.projectileKnockback(cat)'),'normal projectile knockback opt-in');
  check(java('DynamiteCatLastStand').includes('CatAccessories.explosionMultiplier(cat, FINAL_DAMAGE_MULTIPLIER)'), '5x default / 10x accessory routing');
  check(java('entity/FishingRodProjectile').includes('KNOCKBACK_RESISTANCE'),'fishing push respects resistance');
  const attrs=java('genetics/CatAttributeEffects');
  check(attrs.includes('value + accessoryBonus') && attrs.includes('CatAccessories.statBonus(cat, stat)'),'live and GUI use identical bonuses before clamping');
  check(attrs.includes('ACCESSORY_KNOCKBACK_MODIFIER') && attrs.includes('CatAccessories.knockbackImmune(cat) ? 1.0D : 0.0D'),'resistance removed when unequipped');
  const events=java('accessory/CatAccessoryEvents');
  check(events.includes('OnDatapackSyncEvent') && events.includes('AddReloadListenerEvent'), 'login/reload registry wiring');
  check(events.includes('LivingDamageEvent'+(loader.startsWith('forge')?'':'\\.Post').replace('\\','')),'accepted damage hook');
  check(java('LaoWuMod').includes('CatAccessoryItems.register((name, factory) -> ITEMS.register(name, factory))'),'deferred registration before freeze');
  check(java('LaoWuMod').includes('CatAccessoryEvents.class'),'server events registered');
  check(java('CommonEvents').includes('CatAccessories.tick(cat)') && java('CommonEvents').includes('ModNetwork.syncCatAccessories(cat, player'),'tick and tracking state');
  check(java('client/CatScannerItemRenderer').includes('CatAccessoryPreview.forStack'),'item scanner bonuses');
  check(java('client/CatStatsGoggleOverlay').includes('CatAccessoryPreview.forStack'),'goggles item bonuses');
  check(java('client/CatPancakeHoverPanelEvents').includes('CatAccessoryPreview.forStack'),'inventory pancake bonuses');
  check(java('client/CatAccessoryPreview').includes('nextPreviewId = -1_000_000')
    && java('client/BreedingBoxScreen').includes('CatAccessoryPreview.nextEntityId()'),
    'GUI previews cannot collide with real entity-ID keyed caches');
  check(java('create/BreedingBoxBlockEntity').includes('CatPancakeItem.babyVariantStack'),
    'breeding constructs a fresh child rather than copying equipped parent inventory');
  check(read(resources+'kubejs.classfilter.txt').trim()==='+cn.laowu.mod.api','stable script facade whitelist');
  check(!/kubejs|rhino|photon|ldlib/i.test(read(loader+'/gradle.properties')),'no new mandatory mod dependency');
}
// Execute the shipped JS against narrow API doubles, checking IDs and emitted v1 data.
// This checks the examples, NOT an actual KubeJS/Minecraft runtime.
const base='docs/examples/cat-accessories/';
for(const loader of ['forge','neoforge']){
  const made=[], data=[];
  const builder={displayName(){return this},maxStackSize(){return this},texture(){return this}};
  const context={
    StartupEvents:{registry(kind,fn){check(kind==='item','KubeJS item registry');fn({create(id){made.push('kubejs:'+id);return builder;}})}},
    ServerEvents:{
      highPriorityData(fn){check(loader==='forge','Forge generation hook');fn({addJson(id,json){data.push([id,json]);}})},
      generateData(stage,fn){check(loader==='neoforge'&&stage==='after_mods','NeoForge generation hook');fn({json(id,json){data.push([id,json]);}})}
    }
  };
  vm.runInNewContext(read(base+'startup_scripts/cat_accessories.js'),context);
  vm.runInNewContext(read(base+loader+'/server_scripts/cat_accessories.js'),context);
  check(data.length===1 && made.includes(data[0][1].item),'script registration matches generated definition');
  check(data[0][0]==='kubejs:cat_accessories/storm_cat_charm.json','correct resource path');
  check(JSON.stringify(data[0][1])===JSON.stringify(JSON.parse(read(base+'data/kubejs/cat_accessories/storm_cat_charm.json'))),'static and generated definitions equivalent');
}
console.log('PASS: '+checks+' accessory wiring, recipe, translation and KubeJS-example checks (not live gameplay)');
