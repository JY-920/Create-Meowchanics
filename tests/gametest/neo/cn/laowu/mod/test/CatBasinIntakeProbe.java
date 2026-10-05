package cn.laowu.mod.test;

import cn.laowu.mod.create.CatMachineBlocks;
import cn.laowu.mod.create.CatMachineOrientation;
import cn.laowu.mod.create.HajiBasinBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CatBasinIntakeProbe {
    // Catches both Create's inherited world-up landing insertion and gaps produced by
    // rotating its deliberately shortened item-only rim into a solid world-top wall.
    @GameTest(template="artillery_probe", batch="cat_basin_intake", timeoutTicks=40)
    public static void fallingOntoClosedWorldTopNeverInserts(GameTestHelper h) {
        BlockPos pos = new BlockPos(5, 5, 5);
        for (Direction bottom : Direction.values()) {
            if (bottom == Direction.DOWN) continue;
            for (double x : new double[]{.0625, .5, .9375})
                for (double z : new double[]{.0625, .5, .9375}) {
                    HajiBasinBlockEntity basin = placeCatBasin(h, pos, bottom);
                    ItemEntity item = spawn(h, pos, new Vec3(x, 1.75, z), 7);
                    item.setDeltaMovement(Vec3.ZERO);
                    for (int tick = 0; tick < 30 && item.isAlive(); tick++) {
                        item.tick();
                        basin.tick();
                        h.assertTrue(basin.inputInventory.isEmpty(),
                                "Closed world-top admitted falling item: bottom=" + bottom
                                + " x=" + x + " z=" + z + " tick=" + tick
                                + " itemPosition=" + item.position());
                    }
                    h.assertTrue(item.isAlive() && item.getItem().getCount() == 7,
                            "Rejected top item was lost or changed: " + bottom);
                    item.discard();
                }
        }
        h.succeed();
    }

    // Hand-authored mouth coordinates, not the production rotation helper.
    // Catches blocking all dropped items or removing real side-facing opening intake.
    @GameTest(template="artillery_probe", batch="cat_basin_intake", timeoutTicks=40)
    public static void movingItemsEnterTheActualMouthInEveryDirection(GameTestHelper h) {
        BlockPos pos = new BlockPos(5, 5, 5);
        Direction[] bottoms = {Direction.DOWN, Direction.UP, Direction.NORTH,
                Direction.SOUTH, Direction.WEST, Direction.EAST};
        Vec3[] starts = {new Vec3(.5, 1.25, .5), new Vec3(.5, -.5, .5),
                new Vec3(.5, .375, 1.25), new Vec3(.5, .375, -.25),
                new Vec3(1.25, .375, .5), new Vec3(-.25, .375, .5)};
        Vec3[] velocities = {new Vec3(0, -.15, 0), new Vec3(0, .15, 0),
                new Vec3(0, 0, -.15), new Vec3(0, 0, .15),
                new Vec3(-.15, 0, 0), new Vec3(.15, 0, 0)};
        for (int i = 0; i < bottoms.length; i++) {
            HajiBasinBlockEntity basin = placeCatBasin(h, pos, bottoms[i]);
            ItemEntity item = spawn(h, pos, starts[i], 7);
            item.setNoGravity(true);
            item.setDeltaMovement(velocities[i]);
            for (int tick = 0; tick < 12 && item.isAlive(); tick++) {
                item.tick();
                basin.tick();
            }
            h.assertTrue(!item.isAlive() && basin.inputInventory.getStackInSlot(0).getCount() == 7,
                    "Actual oriented mouth rejected moving item: " + bottoms[i]
                    + " finalPosition=" + item.position());
            clear(basin);
        }
        h.succeed();
    }

    @GameTest(template="artillery_probe", batch="cat_basin_intake", timeoutTicks=40)
    public static void stationaryInteriorAndPartialInsertionRemainConserved(GameTestHelper h) {
        BlockPos pos = new BlockPos(5, 5, 5);
        for (Direction bottom : Direction.values()) {
            HajiBasinBlockEntity basin = placeCatBasin(h, pos, bottom);
            basin.inputInventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 60));
            ItemEntity item = spawn(h, pos, new Vec3(.5, .4, .5), 10);
            item.setNoGravity(true);
            item.setDeltaMovement(Vec3.ZERO);
            basin.tick();
            h.assertTrue(item.isAlive() && item.getItem().getCount() == 6
                    && basin.inputInventory.getStackInSlot(0).getCount() == 64,
                    "Stationary interior partial insertion lost/duplicated items: " + bottom);
            basin.tick();
            h.assertTrue(item.isAlive() && item.getItem().getCount() == 6,
                    "Full basin consumed uninsertable remainder: " + bottom);
            item.discard();
            clear(basin);
        }
        h.succeed();
    }

    @GameTest(template="artillery_probe", batch="cat_basin_intake", timeoutTicks=40)
    public static void nativeCreateBasinStillAcceptsFallingItems(GameTestHelper h) {
        BlockPos pos = new BlockPos(5, 5, 5);
        h.setBlock(pos, AllBlocks.BASIN.get());
        BasinBlockEntity basin = (BasinBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
        ItemEntity item = spawn(h, pos, new Vec3(.5, 1.75, .5), 7);
        item.setDeltaMovement(Vec3.ZERO);
        for (int tick = 0; tick < 30 && item.isAlive(); tick++) {
            item.tick();
            basin.tick();
        }
        h.assertTrue(!item.isAlive() && basin.inputInventory.getStackInSlot(0).getCount() == 7,
                "Native Create basin falling-item intake changed");
        h.succeed();
    }

    // The collision callback must not be an unguarded second path into storage.
    // This item is inside the block cell, but wholly above every non-upright interior.
    @GameTest(template="artillery_probe", batch="cat_basin_intake", timeoutTicks=40)
    public static void landingCallbackCannotBypassRotatedInterior(GameTestHelper h) {
        BlockPos pos = new BlockPos(5, 5, 5);
        for (Direction bottom : Direction.values()) {
            if (bottom == Direction.DOWN) continue;
            HajiBasinBlockEntity basin = placeCatBasin(h, pos, bottom);
            ItemEntity item = spawn(h, pos, new Vec3(.5, .9, .5), 7);
            item.setNoGravity(true);
            item.setDeltaMovement(new Vec3(0, -.1, 0));
            h.getBlockState(pos).getBlock().updateEntityAfterFallOn(h.getLevel(), item);
            h.assertTrue(basin.inputInventory.isEmpty() && item.isAlive()
                    && item.getItem().getCount() == 7,
                    "Landing callback bypassed rotated interior: " + bottom);
            h.assertTrue(item.getDeltaMovement().y == 0,
                    "Rejected landing must retain ordinary vertical motion stop");
            item.discard();
        }
        h.succeed();
    }

    @GameTest(template="artillery_probe", batch="cat_basin_intake", timeoutTicks=40)
    public static void physicalFaceRestrictionsDoNotRestrictAutomation(GameTestHelper h) {
        BlockPos pos = new BlockPos(5, 5, 5);
        for (Direction bottom : Direction.values()) {
            HajiBasinBlockEntity basin = placeCatBasin(h, pos, bottom);
            for (Direction side : Direction.values()) {
                var handler = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), side);
                h.assertTrue(handler != null, "Missing automation handler: " + bottom + "/" + side);
                var remainder = net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(handler,
                        new ItemStack(Items.IRON_INGOT, 3), false);
                h.assertTrue(remainder.isEmpty() && basin.inputInventory.getStackInSlot(0).getCount() == 3,
                        "Physical intake rules blocked automation: " + bottom + "/" + side);
                clear(basin);
            }
        }
        h.succeed();
    }


    private static HajiBasinBlockEntity placeCatBasin(GameTestHelper h, BlockPos pos, Direction bottom) {
        if (h.getLevel().getBlockEntity(h.absolutePos(pos)) instanceof BasinBlockEntity previous)
            clear(previous);
        h.setBlock(pos, Blocks.AIR);
        h.setBlock(pos, CatMachineBlocks.HAJI_BASIN.get().defaultBlockState()
                .setValue(CatMachineOrientation.BOTTOM, bottom));
        return (HajiBasinBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
    }

    private static ItemEntity spawn(GameTestHelper h, BlockPos pos, Vec3 offset, int count) {
        Vec3 location = Vec3.atLowerCornerOf(h.absolutePos(pos)).add(offset);
        ItemEntity item = new ItemEntity(h.getLevel(), location.x, location.y, location.z,
                new ItemStack(Items.IRON_INGOT, count));
        h.getLevel().addFreshEntity(item);
        return item;
    }

    private static void clear(BasinBlockEntity basin) {
        for (int slot = 0; slot < basin.inputInventory.getSlots(); slot++)
            basin.inputInventory.setStackInSlot(slot, ItemStack.EMPTY);
    }
}
