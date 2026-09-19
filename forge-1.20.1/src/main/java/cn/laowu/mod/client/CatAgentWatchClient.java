package cn.laowu.mod.client;

import cn.laowu.mod.CatAgentWatch;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import java.util.*;

/** An expiring client render overlay, not an entity flag, potion or scoreboard team mutation. */
public final class CatAgentWatchClient {
    public static final int RED = 0xFF3030;
    private record Mark(int entityId, long until) {}
    private static final Map<Level, Map<UUID, Mark>> MARKS = new WeakHashMap<>();
    public static void receive(Level level, int entityId, UUID uuid, int duration) {
        if (!level.isClientSide || duration < 0 || duration > CatAgentWatch.VISUAL_TICKS) return;
        var marks = MARKS.computeIfAbsent(level, ignored -> new HashMap<>());
        long now = level.getGameTime();
        marks.entrySet().removeIf(e -> e.getValue().until <= now);
        if (duration == 0) marks.remove(uuid);
        else marks.put(uuid, new Mark(entityId, now + duration));
        // Cache by both UUID and entity ID: packets may precede the spawn/tracking packet.
    }
    public static boolean visible(Entity entity) {
        if (!(entity instanceof LivingEntity) || !entity.level().isClientSide || !entity.isAlive() || entity.isRemoved())
            return false;
        var marks = MARKS.get(entity.level());
        if (marks == null) return false;
        Mark mark = marks.get(entity.getUUID());
        if (mark == null) return false;
        if (mark.until <= entity.level().getGameTime()) { marks.remove(entity.getUUID()); return false; }
        return mark.entityId == entity.getId();
    }
    private CatAgentWatchClient() {}
}
