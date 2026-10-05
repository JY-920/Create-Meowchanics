package cn.laowu.mod.create;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
public final class CatAutoLaserBlockItem extends BlockItem {
    public CatAutoLaserBlockItem(Block block,Properties properties){super(block,properties);}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if(renderer==null) {
                    var mc=net.minecraft.client.Minecraft.getInstance();
                    renderer=new cn.laowu.mod.client.CatAutoLaserItemRenderer(mc.getBlockEntityRenderDispatcher(),mc.getEntityModels());
                }
                return renderer;
            }
        });
    }
}
