package cn.laowu.mod.client;
import cn.laowu.mod.create.CatMachineBlocks;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.world.item.*;
import com.mojang.blaze3d.vertex.PoseStack;
public final class CatAutoLaserItemRenderer extends BlockEntityWithoutLevelRenderer {
    public CatAutoLaserItemRenderer(BlockEntityRenderDispatcher dispatcher,EntityModelSet models){super(dispatcher,models);}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose();
        // GUI keeps its fitted thumbnail. Other contexts already receive the
        // vanilla block display transforms from the item model.
        if(context==ItemDisplayContext.GUI){
            pose.translate(.5,.5,.5);
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(30));pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(225));
            pose.scale(.56f,.56f,.56f);pose.translate(-.5,-.375,-.5);
        }
        var state=CatMachineBlocks.CAT_AUTO_LASER.get().defaultBlockState();
        net.minecraft.client.Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state,pose,buffers,light,overlay);
        CatAutoLaserRenderer.renderModel(state,0,0,0,false,pose,buffers,light,overlay);
        pose.popPose();
    }
    @Override public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager manager){RuntimeBlockbenchModel.clearCache();}
}
