package cn.laowu.mod.client;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.level.block.entity.BlockEntity;
public final class CatCareRenderers {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static BlockEntityRenderer<BlockEntity> breeding(BlockEntityRendererProvider.Context context) {
        if(cn.laowu.mod.compat.create.CreateIntegration.isLoaded()) return cn.laowu.mod.compat.create.CreateClientEvents.breedingRenderer(context);
        return (BlockEntityRenderer)new StandaloneBreedingBoxRenderer(context);
    }
    private CatCareRenderers() {}
}
