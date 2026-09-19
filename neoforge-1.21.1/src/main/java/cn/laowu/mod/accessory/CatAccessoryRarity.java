package cn.laowu.mod.accessory;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import java.util.Set;

/** Three native name-color tiers; boss exclusivity is independent of an item's rarity. */
public final class CatAccessoryRarity {
    public static final TagKey<Item> BOSS_ONLY = TagKey.create(Registries.ITEM, LaoWuMod.id("boss_accessories"));
    private static final Set<String> COMMON = Set.of(
            "cat_taunt_bell", "cat_silent_bell", "cat_impact_core",
            "cat_health_badge", "cat_attack_badge", "cat_speed_badge",
            "cat_stamina_badge", "cat_intelligence_badge", "cat_luck_badge",
            "cat_old_food_bowl", "cat_sorting_pouch");
    private static final Set<String> EPIC = Set.of(
            "cat_fire_charm", "cat_followup_gear", "cat_ace_feather", "cat_blast_fuse",
            "cat_butter_cube", "cat_blue_flame_nozzle", "cat_concentrated_pouch",
            "cat_mixed_magazine", "cat_guard_bandage", "cat_medic_smoke_canister",
            "cat_rebirth_ootheca", "cat_roly_poly", "cat_chew_bone");

    public static Rarity forItem(String id) {
        String path = id.startsWith("laowu:") ? id.substring(6) : id;
        return COMMON.contains(path) ? Rarity.COMMON : EPIC.contains(path) ? Rarity.EPIC : Rarity.RARE;
    }
    public static int requirements(ItemStack reward) {
        return switch (reward.getRarity()) {
            case COMMON -> 2;
            case UNCOMMON, RARE -> 3;
            case EPIC -> 4;
        };
    }
    public static boolean bossOnly(ItemStack stack) {
        // A datapack cannot accidentally turn the existing trophy into an adoption reward.
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())
                .equals(LaoWuMod.id("cat_butter_cube")) || stack.is(BOSS_ONLY);
    }
    private CatAccessoryRarity() {}
}
