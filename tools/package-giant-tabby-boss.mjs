import { createRequire } from 'node:module';
import { createHash } from 'node:crypto';
import { mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import assert from 'node:assert/strict';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const source = path.join(root, 'art/giant-tabby-boss');
const dependencyRoot = process.env.MEOW_ART_NODE_MODULES ||
  'C:/Users/16611/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules';
const require = createRequire(import.meta.url);
const { chromium } = require(path.join(dependencyRoot, 'playwright'));
const { PNG } = require(path.join(dependencyRoot, 'pngjs'));
const JSZip = require(path.join(dependencyRoot, 'jszip'));
const browser = await chromium.launch({ headless: true,
  executablePath: process.env.MEOW_ART_BROWSER || 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe' });
try {
  const page = await browser.newPage({ viewport: { width: 1600, height: 1150 }, deviceScaleFactor: 1 });
  await page.goto(pathToFileURL(path.join(source, 'previews/preview.html')).href);
  await page.evaluate(() => Promise.all(Array.from(document.images, i => i.decode())));
  await page.screenshot({ path: path.join(source, 'previews/overview.png') });
} finally { await browser.close(); }
const hero = PNG.sync.read(readFileSync(path.join(source, 'previews/hero.png')));
let opaque = 0;
for (let i = 3; i < hero.data.length; i += 4) if (hero.data[i] > 128) opaque++;
assert(opaque > hero.width * hero.height * .15, 'Hero render empty or camera too distant');
const native = JSON.parse(readFileSync(path.join(source, 'blockbench-validation.json')));
assert.equal(native.cubes, 23); assert.equal(native.bones, 20); assert.equal(native.animations, 8);
const contacts = JSON.parse(readFileSync(path.join(source, 'animation-contact-validation.json')));
assert.equal(contacts.length, 8);
assert(contacts.every(a => a.minimum_y > -.03));
const zip = new JSZip(), records = {};
function addDir(dir, prefix = '') {
  for (const f of readdirSync(dir, { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name))) {
    const p = path.join(dir, f.name), rel = prefix + f.name;
    if (f.isDirectory()) addDir(p, rel + '/');
    else {
      const bytes = readFileSync(p);
      zip.file(rel, bytes, { date: new Date('2026-09-18T00:00:00Z') });
      records[rel] = { bytes: bytes.length, sha256: createHash('sha256').update(bytes).digest('hex') };
    }
  }
}
addDir(source);
assert(Object.keys(records).every(f => !/\.(jar|mp4|jpg)$|source-contact|source-selected|kubejs/i.test(f)));
const bytes = await zip.generateAsync({ type: 'nodebuffer', compression: 'DEFLATE', compressionOptions: { level: 9 } });
const output = path.join(root, 'releases/giant-tabby-boss-model-v1.zip');
mkdirSync(path.dirname(output), { recursive: true });
writeFileSync(output, bytes);
const reopened = await JSZip.loadAsync(readFileSync(output), { checkCRC32: true });
for (const [name, r] of Object.entries(records)) {
  const b = await reopened.file(name).async('nodebuffer');
  assert.equal(createHash('sha256').update(b).digest('hex'), r.sha256, 'ZIP mismatch ' + name);
}
const report = { file: output, bytes: bytes.length, sha256: createHash('sha256').update(bytes).digest('hex'),
  entries: Object.keys(records).length, contact_frames: contacts.reduce((n, a) => n + a.frames, 0),
  source_images_included: false, mod_jars_changed: false, files: records };
writeFileSync(path.join(root, 'releases/giant-tabby-boss-model-v1.manifest.json'), JSON.stringify(report, null, 2) + '\n');
console.log(JSON.stringify({ ...report, files: undefined }, null, 2));
