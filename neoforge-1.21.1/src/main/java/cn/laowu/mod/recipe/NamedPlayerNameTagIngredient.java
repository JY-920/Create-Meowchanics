package cn.laowu.mod.recipe;

import cn.laowu.mod.LaoWuMod;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * A renamed Name Tag whose text identifies the future cat owner.
 *
 * <p>Create matches basin ingredients and rolls the result synchronously on
 * the same server thread. Keeping the just-matched text in a ThreadLocal lets
 * the specialized mixing recipe copy that text without registry scans or a
 * global mutable recipe result.</p>
 */
public final class NamedPlayerNameTagIngredient implements ICustomIngredient {
    private static final ThreadLocal<String> MATCHED_OWNER = new ThreadLocal<>();
    private static final MapCodec<NamedPlayerNameTagIngredient> CODEC =
            MapCodec.unit(NamedPlayerNameTagIngredient::new);

    public static IngredientType<NamedPlayerNameTagIngredient> createType() {
        return new IngredientType<>(CODEC);
    }

    @Override
    public boolean test(ItemStack stack) {
        MATCHED_OWNER.remove();
        if (!stack.is(Items.NAME_TAG) || !stack.has(DataComponents.CUSTOM_NAME)) {
            return false;
        }
        String owner = stack.getHoverName().getString().trim();
        if (owner.isEmpty()) return false;
        MATCHED_OWNER.set(owner);
        return true;
    }

    public static Optional<String> consumeMatchedOwner() {
        String owner = MATCHED_OWNER.get();
        MATCHED_OWNER.remove();
        return Optional.ofNullable(owner);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public Stream<ItemStack> getItems() {
        return Stream.of(displayStack());
    }

    @Override
    public IngredientType<?> getType() {
        return LaoWuMod.NAMED_PLAYER_NAME_TAG_INGREDIENT.get();
    }

    private static ItemStack displayStack() {
        ItemStack stack = new ItemStack(Items.NAME_TAG);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(
                "ingredient.laowu.named_player_name_tag"));
        return stack;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof NamedPlayerNameTagIngredient;
    }

    @Override
    public int hashCode() {
        return NamedPlayerNameTagIngredient.class.hashCode();
    }
}
