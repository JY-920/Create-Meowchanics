package cn.laowu.mod.mixin;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Narrow state bridge used only while a fire cat touches a Blaze Burner. */
@Mixin(value = BlazeBurnerBlockEntity.class, remap = false)
public interface BlazeBurnerBlockEntityAccessor {
    @Accessor("activeFuel")
    void laowu$setActiveFuel(BlazeBurnerBlockEntity.FuelType fuel);

    @Accessor("remainingBurnTime")
    void laowu$setRemainingBurnTime(int ticks);
}
