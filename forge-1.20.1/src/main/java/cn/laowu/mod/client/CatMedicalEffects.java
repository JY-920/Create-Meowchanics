package cn.laowu.mod.client;

import cn.laowu.mod.CatSupportRules;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import java.util.List;

/** Terrain-occluded healing circle and rising cross glyphs; drawn inside the outline's GL guard. */
public final class CatMedicalEffects {
    public record Target(Vec3 position, float width, float height, boolean caster, float seed,
                         float radius, boolean stationed) {}
    private static ShaderInstance crossShader;
    static void setShader(ShaderInstance shader) { crossShader = shader; }
    static void draw(RenderLevelStageEvent event, Matrix4f view, Matrix4f projection, List<Target> targets) {
        if (targets.isEmpty()) return;
        float time = (net.minecraft.client.Minecraft.getInstance().level.getGameTime() % 24000
                + event.getPartialTick()) * .05F;
        Vec3 camera = event.getCamera().getPosition();
        drawFrame(targets, time, camera, view, projection);
    }
    static void drawFrame(List<Target> targets, float time, Vec3 camera, Matrix4f view, Matrix4f projection) {
        RenderSystem.enableDepthTest(); RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false); RenderSystem.disableCull(); RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        CatSupportAreas.draw(targets.stream().filter(Target::caster).map(target ->
                new CatSupportAreas.Area(target.position, target.radius, target.stationed, target.seed)).toList(),
                time, camera, view, projection, .12F, 1F, .35F);
        drawGlyphs(targets, time, camera, view, projection);
    }
    /** Production projection path is also used by the perspective/depth GPU regression probe. */
    static int drawGlyphs(List<Target> targets, float time, Vec3 camera, Matrix4f view, Matrix4f projection) {
        if (crossShader == null || targets.isEmpty()) return 0;
        int emitted = 0;
        RenderSystem.setShader(() -> crossShader);
        RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        BufferBuilder glyphs = Tesselator.getInstance().getBuilder();
        glyphs.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (Target target : targets) {
            Vec3 relative = target.position.add(0, target.height * .5, 0).subtract(camera);
            Vector3f center = view.transformPosition(new Vector3f((float)relative.x, (float)relative.y, (float)relative.z));
            // A steady overhead + keeps the healing state readable even between rising glyphs.
            Vec3 overhead = target.position.add(0, target.height + .28, 0).subtract(camera);
            Vector3f top = view.transformPosition(new Vector3f((float)overhead.x, (float)overhead.y, (float)overhead.z));
            emitted += cross(glyphs, projection, top, .24F * Mth.clamp(target.width, .9F, 1.6F), .95F);
            for (int i = 0; i < 5; i++) {
                float clock = time * (.43F + i * .027F) + i * .2F + target.seed;
                float phase = clock - (float)Math.floor(clock);
                float fade = smooth(phase / .15F) * (1 - smooth((phase - .78F) / .22F));
                float seed = target.seed * 57 + i * 4.7F + (float)Math.floor(clock) * 1.37F;
                float lateral = (i % 2 == 0 ? -1 : 1) * target.width * (.54F + .19F * random(seed));
                Vector3f point = new Vector3f(center).add(lateral, target.height * (phase * 1.25F - .65F), target.width * .25F);
                float size = (.20F + .08F * random(seed + 9)) * Mth.clamp(target.width, .9F, 1.5F);
                emitted += cross(glyphs, projection, point, size, .98F * fade);
            }
        }
        BufferBuilder.RenderedBuffer mesh = glyphs.endOrDiscardIfEmpty();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
        return emitted;
    }
    private static int cross(BufferBuilder b, Matrix4f projection, Vector3f center, float size, float alpha) {
        if (alpha < .01F) return 0;
        Vector4f clip = projection.transform(new Vector4f(center, 1));
        if (clip.w < .06F) return 0;
        float x = clip.x / clip.w, y = clip.y / clip.w, z = clip.z / clip.w;
        float rx = Math.abs(projection.m00() * size / clip.w), ry = Math.abs(projection.m11() * size / clip.w);
        if (Math.abs(z) > 1 || Math.abs(x) > 1 + rx || Math.abs(y) > 1 + ry) return 0;
        b.vertex(x-rx,y-ry,z).uv(-1,-1).color(1F,1F,1F,alpha).endVertex();
        b.vertex(x+rx,y-ry,z).uv(1,-1).color(1F,1F,1F,alpha).endVertex();
        b.vertex(x+rx,y+ry,z).uv(1,1).color(1F,1F,1F,alpha).endVertex();
        b.vertex(x-rx,y+ry,z).uv(-1,1).color(1F,1F,1F,alpha).endVertex();
        return 1;
    }
    private static float random(float seed) { double n = Math.sin(seed * 127.1) * 43758.5453; return (float)(n - Math.floor(n)); }
    private static float smooth(float value) { float t = Mth.clamp(value, 0, 1); return t * t * (3 - 2 * t); }
    private CatMedicalEffects() {}
}
