package cn.laowu.mod.api;

import cn.laowu.mod.CatProfileData;
import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.accessory.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;

/** A short-lived server handle. All changes are saved through the live cat container. */
public final class CatAccessoryHandle {
    private final Cat cat;
    private final int slot;
    private final ItemStack identity;
    private final String definitionId;
    private final Container container;
    CatAccessoryHandle(Cat cat, int slot, Container container, ItemStack stack, String definitionId) {
        this.container = container;
        this.cat = cat; this.slot = slot; this.identity = stack; this.definitionId = definitionId;
    }
    public String getId() { return definitionId; }
    public String getScript() { return definition().script(); }
    public int getSlot() { return slot; }
    public ItemStack getStack() { return live().copy(); }
    private ItemStack live() {
        CatAccessoryApi.requireServer(cat);
        ItemStack stack = container.getItem(slot);
        CatAccessoryDefinition def = CatAccessoryRegistry.find(stack, false);
        if (stack != identity || def == null || !def.id().equals(definitionId)
                || !def.activeFor(CatClothesData.getOutfit(cat).id()))
            throw new IllegalStateException("Accessory handle expired: reacquire it in the current event");
        return stack;
    }
    private CatAccessoryDefinition definition() { return CatAccessoryRegistry.find(live(), false); }
    private CompoundTag data() { return CatAccessoryStackData.read(live()); }
    private void save(CompoundTag data) {
        if (CatAccessoryStackData.read(live()).equals(data)) return;
        CatAccessoryStackData.write(live(), data);
        container.setChanged();
    }
    private long now() { return cat.getServer().overworld().getGameTime(); }
    public int getMaxDurability() { return live().getMaxDamage(); }
    public int getDurability() { var stack=live();return Math.max(0,stack.getMaxDamage()-stack.getDamageValue()); }
    /** Exact wear; true means it broke. Reacquire the handle after a break; never use a saved stack copy. */
    public boolean damageDurability(int amount) {
        if(amount<0||amount>1_000_000)throw new IllegalArgumentException("Invalid durability cost");
        return CatAccessoryDurability.damage(cat,container,slot,live(),amount);
    }
    public int getChargeCapacity() { return definition().charge().capacity(); }
    public int getCharge() {
        var charge = definition().charge();
        CompoundTag data = data();
        return data.contains("Charge", Tag.TAG_ANY_NUMERIC) ? charge.clamp(data.getInt("Charge")) : charge.initial();
    }
    public boolean consumeCharge(int amount) {
        if (amount < 0 || amount > 1_000_000) throw new IllegalArgumentException("Invalid charge cost");
        int available = getCharge();
        if (available < amount) return false;
        if (amount > 0) { CompoundTag data = data(); data.putInt("Charge", available - amount); save(data); }
        return true;
    }
    /** Adds charge only; caller must consume their gas/material separately. Returns actual amount added. */
    public int recharge(int amount) {
        if (amount < 0 || amount > 1_000_000) throw new IllegalArgumentException("Invalid charge amount");
        int before = getCharge(), added = Math.min(amount, getChargeCapacity() - before);
        if (added > 0) { CompoundTag data = data(); data.putInt("Charge", before + added); save(data); }
        return added;
    }
    public long cooldownRemaining(String key) {
        CatAccessoryScriptRules.key(key);
        return Math.max(0, data().getCompound("Cooldowns").getLong(key) - now());
    }
    public void startCooldown(String key, int ticks) {
        CatAccessoryScriptRules.key(key); CatAccessoryScriptRules.ticks(ticks);
        CompoundTag data = data(), cooldowns = data.getCompound("Cooldowns");
        long now = now();
        for (String old : java.util.Set.copyOf(cooldowns.getAllKeys()))
            if (cooldowns.getLong(old) <= now) cooldowns.remove(old);
        room(cooldowns, key);
        if (ticks == 0) cooldowns.remove(key); else cooldowns.putLong(key, now + ticks);
        data.put("Cooldowns", cooldowns); save(data);
    }
    /** Atomically reserves the cooldown and charge BEFORE script effects can trigger other events. */
    public boolean tryActivate(String key, int ticks, int chargeCost) {
        CatAccessoryScriptRules.key(key); CatAccessoryScriptRules.ticks(ticks);
        CompoundTag data = data(), cooldowns = data.getCompound("Cooldowns");
        long now = now();
        int available = getCharge();
        if (!CatAccessoryScriptRules.canActivate(now, cooldowns.getLong(key), available, chargeCost)) return false;
        for (String old : java.util.Set.copyOf(cooldowns.getAllKeys()))
            if (cooldowns.getLong(old) <= now) cooldowns.remove(old);
        room(cooldowns, key);
        if (ticks > 0) cooldowns.putLong(key, now + ticks); else cooldowns.remove(key);
        data.put("Cooldowns", cooldowns);
        if (chargeCost > 0) data.putInt("Charge", available - chargeCost);
        save(data); return true;
    }
    public double number(String key) { CatAccessoryScriptRules.key(key); return data().getCompound("Numbers").getDouble(key); }
    public void setNumber(String key, double value) {
        CatAccessoryScriptRules.key(key);
        if (!Double.isFinite(value) || Math.abs(value) > 1.0E12) throw new IllegalArgumentException("Invalid state number");
        CompoundTag data = data(), values = data.getCompound("Numbers");
        room(values, key); values.putDouble(key, value); data.put("Numbers", values); save(data);
    }
    public double increment(String key, double amount) {
        double value = number(key) + amount; setNumber(key, value); return value;
    }
    /** Dynamic effective-stat bonus: never mutates genes, removed automatically when inactive/unequipped. */
    public void setStatBonus(String stat, int amount) {
        if (!CatAccessoryDefinition.STATS.contains(stat) || amount < -300 || amount > 300)
            throw new IllegalArgumentException("Stat bonus requires a known stat and -300..300");
        CompoundTag data = data(), bonuses = data.getCompound("StatBonuses");
        if (amount == 0) bonuses.remove(stat); else bonuses.putInt(stat, amount);
        data.put("StatBonuses", bonuses); save(data);
    }
    public void clearStatBonuses() {
        CompoundTag data = data(); data.remove("StatBonuses"); save(data);
    }
    public String text(String key) { CatAccessoryScriptRules.key(key); return data().getCompound("Text").getString(key); }
    public void setText(String key, String value) {
        CatAccessoryScriptRules.key(key);
        if (value == null || value.length() > 512) throw new IllegalArgumentException("State text exceeds 512 characters");
        CompoundTag data = data(), values = data.getCompound("Text");
        room(values, key); values.putString(key, value); data.put("Text", values); save(data);
    }
    public void removeState(String key) {
        CatAccessoryScriptRules.key(key); CompoundTag data = data();
        for (String section : new String[]{"Numbers", "Text", "Cooldowns"}) data.getCompound(section).remove(key);
        save(data);
    }
    private static void room(CompoundTag section, String key) {
        if (!section.contains(key) && section.size() >= 64) throw new IllegalArgumentException("At most 64 keys per accessory state section");
    }
}
