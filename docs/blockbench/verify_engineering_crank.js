(() => {
  if (Project.name !== '工程猫摇曲柄') throw new Error('Select the dedicated engineer project');
  const group = name => Group.all.find(g => g.name === name);
  const world = (g,x,y,z) => {
    g.mesh.updateWorldMatrix(true,false);
    return g.mesh.localToWorld(new THREE.Vector3(x,y,z));
  };
  const left = group('left_front_leg'), right = group('right_front_leg');
  const body = group('body'), head = group('head'), pack = group('group');
  const handle = group('reference_crank_handle');
  const hind = [group('left_hind_leg'),group('right_hind_leg')];
  const boxes = ['group2','group3'].map(group);
  const seatTop = 1.784, samples = 720, oldTime = Timeline.time;
  let maximumContactError = 0, minimumFootClearance = Infinity;
  let maximumBoxDrift = 0, highPose;
  const boxVertices = [];
  for (const box of boxes) for (const element of box.children) {
    const points = element.mesh.geometry.getAttribute('position');
    for (let v=0;v<points.count;v++) boxVertices.push({element,local:new THREE.Vector3().fromBufferAttribute(points,v)});
  }
  Timeline.pause();
  try {
    for (let i=0;i<=samples;i++) {
      Timeline.setTime(i/samples*3);
      Animator.preview();
      for (const [paw,rodZ] of [[left,-4],[right,-1]]) {
        const error = world(paw,0,-10,1).distanceTo(world(handle,-7,0,rodZ));
        maximumContactError = Math.max(maximumContactError,error);
        if (!Number.isFinite(error) || error > .01) throw new Error('Paw lost handle at '+i+': '+error);
      }
      for (const foot of hind) for (const x of [-1,1]) for (const y of [-6,0]) for (const z of [1,3]) {
        const clearance = world(foot,x,y,z).y-seatTop;
        minimumFootClearance = Math.min(minimumFootClearance,clearance);
        if (!Number.isFinite(clearance) || clearance < -.005) throw new Error('Foot crossed seat at '+i+': '+clearance);
      }
      for (const box of boxes) {
        if (!box.visibility || !box.mesh.visible || box.children.some(e=>!e.visibility || !e.mesh.visible))
          throw new Error('Tool box hidden: '+box.name);
      }
      const phase=i/samples*Math.PI*2, cycle=(i/samples+.25)%1;
      const rise=cycle<.5?cycle*2:(1-cycle)*2;
      const stand=rise*rise*rise*(10+rise*(-15+6*rise));
      const torsoZ = body.mesh.position.z;
      const expected = 13.3 + 3.6*Math.cos(phase);
      if (Math.abs(torsoZ-expected)>.001) throw new Error('Incorrect rearward torso offset at '+i);
      if (Math.abs(body.mesh.position.y-(17.45+5.7*stand))>.005)
        throw new Error('Missing rigid stand-up translation at '+i);
      if (body.mesh.scale.distanceTo(new THREE.Vector3(1,.72,1))>.00001
          || pack.mesh.scale.distanceTo(new THREE.Vector3(1,1,.72))>.00001)
        throw new Error('Body or tool boxes stretched at '+i);
      const headOffset=new THREE.Vector3(-.35,-.072,-1.4).applyQuaternion(body.mesh.quaternion);
      const expectedHeadZ=torsoZ+headOffset.z-4;
      if (Math.abs(head.mesh.position.z-expectedHeadZ)>.005)
        throw new Error('Head was dragged backward with the torso at '+i);
      body.mesh.updateWorldMatrix(true,false);
      const bodyInverse=body.mesh.matrixWorld.clone().invert();
      for(const vertex of boxVertices) {
        vertex.element.mesh.updateWorldMatrix(true,false);
        const local=vertex.element.mesh.localToWorld(vertex.local.clone()).applyMatrix4(bodyInverse);
        if(i===0) vertex.bodyLocal=local;
        const drift=local.distanceTo(vertex.bodyLocal);
        maximumBoxDrift=Math.max(maximumBoxDrift,drift);
        if(drift>.01) throw new Error('Tool-box geometry detached from torso at '+i+': '+drift);
      }
      if(i===samples/4) highPose={bodyY:body.mesh.position.y,
        leftArmLength:left.mesh.scale.y*10,rightArmLength:right.mesh.scale.y*10};
    }
  } finally {
    Timeline.setTime(oldTime);
    Animator.preview();
  }
  return JSON.stringify({samples:samples+1,maximumContactError,minimumFootClearance,
    bothToolBoxesVisible:true,bodyBackOffset:4,standLift:4,bodyAndBoxSizeConstant:true,
    boxVertexCount:boxVertices.length,maximumBoxDrift,highPose,liftTiming:'half-cycle quintic easing'});
})()
