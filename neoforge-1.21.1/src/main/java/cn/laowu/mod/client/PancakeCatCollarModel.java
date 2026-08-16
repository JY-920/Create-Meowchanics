package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

/** Draws a complete red collar ring around the flattened cat's neck. */
public final class PancakeCatCollarModel {
    private static final ResourceLocation MODEL = LaoWuMod.id(
            "models/entity/cat_pancake_collar.bbmodel");
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/misc/white.png");

    public static void render(PoseStack poseStack, MultiBufferSource buffer,
                              int packedLight, int packedOverlay) {
        int packed = DyeColor.RED.getTextureDiffuseColor();
        float[] color = new float[] {
                (packed >> 16 & 0xFF) / 255.0F,
                (packed >> 8 & 0xFF) / 255.0F,
                (packed & 0xFF) / 255.0F
        };
        int red = Mth.clamp((int) (color[0] * 255.0F), 0, 255);
        int green = Mth.clamp((int) (color[1] * 255.0F), 0, 255);
        int blue = Mth.clamp((int) (color[2] * 255.0F), 0, 255);
        var consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        RuntimeBlockbenchModel.get(MODEL).render(
                poseStack, consumer, packedLight, packedOverlay,
                RuntimeBlockbenchModel.GroupSelection.ALL_GROUPS,
                RuntimeBlockbenchModel.HeadMotion.NONE,
                red, green, blue, 255);
    }

    private PancakeCatCollarModel() {
    }
}
