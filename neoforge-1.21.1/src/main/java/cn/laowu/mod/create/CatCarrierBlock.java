package cn.laowu.mod.create;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import java.util.List;

public final class CatCarrierBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final com.mojang.serialization.MapCodec<CatCarrierBlock> CODEC = simpleCodec(CatCarrierBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    public CatCarrierBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) {
        return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
    }
    @Override public BlockState rotate(BlockState s, Rotation r) { return s.setValue(FACING, r.rotate(s.getValue(FACING))); }
    @Override public BlockState mirror(BlockState s, Mirror m) { return rotate(s, m.getRotation(s.getValue(FACING))); }
    @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new CatCarrierBlockEntity(p, s); }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack held, BlockState s, Level l, BlockPos p,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (l.getBlockEntity(p) instanceof CatCarrierBlockEntity box && box.interact(player, hand).consumesAction())
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos p, Player player, BlockHitResult hit) {
        return l.getBlockEntity(p) instanceof CatCarrierBlockEntity box
                ? box.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND) : InteractionResult.PASS;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> type) {
        return l.isClientSide ? null : createTickerHelper(type, LaoWuMod.CAT_CARRIER_BE.get(), CatCarrierBlockEntity::tick);
    }
    @Override public List<ItemStack> getDrops(BlockState s, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        BlockEntity entity = params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        return List.of(entity instanceof CatCarrierBlockEntity box ? box.portableStack() : new ItemStack(this));
    }
    @Override public BlockState playerWillDestroy(Level l, BlockPos p, BlockState s, Player player) {
        if (!l.isClientSide && player.getAbilities().instabuild && l.getBlockEntity(p) instanceof CatCarrierBlockEntity box
                && (!box.catStack().isEmpty() || !box.tank.isEmpty())) popResource(l, p, box.portableStack());
        return super.playerWillDestroy(l, p, s, player);
    }
}
