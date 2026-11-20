package cn.laowu.mod.client;
import cn.laowu.mod.*;import cn.laowu.mod.painting.*;import cn.laowu.mod.client.painting.*;
import cn.laowu.mod.compat.create.client.CatPaintingTableScreen;
import cn.laowu.mod.entity.CatPaintingEntity;
import net.minecraft.client.*;import net.minecraft.core.*;import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import java.util.*;import java.nio.file.*;
@net.neoforged.fml.common.EventBusSubscriber(modid="laowu",value=net.neoforged.api.distmarker.Dist.CLIENT)
public class CatPaintingMultiplayerClientProbe {
 private static int ticks,stage,wait,trial;private static boolean started,failed;
 private static String current;private static final Set<String> printed=new HashSet<>();
 private static Object field(Object o,String name)throws Exception{Class<?> c=o.getClass();while(c!=null){try{var f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException e){c=c.getSuperclass();}}throw new NoSuchFieldException(name);}
 private static void check(boolean value,String text){if(!value)throw new AssertionError(text);}
 private static PaintingSpec spec(){return trial==0?new PaintingSpec(2,1,PaintingSpec.Mode.ORIGINAL,PaintingSpec.Crop.KEEP_WIDTH):trial==1?new PaintingSpec(1,2,PaintingSpec.Mode.PIXEL,PaintingSpec.Crop.KEEP_HEIGHT):new PaintingSpec(3,1,PaintingSpec.Mode.PIXEL,PaintingSpec.Crop.KEEP_WIDTH);}
 private static void select(CatPaintingTableScreen screen,String name,int value)throws Exception{
  var input=(com.simibubi.create.foundation.gui.widget.ScrollInput)field(screen,name);input.setState(value);input.onChanged();
 }
 @net.neoforged.bus.api.SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  
  String role=System.getProperty("laowu.painting_multi","");
  if(role.equals("server")||role.isEmpty()||failed)return;
  var mc=Minecraft.getInstance();
  if(stage==100){if(++wait>30)mc.stop();return;}
  if(stage==90){
   if(mc.getConnection()!=null && mc.getConnection().getOnlinePlayers().stream().anyMatch(p->p.getProfile().getName().equals("PainterB"))){mc.stop();stage=100;wait=0;}
   return;
  }
  if(++ticks>8000){System.err.println("FAIL: PAINTING MULTIPLAYER TIMEOUT "+role+" stage="+stage);mc.stop();return;}
  if(mc.getOverlay()!=null||mc.player==null||mc.level==null){
   if(mc.screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen){
    try{System.err.println("FAIL: PAINTING LOGIN "+role+" reason="+((Component)field(mc.screen,"reason")).getString());}catch(Exception e){e.printStackTrace();}
    Screenshot.grab(mc.gameDirectory,"painting-login-failure.png",mc.getMainRenderTarget(),m->{});failed=true;mc.stop();return;
   }
   if(ticks%400==0)System.out.println("PAINTING CONNECT STATE "+role+" "+(mc.screen==null?"none":mc.screen.getClass().getSimpleName()));
   return;
  }
  try{
   if(!started){
    started=true;mc.options.guiScale().set(2);org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),1280,900);
    if(role.equals("a")){
     Path dir=PaintingImportFiles.folder(mc.gameDirectory.toPath());
     for(int n=0;n<3;n++){
      var image=new java.awt.image.BufferedImage(320,160,java.awt.image.BufferedImage.TYPE_INT_RGB);
      int[] colors=n==0?new int[]{0xEFB748,0x2396A8}:n==1?new int[]{0x934FAD,0x67C06D}:new int[]{0x416BD6,0xE15C59};
      for(int y=0;y<160;y++)for(int x=0;x<320;x++)image.setRGB(x,y,colors[x<160?0:1]);
      javax.imageio.ImageIO.write(image,n==1?"jpg":"png",dir.resolve(n==1?"1-pixel.jpg":n+"-image.png").toFile());image.flush();
     }
     mc.getConnection().sendCommand("paintingprobe table");
    }
    return;
   }
   if(!role.equals("a")){
    var paintings=new ArrayList<CatPaintingEntity>();
    for(var e:mc.level.entitiesForRendering())if(e instanceof CatPaintingEntity painting)paintings.add(painting);
    if(paintings.size()!=3)return;
    Set<String> hashes=new HashSet<>();
    for(var painting:paintings){
     var data=painting.getPaintingData().orElseThrow();hashes.add(data.hash());
     if(PaintingClientCache.resolve(data.hash()).equals(PaintingClientCache.PLACEHOLDER))return;
    }
    check(hashes.size()==3,"Other player sees three distinct images");
    if(role.equals("b") && mc.getConnection().getOnlinePlayers().stream().anyMatch(p->p.getProfile().getName().equals("PainterA")))return;
    if(++wait<35)return;
    Screenshot.grab(mc.gameDirectory,"painting-multiplayer-"+role+".png",mc.getMainRenderTarget(),m->{});
    System.out.println("PASS: PAINTING OTHER CLIENT THREE IMAGES "+role);
    mc.getConnection().sendCommand("paintingprobe done");stage=100;wait=0;return;
   }
   if(stage==0 && mc.screen instanceof CatPaintingTableScreen screen){
    var s=spec();select(screen,"width",s.widthBlocks());select(screen,"height",s.heightBlocks());select(screen,"mode",s.mode().ordinal());select(screen,"crop",s.crop().ordinal());
    stage=1;wait=0;return;
   }
   if(stage==1 && mc.screen instanceof CatPaintingTableScreen screen){
    var snapshot=field(screen,"previewInput");
    if(snapshot==null||!spec().equals(field(snapshot,"spec")))return;
    if(++wait<10)return;
    Screenshot.grab(mc.gameDirectory,"painting-editor-"+trial+".png",mc.getMainRenderTarget(),m->{});
    int x=(int)field(screen,"leftPos"),y=(int)field(screen,"topPos");check(screen.mouseClicked(x+53,y+131,0),"Actual confirm click");stage=2;return;
   }
   if(stage==2){
    if(PaintingClientCache.busy())return;
    if(PaintingClientCache.status().startsWith("印制失败"))throw new AssertionError(PaintingClientCache.status());
    var data=CatPaintingData.read(mc.player.containerMenu.getSlot(1).getItem());if(data.isEmpty())return;
    check(data.get().spec().equals(spec()),"Roundtrip spec");current=data.get().hash();printed.add(current);
    mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,1,0,ClickType.SWAP,mc.player);stage=3;wait=0;return;
   }
   if(stage==3){if(++wait<10)return;mc.player.closeContainer();mc.player.getInventory().selected=0;stage=4;wait=0;return;}
   if(stage==4){
    if(++wait<10)return;var wall=new BlockPos(-3+trial*3,71,0);
    mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(wall),Direction.SOUTH,wall,false));stage=5;return;
   }
   if(stage==5){
    boolean placed=false;
    for(var entity:mc.level.entitiesForRendering())if(entity instanceof CatPaintingEntity painting && painting.getPaintingData().map(d->d.hash().equals(current)).orElse(false))placed=true;
    if(!placed)return;
    Path file=PaintingImportFiles.folder(mc.gameDirectory.toPath()).resolve(trial==1?"1-pixel.jpg":trial+"-image.png");Files.delete(file);
    if(++trial==3){check(printed.size()==3,"Three distinct printed assets");System.out.println("PASS: PAINTING CLIENT A PNG + JPG + ORIGINAL + PIXEL + BOTH CROPS");stage=90;return;}
    mc.getConnection().sendCommand("paintingprobe table");stage=0;
   }
  }catch(Throwable e){failed=true;e.printStackTrace();System.err.println("FAIL: PAINTING MULTIPLAYER CLIENT "+role+" stage="+stage);mc.stop();}
 }
 @net.neoforged.fml.common.EventBusSubscriber(modid="laowu",value=net.neoforged.api.distmarker.Dist.CLIENT,bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
 public static class Identity {
  @net.neoforged.bus.api.SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){
   String role=System.getProperty("laowu.painting_multi","");
   if(role.isEmpty()||role.equals("server"))return;
   e.enqueueWork(()->{
    try{
     String name=role.equals("a")?"PainterA":role.equals("b")?"PainterB":"PainterB2";
     UUID id=UUID.nameUUIDFromBytes(("OfflinePlayer:"+name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
     var user=new User(name,id,"0",Optional.empty(),Optional.empty(),User.Type.LEGACY);
     if(!Minecraft.getInstance().getUser().getName().equals(name))throw new AssertionError("Launch username was not applied");
     org.lwjgl.glfw.GLFW.glfwSetWindowTitle(Minecraft.getInstance().getWindow().getWindow(),"Cat Painting Probe - "+name);
    }catch(Exception error){throw new IllegalStateException(error);}
   });
  }
 }
}

