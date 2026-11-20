package cn.laowu.mod.client;
import cn.laowu.mod.*;import cn.laowu.mod.create.*;import cn.laowu.mod.painting.*;import cn.laowu.mod.client.painting.*;
import cn.laowu.mod.compat.create.client.CatPaintingTableScreen;
import net.minecraft.client.*;import net.minecraft.core.*;import net.minecraft.network.chat.Component;import net.minecraft.world.item.ItemStack;
import java.nio.file.*;import java.util.*;
@net.neoforged.fml.common.EventBusSubscriber(modid="laowu",value=net.neoforged.api.distmarker.Dist.CLIENT)
public class CatPaintingClientProbe {
 private static int ticks,stage,wait;private static boolean started,failed;private static String hash;
 private static final BlockPos TABLE=new BlockPos(0,70,2),WALL=new BlockPos(0,71,0);
 private static Object field(Object o,String name)throws Exception{
  Class<?> c=o.getClass();while(c!=null){try{var f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException e){c=c.getSuperclass();}}throw new NoSuchFieldException(name);
 }
 private static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
 private static void checkTableAppearance(Minecraft mc,CatPaintingTableScreen screen)throws Exception{
  var nativeTable=com.simibubi.create.AllBlocks.SCHEMATIC_TABLE.get().defaultBlockState();
  var table=CatPaintingTableRegistration.BLOCK.get().defaultBlockState();
  var facing=net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
  var shapes=new ArrayList<String>();
  var rotations=new ArrayList<String>();
  var resources=mc.getResourceManager();
  com.google.gson.JsonObject ownModel,referenceModel;
  try(var own=resources.openAsReader(LaoWuMod.id("blockstates/cat_painting_table.json"));
      var ref=resources.openAsReader(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create","blockstates/schematic_table.json"))){
   ownModel=com.google.gson.JsonParser.parseReader(own).getAsJsonObject().getAsJsonObject("variants");
   referenceModel=com.google.gson.JsonParser.parseReader(ref).getAsJsonObject().getAsJsonObject("variants");
  }
  var different=net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME;
  for(Direction direction:Direction.Plane.HORIZONTAL){
   var actual=table.setValue(facing,direction);
   var expected=nativeTable.setValue(facing,direction);
   if(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(actual.getShape(mc.level,TABLE),expected.getShape(mc.level,TABLE),different) ||
      net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(actual.getCollisionShape(mc.level,TABLE),expected.getCollisionShape(mc.level,TABLE),different))
    shapes.add(direction.getName());
   String key="facing="+direction.getName();
   var a=ownModel.getAsJsonObject(key).get("y");
   var b=referenceModel.getAsJsonObject(key).get("y");
   if((a==null?0:a.getAsInt())!=(b==null?0:b.getAsInt()))rotations.add(direction.getName());
  }
  int left=(int)field(screen,"leftPos"),top=(int)field(screen,"topPos");
  int scale=(int)mc.getWindow().getGuiScale();
  int header=CatPaintingTableMenu.INVENTORY_Y-12;
  boolean clean=true;
  try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
   int background=pixels.getPixelRGBA((left+165)*scale,(top+header)*scale);
   for(int y=header;y<header+9;y++)for(int x=30;x<155;x++)
    if(pixels.getPixelRGBA((left+x)*scale,(top+y)*scale)!=background)clean=false;
  }
  check(shapes.isEmpty()&&rotations.isEmpty()&&clean,
   "Table must match native shape/rotation and keep inventory header blank; wrong shapes="+shapes+", wrong rotations="+rotations+", blank header="+clean);
 }
 private static void standalone(Minecraft mc)throws Exception{
  if(!PaintingClientCache.ready())return;
  if(!started){
   started=true;
   check(!cn.laowu.mod.compat.create.CreateIntegration.isLoaded(),"Standalone renderer must run without Create");
   mc.options.guiScale().set(2);org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),1280,900);
   var server=mc.getSingleplayerServer();UUID id=mc.player.getUUID();
   server.execute(()->{
    var service=PaintingService.get(server);
    PaintingClientCache.execute(()->{
     try{
      var image=new java.awt.image.BufferedImage(32,16,java.awt.image.BufferedImage.TYPE_INT_RGB);
      for(int y=0;y<16;y++)for(int x=0;x<32;x++)image.setRGB(x,y,x<16?0xEFB748:0x2396A8);
      var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",bytes);
      var spec=new PaintingSpec(2,1,PaintingSpec.Mode.PIXEL,PaintingSpec.Crop.KEEP_WIDTH);
      var asset=((PaintingAssetStore)field(service,"store")).put(bytes.toByteArray(),spec,id);
      var data=new CatPaintingData(1,asset.hash(),spec,id);
      server.execute(()->{
       var p=server.getPlayerList().getPlayer(id);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
       p.getAbilities().flying=true;p.onUpdateAbilities();p.setNoGravity(true);
       p.teleportTo(p.serverLevel(),1,70,4.5,180,0);p.serverLevel().setDayTime(6000);
       for(int x=-4;x<=4;x++)for(int z=-2;z<=6;z++)p.serverLevel().setBlock(new BlockPos(x,69,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
       for(int x=-4;x<=4;x++)for(int z=-2;z<=6;z++)for(int y=70;y<=76;y++)p.serverLevel().setBlock(new BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
       for(int x=-4;x<=4;x++)for(int y=70;y<=75;y++)p.serverLevel().setBlock(new BlockPos(x,y,0),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
       for(int x=-3;x<=3;x+=3)p.serverLevel().setBlock(new BlockPos(x,74,2),net.minecraft.world.level.block.Blocks.SEA_LANTERN.defaultBlockState(),3);
       var painting=new cn.laowu.mod.entity.CatPaintingEntity(LaoWuMod.CAT_PAINTING_ENTITY.get(),p.serverLevel());
       painting.place(WALL.relative(Direction.SOUTH),Direction.SOUTH,data);p.serverLevel().addFreshEntity(painting);
       mc.execute(()->hash=data.hash());
      });
     }catch(Throwable e){e.printStackTrace();mc.execute(()->{failed=true;mc.stop();});}
    });
   });return;
  }
  if(hash==null || PaintingClientCache.resolve(hash).equals(PaintingClientCache.PLACEHOLDER))return;
  mc.player.getAbilities().flying=true;mc.player.setPos(1,70,4.5);mc.player.setYRot(180);mc.player.setXRot(0);mc.player.yRotO=180;mc.player.xRotO=0;
  mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
  if(++wait<30)return;
  Screenshot.grab(mc.gameDirectory,"painting-without-create.png",mc.getMainRenderTarget(),m->{});
  System.out.println("PASS: PAINTING WORLD ASSET + WALL RENDER WITHOUT CREATE");mc.stop();
 }
 private static void open(Minecraft mc){
  mc.getSingleplayerServer().execute(()->{
   var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
   p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.teleportTo(p.serverLevel(),.5,70,4.5,180,0);
   p.serverLevel().setDayTime(6000);p.serverLevel().setWeatherParameters(6000,0,false,false);
   for(int x=-7;x<=7;x++)for(int z=-5;z<=8;z++)for(int y=70;y<=80;y++)p.serverLevel().setBlock(new BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
   for(int x=-5;x<=5;x++)for(int z=-3;z<=6;z++)p.serverLevel().setBlock(new BlockPos(x,69,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
   p.serverLevel().setBlock(TABLE,CatPaintingTableRegistration.BLOCK.get().defaultBlockState(),3);
   for(int x=-4;x<=4;x++)for(int y=70;y<=75;y++)p.serverLevel().setBlock(new BlockPos(x,y,0),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
   var provider=new net.minecraft.world.SimpleMenuProvider((id,inv,player)->{
    var menu=new CatPaintingTableMenu(id,inv,TABLE);menu.getSlot(0).set(new ItemStack(LaoWuMod.CAT_PAINTING.get()));return menu;
   },Component.translatable("block.laowu.cat_painting_table"));
   p.openMenu(provider,b->b.writeBlockPos(TABLE));
  });
 }
 @net.neoforged.bus.api.SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  
  if(!Boolean.getBoolean("laowu.painting_probe")||failed)return;
  var mc=Minecraft.getInstance();if(++ticks>4000){System.err.println("FAIL: PAINTING CLIENT TIMEOUT stage="+stage);mc.stop();return;}
  if(mc.getOverlay()!=null||mc.player==null||mc.level==null)return;
  try{
   if(Boolean.getBoolean("laowu.painting_without_create")){standalone(mc);return;}
   if(!started){
    started=true;mc.options.guiScale().set(2);org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),1280,900);
    Path imports=PaintingImportFiles.folder(mc.gameDirectory.toPath());
    var image=new java.awt.image.BufferedImage(768,384,java.awt.image.BufferedImage.TYPE_INT_RGB);
    var noise=new java.util.Random(413L);
    for(int y=0;y<384;y++)for(int x=0;x<768;x++)
     image.setRGB(x,y,(x<384?0xC0A040:0x2080A0)|noise.nextInt(0x1000000)&0x1F1F1F);
    javax.imageio.ImageIO.write(image,"png",imports.resolve("test-gold-teal.png").toFile());
    check(Files.size(imports.resolve("test-gold-teal.png"))>256*1024,"Fixture exceeds LWJGL's temporary stack");
    try(var decoded=PaintingClientCache.decodeTexture(Files.readAllBytes(imports.resolve("test-gold-teal.png")))){
     check(decoded.getWidth()==768&&decoded.getHeight()==384,"Large stored PNG must decode without consuming the LWJGL stack");
    }
    Files.write(imports.resolve("00-broken-source-with-a-long-filename.png"),new byte[]{1,2,3});
    open(mc);return;
   }
   if(stage==0 && mc.screen instanceof CatPaintingTableScreen s){
    if(!((String)field(s,"notice")).startsWith("预览失败"))return;
    check(field(s,"preview")==null,"Broken source cannot leave a texture behind");
    check(s.getMenu().getSlot(0).y>110,"The input slot must be below the parchment in the processing footer");
    if(++wait<5)return;
    Screenshot.grab(mc.gameDirectory,"painting-editor-empty.png",mc.getMainRenderTarget(),m->{});
    int left=(int)field(s,"leftPos"),top=(int)field(s,"topPos");
    var selection=(com.simibubi.create.foundation.gui.widget.ScrollInput)field(s,"fileChoice");
    var transitions=new ArrayList<String>();
    for(int x:new int[]{182,110,50}){
     selection.setState(0);
     boolean handled=s.mouseScrolled(left+x,top+29,0,-1);
     transitions.add(x+":"+handled+"/"+selection.getState());
    }
    check(transitions.equals(List.of("182:true/1","110:true/1","50:true/1")),
      "Wheel must advance the file over the right, middle and left of the selector: "+transitions);
    selection.active=false;
    s.mouseScrolled(left+50,top+29,0,1);
    check(selection.getState()==1,"Inactive file selector must not change");
    selection.active=true;
    s.mouseScrolled(left+198,top+60,0,1);
    check(selection.getState()==1,"Wheel outside the file selector must not change the file");
    stage=10;return;
   }
   if(stage==10 && mc.screen instanceof CatPaintingTableScreen s){
    if(field(s,"preview")==null)return;
    int left=(int)field(s,"leftPos"),top=(int)field(s,"topPos");
    for(int i=0;i<2;i++){
     check(s.mouseScrolled(left+19,top+77,0,1),"Width responds over its label");
     check(s.mouseScrolled(left+77,top+77,0,1),"Height responds over its label");
    }
    stage=1;wait=0;return;
   }
   if(stage==1 && mc.screen instanceof CatPaintingTableScreen s){
    var snapshot=field(s,"previewInput");
    if(snapshot==null)return;
    var spec=(PaintingSpec)field(snapshot,"spec");
    if(spec.widthBlocks()!=3||spec.heightBlocks()!=3)return;
    if(++wait<8)return;
    System.out.println("PASS: PAINTING SCREEN WHEEL AT LEFT, MIDDLE AND RIGHT + INACTIVE AND OUTSIDE SELECTOR + SIZE CONTROLS");
    checkTableAppearance(mc,s);
    check(mc.player.containerMenu.getSlot(0).getItem().is(LaoWuMod.CAT_PAINTING.get()),"One input painting");
    Screenshot.grab(mc.gameDirectory,"painting-editor.png",mc.getMainRenderTarget(),m->{});
    check(PaintingService.get(mc.getSingleplayerServer()).usedBytes()==0,"Preview must not upload");
    int left=(int)field(s,"leftPos"),top=(int)field(s,"topPos");
    check(s.mouseClicked(left+53,top+131,0),"Actual footer confirm button receives click");
    stage=2;wait=0;return;
   }
   if(stage==2 && mc.screen instanceof CatPaintingTableScreen s){
    if(PaintingClientCache.busy())return;
    var item=mc.player.containerMenu.getSlot(CatPaintingTableMenu.OUTPUT).getItem();var data=CatPaintingData.read(item);
    if(data.isEmpty())return;
    check(data.get().spec().widthBlocks()==3&&data.get().spec().heightBlocks()==3,"Printed dimensions survive real menu sync");
    check(mc.player.containerMenu.getSlot(CatPaintingTableMenu.INPUT).getItem().isEmpty(),"Printing consumes exactly the left item");
    hash=data.get().hash();
    if(++wait<5)return;
    Screenshot.grab(mc.gameDirectory,"painting-editor-printed.png",mc.getMainRenderTarget(),m->{});
    int left=(int)field(s,"leftPos"),top=(int)field(s,"topPos");
    check(s.mouseClicked(left+174,top+131,0),"Actual right output slot receives click");
    s.mouseReleased(left+174,top+131,0);
    stage=3;wait=0;return;
   }
   if(stage==3){
    if(++wait<10)return;
    var menu=mc.player.containerMenu;
    check(CatPaintingData.read(menu.getCarried()).map(data->hash.equals(data.hash())).orElse(false)&&!menu.getSlot(CatPaintingTableMenu.OUTPUT).hasItem(),"Right slot hands the printed painting to the cursor");
    mc.gameMode.handleInventoryMouseClick(menu.containerId,menu.slots.size()-9,0,net.minecraft.world.inventory.ClickType.PICKUP,mc.player);
    check(CatPaintingData.read(mc.player.getInventory().getItem(0)).isPresent(),"One finished item in hotbar");
    mc.player.closeContainer();mc.player.getInventory().selected=0;stage=4;wait=0;return;
   }
   if(stage==4){
    if(++wait<10)return;
    var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(WALL),Direction.SOUTH,WALL,false);
    mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);stage=5;wait=0;return;
   }
   if(stage==5){
    boolean found=mc.level.entitiesForRendering().iterator().hasNext();
    cn.laowu.mod.entity.CatPaintingEntity painting=null;
    for(var e:mc.level.entitiesForRendering())if(e instanceof cn.laowu.mod.entity.CatPaintingEntity p){painting=p;break;}
    if(painting==null)return;
    check(painting.widthBlocks()==3&&painting.heightBlocks()==3,"Placed width and height sync");
    if(PaintingClientCache.resolve(hash).equals(PaintingClientCache.PLACEHOLDER))return;
    // Force the actual render path as well as normal world rendering.
    mc.getEntityRenderDispatcher().getRenderer(painting).render(painting,0,0,new com.mojang.blaze3d.vertex.PoseStack(),mc.renderBuffers().bufferSource(),net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
    mc.renderBuffers().bufferSource().endBatch();
    Files.deleteIfExists(PaintingImportFiles.folder(mc.gameDirectory.toPath()).resolve("test-gold-teal.png"));
    mc.reloadResourcePacks();stage=6;wait=0;return;
   }
   if(stage==6){
    if(PaintingClientCache.resolve(hash).equals(PaintingClientCache.PLACEHOLDER))return;
    if(++wait<25)return;
    Screenshot.grab(mc.gameDirectory,"painting-wall.png",mc.getMainRenderTarget(),m->{});
    check(PaintingService.get(mc.getSingleplayerServer()).usedBytes()>0,"World owns image after local source removal");
    mc.getSingleplayerServer().execute(()->{
     var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
     player.teleportTo(player.serverLevel(),2.8,70,4.5,150,0);
    });
    stage=7;wait=0;return;
   }
   if(stage==7){
    if(++wait<20)return;
    Screenshot.grab(mc.gameDirectory,"painting-wall-oblique.png",mc.getMainRenderTarget(),m->{});
    System.out.println("PASS: PAINTING REAL GUI PREVIEW + RIGHT OUTPUT CLICK + 3X3 WALL + REDOWNLOAD WITHOUT SOURCE");
    mc.stop();stage=8;
   }
  }catch(Throwable e){failed=true;e.printStackTrace();System.err.println("FAIL: PAINTING CLIENT stage="+stage);mc.stop();}
 }
}

