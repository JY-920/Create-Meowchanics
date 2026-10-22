package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Search adapters can return the same stack once per matching tooltip line. */
public final class CatCreativeSearchResults {
    /** Copy only when needed: the optional adapter also serves non-ItemStack searches. */
    public static <T> List<T> normalize(List<T> results) {
        if (results == null || results.isEmpty()) return results;
        Set<ItemStack> seen = ItemStackLinkedSet.createTypeAndComponentsSet();
        List<T> normalized = null;
        for (int i = 0; i < results.size(); i++) {
            T result = results.get(i);
            if (result instanceof ItemStack stack && isCatItem(stack) && !seen.add(stack)) {
                if (normalized == null) normalized = new ArrayList<>(results.subList(0, i));
            } else if (normalized != null) normalized.add(result);
        }
        return normalized == null ? results : normalized;
    }

    public static boolean deduplicate(List<ItemStack> results) {
        return deduplicate(results, CatCreativeSearchResults::isCatItem);
    }
    static boolean deduplicate(List<ItemStack> results, java.util.function.Predicate<ItemStack> eligible) {
        Set<ItemStack> seen = ItemStackLinkedSet.createTypeAndComponentsSet();
        return results.removeIf(stack -> eligible.test(stack) && !seen.add(stack));
    }
    private static boolean isCatItem(ItemStack stack) {
        return LaoWuMod.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
    }
    private CatCreativeSearchResults() {}
}
