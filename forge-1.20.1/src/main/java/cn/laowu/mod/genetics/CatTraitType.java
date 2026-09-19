package cn.laowu.mod.genetics;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.Set;

/** Shared view of immutable built-ins and reloadable, ID-addressed script traits. */
public interface CatTraitType {
    ResourceLocation id();
    String serializedName();
    CatTraitRarity rarity();
    int maxLevel();
    Set<CatTraitSlot> occupiedSlots();
    Component title();
    Component summary(int level);
    Component description(int level);
    Component nextLevelDescription(int level);
    default boolean upgradable() { return maxLevel() > 1; }
    default int clampLevel(int level) { return Math.max(1, Math.min(maxLevel(), level)); }
    default boolean available() { return true; }
    default boolean enabled() { return available(); }
    default boolean natural() { return enabled(); }
    default boolean mutation() { return enabled(); }
    default boolean inheritable() { return this != CatTrait.DOUGHY && available(); }
    default int generationWeight() { return 1; }
    default Set<ResourceLocation> conflicts() { return Set.of(); }
    default CatStat attributeStat() { return null; }
    default int attributeBonus(int level) { return 0; }
    default int appearanceAttributeBonus(CatStat stat, int level) { return 0; }
}
