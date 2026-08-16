package cn.laowu.mod.client;

import net.minecraft.client.model.CatModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;

public final class HissingCatRenderer extends MobRenderer<Cat, CatModel<Cat>> {
    public HissingCatRenderer(EntityRendererProvider.Context context) {
        super(context, new HissingCatModel(context.bakeLayer(HissingCatModel.LAYER)), 0.4F);
        addLayer(new HissingCatGeometryLayer(this));
        addLayer(new PancakeCatGeometryLayer(this));
        addLayer(new CatClothesLayer(this));
        addLayer(new AdaptiveCatCollarLayer(this, context.getModelSet()));
        addLayer(new CatChestLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(Cat cat) {
        return cat.getVariant().texture();
    }
}
