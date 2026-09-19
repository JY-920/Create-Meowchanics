package cn.laowu.mod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import java.util.List;

/** View-space ring vertices get exactly one camera transform, even at AFTER_LEVEL with a GUI projection. */
final class CatSupportAreas {
    record Area(Vec3 position, float radius, boolean square, float seed) {}
    static void draw(List<Area> areas, float time, Vec3 camera, Matrix4f view, Matrix4f projection,
                     float red, float green, float blue) {
        if (areas.isEmpty()) return;
        var previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        float[] previousColor=RenderSystem.getShaderColor().clone();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushPose(); modelView.setIdentity(); RenderSystem.applyModelViewMatrix();
        try {
            RenderSystem.setProjectionMatrix(projection, sorting);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1,1,1,1);
            RenderSystem.enableDepthTest(); RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(false); RenderSystem.disableCull(); RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            var buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (Area area : areas) {
                if (!Float.isFinite(area.radius) || area.radius <= 0) continue;
                Vec3 p = area.position.subtract(camera).add(0,.06,0);
                // Keep the boundary at least a visible pixel when viewed from normal combat distance.
                float width=(float)Math.max(.12,Math.min(.28,area.position.distanceTo(camera)*.01));
                ring(buffer,view,p,area.radius,width,area.square,red,green,blue,.72F);
                ring(buffer,view,p,Math.max(0,area.radius-width-.10F),width*.4F,area.square,red,green,blue,.28F);
                float pulse=(time*.24F+area.seed)%1;
                ring(buffer,view,p,area.radius*pulse,width*.5F,area.square,red,green,blue,.20F*(1-pulse));
            }
            var mesh=buffer.endOrDiscardIfEmpty();
            if(mesh!=null)BufferUploader.drawWithShader(mesh);
        } finally {
            modelView.popPose(); RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(previousProjection,sorting);
            RenderSystem.setShaderColor(previousColor[0],previousColor[1],previousColor[2],previousColor[3]);
        }
    }
    private static void ring(BufferBuilder b, Matrix4f view, Vec3 p, float radius,float width,boolean square,
                             float red,float green,float blue,float alpha) {
        int segments=square?4:96;
        for(int i=0;i<segments;i++){
            double a=(i+.5)*Math.PI*2/segments,c=(i+1.5)*Math.PI*2/segments;
            float outer=square?radius*(float)Math.sqrt(2):radius;
            float inner=square?Math.max(0,radius-width)*(float)Math.sqrt(2):Math.max(0,radius-width);
            vertex(b,view,p,a,inner,red,green,blue,alpha);vertex(b,view,p,a,outer,red,green,blue,alpha);
            vertex(b,view,p,c,outer,red,green,blue,alpha);vertex(b,view,p,c,inner,red,green,blue,alpha);
        }
    }
    private static void vertex(BufferBuilder b,Matrix4f view,Vec3 p,double a,float radius,
                               float red,float green,float blue,float alpha){
        b.vertex(view,(float)(p.x+Math.cos(a)*radius),(float)p.y,(float)(p.z+Math.sin(a)*radius))
                .color(red,green,blue,alpha).endVertex();
    }
    private CatSupportAreas(){}
}
