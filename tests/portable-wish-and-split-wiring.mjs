import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const root=new URL('../',import.meta.url),read=p=>readFileSync(new URL(p,root),'utf8').replaceAll('\r\n','\n');
let count=0;const check=(value,message)=>{count++;assert.ok(value,message);};
for(const port of ['forge-1.20.1','neoforge-1.21.1']){
 const neo=port.startsWith('neo'),java=p=>read(port+'/src/main/java/cn/laowu/mod/'+p+'.java');
 const item=java('item/WishAdoptionBoxBlockItem'),block=java('create/WishAdoptionBoxBlock'),split=java('CatCockroachSplit');
 for(const hook of ['onCraftedBy','inventoryTick','place','appendHoverText'])check(item.includes(hook),'Craft, migrate, place and view hook '+hook);
 check(item.includes('BlockItem.setBlockEntityData')&&!item.includes('tag.put("Inventory"'),'Standard portable data contains no duplicated cargo');
 check(item.includes('existing.normalized(random)')&&item.includes('if(revised!=existing)'),'Valid cards never reroll on ticks/place');
 check(block.includes('super.getDrops(state,')&&block.includes('box.writeCardToItem(stack)'),'Preserve real loot table and carry current card on destruction');
 check(java('client/WishAdoptionBoxItemRenderer').includes('WishAdoptionBoxBlockItem.offer(stack)'),'Item displays stored reward, not a newly rolled one');
 check(java('recipe/WishAdoptionBoxRecipe').includes('extends ShapedRecipe')&&java('recipe/WishAdoptionBoxRecipe').includes(neo?'MechanicalCraftingInput':'MechanicalCraftingInventory'),'Create assembly stamps actual result, ordinary preview remains stable');
 const recipe=JSON.parse(read(port+'/src/main/resources/data/laowu/'+(neo?'recipe':'recipes')+'/wish_adoption_box_crafting.json'));
 check(recipe.type==='laowu:wish_adoption_box','Custom shaped codec registered');
 check(java('LaoWuMod').includes('WishAdoptionBoxRecipe.Serializer::new'),'Serializer registered on both ends');
 check(java('CommonEvents').includes('CatCockroachSplit.trySplit(cat,event.getSource())'),'Use actual lethal event source, not stale last damage');
 for(const term of ['parent.getTarget()','child.setTarget(inherited)','child.setLastHurtByMob(inherited)','CatTeamRules.canHarm(cat,target)','CareerCatBehavior.tick(child)'])check(split.includes(term),'Kitten combat inheritance '+term);
 const zh=JSON.parse(read(port+'/src/main/resources/assets/laowu/lang/zh_cn.json'));
 check(zh['cat_accessory.laowu.effect.cockroach_split']==='死亡时分裂为两只幼年蟑螂猫，各继承基础六维属性的50%%；','Exact shortened line with Minecraft escaped percent');
}
console.log('PASS: '+count+' portable Wish card and split hostility wiring checks');
