package cn.laowu.mod.client.painting;
@net.neoforged.fml.common.EventBusSubscriber(modid="laowu",value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class PaintingClientEvents {
 @net.neoforged.bus.api.SubscribeEvent
 public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){
  PaintingClientCache.tick();
 }
}
