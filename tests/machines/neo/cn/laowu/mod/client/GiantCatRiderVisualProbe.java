package cn.laowu.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.animal.Cat;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

/** Final GPU dispatcher output, not just the render-offset return value. */
final class GiantCatRiderVisualProbe {
    private static final Path OUTPUT = Path.of("giant-rider-repair-frames");

    interface CheckedAction { void run() throws Exception; }
    static void shifted(Minecraft mc, RemotePlayer rider, CheckedAction neutral, CheckedAction raised) throws Exception {
        Files.createDirectories(OUTPUT);
        neutral.run();
        Frame standing = capture(mc, rider, "fixed-neutral.png");
        raised.run();
        Frame moving = capture(mc, rider, "fixed-raised.png");
        check(standing.pixels > 500 && moving.pixels > 500, "Real dispatcher emits complete player geometry");
        check(Math.abs(Math.abs(moving.top - standing.top) - 45) <= 2,
                "Half-block authored back lift must move final player GPU output by 45px at 90px/block; actual="
                        + (moving.top - standing.top));
        System.out.println("GIANT GPU BOB fixture shift=" + (moving.top - standing.top) + " pixels");
        neutral.run();
        Frame cameraNeutral = capture(mc, rider, "third-person-neutral.png", true);
        raised.run();
        Frame cameraRaised = capture(mc, rider, "third-person-raised.png", true);
        check(Math.abs(Math.abs(cameraRaised.top - cameraNeutral.top) - 45) <= 2,
                "Third-person view must visibly retain the rider's back lift rather than cancelling it with equal camera lift; actual="
                        + (cameraRaised.top - cameraNeutral.top));
    }

    static void heading(Minecraft mc, RemotePlayer rider, Cat cat) throws Exception {
        rider.setYRot(0); rider.yRotO = 0;
        rider.yHeadRot = rider.yHeadRotO = 0;
        rider.yBodyRot = rider.yBodyRotO = 35;
        Frame skew = capture(mc, rider, "rider-prior-heading.png");
        rider.yBodyRot = rider.yBodyRotO = 0;
        Frame aligned = capture(mc, rider, "rider-aligned-heading.png");
        check(skew.hash == aligned.hash,
                "Mounted torso must render with the cat heading, not the rider's prior 35-degree body yaw");
        check(rider.getYRot() == 0 && cat.getVehicle().getYRot() == 0,
                "Aligning render torso must not rotate the player's view or vehicle");
        rider.setYRot(45); rider.yRotO = rider.yHeadRot = rider.yHeadRotO = 45;
        rider.yBodyRot = rider.yBodyRotO = 35;
        Frame looking = capture(mc, rider, "rider-looking-prior-heading.png");
        rider.yBodyRot = rider.yBodyRotO = 0;
        Frame lookingAligned = capture(mc, rider, "rider-looking-aligned-heading.png");
        check(looking.hash == lookingAligned.hash && looking.hash != aligned.hash,
                "Body alignment retains independent nonzero head look in actual GPU geometry");
        check(rider.getYRot() == 45 && rider.yHeadRot == 45, "Rendering never forces player look");
        var vehicle = cat.getVehicle();
        for (boolean riderFirst : new boolean[]{false, true}) {
            GiantCatVisualHeading.clearCache(); cat.tickCount = 100;
            vehicle.setYRot(170); vehicle.yRotO = 170;
            float initial = riderFirst ? GiantCatRiderMotion.bodyYaw(rider, .5F, 35)
                    : GiantCatVisualHeading.yaw(cat, .5F, 0);
            vehicle.yRotO = 170; vehicle.setYRot(-170); cat.tickCount++;
            float first = riderFirst ? GiantCatRiderMotion.bodyYaw(rider, .5F, 35)
                    : GiantCatVisualHeading.yaw(cat, .5F, 0);
            float second = riderFirst ? GiantCatVisualHeading.yaw(cat, .5F, 0)
                    : GiantCatRiderMotion.bodyYaw(rider, .5F, 35);
            check(Math.abs(first - second) < .0001F && first > initial && first - initial <= 18,
                    "Cat/rider render order is idempotent and ±180-degree turns take the bounded short arc");
            check(Math.abs(first - GiantCatRiderMotion.bodyYaw(rider, .5F, 35)) < .0001F,
                    "Repeated rider rendering does not advance the shared heading spring");
        }
        vehicle.setYRot(0); vehicle.yRotO = 0;
        rider.setYRot(0); rider.yRotO = rider.yBodyRot = rider.yBodyRotO = rider.yHeadRot = rider.yHeadRotO = 0;
        GiantCatVisualHeading.clearCache();
    }

    static int top(Minecraft mc, RemotePlayer rider, String name) throws Exception {
        return capture(mc, rider, name).top;
    }

    static void inventory(Minecraft mc, RemotePlayer rider, Cat cat) throws Exception {
        var vehicle = cat.getVehicle();
        rider.yBodyRot = rider.yBodyRotO = rider.yHeadRot = rider.yHeadRotO = 180;
        rider.setYRot(180); rider.yRotO = 180;
        vehicle.setYRot(0); vehicle.yRotO = 0;
        GiantCatVisualHeading.clearCache();
        Frame forward = inventoryFrame(mc, rider, "inventory-world-north.png");
        vehicle.setYRot(80); vehicle.yRotO = 80;
        GiantCatVisualHeading.clearCache();
        Frame turned = inventoryFrame(mc, rider, "inventory-world-turned.png");
        check(forward.pixels > 500 && turned.pixels > 500, "Inventory fixture emits complete player geometry");
        check(forward.hash == turned.hash,
                "Inventory preview must retain its own body heading, independent of world cat yaw");
        cat.tickCount = 300; vehicle.setYRot(0); vehicle.yRotO = 0;
        GiantCatVisualHeading.clearCache();
        GiantCatVisualHeading.yaw(cat, 0, 0);
        vehicle.setYRot(80); vehicle.yRotO = 80;
        inventoryFrame(mc, rider, "inventory-heading-isolation.png");
        float after = GiantCatVisualHeading.yaw(cat, .5F, 0);
        check(after > 0 && after <= 9.0001F,
                "Inventory partialTick=1 must not advance/rewind the world heading spring: " + after);
        try {
            GiantCatRiderMotion.preview(() -> {
                check(GiantCatRiderMotion.offset(rider, 1) == 0
                        && GiantCatRiderMotion.bodyYaw(rider, 1, 123) == 123, "Preview bypasses world presentation");
                throw new IllegalStateException("fixture");
            }).run();
        } catch (IllegalStateException expected) {
            check("fixture".equals(expected.getMessage()), "Expected preview fixture exception");
        }
        check(GiantCatRiderMotion.bodyYaw(rider, .5F, 123) != 123,
                "Preview scope must clear after renderer failure");
        vehicle.setYRot(0); vehicle.yRotO = 0;
        rider.setYRot(0); rider.yRotO = rider.yBodyRot = rider.yBodyRotO = rider.yHeadRot = rider.yHeadRotO = 0;
        GiantCatVisualHeading.clearCache();
    }

    private static Frame inventoryFrame(Minecraft mc, RemotePlayer rider, String name) throws Exception {
        Method capture = GiantCatVisualProbe.class.getDeclaredMethod("capture", Minecraft.class, String.class, Runnable.class);
        capture.setAccessible(true);
        Object frame = capture.invoke(null, mc, OUTPUT.resolve(name).toString(), (Runnable)() -> {
            var gui = new net.minecraft.client.gui.GuiGraphics(mc, mc.renderBuffers().bufferSource());
            gui.pose().translate(0, 0, -11000);
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventory(gui, 320F, 350F, 90F,
                    new org.joml.Vector3f(), new org.joml.Quaternionf().rotateZ((float)Math.PI), null, rider);
            gui.flush();
        });
        return new Frame(((Number)field(frame, "pixels")).intValue(), ((Number)field(frame, "top")).intValue(),
                ((Number)field(frame, "hash")).longValue());
    }

    private static Frame capture(Minecraft mc, RemotePlayer rider, String name) throws Exception {
        return capture(mc, rider, name, false);
    }
    private static Frame capture(Minecraft mc, RemotePlayer rider, String name, boolean thirdPerson) throws Exception {
        Method capture = GiantCatVisualProbe.class.getDeclaredMethod("capture", Minecraft.class, String.class, Runnable.class);
        capture.setAccessible(true);
        Object frame = capture.invoke(null, mc, name == null ? null : OUTPUT.resolve(name).toString(), (Runnable)() -> {
            PoseStack pose = new PoseStack();
            pose.translate(320, 355, -11000);
            pose.scale(90, -90, 90);
            var dispatcher = mc.getEntityRenderDispatcher();
            var buffers = mc.renderBuffers().bufferSource();
            double cameraOffset = 0;
            if (thirdPerson) {
                var camera = new net.minecraft.client.Camera();
                camera.setup(mc.level, rider, true, false, 0);
                cameraOffset = camera.getPosition().y - rider.getY();
            }
            dispatcher.setRenderShadow(false);
            try { dispatcher.render(rider, 0, -cameraOffset, 0, 0, 0, pose, buffers, LightTexture.FULL_BRIGHT); buffers.endBatch(); }
            finally { dispatcher.setRenderShadow(true); }
        });
        return new Frame(((Number)field(frame, "pixels")).intValue(), ((Number)field(frame, "top")).intValue(),
                ((Number)field(frame, "hash")).longValue());
    }

    private static Object field(Object frame, String name) throws Exception {
        Method accessor = frame.getClass().getDeclaredMethod(name);
        accessor.setAccessible(true);
        return accessor.invoke(frame);
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private record Frame(int pixels, int top, long hash) {}
}
