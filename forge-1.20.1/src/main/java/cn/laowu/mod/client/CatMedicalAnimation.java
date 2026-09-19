package cn.laowu.mod.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Standing, open-limb combat recovery; only cushion work uses the seated clinic pose. */
public final class CatMedicalAnimation {
    public static void apply(float age, ModelPart head, ModelPart body, ModelPart leftHind,
                             ModelPart rightHind, ModelPart leftFront, ModelPart rightFront,
                             ModelPart tail, ModelPart tailTip) {
        float t = Mth.clamp(age / 8, 0, 1), w = t * t * (3 - 2 * t);
        float breath = Mth.sin(age * .15F) * .025F;
        // The donor's recovery uses a standing torso and open, low paws, not a sitting base.
        // Keep the native shoulder/hip anchors so the outfit remains attached.
        pose(head, .10F, 15, -9, w);
        pose(body, Mth.HALF_PI + breath, 12, -10, w);
        pose(leftFront, -.12F + breath, 14.1F, -5, w);
        pose(rightFront, -.12F + breath, 14.1F, -5, w);
        leftFront.yRot = -.12F * w; rightFront.yRot = .12F * w;
        leftFront.zRot = -.40F * w; rightFront.zRot = .40F * w;
        pose(leftHind, .18F, 18, 5, w);
        pose(rightHind, .18F, 18, 5, w);
        leftHind.zRot = .13F * w; rightHind.zRot = -.13F * w;
        pose(tail, .78F + breath, 15, 8, w);
        pose(tailTip, 1.8978761F, 20, 14, w);
    }
    /** A separate clinic channel, anchored to vanilla's seated skeleton on the cushion. */
    public static void applyStationed(float age, ModelPart head, ModelPart body, ModelPart leftHind,
                                      ModelPart rightHind, ModelPart leftFront, ModelPart rightFront,
                                      ModelPart tail, ModelPart tailTip) {
        float t = Mth.clamp(age / 10, 0, 1), w = t * t * (3 - 2 * t);
        float breath = Mth.sin(age * .12F) * .018F;
        pose(body, Mth.PI / 4, 8, -5, w);
        pose(head, .08F + breath, 11.7F, -8, w);
        pose(leftHind, -Mth.HALF_PI, 21, 1, w);
        pose(rightHind, -Mth.HALF_PI, 21, 1, w);
        pose(leftFront, -.78F + breath, 15.2F, -7, w);
        pose(rightFront, -.78F + breath, 15.2F, -7, w);
        leftFront.zRot = -.30F * w; rightFront.zRot = .30F * w;
        leftFront.yRot = -.08F * w; rightFront.yRot = .08F * w;
        pose(tail, 1.7278761F, 23, 6, w);
        pose(tailTip, 2.670354F, 22, 13.2F, w);
    }
    private static void pose(ModelPart part, float pitch, float y, float z, float weight) {
        part.xRot = Mth.lerp(weight, part.xRot, pitch);
        part.y = Mth.lerp(weight, part.y, y); part.z = Mth.lerp(weight, part.z, z);
    }
    private CatMedicalAnimation() {}
}
