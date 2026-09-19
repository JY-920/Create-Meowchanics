package cn.laowu.mod;

import cn.laowu.mod.network.AgentMeleePacket;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.Level;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.WeakHashMap;

/** Server-selected cosmetic strikes. A flurry is one ordinary attack, never extra damage ticks. */
public final class CatAgentMeleeMotion {
    public record Strike(int move, int duration, long started) {}
    private static final Map<Cat, Strike> SERVER = new WeakHashMap<>();
    private static final Map<Level, Map<UUID, Strike>> CLIENT = new WeakHashMap<>();
    public static Strike current(Cat cat) {
        var clients = CLIENT.get(cat.level());
        Strike strike = cat.level().isClientSide ? (clients == null ? null : clients.get(cat.getUUID())) : SERVER.get(cat);
        if (strike != null && (!cat.isAlive() || CatClothesData.getOutfit(cat) != CatOutfitType.AGENT
                || cat.isPassenger() || CatPoseData.isPancake(cat)
                || !cat.level().isClientSide && CareerCatBehavior.isCombatResting(cat)
                || cat.level().getGameTime() >= strike.started + strike.duration)) {
            if (cat.level().isClientSide) clients.remove(cat.getUUID()); else SERVER.remove(cat);
            return null;
        }
        return strike;
    }
    public static void begin(Cat cat) {
        if (cat.level().isClientSide || CatClothesData.getOutfit(cat) != CatOutfitType.AGENT) return;
        // Fast attack intervals must not continually restart a pose before its visible strike.
        if (current(cat) != null) return;
        int move = cat.getPersistentData().getInt("LaoWuAgentNextPose");
        cat.getPersistentData().putInt("LaoWuAgentNextPose", (move + 1) % 3);
        SERVER.put(cat, new Strike(Math.floorMod(move, 3),
                Math.max(12, Math.min(18, CareerCatBehavior.careerAttackIntervalTicks(cat) - 1)), cat.level().getGameTime()));
        syncTo(cat, null);
    }
    public static void receive(Cat cat, int move, int duration, int age) {
        if (!cat.level().isClientSide || move < 0 || move > 2 || duration < 6 || duration > 18
                || age < 0 || age >= duration) return;
        var clients = CLIENT.computeIfAbsent(cat.level(), ignored -> new HashMap<>());
        long now = cat.level().getGameTime();
        clients.entrySet().removeIf(e -> now >= e.getValue().started + e.getValue().duration);
        clients.put(cat.getUUID(), new Strike(move, duration, now - age));
    }
    public static void syncTo(Cat cat, ServerPlayer player) {
        Strike strike = current(cat);
        if (strike != null) ModNetwork.agentMelee(cat, player, new AgentMeleePacket(cat.getId(), cat.getUUID(),
                strike.move, strike.duration, (int)(cat.level().getGameTime() - strike.started)));
    }
    private CatAgentMeleeMotion() {}
}
