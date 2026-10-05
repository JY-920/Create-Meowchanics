package cn.laowu.mod.create;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
public final class HajiBasinBlockEntity extends BasinBlockEntity {
    public HajiBasinBlockEntity(BlockPos pos, BlockState state) {
        super(CatMachineBlocks.BASIN_BE.get(), pos, state);
    }
    @Override public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        // Only collect items intersecting the rotated interior, never adjacent inventories.
        var interior = CatMachineOrientation.rotateShape(
            net.minecraft.world.level.block.Block.box(2, 2, 2, 14, 16, 14),
            CatMachineOrientation.bottom(getBlockState())).bounds().move(worldPosition);
        for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                interior, net.minecraft.world.entity.Entity::isAlive)) {
            var remainder = net.minecraftforge.items.ItemHandlerHelper.insertItem(
                inputInventory, item.getItem().copy(), false);
            if (remainder.isEmpty()) item.discard();
            else if (remainder.getCount() != item.getItem().getCount()) item.setItem(remainder);
        }
    }
}
