package cn.laowu.mod;

import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatAttributeProfile;
import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.genetics.CatTraitRarity;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTraitInstance;
import cn.laowu.mod.genetics.CatTraitProfile;
import cn.laowu.mod.genetics.CatTraitRegistry;
import cn.laowu.mod.genetics.CatTraitType;
import cn.laowu.mod.item.CatTraitTokenItem;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;

/** Lossless server-side transfers. The menu owns target authorization and output capacity. */
public final class CatEditorTraits {
    public static ItemStack extract(Cat cat, ResourceLocation id) {
        if (!editable(cat) || id == null) return ItemStack.EMPTY;
        CatTraitProfile profile = CatTraitData.read(cat).orElse(CatTraitProfile.EMPTY);
        CatTraitInstance entry = profile.traits().stream()
                .filter(saved -> saved.trait().id().equals(id)).findFirst().orElse(null);
        if (entry == null) return ItemStack.EMPTY;
        ItemStack token = CatTraitTokenItem.create(entry.trait(), entry.level());
        if (token.isEmpty()) return ItemStack.EMPTY;
        // Removal does not need a live definition and retains every other saved entry.
        CatTraitData.set(cat, profile.withLevel(entry.trait(), 0));
        ModNetwork.syncCatTraitsToTracking(cat);
        return token;
    }

    public static boolean install(Cat cat, ItemStack token) {
        if (!editable(cat)) return false;
        CatTraitProfile installed = validatedInstall(cat, token);
        if (installed == null) return false;
        CatTraitType trait = CatTraitRegistry.resolve(CatTraitTokenItem.traitId(token), false);
        CatTraitData.set(cat, installed);
        token.shrink(1);
        if (trait.rarity() == CatTraitRarity.GOOD || trait.rarity() == CatTraitRarity.EXCELLENT) {
            CatAttributeProfile before = CatAttributeData.ensure(cat);
            CatAttributeProfile after = applyInstallRisk(before, trait.rarity(), cat.getRandom());
            CatAttributeData.set(cat, after);
            ModNetwork.syncCatAttributesToTracking(cat);
        }
        ModNetwork.syncCatTraitsToTracking(cat);
        return true;
    }

    /** Read-only availability for either logical side; install still authorizes on the server. */
    public static boolean canInstall(Cat cat, ItemStack token) {
        return cat != null && cat.isAlive() && !cat.isRemoved() && validatedInstall(cat, token) != null;
    }

    private static CatTraitProfile validatedInstall(Cat cat, ItemStack token) {
        ResourceLocation id = CatTraitTokenItem.traitId(token);
        int level = CatTraitTokenItem.level(token);
        if (id == null || level <= 0) return null;
        CatTraitType trait = CatTraitRegistry.resolve(id, cat.level().isClientSide);
        if (trait == null || !trait.available() || !trait.enabled() || ServerConfig.isTraitDisabled(trait)
                || trait instanceof CatTrait && level > trait.maxLevel()) return null;
        CatTraitProfile profile = CatTraitData.read(cat).orElse(CatTraitProfile.EMPTY);
        if (profile.traits().size() >= CatTraitProfile.MAX_TRAITS) return null;
        for (CatTraitInstance existing : profile.traits()) {
            CatTraitType other = existing.trait();
            if (other.id().equals(id) || other.conflicts().contains(id)
                    || trait.conflicts().contains(other.id())
                    || !Collections.disjoint(other.occupiedSlots(), trait.occupiedSlots())) return null;
        }

        // withLevel() is intentionally an effective-level editor. Loading a validated
        // appended saved entry preserves custom raw ownership through a lowered max.
        CompoundTag saved = profile.save();
        var entries = saved.getList("Traits", Tag.TAG_COMPOUND);
        CompoundTag entry = new CompoundTag();
        entry.putString("Id", id.toString());
        entry.putInt("Level", level);
        entries.add(entry);
        CatTraitProfile installed = CatTraitProfile.load(saved, cat.level().isClientSide).orElse(null);
        if (installed == null || installed.traits().size() != profile.traits().size() + 1
                || installed.rawLevel(trait) != level) return null;
        return installed;
    }

    /** One mutually exclusive bucket, with an independent uniform locus for each loss. */
    public static CatAttributeProfile applyInstallRisk(CatAttributeProfile profile,
                                                       CatTraitRarity rarity, RandomSource random) {
        if (rarity != CatTraitRarity.EXCELLENT && rarity != CatTraitRarity.GOOD) return profile;
        int roll = random.nextInt(100);
        if (rarity == CatTraitRarity.GOOD) {
            return roll < 50 ? loseCurrent(profile, random, 10, 50) : profile;
        }
        if (roll < 50) return loseCurrent(profile, random, 10, 50);
        if (roll < 60) return loseMaximum(profile, random);
        return profile;
    }

    private static CatAttributeProfile loseCurrent(CatAttributeProfile profile, RandomSource random,
                                                    int minimum, int maximum) {
        CatStat stat = CatStat.values()[random.nextInt(CatStat.values().length)];
        int loss = minimum + random.nextInt(maximum - minimum + 1);
        return profile.withValues(stat, profile.current(stat) - loss, profile.potential(stat));
    }

    private static CatAttributeProfile loseMaximum(CatAttributeProfile profile, RandomSource random) {
        CatStat stat = CatStat.values()[random.nextInt(CatStat.values().length)];
        int loss = 10 + random.nextInt(11);
        return profile.withValues(stat, profile.current(stat), profile.potential(stat) - loss);
    }

    private static boolean editable(Cat cat) {
        return cat != null && !cat.level().isClientSide && cat.isAlive() && !cat.isRemoved();
    }

    private CatEditorTraits() {}
}
