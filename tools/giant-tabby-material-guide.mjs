// Lossless reference presentation only, not a replacement texture or repaint.
import fs from 'node:fs';
import path from 'node:path';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
const {chromium}=require('C:/Users/16611/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const root=path.resolve(import.meta.dirname,'..');
const out=path.join(root,'build/giant-tabby-v2');
fs.mkdirSync(out,{recursive:true});
const data=fs.readFileSync('C:/Users/16611/Downloads/大猫猫/yellow_cat.png').toString('base64');
const browser=await chromium.launch({headless:true,executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'});
try{
 const page=await browser.newPage({viewport:{width:2048,height:1024},deviceScaleFactor:1});
 await page.setContent(`<style>html,body{margin:0;background:transparent}img{display:block;width:2048px;height:1024px;image-rendering:pixelated}</style><img src="data:image/png;base64,${data}">`);
 await page.evaluate(()=>Promise.all([...document.images].map(i=>i.decode())));
 await page.screenshot({path:path.join(out,'source-atlas-guide.png'),omitBackground:true});
 console.log(path.join(out,'source-atlas-guide.png'));
}finally{await browser.close()}
