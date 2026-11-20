package cn.laowu.mod.test;
import cn.laowu.mod.entity.CatFishingLootContext;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
@net.minecraftforge.gametest.GameTestHolder("laowu")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public class CatPaintingFishingProbe {
 @GameTest(template="artillery_probe",timeoutTicks=100)
 public static void treasureAndLuck(GameTestHelper h){
  var level=h.getLevel();
  var table=level.getServer().getLootData().getLootTable(BuiltInLootTables.FISHING);
  int[] books=new int[2],treasure=new int[2];
  for(int mode=0;mode<2;mode++){
   var bobber=new CatFishingLootContext(level);
   var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,Vec3.ZERO)
    .withParameter(LootContextParams.TOOL,new ItemStack(Items.FISHING_ROD))
    .withParameter(LootContextParams.THIS_ENTITY,bobber).withLuck(mode==0?0:3).create(LootContextParamSets.FISHING);
   for(int i=0;i<4000;i++)for(var stack:table.getRandomItems(params)){
    if(stack.is(Items.ENCHANTED_BOOK))books[mode]++;
    if(stack.is(Items.ENCHANTED_BOOK)||stack.is(Items.NAME_TAG)||stack.is(Items.SADDLE)||stack.is(Items.NAUTILUS_SHELL)||stack.is(Items.BOW))treasure[mode]++;
   }
   h.assertTrue(books[mode]>0,"Working cats can catch enchanted books");
  }
  h.assertTrue(treasure[1]>treasure[0],"Higher luck improves treasure yield");
  System.out.println("Fishing loot regression: books="+java.util.Arrays.toString(books)+" treasure="+java.util.Arrays.toString(treasure));
  h.succeed();
 }
}

