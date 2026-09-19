import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
function body(source, signature) {
  const start = source.indexOf(signature);
  assert.ok(start >= 0, `Missing ${signature}`);
  const open = source.indexOf('{', start);
  let depth = 1, end = open + 1;
  while (depth && end < source.length) {
    if (source[end] === '{') depth++;
    if (source[end] === '}') depth--;
    end++;
  }
  assert.equal(depth, 0);
  return source.slice(open + 1, end - 1);
}
for (const port of ['forge-1.20.1', 'neoforge-1.21.1']) {
  const source = fs.readFileSync(path.join(root, port, 'src/main/java/cn/laowu/mod/CommonEvents.java'), 'utf8');
  const join = body(source, 'void initializeCatTraits(');
  assert.match(join, /PENDING_CAT_INITIALIZATION\.add\(cat\)/);
  assert.doesNotMatch(join, /\.ensure\(|\.refresh\(|recoverInterruptedViewLock|\.execute\(|\.submit\(/,
    `${port}: join must not initialize directly or enqueue reentrant server tasks`);
  assert.match(source, /PENDING_CAT_INITIALIZATION\s*=\s*Collections\.synchronizedSet\(Collections\.newSetFromMap\(new WeakHashMap<>\(\)\)\)/);
  const init = body(source, 'void initializeCatAfterJoin(');
  assert.match(init, /if \(!PENDING_CAT_INITIALIZATION\.remove\(cat\)\) return/);
  for (const call of ['recoverInterruptedViewLock(cat)', 'CatTraitData.ensure(cat)', 'CatAttributeData.ensure(cat)', 'CatAttributeEffects.refresh(cat)'])
    assert.ok(init.includes(call), `${port}: deferred initialization retains ${call}`);
  const tick = body(source, 'void onLivingTick(');
  assert.ok(tick.indexOf('initializeCatAfterJoin(cat)') >= 0);
  assert.ok(tick.indexOf('initializeCatAfterJoin(cat)') < tick.indexOf('CatTeamRules.friendly'));
  assert.doesNotMatch(init, /tickCount\s*==\s*[01]/, 'Rejoins must not depend on first-ever tickCount');
  const probe = fs.readFileSync(path.join(root, 'tests/gametest', port.startsWith('forge') ? 'forge' : 'neo', 'cn/laowu/mod/test/CatLoadingProbe.java'), 'utf8');
  for (const test of ['freshCatDoesNotReadWorldOnJoin', 'loadedCatPreservesDataAndRecoversViewLock', 'rejoinAfterFirstTickIsDeferredAgain', 'protoChunkCatCompletesFullPromotion'])
    assert.ok(probe.includes(test), `${port}: missing ${test}`);
  assert.match(probe, /proto\.addEntity\(cat\)/);
  assert.match(probe, /ChunkStatus\.FULL/);
}
console.log('PASS: both loaders defer the entire cat-join initialization until a real entity tick; four runtime regressions are wired');
