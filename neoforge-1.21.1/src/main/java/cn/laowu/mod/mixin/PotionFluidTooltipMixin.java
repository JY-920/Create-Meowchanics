package cn.laowu.mod.mixin;

import cn.laowu.mod.HissingPotionTooltip;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Applies the mod's one-use attack wording to Create's potion-fluid tooltip. */
@Mixin(value = PotionFluidHandler.class, remap = false)
abstract class PotionFluidTooltipMixin {
    @Redirect(
            method = "addPotionTooltip(Lnet/neoforged/neoforge/fluids/FluidStack;Ljava/util/function/Consumer;F)V",
            at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"),
            remap = false
    )
    private static void laowu$rewriteHissingAttackLine(Consumer<Component> consumer,
                                                       Object value,
                                                       FluidStack fluid,
                                                       Consumer<Component> originalConsumer,
                                                       float durationFactor) {
        if (!(value instanceof Component component)) return;
        PotionContents contents = fluid.getOrDefault(DataComponents.POTION_CONTENTS,
                PotionContents.EMPTY);
        List<Component> line = new ArrayList<>(List.of(component));
        HissingPotionTooltip.rewrite(contents.getAllEffects(), line);
        consumer.accept(line.getFirst());
    }
}
