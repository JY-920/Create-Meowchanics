package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.network.CatPackageLoadPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Draws package motion without spawning a physical item that can collide, merge or despawn. */
@Mod.EventBusSubscriber(modid = LaoWuMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CatPackageLoadAnimationManager {
    private static final Map<Integer, Animation> ANIMATIONS = new HashMap<>();

    public static void start(CatPackageLoadPacket packet) {
        if (packet.parcel().isEmpty()) return;
        ANIMATIONS.put(packet.catId(), new Animation(packet.seat(), packet.parcel().copy(),
                Math.max(1, packet.duration())));
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getInstance().level == null) {
            ANIMATIONS.clear();
            return;
        }
        Iterator<Animation> iterator = ANIMATIONS.values().iterator();
        while (iterator.hasNext()) {
            Animation animation = iterator.next();
            if (++animation.age > animation.duration) iterator.remove();
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || ANIMATIONS.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        for (Animation animation : ANIMATIONS.values()) {
            float progress = Mth.clamp((animation.age + event.getPartialTick()) / animation.duration,
                    0.0F, 1.0F);
            progress = progress * progress * (3.0F - 2.0F * progress);
            double x = animation.seat.getX() + 0.5D;
            double y = animation.seat.getY() - 0.55D + progress * 1.45D;
            double z = animation.seat.getZ() + 0.5D;

            poseStack.pushPose();
            poseStack.translate(x - camera.x, y - camera.y, z - camera.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.scale(1.49F, 1.49F, 1.49F);
            int light = LevelRenderer.getLightColor(minecraft.level,
                    BlockPos.containing(x, y, z));
            minecraft.getItemRenderer().renderStatic(null, animation.parcel,
                    ItemDisplayContext.FIXED, false, poseStack, buffers,
                    minecraft.level, light, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        buffers.endBatch();
    }

    private static final class Animation {
        private final BlockPos seat;
        private final ItemStack parcel;
        private final int duration;
        private int age;

        private Animation(BlockPos seat, ItemStack parcel, int duration) {
            this.seat = seat;
            this.parcel = parcel;
            this.duration = duration;
        }
    }

    private CatPackageLoadAnimationManager() {}
}
