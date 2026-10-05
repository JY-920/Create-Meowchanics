import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
for (const port of ['forge-1.20.1','neoforge-1.21.1']) {
 const png=readFileSync(`${port}/src/main/resources/assets/laowu/textures/gui/cat_editor.png`);
 assert.deepEqual([png.readUInt32BE(16),png.readUInt32BE(20)],[630,512], 'Use supplied editor v2 atlas');
}
console.log('PASS editor v2 atlas dimensions');
