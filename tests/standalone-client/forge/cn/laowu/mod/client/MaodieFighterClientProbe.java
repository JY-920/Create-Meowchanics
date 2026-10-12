package cn.laowu.mod.client;

import cn.laowu.mod.*;
import cn.laowu.mod.block.BoguShelfBlock;
import cn.laowu.mod.entity.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
@EventBusSubscriber(modid="laowu",value=Dist.CLIENT)
public final class MaodieFighterClientProbe {
 private static final BlockPos ROOT=new BlockPos(0,100,0);
 private static final byte[] PHASES={1,7,3,2,8,6,4,9,5};
 private static final String[] NAMES={"guard","jab","uppercut","combo","block","backstep","roundhouse","jump-slam","pull"};
 private static final int[] PEAKS={4,4,6,14,4,4,8,16,8};
 private static final Set<String> shots=new HashSet<>();
 private static int ticks,ready,nativeClicks,lastClick=-100,renderStage=-1,stableFrames;
 private static boolean done,initialized;
 private static boolean previousHiss,sawShelfGaze;
 private static int hissStarts,hissEnds;
 private static final Set<net.minecraft.client.resources.sounds.TickableSoundInstance> heardHisses=
  Collections.newSetFromMap(new IdentityHashMap<>());
 private static volatile int stage=-2,captured=-1,observedStage=-1;
 private static volatile long observedAges;
 private static volatile boolean holding,finished;
 private static volatile String failure;
 private static volatile float yaw=180,pitch=20;
 private static volatile int originalId=-1;
 private static MaodieFighter fighter;
 private static Cat original;
 private static BoguShelfSeat seat;
 private static long stageStart,lastServerTick=Long.MIN_VALUE,thirdStroke=-1,createdAt=-1,killed=-1;
 private static Vec3 createdPosition;
 private static double pullStart;
 private static final long WALL_START=System.nanoTime();
 private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
 private static void verifyInheritedTextures(Minecraft mc)throws Exception {
  var renderer=(MaodieFighterRenderer)mc.getEntityRenderDispatcher().getRenderer(LaoWuMod.MAODIE_FIGHTER.get().create(mc.level));
  var seen=new HashSet<net.minecraft.resources.ResourceLocation>();
  for(var variant:net.minecraft.core.registries.BuiltInRegistries.CAT_VARIANT.keySet()) {
   var cat=LaoWuMod.MAODIE_FIGHTER.get().create(mc.level);
   cat.setInheritedGenome(cn.laowu.mod.genetics.CatGenome.uniform(variant));
   var location=renderer.getTextureLocation(cat);
   require(seen.add(location),"Each inherited cat variant must select its own texture: "+variant);
   require(mc.getTextureManager().getTexture(location) instanceof net.minecraft.client.renderer.texture.DynamicTexture,
    "Inherited boxer texture needs its solid articulated atlas: "+variant);
   var image=((net.minecraft.client.renderer.texture.DynamicTexture)mc.getTextureManager().getTexture(location)).getPixels();
   var original=net.minecraft.core.registries.BuiltInRegistries.CAT_VARIANT.get(variant).texture();
   try(var stream=mc.getResourceManager().getResource(original).orElseThrow().open();
       var source=com.mojang.blaze3d.platform.NativeImage.read(stream)) {
    require(image.getPixelRGBA(7,6)==source.getPixelRGBA(7,6),"Inherited face pixels must match "+variant);
   }
   for(int x=8;x<16;x++)for(int y=13;y<21;y++)
    require((image.getPixelRGBA(x,y)>>>24)>0,"Articulated arms/fists/legs cannot contain transparent holes: "+variant+" "+x+","+y);
  }
  var mixed=LaoWuMod.MAODIE_FIGHTER.get().create(mc.level);
  mixed.setInheritedGenome(cn.laowu.mod.genetics.CatGenome.uniform(net.minecraft.resources.ResourceLocation.tryParse("minecraft:black"))
   .withMaterial(cn.laowu.mod.genetics.CatRegion.HEAD_PRIMARY,net.minecraft.resources.ResourceLocation.tryParse("minecraft:red")));
  require(!seen.contains(renderer.getTextureLocation(mixed)),"Blended genomes must not collapse to a uniform fixed orange atlas");
  System.out.println("FIGHTER TEXTURES PASS: "+seen.size()+" native cat variants, blended genome and solid articulated limb UVs");
 }
 private static void isolated(Minecraft mc){
  String directory=mc.gameDirectory.toPath().toAbsolutePath().normalize().toString().replace('\\','/');
  require(directory.contains("/build/maodie-client-")||directory.endsWith("/build/standalone-cat-client"),
   "Refusing to edit a non-build client world: "+directory);
  require(mc.getSingleplayerServer()!=null,"Probe requires isolated integrated server");
 }
 private static void setPhase(MaodieFighter f,byte phase)throws Exception{
  var method=MaodieFighter.class.getDeclaredMethod("phase",byte.class);method.setAccessible(true);method.invoke(f,phase);
 }
 @SuppressWarnings("unchecked") private static void freezeClock(ServerLevel l)throws Exception{
  var field=MaodieFighter.class.getDeclaredField("START");field.setAccessible(true);
  fighter.getEntityData().set((EntityDataAccessor<Long>)field.get(null),l.getGameTime()-PEAKS[stage]);
 }
 private static void cooldown()throws Exception{
  var field=MaodieFighter.class.getDeclaredField("cooldown");field.setAccessible(true);field.setInt(fighter,10000);
 }
 private static void aim(ServerPlayer p){
  Vec3 focus=fighter.position().add(0,.8,0),d=focus.subtract(p.getEyePosition());
  yaw=(float)Math.toDegrees(Math.atan2(-d.x,d.z));pitch=(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance()));
  p.setYRot(yaw);p.setXRot(pitch);
 }
 private static void startStage(ServerLevel l,ServerPlayer p,int index)throws Exception{
  holding=false;observedAges=0;observedStage=-1;
  fighter.setNoAi(false);fighter.setPos(5.5,100,.5);fighter.setDeltaMovement(Vec3.ZERO);
  p.connection.teleport(5.5,100.3,6.5,180,20);p.setDeltaMovement(Vec3.ZERO);
  fighter.setTarget(p);setPhase(fighter,MaodieFighter.IDLE);cooldown();
  if(PHASES[index]!=MaodieFighter.IDLE)fighter.beginAttack(PHASES[index]);
  pullStart=p.distanceToSqr(fighter);stageStart=l.getGameTime();stage=index;aim(p);
  System.out.println("FIGHTER CLIENT LIVE ACTION "+NAMES[index]);
 }
 @SubscribeEvent public static void spawned(net.minecraftforge.event.entity.EntityJoinLevelEvent event){
  if(!initialized||stage!=-2||seat==null||!(event.getLevel() instanceof ServerLevel l)
     ||!(event.getEntity() instanceof MaodieFighter f)||!f.isMaodie()||!f.maodieOrigin().equals(ROOT))return;
  // This event runs inside addFreshEntity, before tickSeat clears the source state.
  // Both timestamps are authoritative even if the client observer stalls for several ticks.
  thirdStroke=seat.maodieState().startTick();createdAt=l.getGameTime();createdPosition=f.position();
  System.out.println("FIGHTER SPAWN EVENT: thirdStroke="+thirdStroke+" createdAt="+createdAt
   +" elapsed="+(createdAt-thirdStroke)+" count="+seat.maodieState().count()+" nativeClicks="+nativeClicks+" position="+createdPosition);
 }
 private static void server(Minecraft mc,UUID id){
  try{
   isolated(mc);var l=mc.getSingleplayerServer().overworld();var p=l.getServer().getPlayerList().getPlayer(id);
   if(p==null||l.getGameTime()==lastServerTick||finished)return;lastServerTick=l.getGameTime();
   if(!initialized){
    initialized=true;l.setDayTime(6000);l.setWeatherParameters(60000,0,false,false);l.getServer().setDifficulty(Difficulty.NORMAL,true);
    for(var e:l.getEntitiesOfClass(Entity.class,new AABB(ROOT).inflate(30),e->e instanceof Cat||e instanceof BoguShelfSeat||e instanceof MaodieFighter))e.discard();
    for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++){
     l.setBlock(ROOT.offset(x,-1,z),Blocks.OAK_PLANKS.defaultBlockState(),3);
     for(int y=0;y<7;y++)l.removeBlock(ROOT.offset(x,y,z),false);
    }
    var block=(BoguShelfBlock)LaoWuMod.BOGU_SHELF.get();
    var state=block.defaultBlockState().setValue(BoguShelfBlock.FACING,Direction.SOUTH);
    l.setBlock(ROOT,state,3);block.setPlacedBy(l,ROOT,state,p,ItemStack.EMPTY);
    original=EntityType.CAT.create(l);original.setNoAi(true);original.setPos(.5,100,0);l.addFreshEntity(original);
    seat=LaoWuMod.BOGU_SHELF_SEAT.get().create(l);seat.configure(ROOT,Direction.SOUTH,original);l.addFreshEntity(seat);original.startRiding(seat,true);seat.hold(original);
    originalId=original.getId();p.setShiftKeyDown(false);p.getInventory().clearContent();
    p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=true;p.getAbilities().flying=true;p.setNoGravity(true);p.onUpdateAbilities();
    p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,30000,0,false,false));
    p.connection.teleport(.5,100.25,3,180,0);stageStart=l.getGameTime();return;
   }
   if(stage==-2){
    Vec3 d=original.position().add(0,.45,0).subtract(p.getEyePosition());
    yaw=(float)Math.toDegrees(Math.atan2(-d.x,d.z));pitch=(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance()));
    var list=l.getEntitiesOfClass(MaodieFighter.class,new AABB(ROOT).inflate(16));
    if(!list.isEmpty()){
     require(nativeClicks==3&&list.size()==1,"Exactly three native clicks must create exactly one fighter");
     fighter=list.get(0);
     require(thirdStroke>=0&&createdAt-thirdStroke==10,
      "Fighter creation must be exactly ten ticks after the third hiss: thirdStroke="+thirdStroke+" createdAt="+createdAt
       +" elapsed="+(createdAt-thirdStroke)+" observedAt="+l.getGameTime()+" observerDelay="+(l.getGameTime()-createdAt)
       +" nativeClicks="+nativeClicks+" currentSeatCount="+seat.maodieState().count());
     require(createdPosition!=null&&createdPosition.z>=ROOT.getZ()+1.45&&createdPosition.z<=ROOT.getZ()+2.51
      &&Math.abs(createdPosition.x-1.25)<.76,
      "Fighter must be created close to the displayed cat: created="+createdPosition+" observed="+fighter.position());
     require(!seat.isSummoningMaodie()&&!seat.maodieState().active()&&!fighter.isSummoning(),"No aura/session/summon phase survives direct spawn");
     require(l.getEntity(original.getUUID())==null,"Consumed source cat still exists");
     require(!fighter.hasCustomName(),"Fighter has a custom boss label");
     System.out.println("FIGHTER NATIVE THREE-CLICK DIRECT SPAWN PASS: elapsed="+(createdAt-thirdStroke)+" observerDelay="+(l.getGameTime()-createdAt));
     startStage(l,p,0);
    }else require(l.getGameTime()-stageStart<140,"Native empty-hand right-click summon timed out");
    return;
   }
   if(stage>=NAMES.length){
    if(killed<0){fighter.setNoAi(false);fighter.kill();killed=l.getGameTime();}
    if(l.getGameTime()-killed>=25){
     require(l.getEntity(original.getUUID())==null,"Killing fighter restored the original cat");
     require(l.getEntitiesOfClass(Cat.class,new AABB(ROOT).inflate(20)).isEmpty(),"Killing fighter created an ordinary replacement cat");
     require(l.getEntitiesOfClass(MaodieFighter.class,new AABB(ROOT).inflate(20)).isEmpty(),"Killed fighter remained alive");
     finished=true;
    }
    return;
   }
   cooldown();require(!fighter.hasCustomName(),"Action added a name label");aim(p);
   if(!holding){
    int duration=PHASES[stage]==MaodieFighter.IDLE?12:MaodieFighterTiming.duration(PHASES[stage]);
    if(l.getGameTime()-stageStart>=duration+3){
     require(observedStage==stage&&Long.bitCount(observedAges)>=2,"Client did not observe advancing real "+NAMES[stage]+" timeline before freeze");
     if(PHASES[stage]==MaodieFighter.PULL)require(p.distanceToSqr(fighter)<pullStart-.1,"Connected player did not integrate pull velocity");
     fighter.setPos(5.5,100,.5);fighter.setDeltaMovement(Vec3.ZERO);p.connection.teleport(5.5,100.3,6.5,180,20);p.setDeltaMovement(Vec3.ZERO);
     fighter.setNoAi(false);fighter.setTarget(p);setPhase(fighter,MaodieFighter.IDLE);
     if(PHASES[stage]!=MaodieFighter.IDLE)fighter.beginAttack(PHASES[stage]);
     fighter.setNoAi(true);freezeClock(l);holding=true;
    }
   }else{
    freezeClock(l);
    if(captured==stage){int next=stage+1;if(next<NAMES.length)startStage(l,p,next);else{holding=false;stage=next;}}
   }
  }catch(Throwable e){failure=e.toString();e.printStackTrace();}
 }
 private static void noBossBar(Minecraft mc)throws Exception{
  var field=net.minecraft.client.gui.components.BossHealthOverlay.class.getDeclaredField("events");field.setAccessible(true);
  var bars=(Map<?,?>)field.get(mc.gui.getBossOverlay());
  if(holding){
   require(bars.size()==1,"Tracked boxing cat must send exactly one boss bar");
   var bar=(net.minecraft.world.BossEvent)bars.values().iterator().next();
   require(bar.getName().getString().equals(net.minecraft.network.chat.Component.translatable("entity.laowu.maodie_fighter").getString()),
     "Boss bar uses localized Boxing Cat name");
   require(Math.abs(bar.getProgress()-1)<.001,"Full-health boxing cat bar is full");
  }else if(finished)require(bars.isEmpty(),"Dead boxing cat left a boss bar");
 }
 @SubscribeEvent public static void hud(net.minecraftforge.client.event.RenderGuiEvent.Post event){
  if(holding&&stage==0&&!done){try{var mc=Minecraft.getInstance();noBossBar(mc);photo(mc,"boxing-boss-bar");}
   catch(Throwable e){failure=e.toString();}}
 }
 private static void noAuraRenderer(Minecraft mc){
  // The legacy seat timestamp also synchronizes the ordinary ten-tick hiss.
  // Check rendering registration, not that valid transient gameplay timestamp.
  require(!MaodieAuraEffects.class.isAnnotationPresent(EventBusSubscriber.class),"Legacy aura render subscriber remains registered");
  for(var method:MaodieAuraEffects.class.getDeclaredMethods())
   require(!method.isAnnotationPresent(SubscribeEvent.class),"Legacy aura still declares an event handler");
  require(mc.gameRenderer.getShader("laowu:maodie_wind")==null&&mc.gameRenderer.getShader("maodie_wind")==null,
   "Legacy Maodie wind shader remains registered");
 }
 private static void photo(Minecraft mc,String name){
  if(shots.add(name))net.minecraft.client.Screenshot.grab(mc.gameDirectory,"fighter-v11-"+name+".png",mc.getMainRenderTarget(),message->{});
 }
 @SubscribeEvent public static void render(net.minecraftforge.client.event.RenderLevelStageEvent event){
  int index=stage;
  if(event.getStage()!=net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_LEVEL||done)return;
  try{
   var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
   if(index==-2&&mc.level.getEntity(originalId) instanceof Cat display&&display.getVehicle() instanceof BoguShelfSeat s){
    if(mc.player.position().distanceTo(new Vec3(.5,100.25,3))>.2)return;
    if(!(mc.getEntityRenderDispatcher().getRenderer(display) instanceof HissingCatRenderer renderer))return;
    require(renderer.getModel() instanceof HissingCatModel,"Shelf cat model missing");
    var model=(HissingCatModel)renderer.getModel();
    require(model.isHissing()==s.hissing(),"Rendered shelf geometry disagrees with single hiss clock");
    if(nativeClicks==1&&s.hissing()&&mc.level.getGameTime()-s.hissStartTick()>=3)photo(mc,"shelf-hiss");
    if(nativeClicks==1&&!s.hissing()&&hissEnds==1)photo(mc,"shelf-sitting");
    return;
   }
   if(!holding||index<0||index>=NAMES.length)return;
   if(mc.player.position().distanceTo(new Vec3(5.5,100.3,6.5))>.4)return;
   for(var f:mc.level.getEntitiesOfClass(MaodieFighter.class,new AABB(ROOT).inflate(20))){
    if(f.getPhase()!=PHASES[index]||Math.abs(f.getPhaseTicks(0)-PEAKS[index])>2)continue;
    require(mc.getEntityRenderDispatcher().getRenderer(f) instanceof MaodieFighterRenderer,"Approved boxer renderer not installed");
    require(!f.hasCustomName()&&!f.isSummoning(),"Client still displays named/summoning fighter");
    noBossBar(mc);
    if(renderStage!=index){renderStage=index;stableFrames=0;}
    if(++stableFrames<3)return;
    photo(mc,NAMES[index]);captured=index;return;
   }
  }catch(Throwable e){failure=e.toString();e.printStackTrace();}
 }
 @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event){
  if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
  var mc=Minecraft.getInstance();if(done)return;org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
  if(++ticks>2200||(System.nanoTime()-WALL_START)>115_000_000_000L){
   done=true;System.err.println("FAIL: FIGHTER CLIENT TIMEOUT stage="+stage+" captured="+captured);mc.stop();return;
  }
  if(mc.getOverlay()!=null||mc.level==null||mc.player==null||!mc.gameRenderer.getMainCamera().isInitialized())return;
  try{
   if(failure!=null)throw new AssertionError(failure);
   isolated(mc);
   if(ready++==0){
    verifyInheritedTextures(mc);
    if(Boolean.getBoolean("laowu.fighter_texture_only")){done=true;mc.stop();return;}
    mc.options.pauseOnLostFocus=false;mc.setScreen(null);org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),1280,900);
    mc.options.fov().set(55);mc.options.hideGui=false;
    require(mc.getResourceManager().getResource(MaodieFighterModel.MODEL).isPresent(),"Approved Blockbench model asset missing");
    String[] clips={"guard","jab","uppercut","combo","block","backstep","roundhouse","jump_slam"};
    float[] peaks={0F,.075F,.14F,.3456F,.09F,.105F,.2F,.4F};
    for(int i=0;i<clips.length;i++)
     require(MaodieFighterAnimation.sample(clips[i],peaks[i]).size()==20,"Animation clip failed to load all twenty bones: "+clips[i]);
    require(!MaodieFighterAnimation.samplePhase(MaodieFighter.PULL,8).equals(MaodieFighterAnimation.samplePhase(MaodieFighter.BLOCK,8)),
     "Pull must visibly reach and draw in rather than replay the block pose");
    noAuraRenderer(mc);
    var shelfLines=LaoWuMod.BOGU_SHELF_ITEM.get().getDefaultInstance().getTooltipLines(mc.player,net.minecraft.world.item.TooltipFlag.NORMAL);
    for(String key:List.of("item.laowu.round_bogu_shelf.tooltip","item.laowu.boss_cat_treat.tooltip.warning")){
     String expected=net.minecraft.network.chat.Component.translatable(key).getString();
     require(!expected.equals(key)&&shelfLines.stream().anyMatch(l->l.getString().equals(expected)),"Shelf localized description missing: "+key);
    }
    for(String item:List.of("cat_boxing_gloves","cat_butter_cube","cat_giant_collar")){
     var stack=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(LaoWuMod.id(item)).getDefaultInstance();
     var lines=stack.getTooltipLines(mc.player,net.minecraft.world.item.TooltipFlag.NORMAL);
     require(lines.stream().anyMatch(l->l.getString().contains("25%")&&!l.getString().contains("%s")),
       "Real tooltip must render localized boss source and literal 25%: "+item+" "+lines);
    }
   }
   mc.player.setYRot(yaw);mc.player.setXRot(pitch);mc.player.setYHeadRot(yaw);noBossBar(mc);
   boolean hiss=false;
   if(mc.level.getEntity(originalId) instanceof Cat display&&display.getVehicle() instanceof BoguShelfSeat displaySeat){
    hiss=displaySeat.hissing();
    if(Math.abs(displaySeat.headYaw())>2)sawShelfGaze=true;
   }
   if(hiss&&!previousHiss)hissStarts++;
   if(!hiss&&previousHiss)hissEnds++;
   previousHiss=hiss;
   var soundField=HissingAudioManager.class.getDeclaredField("SHELF_SOUNDS");soundField.setAccessible(true);
   var shelfSounds=(Map<?,?>)soundField.get(null);
   for(Object value:shelfSounds.values()){
    var sound=(net.minecraft.client.resources.sounds.TickableSoundInstance)value;
    if(mc.getSoundManager().isActive(sound))heardHisses.add(sound);
   }
   var sessionField=HissingAudioManager.class.getDeclaredField("SESSIONS");sessionField.setAccessible(true);
   require(!((Map<?,?>)sessionField.get(null)).containsKey(originalId),"Shelf cat started delayed ambient audio");
   if(stage==-2&&nativeClicks<3&&ready>30&&ready-lastClick>=20&&originalId>=0){
    var target=mc.level.getEntity(originalId);
    if(target instanceof Cat&&target.getVehicle() instanceof BoguShelfSeat&&mc.player.distanceTo(target)<3){
     mc.player.setShiftKeyDown(false);require(mc.player.getMainHandItem().isEmpty(),"Fixture hand is not empty");
     mc.gameMode.interact(mc.player,target,InteractionHand.MAIN_HAND);nativeClicks++;lastClick=ready;
     System.out.println("FIGHTER NATIVE EMPTY-HAND RIGHT-CLICK PACKET "+nativeClicks);
    }
   }
   int observedIndex=stage;
   if(!holding&&observedIndex>=0&&observedIndex<NAMES.length){
    for(var f:mc.level.getEntitiesOfClass(MaodieFighter.class,new AABB(ROOT).inflate(20))){
     if(f.getPhase()==PHASES[observedIndex]){
      observedStage=observedIndex;int age=Math.min(63,(int)f.getPhaseTicks(0));observedAges|=1L<<age;
     }
    }
   }
   if(finished){
    require(sawShelfGaze,"Displayed cat never turned its head toward off-centre player");
    require(hissStarts==3&&hissEnds==3,"Three strokes must give exactly three single hiss windows: starts="+hissStarts+" ends="+hissEnds);
    require(heardHisses.size()==3,"Expected exactly three real active shelf sounds, got "+heardHisses.size());
    require(shelfSounds.isEmpty()&&heardHisses.stream().allMatch(s->s.isStopped()&&!mc.getSoundManager().isActive(s)),
      "A shelf hiss remained active after its single window");
    require(shots.contains("shelf-hiss")&&shots.contains("shelf-sitting"),"Missing visible shelf pose screenshots");
    require(shots.contains("boxing-boss-bar"),"Missing HUD boss bar screenshot");
    for(String name:NAMES){
     require(shots.contains(name),"Missing screenshot "+name);
     var path=mc.gameDirectory.toPath().resolve("screenshots/fighter-v11-"+name+".png");
     if(!java.nio.file.Files.exists(path)||java.nio.file.Files.size(path)<1000)return;
    }
    done=true;System.out.println("FIGHTER CLIENT PASS: three real clicks, close spawn, localized boss bar and reward tooltips, nine action timelines, single hiss audio, permanent death; Create="+cn.laowu.mod.compat.create.CreateIntegration.isLoaded());mc.stop();return;
   }
   var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->server(mc,id));
  }catch(Throwable e){done=true;e.printStackTrace();System.err.println("FAIL: FIGHTER CLIENT "+e);mc.stop();}
 }
}
