package cn.laowu.mod.client;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
/** Streamlined mount pose: front paws reach ahead, hind paws trail behind. No land-riding pose. */
public final class CatRideAnimation {
    public static void apply(float age, ModelPart head, ModelPart body, ModelPart leftHind, ModelPart rightHind,
                              ModelPart leftFront, ModelPart rightFront, ModelPart tail, ModelPart tailTip) {
        body.setPos(0,12,-10);body.xRot=Mth.HALF_PI;body.yRot=body.zRot=0;
        head.setPos(0,15,-9);head.xRot=head.yRot=head.zRot=0;
        float breath=Mth.sin(age*.16F)*.025F;
        front(leftFront, true, breath); front(rightFront, false, breath);
        for(ModelPart leg:new ModelPart[]{leftHind,rightHind}){leg.resetPose();leg.xRot=Mth.HALF_PI-breath;leg.yRot=leg.zRot=0;}
        tail.resetPose();tailTip.resetPose();tail.xRot=1.45F;tailTip.xRot=1.65F;
    }
    /** Original accessories.22 forepaw anchors; do not lower the limbs or shift the harness. */
    public static void front(ModelPart leg, boolean left, float breath) {
        leg.resetPose();
        leg.xRot = -Mth.HALF_PI + breath;
        leg.yRot = 0;
        leg.zRot = 0;
    }
    private CatRideAnimation() {}
}
