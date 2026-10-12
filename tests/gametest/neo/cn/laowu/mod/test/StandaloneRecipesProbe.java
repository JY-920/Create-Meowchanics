package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.compat.create.CreateIntegration;
import cn.laowu.mod.item.PheromoneCatFoodItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class StandaloneRecipesProbe {
    private static final String[] BASE={
        "cat_ingot","cat_sheet","cat_strip","cat_powder","cat_component",
        "basic_breeding_box","intermediate_breeding_box","advanced_breeding_box","adoption_box","wish_adoption_box",
        "cat_storage_box","cat_pouch","cat_laser_pointer","cat_engineer_goggles","material_debug_wand","cat_ball",
        "cat_food","breeding_cat_food","mutation_cat_food","pheromone_cat_food","cat_can","golden_cat_can",
        "dried_fish_cod","dried_fish_salmon","golden_dried_fish","butter_bread","giant_cat_treat"
    };
    private static List<String> recipeNames() {
        var names=new ArrayList<>(List.of(BASE));
        for(var stat:List.of("attack","health","speed","stamina","intelligence","luck"))
            names.addAll(List.of(stat+"_cat_can","golden_"+stat+"_cat_can",stat+"_breeding_cat_food"));
        for(var suit:List.of("terminator","fishing","flight","fire","honey","transport","dynamite","engineering","medical","music","agent","diving","cockroach"))
            names.add(suit+"_suit");
        return names;
    }
    private static Item item(String name) {
        return BuiltInRegistries.ITEM.get(name.contains(":")?net.minecraft.resources.ResourceLocation.tryParse(name):LaoWuMod.id(name));
    }
    private static Recipe<?> recipe(GameTestHelper h,String name) {
        return h.getLevel().getRecipeManager().byKey(LaoWuMod.id("standalone/"+name)).orElseThrow().value();
    }
    private static CraftingInput input(List<ItemStack> stacks) {
        var padded=NonNullList.withSize(9,ItemStack.EMPTY);
        for(int i=0;i<stacks.size();i++)padded.set(i,stacks.get(i));
        return CraftingInput.of(3,3,padded);
    }

    @GameTest(template="artillery_probe",timeoutTicks=30)
    public static void recipesAreAbsentWhenCreateIsInstalled(GameTestHelper h) {
        boolean create=CreateIntegration.isLoaded();
        h.assertTrue(recipeNames().size()==58,"Fixture covers all standalone recipes");
        for(var name:recipeNames())
            h.assertTrue(h.getLevel().getRecipeManager().byKey(LaoWuMod.id("standalone/"+name)).isPresent()!=create,"Create-absent recipe gate: "+name);
        for(var name:List.of("cat_ingot_compacting","cat_storage_box","terminator_suit_sequenced_assembly"))
            h.assertTrue(h.getLevel().getRecipeManager().byKey(LaoWuMod.id(name)).isPresent()==create,"Existing Create recipe stays conditional: "+name);
        h.succeed();
    }

    @GameTest(template="artillery_probe",timeoutTicks=30)
    public static void everySurvivalCraftMatchesTheRealRecipeManager(GameTestHelper h) {
        if(CreateIntegration.isLoaded()){h.succeed();return;}
        int crafted=0;
        for(var name:recipeNames()) {
            if(name.startsWith("dried_fish")||name.equals("pheromone_cat_food"))continue;
            var recipe=(CraftingRecipe)recipe(h,name);
            var stacks=NonNullList.withSize(9,ItemStack.EMPTY);
            var ingredients=recipe.getIngredients();
            int width=recipe instanceof ShapedRecipe shaped?shaped.getWidth():3;
            for(int i=0;i<ingredients.size();i++) {
                var choices=ingredients.get(i).getItems();
                if(choices.length>0)stacks.set(recipe instanceof ShapedRecipe?(i/width)*3+i%width:i,choices[0].copyWithCount(1));
            }
            var input=input(stacks);
            var found=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel());
            h.assertTrue(found.isPresent(),"Loaded grid really matches "+name);
            var out=found.orElseThrow().value().assemble(input,h.getLevel().registryAccess());
            int count=switch(name){case "cat_sheet","cat_powder","cat_food","breeding_cat_food"->2;case "cat_strip"->4;case "mutation_cat_food"->8;default->1;};
            h.assertTrue(out.is(item(name))&&out.getCount()==count,"No ambiguous grid/wrong result: "+name+" -> "+out);
            // Every actual filled container returns its vanilla remainder at the same slot.
            var remaining=recipe.getRemainingItems(input);
            h.assertTrue(remaining.size()==input.size(),"Remainders follow the normalized input dimensions: "+name);
            for(int i=0;i<input.size();i++) {
                var stack=input.getItem(i);
                if(stack.is(Items.MILK_BUCKET)||stack.is(Items.LAVA_BUCKET))h.assertTrue(remaining.get(i).is(Items.BUCKET),"Bucket returned for "+name);
                if(stack.is(Items.HONEY_BOTTLE))h.assertTrue(remaining.get(i).is(Items.GLASS_BOTTLE),"Bottle returned for "+name);
            }
            crafted++;
        }
        h.assertTrue(crafted==55,"All ordinary survival recipes assembled");
        for(var name:List.of("dried_fish_cod","dried_fish_salmon")) {
            var recipe=(CampfireCookingRecipe)recipe(h,name);
            var fish=new SingleRecipeInput(new ItemStack(name.endsWith("cod")?Items.COOKED_COD:Items.COOKED_SALMON));
            var found=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING,fish,h.getLevel());
            h.assertTrue(found.isPresent()&&found.get().value().assemble(fish,h.getLevel().registryAccess()).is(LaoWuMod.DRIED_FISH.get()),"Both cooked fish become dried fish");
        }
        h.succeed();
    }

    @GameTest(template="artillery_probe",timeoutTicks=30)
    public static void explicitSurvivalLayoutsAndMaterialsMatch(GameTestHelper h) {
        if(CreateIntegration.isLoaded()){h.succeed();return;}
        var ingot=(CraftingRecipe)recipe(h,"cat_ingot");
        h.assertTrue(ingot.matches(input(List.of(new ItemStack(Items.IRON_INGOT),new ItemStack(item("cat_fur")),new ItemStack(Items.SUGAR))),h.getLevel()),"One sugar, iron and fur craft an ingot");
        h.assertTrue(!ingot.matches(input(List.of(new ItemStack(Items.IRON_INGOT),new ItemStack(item("cat_fur")))),h.getLevel()),"Ingot cannot omit sugar");
        var goggles=(CraftingRecipe)recipe(h,"cat_engineer_goggles");
        h.assertTrue(goggles.matches(input(List.of(ItemStack.EMPTY,new ItemStack(Items.STRING),ItemStack.EMPTY,new ItemStack(Items.GLASS_PANE),new ItemStack(item("cat_sheet")),new ItemStack(Items.GLASS_PANE))),h.getLevel()),"Goggles use string above glass-sheet-glass");
        h.assertTrue(!goggles.matches(input(List.of(new ItemStack(Items.STRING),ItemStack.EMPTY,ItemStack.EMPTY,new ItemStack(Items.GLASS_PANE),new ItemStack(item("cat_sheet")),new ItemStack(Items.GLASS_PANE))),h.getLevel()),"String must be centred, not a shapeless recipe");
        var wish=(CraftingRecipe)recipe(h,"wish_adoption_box");
        var wishGrid=List.of(ItemStack.EMPTY,new ItemStack(Items.PAPER),ItemStack.EMPTY,new ItemStack(Items.EMERALD),new ItemStack(item("adoption_box")),new ItemStack(Items.EMERALD),ItemStack.EMPTY,new ItemStack(Items.PAPER),ItemStack.EMPTY);
        h.assertTrue(wish.matches(input(wishGrid),h.getLevel()),"Wish box uses symmetric emeralds and paper");
        h.assertTrue(!wish.matches(input(List.of(new ItemStack(item("adoption_box")),new ItemStack(Items.PAPER),new ItemStack(Items.PAPER),new ItemStack(Items.EMERALD),new ItemStack(Items.EMERALD))),h.getLevel()),"Wish box no longer accepts arbitrary placement");
        var materials=Map.ofEntries(Map.entry("terminator","minecraft:iron_block"),Map.entry("fishing","minecraft:fishing_rod"),Map.entry("flight","minecraft:phantom_membrane"),Map.entry("fire","minecraft:blaze_rod"),Map.entry("honey","minecraft:honey_block"),Map.entry("transport","minecraft:chest"),Map.entry("dynamite","minecraft:tnt"),Map.entry("engineering","laowu:cat_engineer_goggles"),Map.entry("medical","minecraft:white_wool"),Map.entry("music","minecraft:note_block"),Map.entry("agent","minecraft:tinted_glass"),Map.entry("diving","minecraft:glass"),Map.entry("cockroach","minecraft:bread"));
        for(var entry:materials.entrySet()) {
            var suit=(CraftingRecipe)recipe(h,entry.getKey()+"_suit");
            var grid=new ArrayList<>(Collections.nCopies(9,ItemStack.EMPTY));
            grid.set(1,new ItemStack(item("cat_fur")));grid.set(7,new ItemStack(item("cat_fur")));
            grid.set(3,new ItemStack(item(entry.getValue())));grid.set(5,new ItemStack(item(entry.getValue())));grid.set(4,new ItemStack(item("cat_component")));
            h.assertTrue(suit.matches(input(grid),h.getLevel()),"Fur above/below and profession material left/right: "+entry.getKey());
            grid.set(5,ItemStack.EMPTY);
            h.assertTrue(!suit.matches(input(grid),h.getLevel()),"Cannot omit the second profession material: "+entry.getKey());
        }
        h.succeed();
    }

    @GameTest(template="artillery_probe",timeoutTicks=30)
    public static void namedFoodCopiesOwnerAndReturnsExactNameTag(GameTestHelper h) {
        if(CreateIntegration.isLoaded()){h.succeed();return;}
        var recipe=(CraftingRecipe)recipe(h,"pheromone_cat_food");
        for(var owner:List.of("Alice","Bob")) {
            var tag=new ItemStack(Items.NAME_TAG,3);
            tag.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal(" "+owner+" "));
            var marker=new net.minecraft.nbt.CompoundTag();marker.putInt("ProbeMarker",17);tag.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(marker));
            var stacks=NonNullList.withSize(9,ItemStack.EMPTY);
            stacks.set(0,new ItemStack(LaoWuMod.CAT_FOOD.get(),4));stacks.set(8,tag);
            var input=input(stacks);
            h.assertTrue(recipe.matches(input,h.getLevel()),"Named tag matches");
            var out=recipe.assemble(input,h.getLevel().registryAccess());
            h.assertTrue(PheromoneCatFoodItem.ownerName(out).orElseThrow().equals(owner),"Each craft copies its own trimmed owner, no stale preview");
            var returned=recipe.getRemainingItems(input).get(8);
            h.assertTrue(returned.getCount()==1&&ItemStack.isSameItemSameComponents(returned,tag),"Returns one exact tag with all original metadata");
            h.assertTrue(tag.getCount()==3&&input.getItem(0).getCount()==4,"Preview/remainder calculation never mutates inputs");
            stacks.set(4,new ItemStack(Items.DIRT));
            h.assertTrue(!recipe.matches(input(stacks),h.getLevel()),"Rejects extra ingredients");
            stacks.set(4,ItemStack.EMPTY);stacks.set(8,new ItemStack(Items.NAME_TAG));
            h.assertTrue(!recipe.matches(input(stacks),h.getLevel()),"Unnamed tags cannot make unbound food");
        }
        h.succeed();
    }

    @GameTest(template="artillery_probe",timeoutTicks=30)
    public static void unusedIndustrialEntriesRespectCreateVisibility(GameTestHelper h) {
        CreativeModeTabs.tryRebuildTabContents(FeatureFlags.DEFAULT_FLAGS,true,h.getLevel().registryAccess());
        boolean create=CreateIntegration.isLoaded();
        for(var name:List.of("cat_shell","cat_grenade","hissing_gas_bucket","liquid_cat_bucket")) {
            var item=item(name);
            h.assertTrue(item!=Items.AIR,"Old item ID still resolves: "+name);
            h.assertTrue(LaoWuMod.LAOWU_TAB.get().getDisplayItems().stream().anyMatch(s->s.is(item))==create,"Industrial creative visibility: "+name);
            if(!create) for(var tab:CreativeModeTabs.tabs()) {
                h.assertTrue(tab.getDisplayItems().stream().noneMatch(s->s.is(item)),"No display-tab leak: "+name);
                h.assertTrue(tab.getSearchTabDisplayItems().stream().noneMatch(s->s.is(item)),"No creative-search leak: "+name);
            }
        }
        for(var name:List.of("hissing_gas","flowing_hissing_gas","liquid_cat","flowing_liquid_cat"))
            h.assertTrue(BuiltInRegistries.FLUID.get(LaoWuMod.id(name))!=net.minecraft.world.level.material.Fluids.EMPTY,"Old fluid ID still resolves: "+name);
        h.succeed();
    }

    @GameTest(template="artillery_probe",timeoutTicks=30)
    public static void adoptionBoxesBelongOnlyToCatProgressionTab(GameTestHelper h) {
        CreativeModeTabs.tryRebuildTabContents(FeatureFlags.DEFAULT_FLAGS,true,h.getLevel().registryAccess());
        for(var item:List.of(LaoWuMod.ADOPTION_BOX_ITEM.get(),LaoWuMod.WISH_ADOPTION_BOX_ITEM.get())) {
            h.assertTrue(LaoWuMod.CAT_PROGRESSION_TAB.get().getDisplayItems().stream().anyMatch(s->s.is(item)),"Adoption box appears in cat progression tab");
            h.assertTrue(LaoWuMod.LAOWU_TAB.get().getDisplayItems().stream().noneMatch(s->s.is(item)),"Adoption box no longer in industrial/main tab");
        }
        h.succeed();
    }
}
