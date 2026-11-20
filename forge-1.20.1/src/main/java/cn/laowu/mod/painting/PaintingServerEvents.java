package cn.laowu.mod.painting;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="laowu")
public final class PaintingServerEvents {
 @SubscribeEvent public static void started(ServerStartedEvent e){PaintingService.get(e.getServer());}
 @SubscribeEvent public static void stopped(ServerStoppingEvent e){PaintingService.stop(e.getServer());}
 @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)PaintingService.get(p.server).hello(p);}
 @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)PaintingService.get(p.server).disconnected(p);}
 @SubscribeEvent public static void track(PlayerEvent.StartTracking e){if(e.getEntity() instanceof ServerPlayer p)PaintingService.get(p.server).track(p,e.getTarget(),true);}
 @SubscribeEvent public static void untrack(PlayerEvent.StopTracking e){if(e.getEntity() instanceof ServerPlayer p)PaintingService.get(p.server).track(p,e.getTarget(),false);}
 @SubscribeEvent public static void commands(net.minecraftforge.event.RegisterCommandsEvent e){
  e.getDispatcher().register(Commands.literal("laowu").then(Commands.literal("paintings").requires(s->s.hasPermission(2))
   .then(Commands.literal("status").executes(c->{long bytes=PaintingService.get(c.getSource().getServer()).usedBytes();
    c.getSource().sendSuccess(()->Component.literal("Cat paintings: "+bytes+" bytes"),false);return 1;}))));
 }
 @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent e){if(e.phase==net.minecraftforge.event.TickEvent.Phase.END)PaintingService.get(e.getServer()).tick();}
}
