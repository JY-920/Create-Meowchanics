package cn.laowu.mod.genetics;

import cn.laowu.mod.network.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Cat;
import java.util.Set;

/** Per-cat/per-trait counters, cooldowns and effective bonuses; never genetic attributes. */
public final class CatTraitScriptState {
    public static final String TAG = "LaoWuTraitScriptState";
    public static final String CLIENT_TAG = "LaoWuClientTraitBonuses";
    public static CompoundTag read(Cat cat, String id) { return cat.getPersistentData().getCompound(TAG).getCompound(id).copy(); }
    public static void write(Cat cat, String id, CompoundTag value) {
        var root = cat.getPersistentData().getCompound(TAG);
        root.put(id, value.copy()); cat.getPersistentData().put(TAG, root);
    }
    public static void prune(Cat cat, CatTraitProfile profile) {
        var root = cat.getPersistentData().getCompound(TAG);
        var retained = profile.traits().stream().map(t -> t.trait().id().toString()).collect(java.util.stream.Collectors.toSet());
        for (String id : Set.copyOf(root.getAllKeys())) if (!retained.contains(id)) root.remove(id);
        if (root.isEmpty()) cat.getPersistentData().remove(TAG); else cat.getPersistentData().put(TAG, root);
    }
    public static int bonus(Cat cat, CatStat stat) {
        if (cat.level().isClientSide) return cat.getPersistentData().getCompound(CLIENT_TAG).getInt(stat.serializedName());
        int result = 0;
        var states = cat.getPersistentData().getCompound(TAG);
        for (var entry : CatTraitData.read(cat).orElse(CatTraitProfile.EMPTY).traits())
            if (entry.trait().enabled())
                result += Math.max(-999999, Math.min(999999,
                        states.getCompound(entry.trait().id().toString()).getCompound("StatBonuses").getInt(stat.serializedName())));
        return result;
    }
    public static CompoundTag networkData(Cat cat) {
        var tag = new CompoundTag();
        for (var stat : CatStat.values()) {
            int amount = bonus(cat, stat);
            if (amount != 0) tag.putInt(stat.serializedName(), amount);
        }
        return tag;
    }
    public static void refresh(Cat cat) {
        CatAttributeData.read(cat).ifPresent(attributes -> CatAttributeEffects.refresh(cat, attributes, CatTraitData.ensure(cat)));
        ModNetwork.syncCatTraitsToTracking(cat);
    }
    private CatTraitScriptState() {}
}
