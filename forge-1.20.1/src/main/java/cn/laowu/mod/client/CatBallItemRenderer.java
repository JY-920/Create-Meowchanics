package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.entity.CatBallEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class CatBallItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final net.minecraft.resources.ResourceLocation MODEL = LaoWuMod.id("models/item/cat_ball.bbmodel");
    private static final net.minecraft.resources.ResourceLocation TEXTURE = LaoWuMod.id("textures/item/cat_ball.png");
    private static final float DROPPED_ITEM_SCALE = CatBallEntity.WORLD_SCALE * 0.5F;
    private static final float FILTER_SLOT_SCALE = 0.85F;
    private static final float MODEL_BODY_CENTER_X = 1.5F / 16.0F;
    private static final float MODEL_BODY_CENTER_Y = (24.0F - 2.5F) / 16.0F;
    private static final float MODEL_BODY_CENTER_Z = 1.5F / 16.0F;

    public CatBallItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) { super(dispatcher, models); }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        boolean filterSlotView = context == ItemDisplayContext.FIXED;
        if (filterSlotView) {
            // ValueBoxRenderer already positions and scales the slot itself.
            // Centre the model around its body in local space, turn its face
            // out of the panel, and avoid every GUI/K-editor transform.
            pose.translate(0.5D, 0.5D, 0.5D);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            pose.scale(-FILTER_SLOT_SCALE, -FILTER_SLOT_SCALE, FILTER_SLOT_SCALE);
            pose.translate(-MODEL_BODY_CENTER_X, -MODEL_BODY_CENTER_Y, -MODEL_BODY_CENTER_Z);
            renderModel(pose, buffers, light, overlay);
            pose.popPose();
            return;
        }

        boolean iconView = context == ItemDisplayContext.GUI;
        boolean ballEntityView = context == ItemDisplayContext.NONE;
        float scale = iconView ? 1.48F
                : ballEntityView ? CatBallEntity.WORLD_SCALE
                : context == ItemDisplayContext.GROUND ? DROPPED_ITEM_SCALE : 1.25F;
        // RuntimeBlockbenchModel uses the vanilla 24px entity root. The model
        // itself has a -0.5835px rotated tail tip, so this exact compensation
        // places its true lowest vertex on the entity's Y=0 plane.
        // ItemRenderer first translates every custom model by -0.5. Include
        // that offset as well as the scaled 24px root compensation.
        double groundY = 0.5D + scale
                * (1.5D - CatBallEntity.MODEL_MIN_Y_PIXELS / 16.0D);
        pose.translate(0.5D,
                context == ItemDisplayContext.GROUND || ballEntityView ? groundY : 1.28D,
                0.5D);
        if (iconView) {
            pose.mulPose(Axis.YP.rotationDegrees(35.0F));
            pose.mulPose(Axis.XP.rotationDegrees(-18.0F));
        }
        pose.scale(-scale, -scale, scale);
        renderModel(pose, buffers, light, overlay);
        pose.popPose();
    }

    private static void renderModel(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        RuntimeBlockbenchModel.get(MODEL).render(pose,
                buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay,
                RuntimeBlockbenchModel.GroupSelection.ALL, RuntimeBlockbenchModel.HeadMotion.NONE);
    }

    @Override public void onResourceManagerReload(ResourceManager manager) { RuntimeBlockbenchModel.clearCache(); }
}
