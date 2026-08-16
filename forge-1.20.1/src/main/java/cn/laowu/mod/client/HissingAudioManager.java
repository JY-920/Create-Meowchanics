package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = LaoWuMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class HissingAudioManager {
    private static final int INITIAL_DELAY_TICKS = 10;
    private static final int LOOP_RESTART_DELAY_TICKS = 20;
    private static final int LOOP_SEGMENT_COUNT = 8;
    private static final Map<Integer, Session> SESSIONS = new HashMap<>();

    public static void start(int entityId) {
        if (SESSIONS.containsKey(entityId)) return;
        Session session = new Session(entityId);
        SESSIONS.put(entityId, session);
        session.scheduleIntro();
    }

    public static void stop(int entityId) {
        Session session = SESSIONS.remove(entityId);
        if (session != null) session.stopCurrent();
    }

    public static void playLogisticsSound(double x, double y, double z, boolean arrival) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ResourceLocation event = ResourceLocation.fromNamespaceAndPath(LaoWuMod.MOD_ID,
                arrival ? "logistics_arrive" : "hissing_intro");
        float volume = arrival ? 1.8F : 1.0F;
        mc.getSoundManager().play(new SimpleSoundInstance(event, SoundSource.HOSTILE,
                volume, 1.0F, RandomSource.create(), false, 0,
                SoundInstance.Attenuation.LINEAR, x, y, z, false));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getInstance().level == null) {
            SESSIONS.values().forEach(Session::stopCurrent);
            SESSIONS.clear();
            return;
        }
        SESSIONS.values().removeIf(Session::tick);
    }

    private enum Stage { INTRO_DELAY, INTRO, LOOP, WAIT }

    private static final class Session {
        private final int entityId;
        private Stage stage = Stage.INTRO_DELAY;
        private SoundInstance current;
        private int graceTicks;
        private int waitTicks;

        private Session(int entityId) { this.entityId = entityId; }

        /** @return true when the owning entity vanished and this session should be removed. */
        private boolean tick() {
            Minecraft mc = Minecraft.getInstance();
            Entity owner = mc.level == null ? null : mc.level.getEntity(entityId);
            if (owner == null || !owner.isAlive()) {
                stopCurrent();
                return true;
            }
            if (stage == Stage.INTRO_DELAY) {
                if (--waitTicks <= 0) playIntro();
                return false;
            }
            if (stage == Stage.WAIT) {
                if (--waitTicks <= 0) playRandomLoopSegment();
                return false;
            }
            if (graceTicks-- > 0 || current == null || mc.getSoundManager().isActive(current)) return false;
            if (stage == Stage.INTRO) playRandomLoopSegment();
            else {
                stage = Stage.WAIT;
                waitTicks = LOOP_RESTART_DELAY_TICKS;
                current = null;
            }
            return false;
        }

        private void scheduleIntro() {
            stage = Stage.INTRO_DELAY;
            waitTicks = INITIAL_DELAY_TICKS;
            current = null;
        }

        private void playIntro() {
            stage = Stage.INTRO;
            play(ResourceLocation.fromNamespaceAndPath(LaoWuMod.MOD_ID, "hissing_intro"));
        }

        private void playRandomLoopSegment() {
            stage = Stage.LOOP;
            int segment = RandomSource.create().nextInt(LOOP_SEGMENT_COUNT);
            play(ResourceLocation.fromNamespaceAndPath(LaoWuMod.MOD_ID,
                    "hissing_loop_" + segment));
        }

        private void play(ResourceLocation event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            Entity owner = mc.level.getEntity(entityId);
            if (owner == null) return;
            current = new SimpleSoundInstance(event, SoundSource.HOSTILE, 1.0F, 1.0F,
                    RandomSource.create(), false, 0, SoundInstance.Attenuation.LINEAR,
                    owner.getX(), owner.getY(), owner.getZ(), false);
            graceTicks = 20;
            mc.getSoundManager().play(current);
        }

        private void stopCurrent() {
            if (current != null) Minecraft.getInstance().getSoundManager().stop(current);
        }
    }

    private HissingAudioManager() {}
}
