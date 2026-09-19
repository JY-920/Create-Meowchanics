package cn.laowu.mod.client;

import cn.laowu.mod.compat.netmusic.NetMusicDiscCompat;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Real SoundEngine + installed NetMusic decoder; generated PCM served only on loopback. No user world. */
@EventBusSubscriber(modid="laowu", value=Dist.CLIENT, bus=EventBusSubscriber.Bus.MOD)
public final class NetMusicClientProbe {
    private static boolean ready, started;
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            org.lwjgl.glfw.GLFW.glfwHideWindow(Minecraft.getInstance().getWindow().getWindow());
            ready = true;
        });
    }
    @EventBusSubscriber(modid="laowu", value=Dist.CLIENT, bus=EventBusSubscriber.Bus.GAME)
    public static final class Ticks {
        @SubscribeEvent
        public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
            var mc = Minecraft.getInstance();
            if (!ready || started || mc.getOverlay() != null || mc.screen == null) return;
            started = true;
            CompletableFuture.runAsync(() -> {
                try { verify(mc); }
                catch (Throwable error) {
                    error.printStackTrace();
                    System.err.println("FAIL: NETMUSIC CLIENT");
                    // Only this purpose-built test process is closed; no existing game is touched.
                    mc.execute(mc::stop);
                    return;
                }
                System.out.println("PASS: NETMUSIC CLIENT: real decoder, PCM, independent streams, cancellation, actual SoundEngine and moving positional source");
                mc.execute(mc::stop);
            });
        }
    }
    private static void verify(Minecraft mc) throws Exception {
        mc.submit(() -> verifyDeathSettings(mc)).get(10, TimeUnit.SECONDS);
        check(NetMusicDiscCompat.loaded(), "Real NetMusic mod must be loaded");
        byte[] pcm = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(System.getProperty("laowu.netmusic_fixture")));
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var http = Executors.newCachedThreadPool(r -> { var t=new Thread(r, "NetMusic-probe-http"); t.setDaemon(true); return t; });
        server.setExecutor(http);
        server.createContext("/tone.mp3", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "audio/mpeg");
            exchange.sendResponseHeaders(200, pcm.length);
            try (var body = exchange.getResponseBody()) { body.write(pcm); }
        });
        server.createContext("/slow.mp3", exchange -> {
            entered.countDown();
            try { release.await(15, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            exchange.getResponseHeaders().set("Content-Type", "audio/mpeg");
            exchange.sendResponseHeaders(200, pcm.length);
            try (var body = exchange.getResponseBody()) { body.write(pcm); }
        });
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        try {
            try (var invalid = new NetMusicAudioBridge.Request("not a URL")) {
                check(invalid.future().isCompletedExceptionally(), "Invalid source fails immediately");
            }
            var first = new NetMusicAudioBridge.Request(base + "/tone.mp3");
            var second = new NetMusicAudioBridge.Request(base + "/tone.mp3");
            AudioStream one = first.future().get(20, TimeUnit.SECONDS);
            AudioStream two = second.future().get(20, TimeUnit.SECONDS);
            check(one.getFormat().getSampleRate() == 22050, "Native decoder preserves PCM sample rate");
            check(one.read(4096).remaining() > 0 && two.read(4096).remaining() > 0, "Native decoder emits playable PCM");
            first.close();
            await(() -> closed(one), "Cancelled open stream releases native decoder");
            check(two.read(4096).remaining() > 0, "Stopping one cat does not stop a different cat");
            second.close();
            await(() -> closed(two), "Second stream independently released");

            var delayed = new NetMusicAudioBridge.Request(base + "/slow.mp3");
            check(entered.await(10, TimeUnit.SECONDS), "Delayed request reached real decoder");
            long start = System.nanoTime();
            delayed.close();
            check(System.nanoTime() - start < Duration.ofMillis(100).toNanos(), "Cancel never waits for network on main thread");
            check(delayed.future().isCancelled(), "In-flight future cancelled");
            release.countDown();
            var poolField = NetMusicAudioBridge.class.getDeclaredField("OPENERS"); poolField.setAccessible(true);
            var pool = (ThreadPoolExecutor) poolField.get(null);
            await(() -> pool.getActiveCount() == 0, "Late decoder completes after cancelled download");
            var openedField = NetMusicAudioBridge.Request.class.getDeclaredField("opened"); openedField.setAccessible(true);
            check(((AtomicReference<?>) openedField.get(delayed)).get() == null, "Late stream not leaked by cancelled request");

            EngineSound sound = mc.submit(() -> {
                Cat cat = new Cat(EntityType.CAT, new AgentWatchVisualProbe.ProbeLevel());
                cat.setPos(1, 2, 3);
                var value = new EngineSound(cat, base + "/tone.mp3");
                cat.setPos(4, 5, 6); value.tick();
                check(value.getX() == 4 && value.getY() == 5.5 && value.getZ() == 6, "Cat audio source follows movement");
                // Keep it quiet in this hidden, isolated instance.
                mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.RECORDS).set(0.05);
                mc.getSoundManager().play(value);
                return value;
            }).get(10, TimeUnit.SECONDS);
            AudioStream actual = sound.opened.get(20, TimeUnit.SECONDS);
            check(actual.getFormat().getSampleSizeInBits() == 16, "Production RecordSound is decoded through actual SoundEngine");
            mc.submit(() -> {
                sound.presentationTick(mc);
                check(sound.presenting(), "Decoded active audio starts the cat performance");
                check(sound.performanceAge(0) == 0, "Animation starts when audio starts");
                verifyPerformance(sound);
            }).get(10, TimeUnit.SECONDS);
            mc.submit(() -> { sound.finish(); mc.getSoundManager().stop(sound); }).get(10, TimeUnit.SECONDS);
            await(() -> closed(actual), "Stopping positional source closes its native audio stream");
            check(sound.isStopped(), "Sound stays stopped");
            check(!sound.presenting(), "Stop removes performance and lyrics immediately");
            check(sound.getStream(null, null, false).isCompletedExceptionally(), "Released sound cannot reopen on delayed callback");
            var vanilla = mc.submit(() -> {
                var value = new EngineSound(new Cat(EntityType.CAT, new AgentWatchVisualProbe.ProbeLevel()),
                        ResourceLocation.tryParse("minecraft:music_disc.cat"));
                check(!value.presenting(), "No performance before audio opens");
                mc.getSoundManager().play(value);
                return value;
            }).get(10, TimeUnit.SECONDS);
            vanilla.opened.get(20, TimeUnit.SECONDS);
            mc.submit(() -> {
                vanilla.presentationTick(mc);
                check(vanilla.presenting(), "Vanilla disc performs");
                mc.getSoundManager().stop(vanilla);
            }).get(10, TimeUnit.SECONDS);
            // SoundEngine.stop queues work on its audio executor; channel retirement needs a client tick.
            await(() -> mc.submit(() -> !mc.getSoundManager().isActive(vanilla)).join(),
                    "Vanilla audio channel retires after stop");
            mc.submit(() -> {
                vanilla.presentationTick(mc);
                check(!vanilla.presenting(), "SoundEngine stop clears playback presentation");
            }).get(10, TimeUnit.SECONDS);
        } finally {
            release.countDown(); server.stop(0); http.shutdownNow();
        }
    }
    private static void verifyDeathSettings(Minecraft mc) {
        try {
            var tag=cn.laowu.mod.ServerConfig.snapshot();tag.putBoolean("can_edit",true);
            tag.putInt(cn.laowu.mod.ServerConfig.DEATH_OUTCOME_KEY,1);
            var screen=new WorldSettingsScreen(null);screen.receive(tag);screen.init(mc,520,380);
            var page=WorldSettingsScreen.class.getDeclaredMethod("changePage",int.class);page.setAccessible(true);page.invoke(screen,3);
            var mode=(net.minecraft.client.gui.components.Button) screenField(screen,"deathOutcome");
            var penalty=(net.minecraft.client.gui.components.Button) screenField(screen,"deathPenaltyToggle");
            var loss=(net.minecraft.client.gui.components.EditBox) screenField(screen,"deathLoss");
            var save=(net.minecraft.client.gui.components.Button) screenField(screen,"save");
            check(mode.visible&&penalty.visible&&loss.visible,"Item mode shows penalty controls");
            mode.onPress();
            check(penalty.visible&&loss.visible,"Entity mode shows penalty controls");
            loss.setValue("invalid");check(!save.active,"Visible invalid penalty prevents saving");
            mode.onPress();
            check(!penalty.visible&&!loss.visible&&save.active,"None mode hides penalty and can save despite hidden invalid text");
            check(loss.getValue().equals("invalid"),"Hiding never clears the draft");
            mode.onPress();
            check(penalty.visible&&loss.visible&&!save.active,"Returning to item mode restores pending validation");
            loss.setValue("7");check(save.active,"Valid penalty enables save again");
            tag.getCompound(cn.laowu.mod.ServerConfig.LOCKS_TAG).putBoolean(cn.laowu.mod.ServerConfig.DEATH_OUTCOME_KEY,true);
            screen.receive(tag);
            check(!((net.minecraft.client.gui.components.Button)screenField(screen,"deathOutcome")).active,"Server global mode lock disables selector");
            System.out.println("PASS: DEATH OUTCOME CLIENT UI: all modes, visibility, preserved drafts, validation and global lock");
        } catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
    private static Object screenField(WorldSettingsScreen screen,String name) throws ReflectiveOperationException {
        var field=WorldSettingsScreen.class.getDeclaredField(name);field.setAccessible(true);return field.get(screen);
    }
    @SuppressWarnings("unchecked")
    private static void verifyPerformance(EngineSound sound) {
        try {
            var field = CatMusicRecordClient.class.getDeclaredField("PLAYING"); field.setAccessible(true);
            var playing = (java.util.Map<java.util.UUID, CatMusicRecordClient.RecordSound>) field.get(null);
            var catField = CatMusicRecordClient.RecordSound.class.getDeclaredField("cat");
            catField.setAccessible(true);
            var cat = (Cat) catField.get(sound);
            cat.xo = cat.getX(); cat.yo = cat.getY(); cat.zo = cat.getZ();
            cat.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            cat.getPersistentData().putBoolean(cn.laowu.mod.CatClothesData.EQUIPPED_TAG, true);
            cat.getPersistentData().putString(cn.laowu.mod.CatClothesData.OUTFIT_TAG, "music");
            var root = HissingCatModel.createLayer().bakeRoot();
            var model = new HissingCatModel(root);
            for (int mode = 0; mode < 2; mode++) {
                cat.setUUID(new java.util.UUID(0, mode));
                playing.put(cat.getUUID(), sound);
                try {
                    check(CatMusicRecordClient.performing(cat), "Playing disc supplies public model state");
                    model.prepareMobModel(cat, 0, 0, 0);
                    model.setupAnim(cat, 0, 0, 12, 0, 0);
                    check(model.isPlayingPerformance(), "Disc playback drives actual loaded performance animation");
                    check(model.isPlayingPipa() == (mode == 1), "Stable sequence selects both pipa with prop and street dance");
                    check(!cn.laowu.mod.CatMusicSupport.performing(cat), "Disc playback must not fake a combat buff");
                    cat.setPos(cat.getX() + .2, cat.getY(), cat.getZ());
                    model.prepareMobModel(cat, 0, .3F, 0);
                    model.setupAnim(cat, 0, .3F, 13, 0, 0);
                    check(!model.isPlayingPerformance(), "Network position movement stops disc performance even with zero velocity");
                    check(sound.presenting(), "Movement must not stop music or notes");
                    cat.xo = cat.getX();
                    cat.setDeltaMovement(.15, 0, 0);
                    model.setupAnim(cat, 0, .3F, 14, 0, 0);
                    check(!model.isPlayingPerformance(), "Local horizontal movement stops disc performance");
                    cat.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                    cat.setPos(cat.getX(), cat.getY() + .2, cat.getZ());
                    model.setupAnim(cat, 0, 0, 15, 0, 0);
                    check(!model.isPlayingPerformance(), "Vertical displacement also stops disc performance");
                    cat.yo = cat.getY();
                    model.prepareMobModel(cat, 0, 0, 0);
                    model.setupAnim(cat, 0, 0, 16, 0, 0);
                    check(model.isPlayingPerformance(), "Stopping restores the performance without restarting audio");
                } finally { playing.remove(cat.getUUID()); }
            }
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
    static final class EngineSound extends CatMusicRecordClient.RecordSound {
        EngineSound(Cat cat, ResourceLocation id) { super(cat, id, 20, 600, ""); }
        final CompletableFuture<AudioStream> opened = new CompletableFuture<>();
        EngineSound(Cat cat, String source) {
            super(cat, ResourceLocation.fromNamespaceAndPath("netmusic", "net_music"), 19, 600, source);
        }
        // This test deliberately stays at the title screen; it has no live mc.level.
        @Override boolean obsolete() { return false; }
        @Override public synchronized CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers, Sound sound, boolean looping) {
            var result = super.getStream(buffers, sound, looping);
            result.whenComplete((stream, error) -> { if (error == null) opened.complete(stream); else opened.completeExceptionally(error); });
            return result;
        }
    }
    private static boolean closed(AudioStream stream) {
        try {
            var field = stream.getClass().getDeclaredField("closed"); field.setAccessible(true);
            return ((AtomicBoolean) field.get(stream)).get();
        } catch (Exception error) { throw new AssertionError(error); }
    }
    private static void await(java.util.function.BooleanSupplier condition, String message) throws Exception {
        long end = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < end) Thread.sleep(10);
        check(condition.getAsBoolean(), message);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
