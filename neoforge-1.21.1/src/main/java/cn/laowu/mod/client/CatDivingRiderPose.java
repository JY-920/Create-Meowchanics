package cn.laowu.mod.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Prone scooter grip, shared with armor through Create's existing humanoid pose hook. */
public final class CatDivingRiderPose {
    private static final java.util.Map<HumanoidModel<?>,net.minecraft.client.model.geom.PartPose> CAPE_ORIGINS
            =new java.util.WeakHashMap<>();
    public static void apply(HumanoidModel<?> model,float age,float amount) {
        apply(model,age,amount,0);
    }
    public static void apply(HumanoidModel<?> model,float age,float amount,float relativeYaw) {
        CAPE_ORIGINS.put(model,model.body.storePose());
        CatPoseTransitions.apply(amount,()->{
            pose(model.head,0,17,-1.2F,0,0);
            pose(model.body,0,17,0,Mth.HALF_PI,0);
            pose(model.leftArm,5,19,0,-1.82F,.32F);
            pose(model.rightArm,-5,19,0,-1.82F,-.32F);
            float kick=Mth.sin(age*.2F)*.035F;
            pose(model.leftLeg,1.9F,17,12,Mth.HALF_PI+kick,0);
            pose(model.rightLeg,-1.9F,17,12,Mth.HALF_PI-kick,0);
            float yaw=relativeYaw*Mth.DEG_TO_RAD;
            for(var part:new ModelPart[]{model.head,model.body,model.leftArm,model.rightArm,model.leftLeg,model.rightLeg}) {
                var position=new org.joml.Vector3f(part.x,part.y,part.z).rotateY(yaw);
                part.setPos(position.x,position.y,position.z);
                part.yRot+=yaw;
            }
        },model.head,model.body,model.leftArm,model.rightArm,model.leftLeg,model.rightLeg);
        model.hat.copyFrom(model.head);
        // PlayerModel copies sleeves/jacket after the superclass hook; armor sees these same bones.
    }
    /** Move the independent vanilla cape by the same torso delta, without counting crouch twice. */
    public static void applyCape(com.mojang.blaze3d.vertex.PoseStack stack,HumanoidModel<?> model) {
        var from=CAPE_ORIGINS.get(model);
        if(from==null)return;
        var body=model.body;
        stack.translate(body.x/16F,body.y/16F,body.z/16F);
        stack.mulPose(new org.joml.Quaternionf().rotationZYX(body.zRot,body.yRot,body.xRot));
        stack.mulPose(new org.joml.Quaternionf().rotationZYX(from.zRot,from.yRot,from.xRot).conjugate());
        stack.translate(-from.x/16F,-from.y/16F,-from.z/16F);
    }
    private static void pose(ModelPart part,float x,float y,float z,float pitch,float yaw) {
        part.setPos(x,y,z);part.xRot=pitch;part.yRot=yaw;part.zRot=0;
    }
    private CatDivingRiderPose(){}
}
