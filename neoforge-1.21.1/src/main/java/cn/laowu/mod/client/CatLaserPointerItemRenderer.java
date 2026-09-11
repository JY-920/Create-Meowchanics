package cn.laowu.mod.client;

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

/** Pen and beam share the authored muzzle and the complete current-frame hand transform. */
public final class CatLaserPointerItemRenderer extends BlockEntityWithoutLevelRenderer {
    public CatLaserPointerItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5D, 0.5D, 0.5D);
        float scale;
        if (context == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            scale = 0.68F;
        } else if (context == ItemDisplayContext.GROUND) {
            scale = 0.24F;
        } else if (context == ItemDisplayContext.FIXED) {
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            scale = 0.47F;
        } else {
            if (!LaserPointerAim.orient(context, pose))
                pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            scale = 0.8F;
        }
        pose.scale(scale, scale, scale);
        // The pen body is y=0..3.2 pixels. Centre its actual body, not the 24px root pivot.
        pose.translate(-0.03125D, 1.40625D, 0.125D);
        pose.scale(1.0F, -1.0F, 1.0F);
        RuntimeBlockbenchModel.get(cn.laowu.mod.LaoWuMod.id("models/item/cat_laser_pointer.bbmodel")).render(pose,
                buffers.getBuffer(RenderType.entityCutoutNoCull(cn.laowu.mod.LaoWuMod.id("textures/item/cat_laser_pointer_off.png"))),
                light, overlay, RuntimeBlockbenchModel.GroupSelection.ALL,
                RuntimeBlockbenchModel.HeadMotion.NONE);
        if (LaserPointerAim.local(context)) {
            var target = LaserPointerAim.target(context, pose);
            if (target != null) {
                // Authored exit: (0.5, 1.5, 8) px, in the runtime model's inverted-Y 24px root.
                var muzzle = new org.joml.Vector3f(0.03125F, 1.40625F, 0.5F);
                var direction = target.sub(muzzle);
                float length = direction.length();
                // A wall closer than the muzzle must hide the beam, never send it backwards.
                if (length > .001F && direction.z > .001F) {
                    pose.pushPose();
                    pose.translate(muzzle.x, muzzle.y, muzzle.z);
                    pose.mulPose(new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0,0,1), direction.normalize()));
                    CatLaserEffects.solidBeam(pose, buffers.getBuffer(RenderType.debugQuads()), length,
                            cn.laowu.mod.item.CatLaserPointerItem.color(stack));
                    pose.popPose();
                }
            }
        }
        pose.popPose();
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        RuntimeBlockbenchModel.clearCache();
    }
}
