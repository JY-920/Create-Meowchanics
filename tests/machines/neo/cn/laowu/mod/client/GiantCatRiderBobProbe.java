package cn.laowu.mod.client;

import cn.laowu.mod.CatGiantMount;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.accessory.CatAccessories;
import cn.laowu.mod.entity.CatGiantCarrier;
import cn.laowu.mod.genetics.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Real transformed PlayerRenderer + Camera; authored bone fixtures have hand-derived heights. */
final class GiantCatRiderBobProbe {
    static void verify(Minecraft mc) throws Exception {
        var player = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "giant-bob"));
        player.setPos(mc.player.getX(), mc.player.getY() + 12, mc.player.getZ());
        player.xo = player.getX(); player.yo = player.getY(); player.zo = player.getZ();
        Cat cat = new Cat(EntityType.CAT, mc.level);
        cat.setTame(true, true); cat.setAge(0); cat.setOwnerUUID(player.getUUID());
        CatTraitData.set(cat, CatTraitProfile.EMPTY);
        CatAttributeData.set(cat, CatAttributeProfile.founder(cat.getRandom()).withValues(CatStat.HEALTH, 100, 100));
        CompoundTag state = new CompoundTag(), effects = new CompoundTag();
        effects.putDouble("giant_mount", 1); state.put("Effects", effects);
        cat.getPersistentData().put(CatAccessories.CLIENT_STATE, state);
        // Vanilla mobInteract predicts orderedToSit locally; that field is not a synced fact.
        var originalHand = mc.player.getMainHandItem();
        cat.setOwnerUUID(mc.player.getUUID());
        mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(com.simibubi.create.AllItems.WRENCH.get()));
        try { cat.mobInteract(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND); }
        finally { mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, originalHand); }
        cat.setOwnerUUID(player.getUUID());
        check(cat.isOrderedToSit(), "Real wrench interaction reproduces the client's predicted sit command");
        cat.setInSittingPose(false); // Authoritative standing packet after mounting clears the synced pose.
        cat.setOnGround(true);
        for (int tick = 0; tick <= 60; tick++) {
            cat.tickCount = tick;
            GiantCatAnimation.sample(cat, 0);
        }
        near(GiantCatAnimation.restWeight(cat), 0,
                "A stale client-only wrench sit command must not revive the side-rest pose after dismount");
        cat.setOrderedToSit(false);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        GiantCatAnimation.clearCache();
        var carrier = new CatGiantCarrier(LaoWuMod.CAT_GIANT_CARRIER.get(), mc.level);
        check(cat.startRiding(carrier, true) && player.startRiding(carrier, true), "Actual client giant passengers");
        var grounded = CatGiantCarrier.class.getDeclaredField("GROUNDED"); grounded.setAccessible(true);
        carrier.getEntityData().set((EntityDataAccessor<Boolean>) grounded.get(null), true);
        var renderer = mc.getEntityRenderDispatcher().getRenderer(player);
        Vec3 position = player.position(); var box = player.getBoundingBox();
        var clips = GiantCatAnimation.class.getDeclaredField("clips"); clips.setAccessible(true);
        var savedClips = clips.get(null);
        try {
            verifyRestMountHeading(player, cat, carrier);
            GiantCatRiderVisualProbe.shifted(mc, player,
                    () -> fixture(clips, "group5", new float[][]{{0, 0, 0, 0}}, null),
                    () -> fixture(clips, "group5", new float[][]{{0, 0, 2, 0}}, null));
            // group5's +2 Blockbench pixels move the visible cat back upward by 2/16 * 4 = .5 blocks.
            fixture(clips, "group5", new float[][]{{0, 0, 2, 0}}, null);
            near(renderer.getRenderOffset(player, 0).y, .5, "Player follows animated root, not fixed carrier seat");
            // group2 pivot Y=7.31111; seat contact Y=11.5. Compression drops it .2*(11.5-7.31111)/4.
            fixture(clips, "group2", null, new float[][]{{0, 1, .8F, 1}});
            near(renderer.getRenderOffset(player, 0).y, -.2094445, "Player follows elastic back compression");
            fixture(clips, "body", new float[][]{{0, 0, 1, 0}}, null);
            near(renderer.getRenderOffset(player, 0).y, .25, "Player follows body-only idle sway");
            CompoundTag bonus = new CompoundTag(); bonus.putInt(CatStat.HEALTH.serializedName(), 300);
            cat.getPersistentData().put(CatTraitScriptState.CLIENT_TAG, bonus);
            near(renderer.getRenderOffset(player, 0).y, .5, "Rider sway scales with giant cat size");
            cat.getPersistentData().remove(CatTraitScriptState.CLIENT_TAG);

            fixture(clips, "group5", new float[][]{{0, 0, 2, 0}}, null);
            Camera first = new Camera(), third = new Camera();
            cat.tickCount = 0;
            first.setup(mc.level, player, false, false, 0);
            for (int tick = 1; tick <= 20; tick++) {
                cat.tickCount = tick;
                first.setup(mc.level, player, false, false, 0);
            }
            double firstBob = first.getPosition().y - player.getY();
            check(firstBob > .025 && firstBob <= .06001, "First-person camera follows gently, capped at six centimetres: " + firstBob);
            third.setup(mc.level, player, true, false, 0);
            near(third.getPosition().y - player.getY(), 0, "Third-person camera must not cancel the rider's visible animation");
            fixture(clips, "body", new float[][]{{0, 0, .16F, 0}}, null);
            Camera small = new Camera();
            cat.tickCount = 0; small.setup(mc.level, player, false, false, 0);
            for (int tick = 1; tick <= 20; tick++) {
                cat.tickCount = tick; small.setup(mc.level, player, false, false, 0);
            }
            check(small.getPosition().y - player.getY() > .03,
                    "Small four-centimetre back motion must remain perceptible in first person");
            fixture(clips, "group5", new float[][]{{0, 0, 2, 0}}, null);
            check(player.position().equals(position) && player.getBoundingBox().equals(box),
                    "Rendering and camera do not move player physics or collision");

            Camera slow = new Camera(), fast = new Camera();
            cat.tickCount = 0; slow.setup(mc.level, player, false, false, 0); fast.setup(mc.level, player, false, false, 0);
            for (int tick = 1; tick <= 10; tick++) {
                cat.tickCount = tick; slow.setup(mc.level, player, false, false, 0);
            }
            for (int frame = 1; frame <= 60; frame++) {
                float age = frame / 6F; cat.tickCount = (int)age;
                fast.setup(mc.level, player, false, false, age - cat.tickCount);
            }
            near(slow.getPosition().y, fast.getPosition().y, "Camera smoothing is frame-rate independent");
            player.stopRiding();
            near(renderer.getRenderOffset(player, 0).y, 0, "Dismount removes third-person animation offset");
            first.setup(mc.level, player, false, false, 0);
            near(first.getPosition().y - player.getY(), 0, "Dismount clears camera sway immediately");
            verifyDisconnect(mc);
            // Real shipped curves: walking, sprinting, airborne pose and landing rebound.
            GiantCatAnimation.clearCache(); clips.set(null, savedClips);
            check(player.startRiding(carrier, true), "Remount for authored motion");
            var speedField = CatGiantCarrier.class.getDeclaredField("HORIZONTAL_SPEED"); speedField.setAccessible(true);
            var anticipation = CatGiantCarrier.class.getDeclaredField("JUMP_WINDUP"); anticipation.setAccessible(true);
            Camera walkingCamera = new Camera(), outsideCamera = new Camera();
            double minimum = Double.POSITIVE_INFINITY, maximum = Double.NEGATIVE_INFINITY;
            double firstMinimum = Double.POSITIVE_INFINITY, firstMaximum = Double.NEGATIVE_INFINITY;
            int gpuMinimum = Integer.MAX_VALUE, gpuMaximum = Integer.MIN_VALUE;
            double[] phaseMinimum = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
            double[] phaseMaximum = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
            int[] phaseGpuMinimum = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
            int[] phaseGpuMaximum = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            for (int frame = 0; frame < 240; frame++) {
                float age = frame / 4F; cat.tickCount = (int)age; float partial = age - cat.tickCount;
                carrier.getEntityData().set((EntityDataAccessor<Float>)speedField.get(null), frame < 80 ? .22F : .32F);
                carrier.getEntityData().set((EntityDataAccessor<Boolean>)grounded.get(null), frame < 160 || frame >= 200);
                carrier.getEntityData().set((EntityDataAccessor<Integer>)anticipation.get(null), frame >= 155 && frame < 160 ? 3 : 0);
                double bob = renderer.getRenderOffset(player, partial).y;
                near(renderer.getRenderOffset(player, partial).y, bob, "Repeated same-frame sampling must not advance the cat animation");
                minimum = Math.min(minimum, bob); maximum = Math.max(maximum, bob);
                walkingCamera.setup(mc.level, player, false, false, partial);
                outsideCamera.setup(mc.level, player, true, false, partial);
                double firstOffset = walkingCamera.getPosition().y - player.getY();
                firstMinimum = Math.min(firstMinimum, firstOffset); firstMaximum = Math.max(firstMaximum, firstOffset);
                int phase = frame < 80 ? 0 : frame < 160 ? 1 : 2;
                boolean settled = frame >= 40 && frame < 80 || frame >= 104 && frame < 155 || frame >= 160;
                if (settled) {
                    phaseMinimum[phase] = Math.min(phaseMinimum[phase], firstOffset);
                    phaseMaximum[phase] = Math.max(phaseMaximum[phase], firstOffset);
                }
                if (frame % 4 == 0) {
                    int top = GiantCatRiderVisualProbe.top(mc, player,
                            frame % 40 == 0 ? "authored-" + frame + ".png" : null);
                    gpuMinimum = Math.min(gpuMinimum, top); gpuMaximum = Math.max(gpuMaximum, top);
                    if (settled) {
                        phaseGpuMinimum[phase] = Math.min(phaseGpuMinimum[phase], top);
                        phaseGpuMaximum[phase] = Math.max(phaseGpuMaximum[phase], top);
                    }
                }
                check(Math.abs(walkingCamera.getPosition().y - player.getY()) <= .06001,
                        "First-person cap holds through actual run/jump/rebound curves");
                near(outsideCamera.getPosition().y - player.getY(), 0,
                        "Third-person physical follow leaves the authored rider animation visible");
            }
            check(maximum - minimum > .05, "Authored locomotion and jump visibly move the rider, not a constant offset");
            check(gpuMaximum - gpuMinimum > 4, "Real shipped curves move final dispatcher GPU player output");
            String[] phases = {"walk", "run", "jump/rebound"};
            for (int phase = 0; phase < phases.length; phase++) {
                double cameraRange = phaseMaximum[phase] - phaseMinimum[phase];
                int gpuRange = phaseGpuMaximum[phase] - phaseGpuMinimum[phase];
                System.out.println("GIANT PHASE " + phases[phase] + " cameraRange=" + cameraRange + " finalGpuPixels=" + gpuRange);
                check(cameraRange > .005 && gpuRange > 1,
                        "Each settled authored phase independently moves first-person camera and final GPU rider: " + phases[phase]);
            }
            System.out.println("GIANT MOTION DIAGNOSTIC fullRange=" + (maximum - minimum)
                    + " firstPersonRange=" + (firstMaximum - firstMinimum) + " finalGpuPixels=" + (gpuMaximum - gpuMinimum));
            fixture(clips, "group5", new float[][]{{0, 0, 0, 0}}, null);
            GiantCatRiderVisualProbe.heading(mc, player, cat);
            GiantCatRiderVisualProbe.inventory(mc, player, cat);
            check(player.position().equals(position) && player.getBoundingBox().equals(box),
                    "Full animation sequence leaves player physics untouched");
        } finally {
            player.stopRiding(); cat.stopRiding();
            GiantCatAnimation.clearCache(); clips.set(null, savedClips);
        }
        System.out.println("PASS: GIANT RIDER BOB: transformed player offset, root/body/compression/size, gentle FPS-independent camera, dismount, unchanged physics");
    }
    private static void verifyRestMountHeading(RemotePlayer player, Cat cat, CatGiantCarrier carrier) {
        player.stopRiding(); cat.stopRiding();
        GiantCatAnimation.clearCache(); GiantCatVisualHeading.clearCache();
        cat.setOrderedToSit(true); cat.setInSittingPose(true); cat.setOnGround(true);
        cat.yBodyRotO = cat.yBodyRot = 23;
        for (int tick = 0; tick <= 60; tick++) {
            cat.tickCount = tick;
            GiantCatAnimation.sample(cat, 0);
            near(GiantCatVisualHeading.yaw(cat, 0, GiantCatAnimation.restWeight(cat)), 23,
                    "Unmounted side-rest keeps its original heading");
        }
        float previousRest = GiantCatAnimation.restWeight(cat), previousYaw = 23;
        check(previousRest > .99F, "Mount-heading fixture reaches the actual authored side-rest pose");
        cat.setOrderedToSit(false); cat.setInSittingPose(false);
        carrier.yRotO = 170; carrier.setYRot(170);
        check(cat.startRiding(carrier, true) && player.startRiding(carrier, true),
                "Mount real client passengers directly from side-rest");
        for (int tick = 1; tick <= 20; tick++) {
            cat.tickCount = 60 + tick;
            GiantCatAnimation.sample(cat, 0);
            float rest = GiantCatAnimation.restWeight(cat);
            check(rest > .002F && rest < previousRest,
                    "Mount retains a gradual get-up pose while heading responds; tick=" + tick);
            // Alternate cat-first and rider-first consumers of the shared visual heading.
            float first = tick % 2 == 0 ? GiantCatVisualHeading.yaw(cat, 0, rest)
                    : GiantCatRiderMotion.bodyYaw(player, 0, -999);
            float second = tick % 2 == 0 ? GiantCatRiderMotion.bodyYaw(player, 0, -999)
                    : GiantCatVisualHeading.yaw(cat, 0, rest);
            near(second, first, "Cat/rider heading parity during get-up is independent of render order");
            check(first > previousYaw && first <= previousYaw + 18.001F,
                    "Mounted model turns from the first get-up tick without snapping; tick=" + tick
                            + " previous=" + previousYaw + " actual=" + first + " rest=" + rest);
            near(GiantCatAnimation.restWeight(cat), rest, "Repeated heading consumers cannot advance get-up twice");
            previousRest = rest; previousYaw = first;
        }
        check(previousYaw > 169 && previousYaw <= 170, "Rider heading converges before the rest blend finishes");
        player.stopRiding(); cat.stopRiding();
        cat.setOrderedToSit(true); cat.setInSittingPose(true);
        cat.yBodyRotO = cat.yBodyRot = -80;
        cat.tickCount++;
        GiantCatAnimation.sample(cat, 0);
        near(GiantCatVisualHeading.yaw(cat, 0, GiantCatAnimation.restWeight(cat)), previousYaw,
                "Dismounted resting body locks again instead of following a new look target");
        cat.setOrderedToSit(false); cat.setInSittingPose(false);
        cat.tickCount = 0; cat.yBodyRotO = cat.yBodyRot = 0;
        carrier.yRotO = 0; carrier.setYRot(0);
        check(cat.startRiding(carrier, true) && player.startRiding(carrier, true), "Restore rider motion fixture");
        GiantCatAnimation.clearCache(); GiantCatVisualHeading.clearCache();
        System.out.println("PASS: GIANT REST MOUNT HEADING: immediate smooth turn, retained get-up blend, cat/rider parity, unmounted lock");
    }
    private record ForgottenRide(Camera camera, java.lang.ref.WeakReference<CatGiantCarrier> vehicle) {}
    private static ForgottenRide disconnectedRide(Minecraft mc) {
        var owner = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "disconnected-bob"));
        Cat cat = new Cat(EntityType.CAT, mc.level);
        cat.setTame(true, true);
        cat.setAge(0); cat.setOwnerUUID(owner.getUUID());
        CatTraitData.set(cat, CatTraitProfile.EMPTY);
        CatAttributeData.set(cat, CatAttributeProfile.founder(cat.getRandom()).withValues(CatStat.HEALTH, 100, 100));
        CompoundTag state = new CompoundTag(), effects = new CompoundTag();
        effects.putDouble("giant_mount", 1); state.put("Effects", effects);
        cat.getPersistentData().put(CatAccessories.CLIENT_STATE, state);
        var vehicle = new CatGiantCarrier(LaoWuMod.CAT_GIANT_CARRIER.get(), mc.level);
        check(cat.startRiding(vehicle, true) && owner.startRiding(vehicle, true), "Disconnected fixture mounted");
        var camera = new Camera();
        camera.setup(mc.level, owner, false, false, 0);
        camera.reset();
        return new ForgottenRide(camera, new java.lang.ref.WeakReference<>(vehicle));
    }
    private static void verifyDisconnect(Minecraft mc) throws Exception {
        ForgottenRide forgotten = disconnectedRide(mc);
        for (int attempt = 0; attempt < 20 && forgotten.vehicle().get() != null; attempt++) {
            System.gc(); Thread.sleep(10);
        }
        check(forgotten.vehicle().get() == null,
                "Camera.reset must not keep the old giant carrier/world reachable after disconnect");
        java.lang.ref.Reference.reachabilityFence(forgotten.camera());
    }
    private static void fixture(java.lang.reflect.Field clips, String bone, float[][] position, float[][] scale) throws Exception {
        GiantCatAnimation.clearCache();
        var channels = Class.forName("cn.laowu.mod.client.GiantCatAnimation$Channels").getDeclaredConstructors()[0];
        var clip = Class.forName("cn.laowu.mod.client.GiantCatAnimation$Clip").getDeclaredConstructors()[0];
        channels.setAccessible(true); clip.setAccessible(true);
        Object channel = channels.newInstance(position, null, scale);
        clips.set(null, Map.of("idle", clip.newInstance(1F, true, Map.of(bone, channel))));
    }
    private static void near(double actual, double expected, String label) {
        check(Math.abs(actual - expected) < .0001, label + ": " + actual + " != " + expected);
    }
    private static void check(boolean okay, String label) { if (!okay) throw new AssertionError(label); }
}
