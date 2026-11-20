package cn.laowu.mod.client.painting;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

public final class PaintingRenderTypes extends RenderType {
 private PaintingRenderTypes(String name,VertexFormat format,VertexFormat.Mode mode,int size,boolean crumbling,boolean sorting,Runnable setup,Runnable clear){super(name,format,mode,size,crumbling,sorting,setup,clear);}
 private static final Map<String,RenderType> CACHE=new LinkedHashMap<>();
 public static RenderType image(ResourceLocation texture,boolean smooth){
  String key=texture+"|"+smooth;RenderType existing=CACHE.get(key);if(existing!=null)return existing;
  RenderType type=create("cat_painting",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,false,false,
   CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
    .setTextureState(new TextureStateShard(texture,smooth,false)).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setCullState(NO_CULL).createCompositeState(true));
  if(CACHE.size()>=128)CACHE.remove(CACHE.keySet().iterator().next());CACHE.put(key,type);return type;
 }
}
