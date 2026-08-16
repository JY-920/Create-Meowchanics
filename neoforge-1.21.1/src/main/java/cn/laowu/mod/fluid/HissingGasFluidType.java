package cn.laowu.mod.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.function.Consumer;

/** A pipe-only, lighter-than-air pseudo gas backed by NeoForge's fluid system. */
public final class HissingGasFluidType extends FluidType {
    private static final ResourceLocation STILL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_still");
    private static final ResourceLocation FLOWING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_flow");
    // Alpha 0xCC is 80%, as requested.
    private static final int TINT = 0xCCDCECEF;

    public HissingGasFluidType() {
        super(Properties.create()
                .descriptionId("fluid_type.laowu.hissing_gas")
                .density(-1000)
                .viscosity(100)
                .temperature(300)
                .canPushEntity(false)
                .canSwim(false)
                .canDrown(false)
                .supportsBoating(false)
                .canConvertToSource(false));
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOWING_TEXTURE;
            }

            @Override
            public int getTintColor() {
                return TINT;
            }

            @Override
            public int getTintColor(FluidStack stack) {
                return TINT;
            }

            @Override
            public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                return TINT;
            }
        });
    }
}
