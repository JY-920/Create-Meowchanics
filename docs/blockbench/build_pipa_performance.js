(() => {
  if (Project.name !== '琵琶行猫') throw new Error('Select the dedicated pipa project first');
  if (Format.id !== 'free') Formats.free.select();
  for (const name of ['pipa', 'plectrum']) {
    const old = Group.all.find(g => g.name === name);
    if (old) old.remove();
  }
  for (const a of [...Animation.all]) a.remove();
  const palette = ['#322019','#68452a','#ab7946','#d7b673','#f3dfac','#1d1816','#94613c','#483023'];
  let texture = Texture.all.find(t => t.name === 'cat_pipa.png');
  if (!texture) {
    const canvas = document.createElement('canvas');
    canvas.width = 64; canvas.height = 32;
    const ctx = canvas.getContext('2d');
    for (let i = 0; i < palette.length; i++) {
      ctx.fillStyle = palette[i]; ctx.fillRect(i * 8, 0, 8, 32);
    }
    texture = new Texture({name:'cat_pipa.png',width:64,height:32}).fromDataURL(canvas.toDataURL()).add(false);
  }
  const V = (x,y,z) => new THREE.Vector3(x,y,z);
  const pipa = new Group({name:'pipa',origin:[0,2.5,-5.4],rotation:[0,0,0]}).init();
  const pick = new Group({name:'plectrum',origin:[0,0,0],rotation:[0,0,0]}).init();
  function cube(name,from,to,color,group=pipa,rotation=[0,0,0],pivot) {
    const base = group.origin;
    const c = new Cube({name,from:from.map((v,i)=>v+base[i]),to:to.map((v,i)=>v+base[i]),
      origin:pivot ? pivot.map((v,i)=>v+base[i]) : base.slice(),rotation,box_uv:false,autouv:0}).addTo(group).init();
    c.box_uv = false;
    for (const [face,f] of Object.entries(c.faces)) {
      const shade = face === 'north' || color === 4 || color === 3 ? color : color === 1 ? 0 : color;
      f.texture = texture.uuid; f.uv = [shade*8+2,2,shade*8+5,5];
    }
    return c;
  }
  const widths = [2.0,2.8,3.4,3.7,3.7,3.3,2.8,2.1,1.45];
  for (let y=0;y<widths.length;y++) {
    const w=widths[y];
    cube('pipa_rim_'+y,[-w,y,-1.0],[w,y+1,0.7],2);
    cube('pipa_soundboard_'+y,[-w+0.3,y,-1.12],[w-0.3,y+1,-0.97],1);
  }
  cube('pipa_neck',[-0.8,8.7,-0.6],[0.8,16.6,0.5],0);
  cube('pipa_fingerboard',[-0.6,8.6,-0.75],[0.6,16.6,-0.58],5);
  cube('pipa_headstock',[-0.9,16.1,-0.5],[0.9,18.4,0.6],2);
  cube('pipa_headstock_crown',[-1.1,18.0,-0.4],[1.1,18.6,0.7],0);
  for (let i=0;i<4;i++) {
    const side=i%2===0 ? -1 : 1,y=16.5+Math.floor(i/2)*0.85;
    cube('pipa_peg_'+i,[side<0 ? -1.7 : 0.7,y,-0.05],[side<0 ? -0.7 : 1.7,y+0.35,0.5],3);
    cube('pipa_string_'+i,[-0.45+i*0.30,1.5,-1.31],[-0.36+i*0.30,16.8,-1.22],4);
  }
  for (let i=0;i<6;i++) cube('pipa_fret_'+i,[-0.82,9.1+i*1.10,-1.05],[0.82,9.26+i*1.10,-0.70],3);
  cube('pipa_bridge',[-1.0,1.1,-1.4],[1.0,1.65,-0.9],0);
  cube('pipa_sound_hole_left',[-2.35,4.8,-1.18],[-1.65,5.5,-1.13],5);
  cube('pipa_sound_hole_right',[1.65,4.8,-1.18],[2.35,5.5,-1.13],5);
  cube('pick_grip',[-0.22,-0.55,-0.18],[0.22,0.25,0.18],0,pick);
  cube('pick_blade',[-0.75,-1.15,-0.16],[0.75,0.10,0.16],4,pick,[0,0,30],[0,-0.2,0]);
  Canvas.updateAll();
  const names=['head','body','left_hind_leg','right_hind_leg','left_front_leg','right_front_leg','tail1','tail2','pipa','plectrum'];
  const bones=Object.fromEntries(names.map(n=>[n,Group.all.find(g=>g.name===n)]));
  if (names.some(n=>!bones[n])) throw new Error('Incomplete cat rig');
  const duration=3.2, samples=192;
  const animation=new Animation({name:'animation.cat.pipa_performance',loop:'loop',length:duration,snapping:60}).add();
  animation.select();
  const tracks=Object.fromEntries(names.map(n=>[n,[]]));
  const diagnostics=[];
  function record(name,position,q,scale=[1,1,1]) {
    const e=new THREE.Euler().setFromQuaternion(q,'ZYX');
    tracks[name].push({position:position.clone().sub(V(...bones[name].origin)).toArray(),
      rotation:[e.x,e.y,e.z].map(a=>a*180/Math.PI),scale});
  }
  function link(name,from,to,length,offset=1) {
    const d=to.clone().sub(from),q=new THREE.Quaternion().setFromUnitVectors(V(0,-1,0),d.clone().normalize());
    record(name,from.clone().sub(V(0,0,offset).applyQuaternion(q)),q,[1,d.length()/length,1]);
  }
  for(let i=0;i<=samples;i++) {
    const phase=i/samples*Math.PI*2, sway=Math.sin(phase)*0.035;
    const torsoQ=new THREE.Quaternion().setFromEuler(new THREE.Euler(-0.06,0,sway,'ZYX'));
    const torso=V(0,18.0,5.0);
    const local=(x,y,z)=>V(x,y*0.80,z).applyQuaternion(torsoQ).add(torso);
    record('body',torso,torsoQ,[1,0.80,1]);
    const headQ=new THREE.Quaternion().setFromEuler(new THREE.Euler(0.10+0.06*Math.sin(phase*4),-0.13+0.05*Math.sin(phase),sway,'ZYX'));
    record('head',local(0,-0.2,-6.0),headQ);
    const instrumentQ=new THREE.Quaternion().setFromEuler(new THREE.Euler(-0.055,0,0.27+sway,'ZYX'));
    const instrumentRoot=V(0.6+Math.sin(phase)*0.10,2.65,-5.6);
    const onInstrument=(x,y,z)=>V(x,y,z).applyQuaternion(instrumentQ).add(instrumentRoot);
    record('pipa',instrumentRoot,instrumentQ);
    const fret=onInstrument(-0.55,12.1+0.6*Math.sin(phase*2),-0.80);
    const strum=onInstrument(0.8+1.05*Math.sin(phase*4),4.8+0.65*Math.cos(phase*4),-2.10);
    const leftShoulder=local(-2.05,-3.3,-4.7), rightShoulder=local(2.05,-3.3,-4.7);
    link('left_front_leg',leftShoulder,fret,10);
    link('right_front_leg',rightShoulder,strum,10);
    record('plectrum',strum.clone().add(V(-0.20,-0.3,-0.1)),
      new THREE.Quaternion().setFromEuler(new THREE.Euler(0.10,0.10,0.3+0.35*Math.sin(phase*4),'ZYX')));
    const leftHip=local(-1.5,-17,-4),rightHip=local(1.5,-17,-4);
    for(const [name,hip,x] of [['left_hind_leg',leftHip,-3.4],['right_hind_leg',rightHip,3.4]]) {
      const toe=V(x,1.2,-3.7);
      for(let pass=0;pass<12;pass++) {
        const q=new THREE.Quaternion().setFromUnitVectors(V(0,-1,0),toe.clone().sub(hip).normalize());
        toe.y=Math.abs(V(1,0,0).applyQuaternion(q).y)+Math.abs(V(0,0,1).applyQuaternion(q).y)+0.01;
      }
      link(name,hip,toe,6,2);
    }
    const tailStart=local(0,-17.5,-2),tailMid=V(3.8,1.35,6.4),tailEnd=V(7.7,1.35,2.0+0.30*Math.sin(phase));
    link('tail1',tailStart,tailMid,8,0.5);
    link('tail2',tailMid,tailEnd,8,0.5);
    diagnostics.push({time:i/60,fret:fret.toArray(),strum:strum.toArray()});
  }
  for(const name of names) {
    const animator=animation.getBoneAnimator(bones[name]);
    tracks[name].forEach((frame,i)=>{
      for(const channel of ['position','rotation','scale']) {
        const [x,y,z]=frame[channel];
        animator.addKeyframe({channel,time:i/60,data_points:[{x,y,z}],interpolation:'linear'});
      }
    });
  }
  window.__catPipa={tracks,diagnostics,names,duration,samples};
  Modes.options.animate.select();
  Timeline.setTime(0); Animator.preview();
  return JSON.stringify({name:Project.name,bones:names.length,cubes:Cube.all.length,seconds:duration,frames:samples+1});
})()
