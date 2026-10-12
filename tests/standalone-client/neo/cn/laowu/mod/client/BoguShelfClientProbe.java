package cn.laowu.mod.client;
import cn.laowu.mod.*;
import cn.laowu.mod.block.BoguShelfBlock;
import cn.laowu.mod.entity.BoguShelfSeat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid="laowu",value=Dist.CLIENT)
public final class BoguShelfClientProbe {

 private static void verifySittingFoot(Minecraft mc) throws Exception {
  for(boolean baby:new boolean[]{false,true})for(float initialY:new float[]{14.1F,18F})verifyPaws(mc,baby,initialY);
 }
 private static void verifyPaws(Minecraft mc,boolean baby,float initialY) throws Exception {
  var cat=EntityType.CAT.create(mc.level);cat.setInSittingPose(true);
  if(baby)cat.setAge(-1000);
  var seat=LaoWuMod.BOGU_SHELF_SEAT.get().create(mc.level);cat.startRiding(seat,true);
  final double[] lowest={Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY};
  final double[] depth={Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY};
  var consumer=(com.mojang.blaze3d.vertex.VertexConsumer)java.lang.reflect.Proxy.newProxyInstance(BoguShelfClientProbe.class.getClassLoader(),new Class[]{com.mojang.blaze3d.vertex.VertexConsumer.class},(proxy,method,args)->{
   if((method.getName().equals("vertex")||method.getName().equals("addVertex"))&&args!=null&&args.length>=3) {
    if(args[0] instanceof org.joml.Matrix4f matrix&&args.length>=4) {
     var v=matrix.transformPosition(new org.joml.Vector3f(((Number)args[1]).floatValue(),((Number)args[2]).floatValue(),((Number)args[3]).floatValue()));lowest[0]=Math.min(lowest[0],v.y);lowest[1]=Math.max(lowest[1],v.y);depth[0]=Math.min(depth[0],v.z);depth[1]=Math.max(depth[1],v.z);
    } else if(args[0] instanceof com.mojang.blaze3d.vertex.PoseStack.Pose pose&&args.length>=4) {
     var v=pose.pose().transformPosition(new org.joml.Vector3f(((Number)args[1]).floatValue(),((Number)args[2]).floatValue(),((Number)args[3]).floatValue()));lowest[0]=Math.min(lowest[0],v.y);lowest[1]=Math.max(lowest[1],v.y);depth[0]=Math.min(depth[0],v.z);depth[1]=Math.max(depth[1],v.z);
    } else if(args[0] instanceof Number&&args[1] instanceof Number){lowest[0]=Math.min(lowest[0],((Number)args[1]).doubleValue());lowest[1]=Math.max(lowest[1],((Number)args[1]).doubleValue());depth[0]=Math.min(depth[0],((Number)args[2]).doubleValue());depth[1]=Math.max(depth[1],((Number)args[2]).doubleValue());}
   }
   return method.getReturnType()==com.mojang.blaze3d.vertex.VertexConsumer.class?proxy:null;
  });
  var renderer=(HissingCatRenderer)mc.getEntityRenderDispatcher().getRenderer(cat);
  var saved=new java.util.IdentityHashMap<net.minecraft.client.model.geom.ModelPart,Boolean>();
  // Suppress non-paw cubes only in this test. Renderer transforms remain real.
  // Front legs are the two vanilla roots whose initial Y is 14.1; no mapped field names.
  for(Class<?> cls=renderer.getModel().getClass();cls!=null;cls=cls.getSuperclass())for(var field:cls.getDeclaredFields())
   if(field.getType()==net.minecraft.client.model.geom.ModelPart.class) {
    field.setAccessible(true);var part=(net.minecraft.client.model.geom.ModelPart)field.get(renderer.getModel());
    if(part==null)continue;
    for(var child:part.getAllParts().toList()) {
     saved.putIfAbsent(child,child.skipDraw);child.skipDraw=Math.abs(child.getInitialPose().y-initialY)>.001F;
    }
   }
  try { renderer.render(cat,0,0,new com.mojang.blaze3d.vertex.PoseStack(),type->consumer,net.minecraft.client.renderer.LightTexture.FULL_BRIGHT); }
  finally {saved.forEach((part,value)->part.skipDraw=value);}
  System.out.println("BOGU PAW Y: baby="+baby+", leg="+initialY+", min="+lowest[0]);
  if(!Double.isFinite(lowest[0])||Math.abs(lowest[0])>.006)throw new AssertionError("Front paws must touch shelf, not tail/head minimum: "+lowest[0]);
  double size=baby?.5:1;
  System.out.println("BOGU PAW Z: baby="+baby+", leg="+initialY+", min="+depth[0]+", max="+depth[1]);
  if(initialY==14.1F&&depth[1]-depth[0]>.081*size)throw new AssertionError("Front legs lean forward instead of resting vertically: "+java.util.Arrays.toString(depth));
  if(initialY==18F&&depth[1]>.081*size)throw new AssertionError("Folded rear paws stick out from under haunches: "+java.util.Arrays.toString(depth));
  if(depth[0]<-3D/16-.001||depth[1]>3D/16+.001)throw new AssertionError("Paws overhang the actual six-pixel-deep shelf surface: "+java.util.Arrays.toString(depth));
 }

 private static int ticks,ready;private static boolean failed,hissScreenshot;
 @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {

  var mc=Minecraft.getInstance();if(failed)return;
  if(++ticks>2400){System.err.println("FAIL: BOGU CLIENT TIMEOUT");mc.stop();return;}
  if(ticks==200&&mc.level==null&&mc.getSingleplayerServer()==null)mc.createWorldOpenFlows().openWorld("Standalone",mc::stop);
  if(mc.getOverlay()!=null||mc.level==null||mc.player==null||!mc.gameRenderer.getMainCamera().isInitialized())return;
  try {
   if(ready++==0) {
    mc.options.pauseOnLostFocus=false;mc.setScreen(null);
    org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
    org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),1280,900);mc.options.hideGui=true;mc.options.fov().set(70);
    if(!ClientModEvents.OPEN_HELD_ITEM_TRANSFORM.getDefaultKey().equals(com.mojang.blaze3d.platform.InputConstants.UNKNOWN))throw new AssertionError("Display adjuster not unbound by default");
    for(var key:mc.options.keyMappings)if(key.getName().equals("key.laowu.pilot_descend"))throw new AssertionError("Configurable pilot descent still exists");
    if(cn.laowu.mod.compat.create.CreateIntegration.isLoaded()!=Boolean.getBoolean("laowu.create_expected"))throw new AssertionError("Wrong dependency environment");
    verifySittingFoot(mc);
    var block=LaoWuMod.BOGU_SHELF.get();
    for(var facing:Direction.Plane.HORIZONTAL)for(int i=0;i<9;i++) {
     var s=block.defaultBlockState().setValue(BoguShelfBlock.FACING,facing).setValue(BoguShelfBlock.PART,i);
     var model=mc.getBlockRenderer().getBlockModel(s);
     if(model==mc.getModelManager().getMissingModel()||model.getQuads(s,null,net.minecraft.util.RandomSource.create(0)).isEmpty())throw new AssertionError("Missing shelf geometry "+facing+"/"+i);
    }
    var stack=new net.minecraft.world.item.ItemStack(LaoWuMod.BOGU_SHELF_ITEM.get());var model=mc.getItemRenderer().getModel(stack,mc.level,null,0);
    if(model==mc.getModelManager().getMissingModel())throw new AssertionError("Missing shelf item");
    for(var display:new net.minecraft.world.item.ItemDisplayContext[]{net.minecraft.world.item.ItemDisplayContext.GUI,net.minecraft.world.item.ItemDisplayContext.GROUND,net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND}) {
     mc.getItemRenderer().renderStatic(stack,display,net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,new com.mojang.blaze3d.vertex.PoseStack(),mc.renderBuffers().bufferSource(),mc.level,0);
     mc.renderBuffers().bufferSource().endBatch();
    }
    var uuid=mc.player.getUUID();var server=mc.getSingleplayerServer();
    server.execute(()-> {
     var level=server.overworld();var p=server.getPlayerList().getPlayer(uuid);var root=new BlockPos(0,100,0);
     for(int x=-4;x<=4;x++)for(int z=-7;z<=4;z++) {level.setBlock(new BlockPos(x,99,z),net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState(),3);for(int y=100;y<107;y++)level.removeBlock(new BlockPos(x,y,z),false);}
     var state=block.defaultBlockState();level.setBlock(root,state,3);block.setPlacedBy(level,root,state,p,stack);
     var cat=EntityType.CAT.create(level);cat.setOrderedToSit(false);cat.setPos(.5,100,-2);level.addFreshEntity(cat);
     var seat=LaoWuMod.BOGU_SHELF_SEAT.get().create(level);seat.configure(root,Direction.NORTH,cat);level.addFreshEntity(seat);cat.startRiding(seat,true);seat.hold(cat);
     p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.teleportTo(.5,100.6,-5.5);p.setYRot(0);p.setXRot(10);p.onUpdateAbilities();
     level.setDayTime(6000);p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,30000,0,false,false));
    });
   }
   if(ready==50) {
    mc.setScreen(null);mc.player.setYRot(0);mc.player.setXRot(10);
   }
   if(ready==85) {
    net.minecraft.client.Screenshot.grab(mc.gameDirectory,"bogu-sitting.png",mc.getMainRenderTarget(),c->{});
   }
   if(ready==70)mc.getSingleplayerServer().execute(()->{
    var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
    player.teleportTo(-.25,100,-1.25);player.setYRot(0);player.setXRot(0);
   });
   if(ready==80){mc.player.setYRot(0);mc.player.setXRot(0);}
   if(ready==90)mc.getSingleplayerServer().execute(()->{
    var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
    player.teleportTo(-2.5,100.1,-1.1);player.setYRot(-70);player.setXRot(4);
   });
   if(ready==96){mc.player.setYRot(-70);mc.player.setXRot(4);}
   if(ready==104)net.minecraft.client.Screenshot.grab(mc.gameDirectory,"bogu-sitting-side.png",mc.getMainRenderTarget(),c->{});
   if(ready==112)mc.getSingleplayerServer().execute(()->{var level=mc.getSingleplayerServer().overworld();for(var seat:level.getEntitiesOfClass(BoguShelfSeat.class,new net.minecraft.world.phys.AABB(-2,100,-1,2,104,1)))seat.hissBriefly();});
   if(ready>112&&!hissScreenshot&&mc.level.getEntitiesOfClass(net.minecraft.world.entity.animal.Cat.class,new net.minecraft.world.phys.AABB(-2,100,-1,2,104,1)).stream().anyMatch(c->c.getVehicle() instanceof BoguShelfSeat&&CatPoseData.isHissing(c))) {
    net.minecraft.client.Screenshot.grab(mc.gameDirectory,"bogu-hissing.png",mc.getMainRenderTarget(),c->{});hissScreenshot=true;
   }
   if(ready>=140) {
    if(!hissScreenshot)throw new AssertionError("Client never received brief hissing pose");
    var displayCats=mc.level.getEntitiesOfClass(net.minecraft.world.entity.animal.Cat.class,new net.minecraft.world.phys.AABB(-2,100,-1,2,104,1),c->c.getVehicle() instanceof BoguShelfSeat);
    if(displayCats.isEmpty()||displayCats.stream().anyMatch(c->CatPoseData.isHissing(c)||!c.isInSittingPose()))throw new AssertionError("Client display cat did not return to sitting after brief hiss");
    System.out.println("PASS: BOGU CLIENT 36 BAKED STATES, ITEM GUI/HAND/GROUND, KEYS, REAL SEATED/HISSING CAT; Create="+cn.laowu.mod.compat.create.CreateIntegration.isLoaded());
    mc.stop();
   }
  }catch(Throwable ex){failed=true;ex.printStackTrace();System.err.println("FAIL: BOGU CLIENT "+ex);mc.stop();}
 }
}
