package cn.laowu.mod.api;

import cn.laowu.mod.accessory.CatAccessoryScriptRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

/** Synchronous event lifetime. Store UUIDs/state rather than keeping this event for delayed work. */
public final class CatAccessoryContext {
    private final String type;
    private final Cat cat;
    private final LivingEntity other;
    private final DamageSource source;
    private final Projectile projectile;
    private final int slot;
    private final ItemStack stack;
    private final String reason;
    private final boolean mutable;
    private double damage;
    private boolean canceled, closed;
    public CatAccessoryContext(String type, Cat cat, LivingEntity other, DamageSource source, Projectile projectile,
            double damage, int slot, ItemStack stack, String reason, boolean mutable) {
        this.type=type; this.cat=cat; this.other=other; this.source=source; this.projectile=projectile;
        this.damage=damage; this.slot=slot; this.stack=stack.copy(); this.reason=reason; this.mutable=mutable;
    }
    public String getType() { return type; }
    public Cat getCat() { return cat; }
    /** Attack: victim; hurt: attacker; kill: victim; projectile: intended target. May be null. */
    public LivingEntity getOther() { return other; }
    public DamageSource getSource() { return source; }
    public Projectile getProjectile() { return projectile; }
    public int getSlot() { return slot; }
    public ItemStack getStack() { return stack.copy(); }
    public String getReason() { return reason; }
    public double getDamage() { return damage; }
    /** Stable scripting views: Forge's KubeJS mapping names differ from Mojang Java names. */
    public long getGameTime() { return cat.level().getGameTime(); }
    public String getOtherId() { return other==null?"":other.getUUID().toString(); }
    public float getOtherHealth() { return other==null?0:other.getHealth(); }
    public float getOtherMaxHealth() { return other==null?0:other.getMaxHealth(); }
    public boolean isFireDamage() { return source!=null&&source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE); }
    public boolean isThornsDamage() { return source!=null&&source.is(net.minecraft.world.damagesource.DamageTypes.THORNS); }
    public boolean isBypassDamage() { return source!=null&&source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY); }
    public boolean hasAttacker() { return source!=null&&source.getEntity()!=null; }
    public boolean isSelfDamage() { return source!=null&&source.getEntity()==cat; }
    public boolean hasLivingAttacker() { return source!=null&&source.getEntity() instanceof LivingEntity; }
    public boolean isPassenger() { return cat.isPassenger(); }
    public boolean isSitting() { return cat.isOrderedToSit()||cat.isInSittingPose(); }
    public double getHorizontalSpeedSquared() { return cat.getDeltaMovement().horizontalDistanceSqr(); }
    public double random() { active();return cat.getRandom().nextDouble(); }
    public void extinguish() { active();cat.clearFire(); }
    public double getAmount() { return damage; }
    public void setAmount(double amount) { setDamage(amount); }
    public boolean isCanceled() { return canceled; }
    public boolean isMutable() { return mutable; }
    public void setDamage(double damage) {
        writable();
        if (type.equals("projectile")) throw new IllegalStateException("Use projectile methods, not event damage");
        this.damage = CatAccessoryScriptRules.damage(damage);
    }
    public void cancel() { writable(); canceled = true; }
    public CatAccessoryHandle accessory(String id) { active(); return CatAccessoryApi.accessory(cat, id); }
    public int stat(String name) { return CatAccessoryApi.stat(cat, name); }
    public String getOutfit() { return CatAccessoryApi.outfit(cat); }
    public boolean damageOther(double damage) { active(); return CatAccessoryApi.damage(cat, other, damage); }
    public boolean healSelf(double amount) { active(); return CatAccessoryApi.heal(cat, cat, amount); }
    public void scaleProjectileSpeed(double scale) {
        writable();
        if (!type.equals("projectile") || projectile == null || !Double.isFinite(scale) || scale < 0 || scale > 4)
            throw new IllegalArgumentException("Projectile speed scale requires projectile event and 0..4");
        projectile.setDeltaMovement(projectile.getDeltaMovement().scale(scale));
    }
    public double getProjectileDamage() {
        return projectile instanceof CatAccessoryProjectile shot ? shot.getAccessoryDamage() : 0;
    }
    public void setProjectileDamage(double amount) {
        writable(); CatAccessoryScriptRules.damage(amount);
        if (!type.equals("projectile") || !(projectile instanceof CatAccessoryProjectile shot))
            throw new IllegalStateException("This projectile has no attack damage (for example a support package)");
        shot.setAccessoryDamage(amount);
    }
    public void close() { closed = true; }
    private void active() {
        if (closed) throw new IllegalStateException("Accessory event has ended");
        CatAccessoryApi.requireServer(cat);
    }
    private void writable() {
        active(); if (!mutable) throw new IllegalStateException(type + " is an observation event");
    }
}
