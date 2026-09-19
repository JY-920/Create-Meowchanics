package cn.laowu.mod.client;

import cn.laowu.mod.CatVisualStates;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Cat;

/** Per-entity blends; shared render models and repeated render passes never advance a second time. */
public final class CatPoseTransitions {
    private static final CatVisualStates<State> STATES=new CatVisualStates<>();
    private static final class State { double time=Double.NaN; float ride,dash,wings,age; int mode=1; }
    public record Frame(float ride,float dash,float wings,int mode,float age) {}
    public static Frame sample(Cat cat,boolean riding,int mode,float partial) {
        var s=STATES.getOrCreate(cat,State::new);
        double time=cat.level().getGameTime()+Math.max(0,Math.min(1,partial));
        float elapsed=Double.isNaN(s.time)?0:(float)Math.max(0,Math.min(20,time-s.time));
        s.time=time;
        s.ride=advance(s.ride,riding,elapsed);
        s.dash=advance(s.dash,mode==2,elapsed);
        s.wings=advance(s.wings,mode!=0,elapsed);
        if(mode!=0){s.mode=mode;s.age+=elapsed;}
        if(s.wings<=0)s.age=0;
        return new Frame(ease(s.ride),ease(s.dash),ease(s.wings),s.mode,s.age);
    }
    public static float advance(float value,boolean active,float elapsed) {
        return Mth.clamp(value+(active?1:-1)*Math.max(0,elapsed)/(active?6:8),0,1);
    }
    private static float ease(float v){return v*v*(3-2*v);}
    public static void apply(float weight,Runnable animation,ModelPart... parts) {
        if(weight<=0)return;
        if(weight>=1){animation.run();return;}
        float[][] from=new float[parts.length][];
        for(int i=0;i<parts.length;i++){var p=parts[i];from[i]=new float[]{p.x,p.y,p.z,p.xRot,p.yRot,p.zRot,p.xScale,p.yScale,p.zScale};}
        animation.run();
        for(int i=0;i<parts.length;i++){
            var p=parts[i];var f=from[i];
            p.setPos(Mth.lerp(weight,f[0],p.x),Mth.lerp(weight,f[1],p.y),Mth.lerp(weight,f[2],p.z));
            p.xRot=angle(weight,f[3],p.xRot);p.yRot=angle(weight,f[4],p.yRot);p.zRot=angle(weight,f[5],p.zRot);
            p.xScale=Mth.lerp(weight,f[6],p.xScale);p.yScale=Mth.lerp(weight,f[7],p.yScale);p.zScale=Mth.lerp(weight,f[8],p.zScale);
        }
    }
    private static float angle(float t,float a,float b){return a+t*(float)Math.atan2(Math.sin(b-a),Math.cos(b-a));}
    private CatPoseTransitions(){}
}
