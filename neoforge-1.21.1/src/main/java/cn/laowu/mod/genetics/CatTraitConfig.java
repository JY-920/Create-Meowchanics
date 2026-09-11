package cn.laowu.mod.genetics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Generation policy only: never used to discard saved traits or unregister IDs. */
public final class CatTraitConfig {
    public static final String KEY = "disabled_traits";
    public static final int MAX_IDS = 1024;

    public static boolean inherits(Object value) {
        return value instanceof List<?> list && list.size() == 1
                && list.get(0) instanceof Number n && n.doubleValue() == -1.0D;
    }

    public static boolean validId(Object value) {
        return value instanceof String id && id.length() <= 128
                && id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+");
    }

    public static boolean validList(Object value, boolean allowInheritance) {
        if (allowInheritance && inherits(value)) return true;
        return value instanceof List<?> list && list.size() <= MAX_IDS
                && list.stream().allMatch(CatTraitConfig::validId);
    }

    public static Set<String> ids(Collection<?> values) {
        TreeSet<String> result = new TreeSet<>();
        for (Object value : values) if (validId(value)) result.add((String) value);
        return Collections.unmodifiableSet(result);
    }

    public static void write(CompoundTag tag, Collection<String> ids) {
        ListTag list = new ListTag();
        for (String id : new TreeSet<>(ids)) list.add(StringTag.valueOf(id));
        tag.put(KEY, list);
    }

    public static boolean validTag(CompoundTag tag) {
        // getList alone treats a wrong element type as an empty list.
        if (!(tag.get(KEY) instanceof ListTag list) || list.size() > MAX_IDS
                || (!list.isEmpty() && list.getElementType() != Tag.TAG_STRING)) return false;
        for (int i = 0; i < list.size(); i++) if (!validId(list.getString(i))) return false;
        return true;
    }

    public static Set<String> read(CompoundTag tag) {
        ListTag list = tag.getList(KEY, Tag.TAG_STRING);
        TreeSet<String> result = new TreeSet<>();
        for (int i = 0; i < list.size(); i++) result.add(list.getString(i));
        return Collections.unmodifiableSet(result);
    }

    /** Dedicated-server-safe catalog: read the bundled names, not client language state. */
    public static String[] comments(boolean global) {
        List<String> lines = new ArrayList<>();
        if (global) {
            lines.add("词条禁用列表：[-1]=使用存档；[]=强制不禁用；填写编号数组=全局强制禁用并锁定存档界面。");
            lines.add("Global trait blacklist: [-1] = per-world; [] = force none; ID array = force this list.");
        } else {
            lines.add("本存档禁用新出现的词条；默认[]。游戏内按V -> 世界设置 -> 词条禁用，可搜索和勾选。");
            lines.add("Per-world trait blacklist. Empty [] allows every trait.");
        }
        lines.add("示例：disabled_traits = [\"laowu:night_owl\", \"laowu:sky_cat\"]");
        lines.add("仅限制新生成、注液、繁育继承/突变和调整棒新增；已有猫咪/猫饼的词条、等级、效果和存档数据全部保留。");
        lines.add("禁用面团团后，注液猫也不再强制获得它。未知但格式正确的编号会保留并忽略，方便版本兼容。");
        lines.add("编号固定，不使用会随词条增删而变化的序号。以下为当前完整编号列表：");
        JsonObject names = NamesHolder.NAMES;
        for (CatTrait trait : CatTrait.values()) {
            String key = "trait.laowu." + trait.serializedName() + ".title";
            lines.add(trait.id() + " = " + (names.has(key) ? names.get(key).getAsString() : trait.serializedName()));
        }
        return lines.toArray(String[]::new);
    }

    private static final class NamesHolder {
        private static final JsonObject NAMES = load();
        private static JsonObject load() {
            try (var stream = CatTraitConfig.class.getResourceAsStream("/assets/laowu/lang/zh_cn.json")) {
                if (stream != null) return JsonParser.parseReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (java.io.IOException | RuntimeException ignored) {
                // A missing translation must never prevent a server from loading its world.
            }
            return new JsonObject();
        }
    }

    private CatTraitConfig() {}
}
