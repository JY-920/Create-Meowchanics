package cn.laowu.mod.recipe;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.item.CatPancakeItem;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/** One exact cat skin and outfit state, used to keep paired JEI slots in sync. */
public final class CatPancakeVariantIngredient extends AbstractIngredient {
    private final ResourceLocation variant;
    private final CatOutfitType outfit;

    public CatPancakeVariantIngredient(ResourceLocation variant, boolean terminator) {
        this(variant, terminator ? CatOutfitType.TERMINATOR : CatOutfitType.NONE);
    }

    public CatPancakeVariantIngredient(ResourceLocation variant, CatOutfitType outfit) {
        super(Stream.of(new Ingredient.ItemValue(
                CatPancakeItem.recipeDisplayStack(variant, outfit))));
        this.variant = variant;
        this.outfit = outfit;
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        return stack != null
                && stack.is(LaoWuMod.CAT_PANCAKE.get())
                && CatPancakeItem.isTamed(stack)
                && variant.equals(CatPancakeItem.variantId(stack))
                && CatPancakeItem.getOutfit(stack) == outfit;
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
        json.addProperty("type", LaoWuMod.id("cat_pancake_variant").toString());
        json.addProperty("variant", variant.toString());
        json.addProperty("outfit", outfit.id());
        return json;
    }

    public static final class Serializer
            implements IIngredientSerializer<CatPancakeVariantIngredient> {
        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        @Override
        public CatPancakeVariantIngredient parse(JsonObject json) {
            ResourceLocation variant = ResourceLocation.tryParse(
                    GsonHelper.getAsString(json, "variant"));
            if (variant == null) throw new IllegalArgumentException("Invalid cat variant in ingredient");
            CatOutfitType outfit = json.has("outfit")
                    ? CatOutfitType.byId(GsonHelper.getAsString(json, "outfit"))
                    : GsonHelper.getAsBoolean(json, "terminator", false)
                    ? CatOutfitType.TERMINATOR : CatOutfitType.NONE;
            return new CatPancakeVariantIngredient(variant, outfit);
        }

        @Override
        public CatPancakeVariantIngredient parse(FriendlyByteBuf buffer) {
            return new CatPancakeVariantIngredient(
                    buffer.readResourceLocation(), CatOutfitType.byId(buffer.readUtf(16)));
        }

        @Override
        public void write(FriendlyByteBuf buffer, CatPancakeVariantIngredient ingredient) {
            buffer.writeResourceLocation(ingredient.variant);
            buffer.writeUtf(ingredient.outfit.id(), 16);
        }
    }
}
