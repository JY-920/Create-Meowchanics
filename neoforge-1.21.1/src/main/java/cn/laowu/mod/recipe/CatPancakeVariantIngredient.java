package cn.laowu.mod.recipe;

import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.CatPancakeItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.Optional;
import java.util.stream.Stream;

/** One exact cat skin and outfit state, used to keep paired JEI slots in sync. */
public final class CatPancakeVariantIngredient implements ICustomIngredient {
    private static final MapCodec<CatPancakeVariantIngredient> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("variant")
                            .forGetter(ingredient -> ingredient.variant),
                    Codec.STRING.optionalFieldOf("outfit")
                            .forGetter(ingredient -> Optional.of(ingredient.outfit.id())),
                    Codec.BOOL.optionalFieldOf("terminator", false)
                            .forGetter(ingredient -> ingredient.outfit == CatOutfitType.TERMINATOR)
            ).apply(instance, (variant, outfit, terminator) ->
                    new CatPancakeVariantIngredient(variant,
                            outfit.map(CatOutfitType::byId)
                                    .orElse(terminator ? CatOutfitType.TERMINATOR
                                            : CatOutfitType.NONE))));

    private final ResourceLocation variant;
    private final CatOutfitType outfit;

    public CatPancakeVariantIngredient(ResourceLocation variant, boolean terminator) {
        this(variant, terminator ? CatOutfitType.TERMINATOR : CatOutfitType.NONE);
    }

    public CatPancakeVariantIngredient(ResourceLocation variant, CatOutfitType outfit) {
        this.variant = variant;
        this.outfit = outfit;
    }

    public static IngredientType<CatPancakeVariantIngredient> createType() {
        return new IngredientType<>(CODEC);
    }

    @Override
    public boolean test(ItemStack stack) {
        return stack.is(LaoWuMod.CAT_PANCAKE.get())
                && CatPancakeItem.isTamed(stack)
                && variant.equals(CatPancakeItem.variantId(stack))
                && CatPancakeItem.getOutfit(stack) == outfit;
    }

    @Override
    public Stream<ItemStack> getItems() {
        return Stream.of(CatPancakeItem.recipeDisplayStack(variant, outfit));
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return LaoWuMod.CAT_PANCAKE_VARIANT_INGREDIENT.get();
    }
}
