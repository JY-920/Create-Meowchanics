package cn.laowu.mod;

import cn.laowu.mod.genetics.*;
import cn.laowu.mod.network.ModNetwork;
import cn.laowu.mod.network.CockroachStatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.Level;
import java.util.*;

/** Nearby allies add flat effective stats; genes, breeding and saved inventories never change. */
public final class CatCockroachSwarm {
    public static final double RANGE = 12;
    public static final int PER_ALLY = 6, HURT_TICKS = 10;
    private static final Map<Cat, State> SERVER = new WeakHashMap<>();
    private static final Map<Level, Map<UUID, Client>> CLIENT = new WeakHashMap<>();
    private static final class State {
        int allies, mode; long started, hurtUntil, nextScan, nextSync, leapUntil;
    }
    private record Client(int allies, int mode, long started, long until) {}
    public static int allies(Cat cat) {
        if (CatClothesData.getOutfit(cat) != CatOutfitType.COCKROACH) return 0;
        if (!cat.level().isClientSide) { State s = SERVER.get(cat); return s == null ? 0 : s.allies; }
        Client s = client(cat); return s == null ? 0 : s.allies;
    }
    private static Client client(Cat cat) {
        var map = CLIENT.get(cat.level());
        if (map == null) return null;
        Client s = map.get(cat.getUUID());
        if (s != null && s.until <= cat.level().getGameTime()) { map.remove(cat.getUUID()); return null; }
        return s;
    }
    public static int statBonus(Cat cat, CatStat stat) {
        return stat == CatStat.HEALTH || stat == CatStat.ATTACK
                ? (int)Math.min(999L, (long)allies(cat) * PER_ALLY) : 0;
    }
    public static int count(Cat cat) {
        return cat.level().getEntitiesOfClass(Cat.class, cat.getBoundingBox().inflate(RANGE), other ->
                other != cat && other.isAlive() && other.isTame() && !other.isRemoved()
                && CatClothesData.getOutfit(other) == CatOutfitType.COCKROACH
                && CatTeamRules.friendly(cat, other) && cat.distanceToSqr(other) <= RANGE * RANGE).size();
    }
    public static void hurt(Cat cat) {
        if (!cat.level().isClientSide && CatClothesData.getOutfit(cat) == CatOutfitType.COCKROACH) {
            State s = SERVER.computeIfAbsent(cat, ignored -> new State());
            s.hurtUntil = cat.level().getGameTime() + HURT_TICKS;
            s.started = cat.level().getGameTime(); s.mode = 1; s.nextSync = 0;
        }
    }
    public static void leap(Cat cat, boolean active) {
        if (cat.level().isClientSide) return;
        State s = SERVER.computeIfAbsent(cat, ignored -> new State());
        s.leapUntil = active ? cat.level().getGameTime() + 26 : 0;
        if (active) { s.mode = 2; s.started = cat.level().getGameTime(); }
        s.nextSync = 0; syncTo(cat, null);
    }
    public static void tick(Cat cat) {
        if (cat.level().isClientSide) return;
        if (!cat.isAlive() || CatClothesData.getOutfit(cat) != CatOutfitType.COCKROACH) {
            clear(cat); return;
        }
        State s = SERVER.computeIfAbsent(cat, ignored -> new State());
        long now = cat.level().getGameTime();
        if (now >= s.nextScan) {
            s.nextScan = now + 20;
            int count = count(cat);
            if (s.allies != count) {
                s.allies = count; s.nextSync = 0;
                CatAttributeEffects.refresh(cat);
            }
        }
        var target = cat.getTarget();
        boolean chase = !cat.isNoAi() && !cat.isPassenger() && !CareerCatBehavior.isCombatResting(cat)
                && !CatProfileData.isBeingViewed(cat) && target != null && target.isAlive()
                && CatTeamRules.canHarm(cat, target) && cat.distanceToSqr(target) > 4;
        int mode = !CatPoseData.isPancake(cat) && !cat.isPassenger()
                ? (now < s.hurtUntil ? 1 : chase || now < s.leapUntil ? 2 : 0) : 0;
        if (mode != s.mode) { s.mode = mode; s.started = now; s.nextSync = 0; }
        if (now >= s.nextSync) { s.nextSync = now + 20; syncTo(cat, null); }
    }
    public static void clear(Cat cat) {
        if (!cat.level().isClientSide && SERVER.remove(cat) != null) {
            ModNetwork.cockroachState(cat, null, new CockroachStatePacket(cat.getId(), cat.getUUID(), 0, 0, 0));
            CatAttributeEffects.refresh(cat);
        }
    }
    public static int mode(Cat cat) {
        if (CatClothesData.getOutfit(cat) != CatOutfitType.COCKROACH) return 0;
        if (!cat.level().isClientSide) { State s = SERVER.get(cat); return s == null ? 0 : s.mode; }
        Client s = client(cat); return s == null ? 0 : s.mode;
    }
    public static float age(Cat cat, float partial) {
        long start;
        if (cat.level().isClientSide) { Client s = client(cat); if (s == null) return 0; start = s.started; }
        else { State s = SERVER.get(cat); if (s == null) return 0; start = s.started; }
        return Math.max(0, cat.level().getGameTime() - start + partial);
    }
    public static void receive(Cat cat, int allies, int mode, int age) {
        if (!cat.level().isClientSide) return;
        long now = cat.level().getGameTime();
        var map = CLIENT.computeIfAbsent(cat.level(), ignored -> new HashMap<>());
        map.entrySet().removeIf(e -> e.getValue().until <= now);
        map.put(cat.getUUID(), new Client(Math.max(0, Math.min(100000, allies)), Math.max(0, Math.min(2, mode)),
                now - Math.max(0, Math.min(1200, age)), now + 60));
    }
    public static void syncTo(Cat cat, ServerPlayer player) {
        State s = SERVER.get(cat);
        if (s != null) ModNetwork.cockroachState(cat, player, new CockroachStatePacket(cat.getId(), cat.getUUID(),
                s.allies, s.mode, (int)Math.min(1200, Math.max(0, cat.level().getGameTime() - s.started))));
    }
    private CatCockroachSwarm() {}
}
