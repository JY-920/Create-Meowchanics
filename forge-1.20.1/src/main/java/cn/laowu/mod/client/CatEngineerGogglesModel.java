package cn.laowu.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.BakedModelWrapper;

/**
 * Keeps the supplied 2D inventory icon, but swaps to the editable Blockbench
 * model when Minecraft renders the goggles in a player's head slot.
 */
public final class CatEngineerGogglesModel extends BakedModelWrapper<BakedModel> {
    private final BakedModel wornModel;

    public CatEngineerGogglesModel(BakedModel inventoryModel, BakedModel wornModel) {
        super(inventoryModel);
        this.wornModel = wornModel;
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack,
                                     boolean leftHand) {
        if (context == ItemDisplayContext.HEAD) {
            return wornModel.applyTransform(context, poseStack, leftHand);
        }
        return super.applyTransform(context, poseStack, leftHand);
    }
}
