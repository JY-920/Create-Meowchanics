package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.CatBeltStyle;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltModel;
import com.simibubi.create.foundation.model.BakedQuadHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.ModelEvent;
import java.util.ArrayList;
import java.util.List;

/** Retextures native casing quads only; Create/Flywheel still draw the moving belt and items. */
@EventBusSubscriber(modid=LaoWuMod.MOD_ID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class CatBeltModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<Boolean> CAT=new ModelProperty<>();
    public CatBeltModel(BakedModel original){super(original);}
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void models(ModelEvent.ModifyBakingResult event) {
        for(var state:AllBlocks.BELT.get().getStateDefinition().getPossibleStates()) {
            var key=BlockModelShaper.stateToModelLocation(state);
            var original=event.getModels().get(key);
            if(original!=null)event.getModels().put(key,new CatBeltModel(original));
        }
    }
    @Override public ModelData getModelData(BlockAndTintGetter world,BlockPos pos,BlockState state,ModelData data) {
        var inherited=super.getModelData(world,pos,state,data);
        return inherited.derive().with(CAT,world.getBlockEntity(pos) instanceof CatBeltStyle style && style.laowu$isCatBelt()).build();
    }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType layer) {
        var base=super.getQuads(state,side,random,data,layer);
        if(!Boolean.TRUE.equals(data.get(CAT)))return base;
        var casing=data.get(BeltModel.CASING_PROPERTY);
        if(casing==null || casing==BeltBlockEntity.CasingType.NONE)return base;
        var shift=AllSpriteShifts.ANDESIDE_BELT_CASING;
        var source=casing==BeltBlockEntity.CasingType.ANDESITE ? shift.getTarget() : shift.getOriginal();
        var target=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(LaoWuMod.id("block/cat_belt_casing"));
        var result=new ArrayList<BakedQuad>(base.size());
        for(var quad:base) {
            // Create's own Andesite remap retains the original brass sprite identity.
            if(quad.getSprite()!=shift.getOriginal()){result.add(quad);continue;}
            int[] vertices=quad.getVertices().clone();
            for(int i=0;i<4;i++) {
                float u=(BakedQuadHelper.getU(vertices,i)-source.getU0())/(source.getU1()-source.getU0());
                float v=(BakedQuadHelper.getV(vertices,i)-source.getV0())/(source.getV1()-source.getV0());
                BakedQuadHelper.setU(vertices,i,target.getU0()+u*(target.getU1()-target.getU0()));
                BakedQuadHelper.setV(vertices,i,target.getV0()+v*(target.getV1()-target.getV0()));
            }
            result.add(new BakedQuad(vertices,quad.getTintIndex(),quad.getDirection(),target,quad.isShade()));
        }
        return result;
    }
}
