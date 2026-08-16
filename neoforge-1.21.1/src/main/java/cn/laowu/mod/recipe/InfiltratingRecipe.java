package cn.laowu.mod.recipe;

import cn.laowu.mod.LaoWuMod;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * A Create processing recipe specialized for the infiltration tank. It uses
 * Create's FluidIngredient, ProcessingOutput and HeatCondition serialization.
 */
public final class InfiltratingRecipe extends BasinRecipe {
    public static final ResourceLocation CAT_TOOL_REPAIR_ID =
            LaoWuMod.id("cat_tool_repair_infiltrating");
    public static final ResourceLocation POTION_ARROW_ID =
            LaoWuMod.id("potion_arrow_infiltrating");
    public static final int POTION_ARROW_FLUID_COST = 70;
    private static final IRecipeTypeInfo TYPE_INFO = new IRecipeTypeInfo() {
        @Override public ResourceLocation getId() { return LaoWuMod.id("infiltrating"); }
        @SuppressWarnings("unchecked")
        @Override public <T extends RecipeSerializer<?>> T getSerializer() {
            return (T) LaoWuMod.INFILTRATING_SERIALIZER.get();
        }
        @SuppressWarnings("unchecked")
        @Override public <I extends RecipeInput, R extends Recipe<I>> RecipeType<R> getType() {
            return (RecipeType<R>) LaoWuMod.INFILTRATING_TYPE.get();
        }
    };

    public InfiltratingRecipe(ProcessingRecipeParams params) {
        super(TYPE_INFO, params);
        // The tank is never a cold-processing machine. Datapacks may select
        // HEATED or SUPERHEATED using Create's normal heatRequirement field.
        if (requiredHeat == HeatCondition.NONE) requiredHeat = HeatCondition.HEATED;
    }

    @Override protected int getMaxInputCount() { return 9; }
    @Override protected int getMaxOutputCount() { return 9; }
    @Override protected int getMaxFluidInputCount() { return 1; }
    @Override protected int getMaxFluidOutputCount() { return 1; }

    public static boolean isCatToolRepair(ResourceLocation id) {
        return CAT_TOOL_REPAIR_ID.equals(id);
    }

    public static boolean isPotionArrow(ResourceLocation id) {
        return POTION_ARROW_ID.equals(id);
    }
}
