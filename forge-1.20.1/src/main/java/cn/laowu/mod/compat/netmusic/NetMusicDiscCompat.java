package cn.laowu.mod.compat.netmusic;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** Optional NetMusic 1.5.x bridge. Uses its public item reader on both NBT and component ports. */
public final class NetMusicDiscCompat {
    public static final int MAX_URL = 4096;
    public static final int MAX_TITLE = 256;
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private static volatile Access access;
    private static volatile boolean unavailable;

    public record Track(String url, String title, int ticks) {}
    private record Access(Method read, Field url, Field title, Field seconds) {}

    public static boolean loaded() { return net.minecraftforge.fml.ModList.get().isLoaded("netmusic"); }

    /** Parsing only: no DNS, HTTP, sound classes, or client initialization on the server thread. */
    public static Track read(ItemStack stack) {
        if (stack.isEmpty() || !loaded()) return null;
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.getNamespace().equals("netmusic") || !id.getPath().equals("music_cd")) return null;
        try {
            Access api = access();
            if (api == null) return null;
            Object data = api.read.invoke(null, stack);
            if (data == null) return null;
            String url = (String) api.url.get(data);
            int seconds = api.seconds.getInt(data);
            if (!validSource(url) || seconds <= 0) return null;
            String title = (String) api.title.get(data);
            return new Track(url.strip(), title(title), (int) Math.min(Integer.MAX_VALUE, (long) seconds * 20));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            warn(error);
            return null;
        }
    }

    private static Access access() throws ReflectiveOperationException {
        if (access != null || unavailable) return access;
        synchronized (NetMusicDiscCompat.class) {
            if (access != null || unavailable) return access;
            try {
                Class<?> item = Class.forName("com.github.tartaricacid.netmusic.item.ItemMusicCD");
                Class<?> info = Class.forName("com.github.tartaricacid.netmusic.item.ItemMusicCD$SongInfo");
                access = new Access(item.getMethod("getSongInfo", ItemStack.class),
                        info.getField("songUrl"), info.getField("songName"), info.getField("songTime"));
            } catch (ReflectiveOperationException | LinkageError error) {
                unavailable = true;
                throw error;
            }
            return access;
        }
    }

    public static boolean validSource(String source) {
        if (source == null || source.isBlank() || source.length() > MAX_URL) return false;
        try {
            URI uri = URI.create(source.strip());
            String scheme = uri.getScheme();
            if (scheme == null || uri.getFragment() != null) return false;
            return switch (scheme.toLowerCase(Locale.ROOT)) {
                case "http", "https" -> uri.getHost() != null && !uri.getHost().isEmpty();
                // Matches NetMusic's local-disc support; the path is local to EACH listening client.
                case "file" -> uri.isAbsolute() && uri.getPath() != null && !uri.getPath().isEmpty()
                        && (uri.getHost() == null || uri.getHost().isEmpty());
                default -> false;
            };
        } catch (IllegalArgumentException error) { return false; }
    }

    public static String title(String title) {
        if (title == null || title.isBlank()) return "Net Music";
        title = title.replaceAll("[\\p{Cntrl}]", "").strip();
        if (title.isEmpty()) return "Net Music";
        if (title.length() <= MAX_TITLE) return title;
        int end = Character.isHighSurrogate(title.charAt(MAX_TITLE - 1)) ? MAX_TITLE - 1 : MAX_TITLE;
        return title.substring(0, end);
    }

    private static void warn(Throwable error) {
        if (WARNED.compareAndSet(false, true))
            LogUtils.getLogger().warn("Music cat: NetMusic item API unavailable ({}); ordinary records remain supported",
                    error.getClass().getSimpleName());
    }
    private NetMusicDiscCompat() {}
}
