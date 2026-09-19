// Generated deliverables only; all authored docs/scripts live in source and are copied unchanged.
import {readFileSync,writeFileSync,mkdirSync,copyFileSync,readdirSync,statSync} from 'node:fs';
import {createHash} from 'node:crypto';
import path from 'node:path';
import vm from 'node:vm';
import {fileURLToPath} from 'node:url';
const root=fileURLToPath(new URL('../',import.meta.url));
const out=path.join(root,'build','sdk');
const stamp=Date.now().toString(36);
const stage=path.join(out,stamp,'cat-accessory-kubejs-sdk-1.0.0');
function cpSync(source,target){
 if(statSync(source).isDirectory()){
   mkdirSync(target,{recursive:true});
   for(const name of readdirSync(source))cpSync(path.join(source,name),path.join(target,name));
 }else{mkdirSync(path.dirname(target),{recursive:true});copyFileSync(source,target);}
}
mkdirSync(stage,{recursive:true});
cpSync(path.join(root,'docs/kubejs-sdk'),stage,{recursive:true});
mkdirSync(path.join(stage,'contracts'),{recursive:true});
cpSync(path.join(root,'compatibility/cat-accessory-api-v3.json'),path.join(stage,'contracts/api-v3.json'));
for(const loader of ['forge','neoforge']){
 const target=path.join(stage,'replacement',loader,'kubejs');
 cpSync(path.join(root,'docs/examples/cat-accessories-36/common/kubejs'),target,{recursive:true});
 cpSync(path.join(root,'docs/examples/cat-accessories-36',loader,'kubejs'),target,{recursive:true});
}
const sandbox=vm.createContext({});
vm.runInContext(readFileSync(path.join(root,'docs/examples/cat-accessories-36/common/kubejs/server_scripts/laowu36_catalog.js'),'utf8'),sandbox);
const entries=vm.runInContext('laowuExamples36.entries',sandbox);
if(entries.length!==36)throw Error('Expected 36 catalog entries');
mkdirSync(path.join(stage,'catalog'),{recursive:true});
writeFileSync(path.join(stage,'catalog/accessories36.json'),JSON.stringify({format_version:1,count:36,scripted_count:7,engine_definition_count:29,items:entries},null,2)+'\n');
function files(dir){
 return readdirSync(dir).sort().flatMap(name=>{const p=path.join(dir,name);return statSync(p).isDirectory()?files(p):[p];});
}
const manifest={sdk_version:'1.0.0',contract:'laowu:cat_accessory_api_v3',schema_version:1,tested_build:'accessories.37',
 catalog_count:36,replacement_files_per_loader:4,includes_mod_jars:false,files:files(stage).map(p=>({path:path.relative(stage,p).replaceAll('\\','/'),
 bytes:statSync(p).size,sha256:createHash('sha256').update(readFileSync(p)).digest('hex').toUpperCase()}))};
writeFileSync(path.join(stage,'manifest.json'),JSON.stringify(manifest,null,2)+'\n');
console.log(JSON.stringify({stage,files:manifest.files.length,catalog:entries.length}));
