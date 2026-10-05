// Catch accidental draft-art packaging or fallback to gray Create casing faces.
import assert from 'node:assert/strict';
import {existsSync,readFileSync,readdirSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const sources={
  cat_casing_shaft_opening:'gearbox',
  cat_encased_cogwheel_side:'andesite_encased_cogwheel_side',
  cat_encased_cogwheel_side_connected:'andesite_encased_cogwheel_side_connected',
};
for(const loader of ['forge-1.20.1','neoforge-1.21.1']) {
  const base=path.join(root,loader,'src/main/resources/assets/laowu');
  for(const [name,source] of Object.entries(sources)) {
    const target=path.join(base,'textures/block',name+'.png');
    assert(existsSync(target),`${loader}: user-authored texture is missing: ${name}`);
    assert.deepEqual(readFileSync(target),readFileSync(path.join(root,'art/cat-machines/encased-user-20260926',source+'.png')),
      `${loader}: user PNG bytes must remain unchanged: ${name}`);
  }
  for(const kind of ['block','item'])for(const file of readdirSync(path.join(base,'models',kind))) {
    if(!file.startsWith('cat_encased'))continue;
    const model=JSON.parse(readFileSync(path.join(base,'models',kind,file),'utf8'));
    if(file.includes('shaft'))assert.equal(model.textures.opening,'laowu:block/cat_casing_shaft_opening');
    else {
      const large=file.includes('large');
      assert.equal(model.textures.side,'laowu:block/cat_encased_cogwheel_side'+(large?'_connected':''));
      assert.equal(model.textures['4'],kind==='item'&&large?'create:block/large_cogwheel':'laowu:block/cat_casing_shaft_opening');
    }
  }
}
console.log('PASS: exact user PNGs, shaft/cog bindings and native large-cog item gear preserved');
