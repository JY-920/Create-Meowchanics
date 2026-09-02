package cn.laowu.mod.recipe;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.item.CatPancakeItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.stream.Stream;
import java.util.Optional;
import java.util.Objects;

/**
 * Matches every cat pancake regardless of its captured cat data, while exposing
 * one concrete NBT-bearing stack per cat variant to JEI. This makes Create's own
 * cutting and compacting categories cycle valid cat textures automatically.
 */
public final class CatPancakeIngredient implements ICustomIngredient {
    private static final MapCodec<CatPancakeIngredient> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("baby")
                            .forGetter(ingredient -> Optional.ofNullable(ingredient.baby)),
                    Codec.STRING.optionalFieldOf("outfit")
                            .forGetter(ingredient -> Optional.ofNullable(ingredient.outfit)
                                    .map(CatOutfitType::id))
            ).apply(instance, (baby, outfit) -> new CatPancakeIngredient(
                    baby.orElse(null), outfit.map(CatOutfitType::byId).orElse(null))));

    private final Boolean baby;
    private final CatOutfitType outfit;

    public CatPancakeIngredient() {
        this(null, null);
    }

    private CatPancakeIngredient(Boolean baby, CatOutfitType outfit) {
        this.baby = baby;
        this.outfit = outfit;
    }

    public static IngredientType<CatPancakeIngredient> createType() {
        return new IngredientType<>(CODEC);
    }

    @Override
    public boolean test(ItemStack stack) {
        return stack.is(LaoWuMod.CAT_PANCAKE.get())
                && (baby == null || CatPancakeItem.isBaby(stack) == baby)
                && (outfit == null || CatPancakeItem.getOutfit(stack) == outfit);
    }

    @Override
    public Stream<ItemStack> getItems() {
        ItemStack display = baby != null && baby
                ? CatPancakeItem.defaultBabyDisplayStack()
                : CatPancakeItem.defaultDisplayStack();
        if (outfit != null && outfit != CatOutfitType.NONE) {
            CatPancakeItem.equipOutfit(display, outfit);
        }
        return Stream.of(display);
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
        return other instanceof CatPancakeIngredient ingredient
                && Objects.equals(baby, ingredient.baby)
                && outfit == ingredient.outfit;
    }

    public int hashCode() {
        return Objects.hash(baby, outfit);
    }
}
