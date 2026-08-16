package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** The 3-D cat model is exclusive to an already-fired cat grenade. */
public final class CatGrenadeProjectileModel {
    private static final ResourceLocation MODEL =
            LaoWuMod.id("models/item/cat_grenade.bbmodel");
    private static final ResourceLocation TEXTURE =
            LaoWuMod.id("textures/item/cat_grenade_3d.png");

    private CatGrenadeProjectileModel() { }

    public static void render(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        // Equivalent to the old GROUND item transform after Create's
        // PotatoProjectileRenderer has applied its trajectory transform.
        pose.translate(0.0D, 0.88D, 0.0D);
        pose.scale(-0.58F, -0.58F, 0.58F);
        // The supplied model is centred around X=-5 rather than X=0.
        pose.translate(5.0D / 16.0D, 0.0D, 0.0D);
        RuntimeBlockbenchModel.get(MODEL).render(pose,
                buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay,
                RuntimeBlockbenchModel.GroupSelection.ALL,
                RuntimeBlockbenchModel.HeadMotion.NONE);
        pose.popPose();
    }
}
