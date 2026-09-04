package cn.laowu.mod.client;

import cn.laowu.mod.DynamiteCatLastStand;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Cat;

public final class HissingCatRenderer extends MobRenderer<Cat, CatModel<Cat>> {
    public HissingCatRenderer(EntityRendererProvider.Context context) {
        super(context, new HissingCatModel(context.bakeLayer(HissingCatModel.LAYER)), 0.4F);
        addLayer(new HissingCatGeometryLayer(this));
        addLayer(new PancakeCatGeometryLayer(this));
        addLayer(new CatClothesLayer(this));
        addLayer(new CatAppearanceLayer(this,
                new CatAppearanceModel(context.bakeLayer(CatAppearanceModel.LAYER))));
        addLayer(new AdaptiveCatCollarLayer(this, context.getModelSet()));
        addLayer(new CatChestLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(Cat cat) {
        return CatGenomeTextureManager.resolve(cat);
    }

    @Override
    protected void scale(Cat cat, PoseStack pose, float partialTick) {
        super.scale(cat, pose, partialTick);
        int level = CatTraitData.read(cat)
                .map(profile -> profile.level(CatTrait.BIG_CHONKY_CAT))
                .orElse(0);
        if (level > 0) {
            float scale = CatTrait.BIG_CHONKY_CAT.bigCatScalePercent(level) / 100.0F;
            pose.scale(scale, scale, scale);
        }

        // Vanilla CreeperRenderer performs this entirely in the renderer; it
        // is not a separate model animation. Preserve its pulse and p^4
        // late-fuse swelling curve on the complete cat and every render layer.
        float swelling = DynamiteCatLastStand.swelling(cat, partialTick);
        if (swelling <= 0.0F) return;
        float pulse = 1.0F + Mth.sin(swelling * 100.0F)
                * swelling * 0.01F;
        float eased = Mth.clamp(swelling, 0.0F, 1.0F);
        eased *= eased;
        eased *= eased;
        float horizontal = (1.0F + eased * 0.4F) * pulse;
        float vertical = (1.0F + eased * 0.1F) / pulse;
        pose.scale(horizontal, vertical, horizontal);
    }

    @Override
    protected float getWhiteOverlayProgress(Cat cat, float partialTick) {
        if (!DynamiteCatLastStand.isActive(cat)) {
            return super.getWhiteOverlayProgress(cat, partialTick);
        }
        return DynamiteCatLastStand.whiteOverlayProgress(cat, partialTick);
    }

    @Override
    protected void setupRotations(Cat cat, PoseStack pose, float ageInTicks,
                                  float bodyYaw, float partialTick) {
        super.setupRotations(cat, pose, ageInTicks, bodyYaw, partialTick);
        if (CatTraitData.read(cat)
                .map(profile -> profile.has(CatTrait.OIIAI)).orElse(false)) {
            pose.mulPose(Axis.YP.rotationDegrees(ageInTicks * 20.0F));
        }
    }
}
