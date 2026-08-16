package cn.laowu.mod.mixin;

import cn.laowu.mod.CatPancakeBehavior;
import com.simibubi.create.content.contraptions.actors.roller.RollerMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hooks the moving roller actor when it advances to a new paving position. */
@Mixin(value = RollerMovementBehaviour.class, remap = false)
abstract class RollerMovementBehaviourMixin {
    @Inject(method = "visitNewPosition", at = @At("HEAD"), remap = false)
    private void laowu$flattenCatsUnderMovingRoller(MovementContext context, BlockPos pavingPos,
                                                    CallbackInfo callback) {
        if (context.world.isClientSide) return;

        // Create passes the road/paving block here, two blocks below the roller.
        // Its own damage AABB only covers that lower block, while a cat standing
        // on the road begins at pavingPos.y + 1.  Extend that same native contact
        // cell upward to cover the actual moving roller body.
        AABB rollerColumn = new AABB(pavingPos).inflate(0.15D, 0.0D, 0.15D)
                .expandTowards(0.0D, 2.0D, 0.0D);
        for (Cat cat : context.world.getEntitiesOfClass(Cat.class, rollerColumn,
                cat -> cat.isAlive() && !cat.isPassengerOfSameVehicle(context.contraption.entity))) {
            CatPancakeBehavior.ignoreRollerContraption(cat, context.contraption.entity.getUUID());
            CatPancakeBehavior.flatten(cat);
        }
    }
}
