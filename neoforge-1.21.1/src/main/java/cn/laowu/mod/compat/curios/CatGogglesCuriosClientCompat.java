package cn.laowu.mod.compat.curios;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.compat.create.CreateIntegration;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

/** Optional Curios renderer, with no mandatory Create model layer. */
@OnlyIn(Dist.CLIENT)
public final class CatGogglesCuriosClientCompat {
    public static void registerRenderer() {
        CuriosRendererRegistry.register(LaoWuMod.CAT_ENGINEER_GOGGLES.get(),
                () -> new CatGogglesCurioRenderer(Minecraft.getInstance()
                        .getEntityModels().bakeLayer(CreateIntegration.isLoaded()
                                ? cn.laowu.mod.compat.create.CreateClientEvents.gogglesCurioLayer()
                                : ModelLayers.PLAYER)));
    }

    private CatGogglesCuriosClientCompat() {}
}
