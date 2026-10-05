package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.item.CatTraitTokenItem;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import java.util.function.Predicate;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CatEditorMenuProbe {
    @GameTest(template = "artillery_probe", batch = "cat_editor_v2", timeoutTicks = 30)
    public static void separateRowAndAppearanceSlots(GameTestHelper h) {
        Fixture f = new Fixture(h);
        try {
            Cat cat = f.cat(f.pos.east(), true, true);
            CatEditorMenu menu = f.menu(cat);
            h.assertTrue(menu.input.getContainerSize() == 16,
                    "Four independent trait bottles and twelve material samples");
        } finally { f.close(); }
        h.succeed();
    }
    private static final ResourceLocation STONE = ResourceLocation.tryParse("laowu:block/minecraft/stone");
    @GameTest(template = "artillery_probe", batch = "cat_editor_v2", timeoutTicks = 30)
    public static void compactRowsPageGatesAndStaleDraft(GameTestHelper h) {
        var f = new Fixture(h);
        try {
            Cat cat = f.cat(f.pos.west(), true, true);
            CatTraitData.set(cat, CatTraitProfile.EMPTY.withLevel(CatTrait.NIGHT_OWL, 1).withLevel(CatTrait.THORNS, 1));
            var menu = f.menu(cat);
            h.assertTrue(menu.installedCount() == 2 && menu.installedIndex(2) == -1
                    && menu.slots.get(2).isActive() && !menu.slots.get(3).isActive(), "Only first empty row is insertable");
            menu.input.setItem(3, CatTraitTokenItem.create(CatTrait.TAIL_HELD_HIGH, 1));
            h.assertTrue(!menu.clickMenuButton(f.player, 23) && menu.installedCount() == 2, "Cannot install past first empty row");
            h.assertTrue(menu.clickMenuButton(f.player, action(menu, CatTrait.NIGHT_OWL)), "Extract first row");
            h.assertTrue(menu.installedCount() == 1 && menu.catalog().get(menu.installedIndex(0)).id().equals(CatTrait.THORNS.id())
                    && menu.installedIndex(1) == -1, "Remaining trait compacts to first row");
            h.assertTrue(CatTrait.NIGHT_OWL.id().equals(CatTraitTokenItem.traitId(menu.input.getItem(0))),
                    "Extracted token stays in its independent bottle");
            menu.clickMenuButton(f.player, 1);
            h.assertTrue(!menu.slots.get(0).isActive() && menu.slots.get(4).isActive()
                    && !menu.clickMenuButton(f.player, action(menu, CatTrait.THORNS)), "Appearance page hides and blocks trait operations");
            menu.input.setItem(4, new ItemStack(Items.GOLD_BLOCK, 2));
            CatGenomeData.set(cat, CatGenome.uniform(ResourceLocation.tryParse("minecraft:tabby")));
            var external = CatGenomeData.ensure(cat).save();
            h.assertTrue(!menu.clickMenuButton(f.player, CatEditorMenu.COMMIT)
                    && menu.input.getItem(4).getCount() == 2 && external.equals(CatGenomeData.ensure(cat).save()),
                    "Stale draft cannot overwrite newer appearance or consume samples");
            h.succeed();
        } finally { f.close(); }
    }
    private static final ResourceLocation GOLD = ResourceLocation.tryParse("laowu:block/minecraft/gold_block");

    @GameTest(template = "artillery_probe", batch = "cat_editor_v2", timeoutTicks = 30)
    public static void exhaustedRegionSampleCannotReapplyRemainingWholeSample(GameTestHelper h) {
        var f = new Fixture(h);
        try {
            Cat cat = f.cat(f.pos.west(), true, true);
            var vanilla = CatMaterialRegistry.catVariants().stream()
                    .filter(id -> id.getNamespace().equals("minecraft")).findFirst().orElseThrow();
            CatGenomeData.set(cat, CatGenome.uniform(vanilla));
            var menu = f.menu(cat);
            menu.clickMenuButton(f.player, 1);
            menu.input.setItem(4, new ItemStack(Items.GOLD_BLOCK, 2));
            menu.input.setItem(5, new ItemStack(Items.STONE, 1));
            h.assertTrue(menu.clickMenuButton(f.player, CatEditorMenu.COMMIT), "Mixed material commit succeeds");
            for (CatRegion region : CatRegion.values())
                h.assertTrue(CatGenomeData.ensure(cat).material(region).equals(
                        region == CatRegion.HEAD_PRIMARY ? STONE : GOLD), "Commit preserves local override: " + region);
            var committed = CatGenomeData.ensure(cat).save();
            h.assertTrue(menu.pendingGenome().save().equals(committed),
                    "Consumed local sample cannot expose remaining whole-body sample in the next draft");
            h.assertTrue(menu.input.getItem(4).isEmpty() && menu.input.getItem(5).isEmpty()
                    && total(f, stack -> stack.is(Items.GOLD_BLOCK)) == 1
                    && total(f, stack -> stack.is(Items.STONE)) == 0,
                    "Commit consumes exactly one of each used material and returns unused whole-body sample");
            h.assertTrue(menu.clickMenuButton(f.player, CatEditorMenu.COMMIT)
                    && CatGenomeData.ensure(cat).save().equals(committed)
                    && menu.pendingGenome().save().equals(committed)
                    && total(f, stack -> stack.is(Items.GOLD_BLOCK)) == 1
                    && total(f, stack -> stack.is(Items.STONE)) == 0,
                    "Duplicate commit neither changes appearance nor charges or duplicates materials");
            h.succeed();
        } finally { f.close(); }
    }

    private static final class Fixture {
        final GameTestHelper h;
        final BlockPos pos;
        final ServerPlayer player;
        final List<Cat> cats = new ArrayList<>();
        final List<net.minecraft.world.entity.Entity> seats = new ArrayList<>();
        final List<CatEditorMenu> menus = new ArrayList<>();

        Fixture(GameTestHelper h) {
            this.h = h; pos = h.absolutePos(new BlockPos(8, 3, 6));
            h.getLevel().setBlockAndUpdate(pos, LaoWuMod.CAT_EDITOR.get().defaultBlockState());
            var level = h.getLevel();
            var connection = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "editor-connection"));
            player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "editor-probe"), net.minecraft.server.level.ClientInformation.createDefault());
            player.connection = connection.connection;
            player.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + 2.5, 0, 0);
            player.getInventory().clearContent();
        }
        Cat cat(BlockPos seat, boolean owned, boolean sitting) {
            var cat = EntityType.CAT.create(h.getLevel());
            cat.setTame(true, true); cat.setOwnerUUID(owned ? player.getUUID() : UUID.randomUUID());
            cat.setNoAi(true); cat.setNoGravity(true); cat.setAge(0);
            cat.setPos(Vec3.atCenterOf(seat)); h.getLevel().addFreshEntity(cat); cats.add(cat);
            CatTraitData.set(cat, CatTraitProfile.EMPTY);
            CatGenomeData.set(cat, CatGenome.uniform(STONE));
            if (sitting) {
                h.getLevel().setBlockAndUpdate(seat.below(), Blocks.STONE.defaultBlockState());
                h.getLevel().setBlockAndUpdate(seat, AllBlocks.SEATS.get(DyeColor.WHITE).getDefaultState());
                SeatBlock.sitDown(h.getLevel(), seat, cat);
                h.assertTrue(cat.getVehicle() != null, "Fixture must mount a real Create seat");
                seats.add(cat.getVehicle()); cat.getVehicle().positionRider(cat);
                cat.setOrderedToSit(true); cat.setInSittingPose(true);
            }
            return cat;
        }
        CatEditorMenu menu(Cat cat) {
            var menu = new CatEditorMenu(121 + menus.size(), player.getInventory(), cat, pos);
            menus.add(menu); player.containerMenu = menu; return menu;
        }
        void close() {
            for (var menu : menus) menu.removed(player);
            for (var cat : cats) { cat.stopRiding(); cat.discard(); }
            for (var seat : seats) seat.discard();
            for (var drop : h.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(6))) drop.discard();
            player.discard();
        }
    }

    private static int action(CatEditorMenu menu, CatTrait trait) {
        for (int i = 0; i < menu.catalog().size(); i++)
            if (menu.catalog().get(i).id().equals(trait.id())) return 1000 + i;
        throw new AssertionError("Expected built-in trait in editor roster");
    }
    private static int total(Fixture f, Predicate<ItemStack> match) {
        int count = 0;
        for (int i = 0; i < f.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = f.player.getInventory().getItem(i); if (match.test(stack)) count += stack.getCount();
        }
        for (var drop : f.h.getLevel().getEntitiesOfClass(ItemEntity.class, f.player.getBoundingBox().inflate(6)))
            if (match.test(drop.getItem())) count += drop.getItem().getCount();
        return count;
    }
    private static void rejectedWithoutMutation(Fixture f, CatEditorMenu menu, Cat cat, String reason) {
        var genome = CatGenomeData.ensure(cat).save();
        var traits = CatTraitData.ensure(cat).save();
        menu.input.setItem(4, new ItemStack(Items.GOLD_BLOCK, 2));
        menu.input.setItem(1, CatTraitTokenItem.create(CatTrait.NIGHT_OWL, 3));
        hAssert(f, !menu.stillValid(f.player), reason + " invalidates menu");
        for (int button : new int[]{0, 1, CatEditorMenu.RESET, CatEditorMenu.COMMIT, menu.selectionAction(0, 0), 20, action(menu, CatTrait.THORNS)})
            hAssert(f, !menu.clickMenuButton(f.player, button), reason + " rejects action " + button);
        hAssert(f, genome.equals(CatGenomeData.ensure(cat).save()) && traits.equals(CatTraitData.ensure(cat).save()),
                reason + " never edits locked cat");
        hAssert(f, menu.input.getItem(4).getCount() == 2 && menu.input.getItem(1).getCount() == 1
                && menu.input.getItem(2).isEmpty(), reason + " never consumes input or manufactures output");
    }
    private static void hAssert(Fixture f, boolean condition, String message) { f.h.assertTrue(condition, message); }

    private static cn.laowu.mod.network.CatEditorActionPacket roundTrip(GameTestHelper h, int menu, int action) {
        var packet = new cn.laowu.mod.network.CatEditorActionPacket(menu, action);
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            cn.laowu.mod.network.CatEditorActionPacket.STREAM_CODEC.encode(buffer, packet);
            var decoded = cn.laowu.mod.network.CatEditorActionPacket.STREAM_CODEC.decode(buffer);
            h.assertTrue(decoded.containerId() == menu && decoded.actionId() == action && !buffer.isReadable(),
                    "Actual editor payload preserves full integer action " + action);
            return decoded;
        } finally { buffer.release(); }
    }

    @GameTest(template = "artillery_probe", batch = "cat_editor_packet", timeoutTicks = 30)
    public static void fullIntegerExtractionPacketChecksWindowAndCannotDuplicate(GameTestHelper h) {
        for(int boundary : new int[]{1000,1044,1124,1511,Integer.MAX_VALUE,-1})
            roundTrip(h,97,boundary);
        var f = new Fixture(h);
        try {
            Cat cat = f.cat(f.pos.west(), true, true);
            CatTraitData.set(cat, CatTraitProfile.EMPTY.withLevel(CatTrait.THORNS, 3));
            var menu = f.menu(cat);
            int button = action(menu, CatTrait.THORNS);
            h.assertTrue(button >= 1000 && menu.stillValid(f.player) && !f.player.isSpectator(),
                    "Packet fixture is a currently open, editable cat editor with a large extraction ID");
            roundTrip(h, menu.containerId + 1, button).apply(f.player);
            h.assertTrue(CatTraitData.ensure(cat).rawLevel(CatTrait.THORNS) == 3 && menu.input.getItem(0).isEmpty(),
                    "Wrong window ID refuses large action without removing trait or creating output");
            var packet = roundTrip(h, menu.containerId, button);
            packet.apply(f.player);
            ItemStack token = menu.input.getItem(0);
            h.assertTrue(CatTraitData.ensure(cat).rawLevel(CatTrait.THORNS) == 0
                    && token.getCount() == 1 && CatTrait.THORNS.id().equals(CatTraitTokenItem.traitId(token))
                    && CatTraitTokenItem.level(token) == 3, "Full integer payload reaches the real cat editor and extracts once");
            packet.apply(f.player);
            h.assertTrue(menu.input.getItem(0).getCount() == 1, "Duplicate packet with occupied output cannot copy token");
            ItemStack taken = menu.input.removeItemNoUpdate(0);
            roundTrip(h, menu.containerId, button).apply(f.player);
            h.assertTrue(menu.input.getItem(0).isEmpty() && taken.getCount() == 1
                    && CatTraitData.ensure(cat).rawLevel(CatTrait.THORNS) == 0,
                    "Repeated full-integer packet after taking output cannot copy removed trait");
            h.succeed();
        } finally { f.close(); }
    }

    @GameTest(template = "artillery_probe", batch = "cat_editor_menu", timeoutTicks = 30)
    public static void horizontalOwnedSeatsAndStableSelection(GameTestHelper h) {
        var f = new Fixture(h);
        try {
            for (var direction : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                Cat cat = f.cat(f.pos.relative(direction), true, true);
                h.assertTrue(CatEditorMenu.eligible(cat, f.player, f.pos) && CatEditorMenu.select(f.player, f.pos) == cat,
                        "Each horizontal adjacent seat selects its owned cat: " + direction);
                cat.stopRiding(); cat.discard(); f.seats.get(f.seats.size() - 1).discard();
            }
            Cat noSeat = f.cat(f.pos.north(), true, false);
            Cat foreign = f.cat(f.pos.south(), false, true);
            Cat diagonal = f.cat(f.pos.offset(1, 0, 1), true, true);
            Cat vertical = f.cat(f.pos.west().above(), true, true);
            for (Cat cat : new Cat[]{noSeat, foreign, diagonal, vertical})
                h.assertTrue(!CatEditorMenu.eligible(cat, f.player, f.pos), "Ineligible ownership/seat/diagonal/vertical candidate");
            h.assertTrue(CatEditorMenu.select(f.player, f.pos) == null, "No eligible cat means no target");
            Cat a = f.cat(f.pos.west(), true, true), b = f.cat(f.pos.east(), true, true);
            Cat selected = CatEditorMenu.select(f.player, f.pos);
            h.assertTrue(selected == a || selected == b, "Multiple cats select exactly one eligible target");
            for (int i = 0; i < 20; i++) h.assertTrue(CatEditorMenu.select(f.player, f.pos) == selected, "Selection remains stable");
            var menu = f.menu(selected); menu.input.setItem(5, new ItemStack(Items.GOLD_BLOCK));
            h.assertTrue(menu.clickMenuButton(f.player, 1) && menu.clickMenuButton(f.player, CatEditorMenu.COMMIT), "Selected cat is editable");
            Cat other = selected == a ? b : a;
            h.assertTrue(CatGenomeData.ensure(other).equals(CatGenome.uniform(STONE)), "Editing selected cat leaves other cat untouched");
            h.succeed();
        } finally { f.close(); }
    }

    @GameTest(template = "artillery_probe", batch = "cat_editor_menu", timeoutTicks = 30)
    public static void targetDepartureOwnershipAndReachRejectAllActions(GameTestHelper h) {
        var f = new Fixture(h);
        try {
            Cat cat = f.cat(f.pos.west(), true, true);
            CatTraitData.set(cat, CatTraitProfile.EMPTY.withLevel(CatTrait.THORNS, 3));
            var menu = f.menu(cat); h.assertTrue(menu.stillValid(f.player), "Owned seated target begins valid");
            cat.setOwnerUUID(UUID.randomUUID());
            rejectedWithoutMutation(f, menu, cat, "Ownership change");
            cat.setOwnerUUID(f.player.getUUID());
            f.player.moveTo(f.pos.getX() + 20, f.pos.getY(), f.pos.getZ(), 0, 0);
            rejectedWithoutMutation(f, menu, cat, "Out of reach");
            f.player.moveTo(f.pos.getX() + .5, f.pos.getY(), f.pos.getZ() + 2.5, 0, 0);
            cat.stopRiding();
            Cat replacement = f.cat(f.pos.east(), true, true);
            rejectedWithoutMutation(f, menu, cat, "Locked cat leaves seat");
            h.assertTrue(menu.cat(f.player) == cat && CatGenomeData.ensure(replacement).equals(CatGenome.uniform(STONE)),
                    "Departed target is never silently replaced by another seated cat");
            h.succeed();
        } finally { f.close(); }
    }

    @GameTest(template = "artillery_probe", batch = "cat_editor_menu", timeoutTicks = 30)
    public static void materialActionsConsumeOneAndRespectScope(GameTestHelper h) {
        var f = new Fixture(h);
        try {
            Cat cat = f.cat(f.pos.west(), true, true);
            var attributes = CatAttributeData.ensure(cat).save();
            var owner = cat.getOwnerUUID(); var outfit = CatClothesData.getOutfit(cat);
            var menu = f.menu(cat);
            menu.clickMenuButton(f.player, 1);
            menu.input.setItem(5, new ItemStack(Items.GOLD_BLOCK, 3));
            h.assertTrue(CatGenomeData.ensure(cat).equals(CatGenome.uniform(STONE))
                    && menu.pendingGenome().material(CatRegion.HEAD_PRIMARY).equals(GOLD),
                    "Sample previews head without changing real genome");
            h.assertTrue(menu.clickMenuButton(f.player, CatEditorMenu.COMMIT) && menu.input.getItem(5).isEmpty()
                    && total(f, stack -> stack.is(Items.GOLD_BLOCK)) == 2,
                    "Commit consumes exactly one used block and returns remaining samples");
            for (CatRegion region : CatRegion.values())
                h.assertTrue(CatGenomeData.ensure(cat).material(region).equals(region == CatRegion.HEAD_PRIMARY ? GOLD : STONE),
                        "Single region preserves every unselected region: " + region);
            h.assertTrue(menu.clickMenuButton(f.player, CatEditorMenu.COMMIT) && menu.input.getItem(5).isEmpty()
                    && total(f, stack -> stack.is(Items.GOLD_BLOCK)) == 2,
                    "Repeated commit of unchanged draft consumes nothing");
            menu.input.setItem(4, new ItemStack(Items.GOLD_BLOCK, 3));
            menu.input.setItem(5, new ItemStack(Items.GOLD_BLOCK, 2));
            h.assertTrue(menu.clickMenuButton(f.player, CatEditorMenu.COMMIT) && menu.input.getItem(4).isEmpty()
                    && menu.input.getItem(5).isEmpty() && total(f, stack -> stack.is(Items.GOLD_BLOCK)) == 6,
                    "Whole-body consumes once, unchanged head sample costs nothing, and all remainder returns");
            for (CatRegion region : CatRegion.values())
                h.assertTrue(CatGenomeData.ensure(cat).material(region).equals(GOLD), "Whole body changes every region");
            var before = CatGenomeData.ensure(cat).save();
            h.assertTrue(menu.clickMenuButton(f.player, menu.selectionAction(2, 0)), "Dropdown draft accepts vanilla material");
            menu.input.setItem(6, new ItemStack(Items.STONE, 2));
            h.assertTrue(menu.pendingGenome().material(CatRegion.values()[1]).equals(STONE)
                    && before.equals(CatGenomeData.ensure(cat).save()), "Block sample overrides dropdown without applying");
            int returned = total(f, stack -> stack.is(Items.STONE));
            h.assertTrue(menu.clickMenuButton(f.player, CatEditorMenu.RESET) && menu.selection(2) == -1
                    && menu.input.getItem(6).isEmpty() && total(f, stack -> stack.is(Items.STONE)) == returned + 2
                    && menu.pendingGenome().save().equals(before), "Reset cancels choices and returns samples without editing");
            for (int button : new int[]{100, 101, -1, Integer.MAX_VALUE, 112, 999})
                h.assertTrue(!menu.clickMenuButton(f.player, button), "Reject obsolete or invalid action: " + button);
            h.assertTrue(before.equals(CatGenomeData.ensure(cat).save()), "Rejected actions never alter appearance");
            h.assertTrue(attributes.equals(CatAttributeData.ensure(cat).save()) && owner.equals(cat.getOwnerUUID())
                    && Objects.equals(outfit, CatClothesData.getOutfit(cat)), "Appearance never changes owner, attributes or career");
            h.succeed();
        } finally { f.close(); }
    }

    @GameTest(template = "artillery_probe", batch = "cat_editor_menu", timeoutTicks = 30)
    public static void fullOutputAndConcurrentClicksNeverDuplicate(GameTestHelper h) {
        var f = new Fixture(h);
        try {
            Cat cat = f.cat(f.pos.west(), true, true);
            CatTraitData.set(cat, CatTraitProfile.EMPTY.withLevel(CatTrait.THORNS, 3));
            var first = f.menu(cat); var second = f.menu(cat);
            int extract = action(first, CatTrait.THORNS);
            first.input.setItem(0, new ItemStack(Items.STONE));
            h.assertTrue(!first.clickMenuButton(f.player, extract) && CatTraitData.ensure(cat).rawLevel(CatTrait.THORNS) == 3
                    && first.input.getItem(0).is(Items.STONE), "Full output refuses extraction without removing trait");
            first.input.setItem(0, ItemStack.EMPTY);
            h.assertTrue(first.clickMenuButton(f.player, extract), "First server extraction succeeds");
            ItemStack output = first.input.removeItemNoUpdate(0);
            h.assertTrue(CatTraitTokenItem.level(output) == 3 && output.getCount() == 1, "Whole trait and original level are output once");
            h.assertTrue(!first.clickMenuButton(f.player, extract) && !second.clickMenuButton(f.player, action(second, CatTrait.THORNS))
                    && first.input.getItem(0).isEmpty() && second.input.getItem(0).isEmpty(), "Repeated and concurrent extraction cannot copy removed trait");
            first.input.setItem(0, output);
            h.assertTrue(first.clickMenuButton(f.player, 20) && first.input.getItem(0).isEmpty()
                    && CatTraitData.ensure(cat).rawLevel(CatTrait.THORNS) == 3, "Installation consumes one token and restores original level");
            h.assertTrue(!first.clickMenuButton(f.player, 20) && CatTraitData.ensure(cat).rawLevel(CatTrait.THORNS) == 3,
                    "Repeated installation cannot upgrade or duplicate");
            h.succeed();
        } finally { f.close(); }
    }

    @GameTest(template = "artillery_probe", batch = "cat_editor_menu", timeoutTicks = 30)
    public static void shiftMoveAndCloseReturnItemsExactlyOnce(GameTestHelper h) {
        var f = new Fixture(h);
        try {
            Cat cat = f.cat(f.pos.west(), true, true); var menu = f.menu(cat);
            f.player.getInventory().setItem(0, new ItemStack(Items.GOLD_BLOCK, 3));
            f.player.getInventory().setItem(1, CatTraitTokenItem.create(CatTrait.THORNS, 3));
            menu.clickMenuButton(f.player, 1);
            h.assertTrue(!menu.quickMoveStack(f.player, 43).isEmpty() && menu.input.getItem(4).getCount() == 3
                    && f.player.getInventory().getItem(0).isEmpty(), "Shift moving sample transfers, never copies");
            menu.clickMenuButton(f.player, 0);
            h.assertTrue(!menu.quickMoveStack(f.player, 44).isEmpty() && menu.input.getItem(0).getCount() == 1
                    && f.player.getInventory().getItem(1).isEmpty(), "Shift moving token transfers once");
            menu.clickMenuButton(f.player, 1);
            h.assertTrue(!menu.quickMoveStack(f.player, 4).isEmpty() && menu.input.getItem(4).isEmpty()
                    && total(f, s -> s.is(Items.GOLD_BLOCK)) == 3, "Shift returning sample preserves exact count");
            h.assertTrue(menu.quickMoveStack(f.player, 4).isEmpty(), "Repeated empty shift cannot copy");
            menu.input.setItem(2, CatTraitTokenItem.create(CatTrait.NIGHT_OWL, 2));
            h.assertTrue(!menu.slots.get(2).mayPlace(new ItemStack(Items.STONE)), "Output rejects item insertion");
            menu.removed(f.player); menu.removed(f.player);
            h.assertTrue(menu.input.isEmpty() && total(f, s -> s.is(Items.GOLD_BLOCK)) == 3
                    && total(f, s -> s.is(LaoWuMod.CAT_TRAIT_TOKEN.get())) == 2, "Closing twice returns all transient items exactly once");
            h.assertTrue(!menu.stillValid(f.player) && menu.quickMoveStack(f.player, 1).isEmpty(), "Closed menu cannot transfer again");

            var full = f.menu(cat);
            for (int i = 0; i < 36; i++) f.player.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
            int goldBefore = total(f, s -> s.is(Items.GOLD_BLOCK));
            int tokenBefore = total(f, s -> s.is(LaoWuMod.CAT_TRAIT_TOKEN.get()));
            full.input.setItem(4, new ItemStack(Items.GOLD_BLOCK, 3));
            full.input.setItem(1, CatTraitTokenItem.create(CatTrait.THORNS, 3));
            full.input.setItem(2, CatTraitTokenItem.create(CatTrait.NIGHT_OWL, 2));
            full.removed(f.player); full.removed(f.player);
            h.assertTrue(full.input.isEmpty() && total(f, s -> s.is(Items.GOLD_BLOCK)) == goldBefore + 3
                    && total(f, s -> s.is(LaoWuMod.CAT_TRAIT_TOKEN.get())) == tokenBefore + 2, "Full-inventory close drops inputs/output once instead of losing or copying");
            h.succeed();
        } finally { f.close(); }
    }
}
