import http from 'node:http';
import {readFile} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../art/maodie-fighter-v11');
const mime={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.png':'image/png','.gif':'image/gif','.mp4':'video/mp4','.bbmodel':'application/json','.json':'application/json','.md':'text/plain; charset=utf-8'};
const server=http.createServer(async(req,res)=>{
 try{const name=decodeURIComponent(new URL(req.url,'http://localhost').pathname).replace(/^\/+/,''),file=path.resolve(root,name||'preview.html');if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}const data=await readFile(file),headers={'Content-Type':mime[path.extname(file)]||'application/octet-stream','Cache-Control':'no-store','Accept-Ranges':'bytes'};const range=req.headers.range?.match(/^bytes=(\d*)-(\d*)$/);if(range){const start=range[1]?Number(range[1]):Math.max(0,data.length-Number(range[2])),end=range[1]&&range[2]?Math.min(Number(range[2]),data.length-1):data.length-1;if(start>end||start>=data.length){res.writeHead(416,{'Content-Range':'bytes */'+data.length});return res.end()}res.writeHead(206,{...headers,'Content-Range':`bytes ${start}-${end}/${data.length}`,'Content-Length':end-start+1});return res.end(data.subarray(start,end+1))}res.writeHead(200,{...headers,'Content-Length':data.length});res.end(data)}catch{res.writeHead(404);res.end('Not found')}
});
server.listen(0,'127.0.0.1',()=>console.log('Maodie art preview: http://127.0.0.1:'+server.address().port+'/preview.html'));
