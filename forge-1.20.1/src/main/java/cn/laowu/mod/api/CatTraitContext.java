package cn.laowu.mod.api;

import cn.laowu.mod.genetics.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import java.util.List;

/** Synchronous event context. Breed is profile-only: getCat() is null, use the child/parent methods. */
public final class CatTraitContext {
    private final String type;
    private final Cat cat;
    private final LivingEntity other;
    private final DamageSource source;
    private final boolean mutable;
    private double amount;
    private boolean canceled, closed;
    private String traitId = "", reason = "";
    private int oldLevel, newLevel;
    private CatTraitProfile first, second, child;
    public CatTraitContext(String type, Cat cat, LivingEntity other, DamageSource source, double amount, boolean mutable) {
        this.type = type; this.cat = cat; this.other = other; this.source = source; this.amount = amount; this.mutable = mutable;
    }
    public static CatTraitContext breeding(CatTraitProfile first, CatTraitProfile second, CatTraitProfile child) {
        var context = new CatTraitContext("breed", null, null, null, 0, false);
        context.first = first; context.second = second; context.child = child; return context;
    }
    public CatTraitContext changed(String id, String reason, int oldLevel, int newLevel) {
        this.traitId = id; this.reason = reason; this.oldLevel = oldLevel; this.newLevel = newLevel; return this;
    }
    public String getType() { return type; }
    public Cat getCat() { return cat; }
    public LivingEntity getOther() { return other; }
    public LivingEntity getTarget() { return other; }
    public DamageSource getSource() { return source; }
    public double getAmount() { return amount; }
    public double getDamage() { return amount; }
    public boolean isMutable() { return mutable; }
    public boolean isCanceled() { return canceled; }
    public String getTraitId() { return traitId; }
    public String getReason() { return reason; }
    public int getOldLevel() { return oldLevel; }
    public int getNewLevel() { return newLevel; }
    public CatTraitHandle trait(String id) { active(); return cat == null ? null : CatTraitApi.trait(cat, id); }
    public void setAmount(double value) {
        writable();
        if (!Double.isFinite(value) || value < 0 || value > 1_000_000) throw new IllegalArgumentException("Amount must be finite and 0..1000000");
        amount = value;
    }
    public void setDamage(double value) { setAmount(value); }
    public void cancel() { writable(); canceled = true; }
    public List<String> childTraits() { breed(); return child.traits().stream().map(t -> t.trait().id().toString()).toList(); }
    public int childLevel(String id) { breed(); return child.rawLevel(resolve(id)); }
    public int parentLevel(int parent, String id) {
        breed();
        if (parent != 0 && parent != 1) throw new IllegalArgumentException("Parent index must be 0 or 1");
        return (parent == 0 ? first : second).rawLevel(resolve(id));
    }
    public boolean setChildLevel(String id, int level) {
        breed();
        if (level < 0 || level > 7) throw new IllegalArgumentException("Trait level must be 0..7");
        var trait = resolve(id); child = child.withLevel(trait, level);
        return child.rawLevel(trait) == (level == 0 ? 0 : trait.clampLevel(level));
    }
    public CatTraitProfile childProfile() { return child; }
    private CatTraitType resolve(String id) {
        if (id == null || id.length() > 128) throw new IllegalArgumentException("Invalid trait ID");
        var parsed = net.minecraft.resources.ResourceLocation.tryParse(id);
        if (parsed == null) throw new IllegalArgumentException("Invalid trait ID");
        return CatTraitRegistry.resolve(parsed, false);
    }
    private void active() { if (closed) throw new IllegalStateException("Trait event has ended"); }
    private void writable() { active(); if (!mutable) throw new IllegalStateException(type + " is an observation event"); }
    private void breed() { active(); if (child == null) throw new IllegalStateException("Only available during breed"); }
    public void close() { closed = true; }
}
