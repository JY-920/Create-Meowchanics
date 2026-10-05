package cn.laowu.mod.create;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedShaftBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
public final class CatEncasedShaftBlock extends EncasedShaftBlock {
    public CatEncasedShaftBlock(Properties properties) { super(properties, CatMachineBlocks.CAT_CASING::get); }
    @Override public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
        return CatMachineBlocks.SHAFT_BE.get();
    }
}
