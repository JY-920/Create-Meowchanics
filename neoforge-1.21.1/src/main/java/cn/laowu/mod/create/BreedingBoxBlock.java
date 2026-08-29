package cn.laowu.mod.create;

import cn.laowu.mod.LaoWuMod;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import org.jetbrains.annotations.Nullable;

/** A front-facing, menu-backed cat-pancake breeding machine. */
public final class BreedingBoxBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final MapCodec<BreedingBoxBlock> CODEC =
            simpleCodec(properties -> new BreedingBoxBlock(BreedingBoxTier.BASIC, properties));

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
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                           BlockPos pos, Player player, InteractionHand hand,
                                           BlockHitResult hit) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof BreedingBoxBlockEntity box)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (player.isShiftKeyDown()) {
            ItemStack parent = box.extractParent();
            if (!parent.isEmpty()) {
                giveOrDrop(player, parent);
                playTransferSound(level, pos, 0.9F);
            }
            return ItemInteractionResult.CONSUME;
        }

        if (BreedingBoxBlockEntity.isAdultPancake(stack)) {
            ItemStack offered = stack.copy();
            ItemStack remainder = box.insertParent(offered);
            int inserted = offered.getCount() - remainder.getCount();
            if (inserted > 0) {
                if (!player.getAbilities().instabuild) stack.shrink(inserted);
                playTransferSound(level, pos, 1.05F);
            }
            return ItemInteractionResult.CONSUME;
        }

        if (stack.is(LaoWuMod.CAT_FOOD.get())) {
            ItemStack offered = stack.copy();
            ItemStack remainder = box.insertFood(offered);
            int inserted = offered.getCount() - remainder.getCount();
            if (inserted > 0) {
                if (!player.getAbilities().instabuild) stack.shrink(inserted);
                playTransferSound(level, pos, 1.2F);
            }
            return ItemInteractionResult.CONSUME;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(box, buffer -> buffer.writeBlockPos(pos));
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                            Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof BreedingBoxBlockEntity box)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            ItemStack parent = box.extractParent();
            if (!parent.isEmpty()) {
                giveOrDrop(player, parent);
                playTransferSound(level, pos, 0.9F);
            }
            return InteractionResult.CONSUME;
        }

        ItemStack child = box.extractChild();
        if (!child.isEmpty()) {
            giveOrDrop(player, child);
            playTransferSound(level, pos, 1.35F);
            return InteractionResult.CONSUME;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(box, buffer -> buffer.writeBlockPos(pos));
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
                box.dropContents(level, pos);
            }
            super.onRemove(oldState, level, pos, newState, moving);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (stack.get(DataComponents.CUSTOM_NAME) != null
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
        return level.isClientSide ? null
                : createTickerHelper(type, LaoWuMod.BREEDING_BOX_BE.get(),
                BreedingBoxBlockEntity::serverTick);
    }
}
