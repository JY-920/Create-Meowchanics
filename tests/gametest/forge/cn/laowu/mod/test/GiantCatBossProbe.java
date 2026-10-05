package cn.laowu.mod.test;

import cn.laowu.mod.genetics.CatGenomeData;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class GiantCatBossProbe {

    @GameTest(template="accessory_probe",batch="giant_boss_item_tooltips")
    public static void bothSummonItemsWarnAboutHostileWildCats(GameTestHelper h) {
        for (String id : new String[]{"butter_bread","giant_cat_treat"}) {
            var item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("laowu",id));
            var lines=new java.util.ArrayList<net.minecraft.network.chat.Component>();
            item.appendHoverText(new ItemStack(item),h.getLevel(),lines,net.minecraft.world.item.TooltipFlag.NORMAL);
            h.assertTrue(lines.size()==2,"Summon item retains its usage text and adds a separate warning: "+id);
            h.assertTrue(hasKey(lines,"item.laowu.boss_cat_treat.tooltip.warning",net.minecraft.ChatFormatting.RED),
                    "Both real summon item tooltips include the red hostility warning: "+id);
        }
        h.succeed();
    }

    @GameTest(template="accessory_probe",batch="giant_boss_trophy_tooltips")
    public static void trophiesShareOrderedSourceEffectAndAccessoryLabels(GameTestHelper h) {
        for (String[] entry : new String[][]{
                {"cat_butter_cube","cat_accessory.laowu.boss_drop"},
                {"cat_giant_collar","cat_accessory.laowu.giant_boss_drop"}}) {
            var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("laowu",entry[0])));
            var lines=new java.util.ArrayList<net.minecraft.network.chat.Component>();
            lines.add(stack.getHoverName());
            cn.laowu.mod.accessory.CatAccessoryTooltip.append(stack,lines);
            h.assertTrue(lines.size()>=4,"Trophy has source, effect and accessory labels: "+entry[0]);
            h.assertTrue(lines.get(1).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                            && text.getKey().equals(entry[1])
                            && lines.get(1).getStyle().getColor().equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.DARK_PURPLE)),
                    "Correct boss source appears first below the name in the shared style: "+entry[0]);
            h.assertTrue(hasKey(lines,"cat_accessory.laowu.label",net.minecraft.ChatFormatting.GOLD),
                    "Both trophies retain the shared gold accessory marker");
        }
        h.succeed();
    }

    @GameTest(template="accessory_probe",batch="giant_boss_treat_recipe")
    public static void loadedMixingRecipeConsumesPotatoAnyMushroomAndExactlyFiveHundredGas(GameTestHelper h) {
        var loaded=h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("laowu","giant_cat_treat_mixing"));
        h.assertTrue(loaded.isPresent(),"Giant treat mixing recipe must load through the real recipe manager");
        var recipe=loaded.get();
        h.assertTrue(recipe.getType()==com.simibubi.create.AllRecipeTypes.MIXING.getType(),
                "Recipe registers in the Create mixing type for its machines and JEI");
        for (var mushroom : new net.minecraft.world.item.Item[]{Items.RED_MUSHROOM,Items.BROWN_MUSHROOM}) {
            var pos=new BlockPos(2,2,2);
            h.setBlock(pos,com.simibubi.create.AllBlocks.BASIN.get());
            var basin=(com.simibubi.create.content.processing.basin.BasinBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
            basin.getInputInventory().setStackInSlot(0,new ItemStack(Items.POISONOUS_POTATO,2));
            basin.getInputInventory().setStackInSlot(1,new ItemStack(mushroom,2));
            var tank=basin.inputTank.getPrimaryHandler();
            var gas=BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("laowu","hissing_gas"));
            tank.fill(new net.minecraftforge.fluids.FluidStack(gas,499),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            h.assertTrue(!com.simibubi.create.content.processing.basin.BasinRecipe.match(basin,recipe),
                    "499 mB cannot satisfy the summon recipe");
            tank.fill(new net.minecraftforge.fluids.FluidStack(gas,501),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            h.assertTrue(com.simibubi.create.content.processing.basin.BasinRecipe.match(basin,recipe),
                    "Both red and brown mushrooms match without heating");
            h.assertTrue(com.simibubi.create.content.processing.basin.BasinRecipe.apply(basin,recipe),
                    "Actual basin execution accepts the recipe");
            h.assertTrue(basin.getInputInventory().getStackInSlot(0).getCount()==1
                            &&basin.getInputInventory().getStackInSlot(1).getCount()==1
                            &&tank.getFluidInTank(0).getAmount()==500,
                    "One real execution consumes exactly one potato, one mushroom and 500 mB gas");
            int outputs=0;
            var treat=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("laowu","giant_cat_treat"));
            for(int slot=0;slot<basin.getOutputInventory().getSlots();slot++){
                var output=basin.getOutputInventory().getStackInSlot(slot);
                h.assertTrue(output.isEmpty()||output.is(treat),"No unrelated mixing output");
                if(output.is(treat))outputs+=output.getCount();
            }
            h.assertTrue(outputs==1,"One basin execution produces exactly one giant treat");
            h.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR);
        }
        h.succeed();
    }

    private static boolean hasKey(java.util.List<net.minecraft.network.chat.Component> lines,String key,net.minecraft.ChatFormatting color) {
        return lines.stream().anyMatch(line->line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                &&text.getKey().equals(key)
                &&net.minecraft.network.chat.TextColor.fromLegacyFormat(color).equals(line.getStyle().getColor()));
    }
    @GameTest(template="accessory_probe",timeoutTicks=100,batch="giant_boss_summon")
    public static void treatConvertsWildCatAndCompletesSummoning(GameTestHelper h) {
        var item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("laowu","giant_cat_treat"));
        h.assertTrue(item!=Items.AIR,"Giant cat treat must be registered and usable");
        var cat=h.spawn(EntityType.CAT,new BlockPos(2,1,2));cat.setTame(false);cat.setNoAi(true);
        var genome=CatGenomeData.getOrFallback(cat);
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"giant-boss-test"));
        player.getAbilities().instabuild=false;
        var stack=new ItemStack(item,2);
        var result=item.interactLivingEntity(stack,player,cat,InteractionHand.MAIN_HAND);
        h.assertTrue(result.consumesAction() && stack.getCount()==1 && cat.isRemoved(),
                "Wild cat becomes exactly one boss and survival uses exactly one treat");
        var list=h.getLevel().getEntitiesOfClass(Monster.class,cat.getBoundingBox().inflate(3),
                e->BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().equals("giant_cat_boss"));
        h.assertTrue(list.size()==1,"Conversion creates the registered giant boss");
        var boss=list.get(0);
        h.assertTrue(boss.getMaxHealth()==800 && boss.getHealth()==800,"Summoned boss starts at its full 800 combat health");
        h.assertTrue(Math.abs(boss.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)-.33D)<.000001D,
                "Summoned boss uses .33 ordinary pursuit speed");
        h.assertTrue(Math.abs(boss.getBbWidth()-2)<.001 && Math.abs(boss.getBbHeight()-3)<.001,
                "Boss uses health100 giant dimensions, independent of800 HP");
        h.assertTrue(((cn.laowu.mod.entity.GiantCatBoss)boss).isSummoning()
                        && !boss.doHurtTarget(player),"Summoning blocks attacks");
        h.assertTrue(((cn.laowu.mod.entity.GiantCatBoss)boss).getInheritedGenome().orElseThrow().equals(genome),
                "Conversion preserves the exact original coat genome");
        Vec3 before=boss.getDeltaMovement();boss.knockback(1,1,0);
        h.assertTrue(boss.getDeltaMovement().equals(before),"Boss ignores ordinary knockback");
        h.runAfterDelay(65,()->{
            h.assertTrue(!((cn.laowu.mod.entity.GiantCatBoss)boss).isSummoning() && boss.isAlive(),
                    "Summoning completes and enables real combat AI");
            boss.discard();player.discard();h.succeed();
        });
    }
    @GameTest(template="accessory_probe",batch="giant_boss_tame")
    public static void treatNeverConvertsTamedCat(GameTestHelper h) {
        var item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("laowu","giant_cat_treat"));
        h.assertTrue(item!=Items.AIR,"Giant treat exists");
        var cat=h.spawn(EntityType.CAT,new BlockPos(2,1,2));cat.setTame(true);cat.setNoAi(true);
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"giant-boss-tame"));
        var stack=new ItemStack(item,2);
        var result=item.interactLivingEntity(stack,player,cat,InteractionHand.MAIN_HAND);
        h.assertTrue(!result.consumesAction() && stack.getCount()==2 && !cat.isRemoved(),
                "Owned cats remain safe and the item is not consumed");
        cat.discard();player.discard();h.succeed();
    }
}
