package cn.laowu.mod.item;

import cn.laowu.mod.genetics.CatBreedingMode;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Marker item carrying the breeding strategy consumed by a breeding box. */
public final class BreedingCatFoodItem extends Item {
    private final CatBreedingMode mode;

    public BreedingCatFoodItem(Properties properties, CatBreedingMode mode) {
        super(properties);
        this.mode = mode;
    }

    public CatBreedingMode mode() {
        return mode;
    }

    public static Optional<CatBreedingMode> mode(ItemStack stack) {
        return stack.getItem() instanceof BreedingCatFoodItem food
                ? Optional.of(food.mode) : Optional.empty();
    }

    public static boolean isBreedingFood(ItemStack stack) {
        return stack.getItem() instanceof BreedingCatFoodItem;
    }
}
