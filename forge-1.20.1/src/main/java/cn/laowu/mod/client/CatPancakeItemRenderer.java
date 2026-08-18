package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.CatPancakeItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class CatPancakeItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            LaoWuMod.MOD_ID, "models/item/cat_pancake.bbmodel");
    /** Highest downward-facing model coordinate after the Blockbench root transform. */
    private static final float MODEL_FLOOR_Y = 1.25625F;
    private static final float SURFACE_CLEARANCE = 0.01F;
    /**
     * ItemRenderer translates every custom model down by half a block, while
     * ItemEntityRenderer only restores a quarter block for GROUND rendering.
     * Compensate the remaining quarter block for dropped items here.
     */
    private static final float DROPPED_ITEM_COMPENSATION = 0.25F;
    /** FIXED has no quarter-block restoration, so cancel ItemRenderer's full -0.5 translation. */
    private static final float CREATE_DISPLAY_COMPENSATION = 0.5F;

    public CatPancakeItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        float scale = switch (context) {
            case GUI -> 0.72F;
            case GROUND -> 0.48F;
            // Create renders transported items with FIXED and applies another
            // 0.5 scale first. Doubling this context keeps the pancake at its
            // intended world size on belts, depots, drains and processing blocks.
            case FIXED -> 0.96F;
            default -> 0.62F;
        };
        boolean baby = CatPancakeItem.isBaby(stack);
        if (baby) {
            // A kitten pancake remains visibly smaller in-world, but no longer
            // becomes an unreadably tiny half-scale icon in the GUI or either hand.
            scale *= switch (context) {
                case GROUND, FIXED -> 0.75F;
                default -> 0.80F;
            };
        }

        double offsetX = context == ItemDisplayContext.GUI ? -0.01D : 0.0D;
        double offsetY = context == ItemDisplayContext.GUI ? -0.29D : 0.0D;
        double y;
        if (context == ItemDisplayContext.GROUND || context == ItemDisplayContext.FIXED) {
            // RuntimeBlockbenchModel inverts entity-model Y. Deriving the
            // translation from the final scale places both sizes on the same plane.
            float contextCompensation = context == ItemDisplayContext.GROUND
                    ? DROPPED_ITEM_COMPENSATION : CREATE_DISPLAY_COMPENSATION;
            y = scale * MODEL_FLOOR_Y + SURFACE_CLEARANCE + contextCompensation;
        } else {
            y = 1.32D + offsetY
                    - (baby ? (context == ItemDisplayContext.GUI ? 0.16D : 0.14D) : 0.0D);
        }
        poseStack.translate(0.5D + offsetX, y, 0.5D);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.YP.rotationDegrees(111.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-34.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(52.0F));
        }
        poseStack.scale(-scale, -scale, scale);
        var consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(
                CatGenomeTextureManager.resolve(stack, CatPancakeItem.texture(stack))));
        RuntimeBlockbenchModel.get(MODEL).render(
                poseStack, consumer, packedLight, packedOverlay,
                RuntimeBlockbenchModel.GroupSelection.ALL,
                RuntimeBlockbenchModel.HeadMotion.NONE);
        if (CatPancakeItem.isTamed(stack)) {
            PancakeCatCollarModel.render(poseStack, buffer, packedLight, packedOverlay);
        }
        if (CatPancakeItem.hasOutfit(stack)) {
            TerminatorPancakeModel.render(poseStack, buffer, packedLight, packedOverlay,
                    CatPancakeItem.getOutfit(stack));
        }
        poseStack.popPose();
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        RuntimeBlockbenchModel.clearCache();
    }
}
