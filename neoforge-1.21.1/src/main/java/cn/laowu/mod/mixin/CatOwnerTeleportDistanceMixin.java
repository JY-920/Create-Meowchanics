package cn.laowu.mod.mixin;
import cn.laowu.mod.CareerCatBehavior;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Cat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;
/** Vanilla follow AI must use the same cat recall distance as career combat; other pets are unchanged. */
@Mixin(TamableAnimal.class)
public abstract class CatOwnerTeleportDistanceMixin {
    @ModifyConstant(method = "shouldTryTeleportToOwner", constant = @Constant(doubleValue = 144.0D))
    private double laowu$catRecallDistance(double vanilla) {
        return (Object) this instanceof Cat ? CareerCatBehavior.MAX_OWNER_DISTANCE_SQR : vanilla;
    }
}
