package cn.laowu.mod.client;

import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.OcelotModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.animal.Cat;

/** Runs against the real compiled Minecraft models, without starting a world. */
public final class CatCollarPoseRegression {
    private static final String[] BONES = {"head", "body", "left_hind_leg",
            "right_hind_leg", "left_front_leg", "right_front_leg", "tail1", "tail2"};

    public static void main(String[] args) throws ReflectiveOperationException {
        ModelPart sourceRoot = HissingCatModel.createLayer().bakeRoot();
        HissingCatModel source = new HissingCatModel(sourceRoot);
        ModelPart collarRoot = LayerDefinition.create(
                OcelotModel.createBodyMesh(new CubeDeformation(0.01F)), 64, 32).bakeRoot();
        CatModel<Cat> collar = new CatModel<>(collarRoot);
        check(CatStreetDanceAnimation.isAvailable(), "Animation resource missing");
        var collarHeadCube = firstCube(collarRoot.getChild("head"));
        check(collarHeadCube != firstCube(sourceRoot.getChild("head")),
                "Collar must keep its separate inflated geometry");

        // Reproduce the old bug: copying only model flags leaves the head behind.
        applyDance(sourceRoot, 7.25F);
        source.copyPropertiesTo(collar);
        check(!samePose(sourceRoot.getChild("head"), collarRoot.getChild("head")),
                "Old independent collar pose should differ from the animated head");

        int poses = 0;
        for (boolean young : new boolean[]{false, true}) {
            source.young = young;
            source.riding = young;
            source.attackTime = young ? 0.7F : 0.0F;
            for (int step = 0; step <= 960; step++) {
                applyDance(sourceRoot, step / 40.0F);
                source.copyPoseTo(collar, collarRoot);
                checkPose(sourceRoot, collarRoot);
                check(collar.young == source.young && collar.riding == source.riding
                                && collar.attackTime == source.attackTime,
                        "Age/renderer flags did not follow the cat");
                poses++;
            }
        }
        for (String bone : BONES) {
            ModelPart part = sourceRoot.getChild(bone);
            part.visible = false;
            part.skipDraw = true;
        }
        source.copyPoseTo(collar, collarRoot);
        checkPose(sourceRoot, collarRoot);
        // Shared models must not keep dance scales or hidden parts on the next cat.
        for (String bone : BONES) {
            ModelPart part = sourceRoot.getChild(bone);
            part.resetPose();
            part.xScale = part.yScale = part.zScale = 1.0F;
            part.visible = true;
            part.skipDraw = false;
        }
        source.young = false;
        source.copyPoseTo(collar, collarRoot);
        checkPose(sourceRoot, collarRoot);
        check(!collar.young, "Baby state leaked to an adult");
        check(collarHeadCube == firstCube(collarRoot.getChild("head")),
                "Copying pose must not replace inflated collar geometry");
        System.out.println("PASS: " + poses + " adult/baby poses; all 8 bones follow; "
                + "scale/visibility reset; inflated geometry retained");
    }

    private static ModelPart.Cube firstCube(ModelPart part) throws ReflectiveOperationException {
        var cubes = ModelPart.class.getDeclaredField("cubes");
        cubes.setAccessible(true);
        return (ModelPart.Cube) ((java.util.List<?>) cubes.get(part)).get(0);
    }

    private static void applyDance(ModelPart root, float age) {
        CatStreetDanceAnimation.apply(age, root.getChild(BONES[0]), root.getChild(BONES[1]),
                root.getChild(BONES[2]), root.getChild(BONES[3]), root.getChild(BONES[4]),
                root.getChild(BONES[5]), root.getChild(BONES[6]), root.getChild(BONES[7]));
    }

    private static void checkPose(ModelPart source, ModelPart target) {
        for (String bone : BONES) {
            ModelPart a = source.getChild(bone);
            ModelPart b = target.getChild(bone);
            check(samePose(a, b), "Detached collar bone: " + bone);
            check(a.visible == b.visible && a.skipDraw == b.skipDraw,
                    "Visibility did not follow: " + bone);
        }
    }

    private static boolean samePose(ModelPart a, ModelPart b) {
        return a.x == b.x && a.y == b.y && a.z == b.z
                && a.xRot == b.xRot && a.yRot == b.yRot && a.zRot == b.zRot
                && a.xScale == b.xScale && a.yScale == b.yScale && a.zScale == b.zScale;
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
