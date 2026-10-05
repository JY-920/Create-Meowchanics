package cn.laowu.mod.create;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
public final class CatPressBlock extends CatProcessorBlock {
    public CatPressBlock(Properties p){super(p,false);}
    @Override public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType(){return CatMachineBlocks.PRESS_BE.get();}
}

