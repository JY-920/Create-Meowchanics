package cn.laowu.mod.particle;

import cn.laowu.mod.LaoWuMod;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.simibubi.create.foundation.utility.CreateCodecs;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

/** A nozzle POOF animation carrying the full fluid stack for client tinting. */
public final class NozzleFluidPuffData implements ParticleOptions {
    public static final Codec<NozzleFluidPuffData> CODEC =
            CreateCodecs.FLUID_STACK_CODEC.xmap(NozzleFluidPuffData::new,
                    NozzleFluidPuffData::fluid);

    public static final Deserializer<NozzleFluidPuffData> DESERIALIZER =
            new Deserializer<>() {
                @Override
                public NozzleFluidPuffData fromCommand(
                        ParticleType<NozzleFluidPuffData> type, StringReader reader)
                        throws CommandSyntaxException {
                    reader.expect(' ');
                    ResourceLocation id = ResourceLocation.read(reader);
                    Fluid fluid = ForgeRegistries.FLUIDS.getValue(id);
                    return new NozzleFluidPuffData(fluid == null
                            ? FluidStack.EMPTY : new FluidStack(fluid, 1));
                }

                @Override
                public NozzleFluidPuffData fromNetwork(
                        ParticleType<NozzleFluidPuffData> type, FriendlyByteBuf buffer) {
                    return new NozzleFluidPuffData(buffer.readFluidStack());
                }
            };

    private final FluidStack fluid;

    public NozzleFluidPuffData(FluidStack fluid) {
        this.fluid = fluid.copy();
        this.fluid.setAmount(this.fluid.isEmpty() ? 0 : 1);
    }

    public FluidStack fluid() {
        return fluid;
    }

    @Override
    public ParticleType<?> getType() {
        return LaoWuMod.NOZZLE_FLUID_PUFF.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeFluidStack(fluid);
    }

    @Override
    public String writeToString() {
        ResourceLocation typeId = ForgeRegistries.PARTICLE_TYPES.getKey(getType());
        ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
        return String.format(Locale.ROOT, "%s %s", typeId, fluidId);
    }
}
