package cn.laowu.mod.mixin;

import com.mojang.blaze3d.shaders.BlendMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Forge 1.20.1 shader blend cache, restored together with each effect's GL state. */
@Mixin(BlendMode.class)
public interface PerformanceBlendModeAccessor {
    @Accessor("lastApplied")
    static BlendMode laowu$getLastApplied() {
        throw new AssertionError("Mixin accessor was not transformed");
    }

    @Accessor("lastApplied")
    static void laowu$setLastApplied(BlendMode blend) {
        throw new AssertionError("Mixin accessor was not transformed");
    }
}
