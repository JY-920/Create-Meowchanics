package cn.laowu.mod.client;

import cn.laowu.mod.create.AdoptionPancakeDisplay;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTraitProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Renders item-only previews on the shared box floor; no client entity is created. */
final class AdoptionPancakeRenderer {
    static void render(List<ItemStack> displays, Direction facing, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        if (displays.isEmpty()) return;
        pose.pushPose();
        try {
            // The authored floor top is y=0.1875 model pixels. Slot positions rotate
            // with the box, and remain fixed when adjacent inputs are removed.
            pose.translate(.5, .1875 / 16, .5);
            pose.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
            for (int slot = 0; slot < Math.min(displays.size(), AdoptionPancakeDisplay.MAX_DISPLAYS); slot++) {
                ItemStack display = displays.get(slot);
                if (display.isEmpty()) continue;
                pose.pushPose();
                try {
                    pose.translate((slot % 3 - 1) * .30, 0, (slot / 3 - 1) * .30);
                    int bigLevel = CatTraitData.read(display).orElse(CatTraitProfile.EMPTY).level(CatTrait.BIG_CHONKY_CAT);
                    float bodyScale = bigLevel > 0 ? CatTrait.BIG_CHONKY_CAT.bigCatScalePercent(bigLevel) / 100F : 1F;
                    // The posed model is 26.89 pixels long: 0.96 * 0.17 * 26.89 / 16
                    // fits within 0.275 blocks, leaving a gap even for level-seven cats.
                    float previewScale = Math.min(.13F, .17F / bodyScale);
                    pose.scale(previewScale, previewScale, previewScale);
                    Minecraft.getInstance().getItemRenderer().renderStatic(display, ItemDisplayContext.FIXED,
                            light, overlay, pose, buffers, Minecraft.getInstance().level, slot);
                } finally {
                    pose.popPose();
                }
            }
        } finally {
            pose.popPose();
        }
    }

    private AdoptionPancakeRenderer() {}
}
