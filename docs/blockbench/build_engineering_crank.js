(() => {
  if (Project.name !== '工程猫摇曲柄') throw new Error('Select the dedicated engineer project');
  if (Outliner.root.length) throw new Error('Create a fresh dedicated project before rebuilding');
  const fs = require('fs');
  const root = 'D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/Create-Meowchanics-develop';
  const read = p => JSON.parse(fs.readFileSync(root + '/' + p, 'utf8'));
  const rig = read('docs/blockbench/cat_pipa_performance.bbmodel');
  const names = ['head','body','left_hind_leg','right_hind_leg','left_front_leg','right_front_leg','tail1','tail2'];
  const ids = new Set(rig.groups.filter(g => names.includes(g.name)).map(g => g.uuid));
  rig.groups = rig.groups.filter(g => ids.has(g.uuid));
  rig.outliner = rig.outliner.filter(g => ids.has(g.uuid));
  const kept = new Set(rig.outliner.flatMap(g => g.children));
  rig.elements = rig.elements.filter(e => kept.has(e.uuid));
  rig.textures = rig.textures.slice(0,1);
  rig.animations = [];
  rig.name = '工程猫摇曲柄';
  rig.save_path = '';
  const outfit = read('forge-1.20.1/src/main/resources/assets/laowu/models/entity/engineering_suit.bbmodel');
  rig.textures.push({...outfit.textures[1],source:'data:image/png;base64,' + fs.readFileSync(root + '/forge-1.20.1/src/main/resources/assets/laowu/textures/entity/engineering_suit.png').toString('base64')});
  const clothing = outfit.elements.filter(e => Object.values(e.faces).some(f => f.texture === 1));
  const clothingIds = new Set(clothing.map(e => e.uuid));
  rig.elements.push(...clothing);
  const originalHead = outfit.groups.find(g => g.name === 'head');
  const headTree = outfit.outliner.find(g => g.uuid === originalHead.uuid);
  const targetHead = rig.outliner.find(g => g.uuid === rig.groups.find(x => x.name === 'head').uuid);
  targetHead.children.push(...headTree.children.filter(id => clothingIds.has(id)));
  const extra = outfit.groups.filter(g => !names.includes(g.name));
  rig.groups.push(...extra);
  const rootGroup = extra.find(g => g.name === 'group');
  rig.outliner.push(outfit.outliner.find(g => g.uuid === rootGroup.uuid));
  Codecs.project.parse(rig);
  Project.name = '工程猫摇曲柄';
  Formats.free.select();
  const V = (x,y,z) => new THREE.Vector3(x,y,z);
  const Q = (x=0,y=0,z=0) => new THREE.Quaternion().setFromEuler(new THREE.Euler(x,y,z,'ZYX'));
  const centerY = 17.784, seatTop = 1.784, samples = 180, duration = 3, bodyBackOffset = 4, standLift = 4;
  const textures = new Map();
  function tex(id) {
    if (textures.has(id)) return textures.get(id);
    const p = 'tests/build/engineer-reference/textures/' + id.slice('create:'.length) + '.png';
    const t = new Texture({name:id.split('/').pop()+'.png',width:16,height:16})
      .fromDataURL('data:image/png;base64,' + fs.readFileSync(root + '/' + p).toString('base64')).add(false);
    textures.set(id,t);
    return t;
  }
  function importBlock(model,group,offset,overrides={}) {
    const palette = {...model.textures,...overrides};
    for (const [i,e] of model.elements.entries()) {
      const c = new Cube({name:group.name+'_'+(e.name||i),from:e.from.map((v,j)=>v+offset[j]),
        to:e.to.map((v,j)=>v+offset[j]),origin:e.rotation ? e.rotation.origin.map((v,j)=>v+offset[j]) : group.origin.slice(),
        rotation:e.rotation ? ['x','y','z'].map(a=>a===e.rotation.axis ? e.rotation.angle : 0) : [0,0,0],
        box_uv:false,autouv:0}).addTo(group).init();
      for (const [face,f] of Object.entries(c.faces)) {
        const raw=e.faces[face];
        if (!raw) { f.texture=null; continue; }
        let id=raw.texture;
        for(let n=0;id.startsWith('#') && n<8;n++) id=palette[id.slice(1)];
        f.texture=tex(id).uuid; f.uv=raw.uv.slice(); f.rotation=raw.rotation||0;
      }
    }
  }
  const seat = new Group({name:'reference_seat',origin:[0,0,0]}).init();
  importBlock(read('tests/build/engineer-reference/models/block/seat.json'),seat,[-8,-6.216,-8],
    {'1':'create:block/seat/top_white','2':'create:block/seat/side_white'});
  const orbit = new Group({name:'reference_crank_turn',origin:[0,centerY,0]}).init();
  const handle = new Group({name:'reference_crank_handle',origin:[0,centerY,0],rotation:[0,90,0]}).addTo(orbit).init();
  importBlock(read('tests/build/engineer-reference/models/block/hand_crank/handle.json'),handle,[-8,centerY-8,-8]);
  const shaft = new Group({name:'reference_crank_shaft',origin:[0,centerY,0],rotation:[90,-90,0]}).init();
  importBlock(read('tests/build/engineer-reference/models/block/hand_crank/block.json'),shaft,[-8,centerY-8,-8]);
  const wall = new Group({name:'reference_support',origin:[0,0,0]}).init();
  function support(name,from,to) {
    const c = new Cube({name,from,to,box_uv:false,autouv:0}).addTo(wall).init();
    for(const f of Object.values(c.faces)){f.texture=tex('create:block/smooth_dark_log_top').uuid;f.uv=[0,0,16,16];}
  }
  support('seat_support',[-8,-22.216,-8],[8,-6.216,8]);
  support('crank_support',[8,-6.216,-8],[24,25.784,8]);
  Canvas.updateAll();
  const bones=Object.fromEntries(names.map(n=>[n,Group.all.find(g=>g.name===n)]));
  const pack=Group.all.find(g=>g.name==='group');
  for (const name of ['group2','group3']) {
    const box = Group.all.find(g=>g.name===name);
    box.visibility = true;
    for (const e of box.children) e.visibility = true;
  }
  const animation=new Animation({name:'animation.cat.engineering_crank',loop:'loop',length:duration,snapping:60}).add();
  animation.select();
  const tracks=Object.fromEntries([...names,'group','reference_crank_turn'].map(n=>[n,[]]));
  const diagnostics=[];
  function record(name,position,q,scale=[1,1,1]){
    const group=name==='group'?pack:name==='reference_crank_turn'?orbit:bones[name];
    const e=new THREE.Euler().setFromQuaternion(q,'ZYX');
    tracks[name].push({position:position.clone().sub(V(...group.origin)).toArray(),
      rotation:[e.x,e.y,e.z].map(a=>a*180/Math.PI),scale});
  }
  function link(name,from,to,length,offset=1){
    const d=to.clone().sub(from),q=new THREE.Quaternion().setFromUnitVectors(V(0,-1,0),d.clone().normalize());
    record(name,from.clone().sub(V(0,0,offset).applyQuaternion(q)),q,[1,d.length()/length,1]);
  }
  for(let i=0;i<=samples;i++){
    const phase=i/samples*Math.PI*2;
    const leftGrip=V(-4,centerY+7*Math.sin(phase),7*Math.cos(phase));
    const rightGrip=V(-1,centerY+7*Math.sin(phase),7*Math.cos(phase));
    const cycle=(i/samples+0.25)%1;
    const rise=cycle<0.5?cycle*2:(1-cycle)*2;
    const stand=rise*rise*rise*(10+rise*(-15+6*rise));
    const torsoHeight=0.72;
    const torsoQ=Q(-0.10-0.065*Math.cos(phase),0,0.025*Math.sin(phase));
    const torso=V(-4.5,17.45+(1.7+standLift)*stand,9.3+bodyBackOffset+3.6*Math.cos(phase));
    const local=(x,y,z)=>V(x,y*torsoHeight,z).applyQuaternion(torsoQ).add(torso);
    record('body',torso,torsoQ,[1,torsoHeight,1]);
    record('head',local(-0.35,-0.1,-1.4).sub(V(0,0,bodyBackOffset)),Q(0.08-0.12*Math.sin(phase),-0.50,-0.04));
    const left=local(-2.0,-3.6,-4.3), right=local(2.0,-3.6,-4.3);
    link('left_front_leg',left,leftGrip,10);
    link('right_front_leg',right,rightGrip,10);
    const hips=[local(-1.45,-17,-4.3),local(1.45,-17,-4.3)];
    for(let side=0;side<2;side++){
      const toe=V(side?-2.4:-6.6,seatTop+1.1,5.3);
      for(let pass=0;pass<12;pass++){
        const q=new THREE.Quaternion().setFromUnitVectors(V(0,-1,0),toe.clone().sub(hips[side]).normalize());
        toe.y=seatTop+Math.abs(V(1,0,0).applyQuaternion(q).y)+Math.abs(V(0,0,1).applyQuaternion(q).y)+0.04;
      }
      link(side?'right_hind_leg':'left_hind_leg',hips[side],toe,6,2);
    }
    const tailRoot=local(0,-17.7,-2),tailMid=V(-4.7,seatTop+1.2,10.4),tailEnd=V(-1.1,seatTop+1.0,13.7+0.25*Math.sin(phase));
    link('tail1',tailRoot,tailMid,8,0.5);
    link('tail2',tailMid,tailEnd,8,0.5);
    const packLocal=V(...pack.origin).sub(V(0,12,-10)).applyQuaternion(Q(Math.PI/2));
    packLocal.multiply(V(1,torsoHeight,1)).applyQuaternion(torsoQ).add(torso);
    record('group',packLocal,torsoQ.clone().multiply(Q(Math.PI/2)),[1,1,torsoHeight]);
    record('reference_crank_turn',V(0,centerY,0),Q(-phase));
    diagnostics.push({phase:i/samples,leftGrip:leftGrip.toArray(),rightGrip:rightGrip.toArray(),stand,torso:torso.toArray(),torsoHeight});
  }
  for(const [name,frames] of Object.entries(tracks)){
    const g=name==='group'?pack:name==='reference_crank_turn'?orbit:bones[name];
    const animator=animation.getBoneAnimator(g);
    for(let i=1;i<frames.length;i++)for(let axis=0;axis<3;axis++){
      while(frames[i].rotation[axis]-frames[i-1].rotation[axis]>180)frames[i].rotation[axis]-=360;
      while(frames[i].rotation[axis]-frames[i-1].rotation[axis]<-180)frames[i].rotation[axis]+=360;
    }
    frames.forEach((frame,i)=>{
      for(const channel of ['position','rotation','scale']){
        const [x,y,z]=frame[channel];
        animator.addKeyframe({channel,time:i/60,data_points:[{x,y,z}],interpolation:'linear'});
      }
    });
  }
  window.__catEngineer={names,tracks,diagnostics,centerY,seatTop,samples,duration,bodyBackOffset,standLift};
  Modes.options.animate.select();
  Timeline.setTime(0);Animator.preview();Canvas.updateVisibility();
  return JSON.stringify({project:Project.name,frames:samples+1,bones:names.length,cubes:Cube.all.length,crankCenter:[0,centerY,0],seatTop});
})()
