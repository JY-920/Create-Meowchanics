(() => {
  for (const side of ['left','right']) {
    const name=side+'_front_leg',lowerName=side+'_front_paw';
    const g=Group.all.find(g=>g.name===name),c=g.children.find(e=>e instanceof Cube);
    const lower=Group.all.find(g=>g.name===lowerName);
    if(lower) {
      for(const a of Animation.all)delete a.animators[lower.uuid];
      lower.remove();
    }
    c.from[1]=g.origin[1]-10;c.to[1]=g.origin[1];
    c.box_uv=true;c.uv_offset=[40,0];
  }
  Canvas.updateAll();
  const V=(x,y,z)=>new THREE.Vector3(x,y,z);
  const duration=2.4,count=144;
  const old=Animation.all.find(a=>a.name==='animation.cat.thomas_flare');
  if(old)old.remove();
  for(const a of Animation.all)a.playing=false;
  const animation=new Animation({name:'animation.cat.thomas_flare',loop:'loop',length:duration,snapping:60}).add();
  animation.select();
  const bones=Object.fromEntries(Group.all.map(g=>[g.name,g]));
  const tracks=Object.fromEntries(Object.keys(bones).map(n=>[n,[]]));
  const leftKicks=[[-0.92,0.08,0.40],[-0.65,-0.10,0.70],[-0.20,-0.25,0.95],[-0.75,0.20,0.35],[-0.85,0.65,-0.35],[-0.65,0.68,-0.40],[-0.18,0.97,-0.35],[-0.60,0.55,-0.55]];
  const rightKicks=[[0.92,0.08,0.40],[0.60,0.55,-0.55],[0.18,0.97,-0.35],[0.65,0.68,-0.40],[0.85,0.65,-0.35],[0.75,0.20,0.35],[0.20,-0.25,0.95],[0.65,-0.10,0.70]];
  const smooth=x=>{x=Math.max(0,Math.min(1,x));return x*x*(3-2*x);};
  const guides=[];
  for(let i=0;i<8;i++) {
    const phase=i*Math.PI/4;
    const down=V(10.5*Math.sin(phase),-0.5+2.3*Math.cos(phase),10.5*Math.cos(phase)).normalize();
    const lateral=V(1,0,0).addScaledVector(down,-down.x).normalize();
    const up=down.clone().negate(),normal=new THREE.Vector3().crossVectors(lateral,up).normalize();
    guides.push(new THREE.Quaternion().setFromRotationMatrix(new THREE.Matrix4().makeBasis(lateral,up,normal)));
  }
  function closest(q,reference) {
    q=q.clone();
    if(q.dot(reference)<0)q.set(-q.x,-q.y,-q.z,-q.w);
    return q;
  }
  function log(q) {
    const a=Math.acos(Math.max(-1,Math.min(1,q.w))),s=Math.sin(a);
    return Math.abs(s)<1e-8?V(0,0,0):V(q.x,q.y,q.z).multiplyScalar(a/s);
  }
  function exp(v) {
    const a=v.length(),s=a<1e-8?1:Math.sin(a)/a;
    return new THREE.Quaternion(v.x*s,v.y*s,v.z*s,Math.cos(a)).normalize();
  }
  const controls=guides.map((q,i)=>{
    const inv=q.clone().invert();
    const a=log(inv.clone().multiply(closest(guides[(i+7)%8],q)));
    const b=log(inv.clone().multiply(closest(guides[(i+1)%8],q)));
    return q.clone().multiply(exp(a.add(b).multiplyScalar(-0.25)));
  });
  function torsoRotation(t) {
    const p=(t%1)*8,i=Math.floor(p),u=p-i,j=(i+1)%8;
    const a=guides[i].clone().slerp(guides[j],u);
    const b=controls[i].clone().slerp(controls[j],u);
    return a.slerp(b,2*u*(1-u)).normalize();
  }
  function curve(keys,t) {
    const k=t*keys.length,i=Math.floor(k),u=k-i;
    const p0=keys[(i+keys.length-1)%keys.length],p1=keys[i%keys.length],p2=keys[(i+1)%keys.length],p3=keys[(i+2)%keys.length];
    return V(...p1.map((v,j)=>0.5*((2*v)+(-p0[j]+p2[j])*u+(2*p0[j]-5*v+4*p2[j]-p3[j])*u*u+(-p0[j]+3*v-3*p2[j]+p3[j])*u*u*u)));
  }
  function continuousEuler(q,previous) {
    const e=new THREE.Euler().setFromQuaternion(q,'ZYX');
    const first=[e.x,e.y,e.z],second=[e.x+Math.PI,Math.PI-e.y,e.z+Math.PI];
    if(!previous)return first;
    const candidates=[first,second].map(r=>r.map((v,j)=>{
      while(v-previous[j]>Math.PI)v-=2*Math.PI;
      while(v-previous[j]<-Math.PI)v+=2*Math.PI;
      return v;
    }));
    const distance=r=>r.reduce((sum,v,j)=>sum+(v-previous[j])**2,0);
    return distance(candidates[0])<=distance(candidates[1])?candidates[0]:candidates[1];
  }
  const previousAngles={};
  function record(name,pos,q,scale) {
    const angles=continuousEuler(q,previousAngles[name]);previousAngles[name]=angles;
    tracks[name].push({position:pos.clone().sub(V(...bones[name].origin)).toArray(),rotation:angles.map(v=>v*180/Math.PI),scale:scale||[1,1,1]});
  }
  function link(name,from,to,size,offset) {
    const diff=to.clone().sub(from),len=diff.length();
    const q=new THREE.Quaternion().setFromUnitVectors(V(0,-1,0),diff.normalize());
    const root=from.clone().sub(V(0,0,offset).applyQuaternion(q));
    record(name,root,q,[1,len/size,1]);
  }
  const diagnostics=[];
  for(let i=0;i<=count;i++) {
    const t=i/count,phase=t*Math.PI*2,si=Math.sin(phase),co=Math.cos(phase);
    const front=smooth((0.2-co)/1.2),lowering=1.8*front+1.6*si*si;
    const headAnchor=V(-2.8*si,9.0+0.7*si*si,-co);
    const shoulder=headAnchor.clone().add(V(0,-lowering,0));
    const q=torsoRotation(t),down=V(0,-1,0).applyQuaternion(q),lateral=V(1,0,0).applyQuaternion(q),normal=V(0,0,1).applyQuaternion(q);
    const hip=shoulder.clone().addScaledVector(down,12);
    const bodyRoot=shoulder.clone().sub(V(0,-2.25,-5).applyQuaternion(q));
    record('body',bodyRoot,q,[1,0.75,1]);
    const neck=headAnchor.clone().addScaledVector(down,-2.5).add(V(0,1.4,0));
    const headQ=new THREE.Quaternion().setFromEuler(new THREE.Euler(0.12+0.20*co,0.22*si,-0.20*si,'ZYX'));
    record('head',neck,headQ);
    const paws=[];
    for(const [name,side] of [['left_front_leg',-1],['right_front_leg',1]]) {
      const shoulderJoint=shoulder.clone().addScaledVector(lateral,side*2.6).addScaledVector(down,-0.5);
      const lift=smooth((si*side-0.25)/0.75);
      const raisedDirection=lateral.clone().multiplyScalar(side*0.9).add(V(0,0.1,-0.55)).normalize();
      const liftedPaw=shoulderJoint.clone().addScaledVector(raisedDirection,8.5);
      const planted=V(side*4.8,0.4,0);
      for(let pass=0;pass<8;pass++) {
        const rq=new THREE.Quaternion().setFromUnitVectors(V(0,-1,0),planted.clone().sub(shoulderJoint).normalize());
        planted.y=Math.abs(V(1,0,0).applyQuaternion(rq).y)+Math.abs(V(0,0,1).applyQuaternion(rq).y);
      }
      const paw=planted.lerp(liftedPaw,lift);
      link(name,shoulderJoint,paw,10,1);paws.push(paw.toArray());
    }
    for(const [name,side,keys] of [['left_hind_leg',-1,leftKicks],['right_hind_leg',1,rightKicks]]) {
      const hipJoint=hip.clone().addScaledVector(lateral,side*2.3);
      let direction=curve(keys,t).normalize();
      const outward=direction.dot(lateral)*side;
      if(outward<0.45)direction.addScaledVector(lateral,side*(0.45-outward)).normalize();
      const toe=hipJoint.clone().addScaledVector(direction,8.1);
      if(toe.y<1.6)toe.y=1.6;
      link(name,hipJoint,toe,6,2);
    }
    const tailStart=hip.clone().addScaledVector(normal,1.6).addScaledVector(down,0.2);
    const tailDir=V(down.x*0.7-co*0.65,0.12,down.z*0.7+si*0.65).normalize();
    const tailMid=tailStart.clone().addScaledVector(tailDir,6.5);
    const tailEnd=tailMid.clone().addScaledVector(V(-co*0.8-down.x*0.25,0.2,si*0.8-down.z*0.25).normalize(),6.5);
    link('tail1',tailStart,tailMid,8,0.5);link('tail2',tailMid,tailEnd,8,0.5);
    diagnostics.push({time:t*duration,shoulder:shoulder.toArray(),hip:hip.toArray(),paws,lowering});
  }
  for(const [name,frames] of Object.entries(tracks)) {
    const a=animation.getBoneAnimator(bones[name]);
    for(let i=0;i<frames.length;i++)for(const channel of ['position','rotation','scale']) {
      const v=frames[i][channel];
      a.addKeyframe({channel,time:i/count*duration,data_points:[{x:v[0],y:v[1],z:v[2]}],interpolation:'linear'});
    }
  }
  animation.length=duration;
  window.__thomasFlare={tracks,diagnostics,duration,count};
  Timeline.setTime(0);Animator.preview();
  return JSON.stringify({animation:animation.name,bones:Object.keys(tracks).length,poses:count+1,frontLowering:1.8});
})()
