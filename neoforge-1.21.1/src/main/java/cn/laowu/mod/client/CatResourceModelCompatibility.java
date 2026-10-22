package cn.laowu.mod.client;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Uses the standard cat bake entry point without linking optional CEM classes. */
public final class CatResourceModelCompatibility {
    public static HissingCatModel create(EntityRendererProvider.Context context) {
        ModelPart resourceRoot = context.bakeLayer(ModelLayers.CAT);
        if (hasCustomModel(resourceRoot)) return new HissingCatModel(resourceRoot, true);
        return new HissingCatModel(context.bakeLayer(HissingCatModel.LAYER));
    }

    private static boolean hasCustomModel(ModelPart root) {
        // EMF exposes its live bake result here. Reflect on the returned object only;
        // never load an optional class or infer model support from a texture file.
        if (!root.getClass().getName().startsWith("traben.entity_model_features.")) return false;
        try {
            return root.getClass().getField("containsCustomModel").getBoolean(root);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private CatResourceModelCompatibility() {}
}
