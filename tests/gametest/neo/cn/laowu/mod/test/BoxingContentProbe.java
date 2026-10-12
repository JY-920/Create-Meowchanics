package cn.laowu.mod.test;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
@GameTestHolder("laowu") @PrefixGameTestTemplate(false)
public final class BoxingContentProbe {
 private static CraftingInput grid(boolean filledCenter){
  var stacks=new ArrayList<ItemStack>();
  for(int i=0;i<9;i++)stacks.add(i==4?(filledCenter?Items.OAK_SLAB.getDefaultInstance():ItemStack.EMPTY)
    :(i>=6?Items.STRIPPED_OAK_LOG:Items.OAK_SLAB).getDefaultInstance());
  return net.minecraft.world.item.crafting.CraftingInput.of(3,3,stacks);
 }
 @GameTest(template="accessory_probe",timeoutTicks=20)
 public static void shelfUsesRequestedEightIngredientGrid(GameTestHelper h){
  var recipes=h.getLevel().getRecipeManager();var input=grid(false);
  var found=recipes.getRecipeFor(RecipeType.CRAFTING,input,h.getLevel());
  h.assertTrue(found.isPresent(),"Oak slab frame and stripped oak log base must have a recipe");
  var result=found.orElseThrow().value().assemble(input,h.getLevel().registryAccess());
  h.assertTrue(result.is(LaoWuMod.BOGU_SHELF_ITEM.get())&&result.getCount()==1,"Requested grid crafts one shelf");
  var invalid=recipes.getRecipeFor(RecipeType.CRAFTING,grid(true),h.getLevel());
  h.assertTrue(invalid.isEmpty()||!invalid.get().value().getResultItem(h.getLevel().registryAccess()).is(LaoWuMod.BOGU_SHELF_ITEM.get()),
    "Shelf recipe must retain empty middle");
  h.succeed();
 }
 @GameTest(template="accessory_probe",timeoutTicks=20)
 public static void shelfAndEggAppearWithBossSummonersNotCatCare(GameTestHelper h){
  var params=new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS,true,h.getLevel().registryAccess());
  LaoWuMod.LAOWU_TAB.get().buildContents(params);LaoWuMod.CAT_PROGRESSION_TAB.get().buildContents(params);
  var main=new ArrayList<>(LaoWuMod.LAOWU_TAB.get().getDisplayItems());
  h.assertTrue(main.stream().filter(s->s.is(LaoWuMod.BOGU_SHELF_ITEM.get())).count()==1,"Shelf appears once in main tab");
  h.assertTrue(LaoWuMod.CAT_PROGRESSION_TAB.get().getDisplayItems().stream().noneMatch(s->s.is(LaoWuMod.BOGU_SHELF_ITEM.get())),"Shelf is not a cat care item");
  var egg=BuiltInRegistries.ITEM.get(LaoWuMod.id("boxing_cat_spawn_egg"));
  h.assertTrue(egg instanceof SpawnEggItem,"Boxing cat egg registered");
  h.assertTrue(((SpawnEggItem)egg).getType(egg.getDefaultInstance())==LaoWuMod.MAODIE_FIGHTER.get(),"Egg spawns boxing fighter rather than giant cat");
  h.assertTrue(main.stream().anyMatch(s->s.is(egg)),"Boxing egg is discoverable in main creative tab");
  h.succeed();
 }
}
