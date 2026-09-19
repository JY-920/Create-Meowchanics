package cn.laowu.mod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import java.util.List;

/** Terrain-occluded attack-speed double chevrons, inside the shared outline GL guard. */
public final class CatMusicEffects {
    public record Target(Vec3 position, float width, float height, float seed) {}
    private static ShaderInstance hasteShader;
    static void setShader(ShaderInstance shader) { hasteShader = shader; }
    static void draw(RenderLevelStageEvent event, Matrix4f view, Matrix4f projection, List<Target> targets, List<CatSupportAreas.Area> areas) {
        if (targets.isEmpty() && areas.isEmpty()) return;
        float time = (net.minecraft.client.Minecraft.getInstance().level.getGameTime() % 24000
                + event.getPartialTick().getGameTimeDeltaPartialTick(false)) * .05F;
        RenderSystem.enableDepthTest(); RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false); RenderSystem.disableCull(); RenderSystem.enableBlend();
        CatSupportAreas.draw(areas, time, event.getCamera().getPosition(), view, projection, .65F, .24F, 1F);
        drawGlyphs(targets, time, event.getCamera().getPosition(),
                view, projection);
    }
    static int drawGlyphs(List<Target> targets, float time, Vec3 camera, Matrix4f view, Matrix4f projection) {
        if (hasteShader == null || targets.isEmpty()) return 0;
        int emitted = 0;
        RenderSystem.setShader(() -> hasteShader);
        RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        BufferBuilder glyphs = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (Target target : targets) {
            Vec3 relative = target.position.add(0, target.height * .5, 0).subtract(camera);
            Vector3f center = view.transformPosition(new Vector3f((float)relative.x, (float)relative.y, (float)relative.z));
            // A steady double-chevron above the right shoulder stays separate from green healing crosses.
            Vec3 overhead = target.position.add(0, target.height + .52, 0).subtract(camera);
            Vector3f top = view.transformPosition(new Vector3f((float)overhead.x, (float)overhead.y, (float)overhead.z)).add(.32F, 0, 0);
            emitted += glyph(glyphs, projection, top, .24F * Mth.clamp(target.width, .9F, 1.6F), .95F);
            for (int i = 0; i < 3; i++) {
                float clock = time * (.55F + i * .027F) + i * .2F + target.seed;
                float phase = clock - (float)Math.floor(clock);
                float fade = smooth(phase / .15F) * (1 - smooth((phase - .78F) / .22F));
                float seed = target.seed * 57 + i * 4.7F + (float)Math.floor(clock) * 1.37F;
                float lateral = (i % 2 == 0 ? -1 : 1) * target.width * (.54F + .19F * random(seed));
                Vector3f point = new Vector3f(center).add(lateral, target.height * (phase * 1.25F - .65F), target.width * .25F);
                float size = (.20F + .08F * random(seed + 9)) * Mth.clamp(target.width, .9F, 1.5F);
                emitted += glyph(glyphs, projection, point, size, .98F * fade);
            }
        }
        MeshData mesh = glyphs.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
        return emitted;
    }
    private static int glyph(BufferBuilder b, Matrix4f projection, Vector3f center, float size, float alpha) {
        if (alpha < .01F) return 0;
        Vector4f clip = projection.transform(new Vector4f(center, 1));
        if (clip.w < .06F) return 0;
        float x = clip.x / clip.w, y = clip.y / clip.w, z = clip.z / clip.w;
        float rx = Math.abs(projection.m00() * size / clip.w), ry = Math.abs(projection.m11() * size / clip.w);
        if (Math.abs(z) > 1 || Math.abs(x) > 1 + rx || Math.abs(y) > 1 + ry) return 0;
        b.addVertex(x-rx,y-ry,z).setUv(-1,-1).setColor(1F,1F,1F,alpha);
        b.addVertex(x+rx,y-ry,z).setUv(1,-1).setColor(1F,1F,1F,alpha);
        b.addVertex(x+rx,y+ry,z).setUv(1,1).setColor(1F,1F,1F,alpha);
        b.addVertex(x-rx,y+ry,z).setUv(-1,1).setColor(1F,1F,1F,alpha);
        return 1;
    }
    private static float random(float seed) { double n = Math.sin(seed * 127.1) * 43758.5453; return (float)(n - Math.floor(n)); }
    private static float smooth(float value) { float t = Mth.clamp(value, 0, 1); return t * t * (3 - 2 * t); }
    private CatMusicEffects() {}
}
