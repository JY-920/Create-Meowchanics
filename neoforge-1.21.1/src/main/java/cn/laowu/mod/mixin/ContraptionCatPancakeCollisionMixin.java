package cn.laowu.mod.mixin;

import cn.laowu.mod.CatPancakeBehavior;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps a roller from resolving its collision by shoving its new pancake aside. */
@Mixin(value = Entity.class, remap = false)
abstract class ContraptionCatPancakeCollisionMixin {
    // This method is declared by Entity, not AbstractContraptionEntity. List both
    // the readable and legacy runtime aliases because this project has no refmap.
    @Inject(method = {"canCollideWith", "m_7337_"}, at = @At("HEAD"), cancellable = true,
            remap = false, require = 1)
    private void laowu$ignorePancakeMadeByThisRoller(Entity entity,
                                                     CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof AbstractContraptionEntity contraption
                && entity instanceof Cat cat
                && CatPancakeBehavior.ignoresContraption(cat, contraption.getUUID())) {
            callback.setReturnValue(false);
        }
    }
}
