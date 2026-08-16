package cn.laowu.mod.recipe;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.CatPancakeItem;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import org.jetbrains.annotations.Nullable;

/**
 * Matches every cat pancake regardless of its captured cat data, while exposing
 * one stable orange, NBT-bearing representative to recipe viewers. Runtime
 * matching remains skin-agnostic and preserves the real pancake's cat data.
 */
public final class CatPancakeIngredient extends AbstractIngredient {
    public CatPancakeIngredient() {
        super(CatPancakeItem.jeiDisplayStacks().stream().map(Ingredient.ItemValue::new));
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        return stack != null && stack.is(LaoWuMod.CAT_PANCAKE.get());
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
        json.addProperty("type", LaoWuMod.id("cat_pancake").toString());
        return json;
    }

    public static final class Serializer implements IIngredientSerializer<CatPancakeIngredient> {
        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {}

        @Override
        public CatPancakeIngredient parse(JsonObject json) {
            return new CatPancakeIngredient();
        }

        @Override
        public CatPancakeIngredient parse(FriendlyByteBuf buffer) {
            return new CatPancakeIngredient();
        }

        @Override
        public void write(FriendlyByteBuf buffer, CatPancakeIngredient ingredient) {
            // This ingredient has no configurable payload.
        }
    }
}
