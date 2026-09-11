package cn.laowu.mod;

import cn.laowu.mod.genetics.CatStat;
import net.minecraft.nbt.CompoundTag;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/** Immutable suit values; previews and combat share one interval/bonus calculator. */
public record CatSuitSettings(CatOutfitType outfit, Map<CatSuitSetting, Double> values) {
    public CatSuitSettings {
        var copy = new EnumMap<CatSuitSetting, Double>(CatSuitSetting.class);
        for (CatSuitSetting setting : CatSuitSetting.values()) {
            Double value = values.get(setting);
            if (value == null || !setting.valid(value)) throw new IllegalArgumentException(setting.id());
            copy.put(setting, value);
        }
        values = Collections.unmodifiableMap(copy);
    }
    public static CatSuitSettings read(CatOutfitType outfit, ToDoubleFunction<CatSuitSetting> source) {
        var values = new EnumMap<CatSuitSetting, Double>(CatSuitSetting.class);
        for (CatSuitSetting setting : CatSuitSetting.values()) values.put(setting, source.applyAsDouble(setting));
        return new CatSuitSettings(outfit, values);
    }
    public static CatSuitSettings current(CatOutfitType outfit) {
        return read(outfit, setting -> ServerConfig.careerSetting(outfit, setting));
    }
    public static CatSuitSettings draft(CatOutfitType outfit, CompoundTag draft) {
        return read(outfit, setting -> setting.read(draft, outfit));
    }
    public double value(CatSuitSetting setting) { return values.get(setting); }
    public CatSuitSettings with(CatSuitSetting setting, double value) {
        var updated = new EnumMap<>(values);
        updated.put(setting, value);
        return new CatSuitSettings(outfit, updated);
    }
    public int attribute(int base, CatStat stat) {
        return (int) Math.max(0L, Math.min(999L, (long) base + (int) value(CatSuitSetting.forStat(stat))));
    }
    public int intervalTicks(double scaledSpeed) {
        double interval = value(CatSuitSetting.INTERVAL_BASE)
                - value(CatSuitSetting.INTERVAL_PER_SPEED) * Math.max(0D, scaledSpeed);
        return (int) Math.max(value(CatSuitSetting.MIN_INTERVAL),
                Math.min(ServerConfig.MAX_MULTIPLIER, Math.round(interval)));
    }
}
