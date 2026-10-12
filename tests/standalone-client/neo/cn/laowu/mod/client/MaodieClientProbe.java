package cn.laowu.mod.client;
import cn.laowu.mod.*;
import cn.laowu.mod.block.BoguShelfBlock;
import cn.laowu.mod.entity.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid="laowu",value=Dist.CLIENT)
public final class MaodieClientProbe {
 private static int ticks,seenCycle=-1;private static boolean done;
 private static volatile int cycle;
 private static volatile boolean started,finished;
 private static volatile String serverFailure;
 private static volatile UUID sourceId;
 private static volatile float viewYaw,viewPitch=5;
 private static final BlockPos ROOT=new BlockPos(0,100,0);
 private static Cat source;private static BoguShelfSeat seat;
 private static long lastStroke;private static int strokes,exitTicks;
 private static volatile int combatAge=-1;private static int afterRestore;
 private static void field(GiantCatBoss b,String name,Object value){
  try{var f=GiantCatBoss.class.getDeclaredField(name);f.setAccessible(true);f.set(b,value);}catch(Exception e){throw new AssertionError(e);}
 }
 private static void phase(GiantCatBoss b,byte value){
  try{var m=GiantCatBoss.class.getDeclaredMethod("setPhase",byte.class);m.setAccessible(true);m.invoke(b,value);}catch(Exception e){throw new AssertionError(e);}
 }
 private static void combat(GiantCatBoss b,ServerPlayer p){
  combatAge++;
  field(b,"attackCooldown",10000);
  if(combatAge==0||combatAge==115||combatAge==220){
   phase(b,(byte)1);b.setPos(5.5,100,.5);b.setDeltaMovement(Vec3.ZERO);b.setYRot(0);b.yBodyRot=0;
   p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=true;p.getAbilities().flying=true;p.setNoGravity(true);p.onUpdateAbilities();
   p.teleportTo(9.5,100.5,10.5);b.setTarget(p);
   phase(b,(byte)(combatAge==0?3:combatAge==115?6:9));
  }
  if(combatAge==290)b.setHealth(150);
  if(p.distanceToSqr(b)<36)p.teleportTo(b.getX()+8,b.getY()+.5,b.getZ()+8);
  Vec3 aim=b.position().add(0,1.5,0).subtract(p.getEyePosition());
  viewYaw=(float)Math.toDegrees(Math.atan2(aim.z,aim.x))-90;
  viewPitch=(float)-Math.toDegrees(Math.atan2(aim.y,aim.horizontalDistance()));
  p.setYRot(viewYaw);p.setXRot(viewPitch);
 }
 private static final Set<String> shots=new HashSet<>();

 private static volatile boolean lateAway,lateReturned,cancelReady,cancelCaptured,unloadAway,unloadReturned,unloadReady,unloadCaptured;
 private static int boundaryWait;
 private static boolean noDust(Minecraft mc)throws Exception{
  var field=net.minecraft.client.particle.ParticleEngine.class.getDeclaredField("particles");field.setAccessible(true);
  for(var q:((Map<?,?>)field.get(mc.particleEngine)).values())for(var part:(Collection<?>)q)
   if(part instanceof net.minecraft.client.particle.DustParticle d&&d.isAlive())return false;
  return true;
 }
 private static boolean boundary(net.minecraft.server.level.ServerLevel level,ServerPlayer p)throws Exception{
  long age=level.getGameTime()-lastStroke;
  if(cycle==4){
   if(age>=5&&!lateAway){lateAway=true;p.teleportTo(512,110,512);}
   if(age>=30&&!lateReturned){lateReturned=true;p.teleportTo(.5,100.6,-7.5);viewYaw=0;viewPitch=5;}
  }else if(cycle==5){
   if(age>=20&&!cancelReady){level.removeBlock(ROOT,false);cancelReady=true;}
   if(age>=40){if(!source.isAlive()||!level.getEntitiesOfClass(GiantCatBoss.class,new AABB(ROOT).inflate(64)).isEmpty())throw new AssertionError("Cancellation lost source or spawned boss");
    if(cancelCaptured){cycle=6;started=false;}return true;}
  }else if(cycle==6){
   if(age>=5&&!unloadAway){
    unloadAway=true;p.teleportTo(1024,110,1024);
    // Test-only extended countdown makes actual unload win the race against vanilla ticket grace.
    // Production's sixty-tick timer is proven in the ordinary four-facing cycles.
    var start=cn.laowu.mod.encounter.MaodieSummonState.class.getDeclaredField("startTick");start.setAccessible(true);start.setLong(seat.maodieState(),level.getGameTime()+1200);seat.syncMaodieAura();
   }
   if(unloadAway&&!unloadReturned){
    if(!level.hasChunkAt(ROOT)){
     System.out.println("MAODIE REAL SOURCE CHUNK UNLOADED");unloadReturned=true;p.teleportTo(.5,100.6,-7.5);viewYaw=0;viewPitch=5;
    }
    return true;
   }
   if(unloadReturned){
    if(++boundaryWait>20&&level.getEntity(sourceId) instanceof Cat c){
     if(!(c.getVehicle() instanceof BoguShelfSeat newSeat)||newSeat.isSummoningMaodie()||!level.getEntitiesOfClass(GiantCatBoss.class,new AABB(ROOT).inflate(64)).isEmpty())throw new AssertionError("Real chunk reload retained aura or created boss");
     if(!c.getPersistentData().getString("VisualOriginal").equals("preserved"))throw new AssertionError("Unloaded original data lost");
     unloadReady=true;if(unloadCaptured){System.out.println("MAODIE REAL SOURCE RELOAD PASS");finished=true;}
    }
    return true;
   }
  }
  return false;
 }

 private static void photo(Minecraft mc,String name){
  if(shots.add(cycle+"-"+name))net.minecraft.client.Screenshot.grab(mc.gameDirectory,"maodie-"+cycle+"-"+name+".png",mc.getMainRenderTarget(),c->{});
 }
 private static void serverTick(Minecraft mc,UUID playerId) {
  try {
   var server=mc.getSingleplayerServer();var level=server.overworld();ServerPlayer p=server.getPlayerList().getPlayer(playerId);
   if(finished)return;
   if(afterRestore>0){
    if(++afterRestore>=25){afterRestore=0;cycle=4;started=false;}
    return;
   }
   Direction facing=new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}[cycle%4];
   if(!started) {
    for(var e:level.getEntitiesOfClass(Entity.class,new AABB(ROOT).inflate(64),e->e instanceof Cat||e instanceof GiantCatBoss||e instanceof BoguShelfSeat))e.discard();
    for(int x=-40;x<=40;x++)for(int z=-40;z<=40;z++) {
     level.setBlock(ROOT.offset(x,-1,z),Blocks.OAK_PLANKS.defaultBlockState(),3);
     for(int y=0;y<9;y++)level.removeBlock(ROOT.offset(x,y,z),false);
    }
    var block=(BoguShelfBlock)LaoWuMod.BOGU_SHELF.get();var state=block.defaultBlockState().setValue(BoguShelfBlock.FACING,facing);
    level.setBlock(ROOT,state,3);block.setPlacedBy(level,ROOT,state,p,ItemStack.EMPTY);
    source=EntityType.CAT.create(level);source.setNoAi(true);source.setPos(.5,100,-1);
    source.setCustomName(net.minecraft.network.chat.Component.literal("Visual original "+facing));source.getPersistentData().putString("VisualOriginal","preserved");
    level.addFreshEntity(source);sourceId=source.getUUID();
    seat=LaoWuMod.BOGU_SHELF_SEAT.get().create(level);seat.configure(ROOT,facing,source);level.addFreshEntity(seat);source.startRiding(seat,true);seat.hold(source);
    p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;
    p.teleportTo(.5+facing.getStepX()*8,100.6,.5+facing.getStepZ()*8);viewYaw=facing.getOpposite().toYRot();viewPitch=5;p.setYRot(viewYaw);p.setXRot(viewPitch);p.onUpdateAbilities();
    p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setShiftKeyDown(true);
    level.setDayTime(6000);level.setWeatherParameters(60000,0,false,false);
    strokes=0;lastStroke=level.getGameTime()-10;exitTicks=0;boundaryWait=0;started=true;
   }
   if(strokes<3&&level.getGameTime()-lastStroke>=10) {
    CommonEvents.onCatInteract(new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract(p,InteractionHand.MAIN_HAND,source));
    strokes++;lastStroke=level.getGameTime();
   }
   if(strokes==3&&boundary(level,p))return;
   var bosses=level.getEntitiesOfClass(GiantCatBoss.class,new AABB(ROOT).inflate(64),GiantCatBoss::isMaodie);
   if(!bosses.isEmpty()){
    var boss=bosses.get(0);
    if(exitTicks==0){p.teleportTo(boss.getX(),boss.getY()+.3,boss.getZ()+11);viewYaw=180;viewPitch=3;p.setYRot(viewYaw);p.setXRot(viewPitch);}
    exitTicks++;
    if(!boss.isSummoning()&&exitTicks>70) {
     if(cycle==3){combat(boss,p);if(combatAge<320)return;}
     boss.kill();var restored=level.getEntity(sourceId);
     if(!(restored instanceof Cat c)||!c.getPersistentData().getString("VisualOriginal").equals("preserved"))throw new AssertionError("Real original did not restore");
     System.out.println("MAODIE SERVER INTRO PASS "+facing);
     if(cycle==3){afterRestore=1;return;}
     cycle++;started=false;
    }
   }
  }catch(Throwable e){e.printStackTrace();serverFailure=e.toString();}
 }
 @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {

  var mc=Minecraft.getInstance();if(done)return;
  org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
  if(++ticks>2400){done=true;System.err.println("FAIL: MAODIE CLIENT TIMEOUT");mc.stop();return;}
  if(mc.getOverlay()!=null||mc.level==null||mc.player==null||!mc.gameRenderer.getMainCamera().isInitialized())return;
  try {
   if(serverFailure!=null)throw new AssertionError(serverFailure);
   java.lang.reflect.Method growth=GiantCatBossAnimation.class.getMethod("maodieGrowthScale",float.class);
   if(Math.abs((Float)growth.invoke(null,0F)-.64F)>.001||Math.abs((Float)growth.invoke(null,20F)-4F)>.001)throw new AssertionError("Growth endpoints");
   var pose=GiantCatBossAnimation.sample((byte)10,10F,(byte)9,20F,Map.of());
   if(Math.abs(pose.getOrDefault("head",RuntimeBlockbenchModel.GroupTransform.IDENTITY).xRot())<.2F)throw new AssertionError("Missing giant hiss pose");
   if(seenCycle!=cycle) {
    seenCycle=cycle;mc.options.pauseOnLostFocus=false;mc.setScreen(null);
    org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),1280,900);
    mc.options.hideGui=false;mc.options.fov().set(70);
    if(cn.laowu.mod.compat.create.CreateIntegration.isLoaded()!=Boolean.getBoolean("laowu.create_expected"))throw new AssertionError("Dependency mode mismatch");
   }
   mc.player.setYRot(viewYaw);mc.player.setXRot(viewPitch);mc.player.setYHeadRot(viewYaw);
   for(var s:mc.level.getEntitiesOfClass(BoguShelfSeat.class,new AABB(ROOT).inflate(4)))if(s.isSummoningMaodie()){
    long age=mc.level.getGameTime()-s.maodieAuraStartTick();
    if(cycle==4&&lateReturned&&age>=30&&age<60)photo(mc,"late-aura");
    if(age>=25&&age<50){if(s.cat()==null)throw new AssertionError("Cat missing during aura");photo(mc,"aura-middle");}
    if(age>=50&&age<60){if(s.cat()==null)throw new AssertionError("Cat left aura early");photo(mc,"aura-end");}
   }
   for(var b:mc.level.getEntitiesOfClass(GiantCatBoss.class,new AABB(ROOT).inflate(64),GiantCatBoss::isMaodie)){
    if(b.isSummoning()){if(b.getPhaseTicks(0)<8)photo(mc,"growth-start");else photo(mc,"growth-middle");}
    else photo(mc,"boss-bar");
    if(cycle==3&&combatAge>=0&&b.getPhaseTicks(0)>=6){
     String name=switch(b.getPhase()){case 3->"roll-windup";case 4->"roll";case 5->"roll-recovery";case 6->"jump-windup";case 7->"jump";case 8->"slam-recovery";case 9->"hiss-windup";case 10->"hiss";case 11->"hiss-recovery";default->null;};
     if(name!=null)photo(mc,name);
     if(b.isMaodieEnraged())photo(mc,"enraged");
    }
   }
   if(cycle==5&&cancelReady&&noDust(mc)){
    for(var s:mc.level.getEntitiesOfClass(BoguShelfSeat.class,new AABB(ROOT).inflate(4)))if(s.isSummoningMaodie())throw new AssertionError("Cancelled aura still tracked");
    photo(mc,"cancelled");cancelCaptured=true;
   }
   if(cycle==6&&unloadReady){
    for(var s:mc.level.getEntitiesOfClass(BoguShelfSeat.class,new AABB(ROOT).inflate(4)))if(s.isSummoningMaodie())throw new AssertionError("Unloaded/reloaded aura still tracked");
    photo(mc,"unload-reload");unloadCaptured=true;
   }
   if(finished){
    var bars=net.minecraft.client.gui.components.BossHealthOverlay.class.getDeclaredField("events");bars.setAccessible(true);
    if(!((Map<?,?>)bars.get(mc.gui.getBossOverlay())).isEmpty())throw new AssertionError("Boss bar remains after restored cat");
    for(int i=0;i<4;i++)if(!shots.contains(i+"-aura-middle")||!shots.contains(i+"-boss-bar"))throw new AssertionError("Missing real aura/boss screenshots cycle "+i);
    for(String name:List.of("roll-windup","roll","roll-recovery","jump-windup","jump","slam-recovery","hiss-windup","hiss","hiss-recovery","enraged"))if(!shots.contains("3-"+name))throw new AssertionError("Missing real combat screenshot "+name);
    if(!shots.contains("4-late-aura")||!shots.contains("5-cancelled")||!shots.contains("6-unload-reload"))throw new AssertionError("Missing real late/cancel/unload evidence");
    photo(mc,"restored");System.out.println("MAODIE CLIENT PASS: LATE TRACKING, CANCEL, REAL CHUNK UNLOAD/RELOAD, FOUR FACINGS, AURA, GROWTH, BOSS BAR, THREE ATTACKS, ENRAGE, RESTORE; Create="+cn.laowu.mod.compat.create.CreateIntegration.isLoaded());done=true;mc.stop();return;
   }
   var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->serverTick(mc,id));
  }catch(Throwable e){done=true;e.printStackTrace();System.err.println("FAIL: MAODIE CLIENT "+e);mc.stop();}
 }
}
