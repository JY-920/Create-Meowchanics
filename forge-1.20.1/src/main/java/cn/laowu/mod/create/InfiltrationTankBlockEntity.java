package cn.laowu.mod.create;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.CatToolBehavior;
import cn.laowu.mod.recipe.InfiltratingRecipe;
import cn.laowu.mod.recipe.PotionArrowInfiltration;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;

/** Create basin storage/animation with one 1000 mB input fluid and built-in heat semantics. */
public final class InfiltrationTankBlockEntity extends BasinBlockEntity {
    public static final int FLUID_CAPACITY = 1000;
    private int processingProgress;
    private ResourceLocation processingRecipe;
    private boolean currentFacingMapping = true;

    public InfiltrationTankBlockEntity(BlockPos pos, BlockState state) {
        super(LaoWuMod.INFILTRATION_TANK_BE.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        // BasinRecipe.match() tests this recipe filter against the recipe's
        // result. Give the slot an explicit label so its purpose is clear.
        getFilter().setLabel(Component.translatable("gui.laowu.recipe_output_filter"));

        // Replace Create basin's two-fluid input/output behaviours with a
        // single-fluid pair. The output storage remains available internally
        // for BasinRecipe, while only the input storage is exposed externally.
        behaviours.remove(inputTank);
        behaviours.remove(outputTank);
        inputTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT,
                this, 1, FLUID_CAPACITY, true)
                .whenFluidUpdates(this::notifyChangeOfContents);
        outputTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT,
                this, 1, FLUID_CAPACITY, true)
                .whenFluidUpdates(this::notifyChangeOfContents)
                .forbidInsertion();
        behaviours.add(inputTank);
        behaviours.add(outputTank);

        // BasinBlockEntity normally combines input and output and consequently
        // advertises 2000 mB. This machine is one physical 1000 mB tank, so its
        // pipes, buckets and goggle tooltip all use this single capability.
        fluidCapability.invalidate();
        fluidCapability = inputTank.getCapability().cast();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;

        InfiltratingRecipe matching = level.getRecipeManager()
                .getAllRecipesFor(LaoWuMod.INFILTRATING_TYPE.get())
                .stream()
                .filter(this::matchesRecipeAndSelectedOutput)
                .findFirst()
                .orElse(null);

        if (matching == null) {
            resetProcessing();
            return;
        }

        if (!matching.getId().equals(processingRecipe)) {
            processingRecipe = matching.getId();
            processingProgress = 0;
            notifyUpdate();
        }

        setAreFluidsMoving(true);
        int duration = Math.max(1, matching.getProcessingDuration());
        if (++processingProgress < duration) {
            if ((processingProgress & 7) == 0) notifyUpdate();
            return;
        }

        boolean applied = matching.isCatToolRepair()
                ? applyCatToolRepair()
                : matching.isPotionArrow()
                ? applyPotionArrow()
                : BasinRecipe.apply(this, matching);
        if (applied) {
            // BasinRecipe writes fluid results into Create's internal output
            // tank. This machine exposes one physical 1000 mB tank, so move a
            // completed result back to that same externally extractable tank.
            // Inputs have already been consumed at this point.
            moveFluidResultToPhysicalTank();
            processingProgress = 0;
            processingRecipe = null;
            notifyChangeOfContents();
            notifyUpdate();
        } else {
            processingProgress = duration - 1;
        }
    }

    @Override
    public void lazyTick() {
        // Keep BasinBlock.FACING at DOWN so BasinRecipe stores results in the
        // internal output inventory/tank. The custom shell uses MODEL_FACING;
        // allowing the native output scan to select a horizontal direction
        // would once again make recipe simulation require an adjacent output.
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(com.simibubi.create.content.processing.basin.BasinBlock.FACING)
                || !state.hasProperty(InfiltrationTankBlock.MODEL_FACING)) return;

        Direction oldBasinFacing = state.getValue(
                com.simibubi.create.content.processing.basin.BasinBlock.FACING);
        Direction modelFacing = state.getValue(InfiltrationTankBlock.MODEL_FACING);

        // Worlds saved before MODEL_FACING existed stored the model direction
        // in BasinBlock.FACING. Preserve that appearance while migrating the
        // basin output direction back to its native internal-output value.
        if (oldBasinFacing.getAxis().isHorizontal()) {
            modelFacing = currentFacingMapping
                    ? oldBasinFacing
                    : oldBasinFacing.getOpposite();
        }
        currentFacingMapping = true;
        BlockState corrected = state
                .setValue(com.simibubi.create.content.processing.basin.BasinBlock.FACING, Direction.DOWN)
                .setValue(InfiltrationTankBlock.MODEL_FACING, modelFacing);
        if (corrected != state)
            level.setBlockAndUpdate(worldPosition, corrected);
        setChanged();
    }

    private void resetProcessing() {
        setAreFluidsMoving(false);
        if (processingProgress == 0 && processingRecipe == null) return;
        processingProgress = 0;
        processingRecipe = null;
        notifyUpdate();
    }

    public int processingProgress() {
        return processingProgress;
    }

    private boolean matchesRecipeAndSelectedOutput(InfiltratingRecipe recipe) {
        // Create's native BasinRecipe matcher already checks the filter slot
        // against the first item/fluid output and simulates all inputs.
        if (recipe.isPotionArrow()) return matchesPotionArrow(recipe);
        if (!recipe.isCatToolRepair()) return BasinRecipe.match(this, recipe);

        // Repair is a dynamic in-place output and therefore has no static JSON
        // result for Create to inspect. Treat the repaired input tool as its
        // selected product while retaining native heat and fluid requirements.
        if (findRepairableCatToolSlot() < 0
                || level == null
                || !recipe.getRequiredHeat().testBlazeBurner(
                        BasinBlockEntity.getHeatLevelOf(level.getBlockState(worldPosition.below())))) return false;
        FluidStack stored = inputTank.getPrimaryHandler().getFluid();
        return stored.getAmount() >= CatToolBehavior.REPAIR_FLUID_COST
                && stored.getFluid().isSame(LaoWuMod.HISSING_GAS.get());
    }

    private int findRepairableCatToolSlot() {
        for (int slot = 0; slot < inputInventory.getSlots(); slot++) {
            ItemStack stack = inputInventory.getStackInSlot(slot);
            if (CatToolBehavior.canRepair(stack) && getFilter().test(stack)) return slot;
        }
        return -1;
    }

    private boolean matchesPotionArrow(InfiltratingRecipe recipe) {
        if (level == null || !recipe.getRequiredHeat().testBlazeBurner(
                BasinBlockEntity.getHeatLevelOf(level.getBlockState(worldPosition.below())))) return false;

        int arrowSlot = findArrowSlot();
        FluidStack stored = inputTank.getPrimaryHandler().getFluid();
        if (arrowSlot < 0 || stored.getAmount() < InfiltratingRecipe.POTION_ARROW_FLUID_COST
                || !PotionArrowInfiltration.isUsablePotionFluid(stored)) return false;

        ItemStack result = PotionArrowInfiltration.createArrow(stored);
        return !result.isEmpty() && getFilter().test(result)
                && acceptOutputs(List.of(result), List.of(), true);
    }

    private int findArrowSlot() {
        for (int slot = 0; slot < inputInventory.getSlots(); slot++) {
            if (inputInventory.getStackInSlot(slot).is(Items.ARROW)) return slot;
        }
        return -1;
    }

    private boolean applyPotionArrow() {
        int arrowSlot = findArrowSlot();
        if (arrowSlot < 0) return false;

        var tank = inputTank.getPrimaryHandler();
        FluidStack stored = tank.getFluid();
        if (stored.getAmount() < InfiltratingRecipe.POTION_ARROW_FLUID_COST
                || !PotionArrowInfiltration.isUsablePotionFluid(stored)) return false;

        ItemStack result = PotionArrowInfiltration.createArrow(stored);
        if (result.isEmpty() || !acceptOutputs(List.of(result), List.of(), true)) return false;
        if (!inputInventory.extractItem(arrowSlot, 1, true).is(Items.ARROW)
                || tank.drain(InfiltratingRecipe.POTION_ARROW_FLUID_COST,
                IFluidHandler.FluidAction.SIMULATE).getAmount()
                != InfiltratingRecipe.POTION_ARROW_FLUID_COST) return false;

        inputInventory.extractItem(arrowSlot, 1, false);
        FluidStack drained = tank.drain(InfiltratingRecipe.POTION_ARROW_FLUID_COST,
                IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() != InfiltratingRecipe.POTION_ARROW_FLUID_COST) return false;

        boolean accepted = acceptOutputs(List.of(result), List.of(), false);
        if (accepted) notifyChangeOfContents();
        return accepted;
    }

    /**
     * Repairs the existing stack in place, preserving enchantments, custom name
     * and every other tag that a normal recipe output would otherwise replace.
     */
    private boolean applyCatToolRepair() {
        int toolSlot = findRepairableCatToolSlot();
        if (toolSlot < 0) return false;

        var tank = inputTank.getPrimaryHandler();
        FluidStack stored = tank.getFluid();
        if (stored.getAmount() < CatToolBehavior.REPAIR_FLUID_COST
                || !stored.getFluid().isSame(LaoWuMod.HISSING_GAS.get())) return false;

        FluidStack drained = tank.drain(CatToolBehavior.REPAIR_FLUID_COST,
                IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() != CatToolBehavior.REPAIR_FLUID_COST) return false;

        var tool = inputInventory.getStackInSlot(toolSlot);
        CatToolBehavior.repair(tool, CatToolBehavior.REPAIR_AMOUNT);
        inputInventory.setStackInSlot(toolSlot, tool);
        notifyChangeOfContents();
        return true;
    }

    private void moveFluidResultToPhysicalTank() {
        var output = outputTank.getPrimaryHandler();
        FluidStack produced = output.getFluid();
        if (produced.isEmpty()) return;

        int accepted = inputTank.getPrimaryHandler().fill(
                produced.copy(), IFluidHandler.FluidAction.EXECUTE);
        if (accepted > 0)
            output.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putBoolean("CurrentFacingMapping", true);
        tag.putInt("InfiltratingProgress", processingProgress);
        if (processingRecipe != null) tag.putString("InfiltratingRecipe", processingRecipe.toString());
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        currentFacingMapping = tag.getBoolean("CurrentFacingMapping");
        normalizeStoredFluidAmount();
        processingProgress = tag.getInt("InfiltratingProgress");
        processingRecipe = tag.contains("InfiltratingRecipe")
                ? ResourceLocation.tryParse(tag.getString("InfiltratingRecipe")) : null;
    }

    /**
     * Old worlds may contain up to 4000 mB from an earlier version. Forge keeps
     * that oversized stack while reading NBT even after the tank capacity is
     * lowered, so explicitly migrate it to this machine's real 1000 mB total.
     */
    private void normalizeStoredFluidAmount() {
        if (inputTank == null || outputTank == null) return;
        int remaining = FLUID_CAPACITY;
        remaining -= clampTank(inputTank, remaining);
        clampTank(outputTank, Math.max(0, remaining));
    }

    private static int clampTank(SmartFluidTankBehaviour behaviour, int allowed) {
        var tank = behaviour.getPrimaryHandler();
        FluidStack stored = tank.getFluid();
        if (stored.isEmpty()) return 0;
        int kept = Math.min(stored.getAmount(), Math.max(0, allowed));
        if (kept != stored.getAmount()) {
            if (kept == 0) {
                tank.setFluid(FluidStack.EMPTY);
            } else {
                FluidStack trimmed = stored.copy();
                trimmed.setAmount(kept);
                tank.setFluid(trimmed);
            }
        }
        return kept;
    }
}
