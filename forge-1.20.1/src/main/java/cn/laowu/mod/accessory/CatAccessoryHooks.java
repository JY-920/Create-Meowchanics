package cn.laowu.mod.accessory;

import cn.laowu.mod.*;
import cn.laowu.mod.api.CatAccessoryContext;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.function.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Optional bridge boundary: core gameplay never links a KubeJS/Rhino class. */
public final class CatAccessoryHooks {
    public static final List<String> EVENT_NAMES = List.of("equip", "unequip", "tick", "beforeAttack", "afterAttack",
            "beforeHurt", "afterHurt", "kill", "projectile", "beforeExplosion",
            "fastTick", "beforeAvoid", "beforeHeal", "acceptedAttack", "acceptedHurt");
    private static final Logger LOG = LoggerFactory.getLogger("laowu/accessory_scripts");
    private record Worn(String key, ItemStack stack) {}
    private static final Map<Cat, List<Worn>> WORN = new WeakHashMap<>();
    private static final Set<String> WARNED = new HashSet<>();
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private static boolean scanning;
    private static final ArrayDeque<Runnable> AFTER_DAMAGE = new ArrayDeque<>();
    private static Predicate<String> interested = name -> false;
    private static BiConsumer<String, CatAccessoryContext> dispatcher = (name, context) -> {};
    public static void installBridge(Predicate<String> listeners, BiConsumer<String, CatAccessoryContext> bridge) {
        interested = listeners; dispatcher = bridge;
    }
    public static boolean isDispatching() { return DEPTH.get() > 0; }
    public static <T> T withoutEvents(Supplier<T> action) {
        int previous = DEPTH.get(); DEPTH.set(previous + 1);
        try { return action.get(); } finally { DEPTH.set(previous); }
    }
    private static CatAccessoryContext context(String type, Cat cat, LivingEntity other, DamageSource source,
            Projectile projectile, double damage, boolean mutable) {
        return new CatAccessoryContext(type, cat, other, source, projectile, damage, -1, ItemStack.EMPTY, "", mutable);
    }
    private static boolean hasActive(Cat cat) {
        String outfit = CatClothesData.getOutfit(cat).id();
        return CatAccessories.equipment(cat).stream().map(s -> CatAccessoryRegistry.find(s, false))
                .anyMatch(d -> d != null && d.activeFor(outfit));
    }
    private static CatAccessoryContext fire(CatAccessoryContext context, boolean equipmentEvent) {
        Cat cat = context.getCat();
        if (cat.level().isClientSide || cat.isRemoved() || !cat.isAlive() && !context.getType().equals("acceptedHurt") || isDispatching()
                || !interested.test(context.getType()) || !equipmentEvent && !hasActive(cat)) {
            context.close(); return context;
        }
        int previous = DEPTH.get(); DEPTH.set(previous + 1);
        try { dispatcher.accept(context.getType(), context); }
        catch (RuntimeException | LinkageError error) {
            if (WARNED.add(context.getType())) LOG.error("Accessory script bridge failed in " + context.getType(), error);
        } finally { DEPTH.set(previous); context.close(); }
        return context;
    }
    public static void equipmentChanged(Cat cat) {
        if (scanning || cat.level().isClientSide || !cat.isAlive() || isDispatching()) return;
        if (!interested.test("equip") && !interested.test("unequip")) return;
        scanning = true;
        List<Worn> current = new ArrayList<>();
        try {
            var inventory = CatProfileData.openContainer(cat);
            boolean changed = false;
            for (int i = 0; i < 4; i++) {
                ItemStack stack = inventory.getItem(i);
                if (CatAccessoryRegistry.find(stack, false) != null)
                    changed |= CatAccessoryStackData.ensureIdentity(stack);
            }
            if (changed) inventory.setChanged();
            String outfit = CatClothesData.getOutfit(cat).id();
            Set<String> ids = new HashSet<>(), groups = new HashSet<>();
            for (int i = 0; i < 4; i++) {
                ItemStack stack = inventory.getItem(i);
                var def = CatAccessoryRegistry.find(stack, false);
                boolean active = def != null && def.enabled() && ids.add(def.id());
                if (active && !def.exclusiveGroup().isEmpty()) active = groups.add(def.exclusiveGroup());
                active &= def != null && def.activeFor(outfit);
                current.add(new Worn(active ? def.id() + "/" + CatAccessoryStackData.read(stack).getUUID("Instance") : "",
                        stack.copy()));
            }
        } finally { scanning = false; }
        List<Worn> previous = WORN.put(cat, current);
        for (int i = 0; i < 4; i++) {
            Worn before = previous == null ? new Worn("", ItemStack.EMPTY) : previous.get(i), after = current.get(i);
            if (before.key.equals(after.key)) continue;
            LOG.debug("Accessory change slot {}: {} -> {}", i, before.key, after.key);
            if (!before.key.isEmpty()) fire(new CatAccessoryContext("unequip", cat, null, null, null,
                    0, i, before.stack, "changed", false), true);
            if (!after.key.isEmpty()) fire(new CatAccessoryContext("equip", cat, null, null, null,
                    0, i, after.stack, previous == null ? "load" : "changed", false), true);
        }
    }
    public static void tick(Cat cat) {
        if (cat.level().isClientSide || !cat.isAlive() || Math.floorMod(cat.tickCount + cat.getId(), 10) != 0) return;
        equipmentChanged(cat);
        if(interested.test("fastTick"))fire(context("fastTick",cat,null,null,null,0,false),false);
        if (Math.floorMod(cat.tickCount + cat.getId(), 20) == 0 && interested.test("tick"))
            fire(context("tick", cat, null, null, null, 0, false), false);
    }
    private static Cat attacker(DamageSource source) {
        return source.getEntity() instanceof Cat cat
                && (source.getDirectEntity() == cat || source.getDirectEntity() instanceof Projectile)
                && !source.is(DamageTypes.THORNS) ? cat : null;
    }
    /** Pre-armor, after the loader's early invulnerability checks. Zero means cancel. */
    public static float beforeDamage(LivingEntity victim, DamageSource source, float amount) {
        if (victim.level().isClientSide || isDispatching() || amount <= 0
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
        Cat attacking = attacker(source);
        if (attacking != null && interested.test("beforeAttack")) {
            var event = fire(context("beforeAttack", attacking, victim, source, null, amount, true), false);
            if (event.isCanceled()) return 0;
            amount = (float)event.getDamage();
        }
        if (victim instanceof Cat cat && !DynamiteCatLastStand.isFinishing(cat) && interested.test("beforeHurt")) {
            var other = source.getEntity() instanceof LivingEntity living ? living : null;
            var event = fire(context("beforeHurt", cat, other, source, null, amount, true), false);
            if (event.isCanceled()) return 0;
            amount = (float)event.getDamage();
        }
        return amount;
    }
    /** Normalize loader timing: handlers run at server tick end, after vanilla applies health/death. */
    public static void afterDamage(LivingEntity victim, DamageSource source, float amount) {
        if (victim.level().isClientSide || isDispatching() || amount <= 0) return;
        if ((attacker(source) != null && interested.test("afterAttack")
                || victim instanceof Cat && interested.test("afterHurt")) && AFTER_DAMAGE.size() < 2048)
            AFTER_DAMAGE.add(() -> postDamage(victim, source, amount));
    }
    private static void postDamage(LivingEntity victim, DamageSource source, float amount) {
        Cat attacking = attacker(source);
        if (attacking != null && interested.test("afterAttack"))
            fire(context("afterAttack", attacking, victim, source, null, amount, false), false);
        if (victim instanceof Cat cat && interested.test("afterHurt"))
            fire(context("afterHurt", cat, source.getEntity() instanceof LivingEntity living ? living : null,
                    source, null, amount, false), false);
    }
    /** Earliest avoidance stage; the loader caller already excludes void/forced death. */
    public static boolean beforeAvoid(Cat cat,DamageSource source) {
        if(!interested.test("beforeAvoid"))return false;
        return fire(context("beforeAvoid",cat,source.getEntity() instanceof LivingEntity living?living:null,
                source,null,0,true),false).isCanceled();
    }
    public static float beforeHeal(Cat cat,float amount) {
        if(!interested.test("beforeHeal"))return amount;
        var event=fire(context("beforeHeal",cat,cat,null,null,amount,true),false);
        return event.isCanceled()?0:(float)event.getDamage();
    }
    /** Synchronous, accepted health-loss events; no cancelled hits or nested helper damage. */
    public static void acceptedAttack(Cat cat,LivingEntity victim,DamageSource source,float taken) {
        if(interested.test("acceptedAttack"))fire(context("acceptedAttack",cat,victim,source,null,taken,false),false);
    }
    public static void acceptedHurt(Cat cat,DamageSource source,float taken) {
        if(interested.test("acceptedHurt"))fire(context("acceptedHurt",cat,
                source.getEntity() instanceof LivingEntity living?living:null,source,null,taken,false),false);
    }
    public static void kill(LivingEntity victim, DamageSource source) {
        Cat cat = attacker(source);
        if (cat != null && !cat.level().isClientSide && !isDispatching() && interested.test("kill")
                && AFTER_DAMAGE.size() < 2048)
            AFTER_DAMAGE.add(() -> {
                if (!victim.isAlive()) fire(context("kill", cat, victim, source, null, 0, false), false);
            });
    }
    public static void flushAfterDamage() {
        int count = AFTER_DAMAGE.size();
        for (int i = 0; i < count; i++) {
            Runnable action = AFTER_DAMAGE.poll();
            if (action != null) action.run();
        }
    }
    /** Called after aiming, before addFreshEntity. Cancellation still uses that normal attack cooldown. */
    public static boolean projectile(Cat cat, LivingEntity target, Projectile projectile) {
        if (!interested.test("projectile")) return true;
        return !fire(context("projectile", cat, target, null, projectile, 0, true), false).isCanceled();
    }
    /** Canceling skips the blast, never the cat's final death/pancake conversion. */
    public static double beforeExplosion(Cat cat, double damage) {
        if (!interested.test("beforeExplosion")) return damage;
        var event = fire(context("beforeExplosion", cat, null, null, null, damage, true), false);
        return event.isCanceled() ? -1 : event.getDamage();
    }
    public static void resetWorld() { WORN.clear(); WARNED.clear(); AFTER_DAMAGE.clear(); scanning = false; DEPTH.remove(); }
    private CatAccessoryHooks() {}
}
