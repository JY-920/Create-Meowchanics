package cn.laowu.mod.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.*;

/** Single-level effect images; releasing one must never detach the current world target. */
final class PerformanceTarget extends TextureTarget {
    PerformanceTarget(int width,int height,boolean depth,boolean mac){
        super(width,height,depth,mac);
        int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE),texture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        singleLevel(getColorTextureId());if(depth)singleLevel(getDepthTextureId());
        RenderSystem.activeTexture(active);GL13.glActiveTexture(active);
        RenderSystem.bindTexture(texture);GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);
    }
    private static void singleLevel(int texture){
        RenderSystem.bindTexture(texture);GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL12.GL_TEXTURE_BASE_LEVEL,0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL12.GL_TEXTURE_MAX_LEVEL,0);
    }
    @Override public void destroyBuffers(){
        int read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),owned=frameBufferId;
        super.destroyBuffers();
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read==owned?0:read);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw==owned?0:draw);
    }
}
