package cn.laowu.mod.mixin;

import cn.laowu.mod.CatPoseData;
import cn.laowu.mod.item.CatPancakeItem;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.actors.plough.PloughMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds cat-pancake collection without replacing Create's plough behaviour. */
@Mixin(value = PloughMovementBehaviour.class, remap = false)
abstract class PloughMovementBehaviourMixin {
    @Inject(method = "throwEntity", at = @At("HEAD"), cancellable = true, remap = false)
    private void laowu$scoopCatPancake(MovementContext context, Entity entity, CallbackInfo callback) {
        if (context.world.isClientSide || !(entity instanceof Cat cat)
                || !cat.isAlive() || !CatPoseData.isPancake(cat)) return;

        ((MovementBehaviour) (Object) this).dropItem(context, CatPancakeItem.capture(cat));
        cat.discard();
        callback.cancel();
    }
}
