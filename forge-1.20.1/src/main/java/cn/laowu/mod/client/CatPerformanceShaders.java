package cn.laowu.mod.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import java.io.IOException;

@Mod.EventBusSubscriber(modid="laowu", value=Dist.CLIENT, bus=Mod.EventBusSubscriber.Bus.MOD)
public final class CatPerformanceShaders {
    @SubscribeEvent
    public static void register(RegisterShadersEvent event) {
        CatPerformanceOutline.setMask(null);
        CatPerformanceOutline.setShader(null);
        try {
            ShaderInstance mask=new PerformanceShader(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath("laowu","performance_mask"),DefaultVertexFormat.NEW_ENTITY);
            ShaderInstance outline;
            try {
                outline=new PerformanceShader(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath("laowu","performance_boundary"),DefaultVertexFormat.POSITION_TEX_COLOR);
            } catch(IOException failure){mask.close();throw failure;}
            event.registerShader(mask,CatPerformanceOutline::setMask);
            event.registerShader(outline,CatPerformanceOutline::setShader);
        } catch(IOException failure){
            com.mojang.logging.LogUtils.getLogger().warn("Could not load cat performance outline shaders",failure);
        }
    }
}
