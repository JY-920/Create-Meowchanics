package cn.laowu.mod.create;

import cn.laowu.mod.accessory.CatAccessoryRegistry;
import cn.laowu.mod.accessory.CatAccessoryRarity;
import cn.laowu.mod.genetics.CatAttributeProfile;
import cn.laowu.mod.genetics.CatStat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Immutable, persisted server offer. -1 denotes an open bound, never a hidden 100 cap. */
public record WishAdoptionOffer(boolean maximum, List<Condition> conditions, String reward) {
    public record Condition(CatStat stat, int min, int max) {
        public Condition {
            Objects.requireNonNull(stat);
            if (min < -1 || max < -1 || min > 100 || max > 100
                    || min == -1 && max == -1 || min >= 0 && max >= 0 && min > max)
                throw new IllegalArgumentException("Invalid adoption bounds");
        }
        public boolean matches(int value) {
            return (min < 0 || value >= min) && (max < 0 || value <= max);
        }
    }
    public WishAdoptionOffer {
        conditions = List.copyOf(conditions);
        if (conditions.isEmpty() || conditions.size() > 6
                || conditions.stream().map(Condition::stat).distinct().count() != conditions.size()
                || ResourceLocation.tryParse(reward) == null || reward.equals("minecraft:air"))
            throw new IllegalArgumentException("Invalid adoption offer");
    }

    public boolean matches(CatAttributeProfile profile) {
        return conditions.stream().allMatch(c -> c.matches(
                maximum ? profile.potential(c.stat()) : profile.current(c.stat())));
    }

    public ItemStack rewardStack() {
        var id = ResourceLocation.tryParse(reward);
        return id != null && BuiltInRegistries.ITEM.containsKey(id)
                ? new ItemStack(BuiltInRegistries.ITEM.get(id)) : ItemStack.EMPTY;
    }

    public static WishAdoptionOffer roll(RandomSource random, WishAdoptionOffer previous) {
        List<String> rewards = new ArrayList<>(CatAccessoryRegistry.rewardItemIds());
        if (rewards.isEmpty()) return null;
        if (previous != null && rewards.size() > 1) rewards.remove(previous.reward);
        String reward = rewards.get(random.nextInt(rewards.size()));
        int count = CatAccessoryRarity.requirements(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(reward))));
        List<Condition> conditions = randomConditions(random, count);
        boolean maximum = random.nextBoolean();
        // A completed, unlocked offer visibly refreshes both its card and reward.
        if (previous != null && maximum == previous.maximum && conditions.equals(previous.conditions))
            maximum = !maximum;
        return new WishAdoptionOffer(maximum, conditions, reward);
    }

    private static List<Condition> randomConditions(RandomSource random, int count) {
        List<CatStat> stats = new ArrayList<>(List.of(CatStat.values()));
        for (int i = stats.size() - 1; i > 0; i--) Collections.swap(stats, i, random.nextInt(i + 1));
        List<Condition> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int kind = random.nextInt(3);
            int min = -1, max = -1;
            if (kind == 0) min = 10 * (1 + random.nextInt(9));
            else if (kind == 1) max = 10 * (1 + random.nextInt(9));
            else {
                min = 10 * (1 + random.nextInt(7));
                max = min + 10 * (2 + random.nextInt((100 - min) / 10 - 1));
            }
            result.add(new Condition(stats.get(i), min, max));
        }
        result.sort(Comparator.comparingInt(c -> c.stat.ordinal()));
        return result;
    }

    /** Upgrade old 1-6-rule cards while preserving valid rewards, existing rules and the box lock. */
    public WishAdoptionOffer normalized(RandomSource random) {
        ItemStack stack = rewardStack();
        if (stack.isEmpty() || CatAccessoryRegistry.find(stack, false) == null || CatAccessoryRarity.bossOnly(stack))
            return roll(random, this);
        int required = CatAccessoryRarity.requirements(stack);
        if (conditions.size() == required) return this;
        List<Condition> revised = new ArrayList<>(conditions.subList(0, Math.min(required, conditions.size())));
        for (Condition candidate : randomConditions(random, 6)) {
            if (revised.size() >= required) break;
            if (revised.stream().noneMatch(old -> old.stat() == candidate.stat())) revised.add(candidate);
        }
        revised.sort(Comparator.comparingInt(c -> c.stat().ordinal()));
        return new WishAdoptionOffer(maximum, revised, reward);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", 2);
        tag.putBoolean("Maximum", maximum);
        tag.putString("Reward", reward);
        ListTag rules = new ListTag();
        for (Condition condition : conditions) {
            CompoundTag rule = new CompoundTag();
            rule.putString("Stat", condition.stat.serializedName());
            rule.putInt("Min", condition.min);
            rule.putInt("Max", condition.max);
            rules.add(rule);
        }
        tag.put("Conditions", rules);
        return tag;
    }

    public static WishAdoptionOffer load(CompoundTag tag) {
        try {
            if (tag.getInt("Version") < 1 || tag.getInt("Version") > 2) return null;
            ListTag rules = tag.getList("Conditions", Tag.TAG_COMPOUND);
            if (rules.isEmpty() || rules.size() > 6) return null;
            List<Condition> conditions = new ArrayList<>();
            for (int i = 0; i < rules.size(); i++) {
                CompoundTag rule = rules.getCompound(i);
                if (!rule.contains("Min", Tag.TAG_INT) || !rule.contains("Max", Tag.TAG_INT)) return null;
                CatStat stat = Arrays.stream(CatStat.values()).filter(
                        s -> s.serializedName().equals(rule.getString("Stat"))).findFirst().orElseThrow();
                conditions.add(new Condition(stat, rule.getInt("Min"), rule.getInt("Max")));
            }
            return new WishAdoptionOffer(tag.getBoolean("Maximum"), conditions, tag.getString("Reward"));
        } catch (RuntimeException malformed) {
            return null;
        }
    }
}
