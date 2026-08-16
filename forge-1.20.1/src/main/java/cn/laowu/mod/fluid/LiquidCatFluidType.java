package cn.laowu.mod.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;

import java.util.function.Consumer;

/** Opaque, pale honey-coloured Hakimi Honey with lava-like physical flow settings. */
public final class LiquidCatFluidType extends FluidType {
    private static final ResourceLocation STILL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("laowu", "fluid/hakimi_honey_still");
    private static final ResourceLocation FLOWING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("laowu", "fluid/hakimi_honey_flow");
    // The copied Create honey frames already contain the colour and highlights.
    // A neutral tint preserves that authored material instead of recolouring it.
    private static final int TINT = 0xFFFFFFFF;

    public LiquidCatFluidType() {
        super(Properties.create()
                .descriptionId("fluid_type.laowu.liquid_cat")
                .density(3000)
                .viscosity(6000)
                .temperature(300)
                .lightLevel(0)
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
