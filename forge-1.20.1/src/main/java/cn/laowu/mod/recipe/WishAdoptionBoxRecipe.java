package cn.laowu.mod.recipe;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.WishAdoptionBoxBlockItem;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

/** Standard shaped grid/JEI display; finalize Create's automated output at assembly time. */
public final class WishAdoptionBoxRecipe extends ShapedRecipe {
    private final ShapedRecipe original;
    public WishAdoptionBoxRecipe(ShapedRecipe original) {
        super(original.getId(),original.getGroup(),original.category(),original.getWidth(),original.getHeight(),
                original.getIngredients(),original.getResultItem(RegistryAccess.EMPTY),original.showNotification());
        this.original=original;
    }
    @Override public ItemStack assemble(CraftingContainer input,RegistryAccess access) {
        ItemStack result=super.assemble(input,access);
        // Vanilla previews stay blank; Item.onCraftedBy binds the actual taken result.
        // Create does not call that callback, but this inventory is an executing machine, not a player preview.
        if(input instanceof com.simibubi.create.content.kinetics.crafter.MechanicalCraftingInventory)
            WishAdoptionBoxBlockItem.ensureOffer(result,RandomSource.create());
        return result;
    }
    @Override public RecipeSerializer<?> getSerializer(){return LaoWuMod.WISH_ADOPTION_BOX_RECIPE.get();}
    public static final class Serializer implements RecipeSerializer<WishAdoptionBoxRecipe> {
        private final ShapedRecipe.Serializer vanilla=new ShapedRecipe.Serializer();
        @Override public WishAdoptionBoxRecipe fromJson(ResourceLocation id,JsonObject json) {
            return new WishAdoptionBoxRecipe(vanilla.fromJson(id,json));
        }
        @Override public WishAdoptionBoxRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer) {
            return new WishAdoptionBoxRecipe(vanilla.fromNetwork(id,buffer));
        }
        @Override public void toNetwork(FriendlyByteBuf buffer,WishAdoptionBoxRecipe recipe) {
            vanilla.toNetwork(buffer,recipe.original);
        }
    }
}
