package cn.laowu.mod.item;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolActions;

/** Enhanced use tills a player-facing 3x3 area with the clicked block at bottom-centre. */
public final class CatHoeItem extends HoeItem {
    public CatHoeItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        return ingredient.is(LaoWuMod.CAT_INGOT.get());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!CatToolBehavior.isEmpowered(stack) || context.getPlayer() == null
                || context.getClickedFace() == Direction.DOWN) {
            return super.useOn(context);
        }

        Level level = context.getLevel();
        Direction forward = context.getPlayer().getDirection();
        Direction side = forward.getClockWise();
        boolean changed = false;

        // Depth zero is the row nearest the player, making the clicked block
        // the bottom-centre cell of the player-facing 3x3 field.
        for (int depth = 0; depth < 3; depth++) {
            for (int lateral = -1; lateral <= 1; lateral++) {
                BlockPos target = context.getClickedPos()
                        .relative(forward, depth)
                        .relative(side, lateral);
                BlockHitResult hit = new BlockHitResult(
                        Vec3.atCenterOf(target).add(0.0D, 0.5D, 0.0D),
                        Direction.UP, target, false);
                UseOnContext targetContext = new UseOnContext(level, context.getPlayer(),
                        context.getHand(), stack, hit);
                BlockState oldState = level.getBlockState(target);
                BlockState newState = oldState.getToolModifiedState(
                        targetContext, ToolActions.HOE_TILL, level.isClientSide);

                if (newState != null) {
                    changed = true;
                    if (!level.isClientSide) {
                        newState = hydrate(newState);
                        level.setBlock(target, newState, 11);
                        level.gameEvent(GameEvent.BLOCK_CHANGE, target,
                                GameEvent.Context.of(context.getPlayer(), newState));
                    }
                    continue;
                }

                // Already-tilled farmland is still part of automatic watering.
                if (oldState.hasProperty(FarmBlock.MOISTURE)) {
                    changed = true;
                    if (!level.isClientSide && oldState.getValue(FarmBlock.MOISTURE) < FarmBlock.MAX_MOISTURE) {
                        BlockState wet = hydrate(oldState);
                        level.setBlock(target, wet, 2);
                        level.gameEvent(GameEvent.BLOCK_CHANGE, target,
                                GameEvent.Context.of(context.getPlayer(), wet));
                    }
                }
            }
        }

        if (!changed) return InteractionResult.PASS;
        level.playSound(context.getPlayer(), context.getClickedPos(), SoundEvents.HOE_TILL,
                SoundSource.BLOCKS, 1.0F, 1.0F);
        if (!level.isClientSide) {
            // One enhanced activation costs one normal use; the shared ItemStack
            // hook turns that into exactly three hiss points.
            stack.hurtAndBreak(1, context.getPlayer(),
                    player -> player.broadcastBreakEvent(context.getHand()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static BlockState hydrate(BlockState state) {
        return state.hasProperty(FarmBlock.MOISTURE)
                ? state.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE)
                : state;
    }
}
