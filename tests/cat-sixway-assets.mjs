import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {createRequire} from 'node:module';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const require=createRequire(import.meta.url);
const {PNG}=require('C:/Users/16611/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/pngjs');
for(const loader of ['forge-1.20.1','neoforge-1.21.1']) {
  const base=path.join(root,loader,'src/main/resources/assets/laowu');
  const json=p=>JSON.parse(readFileSync(path.join(base,p),'utf8'));
  for(const id of ['cat_press','cat_mixer','cat_depot','haji_basin']) {
    const variants=json(`blockstates/${id}.json`).variants;
    for(const bottom of ['down','up','north','south','east','west'])assert(Object.keys(variants).some(k=>k.includes(`bottom=${bottom}`)),`${id} missing ${bottom}`);
  }
  for(const id of ['cat_press','cat_mixer']) {
    const source=JSON.parse(readFileSync(path.join(root,'art/cat-machines/sixway',id,'model.bbmodel'),'utf8'));
    const authoredPort=source.elements.find(e=>e.name==='cube_outline');
    assert(authoredPort,`${id} authored port outline missing`);
    const portUv=face=>authoredPort.faces[face].uv.map((v,i)=>v*16/(i%2?source.resolution.height:source.resolution.width));
    const openingPng=PNG.sync.read(readFileSync(path.join(base,`textures/block/${id}_opening.png`)));
    const opaqueUv=uv=>{
      const x0=Math.min(uv[0],uv[2])*openingPng.width/16,x1=Math.max(uv[0],uv[2])*openingPng.width/16;
      const y0=Math.min(uv[1],uv[3])*openingPng.height/16,y1=Math.max(uv[1],uv[3])*openingPng.height/16;
      assert([x0,x1,y0,y1].every(Number.isInteger),`${id} port UV must cover whole source pixels`);
      for(let y=y0;y<y1;y++)for(let x=x0;x<x1;x++)
        assert.equal(openingPng.data[(y*openingPng.width+x)*4+3],255,`${id} opening samples transparent pixel ${x},${y}`);
    };
    const variants=json(`blockstates/${id}.json`).variants;
    for(const [bottom,axis] of [['down','x'],['up','z'],['north','x'],['south','x'],['east','y'],['west','z']]) {
      const closed=variants[`bottom=${bottom},shaft_axis=${axis},shaft_open=false`];
      const open=variants[`bottom=${bottom},shaft_axis=${axis},shaft_open=true`];
      assert.deepEqual(closed,open,`${id} ${bottom}/${axis} must show permanent opposing ports`);
      assert.match(open.model,/_open_[xz]$/,`${id} ${bottom}/${axis} must select a port model`);
    }
    const moving=json(`models/blockbench/${id}_moving.bbmodel`);
    assert(moving.elements.length>=4,'Missing authored moving parts');
    assert(!moving.elements.some(e=>e.uuid==='94929427-fc70-88b8-a6f9-02a2b5302e20'),'Shell duplicated in moving renderer');
    for(const axis of ['x','z']) {
      const elements=json(`models/block/${id}_open_${axis}.json`).elements;
      const sides=axis==='x'?['east','west']:['north','south'];
      const center=axis==='x'?2:0;
      for(const side of sides) {
        const depth=side==='west'||side==='north'?1:15;
        const pane=elements.find(e=>e.from[axis==='x'?0:2]===depth&&e.to[axis==='x'?0:2]===depth
          &&e.from[center]===2&&e.to[center]===14&&e.from[1]===4&&e.to[1]===14
          &&e.faces[side]?.texture==='#opening');
        assert(pane,`${id} ${axis} ${side} opening must sit one model pixel inside the facade`);
        opaqueUv(pane.faces[side].uv);
        const [u0,v0,u1,v1]=portUv('north');
        assert.deepEqual(pane.faces[side].uv,[u0,v1,u1,v0],`${id} ${axis} ${side} must preserve the authored inverted Y winding`);
        // The authored hole spans source rows 1..6. Its center (row 4 boundary)
        // must land on the native shaft center at model Y=8, not Y=10.
        const uv=pane.faces[side].uv;
        const centerY=14-(4/64*16-uv[1])/(uv[3]-uv[1])*10;
        assert.equal(centerY,8,`${id} ${axis} ${side} hole center must match shaft Y=8`);
        const boundary=side==='west'||side==='north'?0:16;
        for(const [lo,hi,y0,y1] of [[0,16,2,4],[0,16,14,16],[0,2,4,14],[14,16,4,14]])
          assert(elements.some(e=>e.from[axis==='x'?0:2]===boundary&&e.to[axis==='x'?0:2]===boundary
            &&e.from[center]===lo&&e.to[center]===hi&&e.from[1]===y0&&e.to[1]===y1
            &&e.faces[side]?.texture==='#opening'),`${id} ${axis} ${side} facade border missing`);
        const near=Math.min(depth,boundary),far=Math.max(depth,boundary);
        for(const [c,normal] of [[2,axis==='x'?'south':'east'],[14,axis==='x'?'north':'west']]) {
          const jamb=elements.find(e=>e.from[axis==='x'?0:2]===near&&e.to[axis==='x'?0:2]===far
            &&e.from[center]===c&&e.to[center]===c&&e.from[1]===4&&e.to[1]===14
            &&e.faces[normal]?.texture==='#opening');
          assert(jamb,`${id} ${axis} ${side} sidewall missing`);
          opaqueUv(jamb.faces[normal].uv);
        }
        for(const [y,normal] of [[4,'up'],[14,'down']]) {
          const jamb=elements.find(e=>e.from[axis==='x'?0:2]===near&&e.to[axis==='x'?0:2]===far
            &&e.from[center]===2&&e.to[center]===14&&e.from[1]===y&&e.to[1]===y
            &&e.faces[normal]?.texture==='#opening');
          assert(jamb,`${id} ${axis} ${side} lintel missing`);
          opaqueUv(jamb.faces[normal].uv);
        }
        assert(!elements.some(e=>e.from[axis==='x'?0:2]===boundary&&e.to[axis==='x'?0:2]===boundary
          &&e.from[center]<=2&&e.to[center]>=14&&e.from[1]<=4&&e.to[1]>=14
          &&e.faces[side]?.texture==='#opening'),`${id} ${axis} ${side} must not be flat across the shaft mouth`);
      }
      for(const side of axis==='x'?['north','south']:['east','west'])
        assert.equal(elements[0].faces[side]?.texture,'#0',`${id} ${axis} must leave perpendicular shell sides closed`);
    }
    const item=json(`models/item/${id}.json`);
    const axle=item.elements.find(e=>e.name==='Inventory shaft');
    assert(axle,`${id} inventory model must include its shaft`);
    assert.deepEqual(axle.from,[0,6,6]);
    assert.deepEqual(axle.to,[16,10,10]);
    assert.equal(item.textures.shaft,'create:block/axis');
    assert.equal(item.textures.shaft_end,'create:block/axis_top');
    for(const depth of [1,15])assert(item.elements.some(e=>e.from[0]===depth&&e.to[0]===depth
      &&e.from[2]===2&&e.to[2]===14&&e.from[1]===4&&e.to[1]===14
      &&e.faces[depth===1?'west':'east']?.texture==='#opening'),`${id} item needs default X opposing openings`);
    if(id==='cat_mixer')assert.deepEqual(item.display.gui,{rotation:[30,225,0],translation:[0,2.25,0],scale:[.45,.45,.45]});
    assert.deepEqual(readFileSync(path.join(base,`textures/block/${id}.png`)),readFileSync(path.join(root,'art/cat-machines/sixway',id,'texture.png')),'Authored texture changed');
  }
}
console.log('PASS: six-way states, opposing ports, moving geometry and original textures');
