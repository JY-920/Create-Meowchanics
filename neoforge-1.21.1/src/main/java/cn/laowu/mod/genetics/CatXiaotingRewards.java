package cn.laowu.mod.genetics;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;

import java.util.Collection;
import java.util.List;

/** Xiaoting's death-loot and vanilla morning-gift rewards. */
public final class CatXiaotingRewards {
    private static final float MORNING_TEMPLATE_CHANCE = 0.20F;

    /**
     * Append the guaranteed death reward to the authoritative drop list.
     * This keeps the reward in the same lifecycle as normal mob loot instead of
     * spawning a second, easy-to-lose item entity from LivingDeathEvent.
     */
    public static boolean addDeathTemplate(Cat cat, Collection<ItemEntity> drops) {
        if (cat.level().isClientSide
                || !CatTraitData.ensure(cat).has(CatTrait.XIAOTING)) {
            return false;
        }

        ItemStack reward = randomTemplate(cat);
        if (reward.isEmpty()) return false;

        ItemEntity drop = new ItemEntity(cat.level(), cat.getX(), cat.getY(), cat.getZ(), reward);
        drop.setDefaultPickUpDelay();
        drops.add(drop);
        return true;
    }

    /** The extra template attached to a vanilla morning gift remains a chance. */
    public static boolean tryGiveMorningTemplate(Cat cat) {
        if (cat.level().isClientSide
                || !CatTraitData.ensure(cat).has(CatTrait.XIAOTING)
                || cat.getRandom().nextFloat() >= MORNING_TEMPLATE_CHANCE) {
            return false;
        }

        ItemStack reward = randomTemplate(cat);
        if (reward.isEmpty()) return false;
        cat.spawnAtLocation(reward);
        return true;
    }

    private static ItemStack randomTemplate(Cat cat) {
        List<Item> templates = BuiltInRegistries.ITEM.stream()
                .filter(CatXiaotingRewards::isSmithingTemplate)
                .toList();
        if (templates.isEmpty()) return ItemStack.EMPTY;

        Item selected = templates.get(cat.getRandom().nextInt(templates.size()));
        return new ItemStack(selected);
    }

    private static boolean isSmithingTemplate(Item item) {
        if (item instanceof SmithingTemplateItem) return true;
        // Also admit conventionally registered modded templates whose runtime
        // item class wraps rather than extends the vanilla implementation.
        var id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && id.getPath().endsWith("_smithing_template");
    }

    private CatXiaotingRewards() {}
}
