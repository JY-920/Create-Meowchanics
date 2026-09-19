package cn.laowu.mod.api;

import cn.laowu.mod.accessory.CatAccessoryScriptRules;
import cn.laowu.mod.genetics.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Cat;

/** Reacquire during each callback. Handles expire when the tick, profile or registry changes. */
public final class CatTraitHandle {
    private final Cat cat;
    private final CatTraitType trait;
    private final CatTraitProfile profile;
    private final long tick, revision;
    CatTraitHandle(Cat cat, CatTraitType trait) {
        this.cat = cat; this.trait = trait; this.profile = CatTraitData.ensure(cat);
        this.tick = cat.getServer().overworld().getGameTime(); this.revision = CatTraitRegistry.revision(false);
    }
    private void live(boolean write) {
        CatTraitApi.requireServer(cat);
        if (cat.getServer().overworld().getGameTime() != tick || CatTraitRegistry.revision(false) != revision
                || CatTraitData.ensure(cat) != profile || !trait.enabled() || !profile.has(trait))
            throw new IllegalStateException("Trait handle expired; reacquire it in the current callback");
        if (write && !cat.isAlive()) throw new IllegalStateException("Dead cat trait state is read-only");
    }
    public String getId() { live(false); return trait.id().toString(); }
    public int getLevel() { live(false); return profile.level(trait); }
    public int getMaxLevel() { live(false); return trait.maxLevel(); }
    private CompoundTag data() { live(false); return CatTraitScriptState.read(cat, trait.id().toString()); }
    private void save(CompoundTag value) { live(true); CatTraitScriptState.write(cat, trait.id().toString(), value); }
    public double number(String key) { CatAccessoryScriptRules.key(key); return data().getCompound("Numbers").getDouble(key); }
    public void setNumber(String key, double value) {
        CatAccessoryScriptRules.key(key);
        if (!Double.isFinite(value) || Math.abs(value) > 1.0E12) throw new IllegalArgumentException("Invalid trait state number");
        var root = data(); var values = root.getCompound("Numbers"); room(values, key);
        values.putDouble(key, value); root.put("Numbers", values); save(root);
    }
    public double increment(String key, double amount) { double value = number(key) + amount; setNumber(key, value); return value; }
    public String text(String key) { CatAccessoryScriptRules.key(key); return data().getCompound("Text").getString(key); }
    public void setText(String key, String value) {
        CatAccessoryScriptRules.key(key);
        if (value == null || value.length() > 512) throw new IllegalArgumentException("Trait text exceeds 512 characters");
        var root = data(); var values = root.getCompound("Text"); room(values, key);
        values.putString(key, value); root.put("Text", values); save(root);
    }
    public long cooldownRemaining(String key) {
        CatAccessoryScriptRules.key(key);
        return Math.max(0, data().getCompound("Cooldowns").getLong(key) - tick);
    }
    public void startCooldown(String key, int ticks) {
        CatAccessoryScriptRules.key(key); CatAccessoryScriptRules.ticks(ticks);
        var root = data(); var values = root.getCompound("Cooldowns");
        for (String old : java.util.Set.copyOf(values.getAllKeys())) if (values.getLong(old) <= tick) values.remove(old);
        room(values, key);
        if (ticks == 0) values.remove(key); else values.putLong(key, tick + ticks);
        root.put("Cooldowns", values); save(root);
    }
    public boolean tryActivate(String key, int ticks) {
        CatAccessoryScriptRules.key(key); CatAccessoryScriptRules.ticks(ticks);
        if (cooldownRemaining(key) > 0) return false;
        startCooldown(key, ticks); return true;
    }
    public void setStatBonus(String statName, int amount) {
        if (java.util.Arrays.stream(CatStat.values()).noneMatch(s -> s.serializedName().equals(statName))
                || amount < -999999 || amount > 999999) throw new IllegalArgumentException("Invalid effective stat bonus");
        var root = data(); var values = root.getCompound("StatBonuses");
        live(true);
        if (values.getInt(statName) == amount) return;
        if (amount == 0) values.remove(statName); else values.putInt(statName, amount);
        root.put("StatBonuses", values); save(root); CatTraitScriptState.refresh(cat);
    }
    public void clearStatBonuses() { var root = data(); root.remove("StatBonuses"); save(root); CatTraitScriptState.refresh(cat); }
    public void removeState(String key) {
        CatAccessoryScriptRules.key(key); var root = data();
        for (String section : new String[]{"Numbers", "Text", "Cooldowns"}) root.getCompound(section).remove(key);
        save(root);
    }
    private static void room(CompoundTag values, String key) {
        if (!values.contains(key) && values.size() >= 64) throw new IllegalArgumentException("At most 64 keys per trait state section");
    }
}
