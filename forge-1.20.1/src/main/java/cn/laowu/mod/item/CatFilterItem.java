package cn.laowu.mod.item;

import cn.laowu.mod.CatFilterMenu;
import cn.laowu.mod.genetics.CatStat;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Configurable Create filter for the six saved cat attributes. */
public final class CatFilterItem extends FilterItem {
    public CatFilterItem(Properties properties) {
        super(properties);
    }

    @Override
    public List<Component> makeSummary(ItemStack stack) {
        CatFilterRules rules = CatFilterRules.read(stack);
        if (rules.isDefault()) {
            return List.of(Component.translatable("item.laowu.cat_filter.summary.default")
                    .withStyle(ChatFormatting.GRAY));
        }

        List<Component> summary = new ArrayList<>();
        for (int page = 0; page <= 1; page++) {
            for (CatStat stat : CatStat.values()) {
                int minimum = rules.min(page, stat);
                int maximum = rules.max(page, stat);
                if (minimum == CatFilterRules.MIN_VALUE
                        && maximum == CatFilterRules.MAX_VALUE) continue;
                summary.add(Component.translatable("item.laowu.cat_filter.summary.range",
                                Component.translatable("attribute.laowu.cat."
                                        + stat.serializedName()),
                                Component.translatable(page == CatFilterRules.CURRENT_PAGE
                                        ? "gui.laowu.cat_stats.current"
                                        : "gui.laowu.cat_stats.limit"),
                                minimum, maximum)
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        rules.requiredTraits().forEach(trait -> summary.add(
                Component.translatable("item.laowu.cat_filter.summary.trait",
                                trait.title())
                        .withStyle(ChatFormatting.GRAY)));
        return summary;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory,
                                            Player player) {
        return new CatFilterMenu(containerId, inventory, player.getMainHandItem());
    }

    @Override
    public FilterItemStack makeStackWrapper(ItemStack stack) {
        return new CatFilterItemStack(stack);
    }

    @Override
    public ItemStack[] getFilterItems(ItemStack stack) {
        return new ItemStack[]{CatPancakeItem.defaultDisplayStack()};
    }
}
