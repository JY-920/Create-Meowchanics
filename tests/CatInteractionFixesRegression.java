package cn.laowu.mod;

import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.client.CatCreativeSearchResults;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.Predicate;

/** Math and real NeoForge component equality; no running-world or UI simulation. */
public final class CatInteractionFixesRegression {
    private static int checks;
    public static void run() throws Exception {
        for (int level = 0; level <= 7; level++) {
            int total = 1 + (level == 0 ? 0 : CatTrait.LONG_FUR.longFurExtraDrops(level));
            check(total == 1 + (level + 1) / 2, "Shearing level " + level);
        }
        // Every height, direction and speed converges instead of hovering with zero input.
        for (int x : new int[]{-32, -8, 0, 8, 32}) for (int y : new int[]{-16, -4, 0, 4, 16})
            for (int z : new int[]{-32, 0, 32}) for (double speed : new double[]{0.1, 0.24, 0.36, 999999}) {
                Vec3 position = Vec3.ZERO, velocity = new Vec3(0.3, 0.2, -0.3);
                Vec3 target = new Vec3(x, y, z);
                for (int tick = 0; tick < 900; tick++) {
                    velocity = CatFlightMotion.step(position, target, velocity, speed);
                    check(Double.isFinite(velocity.length()) && velocity.length() <= .450001, "Bounded finite flight");
                    check(Math.abs(velocity.y) <= .280001, "Vertical cap");
                    position = position.add(velocity);
                }
                check(position.distanceToSqr(target) < .0025, "Reaches 3-D destination");
            }
        Vec3 coast = new Vec3(0.4, -0.2, 0.1);
        for (int i = 0; i < 80; i++) coast = CatFlightMotion.step(Vec3.ZERO, Vec3.ZERO, coast, 0);
        check(coast.length() < 1e-10, "No stale flight momentum on WAIT");
        if (Runtime.version().feature() >= 21) checkSearchVariants();
        System.out.println("PASS: " + checks + " command-flight, fur yield and search-variant checks");
    }

    private static void checkSearchVariants() throws Exception {
        var method = CatCreativeSearchResults.class.getDeclaredMethod("deduplicate", List.class, Predicate.class);
        method.setAccessible(true);
        ItemStack empty = new ItemStack(Items.PAPER);
        ItemStack ownerA = variant("owner", "Alice"), ownerB = variant("owner", "Bob");
        ItemStack red = variant("color", "red"), blue = variant("color", "blue");
        ItemStack contentsA = variant("cats", "cat-a"), contentsB = variant("cats", "cat-b");
        List<ItemStack> originals = List.of(empty, ownerA, ownerB, red, blue, contentsA, contentsB);
        List<ItemStack> hits = net.minecraft.core.NonNullList.create();
        for (ItemStack stack : originals) {
            hits.add(stack); hits.add(stack); hits.add(stack.copy());
        }
        ItemStack foreign = new ItemStack(Items.STONE);
        hits.add(foreign); hits.add(foreign.copy());
        boolean changed = (boolean) method.invoke(null, hits, (Predicate<ItemStack>) stack -> stack.is(Items.PAPER));
        check(changed && hits.size() == 9, "Exact duplicate results collapsed; foreign entries untouched");
        for (int i = 0; i < originals.size(); i++)
            check(hits.get(i) == originals.get(i), "Stable order and first-stack identity");
        check(!(boolean) method.invoke(null, hits, (Predicate<ItemStack>) stack -> stack.is(Items.PAPER)), "Idempotent search");
        check(originals.stream().allMatch(stack -> stack.getCount() == 1), "Source stacks never changed");
        List<ItemStack> unrelated = new ArrayList<>(List.of(foreign, foreign.copy()));
        check(!CatCreativeSearchResults.deduplicate(unrelated) && unrelated.size() == 2, "Public hook limited to laowu namespace");
    }
    private static ItemStack variant(String key, String value) throws Exception {
        ItemStack stack = new ItemStack(Items.PAPER);
        CompoundTag tag = new CompoundTag();
        tag.putString(key, value);
        Class<?> type = Class.forName("net.minecraft.core.component.DataComponentType");
        Object component = Class.forName("net.minecraft.core.component.DataComponents").getField("CUSTOM_DATA").get(null);
        Object data = Class.forName("net.minecraft.world.item.component.CustomData").getMethod("of", CompoundTag.class).invoke(null, tag);
        ItemStack.class.getMethod("set", type, Object.class).invoke(stack, component, data);
        return stack;
    }
    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }
}
