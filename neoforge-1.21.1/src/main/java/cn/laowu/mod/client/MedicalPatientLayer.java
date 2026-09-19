package cn.laowu.mod.client;

import cn.laowu.mod.CatMedicalHealing;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

/** Capture the actual posed model while the renderer's entity transform is still on the stack. */
public final class MedicalPatientLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public MedicalPatientLayer(RenderLayerParent<T, M> parent) { super(parent); }
    @Override
    public void render(PoseStack poses, MultiBufferSource buffers, int light, T entity,
                       float limbSwing, float limbSwingAmount, float partial, float age,
                       float headYaw, float headPitch) {
        if (CatMedicalHealing.glowing(entity))
            CatPerformanceOutline.capturePatient(entity, getParentModel(), poses, getTextureLocation(entity), partial);
    }
}
