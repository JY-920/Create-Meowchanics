package cn.laowu.mod.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Species-stable attributes; ownership/team safety is enforced by the caller. */
public final class CreatureFilterRules {

    public static final int MAX_GROUPS = 8;
    public enum Attribute { BABY, ADULT, TAMED, UNTAMED, HEALTH, HEALTH_PERCENT;
        public String id() { return name().toLowerCase(Locale.ROOT); }
    }
    public enum Comparison { EQ, LT, LE, GT, GE;
        public String id() { return name().toLowerCase(Locale.ROOT); }
        public boolean test(double a,double b) { return switch(this) {
            case EQ -> a==b;case LT -> a<b;case LE -> a<=b;case GT -> a>b;case GE -> a>=b;}; }
    }
    public record AttributeCondition(Attribute attribute,Comparison comparison,double value,boolean inverted) {
        public AttributeCondition {
            if(attribute==null||comparison==null||!Double.isFinite(value)||value<0
                    ||(attribute==Attribute.HEALTH_PERCENT&&value>100)
                    ||(attribute!=Attribute.HEALTH&&attribute!=Attribute.HEALTH_PERCENT
                        &&(comparison!=Comparison.EQ||value!=1)))
                throw new IllegalArgumentException("Invalid creature attribute value");
        }
        boolean supports(Mob mob) {
            return switch(attribute) {
                case BABY,ADULT -> true;
                case TAMED,UNTAMED -> mob instanceof net.minecraft.world.entity.TamableAnimal
                        ||mob instanceof net.minecraft.world.entity.animal.horse.AbstractHorse;
                default -> true;
            };
        }
        boolean matches(Mob mob) {
            if(!supports(mob))return false;
            double actual=switch(attribute) {
                case BABY -> mob.isBaby()?1:0;case ADULT -> mob.isBaby()?0:1;
                case TAMED,UNTAMED -> {
                    boolean tame=mob instanceof net.minecraft.world.entity.TamableAnimal pet?pet.isTame()
                        :((net.minecraft.world.entity.animal.horse.AbstractHorse)mob).isTamed();
                    yield (attribute==Attribute.TAMED?tame:!tame)?1:0;
                }
                case HEALTH -> mob.getHealth();
                case HEALTH_PERCENT -> mob.getMaxHealth()>0?100d*mob.getHealth()/mob.getMaxHealth():Double.NaN;
            };
            return Double.isFinite(actual)&&(comparison.test(actual,value)!=inverted);
        }
    }
    public record Group(ResourceLocation target,Category category,Mode mode,List<AttributeCondition> conditions,boolean inverted) {
        public Group(ResourceLocation target,Category category,Mode mode,List<AttributeCondition> conditions) {
            this(target,category,mode,conditions,false);
        }
        public Group(ResourceLocation target,Mode mode,List<AttributeCondition> conditions) {
            this(target,null,mode,conditions);
        }
        public Group {
            if(target!=null&&category!=null||mode==null||conditions==null||conditions.size()>MAX_CONDITIONS
                    ||conditions.stream().anyMatch(Objects::isNull)
                    ||target!=null&&(target.toString().length()>MAX_ID_LENGTH||target.toString().equals("minecraft:player")))
                throw new IllegalArgumentException("Invalid creature group");
            conditions=List.copyOf(conditions);
        }
        boolean matches(Mob mob) {
            if(target!=null&&!target.equals(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType())))return false;
            if(category!=null&&category!=classify(mob))return false;
            if(conditions.isEmpty())return true;
            // ANY can use another supported branch; ALL and NONE still fail closed.
            if(mode!=Mode.WHITELIST&&conditions.stream().anyMatch(c->!c.supports(mob)))return false;
            return switch(mode) {
                case WHITELIST -> conditions.stream().anyMatch(c->c.matches(mob));
                case WHITELIST_ALL -> conditions.stream().allMatch(c->c.matches(mob));
                case BLACKLIST -> conditions.stream().noneMatch(c->c.matches(mob));
            };
        }
    }
    private List<Group> groups;
    private CreatureFilterRules retainedLegacy;
    private CreatureFilterRules(List<Group> groups,CreatureFilterRules legacy) {
        this(groups,legacy,Mode.WHITELIST);
    }
    private CreatureFilterRules(List<Group> groups,CreatureFilterRules legacy,Mode globalMode) {
        this(ALL_CATEGORIES,globalMode,List.of(),false);
        if(groups==null||groups.size()>MAX_GROUPS||groups.stream().anyMatch(Objects::isNull)
                ||groups.stream().mapToInt(g->g.conditions().size()).sum()+(legacy==null?0:legacy.conditions.size())>MAX_CONDITIONS
                ||legacy!=null&&legacy.isGrouped())throw new IllegalArgumentException("Invalid groups");
        this.groups=List.copyOf(groups);this.retainedLegacy=legacy;
    }
    public static CreatureFilterRules grouped(List<Group> groups) { return new CreatureFilterRules(groups,null); }
    public CreatureFilterRules withGroups(List<Group> groups) {
        return new CreatureFilterRules(groups,isGrouped()?retainedLegacy:isDefault()?null:this,isGrouped()?mode:Mode.WHITELIST);
    }
    public CreatureFilterRules withoutRetainedLegacy() { return new CreatureFilterRules(groups(),null,mode); }
    public CreatureFilterRules withEntryMode(Mode globalMode) {
        return new CreatureFilterRules(groups(),isGrouped()?retainedLegacy:isDefault()?null:this,globalMode);
    }
    public boolean isGrouped() { return groups!=null; }
    public List<Group> groups() { return groups==null?List.of():groups; }
    public CreatureFilterRules retainedLegacy() { return retainedLegacy; }
    public int conditionCount() { return isGrouped()?groups.stream().mapToInt(g->g.conditions().size()).sum()
        +(retainedLegacy==null?0:retainedLegacy.conditions.size()):conditions.size(); }
    private CompoundTag saveGroups() {
        var tag=new CompoundTag();tag.putInt("Version",5);tag.putString("Mode",mode.id());var list=new ListTag();
        for(var group:groups) {
            var g=new CompoundTag();g.putString("Target",group.target()==null?"*":group.target().toString());
            if(group.category()!=null)g.putString("Category",group.category().id());
            g.putBoolean("Inverted",group.inverted());g.putString("Mode",group.mode().id());var cs=new ListTag();
            for(var condition:group.conditions()) {
                var c=new CompoundTag();c.putString("Attribute",condition.attribute().id());
                c.putString("Comparison",condition.comparison().id());c.putDouble("Value",condition.value());
                c.putBoolean("Inverted",condition.inverted());cs.add(c);
            }
            g.put("Conditions",cs);list.add(g);
        }
        tag.put("Groups",list);if(retainedLegacy!=null)tag.put("Legacy",retainedLegacy.save());return tag;
    }
    private static CreatureFilterRules parseGroups(CompoundTag tag) {
        var list=boundedList(tag,"Groups",Tag.TAG_COMPOUND);
        if(list.size()>MAX_GROUPS)throw new IllegalArgumentException("Too many groups");
        var result=new ArrayList<Group>();int count=0;
        for(int i=0;i<list.size();i++) {
            var g=list.getCompound(i);
            boolean categorized=tag.getInt("Version")>=4&&g.contains("Category",Tag.TAG_STRING);
            if(!g.contains("Target",Tag.TAG_STRING)||!g.contains("Mode",Tag.TAG_STRING)||g.getAllKeys().size()!=(categorized?4:3)+(tag.getInt("Version")==5?1:0))
                throw new IllegalArgumentException("Invalid group");
            if(tag.getInt("Version")==5&&(!g.contains("Inverted",Tag.TAG_BYTE)||g.getByte("Inverted")<0||g.getByte("Inverted")>1))
                throw new IllegalArgumentException("Invalid entry inversion");
            var cs=boundedList(g,"Conditions",Tag.TAG_COMPOUND);count+=cs.size();
            if(count>MAX_CONDITIONS)throw new IllegalArgumentException("Too many attributes");
            var attrs=new ArrayList<AttributeCondition>();
            for(int j=0;j<cs.size();j++) {
                var c=cs.getCompound(j);
                if(c.getAllKeys().size()!=4||!c.contains("Attribute",Tag.TAG_STRING)||!c.contains("Comparison",Tag.TAG_STRING)
                        ||!c.contains("Value",Tag.TAG_DOUBLE)||!c.contains("Inverted",Tag.TAG_BYTE)
                        ||c.getByte("Inverted")<0||c.getByte("Inverted")>1)throw new IllegalArgumentException("Invalid attribute");
                attrs.add(new AttributeCondition(Attribute.valueOf(c.getString("Attribute").toUpperCase(Locale.ROOT)),
                        Comparison.valueOf(c.getString("Comparison").toUpperCase(Locale.ROOT)),c.getDouble("Value"),c.getBoolean("Inverted")));
            }
            result.add(new Group(g.getString("Target").equals("*")?null:parseId(g.getString("Target")),
                categorized?Category.valueOf(g.getString("Category").toUpperCase(Locale.ROOT)):null,
                Mode.valueOf(g.getString("Mode").toUpperCase(Locale.ROOT)),attrs,tag.getInt("Version")==5&&g.getBoolean("Inverted")));
        }
        CreatureFilterRules legacy=null;
        if(tag.contains("Legacy")) {
            if(!tag.contains("Legacy",Tag.TAG_COMPOUND)||tag.getCompound("Legacy").getInt("Version")>=3)
                throw new IllegalArgumentException("Invalid legacy branch");
            legacy=parseData(tag.getCompound("Legacy"));
        }
        if(tag.getAllKeys().size()!=(legacy==null?2:3)+(tag.getInt("Version")==5?1:0))throw new IllegalArgumentException("Invalid grouped payload");
        return new CreatureFilterRules(result,legacy,tag.getInt("Version")==5?Mode.valueOf(tag.getString("Mode").toUpperCase(Locale.ROOT)):Mode.WHITELIST);
    }

    public static final int ALL_CATEGORIES = 7;
    public static final int MAX_CONDITIONS = 64;
    public static final int MAX_ENTITY_IDS = MAX_CONDITIONS;
    public static final int MAX_ID_LENGTH = 128;
    public static final String TAG = "LaoWuCreatureFilter";
    public static final CreatureFilterRules DEFAULT = new CreatureFilterRules(ALL_CATEGORIES, Mode.WHITELIST, List.of(), false);
    public static final CreatureFilterRules NONE = DEFAULT;
    private static final Set<String> NEUTRAL_EXCEPTIONS = Set.of("minecraft:piglin", "minecraft:panda",
            "minecraft:llama", "minecraft:trader_llama", "minecraft:dolphin", "minecraft:goat");
    public enum Category {
        PASSIVE(1), NEUTRAL(2), HOSTILE(4);
        private final int bit;
        Category(int bit) { this.bit = bit; }
        public int bit() { return bit; }
        public String id() { return name().toLowerCase(Locale.ROOT); }
    }
    public enum Mode {
        WHITELIST, WHITELIST_ALL, BLACKLIST;
        public String id() { return name().toLowerCase(Locale.ROOT); }
    }
    /** Exactly one attribute, optionally negated before mode aggregation. */
    public record Condition(Category category, ResourceLocation entityId, boolean inverted) {
        public Condition {
            if ((category == null) == (entityId == null))
                throw new IllegalArgumentException("Expected exactly one creature attribute");
            if (entityId != null && (entityId.toString().length() > MAX_ID_LENGTH || entityId.toString().equals("minecraft:player")))
                throw new IllegalArgumentException("Invalid creature entity ID");
        }
        public static Condition category(Category category, boolean inverted) { return new Condition(category, null, inverted); }
        public static Condition entity(ResourceLocation id, boolean inverted) { return new Condition(null, id, inverted); }
    }
    // Compatibility gate for v1 category AND species rules, not a native attribute.
    private final int categoryMask;
    private final Mode mode;
    private final List<Condition> conditions;
    private final boolean legacyEmpty;
    private CreatureFilterRules(int mask, Mode mode, Collection<Condition> conditions, boolean legacyEmpty) {
        if (mask < 0 || (mask & ~ALL_CATEGORIES) != 0 || mode == null || conditions == null
                || conditions.size() > MAX_CONDITIONS || conditions.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Invalid creature filter settings");
        this.categoryMask = mask; this.mode = mode;
        this.conditions = List.copyOf(new LinkedHashSet<>(conditions));
        this.legacyEmpty = legacyEmpty && this.conditions.isEmpty() && mode == Mode.BLACKLIST;
    }
    /** Compatibility factory retaining category gate AND species semantics. */
    public static CreatureFilterRules of(int mask, Mode mode, Collection<ResourceLocation> ids) {
        if (ids == null || ids.size() > MAX_ENTITY_IDS) throw new IllegalArgumentException("Invalid creature IDs");
        return new CreatureFilterRules(mask, mode, ids.stream().map(id -> Condition.entity(id, false)).toList(), true);
    }
    public static CreatureFilterRules conditions(Mode mode, List<Condition> conditions) {
        return new CreatureFilterRules(ALL_CATEGORIES, mode, conditions, false);
    }
    public CreatureFilterRules withConditions(Mode mode, List<Condition> conditions) {
        return new CreatureFilterRules(categoryMask, mode, conditions, false);
    }
    public int categoryMask() { return categoryMask; }
    public Mode mode() { return mode; }
    public List<Condition> conditions() { return conditions; }
    public List<ResourceLocation> entityIds() {
        return conditions.stream().map(Condition::entityId).filter(Objects::nonNull).distinct().toList();
    }
    public boolean enabled(Category category) { return (categoryMask & category.bit()) != 0; }
    public boolean isLegacy() { return categoryMask != ALL_CATEGORIES || legacyEmpty; }
    public boolean isDefault() { return !isGrouped() && !isLegacy() && mode == Mode.WHITELIST && conditions.isEmpty(); }
    public static Category classify(Mob mob) {
        if (mob instanceof NeutralMob || NEUTRAL_EXCEPTIONS.contains(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString()))
            return Category.NEUTRAL;
        return mob instanceof Enemy || mob.getType().getCategory() == MobCategory.MONSTER ? Category.HOSTILE : Category.PASSIVE;
    }
    public boolean matches(Mob mob) {
        if (mob == null) return false;
        if(isGrouped()) {
            boolean result=!groups.isEmpty()&&switch(mode) {
                case WHITELIST -> groups.stream().anyMatch(g->g.matches(mob)!=g.inverted());
                case WHITELIST_ALL -> groups.stream().allMatch(g->g.matches(mob)!=g.inverted());
                case BLACKLIST -> groups.stream().noneMatch(g->g.matches(mob)!=g.inverted());
            };
            return result||retainedLegacy!=null&&retainedLegacy.matches(mob);
        }
        Category category = classify(mob);
        if (!enabled(category)) return false;
        // Create empty attribute filters fall back to direct item matching, which cannot match a creature.
        if (conditions.isEmpty()) return legacyEmpty;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        for (Condition condition : conditions) {
            boolean matches = (condition.category() != null ? condition.category() == category : condition.entityId().equals(id)) != condition.inverted();
            if (matches && mode == Mode.WHITELIST) return true;
            if (matches && mode == Mode.BLACKLIST) return false;
            if (!matches && mode == Mode.WHITELIST_ALL) return false;
        }
        return mode != Mode.WHITELIST;
    }
    public CompoundTag save() {
        if(isGrouped())return saveGroups();
        var tag = new CompoundTag(); tag.putInt("Version", 2);
        tag.putInt("Categories", categoryMask); tag.putString("Mode", mode.id());
        var list = new ListTag();
        for (Condition condition : conditions) {
            var entry = new CompoundTag();
            if (condition.category() != null) entry.putString("Category", condition.category().id());
            else entry.putString("EntityId", condition.entityId().toString());
            entry.putBoolean("Inverted", condition.inverted()); list.add(entry);
        }
        tag.put("Conditions", list);
        if (legacyEmpty) tag.putBoolean("LegacyEmpty", true);
        return tag;
    }
    public void write(ItemStack stack) { stack.getOrCreateTag().put(TAG, save()); }
    public static CreatureFilterRules read(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return DEFAULT;
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(TAG)) return DEFAULT;
        return root.contains(TAG, Tag.TAG_COMPOUND) ? readData(root.getCompound(TAG)) : NONE;
    }
    public static CreatureFilterRules readData(CompoundTag tag) {
        try { return parseData(tag); } catch (IllegalArgumentException invalid) { return NONE; }
    }
    /** Strict network entry point; saved data uses the fail-closed readData wrapper. */
    public static CreatureFilterRules parseData(CompoundTag tag) {
        if(tag!=null&&tag.contains("Version",Tag.TAG_INT)&&(tag.getInt("Version")==3||tag.getInt("Version")==4||tag.getInt("Version")==5))return parseGroups(tag);
        if (tag == null || !tag.contains("Version", Tag.TAG_INT) || !tag.contains("Categories", Tag.TAG_INT)
                || !tag.contains("Mode", Tag.TAG_STRING)) throw new IllegalArgumentException("Invalid creature payload");
        int version = tag.getInt("Version");
        Mode mode = switch (tag.getString("Mode")) {
            case "whitelist" -> Mode.WHITELIST;
            case "whitelist_all" -> Mode.WHITELIST_ALL;
            case "blacklist" -> Mode.BLACKLIST;
            default -> throw new IllegalArgumentException("Invalid mode");
        };
        if (version == 1) {
            if (mode == Mode.WHITELIST_ALL) throw new IllegalArgumentException("Invalid legacy mode");
            ListTag ids = boundedList(tag, "EntityIds", Tag.TAG_STRING);
            var parsed = new ArrayList<ResourceLocation>();
            for (int i = 0; i < ids.size(); i++) parsed.add(parseId(ids.getString(i)));
            return of(tag.getInt("Categories"), mode, parsed);
        }
        if (version != 2) throw new IllegalArgumentException("Unsupported creature filter version");
        ListTag entries = boundedList(tag, "Conditions", Tag.TAG_COMPOUND);
        var conditions = new ArrayList<Condition>();
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            if (!entry.contains("Inverted", Tag.TAG_BYTE) || entry.getByte("Inverted") < 0 || entry.getByte("Inverted") > 1
                    || entry.contains("Category") == entry.contains("EntityId") || entry.getAllKeys().size() != 2)
                throw new IllegalArgumentException("Invalid creature condition");
            boolean inverted = entry.getBoolean("Inverted");
            if (entry.contains("Category", Tag.TAG_STRING)) {
                Category category = switch (entry.getString("Category")) {
                    case "passive" -> Category.PASSIVE;
                    case "neutral" -> Category.NEUTRAL;
                    case "hostile" -> Category.HOSTILE;
                    default -> throw new IllegalArgumentException("Invalid category");
                };
                conditions.add(Condition.category(category, inverted));
            } else if (entry.contains("EntityId", Tag.TAG_STRING)) {
                conditions.add(Condition.entity(parseId(entry.getString("EntityId")), inverted));
            } else throw new IllegalArgumentException("Invalid creature attribute");
        }
        boolean legacy = false;
        if (tag.contains("LegacyEmpty")) {
            if (!tag.contains("LegacyEmpty", Tag.TAG_BYTE) || tag.getByte("LegacyEmpty") != 1 || !conditions.isEmpty() || mode != Mode.BLACKLIST)
                throw new IllegalArgumentException("Invalid legacy empty flag");
            legacy = true;
        }
        return new CreatureFilterRules(tag.getInt("Categories"), mode, conditions, legacy);
    }
    private static ListTag boundedList(CompoundTag tag, String key, int elementType) {
        if (!(tag.get(key) instanceof ListTag list) || list.size() > MAX_CONDITIONS || !list.isEmpty() && list.getElementType() != elementType)
            throw new IllegalArgumentException("Invalid creature list");
        return list;
    }
    private static ResourceLocation parseId(String id) {
        ResourceLocation parsed = id.length() <= MAX_ID_LENGTH ? ResourceLocation.tryParse(id) : null;
        if (parsed == null) throw new IllegalArgumentException("Invalid creature ID");
        return parsed;
    }
}
