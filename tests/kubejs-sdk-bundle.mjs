import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
import vm from 'node:vm';
const root=fileURLToPath(new URL('../',import.meta.url));
const stage=process.argv[2];
if(!stage){console.log('SKIP: pass generated SDK root to validate its portable bundle');process.exit(0);}
const full=path.resolve(stage),read=p=>fs.readFileSync(path.join(full,p),'utf8');
const manifest=JSON.parse(read('manifest.json'));
let checks=0;const check=(ok,message)=>{checks++;assert.ok(ok,message);};
for(const file of manifest.files){
 const resolved=path.resolve(full,file.path);
 check(resolved.startsWith(full+path.sep),'path contained');
 check(createHash('sha256').update(fs.readFileSync(resolved)).digest('hex').toUpperCase()===file.sha256,'file SHA '+file.path);
 check(!/\.(jar|class|log|nbt|mca)$/.test(file.path),'SDK excludes runtime binaries/worlds');
 if(file.path.endsWith('.md')){
  const text=read(file.path);
  check(!text.includes('待最终核验'),'release docs cannot claim pending validation');
  check(!/[CD]:[\\/]/.test(text),'no author-machine paths');
  for(const match of text.matchAll(/\[[^\]]*\]\(([^)]+)\)/g)){
   const url=match[1];
   if(/^[a-z]+:|^#/.test(url))continue;
   check(fs.existsSync(path.resolve(path.dirname(resolved),decodeURIComponent(url.split('#')[0]))),'portable doc link '+file.path+' -> '+url);
  }
 }
}
const catalog=JSON.parse(read('catalog/accessories36.json'));
check(catalog.items.length===36&&catalog.scripted_count===7,'36 entries with explicit implementation boundary');
const schema=JSON.parse(read('contracts/accessory.schema.json')),contract=JSON.parse(read('contracts/api-v3.json'));
for(const item of catalog.items){
 check(contract.stable_builtin_ids.includes(item.definition.item),'stable item ID');
 for(const [key,value] of Object.entries(item.definition.effects)){
  const spec=schema.properties.effects.properties[key];check(!!spec,'known effect');
  check(value>=spec.minimum&&value<=spec.maximum,'effect bounds');
  if(spec.type==='integer')check(Number.isInteger(value),'integer effect');
 }
}
for(const loader of ['forge','neoforge']){
 const prefix='replacement/'+loader+'/kubejs/server_scripts/';
 check(fs.readdirSync(path.join(full,prefix)).filter(x=>x.endsWith('.js')).length===4,'exactly four replacement files');
 let outputs=0,callbacks={};
 const context=vm.createContext({console:{warn:()=>{}},Java:{loadClass:()=>({apiVersion:()=>3,supportsApi:()=>true,schemaVersion:()=>1})},
  CatAccessoryEvents:new Proxy({},{get:(_,key)=>fn=>callbacks[key]=fn}),
  ServerEvents:{highPriorityData:fn=>fn({addJson:()=>outputs++}),generateData:(stage,fn)=>fn({json:()=>outputs++})}});
 for(const name of ['compat','catalog','effects','data'])vm.runInContext(read(prefix+'laowu36_'+name+'.js'),context);
 check(outputs===36,'packaged runtime emits all 36');
 // Validate new item script against its own startup and actual generated definition.
 let registered='',definition;
 const demo=vm.createContext({Java:{loadClass:()=>({apiVersion:()=>3,supportsApi:()=>true,schemaVersion:()=>1})},
  CatAccessoryEvents:{acceptedAttack:fn=>callbacks.demo=fn},
  StartupEvents:{registry:(kind,fn)=>fn({create:id=>{registered='kubejs:'+id;const b={displayName:()=>b,maxStackSize:()=>b,texture:()=>b};return b;}})},
  ServerEvents:{highPriorityData:fn=>fn({addJson:(p,d)=>definition=d}),generateData:(s,fn)=>fn({json:(p,d)=>definition=d})}});
 for(const kind of ['startup_scripts','server_scripts'])
  vm.runInContext(read('examples/new-accessory/'+loader+'/kubejs/'+kind+'/example_cat_charm.js'),demo);
 check(registered===definition.item&&definition.effects.speed===10,'new registration/data identity');
 let heals=0,ready=true;
 callbacks.demo({context:{accessory:()=>({script:definition.script,tryActivate:(key,ticks,cost)=>{check(ticks===200&&cost===0,'cooldown units');const r=ready;ready=false;return r;}}),healSelf:n=>heals+=n}});
 callbacks.demo({context:{accessory:()=>({script:definition.script,tryActivate:()=>false}),healSelf:n=>heals+=n}});
 check(heals===2,'sample callback never heals twice in cooldown');
}
console.log('PASS: '+checks+' portable SDK paths, hashes, schema bounds, generated scripts and new-item checks');
