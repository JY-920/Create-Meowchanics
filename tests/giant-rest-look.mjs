import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {execFileSync} from 'node:child_process';

const jar = path.join(process.env.USERPROFILE ?? os.homedir(), '.gradle/caches/forge_gradle/maven_downloader/org/joml/joml/1.10.5/joml-1.10.5.jar');
const temporary = fs.mkdtempSync(path.join(os.tmpdir(), 'giant-rest-look-'));
const probe = `import java.util.*; import java.lang.reflect.*; import org.joml.Quaternionf; import org.joml.Vector3f;
public class RestLookProbe {
static Class<?> look,candidate; static Method select,angles,delta; static Object state;
static void check(boolean b,String s){if(!b)throw new AssertionError(s);} static void near(float a,float b,String s){check(Math.abs(a-b)<.0001f,s+": "+a+" != "+b);}
static Object entity(int id,boolean player,boolean alive,boolean invisible,boolean spectator,boolean excluded,double x,double y,double z) throws Exception {return candidate.getConstructor(int.class,boolean.class,boolean.class,boolean.class,boolean.class,boolean.class,double.class,double.class,double.class).newInstance(id,player,alive,invisible,spectator,excluded,x,y,z);}
static Object valid(int id,boolean player,double x,double z) throws Exception {return entity(id,player,true,false,false,false,x,0,z);}
static int chosen(float ticks,Object... entities) throws Exception {Object target=select.invoke(state,List.of(entities),0f,ticks);return target==null?-1:(int)candidate.getMethod("id").invoke(target);}
static void reset() throws Exception {state=Class.forName("cn.laowu.mod.client.GiantCatLook$State").getConstructor().newInstance();}
public static void main(String[] args) throws Exception {
try {look=Class.forName("cn.laowu.mod.client.GiantCatLook");} catch(ClassNotFoundException e){throw new AssertionError("Resting giant must independently observe nearby entities with world-aligned head motion",e);}
candidate=Class.forName("cn.laowu.mod.client.GiantCatLook$Candidate");reset();select=state.getClass().getMethod("select",List.class,float.class,float.class);
angles=look.getMethod("boundedAngles",double.class,double.class,double.class,float.class);
delta=look.getMethod("restEulerDelta",float.class,float.class,float.class,Quaternionf.class,float.class,float.class,float.class);
check(chosen(0,valid(4,false,0,2),valid(9,true,0,5))==9,"Player gets visual priority over closer non-player living entity");
reset();check(chosen(0,valid(7,false,0,4))==7,"Acquire nearby living entity when no player exists");
check(chosen(1,valid(7,false,0,4.1),valid(3,false,0,3.9))==7,"Near-equal targets cannot jitter");
check(chosen(1,valid(7,false,0,4.1),valid(3,false,0,1))==7,"Hold target through short distraction");
for(int t=0;t<25;t++)chosen(2,valid(7,false,0,4),valid(3,false,0,1));
check(chosen(1,valid(7,false,0,4),valid(3,false,0,1))==3,"After hold period meaningfully closer target can replace it");
check(chosen(1,valid(3,false,0,1),valid(11,true,0,6))==11,"Player arrival replaces non-player without waiting for distance hysteresis");
check(chosen(1,valid(3,false,0,1))==3,"Lost player immediately releases stale target");
reset();check(chosen(0,valid(1,true,0,-2),valid(2,true,0,8.01),valid(3,true,4,0),entity(4,true,false,false,false,false,0,0,1),entity(5,true,true,true,false,false,0,0,1),entity(6,true,true,false,true,false,0,0,1),entity(7,true,true,false,false,true,0,0,1),valid(8,false,0,2))==8,"Ignore behind/out-of-sector/out-of-range/dead/invisible/spectator/self-or-same-vehicle");
reset();check(chosen(0,valid(10,true,0,3),valid(2,true,0,3))==2,"Equal candidates use deterministic entity id");
reset();chosen(0,valid(1,false,0,4));for(int t=0;t<80;t++)chosen(0,valid(1,false,0,4),valid(2,false,0,1));
check(chosen(0,valid(1,false,0,4),valid(2,false,0,1))==1,"Rendering twice in a tick cannot age the hold window");
for(int t=0;t<25;t++)chosen(2,valid(1,false,0,4),valid(2,false,0,2.9));
check(chosen(1,valid(1,false,0,4),valid(2,false,0,2.9))==1,"A27.5percent closer target is insufficient for35percent hysteresis");
check(chosen(1,valid(1,false,0,4),valid(2,false,0,2.4))==2,"A40percent closer target crosses distance hysteresis");
reset();Object rotated=select.invoke(state,List.of(valid(6,true,-4,0),valid(7,true,0,4)),90f,0f);
check((int)candidate.getMethod("id").invoke(rotated)==6,"Bodyyaw90 front sector points toward world negative X");
reset();check(chosen(0,valid(12,true,0,8))==12,"Eight-block boundary is inclusive");
var a=(float[])angles.invoke(null,-3d,0d,4d,0f);near(a[0],25,"Positive Minecraft yaw is toward negative X, clamped to25");near(a[1],0,"Level target stays level");
a=(float[])angles.invoke(null,0d,3d,3d,0f);near(a[0],0,"Front target yaw");near(a[1],-10,"Higher target raises head no more than10deg");
a=(float[])angles.invoke(null,0d,-3d,3d,0f);near(a[1],10,"Lower target lowers head no more than10deg");
a=(float[])angles.invoke(null,0d,0d,0d,0f);check(Float.isFinite(a[0])&&Float.isFinite(a[1]),"Coincident target never produces NaN");
// Reconstruct the final rendered head quaternion and compare with independently
// authored world-yaw/world-horizontal-pitch corrections. At full side roll a
// naive local Y yaw visibly pitches the head instead of looking horizontally.
for(float body:new float[]{0,77,-179})for(float roll:new float[]{0,-.4f,-1.5707963f})for(float yaw:new float[]{-25,0,25})for(float pitch:new float[]{-10,0,10}) {
Quaternionf parent=new Quaternionf().rotationY((float)Math.toRadians(180-body)).rotateZ((float)Math.PI).rotateZ(roll).rotateX(.08f);
float baseX=.05235988f,baseY=.03f,baseZ=.31415927f;
Quaternionf baseline=new Quaternionf(parent).mul(new Quaternionf().rotationZYX(baseZ,baseY,baseX));
Quaternionf correction=new Quaternionf().rotationY((float)Math.toRadians(-body-yaw)).rotateX((float)Math.toRadians(pitch)).rotateY((float)Math.toRadians(body));
Quaternionf expected=new Quaternionf(correction).mul(baseline);
float[] result=(float[])delta.invoke(null,body,yaw,pitch,parent,baseX,baseY,baseZ);
Quaternionf rendered=new Quaternionf(parent).mul(new Quaternionf().rotationZYX(baseZ+result[2],baseY+result[1],baseX+result[0]));
for(Vector3f axis:new Vector3f[]{new Vector3f(1,0,0),new Vector3f(0,1,0),new Vector3f(0,0,-1)}) {
Vector3f want=expected.transform(new Vector3f(axis)),actual=rendered.transform(new Vector3f(axis));check(actual.distance(want)<.00001f,"World gravity aligned yaw/pitch survives body heading, side roll and authored head roll: body="+body+" roll="+roll+" yaw="+yaw+" pitch="+pitch+" deltas="+Arrays.toString(result)+" actual="+actual+" expected="+want);
}
if(yaw==0&&pitch==0)for(float v:result)near(v,0,"No-look motion preserves authored rest head pose");
}
System.out.println("PASS: player/living priority, front/range/eligibility, stable hold and hysteresis, bounded finite target angles, gravity-aligned yaw/pitch under rolled ancestors and authored head pose");
} }`;
try {
    const file=path.join(temporary,'RestLookProbe.java'); fs.writeFileSync(file,probe);
    for(const loader of ['forge-1.20.1','neoforge-1.21.1']) {
        const helper=loader+'/src/main/java/cn/laowu/mod/client/GiantCatLook.java';
        execFileSync('javac',['-encoding','UTF-8','-cp',jar,'-d',temporary,file,...(fs.existsSync(helper)?[helper]:[])],{stdio:'inherit'});
        execFileSync('java',['-cp',[temporary,jar].join(path.delimiter),'RestLookProbe'],{stdio:'inherit'});
    }
} finally {fs.rmSync(temporary,{recursive:true,force:true});}
