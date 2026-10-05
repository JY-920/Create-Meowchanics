package cn.laowu.mod.create;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
public final class CatMixerBlock extends CatProcessorBlock {
    public CatMixerBlock(Properties p){super(p,true);}
    @Override public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType(){return CatMachineBlocks.MIXER_BE.get();}
}

