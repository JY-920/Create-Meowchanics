package cn.laowu.mod.client;

import cn.laowu.mod.*;
import cn.laowu.mod.item.CatLaserPointerItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;

@net.neoforged.fml.common.EventBusSubscriber(modid = LaoWuMod.MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class CatLaserEffects {
    private static int markedId = -1;
    private static java.util.UUID markedUuid;
    private static java.lang.ref.WeakReference<net.minecraft.client.multiplayer.ClientLevel> markedLevel = new java.lang.ref.WeakReference<>(null);
    private static long until;

    public static void mark(int id, java.util.UUID uuid) {
        var level = Minecraft.getInstance().level;
        markedId = id; markedUuid = uuid; markedLevel = new java.lang.ref.WeakReference<>(level);
        until = level == null ? 0 : level.getGameTime() + 600;
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { markedLevel.clear(); return; }
        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        var buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);

        CatTeamPreview.render(pose, buffers, event.getCamera(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
        if (markedLevel.get() == mc.level && mc.level.getGameTime() < until) {
            var entity = mc.level.getEntity(markedId);
            if (entity != null && entity.isAlive() && entity.getUUID().equals(markedUuid)) {
                // Keep the private marker in step with the interpolated rendered entity.
                float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
                var offset = entity.getPosition(partial).subtract(entity.position());
                LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()),
                        entity.getBoundingBox().move(offset).inflate(.08), 1, 1, 1, 1);
            }
        } else { markedLevel.clear(); markedId = -1; }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    /** Six filled quads, full-bright vertex colour and normal depth testing; no wireframe or texture. */
    public static void solidBeam(PoseStack pose, VertexConsumer out, float length, int rgb) {
        float w = .012F;
        float[][] corners = {
                {-w,-w,0}, {w,-w,0}, {w,w,0}, {-w,w,0},
                {-w,-w,length}, {w,-w,length}, {w,w,length}, {-w,w,length}
        };
        int[][] faces = {{0,3,2,1}, {4,5,6,7}, {0,1,5,4}, {3,7,6,2}, {0,4,7,3}, {1,2,6,5}};
        for (int[] face : faces) {
            for (int index : face) {
                float[] v = corners[index];
                out.addVertex(pose.last().pose(), v[0], v[1], v[2]).setColor(0xFF000000 | rgb);
            }
        }
    }

    private CatLaserEffects() {}
}
