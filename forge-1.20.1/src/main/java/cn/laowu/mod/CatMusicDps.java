package cn.laowu.mod;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.projectile.Projectile;
import java.util.*;

/** Five-second rolling final-damage estimate, shared by musicians instead of rescanning combat logs. */
public final class CatMusicDps {
    private record Hit(long tick, float amount) {}
    private static final Map<Cat, ArrayDeque<Hit>> HITS = new WeakHashMap<>();
    public static void record(DamageSource source, LivingEntity victim, float amount) {
        var owner = source.getEntity();
        if (!(owner instanceof Cat) && source.getDirectEntity() instanceof Projectile projectile) owner = projectile.getOwner();
        if (!(owner instanceof Cat cat) || victim.level().isClientSide || !Float.isFinite(amount) || amount <= 0
                || CatClothesData.getOutfit(cat).isSupport() || CatTeamRules.friendly(cat, victim)) return;
        record(cat, amount);
    }
    public static void record(Cat cat, float amount) {
        if (cat.level().isClientSide || !Float.isFinite(amount) || amount <= 0) return;
        var hits = HITS.computeIfAbsent(cat, key -> new ArrayDeque<>());
        long tick = cat.level().getGameTime();
        prune(hits, tick);
        // Coalesce multi-target/same-tick damage without losing total effective AoE DPS.
        Hit last = hits.peekLast();
        if (last != null && last.tick == tick) { hits.removeLast(); amount += last.amount; }
        hits.addLast(new Hit(tick, amount));
    }
    public static double recent(Cat cat) {
        var hits = HITS.get(cat);
        if (hits == null) return 0;
        prune(hits, cat.level().getGameTime());
        double total = 0;
        for (Hit hit : hits) total += hit.amount;
        return total / (CatMusicRules.DPS_WINDOW / 20.0);
    }
    private static void prune(ArrayDeque<Hit> hits, long now) {
        while (!hits.isEmpty() && hits.peekFirst().tick <= now - CatMusicRules.DPS_WINDOW) hits.removeFirst();
    }
    private CatMusicDps() {}
}
