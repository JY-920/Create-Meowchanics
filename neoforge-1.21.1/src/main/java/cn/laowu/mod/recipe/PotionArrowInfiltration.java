package cn.laowu.mod.recipe;

import com.simibubi.create.AllFluids;
import com.simibubi.create.content.fluids.potion.PotionFluid;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;

/** Converts Create's NBT-bearing potion fluid into the equivalent vanilla tipped arrow. */
public final class PotionArrowInfiltration {
    public static boolean isUsablePotionFluid(FluidStack fluid) {
        if (fluid.isEmpty() || !fluid.getFluid().isSame(AllFluids.POTION.get())) return false;
        PotionContents contents = fluid.get(DataComponents.POTION_CONTENTS);
        return contents != null && (contents.potion().isPresent() || !contents.customEffects().isEmpty());
    }

    public static ItemStack createArrow(FluidStack fluid) {
        if (!isUsablePotionFluid(fluid)) return ItemStack.EMPTY;

        ItemStack arrow = new ItemStack(Items.TIPPED_ARROW);
        arrow.set(DataComponents.POTION_CONTENTS, fluid.get(DataComponents.POTION_CONTENTS));
        return arrow;
    }

    /** Matching ordered lists let JEI cycle each potion fluid beside its own arrow. */
    public static List<FluidStack> jeiFluids() {
        List<FluidStack> fluids = new ArrayList<>();
        BuiltInRegistries.POTION.holders()
                .forEach(potion -> fluids.add(PotionFluid.of(
                        InfiltratingRecipe.POTION_ARROW_FLUID_COST,
                        new PotionContents(potion), PotionFluid.BottleType.REGULAR)));
        return fluids;
    }

    public static List<ItemStack> jeiArrows() {
        List<ItemStack> arrows = new ArrayList<>();
        for (FluidStack fluid : jeiFluids()) {
            ItemStack arrow = createArrow(fluid);
            if (!arrow.isEmpty()) arrows.add(arrow);
        }
        return arrows;
    }

    private PotionArrowInfiltration() {
    }
}
