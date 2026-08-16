package cn.laowu.mod.loot;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.CatToolBehavior;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import org.jetbrains.annotations.NotNull;

/** Converts only actual block drops, so Fortune, Silk Touch and modded loot tables run first. */
public final class CatToolEmpoweredLootModifier extends LootModifier {
    public static final Codec<CatToolEmpoweredLootModifier> CODEC = RecordCodecBuilder.create(
            instance -> codecStart(instance).apply(instance, CatToolEmpoweredLootModifier::new));

    public CatToolEmpoweredLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(
            ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
        BlockState mined = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (tool == null || mined == null || !CatToolBehavior.isEmpowered(tool)) return generatedLoot;

        if (tool.is(LaoWuMod.CAT_AXE.get())) {
            return stripWoodDrops(generatedLoot, mined);
        }
        if (tool.is(LaoWuMod.CAT_PICKAXE.get())) {
            return smeltDrops(generatedLoot, context);
        }
        return generatedLoot;
    }

    private static ObjectArrayList<ItemStack> stripWoodDrops(
            ObjectArrayList<ItemStack> generatedLoot, BlockState mined) {
        BlockState stripped = AxeItem.getAxeStrippingState(mined);
        if (stripped == null) return generatedLoot;
        Item originalItem = mined.getBlock().asItem();
        Item strippedItem = stripped.getBlock().asItem();
        if (strippedItem == net.minecraft.world.item.Items.AIR) return generatedLoot;

        for (int i = 0; i < generatedLoot.size(); i++) {
            ItemStack drop = generatedLoot.get(i);
            if (!drop.is(originalItem)) continue;
            ItemStack replacement = new ItemStack(strippedItem, drop.getCount());
            generatedLoot.set(i, replacement);
        }
        return generatedLoot;
    }

    private static ObjectArrayList<ItemStack> smeltDrops(
            ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ObjectArrayList<ItemStack> converted = new ObjectArrayList<>();
        for (ItemStack drop : generatedLoot) {
            if (drop.isEmpty()) continue;
            SimpleContainer input = new SimpleContainer(drop.copyWithCount(1));
            SmeltingRecipe recipe = context.getLevel().getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, input, context.getLevel())
                    .orElse(null);
            if (recipe == null) {
                converted.add(drop);
                continue;
            }
            ItemStack result = recipe.assemble(input, context.getLevel().registryAccess());
            if (result.isEmpty()) {
                converted.add(drop);
                continue;
            }
            appendSplit(converted, result, result.getCount() * drop.getCount());
        }
        return converted;
    }

    private static void appendSplit(ObjectArrayList<ItemStack> output, ItemStack template, int total) {
        while (total > 0) {
            ItemStack stack = template.copy();
            int count = Math.min(total, stack.getMaxStackSize());
            stack.setCount(count);
            output.add(stack);
            total -= count;
        }
    }
}
