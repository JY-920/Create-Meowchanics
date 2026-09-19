package cn.laowu.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.animal.Cat;

/** Capture the evaluated body pose immediately: the parent model is shared between cats. */
public final class CatPerformanceLayer extends RenderLayer<Cat, CatModel<Cat>> {
    public CatPerformanceLayer(RenderLayerParent<Cat, CatModel<Cat>> parent) { super(parent); }

    @Override
    public void render(PoseStack poses, MultiBufferSource buffers, int light, Cat cat,
                       float limbSwing, float limbSwingAmount, float partialTick, float age,
                       float headYaw, float headPitch) {
        if (getParentModel() instanceof HissingCatModel model && (cn.laowu.mod.CatMusicSupport.glowing(cat) || cn.laowu.mod.CatMedicalHealing.glowing(cat))) {
            CatPerformanceOutline.capture(cat, model, poses, getTextureLocation(cat), partialTick);
        }
    }
}
