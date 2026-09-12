package cn.laowu.mod.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import java.nio.ByteBuffer;

/** Owned world depth for occluding the performance outline. */
final class PerformanceSceneSnapshot {
    private TextureTarget scene;
    private int depthFormat;
    private boolean available;
    private static boolean warned;
    int color(){return scene==null?0:scene.getColorTextureId();}
    int depth(){return scene==null?0:scene.getDepthTextureId();}
    boolean available(){return available;}
    void release(){if(scene!=null){scene.destroyBuffers();scene=null;}available=false;}

    void bind() {
        if(!available)throw new IllegalStateException("Scene snapshot is not ready");
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER,scene.frameBufferId);
        RenderSystem.viewport(0,0,scene.width,scene.height);
    }
    boolean copyFrom(PerformanceSceneSnapshot source) {
        if(!source.available())return false;
        try(Target ignored=new Target()) { source.bind();return capture(); }
    }
    static final class Target implements AutoCloseable {
        private final int read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        private final int draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        private final int[] viewport=new int[4];
        Target(){GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);}
        public void close(){
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);
            RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
        }
    }

    boolean capture() {
        return capture(true);
    }
    /** Hand overlays need the final color even when a shader's GUI target has no depth attachment. */
    boolean captureColor() {
        return capture(false);
    }
    private boolean capture(boolean requireDepth) {
        available=false;
        int draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE),texture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int[] viewport=new int[4];GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
        float[] clear=new float[4];GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clear);
        boolean scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        int sourceReadBuffer=-1;
        if(draw==0||viewport[2]<1||viewport[3]<1)return false;
        try {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,draw);
            sourceReadBuffer=GL11.glGetInteger(GL11.GL_READ_BUFFER);
            int drawBuffer=GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_DRAW_BUFFER0);
            if(drawBuffer==GL11.GL_NONE)return false;
            GL11.glReadBuffer(drawBuffer);
            int format=0,type=0,pixel=0;
            if(requireDepth) {
                int object=GL30.glGetFramebufferAttachmentParameteri(GL30.GL_READ_FRAMEBUFFER,GL30.GL_DEPTH_ATTACHMENT,
                        GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
                if(object==GL11.GL_NONE)return false;
                int bits=GL30.glGetFramebufferAttachmentParameteri(GL30.GL_READ_FRAMEBUFFER,GL30.GL_DEPTH_ATTACHMENT,
                        GL30.GL_FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE);
                int stencil=GL30.glGetFramebufferAttachmentParameteri(GL30.GL_READ_FRAMEBUFFER,GL30.GL_DEPTH_ATTACHMENT,
                        GL30.GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE);
                int component=GL30.glGetFramebufferAttachmentParameteri(GL30.GL_READ_FRAMEBUFFER,GL30.GL_DEPTH_ATTACHMENT,
                        GL30.GL_FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE);
                if(stencil>0) {
                    format=component==GL11.GL_FLOAT?GL30.GL_DEPTH32F_STENCIL8:GL30.GL_DEPTH24_STENCIL8;
                    type=component==GL11.GL_FLOAT?GL30.GL_FLOAT_32_UNSIGNED_INT_24_8_REV:GL30.GL_UNSIGNED_INT_24_8;
                    pixel=GL30.GL_DEPTH_STENCIL;
                } else {
                    format=component==GL11.GL_FLOAT?GL30.GL_DEPTH_COMPONENT32F:
                            bits==32?org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT32:
                            bits==24?org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24:org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT16;
                    type=component==GL11.GL_FLOAT?GL11.GL_FLOAT:GL11.GL_UNSIGNED_INT;
                    pixel=GL11.GL_DEPTH_COMPONENT;
                }
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            if(scene==null||scene.width!=viewport[2]||scene.height!=viewport[3]||depthFormat!=format) {
                release();scene=new PerformanceTarget(viewport[2],viewport[3],requireDepth,Minecraft.ON_OSX);
                depthFormat=format;
                if(requireDepth) {
                    RenderSystem.bindTexture(scene.getDepthTextureId());
                    GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,format,scene.width,scene.height,0,pixel,type,(ByteBuffer)null);
                }
                scene.setFilterMode(GL11.GL_LINEAR);
            }
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,draw);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,scene.frameBufferId);
            if(GL30.glCheckFramebufferStatus(GL30.GL_READ_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE
                    ||GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE)return false;
            // Do not publish an uninitialised black texture after a rejected copy.
            for(int i=0;i<8&&GL11.glGetError()!=GL11.GL_NO_ERROR;i++){}
            // Matching depth storage and equal-sized rectangles also allow multisample resolves.
            GL30.glBlitFramebuffer(viewport[0],viewport[1],viewport[0]+viewport[2],viewport[1]+viewport[3],
                    0,0,scene.width,scene.height,GL11.GL_COLOR_BUFFER_BIT|(requireDepth?GL11.GL_DEPTH_BUFFER_BIT:0),GL11.GL_NEAREST);
            int error=GL11.glGetError();
            if(error!=GL11.GL_NO_ERROR){
                if(!warned){warned=true;com.mojang.logging.LogUtils.getLogger().warn(
                        "Effect scene copy rejected (GL {}, source {}, depth format {}); preserving the world image",error,draw,format);}
                return false;
            }
            available=true;return true;
        } finally {
            if(sourceReadBuffer!=-1){
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,draw);
                GL11.glReadBuffer(sourceReadBuffer);
            }
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);
            RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            RenderSystem.clearColor(clear[0],clear[1],clear[2],clear[3]);
            RenderSystem.activeTexture(active);RenderSystem.bindTexture(texture);
            if(scissor)GL11.glEnable(GL11.GL_SCISSOR_TEST);
        }
    }
}
