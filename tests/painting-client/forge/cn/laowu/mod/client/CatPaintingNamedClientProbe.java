package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.client.painting.PaintingClientCache;
import cn.laowu.mod.entity.CatPaintingEntity;
import cn.laowu.mod.painting.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="laowu",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public class CatPaintingNamedClientProbe {
 private static boolean started,ready,failed; private static int ticks,wait,stage;
 private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 private static void name(ItemStack stack,String title){stack.setHoverName(Component.literal(title));}
 private static void resources(Minecraft mc)throws Exception{
  for(var e:PaintingBuiltinCatalog.namedEntries()){
   var texture=PaintingClientCache.resolve(e.hash());
   check(texture.equals(LaoWuMod.id(e.texturePath())),"Named photo resolves directly: "+e.id());
   try(var stream=mc.getResourceManager().open(texture);var pixels=com.mojang.blaze3d.platform.NativeImage.read(stream)){
    check(pixels.getWidth()==1281&&pixels.getHeight()==(e.id().equals("naiyou")?854:1708),"Full resolution original photo: "+e.id());
   }
   check(PaintingBuiltinCatalog.entries().stream().noneMatch(r->r.hash().equals(e.hash())),"Not in random gallery");
  }
 }
 private static void setup(Minecraft mc){
  var server=mc.getSingleplayerServer();UUID id=mc.player.getUUID();
  server.execute(()->{
   try{
    var p=server.getPlayerList().getPlayer(id);var level=p.serverLevel();
    p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();p.setNoGravity(true);
    p.teleportTo(level,0.5,177,9,180,0);level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
    p.getInventory().clearContent();
    for(var e:level.getEntitiesOfClass(CatPaintingEntity.class,new AABB(-16,165,-2,16,190,30)))e.discard();
    for(int x=-9;x<=9;x++)for(int y=170;y<=184;y++)for(int z=0;z<=3;z++)
     level.setBlock(new BlockPos(x,y,z),z==0?Blocks.SMOOTH_QUARTZ.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
    for(int x=-9;x<=9;x++)for(int z=1;z<=12;z++)level.setBlock(new BlockPos(x,171,z),Blocks.SMOOTH_QUARTZ.defaultBlockState(),3);
    if(cn.laowu.mod.compat.create.CreateIntegration.isLoaded())for(int x:new int[]{-3,0,3}){
     level.setBlock(new BlockPos(x,173,3),Blocks.SMOOTH_QUARTZ.defaultBlockState(),3);
     level.setBlock(new BlockPos(x,174,3),net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(LaoWuMod.id("cat_painting_table")).defaultBlockState()
      .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,Direction.from2DDataValue(x+4)),3);
    }
    int i=0;
    for(var e:PaintingBuiltinCatalog.namedEntries()){
     var wall=new BlockPos(-2+i*4,177,0);
     // Illuminate the actual face while retaining solid hanging support.
     var shape=new CatPaintingEntity(LaoWuMod.CAT_PAINTING_ENTITY.get(),level);
     shape.place(wall.south(),Direction.SOUTH,new CatPaintingData(1,e.hash(),e.spec(),new UUID(0,0)));
     for(var support:shape.occupiedAndSupport())if(support.getZ()==0){
      level.setBlock(support,Blocks.SEA_LANTERN.defaultBlockState(),3);
      level.getChunkSource().getLightEngine().checkBlock(support);
     }
     var stack=new ItemStack(LaoWuMod.CAT_PAINTING.get());name(stack,"《"+e.triggerName()+"》");
     p.setItemInHand(InteractionHand.MAIN_HAND,stack);
     var result=stack.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(wall),Direction.SOUTH,wall,false)));
     check(result.consumesAction()&&stack.isEmpty(),"Actual named item placement: "+e.id());
     i++;
    }
    for(i=0;i<2;i++){var stack=new ItemStack(LaoWuMod.CAT_PAINTING.get());name(stack,PaintingBuiltinCatalog.namedEntries().get(i).triggerName());p.getInventory().setItem(i,stack);}
    p.getInventory().setItem(2,new ItemStack(LaoWuMod.CAT_PAINTING.get()));p.inventoryMenu.broadcastChanges();
    check(PaintingService.get(server).usedBytes()==0,"Packaged named photos must not upload");
    mc.execute(()->ready=true);
   }catch(Throwable t){t.printStackTrace();mc.execute(()->{failed=true;System.err.println("FAIL: NAMED PHOTO SETUP");mc.stop();});}
  });
 }
 @net.minecraftforge.eventbus.api.SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event){
  if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
  if(!Boolean.getBoolean("laowu.painting_named_probe")||failed)return;
  var mc=Minecraft.getInstance();if(++ticks>3000){failed=true;System.err.println("FAIL: NAMED PHOTO TIMEOUT stage="+stage);mc.stop();return;}
  if(mc.getOverlay()!=null||mc.player==null||mc.level==null)return;
  try{
   if(!started){
    started=true;mc.options.guiScale().set(3);mc.options.hideGui=true;
    org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),1440,1000);
    mc.mouseHandler.releaseMouse();resources(mc);setup(mc);return;
   }
   if(!ready)return;
   mc.player.getAbilities().flying=true;mc.player.setPos(0.5,177,9);
   mc.player.setYRot(180);mc.player.setXRot(0);mc.player.yRotO=180;mc.player.xRotO=0;
   mc.options.setCameraType(CameraType.FIRST_PERSON);
   if(stage<2){
    if(mc.screen!=null)return;
    var pictures=new HashSet<String>();boolean lit=true;
    for(var e:mc.level.entitiesForRendering())if(e instanceof CatPaintingEntity p){
     p.getPaintingData().ifPresent(d->pictures.add(d.hash()));
     int block=mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,p.blockPosition());
     int sky=mc.level.getBrightness(net.minecraft.world.level.LightLayer.SKY,p.blockPosition());
     if(block<12&&sky<14)lit=false;
     if(ticks%100==0)System.out.println("Named photo light: "+p.blockPosition()+" block="+block+" sky="+sky);
    }
    if(pictures.size()!=2||++wait<100||!lit)return;
    resources(mc);
    if(stage==0){
     Screenshot.grab(mc.gameDirectory,"cat-painting-named.png",mc.getMainRenderTarget(),m->{});
     mc.reloadResourcePacks();stage=1;wait=0;return;
    }
    Screenshot.grab(mc.gameDirectory,"cat-painting-named-reloaded.png",mc.getMainRenderTarget(),m->{});
    mc.options.hideGui=false;mc.setScreen(new InventoryScreen(mc.player));stage=2;wait=0;return;
   }
   if(++wait<40)return;
   check(mc.player.getInventory().getItem(0).hasFoil()&&mc.player.getInventory().getItem(1).hasFoil(),"Both named items glint on client");
   check(!mc.player.getInventory().getItem(2).hasFoil(),"Ordinary cat painting does not glint");
   Screenshot.grab(mc.gameDirectory,"cat-painting-named-inventory.png",mc.getMainRenderTarget(),m->{});
   System.out.println("PASS: 2 NAMED PHOTO REAL PLACEMENTS + SYNC + HD TEXTURES + WORLD RENDER + RESOURCE RELOAD + INVENTORY GLINT, no uploads");
   stage=3;failed=true;mc.stop();
  }catch(Throwable t){failed=true;t.printStackTrace();System.err.println("FAIL: NAMED PHOTO CLIENT stage="+stage);mc.stop();}
 }
}
