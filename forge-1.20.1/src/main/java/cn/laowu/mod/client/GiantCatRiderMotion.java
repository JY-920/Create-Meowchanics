package cn.laowu.mod.client;

import cn.laowu.mod.CatGiantMount;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.entity.CatGiantCarrier;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.lang.ref.WeakReference;
import java.util.Map;

/** Client presentation only: the physical carrier seat, packets and collision never bob. */
public final class GiantCatRiderMotion {
    private static final ResourceLocation MODEL = LaoWuMod.id("models/entity/giant_cat_mount.bbmodel");
    private static int previewDepth;

    /** Inventory preview supplies its own pose and time; never advance world animation there. */
    public static Runnable preview(Runnable draw) {
        return () -> {
            previewDepth++;
            try { draw.run(); }
            finally { previewDepth--; }
        };
    }

    public static double offset(Entity passenger, float partialTick) {
        Cat cat = mountedCat(passenger);
        if (cat == null) return 0;
        var model = RuntimeBlockbenchModel.get(MODEL);
        PoseStack neutral = modelSpace(), animated = modelSpace();
        if (!model.translateToGroup(neutral, "body", RuntimeBlockbenchModel.HeadMotion.NONE, Map.of())
                || !model.translateToGroup(animated, "body", RuntimeBlockbenchModel.HeadMotion.NONE,
                        GiantCatAnimation.sample(cat, partialTick))) return 0;
        // Convert the standing cat-back contact point into the body's local space.
        // Follow its exact ancestor transforms, including idle sway and elastic scale.
        Vector3f seat = new Matrix4f(neutral.last().pose()).invert().transformPosition(
                new Vector3f(0, (float)(CatGiantMount.SEAT / CatGiantMount.SCALE), 0));
        animated.last().pose().transformPosition(seat);
        double delta = (seat.y - CatGiantMount.SEAT / CatGiantMount.SCALE)
                * CatGiantMount.SCALE * CatGiantMount.sizeFactor(cat);
        return Double.isFinite(delta) ? delta : 0;
    }

    private static PoseStack modelSpace() {
        PoseStack pose = new PoseStack();
        pose.scale(-1, -1, 1);
        pose.translate(0, -1.5, 0);
        return pose;
    }

    private static Cat mountedCat(Entity passenger) {
        if (previewDepth != 0 || !(passenger instanceof Player) || !passenger.isAlive()
                || !(passenger.getVehicle() instanceof CatGiantCarrier carrier)) return null;
        Cat cat = carrier.cat();
        return cat != null && CatGiantMount.active(cat) ? cat : null;
    }

    /** Changes only the rendered torso yaw; player look/control rotations remain untouched. */
    public static float bodyYaw(Entity passenger, float partialTick, float fallback) {
        Cat cat = mountedCat(passenger);
        if (cat == null) return fallback;
        GiantCatAnimation.sample(cat, partialTick);
        return GiantCatVisualHeading.yaw(cat, partialTick, GiantCatAnimation.restWeight(cat));
    }

    /** Owned by each Camera, so alternate views never advance the main camera's smoothing. */
    public static final class CameraSway {
        // Camera.reset clears vanilla references without calling offset again.
        private WeakReference<Entity> carrier = new WeakReference<>(null);
        private float lastAge;
        private double smooth;

        public double offset(Entity focus, float partialTick, boolean thirdPerson) {
            Cat cat = mountedCat(focus);
            if (cat == null) {
                carrier.clear();
                smooth = 0;
                return 0;
            }
            float age = cat.tickCount + partialTick;
            if (carrier.get() != focus.getVehicle() || age < lastAge) {
                carrier = new WeakReference<>(focus.getVehicle());
                lastAge = age;
                smooth = 0;
            }
            double full = GiantCatRiderMotion.offset(focus, partialTick);
            double target = Math.max(-.06, Math.min(.06, full));
            smooth += (target - smooth) * (1 - Math.exp(-(age - lastAge) * .32));
            lastAge = age;
            // Third-person already follows the physical seat. Adding the exact
            // rider animation cancels its visible motion on the player's screen.
            return thirdPerson ? 0 : smooth;
        }
    }

    private GiantCatRiderMotion() {}
}
