package cn.laowu.mod.mixin;
import cn.laowu.mod.recipe.CraftingGridOrigin;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(CraftingInput.class)
public abstract class CraftingGridOriginMixin implements CraftingGridOrigin {
    @Unique private int laowu$w, laowu$h, laowu$l, laowu$t;
    public int laowu$width() { return laowu$w; }
    public int laowu$height() { return laowu$h; }
    public int laowu$left() { return laowu$l; }
    public int laowu$top() { return laowu$t; }
    @Inject(method = "ofPositioned", at = @At("RETURN"))
    private static void laowu$remember(int width, int height, List<ItemStack> items,
            CallbackInfoReturnable<CraftingInput.Positioned> cir) {
        var result = cir.getReturnValue();
        if (result.input().isEmpty()) return;
        var data = (CraftingGridOriginMixin) (Object) result.input();
        data.laowu$w = width; data.laowu$h = height;
        data.laowu$l = result.left(); data.laowu$t = result.top();
    }
}
