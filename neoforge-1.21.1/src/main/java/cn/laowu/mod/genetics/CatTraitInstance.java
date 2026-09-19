package cn.laowu.mod.genetics;

import java.util.Objects;

/** One saved trait and its current level. */
public record CatTraitInstance(CatTraitType trait, int level) {
    public CatTraitInstance {
        trait = Objects.requireNonNull(trait, "trait");
        // Preserve custom levels through missing definitions or a temporary lower max level.
        level = trait instanceof CatTrait ? trait.clampLevel(level)
                : Math.max(1, Math.min(CatTrait.MAX_UPGRADABLE_LEVEL, level));
    }
}
