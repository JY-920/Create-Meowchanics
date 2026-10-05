package cn.laowu.mod.mixin;
import cn.laowu.mod.create.*;
import com.simibubi.create.content.processing.basin.*;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;

@Mixin(value=BasinBlockEntity.class,remap=false)
public abstract class CatBasinOrientationMixin extends SmartBlockEntity {
    protected CatBasinOrientationMixin(BlockEntityType<?> t,BlockPos p,BlockState s){super(t,p,s);}
    @ModifyArg(method={"getOperator","lazyTick","tick","tryClearingSpoutputOverflow","acceptOutputsInner"},
        at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;",remap=true),index=0)
    private BlockPos laowu$entityPosition(BlockPos pos){
        return (Object)this instanceof HajiBasinBlockEntity?CatMachineOrientation.rotatePosition(worldPosition,pos,getBlockState()):pos;
    }
    @ModifyArg(method={"getHeatLevel","tick"},
        at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",remap=true),index=0)
    private BlockPos laowu$statePosition(BlockPos pos){return laowu$entityPosition(pos);}
    @Redirect(method="updateSpoutput",at=@At(value="INVOKE",target="Lcom/simibubi/create/content/processing/basin/BasinBlock;canOutputTo(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"))
    private boolean laowu$spout(BlockGetter world,BlockPos pos,Direction face){return CatBasinOutput.canOutputTo(world,pos,face);}
    @Inject(method="getOperator",at=@At("RETURN"),cancellable=true)
    private void laowu$alignedOperator(CallbackInfoReturnable<Optional<BasinOperatingBlockEntity>> ci){
        if(!((Object)this instanceof HajiBasinBlockEntity))return;
        ci.setReturnValue(ci.getReturnValue().filter(be->CatMachineOrientation.bottom(be.getBlockState())==CatMachineOrientation.bottom(getBlockState())));
    }
    @ModifyArg(method={"tryClearingSpoutputOverflow","acceptOutputsInner"},
        at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getCapability(Lnet/neoforged/neoforge/capabilities/BlockCapability;Lnet/minecraft/core/BlockPos;Ljava/lang/Object;)Ljava/lang/Object;"),index=2)
    private Object laowu$capabilitySide(Object side){
        return (Object)this instanceof HajiBasinBlockEntity && side instanceof Direction d?CatMachineOrientation.toWorld(CatMachineOrientation.bottom(getBlockState()),d):side;
    }
}

