package cn.laowu.mod.client;

import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.compat.netmusic.NetMusicDiscCompat;
import cn.laowu.mod.network.MusicRecordPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Cat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** One owned positional RECORDS-channel sound per musician; never stops another jukebox. */
@net.neoforged.fml.common.EventBusSubscriber(modid="laowu", value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class CatMusicRecordClient {
    private static final Map<UUID, RecordSound> PLAYING = new HashMap<>();
    private static Object world;
    private record Pending(MusicRecordPacket packet, long until) {}
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    public static void receive(MusicRecordPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        checkWorld(mc);
        if (mc.level == null) return;
        if (packet.sound().isEmpty() || packet.remaining() <= 0) {
            PENDING.remove(packet.uuid()); remove(mc, packet.uuid()); return;
        }
        if (!(mc.level.getEntity(packet.entityId()) instanceof Cat cat)
                || !cat.getUUID().equals(packet.uuid()) || CatClothesData.getOutfit(cat) != CatOutfitType.MUSIC) {
            if (PENDING.size() < 256) PENDING.put(packet.uuid(), new Pending(packet, mc.level.getGameTime() + 40));
            return;
        }
        PENDING.remove(packet.uuid());
        RecordSound current = PLAYING.get(packet.uuid());
        ResourceLocation id = ResourceLocation.tryParse(packet.sound());
        if (id == null) { remove(mc, packet.uuid()); return; }
        boolean network = !packet.networkUrl().isEmpty();
        if (network && (!NetMusicDiscCompat.loaded() || !NetMusicDiscCompat.validSource(packet.networkUrl())
                || !packet.sound().equals("netmusic:net_music"))) { remove(mc, packet.uuid()); return; }
        if (current != null && current.cat == cat && current.sequence == packet.sequence()
                && current.getLocation().equals(id) && current.networkUrl.equals(packet.networkUrl())) {
            // Keep completed/failed tracks latched until the server changes the sequence.
            // A one-second heartbeat must never trigger another download or restart the song.
            current.refresh(packet.remaining()); return;
        }
        remove(mc, packet.uuid());
        RecordSound sound = new RecordSound(cat, id, packet.sequence(), packet.remaining(), packet.networkUrl());
        PLAYING.put(cat.getUUID(), sound);
        mc.getSoundManager().play(sound);
        if (network) mc.gui.setNowPlaying(Component.literal(packet.title()));
    }
    private static void remove(Minecraft mc, UUID id) {
        RecordSound previous = PLAYING.remove(id);
        if (previous != null) { previous.finish(); mc.getSoundManager().stop(previous); }
    }
    private static void checkWorld(Minecraft mc) {
        if (world == mc.level) return;
        for (UUID id : List.copyOf(PLAYING.keySet())) remove(mc, id);
        PENDING.clear();
        world = mc.level;
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {

        Minecraft mc = Minecraft.getInstance(); checkWorld(mc);
        if (mc.level != null) for (UUID id : List.copyOf(PENDING.keySet())) {
            Pending pending = PENDING.get(id);
            if (pending.until <= mc.level.getGameTime()) { PENDING.remove(id); continue; }
            if (mc.level.getEntity(pending.packet.entityId()) instanceof Cat cat
                    && cat.getUUID().equals(id) && CatClothesData.getOutfit(cat) == CatOutfitType.MUSIC)
                receive(pending.packet);
        }
        // Muted/missing sounds may never enter SoundEngine's tickable list.
        for (UUID id : List.copyOf(PLAYING.keySet())) {
            RecordSound sound = PLAYING.get(id);
            if (sound.obsolete()) remove(mc, id);
            else if (sound.isStopped()) sound.finish(); // release stream, retain this sequence's latch
            else if (!mc.isPaused()) sound.presentationTick(mc);
        }
    }
    private static RecordSound presentation(Cat cat) {
        RecordSound sound = PLAYING.get(cat.getUUID());
        return sound != null && sound.cat == cat && sound.presenting() ? sound : null;
    }
    public static boolean performing(Cat cat) {
        // Tracked entities can move via position packets with a zero velocity vector.
        // Ignore idle vertical gravity velocity; actual vertical displacement still prevents dancing.
        double dx = cat.getX() - cat.xo, dy = cat.getY() - cat.yo, dz = cat.getZ() - cat.zo;
        return !cat.isPassenger() && dx * dx + dy * dy + dz * dz <= 1.0E-6
                && cat.getDeltaMovement().horizontalDistanceSqr() <= 1.0E-6
                && presentation(cat) != null;
    }
    public static int pose(Cat cat) {
        RecordSound sound = presentation(cat);
        return sound == null ? 0 : Math.floorMod(Long.hashCode(sound.sequence) ^ cat.getUUID().hashCode(), 2);
    }
    public static float age(Cat cat, float partial) {
        RecordSound sound = presentation(cat);
        return sound == null ? 0 : sound.performanceAge(partial);
    }
    static class RecordSound extends AbstractTickableSoundInstance {
        private final Cat cat;
        final long sequence;
        private final String networkUrl;
        private long expires;
        private volatile boolean released;
        private volatile boolean decoded;
        private boolean presenting;
        private int startedAt;
        boolean presenting() { return presenting && !released && !isStopped(); }
        float performanceAge(float partial) { return Math.max(0, cat.tickCount - startedAt + partial); }
        void presentationTick(Minecraft mc) {
            if (released || !decoded) return;
            if (!mc.getSoundManager().isActive(this)) {
                if (presenting) finish();
                return;
            }
            if (!presenting) {
                presenting = true; startedAt = cat.tickCount;
            }
            if (!cat.isInvisible() && (cat.tickCount - startedAt) % 10 == 0)
                cat.level().addParticle(net.minecraft.core.particles.ParticleTypes.NOTE,
                        cat.getX() + (cat.getRandom().nextDouble() - .5) * .6,
                        cat.getY() + cat.getBbHeight() + .35, cat.getZ(),
                        cat.getRandom().nextInt(24) / 24.0, 0, 0);
        }
        private NetMusicAudioBridge.Request request;
        RecordSound(Cat cat, ResourceLocation sound, long sequence, int remaining, String networkUrl) {
            super(SoundEvent.createVariableRangeEvent(sound), SoundSource.RECORDS, cat.getRandom());
            this.cat = cat; this.sequence = sequence; this.networkUrl = networkUrl;
            this.looping = false; this.delay = 0; this.volume = 4; this.pitch = 1;
            this.relative = false; this.attenuation = Attenuation.LINEAR;
            refresh(remaining); position();
        }
        @Override public boolean canStartSilent() { return true; }
        void refresh(int remaining) { expires = cat.level().getGameTime() + Math.min(60, Math.max(1, remaining)); }
        synchronized void finish() {
            released = true;
            presenting = false;
            stop();
            if (request != null) { request.close(); request = null; }
        }
        private void position() { x = cat.getX(); y = cat.getY() + .5; z = cat.getZ(); }
        boolean obsolete() {
            return !cat.isAlive() || cat.isRemoved() || Minecraft.getInstance().level != cat.level()
                    || CatClothesData.getOutfit(cat) != CatOutfitType.MUSIC || cat.level().getGameTime() >= expires;
        }
        @Override public synchronized CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers, Sound sound, boolean looping) {
            if (released) return CompletableFuture.failedFuture(new java.util.concurrent.CancellationException());
            if (networkUrl.isEmpty()) {
                var opening = super.getStream(buffers, sound, looping);
                opening.thenRun(() -> decoded = true);
                return opening;
            }
            if (request == null) {
                request = new NetMusicAudioBridge.Request(networkUrl);
                var opening = request.future();
                opening.whenComplete((stream, error) -> {
                    if (error == null && !released) decoded = true;
                    if (error == null || released) return;
                    Minecraft.getInstance().execute(() -> {
                        if (released) return;
                        finish();
                        if (Minecraft.getInstance().level == cat.level())
                            Minecraft.getInstance().gui.setOverlayMessage(
                                    Component.translatable("message.netmusic.music_player.play_error"), false);
                    });
                });
                return opening;
            }
            return request.future();
        }
        @Override public void tick() {
            if (obsolete()) { finish(); return; }
            position();
        }
    }
    private CatMusicRecordClient() {}
}
