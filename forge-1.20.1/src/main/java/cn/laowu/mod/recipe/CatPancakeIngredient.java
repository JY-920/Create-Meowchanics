package cn.laowu.mod.recipe;

import cn.laowu.mod.CatOutfitType;
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
    private final Boolean baby;
    private final CatOutfitType outfit;

    public CatPancakeIngredient() {
        this(null, null);
    }

    private CatPancakeIngredient(@Nullable Boolean baby,
                                 @Nullable CatOutfitType outfit) {
        super(displayValues(baby, outfit));
        this.baby = baby;
        this.outfit = outfit;
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        return stack != null && stack.is(LaoWuMod.CAT_PANCAKE.get())
                && (baby == null || CatPancakeItem.isBaby(stack) == baby)
                && (outfit == null || CatPancakeItem.getOutfit(stack) == outfit);
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
        if (baby != null) json.addProperty("baby", baby);
        if (outfit != null) json.addProperty("outfit", outfit.id());
        return json;
    }

    private static java.util.stream.Stream<? extends Ingredient.Value> displayValues(
            @Nullable Boolean baby, @Nullable CatOutfitType outfit) {
        if (outfit == null) {
            return (baby != null && baby
                    ? java.util.stream.Stream.of(CatPancakeItem.defaultBabyDisplayStack())
                    : CatPancakeItem.jeiDisplayStacks().stream())
                    .map(Ingredient.ItemValue::new);
        }

        ItemStack display = baby != null && baby
                ? CatPancakeItem.defaultBabyDisplayStack()
                : CatPancakeItem.defaultDisplayStack();
        if (outfit != CatOutfitType.NONE) {
            CatPancakeItem.equipOutfit(display, outfit);
        }
        return java.util.stream.Stream.of(new Ingredient.ItemValue(display));
    }

    public static final class Serializer implements IIngredientSerializer<CatPancakeIngredient> {
        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {}

        @Override
        public CatPancakeIngredient parse(JsonObject json) {
            Boolean baby = json.has("baby") ? json.get("baby").getAsBoolean() : null;
            CatOutfitType outfit = json.has("outfit")
                    ? CatOutfitType.byId(json.get("outfit").getAsString()) : null;
            return new CatPancakeIngredient(baby, outfit);
        }

        @Override
        public CatPancakeIngredient parse(FriendlyByteBuf buffer) {
            byte encoded = buffer.readByte();
            Boolean baby = encoded < 0 ? null : encoded != 0;
            CatOutfitType outfit = buffer.readBoolean()
                    ? CatOutfitType.byId(buffer.readUtf(16)) : null;
            return new CatPancakeIngredient(baby, outfit);
        }

        @Override
        public void write(FriendlyByteBuf buffer, CatPancakeIngredient ingredient) {
            buffer.writeByte(ingredient.baby == null ? -1 : ingredient.baby ? 1 : 0);
            buffer.writeBoolean(ingredient.outfit != null);
            if (ingredient.outfit != null) {
                buffer.writeUtf(ingredient.outfit.id(), 16);
            }
        }
    }
}
