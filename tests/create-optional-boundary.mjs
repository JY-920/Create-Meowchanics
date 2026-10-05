import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const executable=name=>process.env.JAVA_HOME?path.join(process.env.JAVA_HOME,'bin',name+(process.platform==='win32'?'.exe':'')):name;
const output=fs.mkdtempSync(path.join(os.tmpdir(),'meowchanics-create-policy-'));
for(const port of ['forge-1.20.1','neoforge-1.21.1']) {
  execFileSync(executable('javac'),['-d',output,path.join(root,port,'src/main/java/cn/laowu/mod/compat/create/CreateMixinPolicy.java'),path.join(root,'tests/CreateMixinPolicyRegression.java')]);
  const result=execFileSync(executable('java'),['-cp',output,'CreateMixinPolicyRegression'],{encoding:'utf8'});
  assert.match(result,/PASS: 7 optional Create mixin activation cases/);
}
console.log('PASS: dual-loader optional Create mixin behavior without Create on the classpath');
