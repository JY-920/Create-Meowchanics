import cn.laowu.mod.client.CatCreativeSearchResults;
import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Real vanilla ItemStacks and installed JECharacters, without starting FML or a world.
 * Calls the production RETURN handler directly; the separate client probe verifies Mixin application.
 */
public final class CreativeSearchCompatibilityRegression {
    private static int checks;
    private static Method normalize;
    public static void main(String[] args) throws Exception {
        try { normalize = CatCreativeSearchResults.class.getMethod("normalize", List.class); }
        catch (NoSuchMethodException missing) { throw new AssertionError("JECharacters result normalization is missing", missing); }
        java.io.PrintStream out = System.out, err = System.err;
        Item cat;
        try {
            net.minecraft.SharedConstants.tryDetectVersion();
            net.minecraft.server.Bootstrap.bootStrap();
            // Vanilla freezes registries during bootstrap. Only this disposable JVM is
            // reopened to register a real laowu fixture; the game probe uses actual items.
            var frozen = net.minecraft.core.MappedRegistry.class.getDeclaredField("frozen");
            var intrusive = net.minecraft.core.MappedRegistry.class.getDeclaredField("unregisteredIntrusiveHolders");
            frozen.setAccessible(true); intrusive.setAccessible(true);
            frozen.set(BuiltInRegistries.ITEM, false);
            intrusive.set(BuiltInRegistries.ITEM, new IdentityHashMap<>());
            cat = Registry.register(BuiltInRegistries.ITEM,
                    ResourceLocation.tryParse("laowu:search_regression"), new Item(new Item.Properties()));
            BuiltInRegistries.ITEM.freeze();
        } finally { System.setOut(out); System.setErr(err); }
        ItemStack first = new ItemStack(cat), foreign = new ItemStack(Items.STONE);
        ItemStack alice = variant(cat, "owner", "Alice"), bob = variant(cat, "owner", "Bob");
        ItemStack red = variant(cat, "color", "red"), blue = variant(cat, "color", "blue");
        ItemStack contentsA = variant(cat, "cats", "cat-a"), contentsB = variant(cat, "cats", "cat-b");
        List<Object> source = List.of(first, first, first.copy(), alice, alice.copy(), bob,
                foreign, foreign.copy(), "recipe", "recipe", red, blue, contentsA, contentsB);
        List<?> result = normalize(source);
        check(result.size() == 11, "Only exact laowu stack duplicates removed from immutable result");
        check(result.get(0) == first && result.get(1) == alice && result.get(2) == bob,
                "First stack identity and encounter order retained");
        check(result.get(3) == foreign && result.get(4) == source.get(7), "Foreign duplicates retained");
        check(result.subList(5, 7).equals(List.of("recipe", "recipe")), "Generic callers retained");
        check(result.subList(7, 11).equals(List.of(red, blue, contentsA, contentsB)), "Color and content variants retained");
        check(source.size() == 14 && first.getCount() == 1, "Input list and stacks unmodified");
        check(normalize(result) == result, "Already unique results preserve list identity");
        List<?> foreignOnly = List.of(foreign, foreign.copy());
        check(normalize(foreignOnly) == foreignOnly, "Foreign-only result is a no-op");
        List<?> generic = List.of("recipe", "recipe");
        check(normalize(generic) == generic, "Non-ItemStack search keeps duplicates and list identity");
        check(normalize(Collections.emptyList()).isEmpty(), "Empty result supported");
        check(normalize(null) == null, "Null result passes through");

        Class<?> arrayType = Class.forName("me.towdium.jecharacters.utils.FakeArray");
        Object array = arrayType.getConstructor().newInstance();
        String addName = args[0].equals("forge") ? "m_119970_" : "add";
        String searchName = args[0].equals("forge") ? "m_119973_" : "search";
        Method add = arrayType.getMethod(addName, Object.class, String.class);
        for (String line : List.of("猫袋", "已存放猫饼", "右键放入猫饼")) add.invoke(array, first, line);
        add.invoke(array, alice, "猫袋 Alice");
        add.invoke(array, bob, "猫袋 Bob");
        add.invoke(array, foreign, "猫 foreign one");
        add.invoke(array, foreign, "猫 foreign two");
        List<?> raw = (List<?>) arrayType.getMethod(searchName, String.class).invoke(array, "猫");
        check(Collections.frequency(raw, first) == 3, "Installed JECharacters duplicates matching tooltip lines");
        Class<?> mixin = Class.forName("cn.laowu.mod.mixin.JECharactersSearchMixin");
        Method handler = mixin.getDeclaredMethod("laowu$uniqueCatItems", String.class, CallbackInfoReturnable.class);
        handler.setAccessible(true);
        CallbackInfoReturnable<List<?>> callback = new CallbackInfoReturnable<>(searchName, true, raw);
        handler.invoke(mixin.getConstructor().newInstance(), "猫", callback);
        List<?> fixed = callback.getReturnValue();
        check(Collections.frequency(fixed, first) == 1 && fixed.size() == 5,
                "Production RETURN handler normalizes installed adapter output");
        check(Collections.frequency(raw, first) == 3, "Adapter-owned source list unmodified");
        System.out.println("PASS: " + checks + " " + args[0] + " search compatibility checks");
    }
    private static List<?> normalize(List<?> input) throws Exception { return (List<?>) normalize.invoke(null, input); }
    private static ItemStack variant(Item item, String key, String value) throws Exception {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = new CompoundTag(); tag.putString(key, value);
        if (Runtime.version().feature() < 21) ItemStack.class.getMethod("setTag", CompoundTag.class).invoke(stack, tag);
        else {
            Class<?> type = Class.forName("net.minecraft.core.component.DataComponentType");
            Object component = Class.forName("net.minecraft.core.component.DataComponents").getField("CUSTOM_DATA").get(null);
            Object data = Class.forName("net.minecraft.world.item.component.CustomData").getMethod("of", CompoundTag.class).invoke(null, tag);
            ItemStack.class.getMethod("set", type, Object.class).invoke(stack, component, data);
        }
        return stack;
    }
    private static void check(boolean condition, String reason) {
        checks++; if (!condition) throw new AssertionError(reason);
    }
}
