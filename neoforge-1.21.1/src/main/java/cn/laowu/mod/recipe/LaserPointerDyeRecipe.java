package cn.laowu.mod.recipe;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.CatLaserPointerItem;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public final class LaserPointerDyeRecipe extends CustomRecipe {
    public LaserPointerDyeRecipe(CraftingBookCategory category) {
        super(category);
    }
    private ItemStack result(CraftingInput input) {
        int w=input.width(), h=input.height();
        if(input instanceof CraftingGridOrigin origin) { w=origin.laowu$width(); h=origin.laowu$height(); }
        if(w!=3 || h!=3) return ItemStack.EMPTY;
        ItemStack pointer=ItemStack.EMPTY;
        DyeItem dye=null;
        for(int i=0;i<input.size();i++) {
            ItemStack s=input.getItem(i);
            if(s.isEmpty()) continue;
            if(s.getItem() instanceof CatLaserPointerItem && pointer.isEmpty()) pointer=s;
            else if(s.getItem() instanceof DyeItem item && dye==null) dye=item;
            else return ItemStack.EMPTY;
        }
        if(pointer.isEmpty() || dye==null) return ItemStack.EMPTY;
        ItemStack out=pointer.copyWithCount(1);
        CatLaserPointerItem.dye(out,dye.getDyeColor());
        return out;
    }
    @Override public boolean matches(CraftingInput input,Level level){return !result(input).isEmpty();}
    @Override public ItemStack assemble(CraftingInput input,net.minecraft.core.HolderLookup.Provider access){return result(input);}
    @Override public boolean canCraftInDimensions(int w,int h){return w>=3 && h>=3;}
    @Override public RecipeSerializer<?> getSerializer(){return LaoWuMod.LASER_POINTER_DYE_SERIALIZER.get();}
}
