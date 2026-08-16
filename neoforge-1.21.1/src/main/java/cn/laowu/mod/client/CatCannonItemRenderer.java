package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import com.simibubi.create.CreateClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class CatCannonItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation MODEL = LaoWuMod.id("models/item/cat_cannon.bbmodel");
    private static final ResourceLocation TEXTURE = LaoWuMod.id("textures/item/cat_cannon.png");

    public CatCannonItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        float cogAngle = AnimationTickHolder.getRenderTime() * -2.5F;
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player != null) {
            boolean mainHand = player.getMainHandItem() == stack;
            boolean offHand = player.getOffhandItem() == stack;
            if (mainHand || offHand) {
                boolean leftMainArm = player.getMainArm() == HumanoidArm.LEFT;
                float shotAnimation = CreateClient.POTATO_CANNON_RENDER_HANDLER.getAnimation(
                        mainHand ^ leftMainArm, AnimationTickHolder.getPartialTicks());
                cogAngle += 360.0F * Mth.clamp(shotAnimation * 5.0F, 0.0F, 1.0F);
            }
        }
        cogAngle %= 360.0F;

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.5D, 0.5D);
        poseStack.scale(-0.9F, -0.9F, 0.9F);
        var consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        RuntimeBlockbenchModel.get(MODEL).render(poseStack, consumer, packedLight, packedOverlay,
                RuntimeBlockbenchModel.GroupSelection.ALL, RuntimeBlockbenchModel.HeadMotion.NONE,
                new RuntimeBlockbenchModel.GroupMotion("cog", 0.0F, 0.0F,
                        cogAngle * ((float) Math.PI / 180.0F)));
        poseStack.popPose();
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        RuntimeBlockbenchModel.clearCache();
    }
}
