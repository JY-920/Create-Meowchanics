package cn.laowu.mod.client;

import cn.laowu.mod.compat.netmusic.NetMusicDiscCompat;
import net.minecraft.client.sounds.AudioStream;
import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Reuses NetMusic's registered URL resolvers/codecs, without bundling or requiring that mod. */
final class NetMusicAudioBridge {
    private static final ExecutorService OPENERS = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(32), task -> {
                Thread thread = new Thread(task, "Meowchanics-NetMusic-open");
                thread.setDaemon(true);
                // Java Sound discovers NetMusic codecs through the mod-aware context loader.
                // SoundEngine/ForkJoin callers may otherwise supply the system loader.
                thread.setContextClassLoader(NetMusicAudioBridge.class.getClassLoader());
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    private static volatile Constructor<?> decoder;

    /** One request per playback sequence. Close is safe before, during, or after async URL resolution. */
    static final class Request implements AutoCloseable {
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicReference<OwnedStream> opened = new AtomicReference<>();
        private final CompletableFuture<AudioStream> future = new CompletableFuture<>();

        Request(String source) {
            if (!NetMusicDiscCompat.loaded() || !NetMusicDiscCompat.validSource(source)) {
                future.completeExceptionally(new IOException("NetMusic is unavailable or the source is invalid"));
                return;
            }
            try {
                OPENERS.execute(() -> {
                    if (cancelled.get()) { future.cancel(false); return; }
                    OwnedStream stream = null;
                    try {
                        stream = new OwnedStream(decode(URI.create(source.strip()).toURL()));
                        opened.set(stream);
                        if (cancelled.get() || !future.complete(stream)) {
                            opened.compareAndSet(stream, null);
                            stream.close();
                        }
                    } catch (Exception | LinkageError error) {
                        if (stream != null) stream.closeQuietly();
                        future.completeExceptionally(error);
                    }
                });
            } catch (RejectedExecutionException error) { future.completeExceptionally(error); }
            // A dead endpoint must not leave a cat's opener future pending forever. The worker
            // cannot always be interrupted inside a third-party codec; a late stream is closed above.
            future.orTimeout(30, TimeUnit.SECONDS);
        }
        CompletableFuture<AudioStream> future() { return future; }
        @Override public void close() {
            cancelled.set(true);
            future.cancel(false);
            OwnedStream stream = opened.getAndSet(null);
            if (stream != null) CompletableFuture.runAsync(stream::closeQuietly);
        }
    }

    private static AudioStream decode(URL source) throws ReflectiveOperationException {
        Constructor<?> factory = decoder;
        if (factory == null) {
            factory = Class.forName("com.github.tartaricacid.netmusic.client.audio.NetMusicAudioStream")
                    .getConstructor(URL.class);
            decoder = factory;
        }
        return (AudioStream) factory.newInstance(source);
    }

    /** Idempotent close also covers SoundEngine closing a stream after its cat was removed. */
    private static final class OwnedStream implements AudioStream {
        private final AudioStream delegate;
        private final AtomicBoolean closed = new AtomicBoolean();
        OwnedStream(AudioStream delegate) { this.delegate = delegate; }
        @Override public AudioFormat getFormat() { return delegate.getFormat(); }
        @Override public ByteBuffer read(int size) throws IOException {
            return closed.get() ? ByteBuffer.allocateDirect(0) : delegate.read(size);
        }
        @Override public void close() throws IOException {
            if (closed.compareAndSet(false, true)) delegate.close();
        }
        void closeQuietly() { try { close(); } catch (IOException ignored) {} }
    }
    private NetMusicAudioBridge() {}
}
