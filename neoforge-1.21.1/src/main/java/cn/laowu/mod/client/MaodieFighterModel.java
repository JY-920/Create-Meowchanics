package cn.laowu.mod.client;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.entity.MaodieFighter;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import java.util.Map;

/** The approved v11 mesh, UVs and baked animation, not another approximation. */
public final class MaodieFighterModel extends EntityModel<MaodieFighter> {
 public static final ModelLayerLocation LAYER=new ModelLayerLocation(LaoWuMod.id("maodie_fighter"),"main");
 public static final ResourceLocation MODEL=LaoWuMod.id("models/entity/maodie_fighter.bbmodel");
 private Map<String,RuntimeBlockbenchModel.GroupTransform> pose=Map.of();
 public MaodieFighterModel(ModelPart unused){}
 public static LayerDefinition createLayer(){return LayerDefinition.create(new MeshDefinition(),64,32);}
 @Override public void setupAnim(MaodieFighter cat,float limbSwing,float amount,float age,float yaw,float pitch){
  float partial=Mth.clamp(age-cat.tickCount,0,1);
  byte phase=cat.getPhase();
  float ticks=phase==MaodieFighter.IDLE||phase==MaodieFighter.SUMMON?age:cat.getPhaseTicks(partial);
  pose=MaodieFighterAnimation.samplePhase(phase,ticks);
 }
 @Override public void renderToBuffer(PoseStack stack,VertexConsumer out,int light,int overlay,int packedColor){
  RuntimeBlockbenchModel.getCatOutfit(MODEL).renderAnimatedTinted(stack,out,light,overlay,pose,(packedColor>>>16)&255,(packedColor>>>8)&255,packedColor&255,(packedColor>>>24)&255);
 }
}
