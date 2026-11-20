package cn.laowu.mod.client.painting;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="laowu",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class PaintingClientEvents {
 @net.minecraftforge.eventbus.api.SubscribeEvent
 public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent e){
  if(e.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;PaintingClientCache.tick();
 }
}
