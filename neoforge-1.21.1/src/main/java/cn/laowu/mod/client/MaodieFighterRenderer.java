package cn.laowu.mod.client;
import cn.laowu.mod.entity.MaodieFighter;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
/** Independent Butter-sized cat boxer; no giant model or prop layer. */
public final class MaodieFighterRenderer extends MobRenderer<MaodieFighter,MaodieFighterModel>{
 private static final ResourceLocation TEXTURE=cn.laowu.mod.LaoWuMod.id("textures/entity/maodie_fighter.png");
 public MaodieFighterRenderer(EntityRendererProvider.Context context){super(context,new MaodieFighterModel(context.bakeLayer(MaodieFighterModel.LAYER)),.45F);}
 @Override public ResourceLocation getTextureLocation(MaodieFighter cat){return cat.getInheritedGenome()
  .map(genome->CatGenomeTextureManager.resolveFighter(genome,TEXTURE)).orElse(TEXTURE);}
 @Override protected boolean shouldShowName(MaodieFighter cat){return false;}
 @Override protected void scale(MaodieFighter cat,PoseStack pose,float partial){pose.scale(MaodieFighter.MODEL_SCALE,MaodieFighter.MODEL_SCALE,MaodieFighter.MODEL_SCALE);}
}
