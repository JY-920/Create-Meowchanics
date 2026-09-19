package cn.laowu.mod.recipe;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.WishAdoptionBoxBlockItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

/** Standard shaped grid/JEI display; finalize Create's automated output at assembly time. */
public final class WishAdoptionBoxRecipe extends ShapedRecipe {
    private final ShapedRecipe original;
    public WishAdoptionBoxRecipe(ShapedRecipe original) {
        super(original.getGroup(),original.category(),new ShapedRecipePattern(original.getWidth(),original.getHeight(),
                original.getIngredients(),java.util.Optional.empty()),original.getResultItem(RegistryAccess.EMPTY),original.showNotification());
        this.original=original;
    }
    @Override public ItemStack assemble(CraftingInput input,HolderLookup.Provider access) {
        ItemStack result=super.assemble(input,access);
        // Vanilla previews stay blank; crafted callbacks bind the actual taken result (including the vanilla Crafter).
        // Create skips those callbacks, so finalize its executing mechanical input here.
        if(input instanceof com.simibubi.create.content.kinetics.crafter.MechanicalCraftingInput)
            WishAdoptionBoxBlockItem.ensureOffer(result,RandomSource.create());
        return result;
    }
    @Override public RecipeSerializer<?> getSerializer(){return LaoWuMod.WISH_ADOPTION_BOX_RECIPE.get();}
    public static final class Serializer implements RecipeSerializer<WishAdoptionBoxRecipe> {
        private final MapCodec<WishAdoptionBoxRecipe> codec=ShapedRecipe.Serializer.CODEC.xmap(WishAdoptionBoxRecipe::new,r->r.original);
        private final StreamCodec<RegistryFriendlyByteBuf,WishAdoptionBoxRecipe> stream=ShapedRecipe.Serializer.STREAM_CODEC
                .map(WishAdoptionBoxRecipe::new,r->r.original);
        @Override public MapCodec<WishAdoptionBoxRecipe> codec(){return codec;}
        @Override public StreamCodec<RegistryFriendlyByteBuf,WishAdoptionBoxRecipe> streamCodec(){return stream;}
    }
}
