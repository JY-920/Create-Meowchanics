package cn.laowu.mod.genetics;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.Set;

/** An ID reference, not a cached definition: reloads affect existing cats without rewriting their genes. */
public record ScriptedCatTrait(ResourceLocation id, boolean clientSide) implements CatTraitType {
    private ScriptedCatTraitDefinition definition() { return CatTraitRegistry.definition(id, clientSide); }
    @Override public String serializedName() { return id.toString(); }
    @Override public boolean available() { return definition() != null; }
    @Override public boolean enabled() { var d = definition(); return d != null && d.enabled(); }
    @Override public boolean natural() { var d = definition(); return d != null && d.enabled() && d.natural(); }
    @Override public boolean mutation() { var d = definition(); return d != null && d.enabled() && d.mutation(); }
    @Override public boolean inheritable() { var d = definition(); return d != null && d.enabled() && d.inheritable(); }
    @Override public int generationWeight() { var d = definition(); return d == null ? 1 : d.weight(); }
    @Override public CatTraitRarity rarity() { var d = definition(); return d == null ? CatTraitRarity.COMMON : d.rarity(); }
    @Override public int maxLevel() { var d = definition(); return d == null ? 7 : d.maxLevel(); }
    @Override public boolean upgradable() { return available() && maxLevel() > 1; }
    @Override public Set<CatTraitSlot> occupiedSlots() { var d = definition(); return d == null ? Set.of() : d.slots(); }
    @Override public Set<ResourceLocation> conflicts() { var d = definition(); return d == null ? Set.of() : d.conflicts(); }
    @Override public int appearanceAttributeBonus(CatStat stat, int level) {
        var d = definition(); return d == null || !d.enabled() ? 0 : d.bonus(stat, clampLevel(level));
    }
    @Override public Component title() {
        var d = definition();
        return d == null ? Component.translatable("trait.laowu.script_missing.title", id.toString()) : Component.literal(d.title());
    }
    @Override public Component summary(int level) { return description(level); }
    @Override public Component description(int level) {
        var d = definition();
        return d == null ? Component.translatable("trait.laowu.script_missing.description")
                : Component.literal(d.description(clampLevel(level)));
    }
    @Override public Component nextLevelDescription(int level) { return description(clampLevel(level + 1)); }
}
