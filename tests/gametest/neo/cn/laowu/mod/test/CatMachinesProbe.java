package cn.laowu.mod.test;

import com.simibubi.create.AllBlocks;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.decoration.encasing.EncasableBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import java.util.List;
import java.util.UUID;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CatMachinesProbe {
    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("laowu", id));
    }

    @GameTest(template="accessory_probe", batch="cat_machines", timeoutTicks=30)
    public static void casingVariantsRegister(GameTestHelper h) {
        h.assertTrue(block("cat_casing") instanceof CasingBlock, "Cat casing must be a functional Create casing");
        String[] ids = {"cat_encased_shaft", "cat_encased_cogwheel", "cat_encased_large_cogwheel"};
        Block[] originals = {AllBlocks.SHAFT.get(), AllBlocks.COGWHEEL.get(), AllBlocks.LARGE_COGWHEEL.get()};
        for (int i=0; i<ids.length; i++) {
            Block variant=block(ids[i]);
            h.assertTrue(variant!=Blocks.AIR && EncasingRegistry.getVariants(originals[i]).contains(variant),
                    "Right-click encasing must discover "+ids[i]);
            BlockPos pos=new BlockPos(i+1,2,1);
            h.setBlock(pos,variant);
            var be=h.getLevel().getBlockEntity(h.absolutePos(pos));
            h.assertTrue(be!=null && be.getType().isValid(h.getBlockState(pos)),
                    "Encased kinetic block must have a valid persistent block entity: "+ids[i]);
        }
        h.succeed();
    }

    @GameTest(template="accessory_probe", batch="cat_machines", timeoutTicks=30)
    public static void basinRegistersAsProcessingBasin(GameTestHelper h) {
        Block basin=block("haji_basin");
        h.assertTrue(basin instanceof BasinBlock, "Haji basin must be a functional Create basin");
        BlockPos pos=new BlockPos(2,2,2);
        h.setBlock(pos,basin);
        var absolute=h.absolutePos(pos);
        h.assertTrue(BasinBlock.isBasin(h.getLevel(),absolute), "Create operators must recognize the custom basin");
        var be=h.getLevel().getBlockEntity(absolute);
        h.assertTrue(be instanceof BasinBlockEntity && be.getType().isValid(h.getBlockState(pos)),
                "Basin must have a valid persistent basin block entity");
        h.succeed();
    }

    // Exercise the survival interaction path, allowing normal world-tick BE reconnection.
    @GameTest(template="artillery_probe", batch="cat_machines", timeoutTicks=120)
    public static void poweredEncasingWrenchAndReload(GameTestHelper h) {
        String[] ids = {"cat_encased_shaft", "cat_encased_cogwheel", "cat_encased_large_cogwheel"};
        Block[] originals = {AllBlocks.SHAFT.get(), AllBlocks.COGWHEEL.get(), AllBlocks.LARGE_COGWHEEL.get()};
        for (int kind=0; kind<3; kind++) {
            final int variant = kind;
            for (Direction.Axis axis : Direction.Axis.values()) {
                BlockPos pos = new BlockPos(4 + kind*18 + axis.ordinal()*5, 4, 5);
                BlockPos absolute = h.absolutePos(pos);
                h.setBlock(pos, originals[kind].defaultBlockState().setValue(BlockStateProperties.AXIS, axis));
                Direction drive = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
                motor(h, pos.relative(drive.getOpposite()), drive);
                var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "cat-machines"));
                player.setGameMode(GameType.SURVIVAL);
                ItemStack casing = new ItemStack(block("cat_casing"), 16);
                var hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
                float[] originalSpeed = new float[1];
                String label = ids[kind] + "/" + axis;
                // All nine fixtures run three full cycles. Custom BE types are replaced at encasing,
                // so verify reconnection after ten real server ticks, never by injecting speed/ticks.
                for (int cycle=0; cycle<3; cycle++) {
                    int start = 10 + cycle*30;
                    final boolean lastCycle = cycle==2;
                    h.runAfterDelay(start, () -> {
                        h.assertTrue(h.getBlockState(pos).is(originals[variant]), "Missing original before encasing " + label);
                        originalSpeed[0] = ((KineticBlockEntity) h.getLevel().getBlockEntity(absolute)).getSpeed();
                        h.assertTrue(Math.abs(originalSpeed[0])==256, "Motor must power original " + label);
                        player.setItemInHand(InteractionHand.MAIN_HAND, casing);
                        var state = h.getBlockState(pos);
                        ((EncasableBlock) state.getBlock()).tryEncase(state, h.getLevel(), absolute,
                                casing, player, InteractionHand.MAIN_HAND, hit);
                        h.assertTrue(h.getBlockState(pos).is(block(ids[variant])), "Right-click did not encase " + label);
                        h.assertTrue(h.getBlockState(pos).getValue(BlockStateProperties.AXIS)==axis, "Encasing changed axis " + label);
                        h.assertTrue(casing.getCount()==16, "Survival encasing consumed the reusable casing " + label);
                    });
                    h.runAfterDelay(start+10, () -> {
                        h.assertTrue(h.getBlockState(pos).is(block(ids[variant])), "Encased block disappeared while ticking " + label);
                        var encased = (KineticBlockEntity) h.getLevel().getBlockEntity(absolute);
                        h.assertTrue(encased.getSpeed()==originalSpeed[0],
                                "Encased block did not reconnect to motor after ten world ticks: " + label);
                        var saved = encased.saveWithFullMetadata(h.getLevel().registryAccess());
                        var restored = BlockEntity.loadStatic(absolute, h.getBlockState(pos), saved, h.getLevel().registryAccess());
                        h.assertTrue(restored instanceof KineticBlockEntity && restored.getType()==encased.getType(),
                                "Encased kinetic NBT failed to resolve its own registered BE type " + label);
                        h.assertTrue("laowu".equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(restored.getType()).getNamespace()),
                                "Encased kinetic entity must not serialize as a Create BE " + label);
                        h.assertTrue(((KineticBlockEntity) restored).getSpeed()==originalSpeed[0], "Encased NBT lost rotation " + label);
                        player.setItemInHand(InteractionHand.MAIN_HAND, AllItems.WRENCH.asStack());
                        player.setShiftKeyDown(true);
                        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
                        player.setShiftKeyDown(false);
                        h.assertTrue(h.getBlockState(pos).is(originals[variant]), "Sneak-wrench did not restore original " + label);
                        h.assertTrue(h.getBlockState(pos).getValue(BlockStateProperties.AXIS)==axis, "Uncasing changed axis " + label);
                        player.setItemInHand(InteractionHand.MAIN_HAND, casing);
                    });
                    h.runAfterDelay(start+20, () -> {
                        h.assertTrue(h.getBlockState(pos).is(originals[variant]), "Restored block disappeared while ticking " + label);
                        h.assertTrue(((KineticBlockEntity) h.getLevel().getBlockEntity(absolute)).getSpeed()==originalSpeed[0],
                                "Uncased block did not reconnect to motor after ten world ticks: " + label);
                        h.assertTrue(player.getInventory().countItem(originals[variant].asItem())==0,
                                "Uncasing duplicated an original kinetic item into inventory " + label);
                        h.assertTrue(player.getInventory().countItem(block("cat_casing").asItem())==16,
                                "Encasing/uncasing consumed or duplicated casing items " + label);
                        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(absolute).inflate(1)).isEmpty(),
                                "Encasing/uncasing produced duplicate item drops " + label);
                        if (lastCycle) player.getInventory().clearContent();
                    });
                }
            }
        }
        h.runAfterDelay(100, h::succeed);
    }

    @GameTest(template="accessory_probe", batch="cat_machines", timeoutTicks=50)
    public static void basinAutomationAndPersistentContents(GameTestHelper h) {
        BlockPos pos = new BlockPos(2,2,2);
        h.setBlock(pos, block("haji_basin"));
        h.runAfterDelay(2, () -> {
            BasinBlockEntity basin = basin(h, pos);
            IItemHandler items = items(h, pos, Direction.UP);
            IFluidHandler fluids = fluids(h, pos, Direction.NORTH);
            h.assertTrue(ItemHandlerHelper.insertItemStacked(items, new ItemStack(Items.IRON_INGOT,5), true).isEmpty(),
                    "Item automation simulation refused valid input");
            h.assertTrue(count(items, Items.IRON_INGOT)==0, "Simulated insertion mutated inventory");
            insert(h, items, new ItemStack(Items.IRON_INGOT,5));
            h.assertTrue(fluids.fill(new FluidStack(Fluids.WATER,1000), IFluidHandler.FluidAction.SIMULATE)==1000,
                    "Fluid automation simulation refused water");
            h.assertTrue(fluidAmount(fluids, Fluids.WATER)==0, "Simulated filling mutated tanks");
            h.assertTrue(fluids.fill(new FluidStack(Fluids.WATER,1000), IFluidHandler.FluidAction.EXECUTE)==1000,
                    "Fluid input capability failed to fill");
            basin.getFilter().setFilter(new ItemStack(Items.GOLD_INGOT));
            h.assertTrue(basin.acceptOutputs(List.of(new ItemStack(Items.GOLD_INGOT,2)),
                    List.of(new FluidStack(Fluids.LAVA,250)), true), "Basin rejected simulated recipe outputs");
            h.assertTrue(count(items, Items.GOLD_INGOT)==0 && fluidAmount(fluids, Fluids.LAVA)==0,
                    "Output simulation duplicated products");
            h.assertTrue(basin.acceptOutputs(List.of(new ItemStack(Items.GOLD_INGOT,2)),
                    List.of(new FluidStack(Fluids.LAVA,250)), false), "Basin rejected committed recipe outputs");
            h.assertTrue(!basin.getOutputInventory().insertItem(0, new ItemStack(Items.DIAMOND), false).isEmpty(),
                    "External insertion must not bypass output-only inventory");
            h.assertTrue(!basin.acceptOutputs(List.of(new ItemStack(Items.DIAMOND,640)), List.of(), true),
                    "Output overflow should block processing");

            BlockPos absolute = h.absolutePos(pos);
            var tag = basin.saveWithFullMetadata(h.getLevel().registryAccess());
            BlockEntity loaded = BlockEntity.loadStatic(absolute, h.getBlockState(pos), tag, h.getLevel().registryAccess());
            h.assertTrue(loaded instanceof BasinBlockEntity && loaded.getType()==basin.getType(),
                    "Basin reload lost custom registered type");
            h.assertTrue("laowu".equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(loaded.getType()).getNamespace()),
                    "Basin must serialize its own type, not create:basin");
            h.getLevel().removeBlockEntity(absolute);
            h.getLevel().setBlockEntity(loaded);
            BasinBlockEntity reloaded = (BasinBlockEntity) loaded;
            reloaded.initialize();
            IItemHandler restoredItems = items(h, pos, Direction.DOWN);
            IFluidHandler restoredFluids = fluids(h, pos, Direction.SOUTH);
            h.assertTrue(count(restoredItems, Items.IRON_INGOT)==5 && count(restoredItems, Items.GOLD_INGOT)==2,
                    "Basin reload lost/duplicated input or output items");
            h.assertTrue(fluidAmount(restoredFluids, Fluids.WATER)==1000 && fluidAmount(restoredFluids, Fluids.LAVA)==250,
                    "Basin reload lost/duplicated input or output fluids");
            h.assertTrue(reloaded.getFilter().test(new ItemStack(Items.GOLD_INGOT))
                    && !reloaded.getFilter().test(new ItemStack(Items.IRON_INGOT)), "Output filter did not survive reload");
            int extracted = 0;
            for (int slot=0; slot<restoredItems.getSlots(); slot++) extracted += restoredItems.extractItem(slot,64,false).getCount();
            h.assertTrue(extracted==7, "Automation extraction failed conservation of items after reload");
            for (int slot=0; slot<restoredItems.getSlots(); slot++)
                h.assertTrue(restoredItems.extractItem(slot,64,false).isEmpty(), "Repeated extraction duplicated items");
            h.assertTrue(restoredFluids.drain(new FluidStack(Fluids.WATER,1000), IFluidHandler.FluidAction.EXECUTE).getAmount()==1000,
                    "Automation could not drain stored water");
            h.assertTrue(restoredFluids.drain(new FluidStack(Fluids.LAVA,250), IFluidHandler.FluidAction.EXECUTE).getAmount()==250,
                    "Automation could not drain recipe output fluid");
            h.assertTrue(restoredFluids.drain(2000, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                    "Repeated fluid drain duplicated fluids");
            h.succeed();
        });
    }

    @GameTest(template="artillery_probe", batch="cat_machines", timeoutTicks=450)
    public static void mixerHonorsHeatFilterAndConservation(GameTestHelper h) {
        BlockPos pos = new BlockPos(4,3,4);
        h.setBlock(pos, block("haji_basin"));
        h.setBlock(pos.above(2), AllBlocks.MECHANICAL_MIXER.get());
        // Mixers are small cogs, not vertical shafts: drive the side with a Y-axis cog.
        h.setBlock(pos.above(2).east(), AllBlocks.COGWHEEL.getDefaultState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
        motor(h, pos.above(3).east(), Direction.DOWN);
        h.runAfterDelay(5, () -> {
            h.assertTrue(Math.abs(((MechanicalMixerBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos.above(2)))).getSpeed())==256,
                    "Mixer must receive real motor power");
            insert(h, items(h,pos,Direction.UP), new ItemStack(Items.COPPER_INGOT));
            insert(h, items(h,pos,Direction.UP), AllItems.ZINC_INGOT.asStack());
            basin(h,pos).notifyChangeOfContents();
        });
        h.runAfterDelay(60, () -> {
            assertBrassUnprocessed(h,pos,"Without heat");
            basin(h,pos).getFilter().setFilter(new ItemStack(Items.IRON_INGOT));
            h.setBlock(pos.below(), AllBlocks.BLAZE_BURNER.getDefaultState()
                    .setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED));
            ((BlazeBurnerBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos.below()))).isCreative = true;
            basin(h,pos).notifyChangeOfContents();
        });
        h.runAfterDelay(120, () -> {
            assertBrassUnprocessed(h,pos,"With wrong output filter");
            basin(h,pos).getFilter().setFilter(AllItems.BRASS_INGOT.asStack());
            basin(h,pos).notifyChangeOfContents();
        });
        h.runAfterDelay(340, () -> {
            IItemHandler inventory=items(h,pos,Direction.DOWN);
            h.assertTrue(count(inventory,AllItems.BRASS_INGOT.get())==2, "Real mixer did not produce exactly two brass ingots");
            h.assertTrue(count(inventory,Items.COPPER_INGOT)==0 && count(inventory,AllItems.ZINC_INGOT.get())==0,
                    "Mixer did not consume exactly one copper and one zinc");
        });
        h.runAfterDelay(400, () -> {
            h.assertTrue(count(items(h,pos,Direction.DOWN),AllItems.BRASS_INGOT.get())==2,
                    "Continued mixer ticks duplicated recipe outputs");
            h.succeed();
        });
    }

    @GameTest(template="artillery_probe", batch="cat_machines", timeoutTicks=300)
    public static void pressCompactsOnlyOnce(GameTestHelper h) {
        BlockPos pos = new BlockPos(4,3,4);
        h.setBlock(pos, block("haji_basin"));
        h.setBlock(pos.above(2), AllBlocks.MECHANICAL_PRESS.getDefaultState()
                .setValue(MechanicalPressBlock.HORIZONTAL_FACING, Direction.EAST));
        motor(h, pos.above(2).west(), Direction.EAST);
        h.runAfterDelay(5, () -> {
            var press=(MechanicalPressBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos.above(2)));
            h.assertTrue(Math.abs(press.getSpeed())==256, "Press must receive real motor power");
            basin(h,pos).getFilter().setFilter(new ItemStack(Items.IRON_BLOCK));
            insert(h,items(h,pos,Direction.UP),new ItemStack(Items.IRON_INGOT,9));
            basin(h,pos).notifyChangeOfContents();
        });
        h.runAfterDelay(200, () -> {
            IItemHandler inventory=items(h,pos,Direction.DOWN);
            h.assertTrue(count(inventory,Items.IRON_BLOCK)==1 && count(inventory,Items.IRON_INGOT)==0,
                    "Real press must consume nine iron ingots and produce exactly one block");
        });
        h.runAfterDelay(260, () -> {
            h.assertTrue(count(items(h,pos,Direction.DOWN),Items.IRON_BLOCK)==1, "Continued press ticks duplicated iron blocks");
            h.succeed();
        });
    }

    @GameTest(template="accessory_probe", batch="cat_machines", timeoutTicks=30)
    public static void miningToolsAndLootConserveComponents(GameTestHelper h) {
        String[] ids = {"cat_casing", "haji_basin", "cat_encased_shaft",
                "cat_encased_cogwheel", "cat_encased_large_cogwheel"};
        Item[] expected = {block("cat_casing").asItem(), block("haji_basin").asItem(),
                AllBlocks.SHAFT.asItem(), AllBlocks.COGWHEEL.asItem(), AllBlocks.LARGE_COGWHEEL.asItem()};
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "cat-mining"));
        player.setGameMode(GameType.SURVIVAL);
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
        BlockPos pos = new BlockPos(2,2,2);
        for (int i=0; i<ids.length; i++) {
            h.setBlock(pos, block(ids[i]));
            var state = h.getBlockState(pos);
            h.assertTrue(pickaxe.isCorrectToolForDrops(state) && player.hasCorrectToolForDrops(state),
                    "Iron pickaxe must be a correct harvest tool for " + ids[i]);
            // Resolve the real registered loot table with a tool-bearing loot context.
            var drops = Block.getDrops(state, h.getLevel(), h.absolutePos(pos), null, player, pickaxe);
            h.assertTrue(drops.size()==1 && drops.get(0).is(expected[i]) && drops.get(0).getCount()==1,
                    "Mining " + ids[i] + " must drop exactly one " + BuiltInRegistries.ITEM.getKey(expected[i])
                            + ", not a copied Create loot table, casing, or duplicate component; got " + drops);
        }
        player.getInventory().clearContent();
        h.succeed();
    }


    private static void assertBrassUnprocessed(GameTestHelper h, BlockPos pos, String phase) {
        IItemHandler inventory=items(h,pos,Direction.UP);
        h.assertTrue(count(inventory,Items.COPPER_INGOT)==1 && count(inventory,AllItems.ZINC_INGOT.get())==1
                && count(inventory,AllItems.BRASS_INGOT.get())==0, phase + ": brass recipe consumed or duplicated items");
    }

    private static void motor(GameTestHelper h, BlockPos pos, Direction facing) {
        h.setBlock(pos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING,facing));
        ((CreativeMotorBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos))).generatedSpeed.setValue(256);
    }

    private static BasinBlockEntity basin(GameTestHelper h, BlockPos pos) {
        return (BasinBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
    }

    private static IItemHandler items(GameTestHelper h, BlockPos pos, Direction side) {
        IItemHandler result = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), side);
        h.assertTrue(result!=null, "Custom basin has no sided item automation capability");
        return result;
    }

    private static IFluidHandler fluids(GameTestHelper h, BlockPos pos, Direction side) {
        IFluidHandler result = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, h.absolutePos(pos), side);
        h.assertTrue(result!=null, "Custom basin has no sided fluid automation capability");
        return result;
    }

    private static void insert(GameTestHelper h, IItemHandler inventory, ItemStack stack) {
        h.assertTrue(ItemHandlerHelper.insertItemStacked(inventory,stack,false).isEmpty(), "Basin automation refused recipe input");
    }

    private static int count(IItemHandler inventory, Item item) {
        int result=0;
        for (int slot=0; slot<inventory.getSlots(); slot++) {
            ItemStack stack=inventory.getStackInSlot(slot);
            if (stack.is(item)) result+=stack.getCount();
        }
        return result;
    }

    private static int fluidAmount(IFluidHandler tanks, net.minecraft.world.level.material.Fluid fluid) {
        int result=0;
        for (int tank=0; tank<tanks.getTanks(); tank++) {
            FluidStack stack=tanks.getFluidInTank(tank);
            if (stack.getFluid()==fluid) result+=stack.getAmount();
        }
        return result;
    }
}
