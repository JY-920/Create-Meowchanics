package cn.laowu.mod.mixin;

import cn.laowu.mod.genetics.CatMorningGiftRewards;
import net.minecraft.world.entity.animal.Cat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds trait rolls after vanilla has completed its normal morning gift. */
@Mixin(targets = "net.minecraft.world.entity.animal.Cat$CatRelaxOnOwnerGoal", remap = false)
abstract class CatMorningGiftMixin {
    // This project intentionally does not ship a Mixin refmap. Name both the
    // Mojmap development field and its Forge/SRG production alias so the same
    // mixin works in Gradle runs and in an exported PCL/server jar.
    @Shadow(aliases = "f_28198_") @Final private Cat cat;

    @Inject(method = {"giveMorningGift", "m_28215_"}, at = @At("TAIL"), remap = false)
    private void laowu$appendTraitMorningGifts(CallbackInfo callback) {
        CatMorningGiftRewards.giveBonusGifts(cat);
    }
}
