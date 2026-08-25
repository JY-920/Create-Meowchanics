package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Cat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Positional record playback for at most the three nearest Cat Kings. */
@Mod.EventBusSubscriber(modid = LaoWuMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CatKingAudioManager {
    private static final double AUDIBLE_RANGE_SQR = 48.0D * 48.0D;
    private static final int MAX_SIMULTANEOUS_CATS = 3;
    private static final SoundEvent[] RECORDS = {
            SoundEvents.MUSIC_DISC_5, SoundEvents.MUSIC_DISC_11,
            SoundEvents.MUSIC_DISC_13, SoundEvents.MUSIC_DISC_BLOCKS,
            SoundEvents.MUSIC_DISC_CAT, SoundEvents.MUSIC_DISC_CHIRP,
            SoundEvents.MUSIC_DISC_FAR, SoundEvents.MUSIC_DISC_MALL,
            SoundEvents.MUSIC_DISC_MELLOHI, SoundEvents.MUSIC_DISC_PIGSTEP,
            SoundEvents.MUSIC_DISC_STAL, SoundEvents.MUSIC_DISC_STRAD,
            SoundEvents.MUSIC_DISC_WAIT, SoundEvents.MUSIC_DISC_WARD,
            SoundEvents.MUSIC_DISC_OTHERSIDE, SoundEvents.MUSIC_DISC_RELIC
    };
    private static final Map<Integer, Session> SESSIONS = new HashMap<>();
    private static int discoveryCooldown;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            clear();
            return;
        }
        if (--discoveryCooldown <= 0) {
            discoveryCooldown = 20;
            refreshNearbyCats(minecraft);
        }
        SESSIONS.values().removeIf(Session::tick);
    }

    private static void refreshNearbyCats(Minecraft minecraft) {
        List<Cat> candidates = new ArrayList<>();
        for (var entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof Cat cat) || !cat.isAlive()
                    || minecraft.player.distanceToSqr(cat) > AUDIBLE_RANGE_SQR
                    || CatTraitData.read(cat)
                    .filter(profile -> profile.has(CatTrait.CAT_KING)).isEmpty()) continue;
            candidates.add(cat);
        }
        candidates.sort(Comparator.comparingDouble(minecraft.player::distanceToSqr));
        Set<Integer> wanted = new HashSet<>();
        for (int index = 0; index < Math.min(MAX_SIMULTANEOUS_CATS,
                candidates.size()); index++) {
            Cat cat = candidates.get(index);
            wanted.add(cat.getId());
            SESSIONS.computeIfAbsent(cat.getId(), ignored -> new Session(cat));
        }
        SESSIONS.entrySet().removeIf(entry -> {
            if (wanted.contains(entry.getKey())) return false;
            entry.getValue().stop();
            return true;
        });
    }

    private static void clear() {
        SESSIONS.values().forEach(Session::stop);
        SESSIONS.clear();
        discoveryCooldown = 0;
    }

    private static final class Session {
        private final Cat cat;
        private SoundInstance current;
        private int graceTicks;
        private int lastRecord = -1;

        private Session(Cat cat) {
            this.cat = cat;
            playNext();
        }

        /** @return true when the entity or trait disappeared. */
        private boolean tick() {
            Minecraft minecraft = Minecraft.getInstance();
            if (!cat.isAlive() || minecraft.level == null
                    || minecraft.level.getEntity(cat.getId()) != cat
                    || CatTraitData.read(cat)
                    .filter(profile -> profile.has(CatTrait.CAT_KING)).isEmpty()) {
                stop();
                return true;
            }
            if (graceTicks-- > 0 || current != null
                    && minecraft.getSoundManager().isActive(current)) return false;
            playNext();
            return false;
        }

        private void playNext() {
            int selected = RandomSource.create().nextInt(RECORDS.length);
            if (RECORDS.length > 1 && selected == lastRecord) {
                selected = (selected + 1) % RECORDS.length;
            }
            lastRecord = selected;
            current = new MovingRecordSound(RECORDS[selected], cat);
            graceTicks = 20;
            Minecraft.getInstance().getSoundManager().play(current);
        }

        private void stop() {
            if (current != null) Minecraft.getInstance().getSoundManager().stop(current);
            current = null;
        }
    }

    private static final class MovingRecordSound extends AbstractSoundInstance
            implements TickableSoundInstance {
        private final Cat cat;
        private boolean stopped;

        private MovingRecordSound(SoundEvent event, Cat cat) {
            super(event, SoundSource.RECORDS, RandomSource.create());
            this.cat = cat;
            this.looping = false;
            this.relative = false;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            this.volume = 1.0F;
            this.pitch = 1.0F;
            updatePosition();
        }

        @Override
        public void tick() {
            if (!cat.isAlive()) {
                stopped = true;
                return;
            }
            updatePosition();
        }

        private void updatePosition() {
            x = cat.getX();
            y = cat.getY();
            z = cat.getZ();
        }

        @Override
        public boolean isStopped() {
            return stopped;
        }
    }

    private CatKingAudioManager() {}
}
