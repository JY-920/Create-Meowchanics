package cn.laowu.mod.create;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
public final class CatEncasedCogwheelBlock extends EncasedCogwheelBlock {
    public CatEncasedCogwheelBlock(Properties properties, boolean large) {
        super(properties, large, CatMachineBlocks.CAT_CASING::get);
    }
    @Override public BlockEntityType<? extends SimpleKineticBlockEntity> getBlockEntityType() {
        return isLarge ? CatMachineBlocks.LARGE_COG_BE.get() : CatMachineBlocks.COG_BE.get();
    }
}
