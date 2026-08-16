package cn.laowu.mod.mixin;

import cn.laowu.mod.HissingPotionTooltip;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Applies the mod's one-use attack wording to Create's potion-fluid tooltip. */
@Mixin(value = PotionFluidHandler.class, remap = false)
abstract class PotionFluidTooltipMixin {
    @Inject(
            method = "addPotionTooltip(Lnet/minecraftforge/fluids/FluidStack;Ljava/util/List;F)V",
            at = @At("RETURN"),
            remap = false
    )
    private static void laowu$rewriteHissingAttackLine(FluidStack fluid,
                                                       List<Component> tooltip,
                                                       float durationFactor,
                                                       CallbackInfo callback) {
        CompoundTag tag = fluid.getTag();
        if (tag == null) return;
        HissingPotionTooltip.rewrite(PotionUtils.getAllEffects(tag), tooltip);
    }
}
