package cn.laowu.mod.test;
import cn.laowu.mod.*;import cn.laowu.mod.painting.*;import cn.laowu.mod.create.*;
import net.minecraft.core.BlockPos;import net.minecraft.gametest.framework.*;import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;import net.minecraft.world.inventory.*;import net.minecraft.world.item.*;
import java.util.*;import java.util.concurrent.*;import java.lang.reflect.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("laowu") @PrefixGameTestTemplate(false)
public class CatPaintingTransactionProbe {
 static Object field(Class<?> type,Object object,String name){
  try{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(Exception e){throw new AssertionError(e);}
 }
 static ServerPlayer player(GameTestHelper h,BlockPos at){
  var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PaintingTransaction"));
  try{var channel=net.minecraft.network.Connection.class.getDeclaredField("channel");channel.setAccessible(true);channel.set(p.connection.getConnection(),new io.netty.channel.embedded.EmbeddedChannel());}catch(Exception e){throw new AssertionError(e);}
  p.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);return p;
 }
 @GameTest(template="artillery_probe",timeoutTicks=80)
 public static void revisionSurvivesActualContainerWire(GameTestHelper h){
  if(!cn.laowu.mod.compat.create.CreateIntegration.isLoaded()){h.succeed();return;}
  var local=new BlockPos(3,3,3);h.setBlock(local.below(),net.minecraft.world.level.block.Blocks.STONE);h.setBlock(local,CatPaintingTableRegistration.BLOCK.get());var at=h.absolutePos(local);
  var p=player(h,at);var sender=new CatPaintingTableMenu(91,p.getInventory(),at);
  var receiver=new CatPaintingTableMenu(91,p.getInventory(),h.absolutePos(new BlockPos(10,10,10)));
  var be=(CatPaintingTableBlockEntity)h.getLevel().getBlockEntity(at);
  @SuppressWarnings("unchecked")var slots=(List<DataSlot>)field(AbstractContainerMenu.class,sender,"dataSlots");
  for(long revision:new long[]{32767,32768,65535,65536,4294967296L,Long.MAX_VALUE-1}){
   var tag=be.saveWithoutMetadata(h.getLevel().registryAccess());tag.putLong("Revision",revision);be.loadWithComponents(tag,h.getLevel().registryAccess());
   for(int i=0;i<slots.size();i++){
    var buf=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
    try{
     var packet=new net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket(91,i,slots.get(i).get());
     net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket.STREAM_CODEC.encode(buf,packet);var decoded=net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket.STREAM_CODEC.decode(buf);
     receiver.setData(decoded.getId(),decoded.getValue());
    }finally{buf.release();}
   }
   h.assertTrue(receiver.revision()==revision,"Revision "+revision+" must survive actual signed-short container wire; got "+receiver.revision());
  }
  h.succeed();
 }
 @GameTest(template="artillery_probe",batch="painting_tx_close",timeoutTicks=600)
 public static void closeDuringPersistence(GameTestHelper h){transaction(h,"close");}
 @GameTest(template="artillery_probe",batch="painting_tx_distance",timeoutTicks=600)
 public static void distanceDuringPersistence(GameTestHelper h){transaction(h,"distance");}
 @GameTest(template="artillery_probe",batch="painting_tx_disconnect",timeoutTicks=600)
 public static void disconnectDuringPersistence(GameTestHelper h){transaction(h,"disconnect");}
 @GameTest(template="artillery_probe",batch="painting_tx_slot",timeoutTicks=600)
 public static void changedSlotDuringPersistence(GameTestHelper h){transaction(h,"slot");}
 @GameTest(template="artillery_probe",batch="painting_tx_break",timeoutTicks=600)
 public static void destroyedTableDuringPersistence(GameTestHelper h){transaction(h,"break");}
 @SuppressWarnings("unchecked")
 private static void transaction(GameTestHelper h,String invalidation){
  if(!cn.laowu.mod.compat.create.CreateIntegration.isLoaded()){h.succeed();return;}
  var local=new BlockPos(3,3,3);h.setBlock(local.below(),net.minecraft.world.level.block.Blocks.STONE);h.setBlock(local,CatPaintingTableRegistration.BLOCK.get());var at=h.absolutePos(local);
  var p=player(h,at);var list=h.getLevel().getServer().getPlayerList();
  var players=(List<ServerPlayer>)field(PlayerList.class,list,"players");
  var byId=(Map<UUID,ServerPlayer>)field(PlayerList.class,list,"playersByUUID");
  players.add(p);byId.put(p.getUUID(),p);
  var menu=new CatPaintingTableMenu(91,p.getInventory(),at);p.containerMenu=menu;
  var old=CatPaintingProbe.data(1,1);menu.getSlot(0).set(CatPaintingData.createStack(old));
  var be=(CatPaintingTableBlockEntity)h.getLevel().getBlockEntity(at);
  var service=PaintingService.get(h.getLevel().getServer());
  var io=(ThreadPoolExecutor)field(PaintingService.class,service,"io");
  var blocked=new CountDownLatch(2);var release=new CountDownLatch(1);
  var dropped=new java.util.concurrent.atomic.AtomicReference<ItemStack>();
  Runnable hold=()->{blocked.countDown();try{release.await(8,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}};
  io.execute(hold);io.execute(hold);
  byte[] png;
  try{
   var image=new java.awt.image.BufferedImage(16,16,java.awt.image.BufferedImage.TYPE_INT_RGB);
   image.setRGB(0,0,0xFF73B6E1);var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",bytes);
   png=PaintingImages.validateUpload(bytes.toByteArray(),old.spec());
  }catch(Exception e){release.countDown();players.remove(p);byId.remove(p.getUUID());throw new AssertionError(e);}
  UUID token=service.begin(p,91,menu.revision(),old.spec(),png.length,PaintingAssetStore.hash(png));
  h.assertTrue(!token.equals(new UUID(0,0))&&be.locked(),"Live connection and real menu must begin transaction");
  service.accept(p,token,0,png);service.commit(p,token);
  h.runAfterDelay(2,()->{
   try{
    h.assertTrue(blocked.getCount()==0 && !io.getQueue().isEmpty(),"Persistence must be queued behind real IO workers before invalidation");
    switch(invalidation){
     case "close" -> p.containerMenu=p.inventoryMenu;
     case "distance" -> p.setPos(p.getX()+100,p.getY(),p.getZ());
     case "disconnect" -> {service.disconnected(p);players.remove(p);byId.remove(p.getUUID());}
     case "slot" -> menu.getSlot(0).set(CatPaintingData.createStack(old));
     case "break" -> {
      h.setBlock(local,net.minecraft.world.level.block.Blocks.AIR);
      var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(at).inflate(2));
      var paintings=drops.stream().filter(e->CatPaintingData.read(e.getItem()).map(old::equals).orElse(false)).toList();
      h.assertTrue(paintings.stream().mapToInt(e->e.getItem().getCount()).sum()==1,"Destroy drops the original image exactly once before normal item gravity");
      dropped.set(paintings.get(0).getItem().copy());
     }
    }
   }finally{release.countDown();}
  });
  var idleTick=new java.util.concurrent.atomic.AtomicLong(-1);
  h.runAfterDelay(3,()->h.succeedWhen(()->{
    if(io.getActiveCount()!=0 || !io.getQueue().isEmpty())idleTick.set(-1);
    h.assertTrue(io.getActiveCount()==0 && io.getQueue().isEmpty(),"Persistence and late callback must actually finish");
    long now=h.getLevel().getServer().getTickCount();if(idleTick.get()<0)idleTick.set(now);
    h.assertTrue(now>idleTick.get(),"Observe IO idle across a server tick so the completion callback has executed");
    h.assertTrue(!be.locked(),"Cancelled transaction releases slot lock");
    if(invalidation.equals("break")){
     h.assertTrue(dropped.get()!=null && dropped.get().getCount()==1 && CatPaintingData.read(dropped.get()).orElseThrow().equals(old),"Original drop retains image data");
     h.assertTrue(menu.getSlot(0).getItem().isEmpty(),"Late completion cannot refill the removed table or create a replacement painting");
    }else h.assertTrue(menu.getSlot(0).getItem().getCount()==1 && CatPaintingData.read(menu.getSlot(0).getItem()).orElseThrow().equals(old),"Late "+invalidation+" callback must preserve original image and count");
    h.assertTrue(!menu.getSlot(1).hasItem(),"Cancelled print must not produce an output");
    release.countDown();service.disconnected(p);players.remove(p);byId.remove(p.getUUID());p.containerMenu=p.inventoryMenu;
  }));
 }
}
