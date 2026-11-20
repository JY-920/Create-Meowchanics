package cn.laowu.mod.test;
import cn.laowu.mod.*;import cn.laowu.mod.create.*;import cn.laowu.mod.entity.CatPaintingEntity;import cn.laowu.mod.painting.*;
import net.minecraft.core.BlockPos;import net.minecraft.commands.Commands;import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;import net.minecraft.world.item.ItemStack;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="laowu")
public class CatPaintingMultiplayerProbe {
 public static final BlockPos TABLE=new BlockPos(0,70,4);
 @net.minecraftforge.eventbus.api.SubscribeEvent public static void started(net.minecraftforge.event.server.ServerStartedEvent event){
  if(!"server".equals(System.getProperty("laowu.painting_multi")))return;
  var level=event.getServer().overworld();level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
  for(int x=-8;x<=9;x++)for(int z=-2;z<=10;z++){
   level.setBlock(new BlockPos(x,69,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
   for(int y=70;y<=79;y++)if(!(x>=-5&&x<=7&&z==0))level.setBlock(new BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
  }
  for(int x=-5;x<=7;x++)for(int y=70;y<=77;y++)level.setBlock(new BlockPos(x,y,0),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
  level.setBlock(TABLE,CatPaintingTableRegistration.BLOCK.get().defaultBlockState(),3);
  System.out.println("PASS: PAINTING DEDICATED SERVER READY");
 }
 @net.minecraftforge.eventbus.api.SubscribeEvent public static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){
  if(!"server".equals(System.getProperty("laowu.painting_multi"))||!(e.getEntity() instanceof ServerPlayer p))return;
  p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);boolean a=p.getGameProfile().getName().equals("PainterA");
  p.teleportTo(p.serverLevel(),1,70,a?4.5:8,180,0);
 }
 @net.minecraftforge.eventbus.api.SubscribeEvent public static void commands(net.minecraftforge.event.RegisterCommandsEvent e){
  if(!"server".equals(System.getProperty("laowu.painting_multi")))return;
  e.getDispatcher().register(Commands.literal("paintingprobe")
   .then(Commands.literal("table").executes(c->{
    var p=c.getSource().getPlayerOrException();
    if(!p.getGameProfile().getName().equals("PainterA"))return 0;
    var provider=new net.minecraft.world.SimpleMenuProvider((id,inv,player)->{var menu=new CatPaintingTableMenu(id,inv,TABLE);menu.getSlot(0).set(new ItemStack(LaoWuMod.CAT_PAINTING.get()));return menu;},Component.translatable("block.laowu.cat_painting_table"));
    net.minecraftforge.network.NetworkHooks.openScreen(p,provider,TABLE);
    return 1;
   }))
   .then(Commands.literal("done").executes(c->{
    var server=c.getSource().getServer();var paintings=server.overworld().getEntitiesOfClass(CatPaintingEntity.class,new net.minecraft.world.phys.AABB(-8,69,-1,9,80,2));
    if(paintings.size()!=3)throw new AssertionError("Expected exactly three persistent paintings, got "+paintings.size());
    if(paintings.stream().map(p->p.getPaintingData().orElseThrow().hash()).distinct().count()!=3)throw new AssertionError("Distinct native/pixel assets");
    if(PaintingService.get(server).usedBytes()<=0)throw new AssertionError("No persistent world assets");
    server.saveEverything(false,true,true);
    System.out.println("PASS: PAINTING MULTIPLAYER SERVER THREE SAVED ASSETS");
    server.halt(false);return 1;
   })));
 }
}

