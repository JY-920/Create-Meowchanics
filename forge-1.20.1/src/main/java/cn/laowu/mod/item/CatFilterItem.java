package cn.laowu.mod.item;

import cn.laowu.mod.CatFilterMenu;
import cn.laowu.mod.genetics.CatStat;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Configurable Create filter for cat attributes, traits and captured identity. */
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

        return List.of(CatFilterDescription.describe(rules).copy().withStyle(ChatFormatting.GRAY));
    }

    /** Create normally hides a configured filter's summary while Shift is held. */
    @Override
    public void appendHoverText(ItemStack stack, Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        List<Component> summary = makeSummary(stack);
        if (summary.isEmpty()) return;
        tooltip.add(CommonComponents.SPACE);
        tooltip.addAll(summary);
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
