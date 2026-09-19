package cn.laowu.mod;

import cn.laowu.mod.genetics.*;
import cn.laowu.mod.network.ModNetwork;
import cn.laowu.mod.network.MusicSupportPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Cat;
import java.util.*;

/** Server-authoritative performance and strongest-source group haste, with independent visual expiry. */
public final class CatMusicSupport {
    private static final CatVisualStates<State> STATES = new CatVisualStates<>();
    private static final Map<Cat, Offer> OFFERS = new WeakHashMap<>();
    private static final class State {
        long performingUntil, started, bonusUntil, nextSync;
        int pose = -1;
        float radius;
        double bonus;
    }
    private record Offer(long tick, double bonus) {}
    private static long now(Cat cat) { return cat.level().getGameTime(); }
    private static State state(Cat cat) { return STATES.getOrCreate(cat, State::new); }
    public static double intelligence(Cat cat) { return ServerConfig.scale(CatStat.INTELLIGENCE, CatAttributeEffects.effectiveValue(cat, CatStat.INTELLIGENCE)); }
    public static double radius(Cat cat) { return CatMusicRules.radius(intelligence(cat)); }
    public static double strength(Cat cat) { return CatMusicRules.haste(ServerConfig.scale(CatStat.SPEED, CatAttributeEffects.effectiveValue(cat, CatStat.SPEED))); }
    public static boolean performing(Cat cat) {
        State state = STATES.get(cat);
        return state != null && state.performingUntil > now(cat) && cat.isAlive() && !cat.isRemoved()
                && CatClothesData.getOutfit(cat) == CatOutfitType.MUSIC && !CatPoseData.isPancake(cat)
                && (cat.level().isClientSide || !cat.isPassenger() && !cat.isOrderedToSit());
    }
    public static float visualRadius(Cat cat) { State state = STATES.get(cat); return performing(cat) ? state.radius : 0; }
    public static int pose(Cat cat) { State state = STATES.get(cat); return performing(cat) ? state.pose : -1; }
    public static float age(Cat cat, float partial) { State state = STATES.get(cat); return performing(cat) ? Math.max(0, now(cat) - state.started + partial) : 0; }
    public static boolean recipient(Cat cat) {
        CatOutfitType outfit = CatClothesData.getOutfit(cat);
        return cat.isAlive() && !cat.isRemoved() && !CatPoseData.isPancake(cat)
                && outfit != CatOutfitType.NONE && !outfit.isSupport() && !outfit.isPreviewOnly();
    }
    public static double bonus(Cat cat) {
        State state = STATES.get(cat);
        return recipient(cat) && state != null && state.bonusUntil > now(cat) ? state.bonus : 0;
    }
    public static boolean glowing(Cat cat) { return !cat.isInvisible() && bonus(cat) > 0; }
    public static int attackInterval(Cat cat, int original) { return CatMusicRules.interval(original, bonus(cat) + cn.laowu.mod.accessory.CatCommonAccessories.haste(cat)); }
    public static void perform(Cat cat) {
        if (cat.level().isClientSide) return;
        State state = state(cat); boolean start = !performing(cat);
        if (start) { state.started = now(cat); state.pose = cat.getRandom().nextBoolean() ? 0 : 1; }
        state.performingUntil = now(cat) + 30;
        state.radius = (float)radius(cat);
        sync(cat, state, start);
    }
    public static void stop(Cat cat) {
        State state = STATES.get(cat);
        if (state == null || state.performingUntil == 0) return;
        state.performingUntil = 0; state.pose = -1;
        if (!cat.level().isClientSide) sync(cat, state, true);
    }
    public static void offer(Cat patient, double amount) {
        if (patient.level().isClientSide || !recipient(patient) || !Double.isFinite(amount) || amount <= 0) return;
        long tick = now(patient); Offer previous = OFFERS.get(patient);
        OFFERS.put(patient, new Offer(tick, previous != null && previous.tick == tick ? Math.max(previous.bonus, amount) : amount));
    }
    public static void flush(ServerLevel level) {
        long tick = level.getGameTime();
        for (var iterator = OFFERS.entrySet().iterator(); iterator.hasNext();) {
            var entry = iterator.next(); Cat cat = entry.getKey();
            if (cat.level() != level) continue;
            Offer offer = entry.getValue(); iterator.remove();
            if (offer.tick != tick || !recipient(cat)) continue;
            State state = state(cat);
            boolean changed = state.bonusUntil <= tick || Math.abs(state.bonus - offer.bonus) > .000001;
            state.bonus = offer.bonus; state.bonusUntil = tick + CatMusicRules.BUFF_TICKS;
            sync(cat, state, changed);
        }
    }
    private static MusicSupportPacket packet(Cat cat, State state) {
        return new MusicSupportPacket(cat.getId(), cat.getUUID(), performing(cat) ? state.pose : -1,
                (int)Math.min(1000000, Math.max(0, now(cat) - state.started)), bonus(cat), state.radius);
    }
    private static void sync(Cat cat, State state, boolean force) {
        if (!force && now(cat) < state.nextSync) return;
        state.nextSync = now(cat) + 10;
        ModNetwork.musicSupport(cat, null, packet(cat, state));
    }
    public static void syncTo(Cat cat, ServerPlayer player) {
        State state = STATES.get(cat);
        if (state != null) ModNetwork.musicSupport(cat, player, packet(cat, state));
    }
    public static void receive(Cat cat, int pose, int age, double bonus, float radius) {
        if (!cat.level().isClientSide) return;
        State state = state(cat); long tick = now(cat);
        state.pose = pose >= 0 && pose <= 1 ? pose : -1;
        state.started = tick - Math.max(0, Math.min(1000000, age));
        state.performingUntil = state.pose >= 0 ? tick + 30 : 0;
        state.bonus = Double.isFinite(bonus) ? Math.max(0, bonus) : 0;
        state.bonusUntil = state.bonus > 0 ? tick + CatMusicRules.BUFF_TICKS : 0;
        state.radius = Float.isFinite(radius) ? Math.max(0, Math.min(24, radius)) : 0;
    }
    private CatMusicSupport() {}
}
