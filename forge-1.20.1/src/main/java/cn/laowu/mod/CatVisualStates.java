package cn.laowu.mod;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Supplier;

/** Transient state keyed by world AND entity identity: Entity.equals only compares runtime IDs. */
public final class CatVisualStates<T> {
    private record Entry<T>(WeakReference<Entity> entity, T value) {}
    private static final class Bucket<T> {
        // World entities and UI previews may intentionally share UUID and runtime ID.
        final Map<UUID, List<Entry<T>>> entries = new HashMap<>();
        long nextSweep;
    }
    private final Map<Level, Bucket<T>> worlds = new WeakHashMap<>();
    public synchronized T get(Entity entity) {
        var bucket = worlds.get(entity.level());
        var entries = bucket == null ? null : bucket.entries.get(entity.getUUID());
        if (entries != null) for (var entry : entries)
            if (entry.entity.get() == entity) return entry.value;
        return null;
    }
    public synchronized T getOrCreate(Entity entity, Supplier<T> factory) {
        var bucket = worlds.computeIfAbsent(entity.level(), ignored -> new Bucket<>());
        if (entity.level().getGameTime() >= bucket.nextSweep) {
            bucket.nextSweep = entity.level().getGameTime() + 200;
            bucket.entries.entrySet().removeIf(item -> {
                item.getValue().removeIf(entry -> {
                    Entity e = entry.entity.get();
                    return e == null || e.isRemoved();
                });
                return item.getValue().isEmpty();
            });
        }
        var entries = bucket.entries.computeIfAbsent(entity.getUUID(), ignored -> new ArrayList<>());
        for (var entry : entries) if (entry.entity.get() == entity) return entry.value;
        T value = factory.get();
        entries.add(new Entry<>(new WeakReference<>(entity), value));
        return value;
    }
}
