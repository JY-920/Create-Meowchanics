package cn.laowu.mod.item;

import cn.laowu.mod.CreatureFilterMenu;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;

public final class CreatureFilterItem extends FilterItem {
    public CreatureFilterItem(Properties properties) { super(properties.stacksTo(1)); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Create's held-filter menu locks the selected main-hand stack.
        return hand == InteractionHand.MAIN_HAND ? super.use(level, player, hand)
                : InteractionResultHolder.pass(player.getItemInHand(hand));
    }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CreatureFilterMenu(id, inventory, player.getMainHandItem());
    }
    @Override public List<Component> makeSummary(ItemStack stack) {
        var rules = CreatureFilterRules.read(stack);

        if(rules.isGrouped()) {
            var result=new ArrayList<Component>();
            result.add(Component.translatable("gui.laowu.creature_filter.groups_count",rules.groups().size(),rules.conditionCount()));
            result.add(Component.translatable("gui.laowu.creature_filter.mode."+rules.mode().id()));
            for(int i=0;i<rules.groups().size();i++) {
                var g=rules.groups().get(i);
                Component target=g.category()!=null?Component.translatable("gui.laowu.creature_filter.category_all",
                    Component.translatable("gui.laowu.creature_filter.category."+g.category().id())):
                    g.target()==null?Component.translatable("gui.laowu.creature_filter.all_mobs"):
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(g.target())
                        .map(type->type.getDescription()).orElse(Component.literal(g.target().toString()));
                if(g.inverted())target=Component.translatable("gui.laowu.creature_filter.inverted",target);
                result.add(Component.translatable("gui.laowu.creature_filter.group",i+1,target)
                    .append(" · ").append(Component.translatable("gui.laowu.creature_filter.mode."+g.mode().id())));
                if(g.conditions().isEmpty())result.add(Component.translatable("gui.laowu.creature_filter.target_only"));
                for(var c:g.conditions()) {
                    var text=Component.translatable("gui.laowu.creature_filter.attribute."+c.attribute().id());
                    if(c.attribute()==CreatureFilterRules.Attribute.HEALTH||c.attribute()==CreatureFilterRules.Attribute.HEALTH_PERCENT)
                        text.append(" "+switch(c.comparison()){case EQ->"=";case LT->"<";case LE->"≤";case GT->">";case GE->"≥";}
                            +" "+c.value()+(c.attribute()==CreatureFilterRules.Attribute.HEALTH_PERCENT?"%":""));
                    result.add(c.inverted()?Component.translatable("gui.laowu.creature_filter.inverted",text):text);
                }
            }
            if(rules.retainedLegacy()!=null) {
                result.add(Component.translatable("gui.laowu.creature_filter.legacy_retained"));
                var copy=stack.copy();rules.retainedLegacy().write(copy);result.addAll(makeSummary(copy));
            }
            return result;
        }

        var categories = Component.empty();
        for (var category : CreatureFilterRules.Category.values()) if (rules.enabled(category)) {
            if (!categories.getString().isEmpty()) categories.append(Component.literal(" / "));
            categories.append(Component.translatable("gui.laowu.creature_filter.category." + category.id()));
        }
        if (categories.getString().isEmpty()) categories.append(Component.translatable("gui.laowu.creature_filter.none"));
        var result = new ArrayList<Component>();
        if (rules.categoryMask() != CreatureFilterRules.ALL_CATEGORIES)
            result.add(Component.translatable("tooltip.laowu.creature_filter.categories", categories));
        result.add(Component.translatable("tooltip.laowu.creature_filter.list",
                Component.translatable("gui.laowu.creature_filter.mode." + rules.mode().id()), rules.conditions().size()));
        if (rules.conditions().isEmpty() && !rules.isLegacy())
            result.add(Component.translatable("gui.laowu.creature_filter.none"));
        for (int i = 0; i < Math.min(6, rules.conditions().size()); i++) {
            var condition = rules.conditions().get(i);
            Component name = condition.category() != null
                    ? Component.translatable("gui.laowu.creature_filter.category_all",
                        Component.translatable("gui.laowu.creature_filter.category." + condition.category().id()))
                    : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(condition.entityId())
                        .map(type -> type.getDescription()).orElse(Component.literal(condition.entityId().toString()));
            result.add(condition.inverted() ? Component.translatable("gui.laowu.creature_filter.inverted", name) : name);
        }
        if (rules.conditions().size() > 6) result.add(Component.translatable("tooltip.laowu.creature_filter.more", rules.conditions().size() - 6));
        return result;
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        for (var line : makeSummary(stack)) tooltip.add(line.copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.laowu.creature_filter.description").withStyle(ChatFormatting.DARK_GRAY));
    }
    @Override public FilterItemStack makeStackWrapper(ItemStack stack) { return new CreatureOnlyWrapper(stack); }
    @Override public ItemStack[] getFilterItems(ItemStack stack) { return new ItemStack[0]; }

    /** This entity filter must not silently become a permissive item/fluid filter. */
    private static final class CreatureOnlyWrapper extends FilterItemStack {
        private CreatureOnlyWrapper(ItemStack stack) { super(stack); }
        @Override public boolean test(Level level, ItemStack stack) { return false; }
        @Override public boolean test(Level level, ItemStack stack, boolean matchNBT) { return false; }
        @Override public boolean test(Level level, FluidStack stack) { return false; }
        @Override public boolean test(Level level, FluidStack stack, boolean matchNBT) { return false; }
    }
}
