package cn.laowu.mod.recipe;

import cn.laowu.mod.LaoWuMod;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import org.jetbrains.annotations.Nullable;

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
public final class NamedPlayerNameTagIngredient extends AbstractIngredient {
    private static final ThreadLocal<String> MATCHED_OWNER = new ThreadLocal<>();
    private static final ThreadLocal<ItemStack> MATCHED_NAME_TAG = new ThreadLocal<>();

    public NamedPlayerNameTagIngredient() {
        super(Stream.of(new Ingredient.ItemValue(displayStack())));
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        MATCHED_OWNER.remove();
        MATCHED_NAME_TAG.remove();
        if (stack == null || !stack.is(Items.NAME_TAG) || !stack.hasCustomHoverName()) {
            return false;
        }
        String owner = stack.getHoverName().getString().trim();
        if (owner.isEmpty()) return false;
        MATCHED_OWNER.set(owner);
        ItemStack preservedNameTag = stack.copy();
        preservedNameTag.setCount(1);
        MATCHED_NAME_TAG.set(preservedNameTag);
        return true;
    }

    public static Optional<String> consumeMatchedOwner() {
        String owner = MATCHED_OWNER.get();
        MATCHED_OWNER.remove();
        return Optional.ofNullable(owner);
    }

    /**
     * Returns the exact renamed Name Tag that supplied the owner name.
     * Create removes basin inputs before asking a recipe for container
     * remainders, so the matched stack has to be retained alongside its text.
     */
    public static Optional<ItemStack> consumeMatchedNameTag() {
        ItemStack nameTag = MATCHED_NAME_TAG.get();
        MATCHED_NAME_TAG.remove();
        return Optional.ofNullable(nameTag);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IIngredientSerializer<? extends Ingredient> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", LaoWuMod.id("named_player_name_tag").toString());
        return json;
    }

    private static ItemStack displayStack() {
        ItemStack stack = new ItemStack(Items.NAME_TAG);
        stack.setHoverName(Component.translatable(
                "ingredient.laowu.named_player_name_tag"));
        return stack;
    }

    public static final class Serializer
            implements IIngredientSerializer<NamedPlayerNameTagIngredient> {
        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {}

        @Override
        public NamedPlayerNameTagIngredient parse(JsonObject json) {
            return new NamedPlayerNameTagIngredient();
        }

        @Override
        public NamedPlayerNameTagIngredient parse(FriendlyByteBuf buffer) {
            return new NamedPlayerNameTagIngredient();
        }

        @Override
        public void write(FriendlyByteBuf buffer,
                          NamedPlayerNameTagIngredient ingredient) {}
    }
}
