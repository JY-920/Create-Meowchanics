package cn.laowu.mod.recipe;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatMaterialRegistry;
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

import java.util.stream.Stream;

/**
 * Matches every visible item-backed block without expanding the ingredient to
 * the entire block registry. Recipe viewers receive one renamed stone stack,
 * so the slot remains stable even in very large modpacks.
 */
public final class AnyBlockIngredient extends AbstractIngredient {
    public AnyBlockIngredient() {
        super(Stream.of(new Ingredient.ItemValue(displayStack())));
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        return stack != null && CatMaterialRegistry.blockMaterial(stack).isPresent();
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
        json.addProperty("type", LaoWuMod.id("any_block").toString());
        return json;
    }

    private static ItemStack displayStack() {
        ItemStack stack = new ItemStack(Items.STONE);
        stack.setHoverName(Component.translatable("ingredient.laowu.any_block"));
        return stack;
    }

    public static final class Serializer implements IIngredientSerializer<AnyBlockIngredient> {
        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {}

        @Override
        public AnyBlockIngredient parse(JsonObject json) {
            return new AnyBlockIngredient();
        }

        @Override
        public AnyBlockIngredient parse(FriendlyByteBuf buffer) {
            return new AnyBlockIngredient();
        }

        @Override
        public void write(FriendlyByteBuf buffer, AnyBlockIngredient ingredient) {}
    }
}
