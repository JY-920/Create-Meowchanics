package cn.laowu.mod.mixin;
import cn.laowu.mod.create.*;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=BasinBlockEntity.class,remap=false)
public abstract class CatBasinFilterMixin {
    @ModifyArg(method="addBehaviours",at=@At(value="INVOKE",target="Lcom/simibubi/create/foundation/blockEntity/behaviour/filtering/FilteringBehaviour;<init>(Lcom/simibubi/create/foundation/blockEntity/SmartBlockEntity;Lcom/simibubi/create/foundation/blockEntity/behaviour/ValueBoxTransform;)V"),index=1)
    private ValueBoxTransform laowu$filterFrame(ValueBoxTransform original){return (Object)this instanceof HajiBasinBlockEntity?new CatBasinFilter():original;}
}

