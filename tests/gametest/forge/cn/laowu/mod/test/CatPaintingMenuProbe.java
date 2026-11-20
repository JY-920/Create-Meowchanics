package cn.laowu.mod.test;
import cn.laowu.mod.*;import cn.laowu.mod.create.*;import cn.laowu.mod.painting.*;
import net.minecraft.core.BlockPos;import net.minecraft.gametest.framework.*;import net.minecraft.world.item.*;import net.minecraft.world.inventory.*;
import java.util.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("laowu") @PrefixGameTestTemplate(false)
public class CatPaintingMenuProbe {
 @GameTest(template="artillery_probe",timeoutTicks=80)
 public static void paintingSlotTransaction(GameTestHelper h){
  if(!cn.laowu.mod.compat.create.CreateIntegration.isLoaded()){h.succeed();return;}
  var pos=new BlockPos(3,3,3);h.setBlock(pos,CatPaintingTableRegistration.BLOCK.get());var at=h.absolutePos(pos);
  var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PaintingMenu"));p.moveTo(at.getX(),at.getY(),at.getZ(),0,0);
  var menu=new CatPaintingTableMenu(91,p.getInventory(),at);p.containerMenu=menu;
  int firstPlayerSlot=menu.slots.size()-36;
  p.getInventory().setItem(9,new ItemStack(Items.STONE));menu.quickMoveStack(p,firstPlayerSlot);
  h.assertTrue(menu.getSlot(0).getItem().isEmpty(),"Reject non-paintings");
  p.getInventory().setItem(9,new ItemStack(LaoWuMod.CAT_PAINTING.get(),2));menu.quickMoveStack(p,firstPlayerSlot);
  h.assertTrue(menu.getSlot(0).getItem().getCount()==1 && p.getInventory().getItem(9).getCount()==1,"Shift-click moves exactly one");
  long revision=menu.revision();UUID token=UUID.randomUUID();
  h.assertTrue(!menu.lock(token,revision+1),"Reject stale slot revision");
  h.assertTrue(menu.lock(token,revision),"Lock exact item");
  var other=new CatPaintingTableMenu(92,p.getInventory(),at);
  h.assertTrue(!other.lock(UUID.randomUUID(),revision),"Two players cannot lock one table");
  menu.quickMoveStack(p,0);menu.clicked(0,0,ClickType.SWAP,p);
  h.assertTrue(menu.getSlot(0).getItem().getCount()==1,"Locked click/shift/swap preserves item");
  var data=CatPaintingProbe.data(1,1);
  h.assertTrue(menu.commit(token,revision,data),"Commit exact token");
  h.assertTrue(menu.getSlot(0).getItem().isEmpty()&&CatPaintingData.read(menu.getSlot(1).getItem()).map(data::equals).orElse(false),"Successful print moves the one painting from left input to right output");
  h.assertTrue(!menu.getSlot(1).mayPlace(new ItemStack(LaoWuMod.CAT_PAINTING.get())),"Output refuses manual insertion");
  h.assertTrue(!menu.commit(token,revision,data),"Replay commit rejected");
  menu.unlock(token);
  var be=(CatPaintingTableBlockEntity)h.getLevel().getBlockEntity(at);
  var save=be.saveWithoutMetadata();
  var legacy=save.copy();legacy.put("Painting",legacy.getCompound("PrintedPainting").copy());legacy.remove("PrintedPainting");
  be.load(legacy);
  h.assertTrue(CatPaintingData.read(menu.getSlot(0).getItem()).map(data::equals).orElse(false)&&!menu.getSlot(1).hasItem(),"Legacy single-slot save retains its item on the left");
  menu.getSlot(1).set(ItemStack.EMPTY);be.load(save);
  h.assertTrue(CatPaintingData.read(menu.getSlot(1).getItem()).map(data::equals).orElse(false),"Table save/reload preserves the right output");
  menu.quickMoveStack(p,firstPlayerSlot);
  h.assertTrue(menu.getSlot(0).hasItem()&&!menu.lock(UUID.randomUUID(),menu.revision()),"A full output cannot be overwritten by the next print");
  h.assertTrue(CatPaintingData.read(menu.quickMoveStack(p,1)).map(data::equals).orElse(false)&&!menu.getSlot(1).hasItem(),"Shift-click takes the printed output");
  menu.getSlot(0).set(CatPaintingData.createStack(data));
  long next=menu.revision();UUID late=UUID.randomUUID();h.assertTrue(menu.lock(late,next),"Reprint lock");
  h.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR);
  h.assertTrue(!menu.commit(late,next,CatPaintingProbe.data(2,1)),"Broken table rejects late commit");
  var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(at).inflate(2));
  int count=drops.stream().filter(e->CatPaintingData.read(e.getItem()).map(data::equals).orElse(false)).mapToInt(e->e.getItem().getCount()).sum();
  h.assertTrue(count==1,"Breaking locked table drops original image exactly once");
  h.succeed();
 }
}
