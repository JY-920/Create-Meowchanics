package cn.laowu.mod.compat.jei;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.CatPancakeItem;
import cn.laowu.mod.item.CatToolBehavior;
import cn.laowu.mod.recipe.InfiltratingRecipe;
import cn.laowu.mod.recipe.PotionArrowInfiltration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.compat.jei.ItemIcon;
import com.simibubi.create.compat.jei.category.BasinCategory;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.animations.AnimatedBlazeBurner;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.item.ItemHelper;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.createmod.catnip.data.Pair;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Uses Create's standard basin recipe layout, heat bar and animated block-scene
 * presentation. The only custom visual is replacing Create's basin/mixer with
 * the infiltration tank supplied by the mod.
 */
public final class InfiltratingJeiCategory extends BasinCategory {
    public static final RecipeType<RecipeHolder<BasinRecipe>> TYPE =
            RecipeType.createRecipeHolderType(LaoWuMod.id("infiltrating"));

    private final AnimatedBlazeBurner heater = new AnimatedBlazeBurner();
    private final AnimatedInfiltrationTank tank = new AnimatedInfiltrationTank();

    public InfiltratingJeiCategory() {
        super(new CreateRecipeCategory.Info<>(
                TYPE,
                Component.translatable("jei.laowu.infiltrating"),
                new EmptyBackground(177, 103),
                new ItemIcon(() -> LaoWuMod.INFILTRATION_TANK_ITEM.get().getDefaultInstance()),
                Collections::emptyList,
                List.<Supplier<? extends ItemStack>>of(
                        () -> LaoWuMod.INFILTRATION_TANK_ITEM.get().getDefaultInstance())),
                true);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BasinRecipe recipe, IFocusGroup focuses) {
        setRecipe(builder, recipe, focuses, null);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BasinRecipe> holder,
                          IFocusGroup focuses) {
        setRecipe(builder, holder.value(), focuses, holder.id());
    }

    private void setRecipe(IRecipeLayoutBuilder builder, BasinRecipe recipe, IFocusGroup focuses,
                           net.minecraft.resources.ResourceLocation recipeId) {
        boolean toolRepair = InfiltratingRecipe.isCatToolRepair(recipeId);
        boolean potionArrow = InfiltratingRecipe.isPotionArrow(recipeId);
        Item focusedTool = toolRepair
                ? focuses.getItemStackFocuses()
                        .map(focus -> focus.getTypedValue().getIngredient())
                        .filter(CatToolBehavior::isCatTool)
                        .map(ItemStack::getItem)
                        .findFirst()
                        .orElse(null)
                : null;
        List<Pair<Ingredient, MutableInt>> condensed = ItemHelper.condenseIngredients(recipe.getIngredients());
        int inputCount = condensed.size() + recipe.getFluidIngredients().size();
        int inputOffset = inputCount < 3 ? (3 - inputCount) * 19 / 2 : 0;
        int index = 0;

        for (Pair<Ingredient, MutableInt> pair : condensed) {
            int amount = pair.getSecond().intValue();
            List<ItemStack> displayed = displayedStacks(pair.getFirst(), amount,
                    toolRepair, focusedTool);
            builder.addSlot(RecipeIngredientRole.INPUT,
                            17 + inputOffset + index % 3 * 19,
                            51 - index / 3 * 19)
                    .setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
                    .addItemStacks(displayed);
            index++;
        }

        for (var fluid : recipe.getFluidIngredients()) {
            int x = 17 + inputOffset + index % 3 * 19;
            int y = 51 - index / 3 * 19;
            if (potionArrow) {
                builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                        .setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
                        .addIngredients(NeoForgeTypes.FLUID_STACK,
                                PotionArrowInfiltration.jeiFluids())
                        .setFluidRenderer(InfiltratingRecipe.POTION_ARROW_FLUID_COST,
                                false, 16, 16);
            } else {
                CreateRecipeCategory.addFluidSlot(builder, x, y, fluid);
            }
            index++;
        }

        int outputCount = recipe.getRollableResults().size() + recipe.getFluidResults().size();
        index = 0;
        if (potionArrow) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 142, 51)
                    .setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
                    .addItemStacks(PotionArrowInfiltration.jeiArrows());
        }
        for (var output : potionArrow ? List.<com.simibubi.create.content.processing.recipe.ProcessingOutput>of()
                : recipe.getRollableResults()) {
            int x = 142 - (outputCount % 2 != 0 && index == outputCount - 1
                    ? 0 : index % 2 == 0 ? 10 : -9);
            int y = 51 - index / 2 * 19;
            builder.addSlot(RecipeIngredientRole.OUTPUT, x, y)
                    .setBackground(CreateRecipeCategory.getRenderedSlot(output), -1, -1)
                    .addItemStack(output.getStack())
                    .addRichTooltipCallback(CreateRecipeCategory.addStochasticTooltip(output));
            index++;
        }
        for (var output : recipe.getFluidResults()) {
            int x = 142 - (outputCount % 2 != 0 && index == outputCount - 1
                    ? 0 : index % 2 == 0 ? 10 : -9);
            int y = 51 - index / 2 * 19;
            CreateRecipeCategory.addFluidSlot(builder, x, y, output);
            index++;
        }

        if (toolRepair) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 142, 51)
                    .setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
                    .addItemStacks(CatToolBehavior.jeiRepairedStacks(focusedTool, 750));
        }

        var heat = recipe.getRequiredHeat();
        if (!heat.testBlazeBurner(HeatLevel.NONE)) {
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, 81)
                    .addItemStack(AllBlocks.BLAZE_BURNER.asStack());
        }
        if (!heat.testBlazeBurner(HeatLevel.KINDLED)) {
            builder.addSlot(RecipeIngredientRole.CATALYST, 153, 81)
                    .addItemStack(AllItems.BLAZE_CAKE.asStack());
        }
    }

    private static List<ItemStack> displayedStacks(Ingredient ingredient, int amount,
                                                   boolean toolRepair, Item focusedTool) {
        if (toolRepair && ingredient.test(LaoWuMod.CAT_SWORD.get().getDefaultInstance()))
            return CatToolBehavior.jeiStacks(focusedTool, 750);

        ItemStack pancake = LaoWuMod.CAT_PANCAKE.get().getDefaultInstance();
        if (ingredient.test(pancake)) {
            return List.of(CatPancakeItem.defaultDisplayStack().copyWithCount(amount));
        }

        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack stack : ingredient.getItems())
            stacks.add(stack.copyWithCount(amount));
        return stacks;
    }

    @Override
    public void draw(BasinRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        super.draw(recipe, slots, graphics, mouseX, mouseY);
        int center = getBackground().getWidth() / 2 + 3;
        if (recipe.getRequiredHeat() != com.simibubi.create.content.processing.recipe.HeatCondition.NONE) {
            heater.withHeat(recipe.getRequiredHeat().visualizeAsBlazeBurner())
                    .draw(graphics, center, 55);
        }
        tank.draw(graphics, center, 34);
    }

    private static final class AnimatedInfiltrationTank extends AnimatedKinetics {
        @Override
        public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(xOffset, yOffset, 200.0F);
            pose.mulPose(Axis.XP.rotationDegrees(-15.5F));
            pose.mulPose(Axis.YP.rotationDegrees(22.5F));
            blockElement(LaoWuMod.INFILTRATION_TANK.get().defaultBlockState())
                    .atLocal(0, 1.65, 0)
                    .scale(23)
                    .render(graphics);
            pose.popPose();
        }
    }
}
