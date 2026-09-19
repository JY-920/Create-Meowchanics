package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.network.CatEditorActionPacket;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class EditorNetworkingProbe {
    private static ServerPlayer player(GameTestHelper h) {
        var level = h.getLevel();
        var connection = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "editor-connection"));
        var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "editor-probe"), net.minecraft.server.level.ClientInformation.createDefault());
        player.connection = connection.connection;
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        player.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
        return player;
    }

    private static Cat cat(GameTestHelper h, ServerPlayer player) {
        var cat = EntityType.CAT.create(h.getLevel());
        cat.moveTo(player.getX() + 1, player.getY(), player.getZ(), 0, 0);
        cat.setNoAi(true);
        h.getLevel().addFreshEntity(cat);
        CatTraitData.set(cat, CatTraitProfile.EMPTY);
        return cat;
    }

    private static CatEditorActionPacket roundTrip(GameTestHelper h, int menu, int action) {
        var packet = new CatEditorActionPacket(menu, action);
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            CatEditorActionPacket.STREAM_CODEC.encode(buffer, packet);
            var decoded = CatEditorActionPacket.STREAM_CODEC.decode(buffer);
            h.assertTrue(decoded.equals(packet) && !buffer.isReadable(), "Lossless editor packet: " + action);
            return decoded;
        } finally { buffer.release(); }
    }

    private static void click(GameTestHelper h, ServerPlayer player, int action) {
        roundTrip(h, player.containerMenu.containerId, action).apply(player);
    }

    @GameTest(template = "accessory_probe", batch = "editor_network", timeoutTicks = 20)
    public static void traitButtonsSurviveNetwork(GameTestHelper h) {
        var player = player(h);
        var cat = cat(h, player);
        var menu = new CatTraitEditorMenu(91, player.getInventory(), cat);
        player.containerMenu = menu;
        h.assertTrue(h.getLevel().getEntity(cat.getUUID()) == cat, "Editor fixture must be indexed in the server level");
        h.assertTrue(menu.stillValid(player) && !player.isSpectator(),
                "Editor fixture must be alive, reachable and editable before the first packet; alive=" + cat.isAlive()
                        + ", distance=" + player.distanceToSqr(cat) + ", spectator=" + player.isSpectator());
        int count = menu.catalog().size();
        int tested = 0;
        for (var trait : CatTrait.values()) {
            if (ServerConfig.isTraitDisabled(trait)) continue;
            CatTraitData.set(cat, CatTraitProfile.EMPTY);
            click(h, player, count + trait.ordinal());
            h.assertTrue(menu.level(trait) == 1, "Add " + trait);
            click(h, player, count + trait.ordinal());
            h.assertTrue(menu.level(trait) == (trait.upgradable() ? 2 : 1), "Increment " + trait);
            click(h, player, 1000 + count + trait.ordinal());
            h.assertTrue(menu.level(trait) == trait.maxLevel(), "Shift maximum " + trait);
            click(h, player, trait.ordinal());
            h.assertTrue(menu.level(trait) == trait.maxLevel() - 1, "Decrement " + trait);
            click(h, player, 1000 + trait.ordinal());
            h.assertTrue(menu.level(trait) == 0, "Shift remove " + trait);
            click(h, player, 1000 + count + trait.ordinal());
            h.assertTrue(menu.level(trait) == 1, "Shift add still begins at level one: " + trait);
            tested++;
        }
        CatTraitData.set(cat, CatTraitProfile.EMPTY);
        for (var trait : new CatTrait[]{CatTrait.THORNS, CatTrait.NIGHT_OWL, CatTrait.HEAT_RESISTANCE, CatTrait.DOUGHY, CatTrait.LONG_FUR})
            click(h, player, count + trait.ordinal());
        h.assertTrue(CatTraitData.ensure(cat).traits().size() == 4 && menu.level(CatTrait.LONG_FUR) == 0, "Four-trait capacity remains");
        CatTraitData.set(cat, CatTraitProfile.EMPTY);
        click(h, player, count + CatTrait.GOOD_CAT.ordinal());
        click(h, player, count + CatTrait.SELECTED_ELDER.ordinal());
        h.assertTrue(menu.level(CatTrait.GOOD_CAT) == 1 && menu.level(CatTrait.SELECTED_ELDER) == 0, "Exclusive traits remain exclusive");
        menu.removed(player); cat.discard(); player.discard();
        System.out.println("PASS: serialized editor actions: " + tested + " traits, add/remove/increment/Shift and capacity/conflicts");
        h.succeed();
    }

    @GameTest(template = "accessory_probe", batch = "editor_network", timeoutTicks = 20)
    public static void attributeButtonsAndScope(GameTestHelper h) {
        var player = player(h);
        var cat = cat(h, player);
        var menu = new CatAttributeEditorMenu(92, player.getInventory(), cat);
        player.containerMenu = menu;
        for (var stat : CatStat.values()) {
            CatAttributeData.set(cat, CatAttributeData.ensure(cat).withValues(stat, 20, 50));
            click(h, player, 130 + stat.ordinal());
            h.assertTrue(menu.potential(stat) == 60, "Shift potential +10 above byte limit: " + stat);
            click(h, player, 120 + stat.ordinal());
            h.assertTrue(menu.potential(stat) == 50, "Shift potential -10: " + stat);
            click(h, player, 110 + stat.ordinal());
            h.assertTrue(menu.current(stat) == 30, "Shift current +10: " + stat);
            click(h, player, 100 + stat.ordinal());
            h.assertTrue(menu.current(stat) == 20, "Shift current -10: " + stat);
            click(h, player, 10 + stat.ordinal());
            h.assertTrue(menu.current(stat) == 21, "Normal current +1: " + stat);
        }
        menu.removed(player);
        var traits = new CatTraitEditorMenu(93, player.getInventory(), cat);
        player.containerMenu = traits;
        CatTraitData.set(cat, CatTraitProfile.EMPTY);
        int add = traits.action(CatTrait.SELECTED_ELDER, true, false);
        roundTrip(h, 94, add).apply(player);
        h.assertTrue(traits.level(CatTrait.SELECTED_ELDER) == 0, "Reject stale/wrong container id");
        for (int invalid : new int[]{-1, Integer.MIN_VALUE, Integer.MAX_VALUE, 999, 2000 + add}) click(h, player, invalid);
        h.assertTrue(CatTraitData.ensure(cat).traits().isEmpty(), "Reject malformed action ids");
        player.moveTo(player.getX() + 20, player.getY(), player.getZ(), 0, 0);
        click(h, player, add);
        h.assertTrue(traits.level(CatTrait.SELECTED_ELDER) == 0, "Reject out-of-reach target");
        player.moveTo(cat.getX() - 1, cat.getY(), cat.getZ(), 0, 0);
        player.setGameMode(GameType.SPECTATOR);
        click(h, player, add);
        h.assertTrue(traits.level(CatTrait.SELECTED_ELDER) == 0, "Spectators cannot mutate traits");
        player.setGameMode(GameType.CREATIVE);
        click(h, player, add);
        h.assertTrue(traits.level(CatTrait.SELECTED_ELDER) == 1, "Valid target still editable after rejected messages");
        traits.removed(player);
        var pancake = new ItemEntity(h.getLevel(), cat.getX(), cat.getY(), cat.getZ(), LaoWuMod.CAT_PANCAKE.get().getDefaultInstance());
        h.getLevel().addFreshEntity(pancake);
        CatTraitData.set(pancake.getItem(), CatTraitProfile.EMPTY);
        var itemMenu = new CatTraitEditorMenu(94, player.getInventory(), pancake);
        player.containerMenu = itemMenu;
        click(h, player, add);
        h.assertTrue(itemMenu.level(CatTrait.SELECTED_ELDER) == 1, "Dropped pancake editor uses the same network fix");
        pancake.discard();
        click(h, player, 1000 + CatTrait.SELECTED_ELDER.ordinal());
        h.assertTrue(itemMenu.level(CatTrait.SELECTED_ELDER) == 1, "Reject deleted targets");
        itemMenu.removed(player);
        player.containerMenu = player.inventoryMenu;
        roundTrip(h, player.inventoryMenu.containerId, add).apply(player);
        cat.discard(); player.discard();
        System.out.println("PASS: attribute signed-byte boundary, stale/malformed/spectator/distant/deleted/wrong-menu actions, pancake target");
        h.succeed();
    }
}
