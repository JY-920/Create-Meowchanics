package cn.laowu.mod.client;

import cn.laowu.mod.ClientConfig;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class HeldItemTransformState {
    public enum Target {
        HELD,
        GUI
    }

    private static final Values DEFAULTS = new Values(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D);
    private static final Map<ResourceLocation, Values> HELD_DEFAULTS = Map.of(
            LaoWuMod.id("cat_ball"),
            new Values(0.0D, 127.0D, 0.0D, -0.06D, 1.13D, 0.0D, 1.0D),
            LaoWuMod.id("devouring_cat"),
            new Values(0.0D, 64.0D, 0.0D, 0.0D, 0.31D, 0.0D, 1.0D),
            LaoWuMod.id("cat_scanner"),
            new Values(-172.0D, -37.0D, 87.0D, -0.55D, -0.08D, -0.23D, 1.78D)
    );
    private static final Map<ResourceLocation, Values> GUI_DEFAULTS = Map.of(
            LaoWuMod.id("cat_ball"),
            new Values(8.0D, 118.0D, 0.0D, -0.41D, 1.52D, 0.0D, 1.30D),
            LaoWuMod.id("cat_cannon"),
            new Values(0.0D, 0.0D, 0.0D, -0.29D, 0.0D, 0.0D, 1.0D),
            LaoWuMod.id("devouring_cat"),
            new Values(0.0D, 0.0D, 0.0D, 0.0D, 0.14D, 0.0D, 1.0D)
    );
    private static ResourceLocation previewItem;
    private static Target previewTarget;
    private static Values preview;

    public static Values current(ItemStack stack, Target target) {
        ResourceLocation itemId = itemId(stack);
        if (preview != null && target == previewTarget && itemId != null && itemId.equals(previewItem)) {
            return preview;
        }
        return configured(itemId, target);
    }

    public static Values configured(ItemStack stack, Target target) {
        return configured(itemId(stack), target);
    }

    private static Values configured(ResourceLocation itemId, Target target) {
        if (itemId == null) return DEFAULTS;
        String prefix = itemId + "|";
        for (String entry : config(target).get()) {
            if (!entry.startsWith(prefix)) continue;
            String[] parts = entry.split("\\|", -1);
            if (parts.length != 8) continue;
            try {
                return new Values(
                        Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3]), Double.parseDouble(parts[4]),
                        Double.parseDouble(parts[5]), Double.parseDouble(parts[6]),
                        Double.parseDouble(parts[7]));
            } catch (NumberFormatException ignored) {
                return defaults(itemId, target);
            }
        }
        return defaults(itemId, target);
    }

    public static Values defaults(ItemStack stack, Target target) {
        return defaults(itemId(stack), target);
    }

    private static Values defaults(ResourceLocation itemId, Target target) {
        if (itemId == null) return DEFAULTS;
        return (target == Target.GUI ? GUI_DEFAULTS : HELD_DEFAULTS)
                .getOrDefault(itemId, DEFAULTS);
    }

    public static void preview(ItemStack stack, Target target, Values values) {
        previewItem = itemId(stack);
        previewTarget = target;
        preview = values;
    }

    public static void cancelPreview() {
        previewItem = null;
        previewTarget = null;
        preview = null;
    }

    public static void save(ItemStack stack, Target target, Values values) {
        ResourceLocation itemId = itemId(stack);
        if (itemId == null) return;

        String prefix = itemId + "|";
        List<String> entries = new ArrayList<>();
        ModConfigSpec.ConfigValue<List<? extends String>> config = config(target);
        for (String entry : config.get()) {
            if (!entry.startsWith(prefix)) entries.add(entry);
        }
        if (!values.equals(defaults(itemId, target))) {
            entries.add(String.format(Locale.ROOT, "%s|%.2f|%.2f|%.2f|%.2f|%.2f|%.2f|%.2f",
                    itemId, values.rotationX(), values.rotationY(), values.rotationZ(),
                    values.offsetX(), values.offsetY(), values.offsetZ(), values.scale()));
        }
        config.set(entries);
        ClientConfig.SPEC.save();
        cancelPreview();
    }

    private static ModConfigSpec.ConfigValue<List<? extends String>> config(Target target) {
        return target == Target.GUI
                ? ClientConfig.GUI_ITEM_TRANSFORMS
                : ClientConfig.HELD_ITEM_TRANSFORMS;
    }

    private static ResourceLocation itemId(ItemStack stack) {
        return stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    public record Values(double rotationX, double rotationY, double rotationZ,
                         double offsetX, double offsetY, double offsetZ, double scale) { }

    private HeldItemTransformState() {}
}
