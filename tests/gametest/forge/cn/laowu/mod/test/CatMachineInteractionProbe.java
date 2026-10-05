package cn.laowu.mod.test;

import cn.laowu.mod.create.*;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CatMachineInteractionProbe {
    @GameTest(template="artillery_probe",batch="cat_reverse_placement",timeoutTicks=40)
    public static void placingWorkSurfaceOnProcessorLeavesOneCellGap(GameTestHelper h) {
        var player=player(h);var base=new BlockPos(6,6,6);
        for(Direction bottom:Direction.values())for(var processor:processors())for(var surface:surfaces())
        for(boolean crouch:new boolean[]{false,true})for(boolean creative:new boolean[]{false,true}) {
            clear(h,base);
            h.setBlock(base,processor.defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom));
            player.setShiftKeyDown(crouch);
            player.setGameMode(creative?GameType.CREATIVE:GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(surface,3));
            click(h,player,base,bottom);
            var destination=base.relative(bottom,2);
            h.assertTrue(h.getBlockState(destination).is(surface)&&h.getBlockState(base.relative(bottom)).isAir(),
                    "Work surface must skip one cell from processor working face: "+bottom+"/"+surface+"/"+crouch);
            h.assertTrue(h.getBlockState(destination).getValue(CatMachineOrientation.BOTTOM)==bottom,
                    "Reverse placement must align opening toward processor");
            h.assertTrue(player.getMainHandItem().getCount()==(creative?3:2),"Reverse placement consumed wrong amount");
            clear(h,base);
        }
        h.succeed();
    }

    @GameTest(template="artillery_probe",batch="cat_reverse_guards",timeoutTicks=40)
    public static void reversePlacementPreservesObstructionsAndOrdinaryFaces(GameTestHelper h) {
        var player=player(h);var base=new BlockPos(6,6,6);
        for(Direction bottom:Direction.values())for(var processor:processors())for(var surface:surfaces()) {
            for(boolean blockedGap:new boolean[]{false,true}) {
                clear(h,base);
                h.setBlock(base,processor.defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom));
                var obstacle=base.relative(bottom,blockedGap?1:2);
                h.setBlock(obstacle,Blocks.STONE);player.setShiftKeyDown(true);
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(surface,3));
                click(h,player,base,bottom);
                h.assertTrue(h.getBlockState(obstacle).is(Blocks.STONE)&&player.getMainHandItem().getCount()==3,
                        "Blocked reverse placement must neither replace nor consume");
                h.assertTrue(h.getBlockState(base.relative(bottom,3)).isAir()
                        &&h.getBlockState(base.relative(bottom,blockedGap?2:1)).isAir(),
                        "Blocked reverse placement must not fall through to another cell");
            }
            clear(h,base);
            h.setBlock(base,processor.defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom));
            player.setShiftKeyDown(false);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(surface,3));
            click(h,player,base,bottom.getOpposite());
            h.assertTrue(h.getBlockState(base.relative(bottom.getOpposite())).is(surface)
                    &&h.getBlockState(base.relative(bottom.getOpposite(),2)).isAir()
                    &&player.getMainHandItem().getCount()==2,"Non-working face must retain ordinary adjacent placement");
        }
        clear(h,base);h.succeed();
    }

    @GameTest(template="artillery_probe",batch="cat_reverse_replaceable",timeoutTicks=40)
    public static void clickingReplaceableCellDoesNotPretendToClickProcessor(GameTestHelper h) {
        var player=player(h);var base=new BlockPos(6,6,6);var gap=base.east();
        for(var processor:processors())for(var surface:surfaces()) {
            clear(h,base);
            h.setBlock(base,processor.defaultBlockState().setValue(CatMachineOrientation.BOTTOM,Direction.EAST));
            h.setBlock(gap.below(),Blocks.STONE);h.setBlock(gap,Blocks.SNOW);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(surface,3));
            click(h,player,gap,Direction.EAST);
            h.assertTrue(h.getBlockState(gap).is(surface)&&h.getBlockState(base.east(2)).isAir()
                    &&player.getMainHandItem().getCount()==2,
                    "Clicking replaceable snow must replace that cell, not trigger a neighbouring processor shortcut");
            clear(h,base);h.setBlock(gap.below(),Blocks.AIR);
        }
        h.succeed();
    }

    @GameTest(template="artillery_probe",batch="cat_surface_wrench",timeoutTicks=40)
    public static void nonWorkingFacesRotateContainersWithoutLosingContents(GameTestHelper h) {
        Direction[][] turns={{Direction.SOUTH,Direction.NORTH,Direction.DOWN,Direction.UP,Direction.WEST,Direction.EAST},
                {Direction.DOWN,Direction.UP,Direction.EAST,Direction.WEST,Direction.NORTH,Direction.SOUTH},
                {Direction.WEST,Direction.EAST,Direction.NORTH,Direction.SOUTH,Direction.UP,Direction.DOWN}};
        Direction[] backTurns={Direction.SOUTH,Direction.NORTH,Direction.DOWN,Direction.UP,Direction.UP,Direction.DOWN};
        var player=player(h);var base=new BlockPos(6,6,6);
        player.setItemInHand(InteractionHand.MAIN_HAND,AllItems.WRENCH.asStack());
        for(var surface:surfaces())for(Direction bottom:Direction.values())for(Direction face:Direction.values()) {
            if(face==bottom.getOpposite())continue;
            clear(h,base);
            var state=surface.defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom);
            if(surface instanceof BasinBlock)state=state.setValue(BasinBlock.FACING,Direction.SOUTH);
            h.setBlock(base,state);
            var original=h.getLevel().getBlockEntity(h.absolutePos(base));
            if(original instanceof DepotBlockEntity depot)depot.setHeldItem(new ItemStack(Items.DIAMOND,7));
            else ((HajiBasinBlockEntity)original).inputInventory.setStackInSlot(0,new ItemStack(Items.DIAMOND,7));
            click(h,player,base,face);
            var expected=face==bottom?backTurns[bottom.ordinal()]:turns[face.getAxis().ordinal()][bottom.ordinal()];
            h.assertTrue(h.getBlockState(base).getValue(CatMachineOrientation.BOTTOM)==expected,
                    "Non-top wrench must rotate the working direction: "+surface+"/"+bottom+"/"+face
                    +" expected="+expected+" actual="+h.getBlockState(base)+" hand="+player.getMainHandItem()
                    +" direct="+((CatWorkSurfaceWrench)surface).getRotatedBlockState(state,face));
            h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(base))==original,"Rotation must retain the block entity");
            var held=original instanceof DepotBlockEntity depot?depot.getHeldItem():((HajiBasinBlockEntity)original).inputInventory.getStackInSlot(0);
            h.assertTrue(held.is(Items.DIAMOND)&&held.getCount()==7,"Container rotation lost its contents");
            if(surface instanceof BasinBlock)h.assertTrue(h.getBlockState(base).getValue(BasinBlock.FACING)==Direction.SOUTH,
                    "Rotating basin body must preserve its local outlet selection");
        }
        clear(h,base);h.succeed();
    }

    @GameTest(template="artillery_probe",batch="cat_surface_dismantle",timeoutTicks=40)
    public static void crouchingWrenchStillDismantlesContainers(GameTestHelper h) {
        var player=player(h);var base=new BlockPos(6,6,6);
        player.setShiftKeyDown(true);player.setItemInHand(InteractionHand.MAIN_HAND,AllItems.WRENCH.asStack());
        for(var surface:surfaces())for(Direction bottom:Direction.values()) {
            clear(h,base);
            h.setBlock(base,surface.defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom));
            int before=player.getInventory().countItem(surface.asItem());
            click(h,player,base,bottom);
            h.assertTrue(h.getBlockState(base).isAir()&&player.getInventory().countItem(surface.asItem())==before+1,
                    "Sneak wrench must dismantle and return exactly one container");
        }
        h.succeed();
    }

    private static ServerPlayer player(GameTestHelper h) {
        // Create treats every FakePlayer side click as hitting its filter slot,
        // even when the ray misses. Use the actual player path with a test connection.
        var link=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"machine-link"));
        var player=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),
                new GameProfile(UUID.randomUUID(),"machine-interaction"));
        player.connection=link.connection;
        player.setGameMode(GameType.SURVIVAL);player.setShiftKeyDown(false);return player;
    }
    private static Block[] processors(){return new Block[]{CatMachineBlocks.CAT_PRESS.get(),CatMachineBlocks.CAT_MIXER.get()};}
    private static Block[] surfaces(){return new Block[]{CatMachineBlocks.HAJI_BASIN.get(),CatDepotRegistration.CAT_DEPOT.get()};}
    private static void click(GameTestHelper h,ServerPlayer player,BlockPos pos,Direction face) {
        var absolute=h.absolutePos(pos);
        // Hit the face near its rim, outside the basin's existing filter control.
        Vec3 tangent=switch(face.getAxis()) {
            case X -> new Vec3(0,.375,.375);
            case Y -> new Vec3(.375,0,.375);
            case Z -> new Vec3(.375,.375,0);
        };
        Vec3 location=Vec3.atCenterOf(absolute).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)).add(tangent);
        player.gameMode.useItemOn(player,h.getLevel(),player.getMainHandItem(),InteractionHand.MAIN_HAND,
                new BlockHitResult(location,face,absolute,false));
    }
    private static void clear(GameTestHelper h,BlockPos base) {
        clearCell(h,base);
        for(Direction face:Direction.values())for(int distance=1;distance<=3;distance++)clearCell(h,base.relative(face,distance));
    }
    private static void clearCell(GameTestHelper h,BlockPos pos) {
        var be=h.getLevel().getBlockEntity(h.absolutePos(pos));
        if(be instanceof DepotBlockEntity depot)depot.setHeldItem(ItemStack.EMPTY);
        if(be instanceof HajiBasinBlockEntity basin)for(int i=0;i<basin.inputInventory.getSlots();i++)basin.inputInventory.setStackInSlot(i,ItemStack.EMPTY);
        h.setBlock(pos,Blocks.AIR);
    }
}
