import assert from 'node:assert/strict';
import {readFileSync,readdirSync} from 'node:fs';
import {createHash} from 'node:crypto';
const root=new URL('../',import.meta.url);
const read=p=>readFileSync(new URL(p,root),'utf8').replaceAll('\r\n','\n');
const manifest=JSON.parse(read('art/wish-adoption-box/manifest.json'));
let checks=0;
const check=(v,m)=>{checks++;assert.ok(v,m);};
for(const port of ['forge-1.20.1','neoforge-1.21.1']){
 const neo=port.startsWith('neo'),res=port+'/src/main/resources/',java=p=>read(port+'/src/main/java/cn/laowu/mod/'+p+'.java');
 for(const asset of manifest.assets){
  const bytes=readFileSync(new URL(res+'assets/laowu/'+asset.path,root));
  check(createHash('sha256').update(bytes).digest('hex')===asset.sha256,'Exact supplied '+port+' '+asset.path);
 }
 const block=JSON.parse(read(res+'assets/laowu/models/block/wish_adoption_box.bbmodel'));
 check(block.elements.length===13,'All authored elements including the new sign imported');
 const sign=block.elements.find(e=>e.from[0]===-6&&e.to[0]===6&&e.from[2]===7.5);
 check(sign&&sign.from[1]===2&&sign.to[1]===10&&sign.to[2]===8.5,'Dynamic overlay dimensions match actual artist sign');
 const names=JSON.parse(read(res+'assets/laowu/lang/zh_cn.json'));
 check(names['block.laowu.wish_adoption_box']==='心愿领养箱','Simple new block name');
 check(names['item.laowu.cat_rebirth_ootheca']==='爆珠奶茶','Cockroach accessory final name');
 const mod=java('LaoWuMod'),events=java('client/ClientModEvents');
 for(const key of ['WISH_ADOPTION_BOX','WISH_ADOPTION_BOX_BE','WISH_ADOPTION_BOX_ITEM','WISH_ADOPTION_BOX_MENU'])
  check(mod.includes(key),'Registered '+key);
 check(events.includes('WishAdoptionBoxScreen::new')&&events.includes('WishAdoptionBoxRenderer::new'),'Actual client registrations');
 const be=java('create/WishAdoptionBoxBlockEntity'),menu=java('WishAdoptionBoxMenu'),screen=java('client/WishAdoptionBoxScreen');
 check(be.indexOf('if (outputs == null) continue;')<be.indexOf('inventory.setStackInSlot(input'),'Capacity checked before consumption');
 check(be.includes('if (!locked) offer = WishAdoptionOffer.roll'),'Locked transaction retains reward');
 check(menu.includes('player.containerMenu != this')&&menu.includes('player.isSpectator()')&&menu.includes('level.getBlockEntity(pos) == box'),'Server action scope and exact entity guard');
 check(menu.includes('item >>> 16')&&menu.includes('& 65535'),'Full registry item IDs survive short menu data fields');
 check(screen.includes('g.renderItem(offer.rewardStack()')&&!menu.includes('addSlot(new SlotItemHandler(machineInventory, 18'),'Reward is preview, never a real output slot');
 check(!screen.includes('g.drawString(font, title')&&screen.includes('REWARD_X = 116, REWARD_Y = 44'),'No exterior title; reward icon and its tooltip target move up one pixel');
 check(menu.includes('(input ? 27 : 92) + col * 20, (input ? 91 : 97) + row * 20'),'Machine item/hover/click origins move right one and down two together');
 check(!be.includes('net.minecraft.client'),'Dedicated server has no client dependencies');
 const recipe=JSON.parse(read(res+'data/laowu/'+(neo?'recipe':'recipes')+'/wish_adoption_box_crafting.json'));
 check(recipe.result[neo?'id':'item']==='laowu:wish_adoption_box'&&recipe.key.A.item==='laowu:adoption_box','Obtainable separate upgrade, ordinary box unaffected');
 const expectedOrder=['ATTACK','HEALTH','SPEED','STAMINA','INTELLIGENCE','LUCK'];
 check(expectedOrder.every((stat,i)=>java('genetics/CatStat').indexOf(stat)<(i===5?Infinity:java('genetics/CatStat').indexOf(expectedOrder[i+1]))),'Shared stat order unchanged');
 check(screen.includes('CatStat.values()')&&screen.includes('renderAttributeIcon')&&screen.includes('renderConnectedNumberLeft'),'Shared attribute icons, fixed rows and pixel digits');
 check(screen.includes('g.setColor(.25F')&&screen.includes('no_requirement'),'Unused stat icons dimmed, explicit tooltip');
 check(java('accessory/CatAccessoryRegistry').includes('CatAccessoryRarity.bossOnly'),'Boss rewards excluded from pool');
 const boss=JSON.parse(read(res+'data/laowu/tags/'+(neo?'item':'items')+'/boss_accessories.json'));
 check(boss.values.includes('laowu:cat_butter_cube'),'Butter Boss-only tag');
 for(const name of ['adoption_box','wish_adoption_box']){
  const m=JSON.parse(read(res+'assets/laowu/models/block/'+name+'.bbmodel'));
  const floor=m.elements.find(e=>e.uuid==='4cfac539-80ac-e22e-d780-fc8370b1c54e');
  check(floor.from[1]===0.0625&&floor.to[1]===0.1875,'Raised separated bottom faces: '+name);
 }
 const dir=res+'data/laowu/'+(neo?'recipe':'recipes')+'/';
 const accessoryIds=new Set(readdirSync(new URL(res+'data/laowu/cat_accessories/',root)).map(f=>JSON.parse(read(res+'data/laowu/cat_accessories/'+f)).item));
 function scan(rel){
  for(const entry of readdirSync(new URL(rel,root),{withFileTypes:true})){
   if(entry.isDirectory())scan(rel+entry.name+'/');
   else if(entry.name.endsWith('.json')){
    const data=JSON.parse(read(rel+entry.name));
    for(const result of [data.result,...(data.results??[])].filter(Boolean))
     check(!accessoryIds.has(typeof result==='string'?result:(result.item??result.id)),'No recipe anywhere produces an accessory: '+entry.name);
   }
  }
 }
 scan(dir);
 const pouchRecipe=JSON.parse(read(dir+'cat_pouch.json'));
 check(JSON.stringify(pouchRecipe.pattern)===JSON.stringify([' S ','LLL',' B '])&&pouchRecipe.key.S.item==='minecraft:string'&&pouchRecipe.key.L.item==='minecraft:leather'&&pouchRecipe.key.B.item==='create:brass_ingot','New pouch recipe');
 const pouch=java('item/CatPouchItem'),common=java('CommonEvents');
 check(common.indexOf('instanceof cn.laowu.mod.item.CatPouchItem pouch')<common.indexOf('InteractionResult pilotRide'),'Pouch intercepts vanilla cat/flattened-cat interaction');
 check(pouch.indexOf('if (!insertOne(pouch, pancake))')<pouch.indexOf('cat.discard()')&&pouch.includes('busy(cat, level)')&&pouch.includes('player.hasLineOfSight(cat)'),'Atomic capture, active viewers and reach guards');

}
console.log('PASS: '+checks+' wish adoption asset hashes, white sign coordinates, registrations, recipe and transaction wiring checks');
