import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
const root=path.resolve(import.meta.dirname,'..');
for(const loader of ['forge-1.20.1','neoforge-1.21.1']){
  const res=path.join(root,loader,'src/main/resources');
  const def=path.join(res,'data/laowu/cat_accessories/cat_giant_collar.json');
  assert(fs.existsSync(def),'Giant collar must be recognized by the accessory reload pipeline');
  assert.deepEqual(JSON.parse(fs.readFileSync(def)).effects,{giant_mount:1});
  const model=JSON.parse(fs.readFileSync(path.join(res,'assets/laowu/models/item/cat_giant_collar.json')));
  assert.equal(model.textures.layer0,'laowu:item/cat_giant_collar','Use the supplied trophy sprite');
  const sprites=[
    ['cat_giant_collar','00b6844f305e5de0fb57b449f614f363347ba9059a88abdeca82bf02860982ef'],
    ['giant_cat_treat','7c67fb447e1d68c4c601163a4bb0bc02df93c7a079e0eba445ffbb4e3c0de6dd']
  ];
  for(const [name,hash] of sprites){
    const itemModel=JSON.parse(fs.readFileSync(path.join(res,'assets/laowu/models/item',name+'.json')));
    assert.equal(itemModel.textures.layer0,'laowu:item/'+name,'Registered item resolves the supplied artwork');
    const sprite=path.join(res,'assets/laowu/textures/item',name+'.png');
    assert(fs.existsSync(sprite),'Referenced item texture must be packaged');
    const bytes=fs.readFileSync(sprite);
    assert.equal(bytes.readUInt32BE(16),16);assert.equal(bytes.readUInt32BE(20),16);
    assert.equal(createHash('sha256').update(bytes).digest('hex'),hash,'Keep the supplied pixels unchanged');
  }
  for(const language of ['zh_cn','en_us']){
    const lang=JSON.parse(fs.readFileSync(path.join(res,'assets/laowu/lang',language+'.json')));
    assert(lang['item.laowu.cat_giant_collar']);
    assert(lang['cat_accessory.laowu.effect.giant_mount']);
  }
}
console.log('PASS: giant collar data reload, exact supplied summon/trophy sprites and both languages');
