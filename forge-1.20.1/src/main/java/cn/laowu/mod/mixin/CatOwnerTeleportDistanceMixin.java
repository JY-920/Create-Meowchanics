package cn.laowu.mod.mixin;
import cn.laowu.mod.CareerCatBehavior;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;
/** Vanilla follow AI must use the same cat recall distance as career combat; other pets are unchanged. */
@Mixin(FollowOwnerGoal.class)
public abstract class CatOwnerTeleportDistanceMixin {
    @Shadow(aliases = "f_25283_") @Final private TamableAnimal tamable;
    @ModifyConstant(method = {"tick", "m_8037_"}, constant = @Constant(doubleValue = 144.0D))
    private double laowu$catRecallDistance(double vanilla) {
        return tamable instanceof Cat ? CareerCatBehavior.MAX_OWNER_DISTANCE_SQR : vanilla;
    }
}
