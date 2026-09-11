package cn.laowu.mod.item;

import cn.laowu.mod.genetics.CatStat;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.ArrayList;
import java.util.List;

/** One explanation builder for the editor and held filter tooltip. */
public final class CatFilterDescription {
    public static Component describe(CatFilterRules rules) {
        List<Component> identity = new ArrayList<>();
        if (rules.growth() != CatFilterRules.GrowthFilter.ANY)
            identity.add(Component.translatable("gui.laowu.cat_filter.growth." + rules.growth().id()));
        if (rules.ownership() != CatFilterRules.OwnershipFilter.ANY)
            identity.add(Component.translatable("gui.laowu.cat_filter.ownership." + rules.ownership().id()));
        if (rules.career() != CatFilterRules.CareerFilter.ANY)
            identity.add(Component.translatable("gui.laowu.cat_filter.career." + rules.career().id()));
        if (!rules.catName().isEmpty())
            identity.add(Component.translatable("item.laowu.cat_filter.summary.name", rules.catName()));

        List<Component> attributes = new ArrayList<>();
        for (int page = 0; page < 2; page++) for (CatStat stat : CatStat.values()) {
            if (!rules.enabled(page, stat)) continue;
            attributes.add(Component.translatable("item.laowu.cat_filter.summary.range",
                    Component.translatable("attribute.laowu.cat." + stat.serializedName()),
                    Component.translatable(page == 0 ? "gui.laowu.cat_stats.current" : "gui.laowu.cat_stats.limit"),
                    rules.min(page, stat), rules.max(page, stat)));
        }
        List<Component> traits = new ArrayList<>();
        rules.requiredTraits().forEach(trait -> traits.add(trait.title()));
        List<Component> groups = new ArrayList<>();
        if (!attributes.isEmpty()) groups.add(group("attributes", attributes,
                rules.logic().option(CatFilterLogic.ATTRIBUTE_ANY), rules.logic().option(CatFilterLogic.ATTRIBUTE_INVERT)));
        if (!traits.isEmpty()) groups.add(group("traits", traits,
                rules.logic().option(CatFilterLogic.TRAIT_ANY), rules.logic().option(CatFilterLogic.TRAIT_INVERT)));

        Component expression = groups.isEmpty()
                ? Component.translatable("gui.laowu.cat_filter.logic.unrestricted")
                : join(groups, rules.logic().option(CatFilterLogic.GROUP_ANY) ? "or" : "and");
        if (!identity.isEmpty()) expression = Component.translatable(
                "gui.laowu.cat_filter.logic.identity_and", join(identity, "and"), expression);
        return Component.translatable("gui.laowu.cat_filter.logic.result", expression);
    }

    private static Component group(String name, List<Component> conditions, boolean any, boolean inverse) {
        Component value = Component.translatable("gui.laowu.cat_filter.logic.group",
                Component.translatable("gui.laowu.cat_filter.logic." + name),
                Component.translatable("gui.laowu.cat_filter.logic." + (any ? "any" : "all")),
                join(conditions, "list"));
        return inverse ? Component.translatable("gui.laowu.cat_filter.logic.not", value) : value;
    }

    private static Component join(List<Component> values, String separator) {
        MutableComponent result = Component.empty();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) result.append(Component.translatable("gui.laowu.cat_filter.logic." + separator));
            result.append(values.get(i));
        }
        return result;
    }
    private CatFilterDescription() {}
}
