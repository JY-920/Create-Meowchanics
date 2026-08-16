package cn.laowu.mod.recipe;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.CatPancakeItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.stream.Stream;

/**
 * Matches every cat pancake regardless of its captured cat data, while exposing
 * one concrete NBT-bearing stack per cat variant to JEI. This makes Create's own
 * cutting and compacting categories cycle valid cat textures automatically.
 */
public final class CatPancakeIngredient implements ICustomIngredient {
    private static final MapCodec<CatPancakeIngredient> CODEC =
            MapCodec.unit(CatPancakeIngredient::new);

    public static IngredientType<CatPancakeIngredient> createType() {
        return new IngredientType<>(CODEC);
    }

    @Override
    public boolean test(ItemStack stack) {
        return stack.is(LaoWuMod.CAT_PANCAKE.get());
    }

    @Override
    public Stream<ItemStack> getItems() {
        return CatPancakeItem.jeiDisplayStacks().stream();
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return LaoWuMod.CAT_PANCAKE_INGREDIENT.get();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CatPancakeIngredient;
    }

    public int hashCode() {
        return CatPancakeIngredient.class.hashCode();
    }
}
