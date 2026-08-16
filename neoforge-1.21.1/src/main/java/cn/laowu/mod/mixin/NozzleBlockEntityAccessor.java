package cn.laowu.mod.mixin;

import com.simibubi.create.content.kinetics.fan.NozzleBlockEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** Read-only bridge to the entity cache already maintained by Create's nozzle. */
@Mixin(value = NozzleBlockEntity.class, remap = false)
public interface NozzleBlockEntityAccessor {
    @Accessor("pushingEntities")
    List<Entity> laowu$getPushingEntities();

    @Accessor("range")
    float laowu$getRange();

    @Accessor("pushing")
    boolean laowu$isPushing();
}
