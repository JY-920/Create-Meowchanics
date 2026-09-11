package cn.laowu.mod.mixin;
import cn.laowu.mod.CatTeamRules;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Make vanilla target-goal alliance checks agree with collar teams, without editing scoreboards. */
@Mixin(TamableAnimal.class)
public abstract class PetCollarTeamMixin {
    @Inject(method = {"isAlliedTo", "m_7307_"}, at = @At("HEAD"), cancellable = true)
    private void laowu$collarAlly(Entity target, CallbackInfoReturnable<Boolean> cir) {
        TamableAnimal self = (TamableAnimal) (Object) this;
        if (self.isTame() && target instanceof TamableAnimal other && other.isTame())
            cir.setReturnValue(CatTeamRules.friendly(self, other));
    }
}
