import cn.laowu.mod.client.CatPipaAnimation;
import com.google.gson.*;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.nio.file.*;
import java.util.*;

/** Compare runtime poses/geometry to the independently parsed MCP project. */
public class CatPipaRegression {
    static final String[] NAMES={"head","body","left_hind_leg","right_hind_leg",
            "left_front_leg","right_front_leg","tail1","tail2","pipa","plectrum"};
    record Key(float time,Vector3f value) {}
    record Bone(Vector3f origin,Map<String,List<Key>> channels) {}
    static void check(boolean value,String reason) { if(!value) throw new AssertionError(reason); }
    static Vector3f vector(JsonArray a) { return new Vector3f(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat()); }
    static Vector3f sample(List<Key> keys,float time) {
        if(time<=0) return new Vector3f(keys.get(0).value);
        for(int i=1;i<keys.size();i++) if(time<=keys.get(i).time) {
            Key a=keys.get(i-1),b=keys.get(i);
            return new Vector3f(a.value).lerp(b.value,(time-a.time)/(b.time-a.time));
        }
        return new Vector3f(keys.get(keys.size()-1).value);
    }
    static ModelPart[] pose(float age) {
        ModelPart[] p=new ModelPart[10];
        for(int i=0;i<p.length;i++) p[i]=new ModelPart();
        CatPipaAnimation.apply(age,p[0],p[1],p[2],p[3],p[4],p[5],p[6],p[7],p[8],p[9]);
        return p;
    }
    static Quaternionf rotation(ModelPart p) { return new Quaternionf().rotationZYX(p.zRot,p.yRot,p.xRot); }
    static Vector3f world(ModelPart p,Vector3f bbLocal) {
        Vector3f v=new Vector3f(-bbLocal.x,-bbLocal.y,bbLocal.z).mul(p.xScale,p.yScale,p.zScale)
                .rotate(rotation(p)).add(p.x,p.y,p.z);
        return new Vector3f(-v.x,24-v.y,v.z);
    }
    public static void main(String[] args) throws Exception {
        if(args[0].equals("--missing-resource")) {
            check(!CatPipaAnimation.isAvailable(),"Missing resource must fail safely");
            check(pose(10)[0].x==0,"No partial pose on missing resource");
            System.out.println("PASS: missing/corrupt pipa asset safely retains the previous pose");
            return;
        }
        check(CatPipaAnimation.isAvailable(),"Load pipa clip");
        check(CatPipaAnimation.PERIOD_TICKS==64,"3.2-second loop");
        JsonObject model=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();
        JsonObject animation=model.getAsJsonArray("animations").get(0).getAsJsonObject();
        check(animation.get("name").getAsString().equals("animation.cat.pipa_performance"),"Right source");
        Map<String,Bone> bones=new HashMap<>();
        Map<String,String> owner=new HashMap<>();
        Map<String,String> groupNames=new HashMap<>();
        for(JsonElement raw:model.getAsJsonArray("groups")) {
            JsonObject group=raw.getAsJsonObject();
            String name=group.get("name").getAsString(),id=group.get("uuid").getAsString();
            groupNames.put(id,name);
            Map<String,List<Key>> channels=new HashMap<>();
            for(JsonElement k:animation.getAsJsonObject("animators").getAsJsonObject(id).getAsJsonArray("keyframes")) {
                JsonObject key=k.getAsJsonObject(),v=key.getAsJsonArray("data_points").get(0).getAsJsonObject();
                channels.computeIfAbsent(key.get("channel").getAsString(),ignored->new ArrayList<>())
                        .add(new Key(key.get("time").getAsFloat(),new Vector3f(v.get("x").getAsFloat(),v.get("y").getAsFloat(),v.get("z").getAsFloat())));
            }
            channels.values().forEach(keys->keys.sort(Comparator.comparingDouble(Key::time)));
            bones.put(name,new Bone(vector(group.getAsJsonArray("origin")),channels));
        }
        check(bones.size()==10,"Eight original bones plus instrument and pick");
        for(JsonElement raw:model.getAsJsonArray("outliner")) {
            JsonObject group=raw.getAsJsonObject();
            for(JsonElement child:group.getAsJsonArray("children")) owner.put(child.getAsString(),groupNames.get(group.get("uuid").getAsString()));
        }
        int points=0; float maxError=0,minY=Float.POSITIVE_INFINITY,maxContact=0;
        float minStroke=Float.POSITIVE_INFINITY,maxStroke=Float.NEGATIVE_INFINITY;
        for(int frame=0;frame<=960;frame++) {
            float time=frame/960f*3.2f;
            ModelPart[] actual=pose(time*20);
            Map<String,Vector3f> positions=new HashMap<>(),scales=new HashMap<>();
            Map<String,Quaternionf> rotations=new HashMap<>();
            for(String name:NAMES) {
                Bone b=bones.get(name);
                positions.put(name,sample(b.channels.get("position"),time).add(b.origin));
                scales.put(name,sample(b.channels.get("scale"),time));
                Vector3f r=sample(b.channels.get("rotation"),time).mul((float)Math.PI/180);
                rotations.put(name,new Quaternionf().rotationZYX(r.z,r.y,r.x));
            }
            for(JsonElement raw:model.getAsJsonArray("elements")) {
                JsonObject e=raw.getAsJsonObject();
                String name=owner.get(e.get("uuid").getAsString());
                int index=Arrays.asList(NAMES).indexOf(name);
                Vector3f from=vector(e.getAsJsonArray("from")),to=vector(e.getAsJsonArray("to"));
                Vector3f pivot=vector(e.getAsJsonArray("origin"));
                Vector3f er=e.has("rotation") ? vector(e.getAsJsonArray("rotation")).mul((float)Math.PI/180) : new Vector3f();
                Quaternionf eq=new Quaternionf().rotationZYX(er.z,er.y,er.x);
                for(int corner=0;corner<8;corner++) {
                    Vector3f p=new Vector3f((corner&1)==0?from.x:to.x,(corner&2)==0?from.y:to.y,(corner&4)==0?from.z:to.z)
                            .sub(pivot).rotate(eq).add(pivot).sub(bones.get(name).origin);
                    Vector3f expected=new Vector3f(p).mul(scales.get(name)).rotate(rotations.get(name)).add(positions.get(name));
                    Vector3f got=world(actual[index],p);
                    maxError=Math.max(maxError,expected.distance(got));
                    minY=Math.min(minY,got.y); points++;
                    check(expected.distance(got)<0.0003,"Source/runtime geometry mismatch: "+name+" at "+time);
                }
            }
            double phase=time/3.2*Math.PI*2;
            Vector3f fret=world(actual[8],new Vector3f(-0.55f,12.1f+0.6f*(float)Math.sin(phase*2),-0.80f));
            Vector3f strumLocal=new Vector3f(0.8f+1.05f*(float)Math.sin(phase*4),4.8f+0.65f*(float)Math.cos(phase*4),-2.10f);
            Vector3f strum=world(actual[8],strumLocal);
            maxContact=Math.max(maxContact,Math.max(fret.distance(world(actual[4],new Vector3f(0,-10,1))),
                    strum.distance(world(actual[5],new Vector3f(0,-10,1)))));
            minStroke=Math.min(minStroke,strumLocal.x);maxStroke=Math.max(maxStroke,strumLocal.x);
            Vector3f pickPosition=new Vector3f(-actual[9].x,24-actual[9].y,actual[9].z);
            check(pickPosition.distance(world(actual[5],new Vector3f(0,-10,1)).add(-0.2f,-0.3f,-0.1f))<0.02,"Pick detached from paw");
        }
        check(minY>=-0.002,"Geometry below ground: "+minY);
        check(maxContact<0.025,"Paws lose instrument contact: "+maxContact);
        check(maxStroke-minStroke>2,"Plucking motion is too small");
        ModelPart[] invalid=pose(Float.NaN);
        check(invalid[0].x==0,"Ignore invalid age");
        for(int i=0;i<10;i++) {
            ModelPart a=pose(-2)[i],b=pose(62)[i],c=pose(126)[i];
            check(new Vector3f(a.x,a.y,a.z).distance(new Vector3f(b.x,b.y,b.z))<0.0001,"Negative time wrap");
            check(new Vector3f(c.x,c.y,c.z).distance(new Vector3f(b.x,b.y,b.z))<0.0001,"Loop wrap");
        }
        System.out.println("PASS: 961 poses / "+points+" vertices; source error="+maxError+", paw error="+maxContact+", ground="+minY);
    }
}
