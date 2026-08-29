package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTraitEffects;
import cn.laowu.mod.genetics.CatTraitInstance;
import cn.laowu.mod.genetics.CatTraitProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.List;

/** Renders the supplied scanner and a live attribute panel directly on its screen. */
public final class CatScannerItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation MODEL =
            LaoWuMod.id("models/item/cat_scanner.bbmodel");
    private static final String SCREEN_SHELL =
            "ae88b825-bbda-bfbf-b5f6-a85cce44ba8a";
    public CatScannerItemRenderer(BlockEntityRenderDispatcher dispatcher,
                                  EntityModelSet models) {
        super(dispatcher, models);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        CatWorldTarget target = targetedTarget(stack, context);
        CatScannerTextureManager.Layers bodyTextures =
                CatScannerTextureManager.resolveInactive();
        CatTraitProfile traits = CatTraitProfile.EMPTY;
        if (target != null) {
            var profile = target.isLiving()
                    ? CatAttributeData.read(target.cat())
                    : CatAttributeData.read(target.pancake());
            traits = target.isLiving()
                    ? CatTraitData.read(target.cat()).orElse(CatTraitProfile.EMPTY)
                    : CatTraitData.read(target.pancake()).orElse(CatTraitProfile.EMPTY);
            if (profile.isPresent()) {
                bodyTextures = target.isLiving()
                        ? CatScannerTextureManager.resolve(target.cat(), profile.get(), traits)
                        : CatScannerTextureManager.resolve(profile.get(), traits,
                        CatTraitEffects.isNight(Minecraft.getInstance().level),
                        CatTraitEffects.isDay(Minecraft.getInstance().level));
            } else {
                bodyTextures = CatScannerTextureManager.resolveActive();
            }
        }

        pose.pushPose();
        // ItemRenderer applies the builtin/entity half-block offset first.
        // Centre the supplied raw bounds (-13..9, 0..2, -2..10) explicitly.
        pose.translate(0.5D, 1.0D, 0.5D);
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.125D, 0.0625D, -0.25D);
        RuntimeBlockbenchModel.get(MODEL).renderReversedWinding(pose,
                buffers.getBuffer(RenderType.entityCutoutNoCull(bodyTextures.opaque())),
                light, overlay, RuntimeBlockbenchModel.GroupSelection.ALL,
                RuntimeBlockbenchModel.HeadMotion.NONE);
        if (bodyTextures.translucent() != null) {
            // Only the authored screen shell owns partially transparent
            // pixels. Drawing the complete model again makes shader packs
            // treat empty texels on every accessory cube as opaque squares.
            RuntimeBlockbenchModel.get(MODEL).renderExactElementReversedWinding(pose,
                    buffers.getBuffer(RenderType.entityTranslucent(
                            bodyTextures.translucent())),
                    light, overlay, SCREEN_SHELL);
        }

        if (target != null && !traits.traits().isEmpty()) {
            renderTraitTitles(pose, buffers, traits);
        }

        pose.popPose();
    }

    private static CatWorldTarget targetedTarget(ItemStack stack,
                                                  ItemDisplayContext context) {
        if (context != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                && context != ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                && context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                && context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND) return null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || (minecraft.player.getMainHandItem() != stack
                && minecraft.player.getOffhandItem() != stack)) return null;
        return CatWorldTarget.find(minecraft, 5.0D);
    }

    /** Draws localized trait titles exactly on the same authored glass face. */
    private static void renderTraitTitles(PoseStack pose, MultiBufferSource buffers,
                                          CatTraitProfile traits) {
        Font font = Minecraft.getInstance().font;
        float scale = 1.0F / (CatScannerTextureManager.TEXTURE_SCALE * 16.0F);

        // Portrait X follows model -Z while portrait Y follows model -X, the
        // same corrected orientation baked into the top glass texture. The
        // runtime bbmodel renderer shifts direct-root geometry down from its
        // 24 px entity pivot; include that exact 24-2 px Y position here so
        // glyphs sit on their cards instead of floating 24 pixels away.
        Matrix4f screen = new Matrix4f();
        screen.m00(0.0F).m01(0.0F).m02(-scale).m03(0.0F);
        screen.m10(-scale).m11(0.0F).m12(0.0F).m13(0.0F);
        screen.m20(0.0F).m21(scale).m22(0.0F).m23(0.0F);
        screen.m30(8.0F / 16.0F).m31(22.0F / 16.0F)
                .m32(9.0F / 16.0F).m33(1.0F);

        pose.pushPose();
        pose.mulPoseMatrix(screen);
        List<CatTraitInstance> entries = traits.traits();
        for (int index = 0; index < entries.size()
                && index < CatTraitProfile.MAX_TRAITS; index++) {
            CatTraitInstance instance = entries.get(index);
            int cardY = CatScannerTextureManager.TRAIT_Y
                    + index * CatScannerTextureManager.TRAIT_SPACING;
            float availableWidth = CatScannerTextureManager.TRAIT_WIDTH - 8.0F;
            float titleScale = Math.min(0.75F,
                    availableWidth / Math.max(1.0F, font.width(instance.trait().title())));
            float centreX = CatScannerTextureManager.TRAIT_X
                    + 5.0F + availableWidth / 2.0F;
            float centreY = cardY + CatScannerTextureManager.TRAIT_HEIGHT / 2.0F;

            pose.pushPose();
            pose.translate(centreX, centreY, 0.0F);
            pose.scale(titleScale, titleScale, 1.0F);
            font.drawInBatch(instance.trait().title(),
                    -font.width(instance.trait().title()) / 2.0F,
                    -font.lineHeight / 2.0F,
                    instance.trait().rarity().cardTextColour(), false,
                    pose.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET,
                    0, LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
        pose.popPose();
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        CatScannerTextureManager.clear();
        RuntimeBlockbenchModel.clearCache();
    }
}
