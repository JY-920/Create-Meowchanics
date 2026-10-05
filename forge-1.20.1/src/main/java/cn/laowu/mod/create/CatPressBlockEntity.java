package cn.laowu.mod.create;
import java.util.Optional;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class CatPressBlockEntity extends MechanicalPressBlockEntity {
    public CatPressBlockEntity(BlockPos pos,BlockState state){super(CatMachineBlocks.PRESS_BE.get(),pos,state);}
    @Override protected Block getStressConfigKey(){return AllBlocks.MECHANICAL_PRESS.get();}
    @Override public void addBehaviours(java.util.List<com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour> list){
        super.addBehaviours(list);list.remove(pressingBehaviour);pressingBehaviour=new CatPressingBehaviour(this);list.add(pressingBehaviour);
    }
    @Override protected Optional<BasinBlockEntity> getBasin() {
        if(level==null||!CatProcessorPlacement.validTarget(level,worldPosition,getBlockState(),false))return Optional.empty();
        var be=level.getBlockEntity(worldPosition.relative(CatMachineOrientation.bottom(getBlockState()),2));
        return be instanceof BasinBlockEntity basin?Optional.of(basin):Optional.empty();
    }
    @Override public boolean tryProcessInWorld(ItemEntity item,boolean simulate){return false;}
    @Override public void tick() {
        if(level!=null&&!level.isClientSide&&!CatProcessorPlacement.validTarget(level,worldPosition,getBlockState(),false)&&pressingBehaviour.running)onBasinRemoved();
        super.tick();
    }
    @Override protected AABB createRenderBoundingBox(){return new AABB(worldPosition).inflate(2);}
}
