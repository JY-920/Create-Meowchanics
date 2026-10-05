package cn.laowu.mod.mixin;

import cn.laowu.mod.client.GiantCatRiderMotion;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class GiantCatRiderCameraMixin {
    @Unique private final GiantCatRiderMotion.CameraSway laowu$giantSway = new GiantCatRiderMotion.CameraSway();
    @Unique private double laowu$backOffset;

    @Inject(method = "setup", at = @At("HEAD"))
    private void laowu$sampleBack(BlockGetter level, Entity focus, boolean thirdPerson, boolean reverse,
                                 float partialTick, CallbackInfo ci) {
        laowu$backOffset = laowu$giantSway.offset(focus, partialTick, thirdPerson);
    }

    // Adjust the initial eye position before vanilla's third-person wall clipping.
    @ModifyArg(method = "setup", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", ordinal = 0), index = 1)
    private double laowu$followBack(double y) {
        return y + laowu$backOffset;
    }
}
