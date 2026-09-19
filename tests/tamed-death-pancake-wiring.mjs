import assert from 'node:assert/strict';
import fs from 'node:fs';
const root=new URL('../',import.meta.url);
const read=p=>fs.readFileSync(new URL(p,root),'utf8').replaceAll('\r\n','\n');
let checks=0;
for(const port of ['forge-1.20.1','neoforge-1.21.1']){
 const java=name=>read(port+'/src/main/java/cn/laowu/mod/'+name+'.java');
 const source=java('CommonEvents');
 const death=source.slice(source.indexOf('public static void onCatDeath('),source.indexOf('public static void replaceCatStringDropsWithFur('));
 for(const required of ['cat.isTame() || outfit != CatOutfitType.NONE','!split','RULE_DOMOBLOOT','CatPancakeItem.captureDeathDrop(cat)','CatProfileData.dropOnDeath(cat,!split)']){
  assert.ok(death.includes(required),port+': preserved death lifecycle '+required);checks++;
 }
 assert.ok(java('CatProfileData').includes('preserveInPancake && (cat.isTame() || CatClothesData.getOutfit(cat) != CatOutfitType.NONE)'),port+': inventory eligibility matches pancake eligibility');checks++;
 assert.ok(java('ServerConfig').includes('DEFAULT_DEATH_ATTRIBUTE_LOSS = 20'),port+': compatible default death penalty');checks++;
 for(const required of ['ServerConfig.deathAttributePenaltyEnabled() ? ServerConfig.deathAttributeLoss() : 0','if (loss > 0)','attributes.current(reducedStat) - loss']){
  assert.ok(java('item/CatPancakeItem').includes(required),port+': configurable death penalty '+required);checks++;
 }
 // Source guards supplement (not replace) the real config and death/restore probes.
 const ui=java('client/WorldSettingsScreen');
 for(const required of ['b -> changePage(3)','deathPage = page == 3','deathPenaltyToggle.visible = deathLoss.visible = deathPage',
  'Integer.parseInt(deathLoss.getValue())','&& validDeathInput()','if (!traitPage && !deathPage)']){
  assert.ok(ui.includes(required),port+': death settings UI wiring '+required);checks++;
 }
 for(const locale of ['zh_cn','en_us']){
  const strings=JSON.parse(read(port+'/src/main/resources/assets/laowu/lang/'+locale+'.json'));
  for(const suffix of ['tab_death','cat_death_attribute_penalty_enabled','cat_death_attribute_penalty_enabled.help','death_loss','death_loss.help','death_help']){
   assert.ok(strings['screen.laowu.world.'+suffix]?.length>0,port+': death setting translation '+locale+'/'+suffix);checks++;
  }
 }
}
for(const name of ['ServerConfig','GlobalConfig','client/WorldSettingsScreen']){
 const normalized=port=>read(port+'/src/main/java/cn/laowu/mod/'+name+'.java')
  .replaceAll('net.minecraftforge.common.ForgeConfigSpec','net.neoforged.neoforge.common.ModConfigSpec')
  .replaceAll('ForgeConfigSpec','ModConfigSpec').replace('renderBackground(g);','renderBackground(g, mouseX, mouseY, partialTick);');
 assert.equal(normalized('forge-1.20.1'),normalized('neoforge-1.21.1'),'Identical death configuration and UI logic across loaders: '+name);checks++;
}
console.log('PASS: '+checks+' tamed death-pancake, no duplicate cargo, split and loot-rule guards');
