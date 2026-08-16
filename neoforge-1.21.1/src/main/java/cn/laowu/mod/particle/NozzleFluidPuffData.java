package cn.laowu.mod.particle;

import cn.laowu.mod.LaoWuMod;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;

/** A nozzle POOF animation carrying the fluid stack used for client tinting. */
public final class NozzleFluidPuffData implements ParticleOptions {
    public static final MapCodec<NozzleFluidPuffData> CODEC = FluidStack.CODEC
            .xmap(NozzleFluidPuffData::new, NozzleFluidPuffData::fluid)
            .fieldOf("fluid");
    public static final StreamCodec<RegistryFriendlyByteBuf, NozzleFluidPuffData> STREAM_CODEC =
            FluidStack.STREAM_CODEC.map(NozzleFluidPuffData::new, NozzleFluidPuffData::fluid);

    private final FluidStack fluid;

    public NozzleFluidPuffData(FluidStack fluid) {
        this.fluid = fluid.isEmpty() ? FluidStack.EMPTY : fluid.copyWithAmount(1);
    }

    public FluidStack fluid() {
        return fluid;
    }

    @Override
    public ParticleType<?> getType() {
        return LaoWuMod.NOZZLE_FLUID_PUFF.get();
    }
}
