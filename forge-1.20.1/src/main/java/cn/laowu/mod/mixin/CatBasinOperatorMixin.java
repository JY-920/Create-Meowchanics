package cn.laowu.mod.mixin;
import cn.laowu.mod.create.*;
import com.simibubi.create.content.processing.basin.*;
import java.util.Optional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Only rejects wrong-facing cat basins; every ordinary Create basin keeps its native behaviour. */
@Mixin(value=BasinOperatingBlockEntity.class,remap=false)
public abstract class CatBasinOperatorMixin {
    @Inject(method="getBasin",at=@At("RETURN"),cancellable=true)
    private void laowu$rejectSideWall(CallbackInfoReturnable<Optional<BasinBlockEntity>> ci){
        var operator=(BasinOperatingBlockEntity)(Object)this;
        ci.setReturnValue(ci.getReturnValue().filter(basin->!(basin instanceof HajiBasinBlockEntity)
            ||CatMachineOrientation.bottom(basin.getBlockState())==CatMachineOrientation.bottom(operator.getBlockState())));
    }
}

