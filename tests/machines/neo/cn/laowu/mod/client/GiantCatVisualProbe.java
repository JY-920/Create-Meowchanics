package cn.laowu.mod.client;

import cn.laowu.mod.CatGiantMount;
import cn.laowu.mod.accessory.CatAccessories;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatAttributeProfile;
import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.genetics.CatTraitScriptState;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Real registered cat renderer, authored model, coat textures and GPU animation frames. */
final class GiantCatVisualProbe {
    private static final int WIDTH = 640, HEIGHT = 480;
    private static final Path FRAMES = Path.of("giant-cat-frames");

    static void verify(Minecraft mc) throws Exception {
        GiantCatRiderBobProbe.verify(mc);
        CatSixWayClientProbe.check(mc.level != null && mc.player != null, "Giant preview needs the actual quickplay client world");
        var red = cat(mc, "red", 97401);
        var black = cat(mc, "all_black", 97402);
        var renderer = mc.getEntityRenderDispatcher().getRenderer(red);
        CatSixWayClientProbe.check(renderer instanceof HissingCatRenderer, "Giant preview uses the registered cat renderer");
        CatSixWayClientProbe.check(CatGiantMount.active(red) && CatGiantMount.active(black),
                "Adult tame cats with synced collar effect enter the giant render path");
        var redTexture = renderer.getTextureLocation(red);
        var blackTexture = mc.getEntityRenderDispatcher().getRenderer(black).getTextureLocation(black);
        CatSixWayClientProbe.check(!redTexture.equals(blackTexture), "Two coat variants resolve distinct live textures");
        CatSixWayClientProbe.check(mc.getResourceManager().getResource(redTexture).isPresent()
                        && mc.getResourceManager().getResource(blackTexture).isPresent(),
                "Neither giant coat resolves to a missing resource");

        red.getPersistentData().remove(CatAccessories.CLIENT_STATE);
        Frame ordinary = capture(mc, "giant-cat-ordinary-baseline.png", () -> draw(mc, red, WIDTH / 2, 355, 60, 30));
        giant(red);
        Frame huge = capture(mc, "giant-cat-mount-preview.png", () -> {
            draw(mc, red, 170, 355, 35, 30);
            draw(mc, black, 465, 355, 35, 90);
        });
        Frame single = capture(mc, null, () -> draw(mc, red, WIDTH / 2, 355, 60, 30));
        Frame blackSingle = capture(mc, null, () -> draw(mc, black, WIDTH / 2, 355, 60, 30));
        Frame side = capture(mc, null, () -> draw(mc, black, WIDTH / 2, 355, 60, 90));
        CatSixWayClientProbe.check(ordinary.pixels > 300 && single.pixels > 3000,
                "Both ordinary and giant cats emit real GPU geometry");
        CatSixWayClientProbe.check(single.height > ordinary.height * 1.8
                        && single.width > ordinary.width * 1.4,
                "Authored giant silhouette is materially larger than the ordinary cat");
        CatSixWayClientProbe.check(huge.pixels > 6000 && blackSingle.pixels > 3000 && side.pixels > 3000,
                "Three-quarter and side previews both contain giant cat geometry");
        CatSixWayClientProbe.check(single.hash != blackSingle.hash,
                "Distinct resolved coat textures produce distinct GPU pixels on the same giant model");
        verifyHealthScaling(mc);
        verifyRestHeading(mc);
        verifyRestRecovery(mc);
        verifyRestTailFloor(mc);
        verifyRestLook(mc);
        GiantCatHeadOutfitVisualProbe.verify(mc);
        GiantCatBossVisualProbe.verify(mc);

        var still = cat(mc, "red", 97403);
        var stillPose = GiantCatAnimation.sample(still, 0);
        var first = GiantCatAnimation.sample(red, 0);
        CatSixWayClientProbe.check(!first.isEmpty() && first.equals(stillPose), "Both cats begin in the authored idle pose");
        Files.createDirectories(FRAMES);
        Map<String, RuntimeBlockbenchModel.GroupTransform> walk = first, run = first, jump = first, rebound = first;
        Map<String, RuntimeBlockbenchModel.GroupTransform> previous = first;
        int frame = 0;
        for (String phase : new String[]{"idle", "walk", "run", "jump", "rebound"}) {
            for (int step = 0; step < 4; step++) {
                red.tickCount += 2;
                red.setOnGround(!phase.equals("jump"));
                double speed = switch (phase) {
                    case "walk" -> .14;
                    case "run" -> .34;
                    case "jump" -> .18;
                    case "rebound" -> .03;
                    default -> 0;
                };
                red.setDeltaMovement(new Vec3(speed, phase.equals("jump") ? .20 : 0, 0));
                var pose = GiantCatAnimation.sample(red, .5F);
                CatSixWayClientProbe.check(!pose.isEmpty() && finite(pose), "Finite authored pose at " + phase + " frame " + step);
                CatSixWayClientProbe.check(distance(previous, pose) < 24, "No exploding animation step at " + phase + " frame " + step);
                previous = pose;
                Frame image = capture(mc, FRAMES.resolve(String.format("%02d-%s-%d.png", frame++, phase, step)).toString(),
                        () -> draw(mc, red, WIDTH / 2, 355, 60, 45));
                CatSixWayClientProbe.check(image.pixels > 3000, "Actual renderer emits " + phase + " frame " + step);
                if (step == 3) switch (phase) {
                    case "walk" -> walk = pose;
                    case "run" -> run = pose;
                    case "jump" -> jump = pose;
                    case "rebound" -> rebound = pose;
                }
            }
        }
        CatSixWayClientProbe.check(distance(first, walk) > .05 && distance(walk, run) > .05
                        && distance(run, jump) > .05 && distance(jump, rebound) > .05,
                "Idle, walk, run, jump and landing have genuinely distinct sampled poses");
        CatSixWayClientProbe.check(stillPose.equals(GiantCatAnimation.sample(still, 0)),
                "Advancing one giant cat never leaks animation state into another");
        System.out.println("PASS: registered giant-cat renderer, distinct coats, 25/100/400-health GPU scaling, rest/recovery frames, idle/walk/run/jump/rebound frames, per-cat pose isolation");
    }

    private static Cat cat(Minecraft mc, String coat, int id) {
        Cat cat = new Cat(EntityType.CAT, mc.level);
        cat.setId(id);
        cat.setTame(true, true);
        cat.setAge(0);
        cat.setVariant(BuiltInRegistries.CAT_VARIANT.getHolder(ResourceLocation.fromNamespaceAndPath("minecraft", coat)).orElseThrow());
        cat.setOnGround(true);
        health(cat, 100);
        giant(cat);
        return cat;
    }

    /** A fixed-size renderer or a clamped script bonus must fail these silhouette ratios. */
    private static void verifyHealthScaling(Minecraft mc) throws Exception {
        Cat subject = cat(mc, "calico", 97404);
        Frame[] frames = new Frame[3];
        int[] values = {25, 100, 400};
        for (int i = 0; i < values.length; i++) {
            health(subject, values[i]);
            frames[i] = capture(mc, "giant-cat-health-" + values[i] + ".png",
                    () -> draw(mc, subject, WIDTH / 2, 335, 26, 45));
            CatSixWayClientProbe.check(frames[i].pixels > 250 && !frames[i].clipped,
                    "Health " + values[i] + " produces a complete visible GPU silhouette");
        }
        for (int i = 1; i < frames.length; i++) {
            double widthRatio = (double) frames[i].width / frames[i - 1].width;
            double heightRatio = (double) frames[i].height / frames[i - 1].height;
            CatSixWayClientProbe.check(widthRatio > 1.8 && widthRatio < 2.2
                            && heightRatio > 1.8 && heightRatio < 2.2,
                    "Quadrupled effective health doubles rendered dimensions: "
                            + values[i - 1] + " -> " + values[i] + "; ratios=" + widthRatio + "/" + heightRatio);
        }
    }

    /** Missing rest blending, a snap transition, or a stuck rest pose must fail here. */
    private static void verifyRestRecovery(Minecraft mc) throws Exception {
        Files.createDirectories(FRAMES);
        Cat subject = cat(mc, "calico", 97405);
        Cat control = cat(mc, "calico", 97406);
        var previous = GiantCatAnimation.sample(subject, .5F);
        var standing = previous;
        Map<String, RuntimeBlockbenchModel.GroupTransform> firstRest = previous, settledRest = previous;
        Frame baseline = capture(mc, FRAMES.resolve("rest-00-standing.png").toString(),
                () -> draw(mc, subject, WIDTH / 2, 355, 60, 45));
        Frame resting = baseline;
        for (boolean rest : new boolean[]{true, false}) {
            subject.setOrderedToSit(rest);
            subject.setInSittingPose(rest);
            int steps = rest ? 20 : 40;
            for (int step = 0; step < steps; step++) {
                subject.tickCount += 2;
                control.tickCount = subject.tickCount;
                subject.setOnGround(true);
                subject.setDeltaMovement(Vec3.ZERO);
                var pose = GiantCatAnimation.sample(subject, .5F);
                var idle = GiantCatAnimation.sample(control, .5F);
                CatSixWayClientProbe.check(finite(pose) && distance(previous, pose) < 8,
                        "Smooth finite " + (rest ? "rest" : "recovery") + " transition frame " + step);
                previous = pose;
                Frame rendered = capture(mc, FRAMES.resolve(String.format("%s-%02d.png",
                                rest ? "rest-transition" : "rest-recovery", step)).toString(),
                        () -> draw(mc, subject, WIDTH / 2, 355, 60, 45));
                CatSixWayClientProbe.check(rendered.pixels > 3000 && !rendered.clipped,
                        "Complete visible " + (rest ? "rest" : "recovery") + " GPU frame " + step);
                if (rest && step == 0) firstRest = pose;
                if (rest && step == 19) { settledRest = pose; resting = rendered; }
                if (!rest && step == steps - 1) {
                    CatSixWayClientProbe.check(distance(pose, idle) < .15,
                            "After 80 recovery ticks the cat returns to the same-age standing idle pose");
                    CatSixWayClientProbe.check(rendered.height > resting.height * 1.1,
                            "Recovering from rest restores the taller rendered standing silhouette");
                }
            }
        }
        CatSixWayClientProbe.check(distance(standing, settledRest) > 1
                        && distance(standing, firstRest) < distance(standing, settledRest) * .5,
                "Rest is a distinct pose approached gradually rather than snapped into");
        CatSixWayClientProbe.check(resting.height < baseline.height * .9 && resting.hash != baseline.hash,
                "Settled rest visibly lowers the real rendered cat");
    }

    /** Use the actual runtime bone matrices, not just the exporter's geometry math. */
    private static void verifyRestTailFloor(Minecraft mc) throws Exception {
        Cat subject = cat(mc, "calico", 97407);
        subject.setInSittingPose(true);
        for (int tick = 0; tick <= 180; tick++) {
            subject.tickCount = tick;
            GiantCatAnimation.sample(subject, 0);
        }
        for (int value : new int[]{25, 100, 400}) {
            health(subject, value);
            float scale = CatGiantMount.SCALE * CatGiantMount.sizeFactor(subject);
            PoseStack world = new PoseStack();
            world.scale(-scale, -scale, scale);
            world.translate(0, -1.5, 0);
            CatSixWayClientProbe.check(RuntimeBlockbenchModel.get(ResourceLocation.fromNamespaceAndPath("laowu", "models/entity/giant_cat_mount.bbmodel"))
                    .translateToGroup(world, "tail2", RuntimeBlockbenchModel.HeadMotion.NONE,
                            GiantCatAnimation.sample(subject, 0)), "Live tail bone exists");
            var matrix = world.last().pose();
            float lowest = Float.POSITIVE_INFINITY;
            for (float x : new float[]{-.65F, .65F}) for (float y : new float[]{0, 6})
                for (float z : new float[]{-.65F, .65F}) {
                    var vertex = matrix.transformPosition(new org.joml.Vector3f(x / 16, y / 16, z / 16));
                    lowest = Math.min(lowest, vertex.y);
                }
            CatSixWayClientProbe.check(Math.abs(lowest) < .08F * scale / 16,
                    "Live rest tail contacts floor at health " + value + ": " + lowest);
            var near = matrix.transformPosition(new org.joml.Vector3f());
            var far = matrix.transformPosition(new org.joml.Vector3f(0, 6F / 16, 0));
            CatSixWayClientProbe.check(Math.abs(near.y - far.y) < .08F * scale / 16,
                    "Live distal tail lies flat, not tilted, at health " + value);
        }
        health(subject, 100);
        capture(mc, FRAMES.resolve("rest-tail-floor.png").toString(),
                () -> draw(mc, subject, WIDTH / 2, 355, 60, 90));
        System.out.println("PASS: real runtime resting tail grounded and horizontal at 25/100/400 health");
    }

    /** A lying cat must not follow a moving look target with its whole body. */
    private static void verifyRestHeading(Minecraft mc) throws Exception {
        Cat subject = cat(mc, "red", 97420);
        capture(mc, null, () -> draw(mc, subject, WIDTH / 2, 355, 50, 45));
        subject.setOrderedToSit(true);
        subject.setInSittingPose(true);
        for (int i = 0; i < 35; i++) {
            subject.tickCount++;
            capture(mc, null, () -> draw(mc, subject, WIDTH / 2, 355, 50, 45));
        }
        float locked = renderedYaw(mc, subject);
        subject.yBodyRotO = subject.yBodyRot = 100;
        for (int i = 0; i < 10; i++) {
            subject.tickCount++;
            capture(mc, null, () -> draw(mc, subject, WIDTH / 2, 355, 50, 45));
            CatSixWayClientProbe.check(Math.abs(renderedYaw(mc, subject) - locked) < .001,
                    "Side-lying body heading remains fixed despite live look-controller rotation");
        }
        subject.setOrderedToSit(false);
        subject.setInSittingPose(false);
        subject.tickCount++;
        capture(mc, null, () -> draw(mc, subject, WIDTH / 2, 355, 50, 45));
        CatSixWayClientProbe.check(Math.abs(renderedYaw(mc, subject) - locked) < .001,
                "Heading remains locked during the get-up transition");
        for (int i = 0; i < 70; i++) {
            subject.tickCount++;
            capture(mc, null, () -> draw(mc, subject, WIDTH / 2, 355, 50, 45));
        }
        CatSixWayClientProbe.check(Math.abs(renderedYaw(mc, subject) - locked) > 20,
                "Standing restores normal smoothed body turning");
    }

/** Live client-world targets must move only the resting head, in bounded smooth steps. */
    private static void verifyRestLook(Minecraft mc) throws Exception {
        Cat subject = cat(mc, "calico", 97430);
        var origin = mc.player.position().add(20, 0, 20);
        subject.setPos(origin);
        subject.xo = origin.x; subject.yo = origin.y; subject.zo = origin.z;
        subject.setOrderedToSit(true);
        subject.setInSittingPose(true);
        for (int i = 0; i < 60; i++) {
            subject.tickCount++;
            capture(mc, null, () -> draw(mc, subject, WIDTH / 2, 355, 42, -55));
        }
        float body = renderedFacing(mc, subject, "yaw");
        var target = new net.minecraft.world.entity.animal.Cow(EntityType.COW, mc.level);
        target.setId(97431);
        mc.level.addEntity(target);
        try {
            long[] hashes = new long[3];
            for (int side = 0; side < 3; side++) {
                var pose = GiantCatAnimation.sample(subject, .5F);
                var frameMethod = GiantCatRenderer.class.getDeclaredMethod("headFrame", Cat.class,
                        float.class, float.class, Map.class);
                frameMethod.setAccessible(true);
                Object frame = frameMethod.invoke(null, subject, .5F, body, pose);
                var positionMethod = frame.getClass().getDeclaredMethod("position");
                positionMethod.setAccessible(true);
                Vec3 head = (Vec3) positionMethod.invoke(frame);
                Vec3 at = head.add(side == 0 ? 3 : side == 1 ? -3 : 2,
                        1 - target.getEyeHeight(), side == 2 ? 7 : 4);
                target.setPos(at);
                target.xo = at.x; target.yo = at.y; target.zo = at.z;
                float previousYaw = renderedFacing(mc, subject, "headYaw");
                for (int i = 0; i < 50; i++) {
                    subject.tickCount++;
                    Frame image = capture(mc, i == 49 ? FRAMES.resolve("rest-look-" + side + ".png").toString() : null,
                            () -> draw(mc, subject, WIDTH / 2, 355, 42, -55));
                    float headYaw = renderedFacing(mc, subject, "headYaw");
                    float pitch = renderedFacing(mc, subject, "pitch");
                    CatSixWayClientProbe.check(Math.abs(headYaw) <= 25.05 && Math.abs(pitch) <= 10.05,
                            "Rest head observation stays within the approved angular bounds");
                    CatSixWayClientProbe.check(Math.abs(headYaw - previousYaw) <= 2.05,
                            "Rest head follows a moving living target smoothly");
                    CatSixWayClientProbe.check(Math.abs(renderedFacing(mc, subject, "yaw") - body) < .001,
                            "Observing a living target never rotates the lying torso");
                    CatSixWayClientProbe.check(image.pixels > 3000 && !image.clipped,
                            "Target-following rest pose remains fully visible");
                    previousYaw = headYaw;
                    if (i == 49) hashes[side] = image.hash;
                }
                CatSixWayClientProbe.check(side == 0 ? previousYaw < -20
                                : side == 1 ? previousYaw > 20 : previousYaw < -12,
                        "The renderer follows nearby head-local targets, including beyond the body search radius: " + side);
                Frame elevated = capture(mc, FRAMES.resolve("rest-look-elevated-" + side + ".png").toString(),
                        () -> draw(mc, subject, WIDTH / 2, 315, 52, -55, 25));
                CatSixWayClientProbe.check(elevated.pixels > 3000 && !elevated.clipped,
                        "Elevated side view shows the complete resting silhouette");
            }
            CatSixWayClientProbe.check(hashes[0] != hashes[1],
                    "Moving the observation target changes real GPU head geometry");
        } finally {
            target.discard();
        }
        System.out.println("PASS: resting head follows actual living targets with bounded angles, smooth steps and locked torso");
    }


    private static float renderedYaw(Minecraft mc, Cat cat) throws Exception {
        return renderedFacing(mc, cat, "yaw");
    }

    private static float renderedFacing(Minecraft mc, Cat cat, String name) throws Exception {
        var field = HissingCatRenderer.class.getDeclaredField("giant");
        field.setAccessible(true);
        Object renderer = field.get(mc.getEntityRenderDispatcher().getRenderer(cat));
        var mapField = GiantCatRenderer.class.getDeclaredField("facing");
        mapField.setAccessible(true);
        Object facing = ((Map<?, ?>) mapField.get(renderer)).get(cat);
        var yaw = facing.getClass().getDeclaredField(name);
        yaw.setAccessible(true);
        return yaw.getFloat(facing);
    }

    private static void health(Cat cat, int effectiveHealth) {
        CatAttributeProfile profile = CatAttributeProfile.founder(cat.getRandom())
                .withValues(CatStat.HEALTH, Math.min(100, effectiveHealth), 100);
        CatAttributeData.set(cat, profile);
        CompoundTag bonuses = new CompoundTag();
        bonuses.putInt(CatStat.HEALTH.serializedName(), Math.max(0, effectiveHealth - 100));
        cat.getPersistentData().put(CatTraitScriptState.CLIENT_TAG, bonuses);
    }

    private static void giant(Cat cat) {
        CompoundTag state = new CompoundTag();
        CompoundTag effects = new CompoundTag();
        effects.putDouble("giant_mount", 1);
        state.put("Effects", effects);
        ListTag ids = new ListTag();
        ids.add(StringTag.valueOf("laowu:cat_giant_collar"));
        state.put("Ids", ids);
        cat.getPersistentData().put(CatAccessories.CLIENT_STATE, state);
    }

    private static void draw(Minecraft mc, Cat cat, double x, double y, float scale, float angle) {
        draw(mc, cat, x, y, scale, angle, 0);
    }

    private static void draw(Minecraft mc, Cat cat, double x, double y, float scale, float angle, float elevation) {
        PoseStack pose = new PoseStack();
        pose.translate(x, y, -11000);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(elevation));
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        var buffers = mc.renderBuffers().bufferSource();
        mc.getEntityRenderDispatcher().getRenderer(cat).render(cat, 0, .5F, pose, buffers, LightTexture.FULL_BRIGHT);
        buffers.endBatch();
    }

    private static boolean finite(Map<String, RuntimeBlockbenchModel.GroupTransform> pose) {
        for (var transform : pose.values()) {
            if (!Float.isFinite(transform.x()) || !Float.isFinite(transform.y()) || !Float.isFinite(transform.z())
                    || !Float.isFinite(transform.xRot()) || !Float.isFinite(transform.yRot()) || !Float.isFinite(transform.zRot())
                    || !Float.isFinite(transform.scaleX()) || !Float.isFinite(transform.scaleY()) || !Float.isFinite(transform.scaleZ())) return false;
        }
        return true;
    }

    private static double distance(Map<String, RuntimeBlockbenchModel.GroupTransform> a,
                                   Map<String, RuntimeBlockbenchModel.GroupTransform> b) {
        double largest = 0;
        for (String key : a.keySet()) {
            var before = a.get(key);
            var after = b.get(key);
            if (after == null) continue;
            largest = Math.max(largest, Math.abs(before.x() - after.x()) + Math.abs(before.y() - after.y())
                    + Math.abs(before.z() - after.z()) + Math.abs(before.xRot() - after.xRot())
                    + Math.abs(before.yRot() - after.yRot()) + Math.abs(before.zRot() - after.zRot()));
        }
        return largest;
    }

    private static Frame capture(Minecraft mc, String name, Runnable draw) throws Exception {
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix(); view.identity(); RenderSystem.applyModelViewMatrix();
        TextureTarget output = null;
        try (var guard = new CatPerformanceOutline.State(); var framebuffer = new PerformanceSceneSnapshot.Target()) {
            output = new TextureTarget(WIDTH, HEIGHT, true, Minecraft.ON_OSX);
            output.bindWrite(true);
            RenderSystem.clearColor(.10F, .12F, .15F, 1);
            RenderSystem.clearDepth(1); RenderSystem.depthMask(true);
            RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, WIDTH, HEIGHT, 0, 1000, 21000), VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.setShaderColor(1, 1, 1, 1); RenderSystem.enableDepthTest(); RenderSystem.enableCull();
            draw.run();
            try (var image = new NativeImage(WIDTH, HEIGHT, false)) {
                RenderSystem.bindTexture(output.getColorTextureId()); image.downloadTexture(0, false);
                int background = image.getPixelRGBA(0, 0);
                int count = 0, left = WIDTH, right = -1, top = HEIGHT, bottom = -1;
                long hash = 1469598103934665603L;
                for (int y = 0; y < HEIGHT; y++) for (int x = 0; x < WIDTH; x++) {
                    int color = image.getPixelRGBA(x, y);
                    if (color == background) continue;
                    count++; left = Math.min(left, x); right = Math.max(right, x);
                    top = Math.min(top, y); bottom = Math.max(bottom, y);
                    hash = (hash ^ color) * 1099511628211L;
                }
                if (name != null) { image.flipY(); image.writeToFile(Path.of(name)); }
                return new Frame(count, right - left + 1, bottom - top + 1, hash,
                        left <= 1 || right >= WIDTH - 2 || top <= 1 || bottom >= HEIGHT - 2, top);
            }
        } finally {
            if (output != null) output.destroyBuffers();
            view.popMatrix(); RenderSystem.applyModelViewMatrix(); RenderSystem.setProjectionMatrix(projection, sorting);
        }
    }

    private record Frame(int pixels, int width, int height, long hash, boolean clipped, int top) {}
}
