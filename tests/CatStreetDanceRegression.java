import cn.laowu.mod.client.CatStreetDanceAnimation;
import com.google.gson.*;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CatStreetDanceRegression {
    static final String[] NAMES = {"head", "body", "left_hind_leg", "right_hind_leg",
            "left_front_leg", "right_front_leg", "tail1", "tail2"};
    static final float[][] BOXES = {
            {-2.5f,-2,-3,5,4,5}, {-2,3,-8,4,16,6},
            {-1,0,1,2,6,2}, {-1,0,1,2,6,2},
            {-1,0,0,2,10,2}, {-1,0,0,2,10,2},
            {-.5f,0,0,1,8,1}, {-.5f,0,0,1,8,1}
    };
    record Key(float time, Vector3f value) {}
    record Bone(Vector3f origin, Map<String, List<Key>> channels) {}
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    static Vector3f vec(JsonArray values) {
        return new Vector3f(values.get(0).getAsFloat(), values.get(1).getAsFloat(), values.get(2).getAsFloat());
    }
    static Map<String, Bone> readSource(String file) throws Exception {
        JsonObject model = JsonParser.parseString(Files.readString(Path.of(file))).getAsJsonObject();
        JsonObject animation = model.getAsJsonArray("animations").get(0).getAsJsonObject();
        check(animation.get("name").getAsString().equals("animation.cat.thomas_flare"), "Wrong source animation");
        Map<String, Bone> bones = new HashMap<>();
        for (JsonElement element : model.getAsJsonArray("groups")) {
            JsonObject group = element.getAsJsonObject();
            String name = group.get("name").getAsString();
            JsonObject animator = animation.getAsJsonObject("animators").getAsJsonObject(group.get("uuid").getAsString());
            Map<String, List<Key>> channels = new HashMap<>();
            for (JsonElement k : animator.getAsJsonArray("keyframes")) {
                JsonObject key = k.getAsJsonObject(), point = key.getAsJsonArray("data_points").get(0).getAsJsonObject();
                Vector3f value = new Vector3f(point.get("x").getAsFloat(), point.get("y").getAsFloat(), point.get("z").getAsFloat());
                channels.computeIfAbsent(key.get("channel").getAsString(), ignored -> new ArrayList<>())
                        .add(new Key(key.get("time").getAsFloat(), value));
            }
            channels.values().forEach(keys -> keys.sort(Comparator.comparingDouble(Key::time)));
            bones.put(name, new Bone(vec(group.getAsJsonArray("origin")), channels));
        }
        check(bones.size() == 8, "Split leg bones were reintroduced");
        return bones;
    }
    static Vector3f sample(List<Key> keys, float time) {
        if (time <= keys.get(0).time()) return new Vector3f(keys.get(0).value());
        for (int i=1; i<keys.size(); i++) {
            Key previous = keys.get(i-1), next = keys.get(i);
            if (time <= next.time()) {
                return new Vector3f(previous.value()).lerp(next.value(), (time-previous.time())/(next.time()-previous.time()));
            }
        }
        return new Vector3f(keys.get(keys.size()-1).value());
    }
    static ModelPart[] pose(float age) {
        ModelPart[] parts = new ModelPart[8];
        for (int i=0; i<8; i++) parts[i] = new ModelPart();
        CatStreetDanceAnimation.apply(age, parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6], parts[7]);
        return parts;
    }
    static Quaternionf rotation(ModelPart part) {
        return new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot);
    }
    static Vector3f corner(int bone, int corner) {
        float[] b = BOXES[bone];
        return new Vector3f(b[0]+((corner&1)!=0?b[3]:0), b[1]+((corner&2)!=0?b[4]:0), b[2]+((corner&4)!=0?b[5]:0));
    }
    static Vector3f gamePoint(ModelPart part, Vector3f local) {
        Vector3f mc = new Vector3f(local).mul(part.xScale, part.yScale, part.zScale)
                .rotate(rotation(part)).add(part.x, part.y, part.z);
        return new Vector3f(-mc.x, 24-mc.y, mc.z);
    }
    static void testOutfit(ModelPart body) {
        float baseRotation = (float)Math.PI/2;
        Quaternionf live = rotation(body), inverse = new Quaternionf().rotationX(-baseRotation);
        Matrix4f expected = new Matrix4f().translation(body.x, body.y, body.z).rotate(live)
                .scale(body.xScale, body.yScale, body.zScale).rotate(inverse).translate(0,-12,10);
        for (Vector3f anchor : new Vector3f[]{new Vector3f(1.6f,14.5f,-10.1f),new Vector3f(0,24,0)}) {
            Vector3f offset = new Vector3f(anchor).sub(0,12,-10).rotate(inverse)
                    .mul(body.xScale,body.yScale,body.zScale).rotate(live).add(body.x,body.y,body.z);
            Matrix4f actual = new Matrix4f().translation(offset)
                    .rotate(new Quaternionf().rotationZYX(body.zRot,body.yRot,body.xRot-baseRotation))
                    .scale(body.xScale,body.zScale,body.yScale).translate(-anchor.x,-anchor.y,-anchor.z);
            for (Vector3f p : new Vector3f[]{new Vector3f(2,10,-3),new Vector3f(-5,15,6),new Vector3f(0,24,0)}) {
                check(expected.transformPosition(new Vector3f(p)).distance(actual.transformPosition(new Vector3f(p)))<0.0001,
                        "Outfit scale/orbit is not equivalent to live body pose");
            }
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length>0 && args[0].equals("--missing-resource")) {
            check(!CatStreetDanceAnimation.isAvailable(), "Expected safe missing-resource fallback");
            pose(10);
            System.out.println("PASS: missing animation resource retains vanilla pose without crashing");
            return;
        }
        check(CatStreetDanceAnimation.isAvailable(), "Animation asset did not load");
        check(Math.abs(CatStreetDanceAnimation.PERIOD_TICKS-24)<0.0001, "Not 2x / 1.2 seconds per cycle");
        Map<String, Bone> source = readSource(args[0]);
        float maximumError=0, maximumSupportError=0;
        double peakDegreesPerSecond=0;
        Quaternionf previous=null;
        for (int frame=0; frame<=960; frame++) {
            float sourceTime=frame/960f*2.4f,age=sourceTime*10;
            ModelPart[] parts=pose(age);
            float[] bottoms=new float[8];Arrays.fill(bottoms,Float.POSITIVE_INFINITY);
            for(int b=0;b<8;b++) {
                ModelPart part=parts[b];
                check(Float.isFinite(part.x+part.y+part.z+part.xRot+part.yRot+part.zRot
                        +part.xScale+part.yScale+part.zScale), "Non-finite pose");
                Bone bone=source.get(NAMES[b]);
                Vector3f position=sample(bone.channels().get("position"),sourceTime).add(bone.origin());
                Vector3f angles=sample(bone.channels().get("rotation"),sourceTime).mul((float)Math.PI/180);
                Quaternionf bbRotation=new Quaternionf().rotationZYX(angles.z,angles.y,angles.x);
                Vector3f scale=sample(bone.channels().get("scale"),sourceTime);
                for(int c=0;c<8;c++) {
                    Vector3f local=corner(b,c);
                    Vector3f expected=new Vector3f(-local.x,-local.y,local.z).mul(scale).rotate(bbRotation).add(position);
                    Vector3f actual=gamePoint(part,local);
                    float error=actual.distance(expected);
                    maximumError=Math.max(maximumError,error);
                    check(error<0.003,"Blockbench parity failed at "+sourceTime+" / "+NAMES[b]+": "+error);
                    bottoms[b]=Math.min(bottoms[b],actual.y);
                }
            }
            float support=Math.min(bottoms[4],bottoms[5]);
            maximumSupportError=Math.max(maximumSupportError,Math.abs(support));
            check(Math.abs(support)<0.02,"Both paws airborne or underground");
            check(bottoms[1]>0.5f&&bottoms[2]>0.1f&&bottoms[3]>0.1f,"Body or legs cut into the floor");
            Quaternionf current=rotation(parts[1]);
            if(previous!=null) {
                double dot=Math.min(1,Math.abs(previous.dot(current)));
                double speed=Math.toDegrees(2*Math.acos(dot))/(1.2/960);
                peakDegreesPerSecond=Math.max(peakDegreesPerSecond,speed);
            }
            previous=current;
            testOutfit(parts[1]);
        }
        check(peakDegreesPerSecond<900,"Abrupt body flip returned: "+peakDegreesPerSecond);
        for(float age:new float[]{0,.125f,4,12,23.875f,-.125f}) {
            ModelPart[] a=pose(age),b=pose(age+24);
            for(int bone=0;bone<8;bone++)for(int c=0;c<8;c++)
                check(gamePoint(a[bone],corner(bone,c)).distance(gamePoint(b[bone],corner(bone,c)))<0.001,
                        "Animation loop seam");
        }
        ModelPart[] untouched=pose(Float.NaN);
        check(untouched[0].x==0&&untouched[0].xScale==1,"Invalid clock changed the pose");
        System.out.printf(Locale.ROOT,"PASS: 961 poses, 61504 vertices; 2x playback; loop/contact/outfit parity. Max vertex error %.6f px, contact error %.6f px, body peak %.1f deg/s%n",
                maximumError,maximumSupportError,peakDegreesPerSecond);
    }
}
