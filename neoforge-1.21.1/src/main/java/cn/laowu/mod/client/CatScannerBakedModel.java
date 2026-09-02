package cn.laowu.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

/**
 * Selects the ordinary generated sprite before ItemRenderer decides whether
 * to invoke the scanner's custom 3D renderer. This guarantees a truly flat
 * inventory icon instead of trying to draw a sprite from inside a BEWLR whose
 * pose already contains the 3D model's display transform.
 */
public final class CatScannerBakedModel extends BakedModelWrapper<BakedModel> {
    private final BakedModel handheldModel;

    public CatScannerBakedModel(BakedModel flatModel, BakedModel handheldModel) {
        super(flatModel);
        this.handheldModel = handheldModel;
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack,
                                     boolean leftHand) {
        if (isHandContext(context)) {
            return handheldModel.applyTransform(context, poseStack, leftHand);
        }
        return super.applyTransform(context, poseStack, leftHand);
    }

    private static boolean isHandContext(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }
}
