package cn.laowu.bootprobe;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Uses actual production Mixin-transformed JECharacters and registered game items. */
public final class CreativeSearchClientProbe {
    public static void run() throws ReflectiveOperationException {
        // The separate probe mod's AT recreates the reported compatibility condition.
        // Force target loading even if no player ever opens the creative screen.
        var scroll = CreativeModeInventoryScreen.class.getDeclaredField("f_98508_");
        check(Modifier.isPublic(scroll.getModifiers()), "Test AT must widen creative scroll field");
        System.out.println("PASS: widened creative scroll field loaded without search mixin crash");
        Class<?> type;
        try { type = Class.forName("me.towdium.jecharacters.utils.FakeArray"); }
        catch (ClassNotFoundException absent) {
            System.out.println("PASS: JECharacters absent - optional search compatibility is a no-op");
            return;
        }
        Object array = type.getConstructor().newInstance();
        Method add = type.getMethod("m_119970_", Object.class, String.class);
        Method search = type.getMethod("m_119973_", String.class);
        ItemStack pouch = stack("cat_pouch"), box = stack("cat_box"), food = stack("pheromone_cat_food");
        for (String line : List.of("猫袋", "已存放猫饼", "右键放入猫饼")) add.invoke(array, pouch, line);
        for (String line : List.of("猫盒", "已存放猫榴弹", "右键放入猫榴弹")) add.invoke(array, box, line);
        for (String line : List.of("信息素猫粮", "给猫吃", "猫咪变成宠物", "小猫立即长大")) add.invoke(array, food, line);
        List<?> results = (List<?>) search.invoke(array, "猫");
        check(results.size() == 3 && Collections.frequency(results, pouch) == 1
                && Collections.frequency(results, box) == 1 && Collections.frequency(results, food) == 1,
                "Mixin-applied real FakeArray must return one pouch, box and food");

        Object mixed = type.getConstructor().newInstance();
        ItemStack alice = pouch.copy(), bob = pouch.copy(), foreign = new ItemStack(Items.STONE);
        alice.getOrCreateTag().putString("owner", "Alice");
        bob.getOrCreateTag().putString("owner", "Bob");
        add.invoke(mixed, alice, "猫 Alice"); add.invoke(mixed, alice.copy(), "猫 Alice copy");
        add.invoke(mixed, bob, "猫 Bob");
        add.invoke(mixed, foreign, "猫 foreign one"); add.invoke(mixed, foreign, "猫 foreign two");
        Object recipe = new Object();
        add.invoke(mixed, recipe, "猫 recipe one"); add.invoke(mixed, recipe, "猫 recipe two");
        results = (List<?>) search.invoke(mixed, "猫");
        check(results.size() == 6 && Collections.frequency(results, alice) == 1
                && Collections.frequency(results, bob) == 1 && Collections.frequency(results, foreign) == 2
                && Collections.frequency(results, recipe) == 2,
                "Real search must preserve distinct NBT variants, foreign and generic duplicates");
        check(alice.getCount() == 1 && bob.getCount() == 1 && pouch.getCount() == 1,
                "Real search must not change original stacks");
        check(((List<?>) search.invoke(mixed, "没有匹配的字符串")).isEmpty(), "No-match search remains empty");
        System.out.println("PASS: FORGE PRODUCTION JECHARACTERS SEARCH - actual SRG FakeArray Mixin, registered items, variants and generic callers");
    }
    private static ItemStack stack(String id) {
        var item = BuiltInRegistries.ITEM.get(new ResourceLocation("laowu", id));
        check(item != Items.AIR, "Registered laowu item must exist: " + id);
        return new ItemStack(item);
    }
    private static void check(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }
    private CreativeSearchClientProbe() {}
}
