package cn.laowu.mod.client;

import cn.laowu.mod.create.InfiltrationTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.createmod.catnip.platform.ForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraftforge.fluids.FluidStack;

/** Uses Create's BasinRenderer verbatim for items, overriding only the 1000 mB liquid bounds. */
public final class InfiltrationTankRenderer extends BasinRenderer {
    public InfiltrationTankRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected float renderFluids(BasinBlockEntity basin, float partialTicks, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        SmartFluidTankBehaviour input = basin.getBehaviour(SmartFluidTankBehaviour.INPUT);
        SmartFluidTankBehaviour output = basin.getBehaviour(SmartFluidTankBehaviour.OUTPUT);
        float total = basin.getTotalFluidUnits(partialTicks);
        if (total < 1.0F) return 0.0F;

        float fill = Mth.clamp(total / InfiltrationTankBlockEntity.FLUID_CAPACITY, 0.0F, 1.0F);
        float curved = 1.0F - (1.0F - fill) * (1.0F - fill);
        float liquidHeight = 0.125F + 0.75F * curved;
        float cursor = 0.125F;
        float itemSurface = liquidHeight;

        for (SmartFluidTankBehaviour behaviour : new SmartFluidTankBehaviour[]{input, output}) {
            if (behaviour == null) continue;
            for (SmartFluidTankBehaviour.TankSegment segment : behaviour.getTanks()) {
                FluidStack fluid = segment.getRenderedFluid();
                float units = segment.getTotalUnits(partialTicks);
                if (fluid.isEmpty() || units < 1.0F) continue;

                float share = units / total;
                float segmentHeight = 0.75F * curved * share;
                boolean lighter = fluid.getFluid().getFluidType().isLighterThanAir();
                float bottom = lighter ? 0.875F - segmentHeight : cursor;
                float top = lighter ? 0.875F : cursor + segmentHeight;
                // 0.01 .. 0.99 occupies 98% of the block's width and length.
                ForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid,
                        0.01F, bottom, 0.01F, 0.99F, top, 0.99F,
                        buffers, pose, light, false, false);
                if (lighter) itemSurface = bottom;
                else cursor = top;
            }
        }
        // BasinRenderer places items at (returned surface - 0.3 + 0.2).
        // Capping the returned surface at 0.70 therefore caps the final item
        // pivot at exactly 60% of the block height.
        return Math.min(itemSurface, 0.70F);
    }
}
