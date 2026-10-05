package cn.laowu.mod.test;

import com.simibubi.create.content.logistics.depot.DepotBlock;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.ItemHandlerHelper;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CatDepotProbe {
    @GameTest(template="accessory_probe", batch="cat_machines", timeoutTicks=20)
    public static void depotShapeMatchesAuthoredBody(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, cn.laowu.mod.create.CatDepotRegistration.CAT_DEPOT.get());
        var state = h.getBlockState(pos);
        var selection = state.getShape(h.getLevel(), h.absolutePos(pos));
        var collision = state.getCollisionShape(h.getLevel(), h.absolutePos(pos));
        var expected = net.minecraft.world.phys.shapes.Shapes.or(
                net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 11, 16),
                net.minecraft.world.level.block.Block.box(1, 11, 1, 15, 13, 15));
        h.assertTrue(selection.max(Direction.Axis.Y) == 13d / 16, "Cat depot selection must stop at replacement model's 13px tabletop");
        h.assertTrue(collision.max(Direction.Axis.Y) == 13d / 16, "Cat depot collision must stop at replacement model's 13px tabletop");
        h.assertTrue(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(selection, expected, net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME), "Selection must follow inset tabletop and full-width base");
        h.assertTrue(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(collision, expected, net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME), "Collision must follow inset tabletop and full-width base");
        h.succeed();
    }

    // Catches lost belt-processing behaviours when using a custom depot BE registration.
    @GameTest(template="artillery_probe", batch="cat_machines", timeoutTicks=250)
    public static void depotAcceptsPressDeployerAndSpout(GameTestHelper h) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("laowu", "cat_depot"));
        h.assertTrue(block instanceof DepotBlock, "Cat depot must register before processing");
        var pressPos = new BlockPos(4, 2, 4);
        var deployPos = new BlockPos(12, 2, 4);
        var spoutPos = new BlockPos(20, 2, 4);
        for (var pos : new BlockPos[]{pressPos, deployPos, spoutPos}) h.setBlock(pos, block);
        h.setBlock(pressPos.above(2), com.simibubi.create.AllBlocks.MECHANICAL_PRESS.getDefaultState()
                .setValue(com.simibubi.create.content.kinetics.press.MechanicalPressBlock.HORIZONTAL_FACING, Direction.EAST));
        h.setBlock(deployPos.above(2), com.simibubi.create.AllBlocks.DEPLOYER.getDefaultState()
                .setValue(com.simibubi.create.content.kinetics.deployer.DeployerBlock.FACING, Direction.DOWN)
                .setValue(com.simibubi.create.content.kinetics.deployer.DeployerBlock.AXIS_ALONG_FIRST_COORDINATE, true));
        h.setBlock(spoutPos.above(2), com.simibubi.create.AllBlocks.SPOUT.get());
        for (var pos : new BlockPos[]{pressPos, deployPos}) {
            h.setBlock(pos.above(2).west(), com.simibubi.create.AllBlocks.CREATIVE_MOTOR.getDefaultState()
                    .setValue(com.simibubi.create.content.kinetics.motor.CreativeMotorBlock.FACING, Direction.EAST));
            ((com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos.above(2).west()))).generatedSpeed.setValue(256);
        }
        h.runAfterDelay(10, () -> {
            var deployer = (com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(deployPos.above(2)));
            h.assertTrue(Math.abs(deployer.getSpeed()) == 256, "Deployer must be mechanically powered");
            deployer.getPlayer().setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, com.simibubi.create.AllItems.SAND_PAPER.asStack());
            var fluid = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(spoutPos.above(2)), Direction.UP);
            fluid.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            depot(h, pressPos).setHeldItem(new ItemStack(Items.IRON_INGOT));
            depot(h, deployPos).setHeldItem(com.simibubi.create.AllItems.ROSE_QUARTZ.asStack());
            depot(h, spoutPos).setHeldItem(new ItemStack(Items.GLASS_BOTTLE));
        });
        h.runAfterDelay(200, () -> {
            h.assertTrue(depot(h, pressPos).getHeldItem().is(com.simibubi.create.AllItems.IRON_SHEET.get()), "Native press must make an iron sheet on cat depot");
            h.assertTrue(depot(h, deployPos).getHeldItem().is(com.simibubi.create.AllItems.POLISHED_ROSE_QUARTZ.get()), "Native deployer must polish rose quartz on cat depot");
            h.assertTrue(depot(h, spoutPos).getHeldItem().is(Items.POTION), "Native spout must fill a water bottle on cat depot");
            for (var pos : new BlockPos[]{pressPos, deployPos, spoutPos}) h.assertTrue(depot(h, pos).getHeldItem().getCount() == 1, "Depot processing must conserve one output per input");
            h.succeed();
        });
    }

    private static DepotBlockEntity depot(GameTestHelper h, BlockPos pos) {
        return (DepotBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
    }

    // Catches a visual-only block, missing sided inventory, and a foreign/invalid saved BE type.
    @GameTest(template="accessory_probe", batch="cat_machines", timeoutTicks=40)
    public static void depotStoresExtractsAndReloads(GameTestHelper h) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("laowu", "cat_depot"));
        h.assertTrue(block instanceof DepotBlock, "Cat depot must register as a functional Create depot");
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, block);
        var absolute = h.absolutePos(pos);
        var depot = (DepotBlockEntity) h.getLevel().getBlockEntity(absolute);
        h.assertTrue(depot != null && depot.getType().isValid(h.getBlockState(pos)), "Depot BE must accept its own block");
        var inventory = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, absolute, Direction.UP);
        h.assertTrue(ItemHandlerHelper.insertItemStacked(inventory, new ItemStack(Items.IRON_INGOT, 12), false).isEmpty(), "Depot must accept hopper/funnel insertion");
        h.runAfterDelay(10, () -> {
            h.assertTrue(depot.getHeldItem().is(Items.IRON_INGOT) && depot.getHeldItem().getCount() == 12, "Inserted stack must reach native item display");
            var saved = depot.saveWithFullMetadata(h.getLevel().registryAccess());
            var restored = BlockEntity.loadStatic(absolute, h.getBlockState(pos), saved, h.getLevel().registryAccess());
            h.assertTrue(restored instanceof DepotBlockEntity && restored.getType() == depot.getType(), "Depot must reload its registered BE type");
            h.assertTrue(((DepotBlockEntity) restored).getHeldItem().getCount() == 12, "Saved displayed stack must survive reload");
            h.assertTrue("laowu".equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(restored.getType()).getNamespace()), "Depot must not save using Create's depot BE type");
            var extracted = inventory.extractItem(0, 5, false);
            h.assertTrue(extracted.is(Items.IRON_INGOT) && extracted.getCount() == 5 && depot.getHeldItem().getCount() == 7, "Automation extraction must change the displayed stack without loss");
            h.succeed();
        });
    }
}
