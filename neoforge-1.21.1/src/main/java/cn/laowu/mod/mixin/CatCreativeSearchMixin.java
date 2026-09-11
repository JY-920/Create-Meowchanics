package cn.laowu.mod.mixin;

import cn.laowu.mod.client.CatCreativeSearchResults;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No dependency on a search mod; only normalize our exact duplicate search hits. */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CatCreativeSearchMixin {
    @Shadow private float scrollOffs;

    @Inject(method = "refreshSearchResults", at = @At("TAIL"))
    private void laowu$uniqueCatItems(CallbackInfo ci) {
        var menu = ((CreativeModeInventoryScreen) (Object) this).getMenu();
        if (CatCreativeSearchResults.deduplicate(menu.items)) menu.scrollTo(scrollOffs);
    }
}
