package cn.laowu.mod.client;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
/** Separate flight-tail and underwater poses; ordinary locomotion owns the pilot's limbs. */
public final class CatRideAnimation {
    public static void pilot(float age, ModelPart tail, ModelPart tip) {
        pilot(age,1,tail,tip);
    }
    public static void pilot(float age,float blend,ModelPart tail,ModelPart tip) {
        if(blend<=0)return;
        blend=Mth.clamp(blend,0,1);
        var offset=new org.joml.Vector3f(tip.x,tip.y,tip.z).sub(tailEnd(tail));
        var rotor=new org.joml.Quaternionf().rotationZYX(tip.zRot,tip.yRot,tip.xRot)
                .slerp(new org.joml.Quaternionf().rotationX(Mth.PI),blend);
        // Tilt a cone open around a non-spinning axis. Never shortest-angle blend the cyclic phase:
        // doing so flips the blade by half a turn whenever the phase crosses pi during a transition.
        float phase=(age%4F)*(Mth.TWO_PI/4F);
        rotor.rotateY(phase).rotateZ(-blend*Mth.HALF_PI).rotateY(-phase);
        var angles=rotor.getEulerAnglesZYX(new org.joml.Vector3f());
        tip.xRot=angles.x;tip.yRot=angles.y;tip.zRot=angles.z;
        CatPoseTransitions.apply(blend,()->{tail.xRot=Mth.PI;tail.yRot=tail.zRot=0;},tail);
        // Keep the vanilla anchor residual at zero weight, then smoothly close that tiny offset.
        var joint=tailEnd(tail).add(offset.mul(1-blend));
        tip.setPos(joint.x,joint.y,joint.z);
    }
    private static org.joml.Vector3f tailEnd(ModelPart tail) {
        // Vanilla tails are sibling bones. Anchor the tip at the real eight-pixel base end.
        var rotation=new org.joml.Quaternionf().rotationZYX(tail.zRot,tail.yRot,tail.xRot);
        return new org.joml.Vector3f(0,8,0).rotate(rotation).add(tail.x,tail.y,tail.z);
    }
    public static void apply(float age, ModelPart head, ModelPart body, ModelPart leftHind, ModelPart rightHind,
                              ModelPart leftFront, ModelPart rightFront, ModelPart tail, ModelPart tailTip) {
        body.setPos(0,12,-10);body.xRot=Mth.HALF_PI;body.yRot=body.zRot=0;
        head.setPos(0,15,-9);head.xRot=head.yRot=head.zRot=0;
        float breath=Mth.sin(age*.16F)*.025F;
        front(leftFront, true, breath); front(rightFront, false, breath);
        for(ModelPart leg:new ModelPart[]{leftHind,rightHind}){leg.resetPose();leg.xRot=Mth.HALF_PI-breath;leg.yRot=leg.zRot=0;}
        tail.resetPose();tailTip.resetPose();tail.xRot=1.45F;tailTip.xRot=1.65F;
    }
    /** Separate the paws laterally from the helmet, keeping the original shoulder height. */
    public static void front(ModelPart leg, boolean left, float breath) {
        leg.resetPose();
        leg.x = left ? 2.6F : -2.6F;
        leg.xRot = -1.15F + breath;
        leg.yRot = left ? -.42F : .42F;
        leg.zRot = 0;
    }
    private CatRideAnimation() {}
}
