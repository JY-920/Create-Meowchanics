package cn.laowu.mod.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.lwjgl.opengl.*;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Our shaders own their bindings even when another renderer uses raw GL beside Mojang's caches. */
final class PerformanceShader extends ShaderInstance {
    private final Map<String,Binding> bindings=new LinkedHashMap<>();
    private static final class Binding {
        final int location;Object image;
        Binding(int location,Object image){this.location=location;this.image=image;}
    }
    PerformanceShader(ResourceProvider provider,ResourceLocation name,VertexFormat format)throws IOException{
        super(provider,name,format);
    }
    @Override public void setSampler(String name,Object image){
        super.setSampler(name,image);
        Binding binding=bindings.get(name);
        if(binding==null)bindings.put(name,new Binding(GL20.glGetUniformLocation(getId(),name),image));
        else binding.image=image;
    }
    @Override public void apply(){
        // Uniform uploads also need the real program, not only ShaderInstance.lastProgramId.
        GL20.glUseProgram(getId());
        super.apply();
        GL20.glUseProgram(getId());
        int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE),unit=0;
        boolean samplers=GL.getCapabilities().OpenGL33||GL.getCapabilities().GL_ARB_sampler_objects;
        for(Binding binding:bindings.values()){
            if(binding.location<0)continue;
            Object image=binding.image;
            int texture=image instanceof RenderTarget target?target.getColorTextureId():
                    image instanceof AbstractTexture target?target.getId():image instanceof Integer id?id:0;
            GL20.glUniform1i(binding.location,unit);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0+unit);
            GL13.glActiveTexture(GL13.GL_TEXTURE0+unit);
            RenderSystem.bindTexture(texture);GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);
            if(samplers)GL33.glBindSampler(unit,0);
            unit++;
        }
        RenderSystem.activeTexture(active);GL13.glActiveTexture(active);
    }
}
