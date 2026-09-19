import assert from 'node:assert/strict';
import {readFileSync, readdirSync} from 'node:fs';
import vm from 'node:vm';
const root=new URL('../',import.meta.url);
const read=p=>readFileSync(new URL(p,root),'utf8').replaceAll('\r\n','\n');
const folder='docs/examples/cat-accessories-36/';
let checks=0;
const check=(test,message)=>{checks++;assert.ok(test,message);};
for(const [loader,adapter] of [['forge-1.20.1','forge'],['neoforge-1.21.1','neoforge']]){
 const outputs=new Map(),listeners={},environment={
   ServerEvents:{
    highPriorityData:fn=>fn({addJson:(p,d)=>outputs.set(p,d)}),
    generateData:(stage,fn)=>{assert.equal(stage,'after_mods');fn({json:(p,d)=>outputs.set(p,d)});}
   },
   Java:{loadClass:()=>({apiVersion:()=>3,schemaVersion:()=>1,supportsApi:v=>v===3,canHarm:()=>true,isPancake:()=>false,isFinishing:()=>false,reflectDamage:()=>true})},
   CatAccessoryEvents:new Proxy({}, {get:(_,name)=>fn=>(listeners[name]??=[]).push(fn)})
 };
 const context=vm.createContext(environment);
 vm.runInContext(read(folder+'common/kubejs/server_scripts/laowu36_compat.js'),context);
 vm.runInContext(read(folder+'common/kubejs/server_scripts/laowu36_catalog.js'),context);
 vm.runInContext(read(folder+'common/kubejs/server_scripts/laowu36_effects.js'),context);
 vm.runInContext(read(folder+adapter+'/kubejs/server_scripts/laowu36_data.js'),context);
 const entries=vm.runInContext('laowuExamples36.entries',context);
 check(entries.length===36&&outputs.size===36,loader+' emits exactly 36 definitions');
 const resources=loader+'/src/main/resources/data/laowu/cat_accessories/';
 check(readdirSync(new URL(resources,root)).filter(x=>x.endsWith('.json')).length===36,'all native definitions accounted for');
 let scripted=0;
 for(const entry of entries){
  const path='laowu:cat_accessories/'+entry.id+'.json',data=outputs.get(path);
  const native=JSON.parse(read(resources+entry.id+'.json'));
  check(!!data,entry.id+' generated');
  check(data.script==='laowu:examples36','runtime marker');
  check(data.item===native.item&&data.exclusive_group===native.exclusive_group&&data.required_outfit===native.required_outfit,'identity/outfit/exclusion');
  for(const [key,value] of Object.entries(native.effects)){
   if(key===entry.scripted){check(data.effects[key]===undefined,'scripted effect cannot double apply');scripted++;}
   else check(data.effects[key]===value,'native knob preserved '+entry.id+'/'+key);
  }
 }
 check(scripted===7,'seven event implementations, other 29 declarative');
 const java=p=>read(loader+'/src/main/java/cn/laowu/mod/'+p+'.java');
 for(const name of ['fastTick','beforeAvoid','beforeHeal','acceptedAttack','acceptedHurt']){
  check(java('accessory/CatAccessoryHooks').includes('"'+name+'"'),'bridge exports '+name);
 }
 check(java('accessory/CatAccessoryItems').includes('.durability(50)'),'native max durability');
 check(/now\(cat\)\s*\+\s*300/.test(java('accessory/CatCommonAccessories')),'15-second native cooldown');
 check(java('api/CatAccessoryHandle').includes('damageDurability'),'public durable handle');
 check(java('accessory/CatCommonAccessories').includes('ShieldLastUse'),'final-use shield survives');
 check(java('accessory/CatAccessoryDurability').includes('inventory.setChanged()'),'durability is saved');
 const ignoreCtx={accessory:()=>({script:'somepack:different'}),outfit:'flight',hasAttacker:()=>true,random:()=>0,fireDamage:true,
   hasLivingAttacker:()=>true,cat:{},other:{},damage:10,amount:5,stack:{id:'laowu:cat_chew_bone'},setDamage:()=>{throw Error('wrong marker modified damage');},
   setAmount:()=>{throw Error('wrong marker modified healing');},extinguish:()=>{throw Error('wrong marker extinguished');}};
 for(const handlers of Object.values(listeners))for(const fn of handlers)fn({context:ignoreCtx,cancel:()=>{throw Error('wrong marker canceled');}});
 check(true,'foreign marker disables all seven event implementations');
}
console.log('PASS: '+checks+' example generation, anti-duplication, API and durability wiring checks');

for(const [label,version,supported,schema,bootstrap,effects,failRegistration,expected] of [
 ['legacy API3',3,undefined,1,true,true,false,36],
 ['future API4 keeps API3',4,true,1,true,true,false,36],
 ['unsupported API4',4,false,1,true,true,false,0],
 ['legacy API2',2,undefined,1,true,true,false,0],
 ['unknown schema',3,true,2,true,true,false,0],
 ['missing bootstrap',3,true,1,false,true,false,0],
 ['missing effects',3,true,1,true,false,false,0],
 ['partial registration',3,true,1,true,true,true,0]
]){
 let emitted=0;
 const api={apiVersion:()=>version,schemaVersion:()=>schema};
 if(supported!==undefined)api.supportsApi=()=>supported;
 const context=vm.createContext({console:{warn:()=>{}},Java:{loadClass:()=>api},
   CatAccessoryEvents:new Proxy({},{get:(_,name)=>()=>{if(failRegistration&&name==='beforeHeal')throw Error('simulated registration failure');}}),
   ServerEvents:{highPriorityData:fn=>fn({addJson:()=>emitted++})}});
 if(bootstrap)vm.runInContext(read(folder+'common/kubejs/server_scripts/laowu36_compat.js'),context);
 vm.runInContext(read(folder+'common/kubejs/server_scripts/laowu36_catalog.js'),context);
 if(effects){try{vm.runInContext(read(folder+'common/kubejs/server_scripts/laowu36_effects.js'),context);}catch(e){if(!failRegistration)throw e;}}
 vm.runInContext(read(folder+'forge/kubejs/server_scripts/laowu36_data.js'),context);
 check(emitted===expected,label+' must emit '+expected+' overrides, got '+emitted);
}
console.log('PASS: 8 compatibility, legacy/future API and fail-safe activation cases');
