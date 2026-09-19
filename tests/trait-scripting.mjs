import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
const root=new URL('../',import.meta.url);
const read=path=>readFileSync(new URL(path,root),'utf8').replaceAll('\r\n','\n');
const contract=JSON.parse(read('compatibility/cat-trait-api-v1.json'));
let checks=0;
const check=(value,message)=>{checks++;assert.ok(value,message)};
check(contract.api_version===1&&contract.schema_version===1,'Versioned API and schema');
check(contract.stable_builtin_ids.length===82&&new Set(contract.stable_builtin_ids).size===82,'82 permanent native IDs');
for(const [port,loader] of [['forge-1.20.1','forge'],['neoforge-1.21.1','neoforge']]) {
  const java=n=>read(port+'/src/main/java/cn/laowu/mod/'+n+'.java');
  const resources=n=>read(port+'/src/main/resources/'+n);
  check(resources('kubejs.plugins.txt').split('\n').filter(s=>s.endsWith('CatTraitsKubePlugin')).length===1,'One trait plugin');
  check(java('compat/kubejs/CatTraitsKubePlugin').includes('GROUP.server'),'Server-only trait callbacks');
  check(java('CommonEvents').includes('CatTraitHooks.tick(cat)'),'Real cat tick dispatch');
  check(java('accessory/CatAccessoryEvents').includes('new cn.laowu.mod.genetics.CatTraitRegistry()'),'Data reload wired');
  check(java('client/ClientInputEvents').includes('CatTraitRegistry.resetClient()'),'Client mirror clears on logout');
  check(java('client/ClientPacketHandler').includes('CatTraitRegistry.receive'),'Client receives authoritative definitions');
  check(java('client/CatScannerTextureManager').includes('CatTraitRegistry.revision(true)'), 'Scanner cache refreshes when trait definitions change');
  check(java('api/CatTraitApi').includes('server.isSameThread()'),'Pancake edits require actual server main thread');
  check(java('api/CatTraitApi').includes('CatTraitHooks.withoutEvents'),'Helper effects suppress recursive trait/ accessory callbacks');
  const handle=java('api/CatTraitHandle');
  check(handle.includes('Trait handle expired')&&handle.includes('CatTraitRegistry.revision(false) != revision'),'Tick/profile/reload lifetime guards');
  const menu=java('CatTraitEditorMenu');
  check(menu.includes('writeOpeningData')&&menu.includes('readCatalog')&&menu.includes('registryRevision != CatTraitRegistry.revision(false)'),'Frozen menu IDs and stale reload guard');
  check(java('item/CatFilterRules').includes('CatTraitRegistry.resolve')&&java('CatFilterMenu').includes('traitRevision != CatTraitRegistry.revision(false)'),'Filter supports permanent custom IDs');
  check(read(port+'/build.gradle').includes("apply from: '../gradle/cat-trait-compatibility.gradle'"),'Mandatory ABI guard');
  check(java('genetics/CatTraitRegistry').includes('server = Map.copyOf(next)')&&java('genetics/CatTraitRegistry').includes('client = Map.copyOf(next)'),'Independent atomic registries');
  for(const event of contract.events)check(java('genetics/CatTraitHooks').includes('"'+event+'"'),'Event '+event);
  check(java('genetics/CatTraitHooks').includes('!cat.isAlive() && !context.getType().equals("death")'),'Dead cats only receive the read-only death event');
  for(const lang of ['zh_cn','en_us']) {
    const translated=JSON.parse(resources('assets/laowu/lang/'+lang+'.json'));
    check(!!translated['trait.laowu.script_missing.title']&&!!translated['trait.laowu.script_missing.description'],'Missing data is explained');
  }
  const example=read('docs/examples/cat-traits-v1/'+loader+'/kubejs/server_scripts/cat_traits.js');
  const definitions={},events={},api={supportsApi:v=>v===1,schemaVersion:()=>1};
  const context={
    Java:{loadClass:name=>{check(name==='cn.laowu.mod.api.CatTraitApi','Public API only in shipped script');return api}},
    CatTraitEvents:Object.fromEntries(contract.events.map(name=>[name,fn=>events[name]=fn])),
    ServerEvents:loader==='forge'?{highPriorityData:fn=>fn({addJson:(k,v)=>definitions[k]=v})}:
      {generateData:(phase,fn)=>{check(phase==='after_mods','Neo data phase');fn({json:(k,v)=>definitions[k]=v})}}
  };
  vm.runInNewContext(example,context,{filename:loader+'/cat_traits.js'});
  check(Object.keys(definitions).length===3,'Three actual generated definitions');
  for(const [id,def] of Object.entries(definitions)) {
    check(/^examplepack:cat_traits\/[a-z_]+\.json$/.test(id),'Correct resource path');
    check(def.schema_version===1&&def.natural===false&&def.mutation===false&&def.inheritable===true,'Examples opt in to random generation');
    check(def.description.length===def.max_level,'Description levels match');
  }
  const state={hits:0,heals:0,cooldown:false};
  const trait={level:2,tryActivate:(key,ticks)=>{check(ticks===80,'Shipped heal cooldown is 80 ticks');if(state.cooldown)return false;state.cooldown=true;return true},
    increment:(key,n)=>{if(key.endsWith(':hits'))state.hits+=n;else state.heals+=n}};
  const cat={hp:5,max:20};
  api.health=c=>c.hp;api.maxHealth=c=>c.max;api.heal=(self,other,amount)=>{other.hp+=amount;return true};
  const ctx={cat,damage:3,trait:id=>trait,setDamage:v=>ctx.damage=v};
  events.beforeAttack({context:ctx});check(ctx.damage===5,'Shipped attack example adds trait level');
  events.afterAttack({context:ctx});check(state.hits===1,'Shipped hit counter');
  events.tick({context:ctx});events.tick({context:ctx});
  check(cat.hp===7&&state.heals===1,'Shipped healing example cooldown reserves once');
  ctx.trait=()=>null;events.beforeAttack({context:ctx});events.tick({context:ctx});
  check(cat.hp===7&&ctx.damage===5,'No trait means no effect');
}
console.log('PASS: '+checks+' trait API/loader wiring and shipped script checks (real runtime is tested separately)');
