package cn.laowu.mod.create;

import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class CatDepotBlockEntity extends DepotBlockEntity {
    public CatDepotBlockEntity(BlockPos pos, BlockState state) {
        super(CatDepotRegistration.DEPOT_BE.get(), pos, state);
    }
}
