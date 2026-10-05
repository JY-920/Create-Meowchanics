package cn.laowu.mod.test;

import cn.laowu.mod.create.*;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.deployer.DeployerBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import net.minecraftforge.common.util.FakePlayerFactory;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CatMachineCraftingProbe {
    // Missing cat recipes, original casing accepted, or incorrect basin quantities must fail here.
    @GameTest(template="accessory_probe",batch="cat_machine_crafting",timeoutTicks=30)
    public static void catMaterialsCraftFourMachines(GameTestHelper h) {
        craft(h,"cat_press",new String[]{"create:shaft","laowu:cat_casing","minecraft:iron_block"});
        craft(h,"cat_mixer",new String[]{"create:cogwheel","laowu:cat_casing","create:whisk"});
        craft(h,"cat_depot",new String[]{"create:andesite_alloy","laowu:cat_casing"});
        craft(h,"haji_basin",new String[]{"laowu:cat_ingot","","laowu:cat_ingot","create:andesite_alloy","create:andesite_alloy","create:andesite_alloy"});
        h.succeed();
    }

    private static void craft(GameTestHelper h,String output,String[] ingredients) {
        var stacks=net.minecraft.core.NonNullList.withSize(9,ItemStack.EMPTY);
        boolean vertical=output.equals("cat_press")||output.equals("cat_mixer");
        for(int i=0;i<ingredients.length;i++) if(!ingredients[i].isEmpty())
            stacks.set(vertical?i*3:i,new ItemStack(item(ingredients[i])));
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,-1) {
            public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}
            public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}
        };
        var input=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
        for(int i=0;i<9;i++)input.setItem(i,stacks.get(i));
        var found=h.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,h.getLevel());
        h.assertTrue(found.isPresent(),"Missing craft for "+output);
        var result=found.get().assemble(input,h.getLevel().registryAccess());
        h.assertTrue(result.is(item("laowu:"+output))&&result.getCount()==1,"Wrong crafted output "+output+": "+result);
        if(!output.equals("haji_basin")) {
            int casing=vertical?3:1;
            stacks.set(casing,new ItemStack(AllBlocks.ANDESITE_CASING.get()));
            var wrongInput=input;
            for(int i=0;i<9;i++)wrongInput.setItem(i,stacks.get(i));
            var original=h.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,wrongInput,h.getLevel());
            h.assertTrue(original.isEmpty()||!original.get().assemble(wrongInput,h.getLevel().registryAccess()).is(item("laowu:"+output)),"Ordinary casing crafted cat machine "+output);
        }
    }

    // Exercise loader events through the same game mode interaction used by players, not a direct recipe invocation.
    @GameTest(template="artillery_probe",batch="cat_machine_manual",timeoutTicks=40)
    public static void cardboardAcceptsCatIngotAndStrippedWoodIsRejected(GameTestHelper h) {
        Block[] woods={Blocks.STRIPPED_OAK_LOG,Blocks.STRIPPED_SPRUCE_LOG,Blocks.STRIPPED_BIRCH_LOG,
            Blocks.STRIPPED_JUNGLE_LOG,Blocks.STRIPPED_ACACIA_LOG,Blocks.STRIPPED_DARK_OAK_LOG,
            Blocks.STRIPPED_MANGROVE_LOG,Blocks.STRIPPED_CHERRY_LOG,Blocks.STRIPPED_CRIMSON_STEM,Blocks.STRIPPED_WARPED_STEM,
            Blocks.STRIPPED_OAK_WOOD,Blocks.STRIPPED_SPRUCE_WOOD,Blocks.STRIPPED_BIRCH_WOOD,
            Blocks.STRIPPED_JUNGLE_WOOD,Blocks.STRIPPED_ACACIA_WOOD,Blocks.STRIPPED_DARK_OAK_WOOD,
            Blocks.STRIPPED_MANGROVE_WOOD,Blocks.STRIPPED_CHERRY_WOOD,Blocks.STRIPPED_CRIMSON_HYPHAE,Blocks.STRIPPED_WARPED_HYPHAE};
        var player=FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"cat-casing-use"));
        var pos=new BlockPos(5,5,5);
        for(var wood:new Block[]{AllBlocks.CARDBOARD_BLOCK.get()})for(var hand:InteractionHand.values())for(boolean creative:new boolean[]{false,true}) {
            player.setGameMode(creative?GameType.CREATIVE:GameType.SURVIVAL);
            h.setBlock(pos,wood.defaultBlockState());
            player.setItemInHand(hand,new ItemStack(item("laowu:cat_ingot"),3));
            player.gameMode.useItemOn(player,h.getLevel(),player.getItemInHand(hand),hand,hit(h.absolutePos(pos),Direction.UP));
            h.assertTrue(h.getBlockState(pos).is(CatMachineBlocks.CAT_CASING.get()),"Hand application failed "+wood+"/"+hand+"/"+creative);
            h.assertTrue(player.getItemInHand(hand).getCount()==(creative?3:2),"Casing hand use consumed wrong amount");
        }
        player.setGameMode(GameType.SURVIVAL);
        for(var wood:woods) {
            h.setBlock(pos,wood);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item("laowu:cat_ingot"),3));
            player.gameMode.useItemOn(player,h.getLevel(),player.getMainHandItem(),InteractionHand.MAIN_HAND,hit(h.absolutePos(pos),Direction.UP));
            h.assertTrue(h.getBlockState(pos).is(wood)&&player.getMainHandItem().getCount()==3,"Stripped wood must no longer make cat casing: "+wood);
        }
        h.setBlock(pos,Blocks.OAK_LOG);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item("laowu:cat_ingot"),3));
        player.gameMode.useItemOn(player,h.getLevel(),player.getMainHandItem(),InteractionHand.MAIN_HAND,hit(h.absolutePos(pos),Direction.UP));
        h.assertTrue(h.getBlockState(pos).is(Blocks.OAK_LOG)&&player.getMainHandItem().getCount()==3,"Unstripped wood must not turn into casing");
        h.succeed();
    }

    // Use actual block-first dispatch: normal depot insertion and crouching
    // processor placement must remain distinct, like Create.
    @GameTest(template="artillery_probe",batch="cat_machine_use_modes",timeoutTicks=40)
    public static void normalDepotInsertionAndSneakPlacementRemainDistinct(GameTestHelper h) {
        var player=FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"cat-machine-use"));
        player.setGameMode(GameType.SURVIVAL);
        for(Direction bottom:Direction.values())for(boolean mixer:new boolean[]{false,true})
        for(boolean depot:new boolean[]{false,true})for(boolean sneak:new boolean[]{false,true}) {
            var base=new BlockPos(5,5,5);
            var top=bottom.getOpposite();
            var destination=base.relative(top,2);
            var machine=mixer?CatMachineBlocks.CAT_MIXER.get():CatMachineBlocks.CAT_PRESS.get();
            h.setBlock(base.relative(top),Blocks.AIR);
            h.setBlock(destination,Blocks.AIR);
            h.setBlock(base,(depot?CatDepotRegistration.CAT_DEPOT.get():CatMachineBlocks.HAJI_BASIN.get()).defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom));
            player.setShiftKeyDown(sneak);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(machine,3));
            player.gameMode.useItemOn(player,h.getLevel(),player.getMainHandItem(),InteractionHand.MAIN_HAND,hit(h.absolutePos(base),top));
            if(depot&&!sneak){
                var held=((DepotBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(base))).getHeldItem();
                h.assertTrue(held.is(machine.asItem())&&held.getCount()==3&&player.getMainHandItem().isEmpty()
                        &&h.getBlockState(destination).isAir(),"Normal depot interaction must store machines before placement");
                ((DepotBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(base))).setHeldItem(ItemStack.EMPTY);
                h.setBlock(base,Blocks.AIR);
                continue;
            }
            h.assertTrue(h.getBlockState(destination).is(machine)&&h.getBlockState(base.relative(top)).isAir(),"Processor must skip one cell "+bottom+"/"+sneak+"/"+depot);
            h.assertTrue(CatMachineOrientation.bottom(h.getBlockState(destination))==bottom&&player.getMainHandItem().getCount()==2,"Processor facing/consumption wrong");
            if(depot)h.assertTrue(((DepotBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(base))).getHeldItem().isEmpty(),
                    "Processor must not be stored on the cat depot");
            h.setBlock(destination,Blocks.AIR);
            h.setBlock(base,Blocks.AIR);
        }
        player.setShiftKeyDown(false);
        h.succeed();
    }


    // Removing a tool tag must break actual survival harvest, not just a source-text assertion.
    @GameTest(template="artillery_probe",batch="cat_machine_wood_harvest",timeoutTicks=40)
    public static void woodenAxeAndPickaxeHarvestCatMachines(GameTestHelper h) {
        var player=FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"cat-wood-tools"));
        player.setGameMode(GameType.SURVIVAL);
        var position=h.absolutePos(new BlockPos(5,3,5));
        player.setPos(Vec3.atCenterOf(position).add(0,0,2));
        Block[] both={CatMachineBlocks.CAT_MIXER.get(),CatMachineBlocks.CAT_PRESS.get(),
            CatDepotRegistration.CAT_DEPOT.get(),CatMachineBlocks.CAT_CASING.get()};
        for(var block:both)for(var tool:new Item[]{Items.WOODEN_AXE,Items.WOODEN_PICKAXE})
            harvest(h,player,position,block,tool,true);
        harvest(h,player,position,CatMachineBlocks.HAJI_BASIN.get(),Items.WOODEN_PICKAXE,true);
        harvest(h,player,position,CatMachineBlocks.HAJI_BASIN.get(),Items.WOODEN_AXE,false);
        h.succeed();
    }

    private static void harvest(GameTestHelper h,net.minecraft.server.level.ServerPlayer player,BlockPos position,Block block,Item tool,boolean drops) {
        var area=new net.minecraft.world.phys.AABB(position).inflate(3);
        h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area).forEach(net.minecraft.world.entity.Entity::discard);
        h.getLevel().setBlockAndUpdate(position,block.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(tool));
        var state=h.getLevel().getBlockState(position);
        h.assertTrue(player.hasCorrectToolForDrops(state)==drops,"Wrong harvest suitability "+block+"/"+tool);
        if(drops)h.assertTrue(player.getMainHandItem().getDestroySpeed(state)>1,"Wood tool must also mine efficiently "+block+"/"+tool);
        h.assertTrue(player.gameMode.destroyBlock(position),"Actual survival break failed "+block+"/"+tool);
        var loot=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area);
        int count=loot.stream().filter(e->e.getItem().is(block.asItem())).mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(count==(drops?1:0),"Wrong survival drop "+block+"/"+tool+": "+count);
    }

    // Physical world hand and real depot processing must both resolve item_application, without duplicate deployer recipes.
    @GameTest(template="artillery_probe",batch="cat_machine_deployer",timeoutTicks=240)
    public static void poweredDeployerAppliesCatIngotToCardboardAndDepot(GameTestHelper h) {
        var ingot=item("laowu:cat_ingot");
        Block[] woods={AllBlocks.CARDBOARD_BLOCK.get(),Blocks.STRIPPED_OAK_LOG};
        for(int kind=0;kind<4;kind++) {
            var pos=new BlockPos(5+kind*12,3,5);
            boolean onDepot=kind>=2;
            h.setBlock(pos,onDepot?CatDepotRegistration.CAT_DEPOT.get():woods[kind]);
            h.setBlock(pos.above(2),AllBlocks.DEPLOYER.getDefaultState().setValue(DeployerBlock.FACING,Direction.DOWN)
                .setValue(DeployerBlock.AXIS_ALONG_FIRST_COORDINATE,true));
            h.setBlock(pos.above(2).west(),AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING,Direction.EAST));
            ((CreativeMotorBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos.above(2).west()))).generatedSpeed.setValue(256);
            final int variant=kind;
            h.runAfterDelay(10,()->{
                var deployer=(DeployerBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos.above(2)));
                h.assertTrue(Math.abs(deployer.getSpeed())==256,"Deployer has no real power");
                deployer.getPlayer().setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ingot,2));
                if(onDepot)((DepotBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos))).setHeldItem(new ItemStack(woods[variant-2]));
            });
            h.runAfterDelay(190,()->{
                if(variant%2==1) {
                    if(onDepot) {
                        var held=((DepotBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos))).getHeldItem();
                        h.assertTrue(held.is(Blocks.STRIPPED_OAK_LOG.asItem())&&held.getCount()==1,"Old wood recipe still accepted on depot");
                    } else h.assertTrue(h.getBlockState(pos).is(Blocks.STRIPPED_OAK_LOG),"Old wood recipe still accepted in world");
                    var remaining=((DeployerBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos.above(2)))).getPlayer().getMainHandItem();
                    h.assertTrue(remaining.is(ingot)&&remaining.getCount()==2,"Rejected wood consumed ingot");
                    return;
                }
                if(onDepot) {
                    var held=((DepotBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos))).getHeldItem();
                    h.assertTrue(held.is(CatMachineBlocks.CAT_CASING_ITEM.get())&&held.getCount()==1,"Deployer failed to make casing on depot "+variant+": "+held);
                } else h.assertTrue(h.getBlockState(pos).is(CatMachineBlocks.CAT_CASING.get()),"Deployer failed world application "+variant);
                var remaining=((DeployerBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos.above(2)))).getPlayer().getMainHandItem();
                h.assertTrue(remaining.is(ingot)&&remaining.getCount()==1,"Deployer consumed wrong amount "+variant+": "+remaining);
            });
        }
        h.runAfterDelay(210,h::succeed);
    }

    private static Item item(String id) {return BuiltInRegistries.ITEM.get(new ResourceLocation(id));}
    private static BlockHitResult hit(BlockPos pos,Direction face) {return new BlockHitResult(Vec3.atCenterOf(pos),face,pos,false);}
}
