package cn.laowu.mod;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.BooleanSupplier;

/** Ordinary projectile damage, with an impact-scoped knockback policy. */
public final class CatProjectileDamage {
    private static final KnockbackScope<LivingEntity> KNOCKBACK = new KnockbackScope<>();

    public static boolean hurt(LivingEntity target, DamageSource source, float amount) {
        return hurt(target, source, amount, false);
    }

    /** Future accessories can opt an impact into vanilla knockback without changing damage. */
    public static boolean hurt(LivingEntity target, DamageSource source, float amount, boolean allowKnockback) {
        return KNOCKBACK.call(target, allowKnockback, () -> target.hurt(source, amount));
    }

    public static boolean suppressesKnockback(LivingEntity target) {
        return KNOCKBACK.suppresses(target);
    }

    /**
     * Cancel only this victim's knockback during this synchronous hurt call.
     * No velocity reset, temporary resistance modifier, saved flag or per-tick cleanup.
     */
    static final class KnockbackScope<T> {
        private record Impact<T>(T target, boolean allowKnockback) {}
        private final ThreadLocal<Impact<T>> current = new ThreadLocal<>();

        boolean call(T target, boolean allowKnockback, BooleanSupplier damage) {
            Impact<T> previous = current.get();
            current.set(new Impact<>(target, allowKnockback));
            try {
                return damage.getAsBoolean();
            } finally {
                if (previous == null) current.remove();
                else current.set(previous);
            }
        }

        boolean suppresses(T target) {
            Impact<T> impact = current.get();
            return impact != null && impact.target() == target && !impact.allowKnockback();
        }
    }

    private CatProjectileDamage() {}
}
