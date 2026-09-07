package cn.laowu.mod.item;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTraitEffects;
import cn.laowu.mod.genetics.CatTraitProfile;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Create-native filter predicate for every saved field on item-form cat pancakes. */
public final class CatFilterItemStack extends FilterItemStack {
    private final CatFilterRules rules;

    public CatFilterItemStack(ItemStack filter) {
        super(filter);
        rules = CatFilterRules.read(filter);
    }

    @Override
    public boolean test(Level level, ItemStack candidate, boolean matchNBT) {
        if (!candidate.is(LaoWuMod.CAT_PANCAKE.get())) return false;
        return CatAttributeData.read(candidate)
                .map(attributes -> rules.matches(candidate, attributes,
                        CatTraitData.read(candidate).orElse(CatTraitProfile.EMPTY),
                        CatTraitEffects.isNight(level), CatTraitEffects.isDay(level)))
                .orElse(false);
    }
}
