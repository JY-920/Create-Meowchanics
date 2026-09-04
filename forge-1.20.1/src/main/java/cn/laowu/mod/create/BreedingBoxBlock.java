package cn.laowu.mod.create;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.BreedingCatFoodItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/** A front-facing, menu-backed cat-pancake breeding machine. */
public final class BreedingBoxBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final BreedingBoxTier tier;

    public BreedingBoxBlock(BreedingBoxTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public BreedingBoxTier tier() {
        return tier;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Exact supplied Blockbench geometry and UVs are rendered by the BER.
        return RenderShape.INVISIBLE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof BreedingBoxBlockEntity box)) {
            return InteractionResult.PASS;
        }
        if (tier == BreedingBoxTier.BASIC && player instanceof FakePlayer) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            ItemStack extracted = box.extractManual();
            if (!extracted.isEmpty()) {
                giveOrDrop(player, extracted);
                playTransferSound(level, pos, 0.9F);
            }
            return InteractionResult.CONSUME;
        }

        if (BreedingBoxBlockEntity.isAdultPancake(held)) {
            ItemStack offered = held.copy();
            ItemStack remainder = box.insertParent(offered);
            int inserted = offered.getCount() - remainder.getCount();
            if (inserted > 0) {
                if (!player.getAbilities().instabuild) held.shrink(inserted);
                playTransferSound(level, pos, 1.05F);
            }
            return InteractionResult.CONSUME;
        }

        if (BreedingCatFoodItem.isBreedingFood(held)) {
            ItemStack offered = held.copy();
            ItemStack remainder = box.insertFood(offered);
            int inserted = offered.getCount() - remainder.getCount();
            if (inserted > 0) {
                if (!player.getAbilities().instabuild) held.shrink(inserted);
                playTransferSound(level, pos, 1.2F);
            }
            return InteractionResult.CONSUME;
        }

        // Every non-sneaking interaction with something other than a valid
        // parent pancake or dedicated breeding food opens the status panel.
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, box, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.addItem(stack)) player.drop(stack, false);
    }

    private static void playTransferSound(Level level, BlockPos pos, float pitch) {
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS,
                0.45F, pitch);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos,
                         BlockState newState, boolean moving) {
        if (!oldState.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof BreedingBoxBlockEntity box) {
                if (!level.isClientSide) {
                    box.dropContents(level, pos);
                    box.destroy();
                }
            }
            super.onRemove(oldState, level, pos, newState, moving);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (stack.hasCustomHoverName()
                && level.getBlockEntity(pos) instanceof BreedingBoxBlockEntity box) {
            box.setCustomName(stack.getHoverName());
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BreedingBoxBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, LaoWuMod.BREEDING_BOX_BE.get(),
                BreedingBoxBlockEntity::tickBox);
    }
}
