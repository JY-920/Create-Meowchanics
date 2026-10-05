package cn.laowu.mod.client;

import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.CatGiantMount;
import cn.laowu.mod.CatOutfitType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.animal.Cat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** GPU fixtures share the existing giant capture utility; never included in production. */
final class GiantCatHeadOutfitVisualProbe {
    private static final Path FRAMES = Path.of("giant-head-outfit-frames");

    static void verify(Minecraft minecraft) throws Exception {
        Method makeCat = GiantCatVisualProbe.class.getDeclaredMethod(
                "cat", Minecraft.class, String.class, int.class);
        makeCat.setAccessible(true);
        Cat cat = (Cat) makeCat.invoke(null, minecraft, "red", 97450);
        Files.createDirectories(FRAMES);
        outfit(cat, CatOutfitType.NONE);
        Frame naked = capture(minecraft, null, () -> drawRegistered(minecraft, cat, 45));
        Frame empty = capture(minecraft, null, () -> drawHeadOnly(minecraft, cat, 45));
        check(empty.pixels == 0, "No career means no giant headwear geometry");
        for (CatOutfitType career : CatOutfitType.values()) {
            if (career == CatOutfitType.NONE) continue;
            outfit(cat, career);
            Frame isolated = capture(minecraft, FRAMES.resolve(career.id() + "-head-only.png"),
                    () -> drawHeadOnly(minecraft, cat, 45));
            check(isolated.pixels > 8 && !isolated.clipped,
                    career + " emits visible headwear using the real texture and GPU");
            check(isolated.width < 180 && isolated.height < 210,
                    career + " isolated headwear cannot include body/vehicle-sized geometry");
            Frame standing = capture(minecraft, FRAMES.resolve(career.id() + "-standing.png"),
                    () -> drawRegistered(minecraft, cat, 45));
            Frame side = capture(minecraft, FRAMES.resolve(career.id() + "-side.png"),
                    () -> drawRegistered(minecraft, cat, 90));
            check(standing.pixels > 3000 && side.pixels > 3000
                            && !standing.clipped && !side.clipped,
                    career + " complete giant remains visible from three-quarter and side");
            check(standing.hash != naked.hash,
                    career + " registered giant renderer actually includes career headwear");
        }
        // Attachment follows the live giant animation rather than ordinary-cat sitting bones.
        outfit(cat, CatOutfitType.FLIGHT);
        cat.setOrderedToSit(true);
        cat.setInSittingPose(true);
        for (int tick = 0; tick < 60; tick++) {
            cat.tickCount++;
            GiantCatAnimation.sample(cat, .5F);
        }
        capture(minecraft, FRAMES.resolve("flight-resting.png"),
                () -> drawRegistered(minecraft, cat, 45));
        capture(minecraft, FRAMES.resolve("flight-resting-head-only.png"),
                () -> drawHeadOnly(minecraft, cat, 45));
        System.out.println("PASS: all 13 giant career headwear GPU meshes/textures and bounded head-only output; registered standing/side and resting-flight fixtures");
    }

    private static void outfit(Cat cat, CatOutfitType outfit) {
        // Set only the client marker, not equip(): no network/gameplay side effects in fixtures.
        cat.getPersistentData().remove(CatClothesData.EQUIPPED_TAG);
        cat.getPersistentData().putString(CatClothesData.OUTFIT_TAG, outfit.id());
    }

    private static PoseStack previewPose(float angle) {
        PoseStack pose = new PoseStack();
        pose.translate(320, 355, -11000);
        // Full side view includes the long tail; reserve enough frame margin for it.
        pose.scale(42, -42, 42);
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        return pose;
    }

    private static void drawRegistered(Minecraft minecraft, Cat cat, float angle) {
        var buffers = minecraft.renderBuffers().bufferSource();
        minecraft.getEntityRenderDispatcher().getRenderer(cat).render(
                cat, 0, .5F, previewPose(angle), buffers, LightTexture.FULL_BRIGHT);
        buffers.endBatch();
    }

    private static void drawHeadOnly(Minecraft minecraft, Cat cat, float angle) {
        PoseStack pose = previewPose(angle);
        pose.mulPose(Axis.YP.rotationDegrees(180));
        float scale = CatGiantMount.SCALE * CatGiantMount.sizeFactor(cat);
        pose.scale(-scale, -scale, scale);
        pose.translate(0, -1.5, 0);
        var buffers = minecraft.renderBuffers().bufferSource();
        GiantCatHeadOutfit.render(cat, .5F, pose, buffers, LightTexture.FULL_BRIGHT,
                RuntimeBlockbenchModel.HeadMotion.NONE, GiantCatAnimation.sample(cat, .5F));
        buffers.endBatch();
    }

    private static Frame capture(Minecraft minecraft, Path path, Runnable draw) throws Exception {
        Method capture = GiantCatVisualProbe.class.getDeclaredMethod(
                "capture", Minecraft.class, String.class, Runnable.class);
        capture.setAccessible(true);
        Object frame;
        try {
            frame = capture.invoke(null, minecraft, path == null ? null : path.toString(), draw);
        } catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof Exception exception) throw exception;
            throw failure;
        }
        return new Frame(((Number) field(frame, "pixels")).intValue(),
                ((Number) field(frame, "width")).intValue(),
                ((Number) field(frame, "height")).intValue(),
                ((Number) field(frame, "hash")).longValue(), (boolean) field(frame, "clipped"));
    }

    private static Object field(Object frame, String name) throws Exception {
        Method accessor = frame.getClass().getDeclaredMethod(name);
        accessor.setAccessible(true);
        return accessor.invoke(frame);
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    private record Frame(int pixels, int width, int height, long hash, boolean clipped) {
    }
}
