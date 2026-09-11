package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import java.util.List;
import java.util.Set;

/** Search adapters can return the same stack once per matching tooltip line. */
public final class CatCreativeSearchResults {
    public static boolean deduplicate(List<ItemStack> results) {
        return deduplicate(results, stack -> LaoWuMod.MOD_ID.equals(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace()));
    }
    static boolean deduplicate(List<ItemStack> results, java.util.function.Predicate<ItemStack> eligible) {
        Set<ItemStack> seen = ItemStackLinkedSet.createTypeAndTagSet();
        return results.removeIf(stack -> eligible.test(stack) && !seen.add(stack));
    }
    private CatCreativeSearchResults() {}
}
