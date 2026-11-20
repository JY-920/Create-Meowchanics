package cn.laowu.mod.client;
import cn.laowu.mod.*;
import cn.laowu.mod.painting.*;
import cn.laowu.mod.client.painting.*;
import cn.laowu.mod.entity.CatPaintingEntity;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="laowu",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public class CatPaintingGalleryClientProbe {
 private static boolean started,ready,failed;private static int ticks,wait,stage;
 private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 private static void resources(Minecraft mc)throws Exception{
  for(var e:PaintingBuiltinCatalog.entries()){
   var texture=PaintingClientCache.resolve(e.hash());
   check(texture.equals(LaoWuMod.id(e.texturePath())),"Builtin resolves directly, never placeholder: "+e.id());
   try(var stream=mc.getResourceManager().open(texture);var pixels=com.mojang.blaze3d.platform.NativeImage.read(stream)){
    check(pixels.getWidth()==e.width()*16&&pixels.getHeight()==e.height()*16,"Native pixel dimensions: "+e.id());
   }
  }
 }
 private static void setup(Minecraft mc){
  var server=mc.getSingleplayerServer();UUID id=mc.player.getUUID();
  server.execute(()->{
   try{
    var p=server.getPlayerList().getPlayer(id);var level=p.serverLevel();
    p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.setNoGravity(true);
    p.teleportTo(level,0,177,16,180,0);level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
    for(var e:level.getEntitiesOfClass(CatPaintingEntity.class,new net.minecraft.world.phys.AABB(-16,165,-2,16,190,30)))e.discard();
    for(int x=-13;x<=13;x++)for(int y=169;y<=188;y++)for(int z=0;z<=3;z++)
     level.setBlock(new BlockPos(x,y,z),z==0?Blocks.SMOOTH_QUARTZ.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
    for(int x=-13;x<=13;x++)for(int z=1;z<=20;z++)level.setBlock(new BlockPos(x,169,z),Blocks.SMOOTH_QUARTZ.defaultBlockState(),3);
    int i=0;
    for(var e:PaintingBuiltinCatalog.entries()){
     var entity=new CatPaintingEntity(LaoWuMod.CAT_PAINTING_ENTITY.get(),level);
     entity.place(new BlockPos(-8+i%4*5,183-i/4*5,1),Direction.SOUTH,new CatPaintingData(1,e.hash(),e.spec(),new UUID(0,0)));
     // Real light sources keep visual QA independent of asynchronous high-altitude skylight.
     level.setBlock(entity.blockPosition().north(),Blocks.SEA_LANTERN.defaultBlockState(),3);
     check(entity.survives(),"Gallery wall support: "+e.id());check(level.addFreshEntity(entity),"Gallery spawn");i++;
    }
    check(PaintingService.get(server).usedBytes()==0,"Builtin gallery must not upload or store image bytes");
    mc.execute(()->ready=true);
   }catch(Throwable t){t.printStackTrace();mc.execute(()->{failed=true;System.err.println("FAIL: GALLERY SETUP");mc.stop();});}
  });
 }
 @net.minecraftforge.eventbus.api.SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event){
  if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
  if(!Boolean.getBoolean("laowu.painting_gallery_probe")||failed)return;
  var mc=Minecraft.getInstance();if(++ticks>4000){failed=true;System.err.println("FAIL: GALLERY TIMEOUT stage="+stage);mc.stop();return;}
  if(mc.getOverlay()!=null||mc.player==null||mc.level==null)return;
  try{
   if(!started){
    started=true;mc.options.guiScale().set(2);mc.options.hideGui=true;
    org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),1440,1000);
    resources(mc);setup(mc);return;
   }
   if(!ready||mc.screen!=null)return;
   mc.player.getAbilities().flying=true;mc.player.setPos(0,177,16);
   mc.player.setYRot(180);mc.player.setXRot(0);mc.player.yRotO=180;mc.player.xRotO=0;
   mc.options.setCameraType(CameraType.FIRST_PERSON);
   var pictures=new HashSet<String>();
   boolean lit=true;
   for(var e:mc.level.entitiesForRendering())if(e instanceof CatPaintingEntity p){
    p.getPaintingData().ifPresent(d->pictures.add(d.hash()));
    if(mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,p.blockPosition())<12
      &&mc.level.getBrightness(net.minecraft.world.level.LightLayer.SKY,p.blockPosition())<14)lit=false;
   }
   if(pictures.size()!=12)return;
   if(!lit||++wait<120)return;
   if(stage==0){
    Screenshot.grab(mc.gameDirectory,"cat-painting-gallery.png",mc.getMainRenderTarget(),m->{});
    resources(mc);mc.reloadResourcePacks();stage=1;wait=0;return;
   }
   resources(mc);
   Screenshot.grab(mc.gameDirectory,"cat-painting-gallery-reloaded.png",mc.getMainRenderTarget(),m->{});
   System.out.println("PASS: 12 BUILTIN PAINTINGS SYNC + DIRECT TEXTURES + WORLD RENDER + RESOURCE RELOAD, no uploads");
   mc.stop();stage=2;failed=true;
  }catch(Throwable t){failed=true;t.printStackTrace();System.err.println("FAIL: GALLERY CLIENT stage="+stage);mc.stop();}
 }
}

