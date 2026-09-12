import { readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';

// Direct client for the user's existing local Blockbench MCP server.
const endpoint = process.env.BLOCKBENCH_MCP_URL || 'http://localhost:3000/bb-mcp';
let session, sequence = 0;
async function rpc(method, params, notification = false) {
  const response = await fetch(endpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json, text/event-stream',
      ...(session ? { 'mcp-session-id': session } : {}) },
    body: JSON.stringify({ jsonrpc: '2.0', ...(notification ? {} : { id: ++sequence }), method, params }),
    signal: AbortSignal.timeout(30000)
  });
  if (!response.ok) throw new Error('MCP HTTP ' + response.status);
  session = response.headers.get('mcp-session-id') || session;
  const source = await response.text();
  if (!source) return {};
  const message = source.startsWith('event:') || source.startsWith('data:')
    ? JSON.parse(source.split('\n').find(line => line.startsWith('data:')).slice(5))
    : JSON.parse(source);
  if (message.error) throw new Error(JSON.stringify(message.error));
  return message.result;
}
await rpc('initialize', { protocolVersion: '2024-11-05', capabilities: {},
  clientInfo: { name: 'meowchanics-model-authoring', version: '1.0' } });
await rpc('notifications/initialized', {}, true);
const [action, argument, extra] = process.argv.slice(2);
let result;
if (action === 'schema') {
  result = (await rpc('tools/list', {})).tools.filter(tool => new RegExp(argument || '.').test(tool.name));
} else {
  let name = action, args = {};
  if (action === 'eval-file') {
    name = 'risky_eval';
    args = { code: readFileSync(argument, 'utf8') };
  } else if (action === 'import-cat') {
    const project = JSON.parse(readFileSync(argument, 'utf8'));
    project.animations = [];
    project.name = '琵琶行猫';
    project.save_path = '';
    name = 'risky_eval';
    // Encode literal slashes inside JSON strings; the server rejects comment
    // markers even when they occur inside embedded PNG base64 data.
    const projectJson = JSON.stringify(project).replaceAll('/', '\\u002f');
    args = { code: `(() => { Codecs.project.parse(${projectJson}); Project.name='琵琶行猫'; return JSON.stringify({name:Project.name,groups:Group.all.map(g=>({name:g.name,origin:g.origin,rotation:g.rotation})),textures:Texture.all.map(t=>({name:t.name,width:t.width,height:t.height}))}); })()` };
  } else if (action === 'capture') {
    name = 'capture_screenshot'; args = {};
  } else {
    args = argument ? JSON.parse(argument) : {};
  }
  result = await rpc('tools/call', { name, arguments: args });
  if (result.isError) throw new Error(JSON.stringify(result));
  if (action === 'capture') {
    const img = result.content.find(item => item.type === 'image');
    if (!img) throw new Error('Screenshot did not contain image data');
    writeFileSync(path.resolve(argument), Buffer.from(img.data, 'base64'));
    result = { screenshot: path.resolve(argument), mimeType: img.mimeType };
  }
}
if (result?.content) result.content = result.content.map(item => {
  if (item.type === 'image') return { type: 'image', mimeType: item.mimeType, bytes: item.data.length };
  if (item.type === 'text') {
    let value = item.text;
    for (let i = 0; i < 3 && typeof value === 'string'; i++) {
      try { value = JSON.parse(value); } catch { break; }
    }
    return { type: 'text', value };
  }
  return item;
});
process.stdout.write(JSON.stringify(result, null, 2) + '\n');
