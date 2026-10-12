package cn.laowu.mod.client;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.entity.MaodieFighter;
import cn.laowu.mod.entity.MaodieFighterTiming;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import java.io.Reader;
import java.util.*;

/** Exact linear baked tracks; cached once and sampled by the synchronized server phase clock. */
public final class MaodieFighterAnimation {
 private static final ResourceLocation CURVES=LaoWuMod.id("cat_animation_clips/maodie_fighter.json");
 private static Map<String,Clip> clips;
 private MaodieFighterAnimation(){}
 public static void clearCache(){clips=null;}
 public static String clipForPhase(byte phase){return switch(phase){
  case MaodieFighter.COMBO -> "combo";case MaodieFighter.JAB -> "jab";
  case MaodieFighter.UPPERCUT -> "uppercut";case MaodieFighter.SWEEP -> "roundhouse";
  case MaodieFighter.BACKSTEP -> "backstep";case MaodieFighter.JUMP_SLAM -> "jump_slam";
  case MaodieFighter.BLOCK -> "block";default -> "guard";
 };}
 /** PULL is a reach-and-draw gesture over guard, never the defensive block clip. */
 public static Map<String,RuntimeBlockbenchModel.GroupTransform> samplePhase(byte phase,float ticks){
  float seconds=MaodieFighterTiming.clipSeconds(phase,ticks);
  Map<String,RuntimeBlockbenchModel.GroupTransform> pose=sample(clipForPhase(phase),seconds);
  if(phase!=MaodieFighter.PULL)return pose;
  pose=new HashMap<>(pose);
  float t=Math.max(0,Math.min(.5F,seconds));
  float reach=t<.1F?t/.1F:Math.max(0,1-(t-.1F)/.2F);
  float draw=t<.1F?0:(float)Math.sin(Math.PI*Math.min(1,(t-.1F)/.4F));
  float radians=(float)Math.PI/180;
  for(String side:List.of("left","right")){
   float sign=side.equals("left")?1:-1;
   rotate(pose,side+"_arm",(-65*reach+15*draw)*radians,0,sign*(20*reach+10*draw)*radians);
   rotate(pose,side+"_elbow",(90*reach-15*draw)*radians,0,0);
  }
  return pose;
 }
 private static void rotate(Map<String,RuntimeBlockbenchModel.GroupTransform> pose,String bone,float x,float y,float z){
  var p=pose.get(bone);if(p==null)return;
  pose.put(bone,new RuntimeBlockbenchModel.GroupTransform(p.x(),p.y(),p.z(),p.xRot()+x,p.yRot()+y,p.zRot()+z,p.scaleX(),p.scaleY(),p.scaleZ()));
 }
 public static Map<String,RuntimeBlockbenchModel.GroupTransform> sample(String name,float seconds){
  if(clips==null)clips=load();Clip clip=clips.get(name);if(clip==null)return Map.of();
  float t=Float.isFinite(seconds)?Math.max(0,seconds):0;t=clip.loop?t%clip.length:Math.min(t,clip.length);
  Map<String,RuntimeBlockbenchModel.GroupTransform> pose=new HashMap<>();
  for(var entry:clip.bones.entrySet()){
   Channels c=entry.getValue();float[] p=interpolate(c.position,t),r=interpolate(c.rotation,t),s=interpolate(c.scale,t);
   // Match getCatOutfit's reflection and side-name exchange, cancelling the living renderer's X reflection.
   String bone=entry.getKey();if(bone.startsWith("left_"))bone="right_"+bone.substring(5);else if(bone.startsWith("right_"))bone="left_"+bone.substring(6);
   float d=(float)Math.PI/180;
   pose.put(bone,new RuntimeBlockbenchModel.GroupTransform(-p[0],p[1],p[2],-r[0]*d,-r[1]*d,r[2]*d,s[0],s[1],s[2]));
  }return pose;
 }
 /** Already-unwrapped Euler keys must not be shortest-path wrapped or eased again. */
 static float[] interpolate(float[][] keys,float t){
  int lo=0,hi=keys.length-1;
  if(t<=keys[lo][0])return Arrays.copyOfRange(keys[lo],1,4);
  if(t>=keys[hi][0])return Arrays.copyOfRange(keys[hi],1,4);
  while(hi-lo>1){int mid=(lo+hi)>>>1;if(keys[mid][0]<=t)lo=mid;else hi=mid;}
  float u=(t-keys[lo][0])/(keys[hi][0]-keys[lo][0]);
  return new float[]{keys[lo][1]+(keys[hi][1]-keys[lo][1])*u,keys[lo][2]+(keys[hi][2]-keys[lo][2])*u,keys[lo][3]+(keys[hi][3]-keys[lo][3])*u};
 }
 private static Map<String,Clip> load(){
  try(Reader reader=Minecraft.getInstance().getResourceManager().getResourceOrThrow(CURVES).openAsReader()){
   Map<String,Clip> result=new HashMap<>();
   for(var entry:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("clips").entrySet()){
    JsonObject a=entry.getValue().getAsJsonObject();Map<String,Channels> bones=new HashMap<>();
    for(var bone:a.getAsJsonObject("bones").entrySet()){var c=bone.getValue().getAsJsonObject();bones.put(bone.getKey(),new Channels(channel(c,"position"),channel(c,"rotation"),channel(c,"scale")));}
    float length=a.get("length").getAsFloat();if(!(length>0))throw new IllegalArgumentException("Invalid fighter clip length");
    result.put(entry.getKey(),new Clip(length,a.get("loop").getAsBoolean(),Map.copyOf(bones)));
   }return Map.copyOf(result);
  }catch(Exception e){LogUtils.getLogger().error("Could not load fighter animation clips",e);return Map.of();}
 }
 private static float[][] channel(JsonObject c,String name){
  JsonArray a=c.getAsJsonArray(name);if(a==null||a.isEmpty())throw new IllegalArgumentException("Empty fighter channel");
  float[][] keys=new float[a.size()][4];float previous=-1;
  for(int i=0;i<a.size();i++){var row=a.get(i).getAsJsonArray();for(int j=0;j<4;j++){keys[i][j]=row.get(j).getAsFloat();if(!Float.isFinite(keys[i][j]))throw new IllegalArgumentException("Non-finite fighter frame");}if(keys[i][0]<=previous)throw new IllegalArgumentException("Unsorted fighter track");previous=keys[i][0];}
  return keys;
 }
 private record Channels(float[][] position,float[][] rotation,float[][] scale){}
 private record Clip(float length,boolean loop,Map<String,Channels> bones){}
}
