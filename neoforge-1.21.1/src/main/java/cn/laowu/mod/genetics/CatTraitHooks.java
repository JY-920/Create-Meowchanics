package cn.laowu.mod.genetics;

import cn.laowu.mod.DynamiteCatLastStand;
import cn.laowu.mod.accessory.CatAccessoryHooks;
import cn.laowu.mod.api.CatTraitContext;
import net.minecraft.world.damagesource.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.projectile.Projectile;
import java.util.*;
import java.util.function.*;

/** Optional bridge. No KubeJS classes, entity-join dispatch, chunk loads or global entity scans. */
public final class CatTraitHooks {
    public static final List<String> EVENT_NAMES = List.of("added", "removed", "levelChanged", "tick", "fastTick",
            "beforeAttack", "afterAttack", "beforeHurt", "afterHurt", "beforeHeal", "death", "kill", "breed");
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("laowu/trait_scripts");
    private record Seen(Map<String, Integer> levels, long revision) {}
    private static final Map<Cat, Seen> SEEN = new WeakHashMap<>();
    private static final Map<Cat, Long> STATE_REVISIONS = new WeakHashMap<>();
    private static final Set<String> WARNED = new HashSet<>();
    private static final ArrayDeque<Runnable> PENDING = new ArrayDeque<>();
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private static Predicate<String> interested = name -> false;
    private static BiConsumer<String, CatTraitContext> dispatcher = (name, event) -> {};
    public static void installBridge(Predicate<String> listeners, BiConsumer<String, CatTraitContext> bridge) {
        interested = listeners; dispatcher = bridge;
    }
    public static boolean isDispatching() { return DEPTH.get() > 0 || CatAccessoryHooks.isDispatching(); }
    public static <T> T withoutEvents(Supplier<T> action) {
        int previous = DEPTH.get(); DEPTH.set(previous + 1);
        try { return CatAccessoryHooks.withoutEvents(action); } finally { DEPTH.set(previous); }
    }
    private static CatTraitContext fire(CatTraitContext context, boolean change) {
        Cat cat = context.getCat();
        if (isDispatching() || !interested.test(context.getType())
                || cat != null && (cat.level().isClientSide || cat.isRemoved()
                || !cat.isAlive() && !context.getType().equals("death")
                || !change && CatTraitData.ensure(cat).traits().stream().noneMatch(t -> t.trait().enabled()))) {
            context.close(); return context;
        }
        int previous = DEPTH.get(); DEPTH.set(previous + 1);
        try { dispatcher.accept(context.getType(), context); }
        catch (RuntimeException | LinkageError error) {
            if (WARNED.add(context.getType())) LOG.error("Trait script callback failed: " + context.getType(), error);
        } finally { DEPTH.set(previous); context.close(); }
        return context;
    }
    private static CatTraitContext event(String type, Cat cat, LivingEntity other, DamageSource source, double amount, boolean mutable) {
        return new CatTraitContext(type, cat, other, source, amount, mutable);
    }
    public static void tick(Cat cat) {
        if (cat.level().isClientSide || !cat.isAlive() || isDispatching() || Math.floorMod(cat.tickCount + cat.getId(), 10) != 0) return;
        long revision = CatTraitRegistry.revision(false);
        if ((cat.getPersistentData().contains(CatTraitScriptState.TAG)
                || CatTraitData.ensure(cat).traits().stream().anyMatch(t -> t.trait() instanceof ScriptedCatTrait))
                && !Objects.equals(STATE_REVISIONS.put(cat, revision), revision)) CatTraitScriptState.refresh(cat);
        boolean changes = interested.test("added") || interested.test("removed") || interested.test("levelChanged");
        if (changes) {
            Map<String, Integer> levels = new LinkedHashMap<>();
            var profile = CatTraitData.ensure(cat);
            for (var saved : profile.traits()) if (saved.trait().enabled())
                levels.put(saved.trait().id().toString(), profile.level(saved.trait()));
            Seen previous = SEEN.put(cat, new Seen(Map.copyOf(levels), revision));
            Map<String, Integer> before = previous == null ? Map.of() : previous.levels();
            Set<String> ids = new LinkedHashSet<>(before.keySet()); ids.addAll(levels.keySet());
            for (String id : ids) {
                int oldLevel = before.getOrDefault(id, 0), newLevel = levels.getOrDefault(id, 0);
                if (oldLevel == newLevel) continue;
                String type = oldLevel == 0 ? "added" : newLevel == 0 ? "removed" : "levelChanged";
                String reason = previous == null ? "load" : previous.revision() != revision ? "reload" : "changed";
                fire(event(type, cat, null, null, 0, false).changed(id, reason, oldLevel, newLevel), true);
            }
        }
        if (interested.test("fastTick")) fire(event("fastTick", cat, null, null, 0, false), false);
        if (Math.floorMod(cat.tickCount + cat.getId(), 20) == 0 && interested.test("tick"))
            fire(event("tick", cat, null, null, 0, false), false);
    }
    private static Cat attacker(DamageSource source) {
        return source.getEntity() instanceof Cat cat && (source.getDirectEntity() == cat || source.getDirectEntity() instanceof Projectile)
                && !source.is(DamageTypes.THORNS) ? cat : null;
    }
    public static float beforeDamage(LivingEntity victim, DamageSource source, float amount) {
        if (victim.level().isClientSide || isDispatching() || amount <= 0 || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
        Cat attacking = attacker(source);
        if (attacking != null) {
            var context = fire(event("beforeAttack", attacking, victim, source, amount, true), false);
            if (context.isCanceled()) return 0; amount = (float) context.getAmount();
        }
        if (victim instanceof Cat cat && !DynamiteCatLastStand.isFinishing(cat)) {
            var other = source.getEntity() instanceof LivingEntity living ? living : null;
            var context = fire(event("beforeHurt", cat, other, source, amount, true), false);
            if (context.isCanceled()) return 0; amount = (float) context.getAmount();
        }
        return amount;
    }
    public static float beforeHeal(Cat cat, float amount) {
        if (amount <= 0 || cat.level().isClientSide || isDispatching()) return amount;
        var context = fire(event("beforeHeal", cat, cat, null, amount, true), false);
        return context.isCanceled() ? 0 : (float) context.getAmount();
    }
    public static void afterDamage(LivingEntity victim, DamageSource source, float amount) {
        if (victim.level().isClientSide || isDispatching() || amount <= 0) return;
        Cat attacking = attacker(source);
        if (attacking != null && interested.test("afterAttack"))
            enqueue(() -> fire(event("afterAttack", attacking, victim, source, amount, false), false));
        if (victim instanceof Cat cat && interested.test("afterHurt"))
            enqueue(() -> fire(event("afterHurt", cat, source.getEntity() instanceof LivingEntity other ? other : null, source, amount, false), false));
    }
    public static void death(LivingEntity victim, DamageSource source) {
        if (victim.level().isClientSide || isDispatching()) return;
        Cat attacking = attacker(source);
        if (victim instanceof Cat cat && interested.test("death"))
            enqueue(() -> { if (!cat.isAlive()) fire(event("death", cat, source.getEntity() instanceof LivingEntity other ? other : null, source, 0, false), false); });
        if (attacking != null && interested.test("kill"))
            enqueue(() -> { if (!victim.isAlive()) fire(event("kill", attacking, victim, source, 0, false), false); });
    }
    public static CatTraitProfile breed(CatTraitProfile first, CatTraitProfile second, CatTraitProfile child) {
        if (isDispatching() || !interested.test("breed") || !net.neoforged.fml.util.thread.EffectiveSide.get().isServer()) return child;
        return fire(CatTraitContext.breeding(first, second, child), true).childProfile();
    }
    private static void enqueue(Runnable action) { if (PENDING.size() < 2048) PENDING.add(action); }
    public static void flush() {
        int count = PENDING.size();
        for (int i = 0; i < count; i++) { var action = PENDING.poll(); if (action != null) action.run(); }
    }
    public static void reset() { SEEN.clear(); STATE_REVISIONS.clear(); PENDING.clear(); WARNED.clear(); }
    private CatTraitHooks() {}
}
