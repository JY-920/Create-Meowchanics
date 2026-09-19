package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.compat.netmusic.NetMusicDiscCompat;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTraitProfile;
import cn.laowu.mod.network.MusicRecordPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.Map;
import java.util.UUID;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class NetMusicServerProbe {
    private static final String SOURCE = "https://127.0.0.1:9/probe-not-requested-on-server.ogg?name=music";
    @GameTest(template = "netmusic_probe", timeoutTicks = 120)
    public static void playlistAndMetadata(GameTestHelper h) throws Exception {
        boolean expected = Boolean.getBoolean("laowu.netmusic_expected");
        h.assertTrue(NetMusicDiscCompat.loaded() == expected, "Optional NetMusic runtime matches fixture");
        h.assertTrue(NetMusicDiscCompat.validSource(SOURCE), "HTTPS source accepted");
        for (String bad : new String[] {"", "javascript:alert(1)", "ftp://localhost/a", "http://", "http://x/#fragment", "file://remote/x", "x".repeat(4097)})
            h.assertTrue(!NetMusicDiscCompat.validSource(bad), "Malformed/unsupported source rejected");

        Cat cat = EntityType.CAT.create(h.getLevel());
        cat.setTame(true, false); cat.setOwnerUUID(UUID.randomUUID());
        cat.setNoAi(true); cat.setNoGravity(true);
        cat.moveTo(h.absolutePos(new BlockPos(2, 2, 2)), 0, 0);
        h.getLevel().addFreshEntity(cat);
        CatTraitData.set(cat, CatTraitProfile.EMPTY);
        CatClothesData.equip(cat, CatOutfitType.MUSIC);
        CatProfileData.beginViewing(cat); // Production profile holds AI, but must not stop the playlist.
        var inv = CatProfileData.openContainer(cat);
        int offset = CatProfileData.ACCESSORY_SLOTS;
        inv.setItem(0, new ItemStack(Items.MUSIC_DISC_11));
        inv.setItem(offset + 1, new ItemStack(Items.MUSIC_DISC_13));
        inv.setItem(offset + 8, new ItemStack(Items.MUSIC_DISC_CAT));
        h.assertTrue(CatMusicRecords.nextSlot(cat, -1) == 1, "Ignore accessory slots and empty inventory slots");
        CatMusicRecords.tick(cat);
        h.assertTrue(CatMusicRecords.playingSlot(cat) == 1, "Ordinary record plays with or without NetMusic");
        forceEnd(cat); CatMusicRecords.tick(cat);
        h.assertTrue(CatMusicRecords.playingSlot(cat) == 8, "Native playlist advances");
        forceEnd(cat); CatMusicRecords.tick(cat);
        h.assertTrue(CatMusicRecords.playingSlot(cat) == 1, "Native playlist wraps");
        packet(h, new MusicRecordPacket(cat.getId(), cat.getUUID(), "minecraft:music_disc.cat", 123, 100));
        packet(h, new MusicRecordPacket(cat.getId(), cat.getUUID(), "netmusic:net_music", 124, 900,
                SOURCE, "网络唱片 — 测试 🎵"));
        packet(h, new MusicRecordPacket(cat.getId(), cat.getUUID(), "", 0, 0));

        if (expected) {
            var id = ResourceLocation.fromNamespaceAndPath("netmusic", "music_cd");
            var blank = new ItemStack(BuiltInRegistries.ITEM.get(id));
            h.assertTrue(!blank.isEmpty() && NetMusicDiscCompat.read(blank) == null, "Blank network disc is not a song");
            h.assertTrue(NetMusicDiscCompat.read(new ItemStack(Items.STONE)) == null, "Ordinary item ignored");
            for (int seconds : new int[] {0, -1})
                h.assertTrue(NetMusicDiscCompat.read(disc(blank, SOURCE, "invalid", seconds)) == null, "Invalid duration skipped");
            h.assertTrue(NetMusicDiscCompat.read(disc(blank, "not a URL", "invalid", 10)) == null, "Bad URL skipped");
            var huge = NetMusicDiscCompat.read(disc(blank, SOURCE, "long", Integer.MAX_VALUE));
            h.assertTrue(huge.ticks() == Integer.MAX_VALUE, "Duration cannot overflow");
            var net = disc(blank, SOURCE, "网络测试曲", 31);
            var start = System.nanoTime();
            var track = NetMusicDiscCompat.read(net);
            h.assertTrue(System.nanoTime() - start < 1_000_000_000L, "Metadata never opens network on server");
            h.assertTrue(track.url().equals(SOURCE) && track.title().equals("网络测试曲") && track.ticks() == 620,
                    "Read actual NetMusic native metadata (NBT on Forge, component on NeoForge)");
            inv.setItem(offset + 4, net);
            inv.setItem(offset + 5, blank);
            h.assertTrue(CatMusicRecords.nextSlot(cat, 1) == 4 && CatMusicRecords.nextSlot(cat, 4) == 8,
                    "Native/network mixed order; blank disc skipped");
            forceEnd(cat); CatMusicRecords.tick(cat);
            h.assertTrue(CatMusicRecords.playingSlot(cat) == 4, "Network disc really becomes current session");
            Object state = session(cat);
            var songField = state.getClass().getDeclaredField("song"); songField.setAccessible(true);
            var song = (CatMusicSongs.Song) songField.get(state);
            h.assertTrue(song.networkUrl().equals(SOURCE) && song.ticks() == 620
                    && song.sound().toString().equals("netmusic:net_music"), "Session carries decoder event, source and time");
            var restored = EntityType.CAT.create(h.getLevel());
            restored.load(cat.saveWithoutId(new CompoundTag()));
            restored.setUUID(UUID.randomUUID()); // Separate fixture identity bypasses the live-container cache.
            var restoredDisc = CatProfileData.openContainer(restored).getItem(offset + 4);
            h.assertTrue(NetMusicDiscCompat.read(restoredDisc).equals(track), "Native network metadata survives cat save/reload");
            restored.discard();
            h.assertTrue(ItemStack.matches(inv.getItem(offset + 4), net) && net.getCount() == 1,
                    "Playback neither consumes nor edits network disc");
            var removed = inv.removeItem(offset + 4, 1);
            h.assertTrue(CatMusicRecords.playingSlot(cat) == 8, "Removing playing network disc immediately advances");
            inv.setItem(offset + 4, removed);
            forceEnd(cat); CatMusicRecords.tick(cat); forceEnd(cat); CatMusicRecords.tick(cat);
            h.assertTrue(CatMusicRecords.playingSlot(cat) == 4, "Mixed playlist wraps back to network disc");
            inv.setItem(offset + 4, disc(blank, SOURCE + "&v=2", "replacement", 40));
            h.assertTrue(CatMusicRecords.playingSlot(cat) == 8, "Replacing song metadata advances and invalidates old session");
        }
        inv.clearContent(); CatMusicRecords.tick(cat);
        h.assertTrue(CatMusicRecords.playingSlot(cat) == -1, "Empty nine-slot inventory stops audio");
        h.assertTrue(!CatChestData.hasInventory(cat), "No 27-slot inventory created");
        CatProfileData.endViewing(cat); cat.discard();
        System.out.println("PASS: NETMUSIC SERVER " + (expected ? "PRESENT: native metadata, mixed playlist, persistence, removal/replacement" : "ABSENT: vanilla fallback without optional linkage"));
        h.succeed();
    }

    private static ItemStack disc(ItemStack blank, String url, String name, int seconds) throws Exception {
        var cd = Class.forName("com.github.tartaricacid.netmusic.item.ItemMusicCD");
        var info = Class.forName("com.github.tartaricacid.netmusic.item.ItemMusicCD$SongInfo");
        Object value = info.getConstructor(String.class, String.class, int.class, boolean.class).newInstance(url, name, seconds, false);
        return (ItemStack) cd.getMethod("setSongInfo", info, ItemStack.class).invoke(null, value, blank.copy());
    }
    private static Object session(Cat cat) throws Exception {
        var field = CatMusicRecords.class.getDeclaredField("SESSIONS"); field.setAccessible(true);
        return ((Map<?, ?>) field.get(null)).get(cat);
    }
    private static void forceEnd(Cat cat) throws Exception {
        Object state = session(cat);
        var until = state.getClass().getDeclaredField("until"); until.setAccessible(true);
        until.setLong(state, cat.level().getGameTime());
    }
    private static void packet(GameTestHelper h, MusicRecordPacket packet) {
        var buf = new net.minecraft.network.RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            MusicRecordPacket.STREAM_CODEC.encode(buf, packet);
            h.assertTrue(packet.equals(MusicRecordPacket.STREAM_CODEC.decode(buf)) && !buf.isReadable(),
                    "Packet roundtrip preserves bounded URL, Unicode title, sequence and identity");
        } finally { buf.release(); }
    }
}
