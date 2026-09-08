package cn.laowu.mod.genetics;

import java.util.Objects;

/** One saved trait and its current level. */
public record CatTraitInstance(CatTrait trait, int level) {
    public CatTraitInstance {
        trait = Objects.requireNonNull(trait, "trait");
        level = trait.clampLevel(level);
    }
}
