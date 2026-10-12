import {readFileSync,writeFileSync,copyFileSync,readdirSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {createRequire} from 'node:module';
import {execFileSync} from 'node:child_process';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..'),out=path.join(root,'art/maodie-fighter-v11');
const require=createRequire(import.meta.url),sharp=require('C:/Users/16611/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const clips=JSON.parse(readFileSync(path.join(out,'clips.json'),'utf8'));
const data=clips.map(c=>({...c,frames:readdirSync(path.join(out,'previews',c.name)).filter(f=>f.endsWith('.png')).sort().map(f=>'previews/'+c.name+'/'+f)}));
writeFileSync(path.join(out,'preview-data.js'),'window.MAODIE_PREVIEW='+JSON.stringify(data)+';\n');
copyFileSync(path.join(root,'tools/maodie-fighter-preview.html'),path.join(out,'preview.html'));
for(const name of ['combat_flow','combo','combo_front','combo_side']){
 const clip=data.find(c=>c.name===(name.startsWith('combo')?'combo':name)),fps=(clip.frames.length-1)/clip.length;
 execFileSync('ffmpeg',['-hide_banner','-loglevel','error','-y','-framerate',String(fps),'-i',path.join(out,'previews',name,'%03d.png'),'-f','lavfi','-i','color=c=0xe0e3d8:s=720x720:r=60','-filter_complex','[1:v][0:v]overlay=shortest=1:format=auto,format=yuv420p','-t',String(clip.length),'-r','60','-c:v','libx264','-crf','18','-movflags','+faststart',path.join(out,'previews',name+'.mp4')]);
 console.log('Packaged video',name);
}
for(const clip of data.filter(c=>c.name!=='guard')){
 const frames=[];let width,height;
 for(const src of clip.frames){const r=await sharp(path.join(out,src)).ensureAlpha().raw().toBuffer({resolveWithObject:true});width=r.info.width;height=r.info.height;frames.push(r.data)}
 await sharp(Buffer.concat(frames),{limitInputPixels:width*height*frames.length,raw:{width,height:height*frames.length,channels:4,pageHeight:height}}).gif({loop:0,delay:clip.frames.map((_,i)=>i===clip.frames.length-1?350:Math.round(clip.length/(clip.frames.length-1)*400)*10),effort:3}).toFile(path.join(out,'previews',clip.name+'.gif'));
 console.log('Packaged slow-motion GIF',clip.name,frames.length,'frames');
}
