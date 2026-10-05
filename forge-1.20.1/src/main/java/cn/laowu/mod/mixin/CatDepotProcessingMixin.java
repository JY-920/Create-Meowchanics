package cn.laowu.mod.mixin;

import cn.laowu.mod.create.*;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=DepotBehaviour.class,remap=false)
public abstract class CatDepotProcessingMixin extends BlockEntityBehaviour {
    @Shadow TransportedItemStack heldItem;
    protected CatDepotProcessingMixin(SmartBlockEntity be) { super(be); }
    @Redirect(method="tick()V",at=@At(value="INVOKE",target="Lnet/minecraft/core/BlockPos;above(I)Lnet/minecraft/core/BlockPos;",remap=true))
    private BlockPos laowu$processorPosition(BlockPos pos,int distance) {
        return blockEntity instanceof CatDepotBlockEntity ? pos.relative(CatMachineOrientation.top(blockEntity.getBlockState()),distance) : pos.above(distance);
    }
    @Redirect(method="tick()V",at=@At(value="INVOKE",target="Lcom/simibubi/create/content/kinetics/belt/behaviour/BeltProcessingBehaviour;isBlocked(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean laowu$localGap(net.minecraft.world.level.BlockGetter level,BlockPos pos) {
        if(!(blockEntity instanceof CatDepotBlockEntity))return BeltProcessingBehaviour.isBlocked(level,pos);
        var state=blockEntity.getBlockState();
        var gap=pos.relative(CatMachineOrientation.top(state));
        var processor=level.getBlockState(pos.relative(CatMachineOrientation.top(state),2));
        return CatMachineOrientation.bottom(processor)!=CatMachineOrientation.bottom(state) || !level.getBlockState(gap).getCollisionShape(level,gap).isEmpty();
    }
    @Inject(method="tick()V",at=@At("HEAD"))
    private void laowu$releaseInvalidTarget(CallbackInfo ci) {
        if(blockEntity instanceof CatDepotBlockEntity && heldItem!=null && heldItem.locked && laowu$localGap(getWorld(),getPos()))heldItem.locked=false;
    }
}
