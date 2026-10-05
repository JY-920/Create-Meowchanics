package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.CatDepotRegistration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Keep Create's transported item display; the authored cat shell is a normal baked model. */
public final class CatDepotClient {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CatDepotRegistration.DEPOT_BE.get(), CatDepotRenderer::new);
    }

    private CatDepotClient() {}
}
