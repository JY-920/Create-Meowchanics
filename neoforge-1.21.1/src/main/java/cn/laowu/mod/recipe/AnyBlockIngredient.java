package cn.laowu.mod.recipe;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatMaterialRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.stream.Stream;

/**
 * Matches every visible item-backed block without expanding the ingredient to
 * the entire block registry. Recipe viewers receive one renamed stone stack,
 * so the slot remains stable even in very large modpacks.
 */
public final class AnyBlockIngredient implements ICustomIngredient {
    private static final MapCodec<AnyBlockIngredient> CODEC =
            MapCodec.unit(AnyBlockIngredient::new);

    public static IngredientType<AnyBlockIngredient> createType() {
        return new IngredientType<>(CODEC);
    }

    @Override
    public boolean test(ItemStack stack) {
        return CatMaterialRegistry.blockMaterial(stack).isPresent();
    }

    @Override
    public Stream<ItemStack> getItems() {
        return Stream.of(displayStack());
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return LaoWuMod.ANY_BLOCK_INGREDIENT.get();
    }

    private static ItemStack displayStack() {
        ItemStack stack = new ItemStack(Items.STONE);
        stack.set(DataComponents.CUSTOM_NAME,
                Component.translatable("ingredient.laowu.any_block"));
        return stack;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AnyBlockIngredient;
    }

    @Override
    public int hashCode() {
        return AnyBlockIngredient.class.hashCode();
    }
}
