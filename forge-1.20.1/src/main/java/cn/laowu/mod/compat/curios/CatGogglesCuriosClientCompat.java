package cn.laowu.mod.compat.curios;

import cn.laowu.mod.LaoWuMod;
import com.simibubi.create.compat.curios.GogglesCurioRenderer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

/** Client-only renderer bridge using Create's own HEAD item render path. */
@OnlyIn(Dist.CLIENT)
public final class CatGogglesCuriosClientCompat {
    public static void registerRenderer() {
        CuriosRendererRegistry.register(LaoWuMod.CAT_ENGINEER_GOGGLES.get(),
                () -> new CatGogglesCurioRenderer(Minecraft.getInstance()
                        .getEntityModels().bakeLayer(GogglesCurioRenderer.LAYER)));
    }

    private CatGogglesCuriosClientCompat() {}
}
