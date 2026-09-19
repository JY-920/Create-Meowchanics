package cn.laowu.mod.mixin;

import cn.laowu.mod.entity.CatFlightCarrier;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.render.PlayerSkyhookRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reuse Create's complete animated wrench-hanging pose, without altering its real chain-rider list. */
@Mixin(value = PlayerSkyhookRenderer.class, remap = false)
public abstract class PilotSkyhookPoseMixin {
    @Shadow(remap = false) private static void setHangingPose(boolean left, HumanoidModel<?> model) { throw new AssertionError(); }
    @Inject(method = "afterSetupAnim", at = @At("HEAD"), cancellable = true, remap = false)
    private static void laowu$pilotPose(Player player, HumanoidModel<?> model, CallbackInfo ci) {
        if (!(player.getVehicle() instanceof CatFlightCarrier)) return;
        boolean left = (player.getMainArm() == HumanoidArm.LEFT) ^ !AllItems.WRENCH.isIn(player.getMainHandItem());
        setHangingPose(left, model);
        ci.cancel();
    }
}
