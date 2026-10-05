import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';

// Compile the actual dependency-free production formatter, not a JS reimplementation.
// Catches message/URL leakage, lost root causes, and nontermination on cyclic causes.
for (const port of ['forge-1.20.1', 'neoforge-1.21.1']) {
  const source = fs.readFileSync(new URL(`../${port}/src/main/java/cn/laowu/mod/client/CatMusicRecordClient.java`, import.meta.url), 'utf8');
  const start = source.indexOf('static String failureTypes(Throwable error)');
  assert.ok(start >= 0, `${port}: playback failures need a privacy-safe cause-chain formatter`);
  let end = source.indexOf('{', start), depth = 1;
  while (depth && ++end < source.length) {
    if (source[end] === '{') depth++;
    else if (source[end] === '}') depth--;
  }
  const method = source.slice(start, end + 1);
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'meowchanics-music-diagnostics-'));
  try {
    const fixture = `import java.util.*;
public class MusicFailureDiagnosticsProbe {
${method}
public static void main(String[] args) {
  var root = new java.net.SocketTimeoutException("https://user:password@example.test/audio?token=SECRET");
  var wrapped = new java.util.concurrent.CompletionException(new java.lang.reflect.InvocationTargetException(root));
  String result = failureTypes(wrapped);
  if (!result.equals("java.util.concurrent.CompletionException -> java.lang.reflect.InvocationTargetException -> java.net.SocketTimeoutException")) throw new AssertionError(result);
  if (result.contains("SECRET") || result.contains("password") || result.contains("example.test")) throw new AssertionError("Sensitive exception text leaked");
  var a = new RuntimeException("secret a"); var b = new IllegalStateException("secret b");
  a.initCause(b); b.initCause(a);
  if (!failureTypes(a).equals("java.lang.RuntimeException -> java.lang.IllegalStateException -> [cycle]")) throw new AssertionError("Cause cycle must terminate");
  System.out.println("PASS: private cause-chain diagnostics");
}
}`;
    const file = path.join(dir, 'MusicFailureDiagnosticsProbe.java');
    fs.writeFileSync(file, fixture);
    const run = spawnSync('java', [file], { encoding: 'utf8', timeout: 15000 });
    assert.equal(run.status, 0, `${port}: ${run.stdout}\n${run.stderr}`);
    console.log(`${port}: ${run.stdout.trim()}`);
  } finally {
    fs.rmSync(dir, { recursive: true, force: true });
  }
}
