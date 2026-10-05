package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.CatDepotRegistration;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Keep Create's transported item display; the authored cat shell is a normal baked model. */
@EventBusSubscriber(modid=LaoWuMod.MOD_ID, value=Dist.CLIENT, bus=EventBusSubscriber.Bus.MOD)
public final class CatDepotClient {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CatDepotRegistration.DEPOT_BE.get(), CatDepotRenderer::new);
    }

    private CatDepotClient() {}
}
