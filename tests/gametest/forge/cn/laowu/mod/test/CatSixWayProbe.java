package cn.laowu.mod.test;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraftforge.gametest.*;
@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CatSixWayProbe {
    @GameTest(template="artillery_probe",batch="cat_work_face_dispatch",timeoutTicks=40)
    public static void normalDepotInsertionPrecedesSneakProcessorPlacement(GameTestHelper h){
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"work-face-dispatch"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var base=new BlockPos(6,5,6);
        for(Direction bottom:Direction.values())for(boolean mixer:new boolean[]{false,true})
        for(var target:new net.minecraft.world.level.block.Block[]{cn.laowu.mod.create.CatMachineBlocks.HAJI_BASIN.get(),cn.laowu.mod.create.CatDepotRegistration.CAT_DEPOT.get()})
        for(boolean crouch:new boolean[]{false,true}){
            var top=bottom.getOpposite();
            var machine=mixer?cn.laowu.mod.create.CatMachineBlocks.CAT_MIXER.get():cn.laowu.mod.create.CatMachineBlocks.CAT_PRESS.get();
            h.setBlock(base,Blocks.AIR);
            h.setBlock(base.relative(top),Blocks.AIR);
            h.setBlock(base.relative(top,2),Blocks.AIR);
            h.setBlock(base,target.defaultBlockState().setValue(cn.laowu.mod.create.CatMachineOrientation.BOTTOM,bottom));
            player.setShiftKeyDown(crouch);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(machine,2));
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(base)),top,h.absolutePos(base),false);
            // Use the real server dispatch: block interaction precedes item placement.
            // Ordinary depot clicks insert; only a crouching click bypasses that interaction.
            var result=player.gameMode.useItemOn(player,h.getLevel(),player.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            if(target instanceof com.simibubi.create.content.logistics.depot.DepotBlock&&!crouch){
                var depot=(com.simibubi.create.content.logistics.depot.DepotBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(base));
                h.assertTrue(result.consumesAction()&&depot.getHeldItem().is(machine.asItem())&&depot.getHeldItem().getCount()==2
                        &&player.getMainHandItem().isEmpty()&&h.getBlockState(base.relative(top,2)).isAir(),
                        "Ordinary depot click must store the held machines, not place them: "+bottom);
                depot.setHeldItem(net.minecraft.world.item.ItemStack.EMPTY);
                h.setBlock(base,Blocks.AIR);
                continue;
            }
            h.assertTrue(result.consumesAction()&&h.getBlockState(base.relative(top,2)).is(machine)
                    &&h.getBlockState(base.relative(top)).isAir(),"Full right-click must skip one cell: "+bottom+"/"+target+"/mixer="+mixer+"/crouch="+crouch+" result="+result);
            h.assertTrue(cn.laowu.mod.create.CatMachineOrientation.bottom(h.getBlockState(base.relative(top,2)))==bottom
                    &&player.getMainHandItem().getCount()==1,"Full click must align and consume exactly one machine");
            if(h.getLevel().getBlockEntity(h.absolutePos(base)) instanceof com.simibubi.create.content.logistics.depot.DepotBlockEntity depot)
                h.assertTrue(depot.getHeldItem().isEmpty(),"Machine was inserted into depot instead of placed");
            h.setBlock(base.relative(top,2),Blocks.AIR);
            h.setBlock(base,Blocks.AIR);
        }
        h.succeed();
    }

@GameTest(template="artillery_probe",batch="cat_placement_modes",timeoutTicks=40)
    public static void topClickKeepsMaterialsAndCreativePlacementSemantics(GameTestHelper h){
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"top-click-safety"));
        var base=new BlockPos(6,5,6);
        for(Direction bottom:Direction.values()){
            var top=bottom.getOpposite();var gap=base.relative(top);var destination=base.relative(top,2);
            h.setBlock(base,cn.laowu.mod.create.CatDepotRegistration.CAT_DEPOT.get().defaultBlockState().setValue(cn.laowu.mod.create.CatMachineOrientation.BOTTOM,bottom));
            var depot=(com.simibubi.create.content.logistics.depot.DepotBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(base));
            depot.setHeldItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND));
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(base)),top,h.absolutePos(base),false);
            player.setShiftKeyDown(true);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            for(boolean blockedGap:new boolean[]{false,true}){
                h.setBlock(gap,blockedGap?Blocks.STONE:Blocks.AIR);
                h.setBlock(destination,blockedGap?Blocks.AIR:Blocks.STONE);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(cn.laowu.mod.create.CatMachineBlocks.CAT_PRESS.get(),2));
                player.gameMode.useItemOn(player,h.getLevel(),player.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,hit);
                h.assertTrue(player.getMainHandItem().getCount()==2&&depot.getHeldItem().is(net.minecraft.world.item.Items.DIAMOND)
                    &&h.getBlockState(blockedGap?gap:destination).is(Blocks.STONE),"Blocked top placement must preserve both hand and depot "+bottom);
            }
            h.setBlock(gap,Blocks.AIR);h.setBlock(destination,Blocks.AIR);
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(cn.laowu.mod.create.CatMachineBlocks.CAT_PRESS.get(),2));
            player.gameMode.useItemOn(player,h.getLevel(),player.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            h.assertTrue(h.getBlockState(destination).is(cn.laowu.mod.create.CatMachineBlocks.CAT_PRESS.get())
                    &&player.getMainHandItem().getCount()==2&&depot.getHeldItem().is(net.minecraft.world.item.Items.DIAMOND),"Creative top placement must not consume or retrieve depot contents");
            h.setBlock(destination,Blocks.AIR);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setShiftKeyDown(false);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT,3));
            int diamondsBefore=player.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND);
            player.gameMode.useItemOn(player,h.getLevel(),player.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            h.assertTrue(player.getMainHandItem().isEmpty()&&depot.getHeldItem().is(net.minecraft.world.item.Items.IRON_INGOT)
                    &&depot.getHeldItem().getCount()==3,"Ordinary material insertion must retain Create's interaction");
            h.assertTrue(player.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND)==diamondsBefore+1,
                    "Replacing held material must return the original diamond without loss");
            int ironBefore=player.getInventory().countItem(net.minecraft.world.item.Items.IRON_INGOT);
            player.gameMode.useItemOn(player,h.getLevel(),player.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            h.assertTrue(depot.getHeldItem().isEmpty()&&player.getInventory().countItem(net.minecraft.world.item.Items.IRON_INGOT)==ironBefore+3,
                    "Empty-hand material retrieval must return all three ingots without loss");
            h.setBlock(base,Blocks.AIR);
        }
        h.succeed();
    }

@GameTest(template="artillery_probe",batch="cat_placement_modes",timeoutTicks=40)
    public static void crouchingFacesPlayerInsteadOfMountFace(GameTestHelper h){
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"placement-modes"));
        var pos=new BlockPos(6,6,6);
        // Fixed view directions independent of clicked face; crouching must use the view.
        float[][] angles={{0,90},{0,-90},{180,0},{0,0},{90,0},{-90,0}};
        Direction[] looks={Direction.DOWN,Direction.UP,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST};
        for(var block:new net.minecraft.world.level.block.Block[]{cn.laowu.mod.create.CatMachineBlocks.HAJI_BASIN.get(),cn.laowu.mod.create.CatDepotRegistration.CAT_DEPOT.get(),cn.laowu.mod.create.CatMachineBlocks.CAT_PRESS.get(),cn.laowu.mod.create.CatMachineBlocks.CAT_MIXER.get()})
        for(int i=0;i<looks.length;i++)for(boolean crouch:new boolean[]{false,true}){
            player.setYRot(angles[i][0]);player.setYHeadRot(angles[i][0]);player.setXRot(angles[i][1]);player.setShiftKeyDown(crouch);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(block));
            var c=new net.minecraft.world.item.context.BlockPlaceContext(context(player,h.absolutePos(pos),Direction.UP));
            var state=block.getStateForPlacement(c);
            boolean processor=block instanceof cn.laowu.mod.create.CatProcessorBlock;
            Direction expected=crouch?(processor?looks[i].getOpposite():looks[i]):Direction.DOWN;
            h.assertTrue(state!=null&&cn.laowu.mod.create.CatMachineOrientation.bottom(state)==expected,
                "Wrong placement facing "+block+"/"+looks[i]+"/crouch="+crouch+" expected="+expected+" actual="+state);
        }
        h.succeed();
    }

    @GameTest(template="artillery_probe",batch="cat_basin_capture",timeoutTicks=40)
    public static void basinCapturesInteriorItemsWithoutFallingAndKeepsRemainder(GameTestHelper h){
        for(Direction bottom:Direction.values()){
            var pos=new BlockPos(5,5,5);
            h.setBlock(pos,Blocks.AIR);
            var state=cn.laowu.mod.create.CatMachineBlocks.HAJI_BASIN.get().defaultBlockState().setValue(cn.laowu.mod.create.CatMachineOrientation.BOTTOM,bottom);
            h.setBlock(pos,state);
            var be=(cn.laowu.mod.create.HajiBasinBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
            var center=net.minecraft.world.phys.Vec3.atLowerCornerOf(h.absolutePos(pos)).add(.5,.4,.5);
            var inside=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),center.x,center.y,center.z,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT,10));
            inside.setNoGravity(true);inside.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);h.getLevel().addFreshEntity(inside);
            be.tick();
            h.assertTrue(!inside.isAlive()&&be.inputInventory.getStackInSlot(0).getCount()==10,"Basin failed to capture stationary interior item "+bottom);
            be.inputInventory.setStackInSlot(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT,60));
            var partial=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),center.x,center.y,center.z,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT,10));
            partial.setNoGravity(true);h.getLevel().addFreshEntity(partial);be.tick();
            h.assertTrue(partial.isAlive()&&partial.getItem().getCount()==6&&be.inputInventory.getStackInSlot(0).getCount()==64,"Partial insertion lost or duplicated items "+bottom);
            be.tick();
            h.assertTrue(partial.getItem().getCount()==6,"Full basin destroyed remainder "+bottom);
            partial.discard();
            var outside=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),center.x+1,center.y,center.z,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_INGOT,5));
            outside.setNoGravity(true);h.getLevel().addFreshEntity(outside);be.tick();
            h.assertTrue(outside.isAlive()&&outside.getItem().getCount()==5,"Basin sucked items from outside its block "+bottom);
            outside.discard();
            // Reuse the loaded fixture cell without dropping the previous orientation's inventory.
            be.inputInventory.setStackInSlot(0,net.minecraft.world.item.ItemStack.EMPTY);
        }
        h.succeed();
    }


    @GameTest(template="artillery_probe",batch="cat_sixway_quick_place",timeoutTicks=40)
    public static void sneakingWorkFacePlacesAtProcessingOffset(GameTestHelper h){
        for(Direction bottom:Direction.values())for(boolean mixer:new boolean[]{false,true}){
            var base=new BlockPos(5+bottom.ordinal()*11,5,mixer?10:3);
            var top=bottom.getOpposite();
            var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"machine-quick-place"));
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.setShiftKeyDown(true);
            var machine=mixer?cn.laowu.mod.create.CatMachineBlocks.CAT_MIXER.get():cn.laowu.mod.create.CatMachineBlocks.CAT_PRESS.get();
            for(var target:new net.minecraft.world.level.block.Block[]{cn.laowu.mod.create.CatMachineBlocks.HAJI_BASIN.get(),cn.laowu.mod.create.CatDepotRegistration.CAT_DEPOT.get()}){
                h.setBlock(base,target.defaultBlockState().setValue(cn.laowu.mod.create.CatMachineOrientation.BOTTOM,bottom));
                var destination=base.relative(top,2);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(machine,4));
                var use=context(player,h.absolutePos(base),top);
                player.getMainHandItem().useOn(use);
                h.assertTrue(h.getBlockState(destination).is(machine)&&h.getBlockState(base.relative(top)).isAir(),"Sneak work-face placement did not skip exactly one cell "+bottom+"/"+mixer);
                h.assertTrue(h.getBlockState(destination).getValue(cn.laowu.mod.create.CatMachineOrientation.BOTTOM)==bottom&&player.getMainHandItem().getCount()==3,"Quick place orientation or consumption wrong "+bottom);
                player.getMainHandItem().useOn(context(player,h.absolutePos(base),top));
                h.assertTrue(player.getMainHandItem().getCount()==3&&h.getBlockState(destination).is(machine),"Occupied target consumed/replaced machine");
                h.setBlock(destination,Blocks.AIR);h.setBlock(base.relative(top),Blocks.STONE);
                player.getMainHandItem().useOn(context(player,h.absolutePos(base),top));
                h.assertTrue(h.getBlockState(destination).isAir()&&player.getMainHandItem().getCount()==3,"Quick placement crossed an obstructed gap");
                h.setBlock(base.relative(top),Blocks.AIR);
                player.setShiftKeyDown(false);
                player.getMainHandItem().useOn(context(player,h.absolutePos(base),top));
                h.assertTrue(h.getBlockState(destination).is(machine)&&player.getMainHandItem().getCount()==2,"Non-sneaking item placement must use the native processing offset");
                h.setBlock(destination,Blocks.AIR);
                player.setShiftKeyDown(true);
                player.getMainHandItem().useOn(context(player,h.absolutePos(base),bottom));
                h.assertTrue(h.getBlockState(base.relative(bottom)).is(machine)&&h.getBlockState(destination).isAir(),"Back-face click should remain ordinary adjacent placement");
                h.setBlock(base.relative(bottom),Blocks.AIR);
            }
        }
        h.succeed();
    }

    @GameTest(template="artillery_probe",batch="cat_sixway_rotation",timeoutTicks=40)
    public static void wrenchRotatesBottomAndShaftTogether(GameTestHelper h){
        // Literal expected directions for Minecraft/Create clockwise rotation around positive X/Y/Z.
        Direction[][] expected={{Direction.SOUTH,Direction.NORTH,Direction.DOWN,Direction.UP,Direction.WEST,Direction.EAST},
            {Direction.DOWN,Direction.UP,Direction.EAST,Direction.WEST,Direction.NORTH,Direction.SOUTH},
            {Direction.WEST,Direction.EAST,Direction.NORTH,Direction.SOUTH,Direction.UP,Direction.DOWN}};
        var pos=new BlockPos(6,6,6);var absolute=h.absolutePos(pos);
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"machine-rotate"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,com.simibubi.create.AllItems.WRENCH.asStack());
        for(var machine:new cn.laowu.mod.create.CatProcessorBlock[]{cn.laowu.mod.create.CatMachineBlocks.CAT_PRESS.get(),cn.laowu.mod.create.CatMachineBlocks.CAT_MIXER.get()})
        for(Direction bottom:Direction.values())for(Direction face:Direction.values()){
            var axis=bottom.getAxis()==Direction.Axis.X?Direction.Axis.Z:Direction.Axis.X;
            var start=machine.defaultBlockState().setValue(cn.laowu.mod.create.CatMachineOrientation.BOTTOM,bottom).setValue(cn.laowu.mod.create.CatProcessorBlock.SHAFT_AXIS,axis);
            h.setBlock(pos,start);var be=h.getLevel().getBlockEntity(absolute);
            player.getMainHandItem().useOn(context(player,absolute,face));
            var actual=h.getBlockState(pos);int turnAxis=face.getAxis().ordinal();
            h.assertTrue(actual.getValue(cn.laowu.mod.create.CatMachineOrientation.BOTTOM)==expected[turnAxis][bottom.ordinal()],"Wrench did not rotate bottom about clicked axis "+bottom+"/"+face);
            var positive=axis==Direction.Axis.X?Direction.EAST:axis==Direction.Axis.Y?Direction.UP:Direction.SOUTH;
            h.assertTrue(actual.getValue(cn.laowu.mod.create.CatProcessorBlock.SHAFT_AXIS)==expected[turnAxis][positive.ordinal()].getAxis(),"Wrench did not rotate shaft with body "+bottom+"/"+face);
            h.assertTrue(h.getLevel().getBlockEntity(absolute)==be,"Wrench replaced block entity instead of preserving it");
            for(int i=0;i<3;i++)player.getMainHandItem().useOn(context(player,absolute,face));
            h.assertTrue(h.getBlockState(pos).equals(start),"Four quarter-turns failed to return to original orientation");
        }
        h.succeed();
    }

    @GameTest(template="artillery_probe",batch="cat_sixway_ports_default",timeoutTicks=30)
    public static void permanentPortsIncludeLegacyClosedState(GameTestHelper h){
        for(var machine:new cn.laowu.mod.create.CatProcessorBlock[]{cn.laowu.mod.create.CatMachineBlocks.CAT_PRESS.get(),cn.laowu.mod.create.CatMachineBlocks.CAT_MIXER.get()})
        for(Direction bottom:Direction.values())for(boolean legacyOpen:new boolean[]{false,true}){
            var axis=bottom.getAxis()==Direction.Axis.X?Direction.Axis.Z:Direction.Axis.X;
            var state=machine.defaultBlockState().setValue(cn.laowu.mod.create.CatMachineOrientation.BOTTOM,bottom).setValue(cn.laowu.mod.create.CatProcessorBlock.SHAFT_AXIS,axis).setValue(cn.laowu.mod.create.CatProcessorBlock.SHAFT_OPEN,legacyOpen);
            int count=0;
            for(Direction face:Direction.values()){
                boolean connects=machine.hasShaftTowards(h.getLevel(),h.absolutePos(new BlockPos(5,5,5)),state,face);
                h.assertTrue(connects==(face.getAxis()==axis),"Permanent opposing shaft pair unavailable "+bottom+"/"+legacyOpen+"/"+face);
                if(connects)count++;
            }
            h.assertTrue(count==2,"Machine must always have exactly two opposite shaft faces");
        }
        h.succeed();
    }



    @GameTest(template="artillery_probe",batch="cat_sixway_interaction",timeoutTicks=30)
    public static void handInteractionFollowsDepotWorkFace(GameTestHelper h){
        for(Direction bottom:Direction.values()){
            var pos=new BlockPos(5+bottom.ordinal()*10,5,5);
            var block=cn.laowu.mod.create.CatDepotRegistration.CAT_DEPOT.get();
            h.setBlock(pos,block.defaultBlockState().setValue(cn.laowu.mod.create.CatMachineOrientation.BOTTOM,bottom));
            var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"sixway-depot-hand"));
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var absolute=h.absolutePos(pos);
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(absolute),bottom.getOpposite(),absolute,false);
            var held=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT,12);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,held);
            var state=h.getBlockState(pos);
            state.use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            var depot=(com.simibubi.create.content.logistics.depot.DepotBlockEntity)h.getLevel().getBlockEntity(absolute);
            h.assertTrue(depot.getHeldItem().is(net.minecraft.world.item.Items.IRON_INGOT)&&depot.getHeldItem().getCount()==12&&player.getMainHandItem().isEmpty(),"Cannot place material on rotated depot "+bottom);
            state.use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            h.assertTrue(depot.getHeldItem().isEmpty()&&player.getInventory().countItem(net.minecraft.world.item.Items.IRON_INGOT)==12,"Empty-hand retrieval lost or duplicated items "+bottom);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_INGOT,3));
            hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(absolute),bottom,absolute,false);
            state.use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            h.assertTrue(depot.getHeldItem().isEmpty()&&player.getMainHandItem().getCount()==3,"Non-working back face accepted items "+bottom);
        }
        h.succeed();
    }
    @GameTest(template="artillery_probe",batch="cat_sixway_placement",timeoutTicks=40)
    public static void processorsCanStandAloneButNotTouchWorkSurface(GameTestHelper h){
        for(Direction bottom:Direction.values())for(boolean mixer:new boolean[]{false,true}){
            var pos=new BlockPos(5+bottom.ordinal()*11,5,mixer?10:3);
            var absolute=h.absolutePos(pos);
            var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"sixway-freestanding"));
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var machine=mixer?cn.laowu.mod.create.CatMachineBlocks.CAT_MIXER.get():cn.laowu.mod.create.CatMachineBlocks.CAT_PRESS.get();
            var support=pos.relative(bottom);
            h.setBlock(support,Blocks.STONE);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(machine,4));
            player.getMainHandItem().useOn(context(player,h.absolutePos(support),bottom.getOpposite()));
            h.assertTrue(h.getBlockState(pos).is(machine),"Freestanding machine placement wrongly requires a processing target "+bottom+"/"+mixer);
            h.assertTrue(h.getBlockState(pos).getValue(cn.laowu.mod.create.CatMachineOrientation.BOTTOM)==bottom,"Freestanding orientation must follow placement face "+bottom);
            h.setBlock(pos,Blocks.AIR);
            for(var target:new net.minecraft.world.level.block.Block[]{cn.laowu.mod.create.CatMachineBlocks.HAJI_BASIN.get(),cn.laowu.mod.create.CatDepotRegistration.CAT_DEPOT.get()}){
                h.setBlock(support,target.defaultBlockState().setValue(cn.laowu.mod.create.CatMachineOrientation.BOTTOM,bottom));
                // Click a side mounting block so depot's own hand interaction cannot hide the placement result.
                Direction side=bottom.getAxis()==Direction.Axis.X?Direction.NORTH:Direction.WEST;
                var mount=pos.relative(side);h.setBlock(mount,Blocks.STONE);
                int before=player.getMainHandItem().getCount();
                player.getMainHandItem().useOn(context(player,h.absolutePos(mount),side.getOpposite()));
                h.assertTrue(h.getBlockState(pos).isAir()&&player.getMainHandItem().getCount()==before,"Machine allowed flush against work face "+bottom+"/"+target);
                h.setBlock(support,Blocks.AIR);
            }
        }
        h.succeed();
    }


    @GameTest(template="artillery_probe",batch="cat_sixway_overflow",timeoutTicks=140)
    public static void blockedSpoutQueuesSurviveReloadAndResume(GameTestHelper h){
        for(Direction bottom:Direction.values()){
            var p=new BlockPos(5+bottom.ordinal()*11,5,5);
            var direction=cn.laowu.mod.create.CatMachineOrientation.toWorld(bottom,Direction.SOUTH);
            var output=p.relative(bottom).relative(direction);
            var state=cn.laowu.mod.create.CatMachineBlocks.HAJI_BASIN.get().defaultBlockState().setValue(cn.laowu.mod.create.CatMachineOrientation.BOTTOM,bottom);
            h.setBlock(p,state);h.setBlock(output,state);
            var source=new com.simibubi.create.content.processing.basin.BasinBlockEntity[]{(com.simibubi.create.content.processing.basin.BasinBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p))};
            var target=(com.simibubi.create.content.processing.basin.BasinBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(output));
            h.runAfterDelay(10,()->{
                if(h.getBlockState(p).getValue(com.simibubi.create.content.processing.basin.BasinBlock.FACING)!=Direction.SOUTH)source[0].onWrenched(Direction.SOUTH);
                h.assertTrue(h.getBlockState(p).getValue(com.simibubi.create.content.processing.basin.BasinBlock.FACING)==Direction.SOUTH,"Spout unavailable before queue test "+bottom);
                for(var inv:target.getInvs())for(int i=0;i<inv.getSlots();i++)inv.setStackInSlot(i,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,64));
                for(var behaviour:target.getTanks())((com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.InternalFluidHandler)behaviour.getCapability().orElseThrow(IllegalStateException::new)).forceFill(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,10000),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                h.assertTrue(source[0].acceptOutputs(java.util.List.of(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT,3)),java.util.List.of(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,250)),false),"Could not queue spout output "+bottom);
            });
            h.runAfterDelay(30,()->{
                h.assertTrue(!source[0].canContinueProcessing(),"Full destination lost pending spout outputs "+bottom);
                var tag=source[0].saveWithFullMetadata();
                var absolute=h.absolutePos(p);
                var loaded=net.minecraft.world.level.block.entity.BlockEntity.loadStatic(absolute,h.getBlockState(p),tag);
                h.assertTrue(loaded instanceof cn.laowu.mod.create.HajiBasinBlockEntity,"Reload changed basin BE type");
                h.getLevel().removeBlockEntity(absolute);h.getLevel().setBlockEntity(loaded);
                source[0]=(com.simibubi.create.content.processing.basin.BasinBlockEntity)loaded;source[0].initialize();
                h.assertTrue(!source[0].canContinueProcessing(),"Reload lost spout queue "+bottom);
                h.assertTrue(cn.laowu.mod.create.CatMachineOrientation.bottom(loaded.getBlockState())==bottom,"Reload lost bottom "+bottom);
            });
            h.runAfterDelay(50,()->{
                for(var inv:target.getInvs())for(int i=0;i<inv.getSlots();i++)inv.setStackInSlot(i,net.minecraft.world.item.ItemStack.EMPTY);
                for(var behaviour:target.getTanks())behaviour.getCapability().orElseThrow(IllegalStateException::new).drain(10000,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                target.notifyChangeOfContents();
            });
            h.runAfterDelay(110,()->{
                int items=0,water=0;
                for(var inv:target.getInvs())for(int i=0;i<inv.getSlots();i++)if(inv.getStackInSlot(i).is(net.minecraft.world.item.Items.IRON_INGOT))items+=inv.getStackInSlot(i).getCount();
                for(var behaviour:target.getTanks()){var handler=behaviour.getCapability().orElseThrow(IllegalStateException::new);for(int i=0;i<handler.getTanks();i++)water+=handler.getFluidInTank(i).getAmount();}
                h.assertTrue(items==3&&water==250&&source[0].canContinueProcessing(),"Reloaded spout failed conservation "+bottom+" items="+items+" water="+water+" source="+source[0].saveWithFullMetadata());
            });
        }
        h.runAfterDelay(120,h::succeed);
    }


    @GameTest(template="artillery_probe",batch="cat_sixway_placement",timeoutTicks=40)
    public static void survivalPlacementAndFrameInverses(GameTestHelper h){
        for(Direction bottom:Direction.values()){
            h.assertTrue(cn.laowu.mod.create.CatMachineOrientation.toWorld(bottom,Direction.DOWN)==bottom,"Wrong physical bottom "+bottom);
            for(Direction local:Direction.values())
                h.assertTrue(cn.laowu.mod.create.CatMachineOrientation.toLocal(bottom,cn.laowu.mod.create.CatMachineOrientation.toWorld(bottom,local))==local,"Frame not invertible "+bottom+"/"+local);
            for(boolean mixer:new boolean[]{false,true}){
                var p=new BlockPos(5+bottom.ordinal()*11,5,mixer?10:3);
                var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"sixway-placement"));
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                var target=mixer?cn.laowu.mod.create.CatMachineBlocks.HAJI_BASIN.get():cn.laowu.mod.create.CatDepotRegistration.CAT_DEPOT.get();
                var support=p.relative(bottom);
                h.setBlock(support,Blocks.STONE);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(target,2));
                player.getMainHandItem().useOn(context(player,h.absolutePos(support),bottom.getOpposite()));
                h.assertTrue(h.getBlockState(p).is(target)&&h.getBlockState(p).getValue(cn.laowu.mod.create.CatMachineOrientation.BOTTOM)==bottom,"Target item placement facing "+bottom);
                var machine=p.relative(bottom.getOpposite(),2);
                Direction side=bottom.getAxis()==Direction.Axis.X?Direction.NORTH:Direction.WEST;
                var mounting=machine.relative(side);
                h.setBlock(mounting,Blocks.STONE);
                var item=mixer?cn.laowu.mod.create.CatMachineBlocks.CAT_MIXER_ITEM.get():cn.laowu.mod.create.CatMachineBlocks.CAT_PRESS_ITEM.get();
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(item,2));
                var use=context(player,h.absolutePos(mounting),side.getOpposite());
                player.getMainHandItem().useOn(use);
                var state=h.getBlockState(machine);
                h.assertTrue(state.getBlock() instanceof cn.laowu.mod.create.CatProcessorBlock&&state.getValue(cn.laowu.mod.create.CatMachineOrientation.BOTTOM)==bottom,"Processor item placement failed "+bottom+"/"+mixer);
                h.assertTrue(state.getValue(cn.laowu.mod.create.CatProcessorBlock.SHAFT_OPEN)&&player.getMainHandItem().getCount()==1,"New processor must have permanent ports and consume one item");
                h.setBlock(machine,Blocks.AIR);
                h.setBlock(machine.relative(bottom),Blocks.STONE);
                player.getMainHandItem().useOn(use);
                h.assertTrue(h.getBlockState(machine).getBlock() instanceof cn.laowu.mod.create.CatProcessorBlock&&player.getMainHandItem().isEmpty(),"Blocked processing gap must not forbid placement "+bottom);
                h.assertTrue(!cn.laowu.mod.create.CatProcessorPlacement.validTarget(h.getLevel(),h.absolutePos(machine),h.getBlockState(machine),mixer),"Blocked gap allowed processing "+bottom);
                if(mixer){
                    h.setBlock(machine,Blocks.AIR);
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(item));
                    h.setBlock(machine.relative(bottom),Blocks.AIR);
                    h.setBlock(p,cn.laowu.mod.create.CatDepotRegistration.CAT_DEPOT.get().defaultBlockState().setValue(cn.laowu.mod.create.CatMachineOrientation.BOTTOM,bottom));
                    player.getMainHandItem().useOn(context(player,h.absolutePos(mounting),side.getOpposite()));
                    h.assertTrue(h.getBlockState(machine).is(cn.laowu.mod.create.CatMachineBlocks.CAT_MIXER.get()),"Mixer placement must not require a compatible recipe target "+bottom);
                    h.assertTrue(!cn.laowu.mod.create.CatProcessorPlacement.validTarget(h.getLevel(),h.absolutePos(machine),h.getBlockState(machine),true),"Mixer must not process depot "+bottom);
                }
            }
        }
        h.succeed();
    }
    private static net.minecraft.world.item.context.UseOnContext context(net.minecraft.world.entity.player.Player player,BlockPos pos,Direction face){
        return new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),face,pos,false));
    }


    @GameTest(template="artillery_probe",batch="cat_sixway_isolation",timeoutTicks=250)
    public static void ordinaryPressRejectsSidewaysBasin(GameTestHelper h){
        var p=new BlockPos(6,5,5);
        h.setBlock(p,cn.laowu.mod.create.CatMachineBlocks.HAJI_BASIN.get().defaultBlockState().setValue(cn.laowu.mod.create.CatMachineOrientation.BOTTOM,Direction.NORTH));
        h.setBlock(p.above(2),com.simibubi.create.AllBlocks.MECHANICAL_PRESS.getDefaultState().setValue(com.simibubi.create.content.kinetics.press.MechanicalPressBlock.HORIZONTAL_FACING,Direction.EAST));
        var basin=(com.simibubi.create.content.processing.basin.BasinBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p));
        basin.getFilter().setFilter(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_BLOCK));
        basin.inputInventory.insertItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT,9),false);
        h.runAfterDelay(10,()->{
            h.setBlock(p.above(2).west(),com.simibubi.create.AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(com.simibubi.create.content.kinetics.motor.CreativeMotorBlock.FACING,Direction.EAST));
            ((com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p.above(2).west()))).generatedSpeed.setValue(256);
        });
        h.runAfterDelay(180,()->{
            h.assertTrue(basin.inputInventory.getStackInSlot(0).getCount()==9 && basin.getOutputInventory().isEmpty(),"Ordinary press processed a sideways basin through its solid side");
            h.succeed();
        });
    }
    @GameTest(template="artillery_probe",batch="cat_sixway_packing",timeoutTicks=250)
    public static void compactionAndPlacementContract(GameTestHelper h){
        for(Direction bottom:Direction.values()){
            var p=new BlockPos(5+bottom.ordinal()*11,5,5);
            setup(h,p,bottom,true,false);
            var processor=p.relative(bottom.getOpposite(),2);
            var state=h.getBlockState(processor);
            h.assertTrue(cn.laowu.mod.create.CatProcessorPlacement.validTarget(h.getLevel(),h.absolutePos(processor),state,false),"Valid basin rejected "+bottom);
            var basin=(com.simibubi.create.content.processing.basin.BasinBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p));
            basin.getFilter().setFilter(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_BLOCK));
            h.runAfterDelay(10,()->{
                h.setBlock(processor.relative(bottom),Blocks.STONE);
                h.assertTrue(!cn.laowu.mod.create.CatProcessorPlacement.validTarget(h.getLevel(),h.absolutePos(processor),state,false),"Solid gap accepted "+bottom);
                basin.inputInventory.insertItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT,9),false);
            });
            h.runAfterDelay(60,()->{
                h.assertTrue(basin.inputInventory.getStackInSlot(0).getCount()==9,"Blocked machine consumed input "+bottom);
                h.setBlock(processor.relative(bottom),Blocks.AIR);
                basin.notifyChangeOfContents();
            });
            h.runAfterDelay(190,()->{
                int count=0;for(var inv:basin.getInvs())for(int i=0;i<inv.getSlots();i++)if(inv.getStackInSlot(i).is(net.minecraft.world.item.Items.IRON_BLOCK))count+=inv.getStackInSlot(i).getCount();
                h.assertTrue(count==1&&basin.inputInventory.isEmpty(),"Six-way compaction did not conserve inputs "+bottom);
            });
        }
        h.runAfterDelay(210,h::succeed);
    }


    @GameTest(template="artillery_probe",batch="cat_sixway_isolation",timeoutTicks=250)
    public static void horizontalPressIgnoresOrdinaryDepotBelow(GameTestHelper h){
        var target=new BlockPos(10,6,5);
        setup(h,target,Direction.EAST,false,false);
        var processor=target.west(2);
        var wrong=processor.below(2);
        h.setBlock(wrong,com.simibubi.create.AllBlocks.DEPOT.get());
        h.runAfterDelay(10,()->((com.simibubi.create.content.logistics.depot.DepotBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(wrong))).setHeldItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT)));
        h.runAfterDelay(170,()->{
            var be=(com.simibubi.create.content.logistics.depot.DepotBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(wrong));
            h.assertTrue(be.getHeldItem().is(net.minecraft.world.item.Items.IRON_INGOT),"Sideways press processed a second, non-target ordinary depot below");
            h.succeed();
        });
    }


    @GameTest(template="artillery_probe",batch="cat_sixway_ports",timeoutTicks=150)
    public static void wrenchRotationReconnectsPowerAndDismantles(GameTestHelper h){
        for(Direction bottom:Direction.values()){
            var depotPos=new BlockPos(5+bottom.ordinal()*11,5,5);
            setup(h,depotPos,bottom,false,false);
            var pos=depotPos.relative(bottom.getOpposite(),2);var absolute=h.absolutePos(pos);
            Direction side=bottom.getAxis()==Direction.Axis.X?Direction.NORTH:Direction.WEST;
            var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"sixway-wrench"));
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,com.simibubi.create.AllItems.WRENCH.asStack());
            h.runAfterDelay(10,()->{
                h.assertTrue(Math.abs(((cn.laowu.mod.create.CatPressBlockEntity)h.getLevel().getBlockEntity(absolute)).getSpeed())==256,"Permanent port lacks power "+bottom);
                player.getMainHandItem().useOn(context(player,absolute,bottom));
            });
            h.runAfterDelay(30,()->{
                var state=h.getBlockState(pos);
                h.assertTrue(state.getValue(cn.laowu.mod.create.CatMachineOrientation.BOTTOM)==bottom&&state.getValue(cn.laowu.mod.create.CatProcessorBlock.SHAFT_AXIS)!=side.getAxis(),"Work-face wrench did not turn shaft axis "+bottom);
                h.assertTrue(((cn.laowu.mod.create.CatPressBlockEntity)h.getLevel().getBlockEntity(absolute)).getSpeed()==0,"Rotated-away shaft retained phantom power "+bottom);
                player.getMainHandItem().useOn(context(player,absolute,bottom));
            });
            h.runAfterDelay(50,()->{
                h.assertTrue(Math.abs(((cn.laowu.mod.create.CatPressBlockEntity)h.getLevel().getBlockEntity(absolute)).getSpeed())==256,"Rotated-back shaft failed to reconnect "+bottom);
                player.getMainHandItem().useOn(context(player,absolute,side));
                h.assertTrue(h.getBlockState(pos).getValue(cn.laowu.mod.create.CatMachineOrientation.BOTTOM)!=bottom,"Side-face wrench did not reorient working end "+bottom);
                h.assertTrue(!cn.laowu.mod.create.CatProcessorPlacement.validTarget(h.getLevel(),absolute,h.getBlockState(pos),false),"Rotation retained obsolete processing target "+bottom);
            });
            h.runAfterDelay(70,()->{
                h.assertTrue(Math.abs(((cn.laowu.mod.create.CatPressBlockEntity)h.getLevel().getBlockEntity(absolute)).getSpeed())==256,"Rotation around shaft lost power "+bottom);
                player.setShiftKeyDown(true);player.getMainHandItem().useOn(context(player,absolute,side));player.setShiftKeyDown(false);
                h.assertTrue(h.getBlockState(pos).isAir(),"Sneak wrench did not dismantle "+bottom);
                h.assertTrue(player.getInventory().contains(new net.minecraft.world.item.ItemStack(cn.laowu.mod.create.CatMachineBlocks.CAT_PRESS_ITEM.get())),"Dismantle lost block item "+bottom);
            });
        }
        h.runAfterDelay(90,h::succeed);
    }
    @GameTest(template="artillery_probe",batch="cat_sixway_spouts",timeoutTicks=400)
    public static void spoutsAndLocalFiltersInAllDirections(GameTestHelper h){
        var tickingPositions=new java.util.ArrayList<BlockPos>();
        var scenarios=new java.util.ArrayList<Runnable>();
        for(Direction bottom:Direction.values())for(Direction local:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}){
            int i=local.ordinal()-2;
            var p=new BlockPos(5+bottom.ordinal()*11,3+(i/2)*5,3+(i%2)*7);
            var side=cn.laowu.mod.create.CatMachineOrientation.toWorld(bottom,local);
            var output=p.relative(bottom).relative(side);
            var state=cn.laowu.mod.create.CatMachineBlocks.HAJI_BASIN.get().defaultBlockState().setValue(cn.laowu.mod.create.CatMachineOrientation.BOTTOM,bottom);
            h.setBlock(p,state);h.setBlock(output,state);
            for(var testPos:java.util.List.of(p,output)){
                var absolute=h.absolutePos(testPos);
                tickingPositions.add(absolute);
                var chunk=new net.minecraft.world.level.ChunkPos(absolute);
                h.getLevel().setChunkForced(chunk.x,chunk.z,true);
            }
            var basin=(com.simibubi.create.content.processing.basin.BasinBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p));
            scenarios.add(()->{
            h.runAfterDelay(10,()->{
                if(h.getBlockState(p).getValue(com.simibubi.create.content.processing.basin.BasinBlock.FACING)!=local)basin.onWrenched(local);
                h.assertTrue(h.getBlockState(p).getValue(com.simibubi.create.content.processing.basin.BasinBlock.FACING)==local,"Spout discovery missed "+bottom+"/"+local);
                h.assertTrue(basin.acceptOutputs(java.util.List.of(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT,3)),java.util.List.of(),false),"Spout rejected output "+bottom);
                var transform=(com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided)basin.getFilter().getSlotPositioning();
                for(Direction face:Direction.values()){
                    transform.fromSide(face);
                    var offset=transform.getLocalOffset(h.getLevel(),h.absolutePos(p),state);
                    h.assertTrue(transform.testHit(h.getLevel(),h.absolutePos(p),state,offset)==(face.getAxis()!=bottom.getAxis()),"Filter hit test wrong "+bottom+"/"+face);
                }
            });
            h.runAfterDelay(60,()->{
                var target=(com.simibubi.create.content.processing.basin.BasinBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(output));
                int count=0;for(var inv:target.getInvs())for(int slot=0;slot<inv.getSlots();slot++)count+=inv.getStackInSlot(slot).getCount();
                h.assertTrue(count==3,"Spout output lost/duplicated "+bottom+" count="+count+" source="+basin.saveWithFullMetadata()+" target="+target.saveWithFullMetadata()+" sourceState="+basin.getBlockState()+" targetState="+target.getBlockState());
            });
            });
        }
        // The 72-block fixture exceeds vanilla GameTest\'s corner-centred forced area.
        // Start the original 10/60-tick schedule only once every source/target really ticks.
        h.startSequence().thenWaitUntil(()->{
            for(var pos:tickingPositions)h.assertTrue(h.getLevel().isPositionEntityTicking(pos),"Spout fixture is not ticking yet: "+pos);
        }).thenExecute(()->scenarios.forEach(Runnable::run)).thenExecuteAfter(70,h::succeed);
    }


    @GameTest(template="artillery_probe", batch="cat_sixway_processing", timeoutTicks=480)
    public static void nativeProcessingInEveryDirection(GameTestHelper h) {
        for (Direction bottom:Direction.values()) {
            int i=bottom.ordinal();
            setup(h,new BlockPos(5+i*11,5,3),bottom,false,false);
            setup(h,new BlockPos(5+i*11,5,10),bottom,true,true);
        }
        h.runAfterDelay(10,()-> {
            for(Direction bottom:Direction.values()) {
                var p=new BlockPos(5+bottom.ordinal()*11,5,3);
                var q=new BlockPos(p.getX(),5,10);
                var press=(cn.laowu.mod.create.CatPressBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p.relative(bottom.getOpposite(),2)));
                var mixer=(cn.laowu.mod.create.CatMixerBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(q.relative(bottom.getOpposite(),2)));
                h.assertTrue(Math.abs(press.getSpeed())==256 && Math.abs(mixer.getSpeed())==256,"Real shaft power "+bottom);
                ((com.simibubi.create.content.logistics.depot.DepotBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p))).setHeldItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT));
                var basin=(com.simibubi.create.content.processing.basin.BasinBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(q));
                basin.inputInventory.insertItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COPPER_INGOT),false);
                basin.inputInventory.insertItem(1,com.simibubi.create.AllItems.ZINC_INGOT.asStack(),false);
                basin.notifyChangeOfContents();
            }
        });
        h.runAfterDelay(100,()->{
            for(Direction bottom:Direction.values()) {
                var q=new BlockPos(5+bottom.ordinal()*11,5,10);
                var basin=(com.simibubi.create.content.processing.basin.BasinBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(q));
                h.assertTrue(!basin.inputInventory.isEmpty(),"Mixer ignored local heat "+bottom);
                var heat=q.relative(bottom);
                h.setBlock(heat,com.simibubi.create.AllBlocks.BLAZE_BURNER.getDefaultState().setValue(com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL,com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.KINDLED));
                ((com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(heat))).isCreative=true;
                basin.notifyChangeOfContents();
            }
        });
        h.runAfterDelay(430,()->{
            for(Direction bottom:Direction.values()) {
                var p=new BlockPos(5+bottom.ordinal()*11,5,3);
                var depot=(com.simibubi.create.content.logistics.depot.DepotBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p));
                h.assertTrue(depot.getHeldItem().is(com.simibubi.create.AllItems.IRON_SHEET.get())&&depot.getHeldItem().getCount()==1,"Six-way depot press failed: "+bottom+" got "+depot.getHeldItem());
                var basin=(com.simibubi.create.content.processing.basin.BasinBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(p.getX(),5,10)));
                int count=0;
                for(var inv:basin.getInvs())for(int slot=0;slot<inv.getSlots();slot++)if(inv.getStackInSlot(slot).is(com.simibubi.create.AllItems.BRASS_INGOT.get()))count+=inv.getStackInSlot(slot).getCount();
                h.assertTrue(count==2 && basin.inputInventory.isEmpty(),"Six-way heated mixer failed: "+bottom+" brass="+count);
            }
            h.succeed();
        });
    }
    private static void setup(GameTestHelper h, BlockPos p,Direction bottom,boolean basin,boolean mixer) {
        h.setBlock(p,(basin?cn.laowu.mod.create.CatMachineBlocks.HAJI_BASIN.get():cn.laowu.mod.create.CatDepotRegistration.CAT_DEPOT.get()).defaultBlockState().setValue(cn.laowu.mod.create.CatMachineOrientation.BOTTOM,bottom));
        Direction shaft=bottom.getAxis()==Direction.Axis.X?Direction.NORTH:Direction.WEST;
        BlockPos machine=p.relative(bottom.getOpposite(),2);
        h.setBlock(machine,(mixer?cn.laowu.mod.create.CatMachineBlocks.CAT_MIXER.get():cn.laowu.mod.create.CatMachineBlocks.CAT_PRESS.get()).defaultBlockState()
            .setValue(cn.laowu.mod.create.CatMachineOrientation.BOTTOM,bottom)
            .setValue(cn.laowu.mod.create.CatProcessorBlock.SHAFT_AXIS,shaft.getAxis()).setValue(cn.laowu.mod.create.CatProcessorBlock.SHAFT_OPEN,true));
        h.setBlock(machine.relative(shaft),com.simibubi.create.AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(com.simibubi.create.content.kinetics.motor.CreativeMotorBlock.FACING,shaft.getOpposite()));
        ((com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(machine.relative(shaft)))).generatedSpeed.setValue(256);
    }

    @GameTest(template="accessory_probe", batch="cat_sixway", timeoutTicks=30)
    public static void sixWayMachineContracts(GameTestHelper h) {
        for (String id : new String[]{"cat_press","cat_mixer","haji_basin","cat_depot"}) {
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("laowu",id));
            h.assertTrue(block != Blocks.AIR, "Missing functional cat machine: " + id);
            var property = block.getStateDefinition().getProperty("bottom");
            h.assertTrue(property instanceof DirectionProperty, id + " lacks six-way bottom");
            var bottom = (DirectionProperty) property;
            h.assertTrue(bottom.getPossibleValues().size()==6, id + " must allow all six bottoms");
            h.assertTrue(block.defaultBlockState().getValue(bottom)==Direction.DOWN, id + " broke legacy upright default");
        }
        h.succeed();
    }
}
