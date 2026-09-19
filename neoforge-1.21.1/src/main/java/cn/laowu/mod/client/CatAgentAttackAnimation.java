package cn.laowu.mod.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Native-cat adaptation of local jujutsuvfx CatCombatPose JAB/RAPID/KICK timing:
 * shoulder drive, alternating fists, one complete turning kick. No donor mod dependency.
 * The vanilla cat has unsplit arms, so shoulders stay below/outside its muzzle.
 */
public final class CatAgentAttackAnimation {
    private static float smooth(float t) { t=Mth.clamp(t,0,1);return t*t*(3-2*t); }
    public static void apply(int move, float progress, ModelPart head, ModelPart body,
                             ModelPart lh, ModelPart rh, ModelPart lf, ModelPart rf, ModelPart tail, ModelPart tip) {
        float t=Mth.clamp(progress,0,1),weight=smooth(t/.12F)*(1-smooth((t-.78F)/.22F));
        if(weight<=0)return;
        ModelPart[] bones={head,body,lh,rh,lf,rf,tail,tip};
        float[][] previous=new float[bones.length][6];
        for(int i=0;i<bones.length;i++){
            var p=bones[i];previous[i]=new float[]{p.x,p.y,p.z,p.xRot,p.yRot,p.zRot};p.resetPose();
        }
        // As in the donor: a standing torso with spread limbs, never the vanilla seated hips.
        body.setPos(0,12,-10);body.xRot=Mth.HALF_PI;
        head.setPos(0,15,-9);head.xRot=-.10F;
        lh.setPos(1.1F,18,5);rh.setPos(-1.1F,18,5);lh.xRot=rh.xRot=.18F;
        lh.zRot=.20F;rh.zRot=-.20F;
        // One-piece forelegs need a little outward clearance from the native cat's broad muzzle.
        lf.setPos(2.7F,14.1F,-5);rf.setPos(-2.7F,14.1F,-5);
        tail.setPos(0,15,8);tail.xRot=.9F;tip.setPos(0,20,14);tip.xRot=1.7278761F;
        float rootYaw=0,rootRoll=0,rootPitch=0;
        if(move==2){
            float lift=smooth(t/.22F),extend=smooth((t-.22F)/.17F),recover=smooth((t-.56F)/.44F);
            float air=lift*(1-recover),strike=extend*(1-recover);
            lf.xRot=-.6F;rf.xRot=-.25F;lf.zRot=-.35F;rf.zRot=.35F;
            lh.xRot=Mth.lerp(strike,.70F,-1.85F);lh.yRot=-.22F*air;
            rh.xRot=.18F+.74F*air;rh.zRot=-.42F*air;
            rootYaw=Mth.TWO_PI*smooth((t-.06F)/.55F);
            rootRoll=.22F*air;rootPitch=-.18F*air;
            head.xRot=.18F*air;tail.xRot-=.3F*air;
        }else{
            float punch=move==1?pulse((t*3)%1):pulse(t);
            float other=move==1?pulse((t*3+.5F)%1):.12F;
            lf.xRot=-.35F-1.17F*punch;rf.xRot=-.35F-1.17F*other;
            lf.yRot=-.24F;rf.yRot=.24F;
            lf.zRot=-.20F;rf.zRot=.20F;
            lh.xRot=.18F+.28F*punch;rh.xRot=.36F-.18F*punch;
            lf.z-=1.5F*punch;rf.z-=1.5F*other;
            body.yRot=.16F*(punch-other);head.yRot=-body.yRot*.5F;
            rootRoll=.045F*(punch-other);
        }
        Quaternionf turn=new Quaternionf().rotationYXZ(rootYaw,rootPitch,rootRoll);
        for(int i=0;i<bones.length;i++){
            var p=bones[i];
            Vector3f pos=new Vector3f(p.x,p.y-15,p.z).rotate(turn).add(0,15,0);
            Vector3f angles=eulerZYX(new Quaternionf(turn).mul(new Quaternionf().rotationZYX(p.zRot,p.yRot,p.xRot)));
            p.setPos(Mth.lerp(weight,previous[i][0],pos.x),Mth.lerp(weight,previous[i][1],pos.y),Mth.lerp(weight,previous[i][2],pos.z));
            p.xRot=angle(previous[i][3],angles.x,weight);p.yRot=angle(previous[i][4],angles.y,weight);p.zRot=angle(previous[i][5],angles.z,weight);
        }
    }
    /** Explicit normalized ZYX extraction: bundled JOML 1.10.5 has a wrong-sign X denominator here. */
    static Vector3f eulerZYX(Quaternionf q) {
        q.normalize();
        float sinY=Mth.clamp(2*(q.w*q.y-q.z*q.x),-1,1);
        float x=(float)Math.atan2(2*(q.w*q.x+q.y*q.z),1-2*(q.x*q.x+q.y*q.y));
        float y=(float)Math.asin(sinY);
        float z=(float)Math.atan2(2*(q.w*q.z+q.x*q.y),1-2*(q.y*q.y+q.z*q.z));
        if(Math.abs(sinY)>.999999F) {
            x=(float)Math.atan2(2*(q.w*q.x-q.y*q.z),1-2*(q.x*q.x+q.z*q.z));z=0;
        }
        return new Vector3f(x,y,z);
    }
    private static float pulse(float t){return smooth(t/.26F)*(1-smooth((t-.35F)/.55F));}
    private static float angle(float from,float to,float weight){return from+Mth.wrapDegrees((to-from)*Mth.RAD_TO_DEG)*Mth.DEG_TO_RAD*weight;}
    private CatAgentAttackAnimation() {}
}
