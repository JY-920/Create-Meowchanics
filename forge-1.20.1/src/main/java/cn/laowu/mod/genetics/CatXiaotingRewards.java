package cn.laowu.mod.genetics;

import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Shared Xiaoting reward roll for death loot and vanilla morning gifts. */
public final class CatXiaotingRewards {
    private static final float TEMPLATE_CHANCE = 0.20F;

    public static boolean tryDropTemplate(Cat cat) {
        if (cat.level().isClientSide
                || !CatTraitData.ensure(cat).has(CatTrait.XIAOTING)
                || cat.getRandom().nextFloat() >= TEMPLATE_CHANCE) {
            return false;
        }

        List<Item> templates = ForgeRegistries.ITEMS.getValues().stream()
                .filter(CatXiaotingRewards::isSmithingTemplate)
                .toList();
        if (templates.isEmpty()) return false;

        Item selected = templates.get(cat.getRandom().nextInt(templates.size()));
        cat.spawnAtLocation(new ItemStack(selected));
        return true;
    }

    private static boolean isSmithingTemplate(Item item) {
        if (item instanceof SmithingTemplateItem) return true;
        // Also admit conventionally registered modded templates whose runtime
        // item class wraps rather than extends the vanilla implementation.
        var id = ForgeRegistries.ITEMS.getKey(item);
        return id != null && id.getPath().endsWith("_smithing_template");
    }

    private CatXiaotingRewards() {}
}
