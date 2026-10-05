package cn.laowu.mod.test;

import cn.laowu.mod.create.*;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CatAutoLaserProbe {
    private static final ResourceLocation FILTER_ID=ResourceLocation.fromNamespaceAndPath("laowu","creature_filter");

    @GameTest(template="artillery_probe",batch="auto_laser_filter_slot",timeoutTicks=1100)
    public static void filterSlotRetainsExactlyOneItemInEveryOrientation(GameTestHelper h) {
        CatAutoLaserTestArea.ready(h,()->{
            var item=BuiltInRegistries.ITEM.get(FILTER_ID);
            h.assertTrue(item!=net.minecraft.world.item.Items.AIR,"Creature filter is not registered");
            for(Direction bottom:Direction.values()) {
                var p=new BlockPos(8+bottom.ordinal()*8,5,6);
                h.setBlock(p,machine(h).defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom));
                var be=(CatAutoLaserBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p));
                var input=new net.minecraft.world.item.ItemStack(item,3);
                try {
                    var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"filter-install"));
                    player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                    player.setPos(Vec3.atCenterOf(h.absolutePos(p)).add(2,0,0));
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,input);
                    Vec3 local=CatMachineOrientation.position(be.getBlockState(),new Vec3(.5,3d/16,0));
                    var hit=new BlockHitResult(local.add(Vec3.atLowerCornerOf(h.absolutePos(p))),CatMachineOrientation.toWorld(bottom,Direction.NORTH),h.absolutePos(p),false);
                    player.gameMode.useItemOn(player,h.getLevel(),input,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
                    h.assertTrue(input.getCount()==2,"Rear slot did not consume exactly one filter");
                    var saved=(net.minecraft.world.item.ItemStack)be.getClass().getMethod("getFilter").invoke(be);
                    h.assertTrue(saved.getCount()==1,"Installed filter must retain exactly one item");
                    saved.shrink(1);
                    h.assertTrue(!((net.minecraft.world.item.ItemStack)be.getClass().getMethod("getFilter").invoke(be)).isEmpty(),"Caller mutated installed filter");
                    var snapshot=be.getUpdateTag(h.getLevel().registryAccess());
                    var copy=new CatAutoLaserBlockEntity(h.absolutePos(p),be.getBlockState());
                    copy.loadWithComponents(snapshot,h.getLevel().registryAccess());
                    h.assertTrue(((net.minecraft.world.item.ItemStack)copy.getClass().getMethod("getFilter").invoke(copy)).is(item),"Filter lost on reload");
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
                    player.gameMode.useItemOn(player,h.getLevel(),player.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,hit);
                    h.assertTrue(player.getMainHandItem().is(item)&&player.getMainHandItem().getCount()==1&&!be.hasFilter(),"Empty hand did not retrieve exactly one filter");
                    player.gameMode.useItemOn(player,h.getLevel(),player.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,hit);
                    h.assertTrue(be.hasFilter()&&player.getMainHandItem().isEmpty(),"Reinstall failed");
                    h.setBlock(p,Blocks.AIR);
                    var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(h.absolutePos(p)).inflate(2));
                    h.assertTrue(drops.stream().filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum()==1,"Breaking marker lost or duplicated installed filter");
                    drops.forEach(net.minecraft.world.entity.Entity::discard);
                } catch(ReflectiveOperationException ex) {throw new AssertionError("Missing persistent marker filter slot",ex);}
            }
            h.succeed();
        });
    }

    private static Block machine(GameTestHelper h) {
        Block block=BuiltInRegistries.BLOCK.get(ID);
        h.assertTrue(block!=Blocks.AIR,"Automatic laser workstation is not registered");
        return block;
    }
    @GameTest(template="artillery_probe",batch="auto_laser_kinetics",timeoutTicks=200)
    public static void bottomOnlyReceivesRealKineticsInSixDirections(GameTestHelper h) {
        CatAutoLaserTestArea.ready(h,()->runbottomOnlyReceivesRealKineticsInSixDirections(h));
    }
    private static void runbottomOnlyReceivesRealKineticsInSixDirections(GameTestHelper h) {
        Block block=machine(h);
        for(Direction bottom:Direction.values()) {
            BlockPos p=new BlockPos(8+bottom.ordinal()*8,5,6);
            h.setBlock(p,block.defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom));
            var state=h.getBlockState(p);
            var kinetic=(com.simibubi.create.content.kinetics.base.IRotate)block;
            h.assertTrue(kinetic.getRotationAxis(state)==bottom.getAxis(),"Wrong bottom shaft axis");
            for(Direction side:Direction.values())
                h.assertTrue(kinetic.hasShaftTowards(h.getLevel(),h.absolutePos(p),state,side)==(side==bottom),"Shaft accepted non-bottom "+bottom+"/"+side);
            h.setBlock(p.relative(bottom),AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING,bottom.getOpposite()));
            ((CreativeMotorBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p.relative(bottom)))).generatedSpeed.setValue(16);
            h.runAfterDelay(35,()->{
                var be=(KineticBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p));
                h.assertTrue(Math.abs(be.getSpeed())==16,"Actual kinetic network did not reach "+bottom);
                h.assertTrue(extension(be)>.99f,"Machine did not unfold in 25 ticks");
                h.assertTrue(Math.abs(be.calculateStressApplied()-4)<.001,"Wrong stress consumption");
            });
        }
        h.runAfterDelay(40,h::succeed);
    }
    @GameTest(template="artillery_probe",batch="auto_laser_power",timeoutTicks=200)
    public static void powerTransitionsAreContinuousAndOverstressStopsWork(GameTestHelper h) {
        CatAutoLaserTestArea.ready(h,()->runpowerTransitionsAreContinuousAndOverstressStopsWork(h));
    }
    private static void runpowerTransitionsAreContinuousAndOverstressStopsWork(GameTestHelper h) {
        var p=new BlockPos(8,5,6);
        h.setBlock(p,machine(h));
        var be=(KineticBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p));
        h.setBlock(p.below(),AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING,Direction.UP));
        var motor=(CreativeMotorBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p.below()));
        motor.generatedSpeed.setValue(-16);
        float[] partial={0};
        h.runAfterDelay(15,()->{
            partial[0]=extension(be);
            h.assertTrue(partial[0]>.2f&&partial[0]<.8f,"Negative speed failed to unfold continuously: "+partial[0]);
            motor.generatedSpeed.setValue(0);
        });
        h.runAfterDelay(17,()->{
            h.assertTrue(Math.abs(extension(be)-partial[0])<=.121,"Power reversal snapped animation");
            h.assertTrue(!working(be),"Stopped machine still working");
            motor.generatedSpeed.setValue(16);
        });
        h.runAfterDelay(50,()->{
            h.assertTrue(working(be),"Fully unfolded machine not working");
            be.updateFromNetwork(0,1000,1);be.tick();
            h.assertTrue(!working(be),"Overstressed machine still working");
            h.succeed();
        });
    }
    private static float extension(KineticBlockEntity be) {
        try {return ((Number)be.getClass().getMethod("getExtension",float.class).invoke(be,1f)).floatValue();}
        catch(ReflectiveOperationException e){throw new AssertionError("Missing extension state",e);}
    }
    @GameTest(template="artillery_probe",batch="auto_laser_wrench",timeoutTicks=150)
    public static void wrenchRotatesAndSneakDismantles(GameTestHelper h) {
        CatAutoLaserTestArea.ready(h,()->runwrenchRotatesAndSneakDismantles(h));
    }
    private static void runwrenchRotatesAndSneakDismantles(GameTestHelper h) {
        Block block=machine(h);var p=new BlockPos(8,5,6);h.setBlock(p,block);
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"auto-laser-wrench"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(h.absolutePos(p)).add(2,0,0));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,com.simibubi.create.AllItems.WRENCH.asStack());
        var hit=new BlockHitResult(Vec3.atCenterOf(h.absolutePos(p)),Direction.EAST,h.absolutePos(p),false);
        player.gameMode.useItemOn(player,h.getLevel(),player.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        h.assertTrue(CatMachineOrientation.bottom(h.getBlockState(p))!=Direction.DOWN,"Wrench failed to rotate working face");
        player.setShiftKeyDown(true);
        player.gameMode.useItemOn(player,h.getLevel(),player.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        h.assertTrue(h.getBlockState(p).isAir(),"Sneak wrench failed to dismantle");
        int count=0;
        for(var stack:player.getInventory().items)if(stack.is(block.asItem()))count+=stack.getCount();
        for(var e:h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(h.absolutePos(p)).inflate(3)))
            if(e.getItem().is(block.asItem()))count+=e.getItem().getCount();
        h.assertTrue(count==1,"Wrench must return exactly one workstation");
        player.setShiftKeyDown(false);h.succeed();
    }
    private static boolean working(KineticBlockEntity be) {
        try {return (boolean)be.getClass().getMethod("isWorking").invoke(be);}
        catch(ReflectiveOperationException e){throw new AssertionError("Missing authoritative working state",e);}
    }
    private static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath("laowu","cat_auto_laser");
}
