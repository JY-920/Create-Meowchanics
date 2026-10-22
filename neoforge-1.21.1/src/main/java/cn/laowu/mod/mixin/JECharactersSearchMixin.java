package cn.laowu.mod.mixin;

import cn.laowu.mod.client.CatCreativeSearchResults;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional, client-only compatibility at the source of repeated tooltip matches. */
@Pseudo
@Mixin(targets = "me.towdium.jecharacters.utils.FakeArray", remap = false)
public class JECharactersSearchMixin {
    // Unknown future adapter methods should disable this fix, not prevent game startup.
    @Inject(method = "search(Ljava/lang/String;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void laowu$uniqueCatItems(String query, CallbackInfoReturnable<List<?>> ci) {
        List<?> original = ci.getReturnValue();
        List<?> normalized = CatCreativeSearchResults.normalize(original);
        if (normalized != original) ci.setReturnValue(normalized);
    }
}
