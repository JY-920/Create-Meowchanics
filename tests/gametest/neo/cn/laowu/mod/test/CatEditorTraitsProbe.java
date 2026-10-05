package cn.laowu.mod.test;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.*;
import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CatEditorTraitsProbe {
    private static final ResourceLocation TRANSFER = id("testpack:editor_transfer");
    private static final ResourceLocation KEPT = id("testpack:editor_kept");
    private static final ResourceLocation CONFLICT = id("testpack:editor_conflict");
    private static final ResourceLocation DISABLED = id("testpack:editor_disabled");
    private static final ResourceLocation SLOT = id("testpack:editor_slot");

    private static ResourceLocation id(String value) { return ResourceLocation.tryParse(value); }

    // Reflection lets the missing feature fail at runtime before production classes exist.
    private static Object invoke(GameTestHelper h, String className, String method, Class<?>[] types, Object... args) {
        try { return Class.forName(className).getMethod(method, types).invoke(null, args); }
        catch (ClassNotFoundException | NoSuchMethodException missing) {
            h.assertTrue(false, "Cat editor trait feature missing: " + className + "." + method);
            throw new AssertionError(missing);
        } catch (InvocationTargetException error) { throw new RuntimeException(error.getCause()); }
        catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
    }

    private static ItemStack token(GameTestHelper h, CatTraitType type, int level) {
        return (ItemStack) invoke(h, "cn.laowu.mod.item.CatTraitTokenItem", "create",
                new Class<?>[]{CatTraitType.class, int.class}, type, level);
    }
    private static ItemStack extract(GameTestHelper h, Cat cat, ResourceLocation id) {
        return (ItemStack) invoke(h, "cn.laowu.mod.CatEditorTraits", "extract",
                new Class<?>[]{Cat.class, ResourceLocation.class}, cat, id);
    }
    private static boolean install(GameTestHelper h, Cat cat, ItemStack stack) {
        return (boolean) invoke(h, "cn.laowu.mod.CatEditorTraits", "install",
                new Class<?>[]{Cat.class, ItemStack.class}, cat, stack);
    }
    private static ResourceLocation tokenId(GameTestHelper h, ItemStack stack) {
        return (ResourceLocation) invoke(h, "cn.laowu.mod.item.CatTraitTokenItem", "traitId",
                new Class<?>[]{ItemStack.class}, stack);
    }
    private static int tokenLevel(GameTestHelper h, ItemStack stack) {
        return (int) invoke(h, "cn.laowu.mod.item.CatTraitTokenItem", "level",
                new Class<?>[]{ItemStack.class}, stack);
    }

    private static Cat cat(GameTestHelper h) {
        Cat cat = EntityType.CAT.create(h.getLevel());
        cat.setNoAi(true); cat.setNoGravity(true);
        cat.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 2, 2))));
        h.getLevel().addFreshEntity(cat);
        CatTraitData.set(cat, CatTraitProfile.EMPTY);
        return cat;
    }
    private static CatTraitProfile profile(String... ids) {
        var root = new CompoundTag(); root.putInt("Version", 1);
        var entries = new ListTag();
        for (String id : ids) {
            var entry = new CompoundTag(); entry.putString("Id", id); entry.putInt("Level", 7); entries.add(entry);
        }
        root.put("Traits", entries);
        return CatTraitProfile.load(root).orElseThrow();
    }
    private static int raw(Cat cat, ResourceLocation id) {
        return CatTraitData.read(cat).orElseThrow().rawLevel(CatTraitRegistry.resolve(id, false));
    }
    private static Map<ResourceLocation, JsonElement> definitions() {
        var backup = new LinkedHashMap<ResourceLocation, JsonElement>();
        var entries = CatTraitRegistry.networkData().getList("Definitions", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.getCompound(i);
            backup.put(id(entry.getString("Id")), JsonParser.parseString(entry.getString("Json")));
        }
        return backup;
    }
    private static void reload(Map<ResourceLocation, JsonElement> values) {
        try {
            var method = CatTraitRegistry.class.getDeclaredMethod("apply", Map.class,
                    net.minecraft.server.packs.resources.ResourceManager.class, net.minecraft.util.profiling.ProfilerFiller.class);
            method.setAccessible(true); method.invoke(new CatTraitRegistry(), values, null, null);
        } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
    }
    private static Map<ResourceLocation, JsonElement> fixture(Map<ResourceLocation, JsonElement> backup) {
        var result = new LinkedHashMap<>(backup);
        result.put(TRANSFER, JsonParser.parseString("{\"schema_version\":1,\"title\":\"Transfer\",\"rarity\":\"common\",\"max_level\":2,\"slots\":[\"speed_bonus\"],\"conflicts\":[\"testpack:editor_conflict\"]}"));
        result.put(CONFLICT, JsonParser.parseString("{\"schema_version\":1,\"title\":\"Conflict\",\"rarity\":\"common\",\"max_level\":7}"));
        result.put(DISABLED, JsonParser.parseString("{\"schema_version\":1,\"title\":\"Disabled\",\"rarity\":\"common\",\"enabled\":false}"));
        result.put(SLOT, JsonParser.parseString("{\"schema_version\":1,\"title\":\"Slot\",\"rarity\":\"common\",\"slots\":[\"speed_bonus\"]}"));
        return result;
    }
    private static void unchanged(GameTestHelper h, Cat cat, ItemStack stack, String reason) {
        var before = CatTraitData.read(cat).orElseThrow().save();
        var state = cat.getPersistentData().getCompound(CatTraitScriptState.TAG).copy();
        int count = stack.getCount();
        h.assertTrue(!install(h, cat, stack), reason + " refuses install");
        h.assertTrue(before.equals(CatTraitData.read(cat).orElseThrow().save()) && count == stack.getCount(),
                reason + " does not mutate profile or consume token");
        h.assertTrue(state.equals(cat.getPersistentData().getCompound(CatTraitScriptState.TAG)),
                reason + " preserves script state");
    }

    @GameTest(template = "artillery_probe", batch = "cat_editor_traits", timeoutTicks = 30)
    public static void wholeRawLevelRoundTrip(GameTestHelper h) {
        var backup = definitions(); reload(fixture(backup));
        Cat from = cat(h), to = cat(h);
        try {
            CatTraitData.set(from, profile(TRANSFER.toString(), KEPT.toString()));
            var state = new CompoundTag(); state.putInt("kept", 41);
            CatTraitScriptState.write(from, KEPT.toString(), state);
            ItemStack stack = extract(h, from, TRANSFER);
            h.assertTrue(!stack.isEmpty() && stack.getCount() == 1 && stack.getMaxStackSize() == 1,
                    "Extraction returns exactly one nonstackable token");
            h.assertTrue(TRANSFER.equals(tokenId(h, stack)) && tokenLevel(h, stack) == 7,
                    "Token stores full raw level despite current definition maximum of two");
            h.assertTrue(raw(from, TRANSFER) == 0 && raw(from, KEPT) == 7, "Extraction removes whole trait only");
            h.assertTrue(CatTraitScriptState.read(from, KEPT.toString()).getInt("kept") == 41, "Unknown other trait state survives extraction");
            h.assertTrue(install(h, to, stack) && stack.isEmpty() && raw(to, TRANSFER) == 7,
                    "Successful install consumes one token and preserves raw seven");
            h.assertTrue(CatTraitData.read(to).orElseThrow().level(CatTraitRegistry.resolve(TRANSFER, false)) == 2,
                    "Effective level still obeys current definition without rewriting ownership");
            h.assertTrue(extract(h, from, TRANSFER).isEmpty(), "Repeated extraction cannot duplicate token");
            h.succeed();
        } finally { reload(backup); from.discard(); to.discard(); }
    }

    @GameTest(template = "artillery_probe", batch = "cat_editor_traits", timeoutTicks = 30)
    public static void rejectsDuplicateConflictAndFull(GameTestHelper h) {
        var backup = definitions(); reload(fixture(backup)); Cat cat = cat(h);
        try {
            var type = CatTraitRegistry.resolve(TRANSFER, false);
            CatTraitData.set(cat, profile(TRANSFER.toString()));
            unchanged(h, cat, token(h, type, 7), "Duplicate");
            CatTraitData.set(cat, profile(CONFLICT.toString()));
            unchanged(h, cat, token(h, type, 7), "Conflict");
            CatTraitData.set(cat, profile(TRANSFER.toString()));
            unchanged(h, cat, token(h, CatTraitRegistry.resolve(CONFLICT, false), 7), "Reverse conflict");
            CatTraitData.set(cat, profile(SLOT.toString()));
            unchanged(h, cat, token(h, type, 7), "Occupied exclusive slot");
            CatTraitData.set(cat, profile("testpack:missing_a", "testpack:missing_b", "testpack:missing_c", "testpack:missing_d"));
            var state = new CompoundTag(); state.putInt("retained", 93);
            CatTraitScriptState.write(cat, "testpack:missing_b", state);
            unchanged(h, cat, token(h, type, 7), "Four unknown occupied slots");
            h.succeed();
        } finally { reload(backup); cat.discard(); }
    }

    @GameTest(template = "artillery_probe", batch = "cat_editor_traits", timeoutTicks = 30)
    public static void missingDisabledAndWrongItemsNeverConsume(GameTestHelper h) {
        var backup = definitions(); reload(fixture(backup)); Cat cat = cat(h);
        try {
            CatTraitData.set(cat, profile(KEPT.toString()));
            unchanged(h, cat, token(h, CatTraitRegistry.resolve(id("testpack:editor_absent"), false), 7), "Missing definition");
            unchanged(h, cat, token(h, CatTraitRegistry.resolve(DISABLED, false), 7), "Disabled definition");
            unchanged(h, cat, new ItemStack(net.minecraft.world.item.Items.STONE), "Wrong item");
            unchanged(h, cat, ItemStack.EMPTY, "Empty input");
            ItemStack invalid = token(h, CatTraitRegistry.resolve(TRANSFER, false), 7);
            cn.laowu.mod.item.ItemCustomData.update(invalid, rootTag -> rootTag.getCompound("LaoWuCatTraitToken").putInt("Level", 0));
            unchanged(h, cat, invalid, "Malformed zero level");
            h.assertTrue(tokenLevel(h, invalid) == 0, "Malformed level is not normalized into a valid token");
            h.assertTrue(tokenId(h, new ItemStack(net.minecraft.world.item.Items.STONE)) == null
                    && tokenLevel(h, ItemStack.EMPTY) == 0, "Token readers reject non-token and empty stack");
            h.succeed();
        } finally { reload(backup); cat.discard(); }
    }

    @GameTest(template = "artillery_probe", batch = "cat_editor_traits", timeoutTicks = 30)
    public static void missingDefinitionCanBeExtractedAndRestored(GameTestHelper h) {
        var backup = definitions(); reload(fixture(backup)); Cat cat = cat(h);
        try {
            CatTraitData.set(cat, profile(TRANSFER.toString(), KEPT.toString()));
            reload(backup);
            ItemStack stack = extract(h, cat, TRANSFER);
            h.assertTrue(TRANSFER.equals(tokenId(h, stack)) && tokenLevel(h, stack) == 7,
                    "Definition removal does not destroy stored token identity or raw level");
            unchanged(h, cat, stack, "Removed definition");
            reload(fixture(backup));
            h.assertTrue(install(h, cat, stack) && raw(cat, TRANSFER) == 7 && raw(cat, KEPT) == 7,
                    "Restoring definition allows lossless installation without cleaning other missing entries");
            h.succeed();
        } finally { reload(backup); cat.discard(); }
    }
}
