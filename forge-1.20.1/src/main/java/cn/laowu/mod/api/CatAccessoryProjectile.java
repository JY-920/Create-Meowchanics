package cn.laowu.mod.api;

/** Shared projectile damage hook, independent of the concrete career projectile class. */
public interface CatAccessoryProjectile {
    float getAccessoryDamage();
    void setAccessoryDamage(double amount);
}
