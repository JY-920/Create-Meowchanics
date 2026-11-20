package cn.laowu.mod.test;

import cn.laowu.mod.create.*;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlock;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CatPressBeltProbe {
    // Belts have a world-up work face, unlike six-way cat depots/basins.
    @GameTest(template="artillery_probe", batch="cat_press_belt", timeoutTicks=20)
    public static void beltWorkFaceOnlyAcceptsUprightPressAndHorizontalBelt(GameTestHelper h) {
        var pos = new BlockPos(8, 5, 7);
        var press = CatMachineBlocks.CAT_PRESS.get().defaultBlockState();
        for (Direction bottom : Direction.values()) {
            var target = pos.relative(bottom, 2);
            for (var slope : com.simibubi.create.content.kinetics.belt.BeltSlope.values()) {
                h.setBlock(target, AllBlocks.BELT.getDefaultState().setValue(com.simibubi.create.content.kinetics.belt.BeltBlock.SLOPE, slope));
                boolean expected = bottom == Direction.DOWN && slope == com.simibubi.create.content.kinetics.belt.BeltSlope.HORIZONTAL;
                h.assertTrue(CatProcessorPlacement.validTarget(h.getLevel(), h.absolutePos(pos), press.setValue(CatMachineOrientation.BOTTOM, bottom), false) == expected,
                        "Belt target must require downward press and horizontal slope: " + bottom + "/" + slope);
                h.assertTrue(!CatProcessorPlacement.validTarget(h.getLevel(), h.absolutePos(pos), press.setValue(CatMachineOrientation.BOTTOM, bottom), true), "Mixers must not gain belt recipes");
            }
            h.setBlock(target, net.minecraft.world.level.block.Blocks.AIR);
        }
        h.succeed();
    }

    // Catches rejecting native belt handlers and cancelling their active pressing cycle.
    @GameTest(template="artillery_probe", batch="cat_press_belt", timeoutTicks=240)
    public static void poweredCatPressProcessesMovingBeltLikeNativePress(GameTestHelper h) {
        for (int x : new int[]{4, 12}) {
            var start = new BlockPos(x, 2, 2);
            var end = new BlockPos(x, 2, 11);
            h.setBlock(start, AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
            h.setBlock(end, AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
            BeltConnectorItem.createBelts(h.getLevel(), h.absolutePos(start), h.absolutePos(end));
            var press = new BlockPos(x, 4, 7);
            h.setBlock(press, x == 4 ? AllBlocks.MECHANICAL_PRESS.getDefaultState()
                    .setValue(MechanicalPressBlock.HORIZONTAL_FACING, Direction.EAST)
                    : CatMachineBlocks.CAT_PRESS.get().defaultBlockState());
            for (var pos : new BlockPos[]{start.west(), press.west()}) {
                h.setBlock(pos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST));
                ((CreativeMotorBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos))).generatedSpeed.setValue(64);
            }
        }
        h.runAfterDelay(20, () -> {
            for (int x : new int[]{4, 12}) {
                var belt = belt(h, x);
                var press = (MechanicalPressBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(x, 4, 7)));
                h.assertTrue(Math.abs(belt.getSpeed()) == 64 && Math.abs(press.getSpeed()) == 64, "Both belt and press need real kinetic power");
                var simulation = new TransportedItemStack(new ItemStack(Items.IRON_INGOT));
                var simulatedOutputs = new java.util.ArrayList<ItemStack>();
                h.assertTrue(press.tryProcessOnBelt(simulation, simulatedOutputs, true), "Native pressing recipe must match during simulation");
                h.assertTrue(simulation.stack.is(Items.IRON_INGOT) && simulation.stack.getCount() == 1 && simulatedOutputs.isEmpty(), "Simulation must not consume inputs or manufacture outputs");
                var cargo = new TransportedItemStack(new ItemStack(Items.IRON_INGOT));
                cargo.beltPosition = belt.getDirectionAwareBeltMovementSpeed() > 0 ? 3.75f : 6.75f;
                cargo.prevBeltPosition = cargo.beltPosition;
                cargo.insertedAt = (int)cargo.beltPosition;
                cargo.insertedFrom = Direction.UP;
                belt.getInventory().addItem(cargo);
            }
        });
        h.runAfterDelay(200, () -> {
            for (int x : new int[]{4, 12}) {
                int sheets = 0, ingots = 0;
                for (var cargo : belt(h, x).getInventory().getTransportedItems()) {
                    if (cargo.stack.is(AllItems.IRON_SHEET.get())) sheets += cargo.stack.getCount();
                    if (cargo.stack.is(Items.IRON_INGOT)) ingots += cargo.stack.getCount();
                }
                for (var entity : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(x-1, 0, 0))), net.minecraft.world.phys.Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(x+2, 6, 14)))))) {
                    if (entity.getItem().is(AllItems.IRON_SHEET.get())) sheets += entity.getItem().getCount();
                    if (entity.getItem().is(Items.IRON_INGOT)) ingots += entity.getItem().getCount();
                }
                h.assertTrue(sheets == 1 && ingots == 0, (x == 4 ? "Native" : "Cat") + " press must turn one moving belt ingot into exactly one sheet; sheets=" + sheets + ", ingots=" + ingots);
            }
            h.succeed();
        });
    }

    private static BeltBlockEntity belt(GameTestHelper h, int x) {
        return ((BeltBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(x, 2, 2)))).getControllerBE();
    }
}
