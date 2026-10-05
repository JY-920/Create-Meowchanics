import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const fixture = fs.mkdtempSync(path.join(os.tmpdir(), 'giant-cat-export-'));
const archived = path.join(fixture, 'art/giant-cat-mount/source');
const exporter = path.join(fixture, 'tools/export-giant-cat-mount.mjs');
function write(file, content) {
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, content);
}
try {
    write(exporter, fs.readFileSync(path.join(root, 'tools/export-giant-cat-mount.mjs')));
    const original = JSON.parse(fs.readFileSync(path.join(root, 'art/giant-cat-mount/source/vanilla_yellow_cat.bbmodel')));
    original.elements[0].name = 'fixture_head_from_archive';
    const archivedModel = path.join(archived, 'vanilla_yellow_cat.bbmodel');
    const archiveBytes = Buffer.from(JSON.stringify(original));
    write(archivedModel, archiveBytes);
    write(path.join(archived, 'yellow_cat.png'),
        fs.readFileSync(path.join(root, 'art/giant-cat-mount/source/yellow_cat.png')));

    execFileSync(process.execPath, [exporter], { cwd: fixture, stdio: 'pipe' });
    const generated = JSON.parse(fs.readFileSync(path.join(fixture, 'art/giant-cat-mount/giant_cat_mount.bbmodel')));
    assert.equal(generated.elements[0].name, 'fixture_head_from_archive',
        'default export reads its own archived source, not a user Downloads path');
    assert.deepEqual(fs.readFileSync(archivedModel), archiveBytes,
        'a rebuild never rewrites the archived original');

    const imported = structuredClone(original);
    imported.elements[0].name = 'explicit_source_head';
    const external = path.join(fixture, 'external/model.bbmodel');
    write(external, JSON.stringify(imported));
    execFileSync(process.execPath, [exporter, '--source', external], { cwd: fixture, stdio: 'pipe' });
    const selected = JSON.parse(fs.readFileSync(path.join(fixture, 'art/giant-cat-mount/giant_cat_mount.bbmodel')));
    assert.equal(selected.elements[0].name, 'explicit_source_head',
        '--source deliberately selects another Blockbench project');
    assert.deepEqual(fs.readFileSync(archivedModel), archiveBytes,
        'explicit --source does not silently replace the original archive');
    console.log('giant cat mount exporter: OK');
} finally {
    fs.rmSync(fixture, { recursive: true, force: true });
}
