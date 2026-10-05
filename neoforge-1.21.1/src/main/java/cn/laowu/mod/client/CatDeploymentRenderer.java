package cn.laowu.mod.client;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.item.ItemDisplayContext;
public final class CatDeploymentRenderer implements BlockEntityRenderer<CatDeploymentBlockEntity> {
    private static final PartialModel PLATE=PartialModel.of(LaoWuMod.id("block/cat_ejecting_deployment_platform_plate"));
    private static final PartialModel ROD=PartialModel.of(LaoWuMod.id("block/cat_ejecting_deployment_platform_rod"));
    public CatDeploymentRenderer(BlockEntityRendererProvider.Context context){}
    public static void init(){}
    public static void renderTarget(PoseStack pose,MultiBufferSource buffers){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null)return;
        net.minecraft.core.BlockPos target=null;
        for(var hand:net.minecraft.world.InteractionHand.values()){
            var stack=mc.player.getItemInHand(hand);
            if(stack.is(LaoWuMod.CAT_EJECTING_DEPLOYMENT_PLATFORM_ITEM.get())){
                var selected=cn.laowu.mod.item.CatDeploymentBlockItem.selection(stack);
                int[] pos=selected.getIntArray("Pos");
                if(pos.length==3&&selected.getString("Dimension").equals(mc.level.dimension().location().toString()))target=new net.minecraft.core.BlockPos(pos[0],pos[1],pos[2]);
            }
        }
        if(target==null&&mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit
                &&mc.level.getBlockEntity(hit.getBlockPos()) instanceof CatDeploymentBlockEntity be)target=be.target();
        if(target!=null)LevelRenderer.renderLineBox(pose,buffers.getBuffer(RenderType.lines()),new net.minecraft.world.phys.AABB(target).inflate(.01),.3f,1f,.6f,1f);
    }
    @Override public void render(CatDeploymentBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        renderFuel(be,pose,buffers,light);
        float cycle=be.launchProgress(partial);
        if(be.ejecting()){
            pose.pushPose();
            pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(180-be.getBlockState().getValue(CatDeploymentBlock.FACING).toYRot()));pose.translate(-.5,0,-.5);
            pose.pushPose();pose.translate(0,cycle,0);
            CachedBuffers.partial(PLATE,be.getBlockState()).light(light).renderInto(pose,buffers.getBuffer(RenderType.cutoutMipped()));
            pose.popPose();
            pose.translate(0,cycle*9/16d,0);pose.translate(.5,11/16d,.5);pose.scale(1,1+7*cycle,1);pose.translate(-.5,-11/16d,-.5);
            CachedBuffers.partial(ROD,be.getBlockState()).light(light).renderInto(pose,buffers.getBuffer(RenderType.cutoutMipped()));
            pose.popPose();
        }
        if(!be.catStack().isEmpty()){
            pose.pushPose();pose.translate(.5,(be.ejecting()?14/16d+cycle:13/16d)+.04,.5);
            // Cat Pancake's FIXED renderer already lays the authored model flat.
            pose.scale(.5f,.5f,.5f);
            Minecraft.getInstance().getItemRenderer().renderStatic(be.catStack(),ItemDisplayContext.FIXED,light,overlay,pose,buffers,be.getLevel(),0);
            pose.popPose();
        }
    }
    @SuppressWarnings("unchecked")
    private static void renderFuel(CatDeploymentBlockEntity be,PoseStack pose,MultiBufferSource buffers,int light){
        var fluid=be.tank.getFluid();
        if(fluid.isEmpty())return;
        float fill=net.minecraft.util.Mth.clamp(fluid.getAmount()/(float)CatDeploymentBlockEntity.CAPACITY,0,1);
        // Stay inside the authored 3..13 cavity, below the ejector's fixed piston roof.
        // Insets prevent coplanar liquid/window surfaces from flickering.
        float min=3.01f/16f,max=12.99f/16f;
        float ceiling=(be.ejecting()?9.99f:12.99f)/16f;
        ((net.createmod.catnip.render.FluidRenderHelper<net.neoforged.neoforge.fluids.FluidStack>)(net.createmod.catnip.render.FluidRenderHelper<?>)net.createmod.catnip.platform.CatnipServices.FLUID_RENDERER).renderFluidBox(fluid,
                min,min,min,max,min+(ceiling-min)*fill,max,buffers,pose,light,false,false);
    }
}
