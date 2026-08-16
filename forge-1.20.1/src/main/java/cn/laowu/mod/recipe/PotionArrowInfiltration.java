package cn.laowu.mod.recipe;

import com.simibubi.create.AllFluids;
import com.simibubi.create.content.fluids.potion.PotionFluid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** Converts Create's NBT-bearing potion fluid into the equivalent vanilla tipped arrow. */
public final class PotionArrowInfiltration {
    private static final String BOTTLE_TAG = "Bottle";
    private static final String POTION_TAG = "Potion";
    private static final String CUSTOM_EFFECTS_TAG = "CustomPotionEffects";

    public static boolean isUsablePotionFluid(FluidStack fluid) {
        if (fluid.isEmpty() || !fluid.getFluid().isSame(AllFluids.POTION.get()) || !fluid.hasTag())
            return false;
        CompoundTag tag = fluid.getTag();
        return tag != null && (tag.contains(POTION_TAG, Tag.TAG_STRING)
                || tag.contains(CUSTOM_EFFECTS_TAG, Tag.TAG_LIST));
    }

    public static ItemStack createArrow(FluidStack fluid) {
        if (!isUsablePotionFluid(fluid)) return ItemStack.EMPTY;

        CompoundTag arrowTag = fluid.getTag().copy();
        // Bottle form is only Create fluid metadata; tipped arrows use the
        // remaining vanilla Potion/CustomPotionEffects/CustomPotionColor data.
        arrowTag.remove(BOTTLE_TAG);
        ItemStack arrow = new ItemStack(Items.TIPPED_ARROW);
        arrow.setTag(arrowTag);
        return arrow;
    }

    /** Matching ordered lists let JEI cycle each potion fluid beside its own arrow. */
    public static List<FluidStack> jeiFluids() {
        List<FluidStack> fluids = new ArrayList<>();
        ForgeRegistries.POTIONS.getValues().stream()
                .filter(potion -> potion != Potions.EMPTY)
                .forEach(potion -> fluids.add(PotionFluid.of(
                        InfiltratingRecipe.POTION_ARROW_FLUID_COST,
                        potion, PotionFluid.BottleType.REGULAR)));
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
