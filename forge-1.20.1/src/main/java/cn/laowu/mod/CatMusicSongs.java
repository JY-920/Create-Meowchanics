package cn.laowu.mod;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.resources.ResourceLocation;
/** Loader adapter: Forge RecordItem and NeoForge data-component jukebox songs. */
public final class CatMusicSongs {
    public record Song(ResourceLocation sound, int ticks, String networkUrl, String title) {
        public Song(ResourceLocation sound, int ticks) { this(sound, ticks, "", ""); }
    }
    public static Song read(Cat cat, ItemStack stack) {
        var network = cn.laowu.mod.compat.netmusic.NetMusicDiscCompat.read(stack);
        if (network != null) return new Song(ResourceLocation.fromNamespaceAndPath("netmusic", "net_music"),
                network.ticks(), network.url(), network.title());
        if (stack.isEmpty() || !(stack.getItem() instanceof RecordItem disc)) return null;
        return new Song(disc.getSound().getLocation(), Math.max(1, disc.getLengthInTicks()));
    }
    private CatMusicSongs() {}
}
