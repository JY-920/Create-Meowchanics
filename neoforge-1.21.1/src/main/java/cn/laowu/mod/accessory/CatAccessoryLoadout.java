package cn.laowu.mod.accessory;

import java.util.*;

/** Pure resolution rules, shared by real cats, item previews and regression tests. */
public record CatAccessoryLoadout(Map<String, Double> effects, Set<String> ids) {
    public CatAccessoryLoadout {
        effects = Map.copyOf(effects);
        ids = Set.copyOf(ids);
    }
    public static CatAccessoryLoadout resolve(List<CatAccessoryDefinition> equipment, String outfit) {
        Map<String, Double> values = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>(), groups = new HashSet<>(), ids = new LinkedHashSet<>();
        for (int slot = 0; slot < Math.min(4, equipment.size()); slot++) {
            CatAccessoryDefinition def = equipment.get(slot);
            if (def == null || !def.enabled() || !seen.add(def.id())) continue;
            if (!def.exclusiveGroup().isEmpty() && !groups.add(def.exclusiveGroup())) continue;
            ids.add(def.id());
            ids.add(def.item());
            if (!def.activeFor(outfit)) continue;
            def.effects().forEach((key, value) -> {
                if (CatAccessoryDefinition.STATS.contains(key) || key.equals("aggro_bias"))
                    values.merge(key, value, Double::sum);
                else values.merge(key, value, Math::max);
            });
        }
        return new CatAccessoryLoadout(values, ids);
    }
}
