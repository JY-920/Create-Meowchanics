package cn.laowu.mod.client;

import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.DynamiteCatLastStand;
import cn.laowu.mod.LaoWuMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;

import java.util.Map;

/** Only career headwear fits the giant model's unchanged head; never draw body/vehicle props. */
final class GiantCatHeadOutfit {
    private static final ResourceLocation GIANT_MODEL =
            LaoWuMod.id("models/entity/giant_cat_mount.bbmodel");

    /** Called in the same scaled/root-translated space, with the same sampled pose, as the giant. */
    static void render(Cat cat, float partialTick, PoseStack pose, MultiBufferSource buffers, int light,
                       RuntimeBlockbenchModel.HeadMotion headMotion,
                       Map<String, RuntimeBlockbenchModel.GroupTransform> animation) {
        CatOutfitType outfit = CatClothesData.getOutfit(cat);
        CatOutfitModels.Definition definition = CatOutfitModels.get(outfit);
        if (definition == null) return;

        pose.pushPose();
        if (RuntimeBlockbenchModel.get(GIANT_MODEL).translateToGroup(
                pose, "head", headMotion, animation)) {
            // Outfit reference pivot is model (0,9,-9), Java (0,15,-9).
            // Undo it after entering giant head-local space. Its same-sized
            // head at model Y8 then receives the outfit without any deformation.
            pose.translate(0.0D, -15.0D / 16.0D, 9.0D / 16.0D);
            int overlay = LivingEntityRenderer.getOverlayCoords(cat,
                    DynamiteCatLastStand.whiteOverlayProgress(cat, partialTick));
            renderTexture(pose, buffers, light, overlay, cat, outfit, definition,
                    definition.texture(), 1);
            if (definition.translucentTexture() != null) {
                renderTexture(pose, buffers, light, overlay, cat, outfit, definition,
                        definition.translucentTexture(), 2);
            }
        }
        pose.popPose();
    }

    private static void renderTexture(PoseStack pose, MultiBufferSource buffers,
                                      int light, int overlay, Cat cat, CatOutfitType outfit,
                                      CatOutfitModels.Definition definition,
                                      ResourceLocation texture, int textureIndex) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderType type = cat.isInvisible()
                ? (minecraft.shouldEntityAppearGlowing(cat) ? RenderType.outline(texture)
                : RenderType.itemEntityTranslucentCull(texture))
                : outfit == CatOutfitType.HONEY ? RenderType.entityTranslucent(texture)
                : RenderType.entityCutoutNoCull(texture);
        var consumer = buffers.getBuffer(type);
        var runtime = RuntimeBlockbenchModel.getCatOutfit(definition.model());
        // Flight's generic root mixes one helmet with an entire aircraft.
        if (outfit == CatOutfitType.FLIGHT) {
            runtime.renderFlightTexture(pose, consumer, light, overlay,
                    RuntimeBlockbenchModel.HeadMotion.NONE, Map.of(), true);
        } else {
            // Terminator alone has an extra facial mesh in its generic root.
            // Every other outfit has all headwear under head (including children).
            runtime.renderTexture(pose, consumer, light, overlay,
                    definition.genericRootFollowsHead()
                            ? RuntimeBlockbenchModel.GroupSelection.CAT_HEAD_ONLY
                            : RuntimeBlockbenchModel.GroupSelection.CAT_HEAD_ONLY_PLAIN,
                    RuntimeBlockbenchModel.HeadMotion.NONE, Map.of(), textureIndex);
        }
    }

    private GiantCatHeadOutfit() {
    }
}
