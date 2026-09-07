package cn.laowu.mod.genetics;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/** Stable trait definitions. Saved cats refer to these by resource location. */
public enum CatTrait {
    THORNS("thorns", CatTraitRarity.EXCELLENT, true),
    NIGHT_OWL("night_owl", CatTraitRarity.COMMON, true),
    HEAT_RESISTANCE("heat_resistance", CatTraitRarity.GOOD, false),
    DOUGHY("doughy", CatTraitRarity.DEFECT, false),
    LONG_FUR("long_fur", CatTraitRarity.GOOD, true),
    BRISTLING_RAGE("bristling_rage", CatTraitRarity.GOOD, true),
    HEALING_PURR("healing_purr", CatTraitRarity.EXCELLENT, true),
    LU_BU_REBORN("lu_bu_reborn", CatTraitRarity.EXCELLENT, false,
            CatTraitSlot.HOSTILE_PRESSURE),
    BEEBEE_GENE("beebee_gene", CatTraitRarity.GOOD, true),
    PROSPEROUS_LITTER("prosperous_litter", CatTraitRarity.EXCELLENT, true),
    ANGLERS_FORTUNE("anglers_fortune", CatTraitRarity.GOOD, true),
    SUPERHEAT_GENE("superheat_gene", CatTraitRarity.EXCELLENT, false,
            CatTraitSlot.FIRE_CAREER),
    PROTECTIVE_INSTINCT("protective_instinct", CatTraitRarity.GOOD, true),
    WET_FURY("wet_fury", CatTraitRarity.GOOD, true,
            CatTraitSlot.WET_CONDITION),
    CHONKY_PRESENCE("chonky_presence", CatTraitRarity.GOOD, true),
    GLASS_CLAWS("glass_claws", CatTraitRarity.GOOD, true),
    TAIL_HELD_HIGH("tail_held_high", CatTraitRarity.COMMON, true),
    LOAF_THOUGHTS("loaf_thoughts", CatTraitRarity.COMMON, true),
    NINE_LIVES("nine_lives", CatTraitRarity.EXCELLENT, true,
            CatTraitSlot.DAMAGE_AVOIDANCE),
    WATER_SHY("water_shy", CatTraitRarity.DEFECT, false,
            CatTraitSlot.WET_CONDITION),
    DAY_DROWSY("day_drowsy", CatTraitRarity.DEFECT, false),
    TIMID("timid", CatTraitRarity.DEFECT, false,
            CatTraitSlot.HOSTILE_PRESSURE),
    ATTENTION_MAGNET("attention_magnet", CatTraitRarity.GOOD, true),
    CAIN_MARK("cain_mark", CatTraitRarity.EXCELLENT, true,
            CatTraitSlot.DAMAGE_AVOIDANCE),
    ENERGY_RECOVERY("energy_recovery", CatTraitRarity.EXCELLENT, false),

    // Appearance traits. They all occupy the same exclusive channel so a cat
    // cannot combine silhouettes or animations that would fight each other.
    LOLI("loli", CatTraitRarity.GOOD, false, CatTraitSlot.APPEARANCE),
    HIM("him", CatTraitRarity.GOOD, false, CatTraitSlot.APPEARANCE),
    ISAAC("isaac", CatTraitRarity.COMMON, false, CatTraitSlot.APPEARANCE),
    ROUND_HEAD("round_head", CatTraitRarity.COMMON, false, CatTraitSlot.APPEARANCE),
    OIIAI("oiiai", CatTraitRarity.COMMON, false, CatTraitSlot.APPEARANCE),
    RAINBOW_CAT("rainbow_cat", CatTraitRarity.EXCELLENT, false,
            CatTraitSlot.APPEARANCE),
    NEKOMATA("nekomata", CatTraitRarity.EXCELLENT, false,
            CatTraitSlot.APPEARANCE),
    PUSS_IN_BOOTS("puss_in_boots", CatTraitRarity.GOOD, false,
            CatTraitSlot.APPEARANCE),
    BIG_CHONKY_CAT("big_chonky_cat", CatTraitRarity.GOOD, true,
            CatTraitSlot.APPEARANCE),

    // Behaviour traits. The narrow slots prevent competing AI targets while
    // still allowing compatible passive, environmental and audio behaviours.
    ANOREXIA("anorexia", CatTraitRarity.DEFECT, false),
    GOOD_CAT("good_cat", CatTraitRarity.GOOD, false,
            CatTraitSlot.HISSING_BEHAVIOUR),
    LOW_LEVEL_CODE("low_level_code", CatTraitRarity.DEFECT, false,
            CatTraitSlot.MOVEMENT_BEHAVIOUR),
    SHEDDING("shedding", CatTraitRarity.DEFECT, false,
            CatTraitSlot.MOVEMENT_BEHAVIOUR),
    STITCH("stitch", CatTraitRarity.DEFECT, false,
            CatTraitSlot.COMBAT_BEHAVIOUR),
    XIAOTING("xiaoting", CatTraitRarity.GOOD, false),
    DORAEMON("doraemon", CatTraitRarity.GOOD, false),
    EDWARD("edward", CatTraitRarity.DEFECT, false,
            CatTraitSlot.COMBAT_BEHAVIOUR),
    CAT_KING("cat_king", CatTraitRarity.GOOD, false),
    DING_DONG_CAT("ding_dong_cat", CatTraitRarity.COMMON, false),
    CODE_CONFLICT("code_conflict", CatTraitRarity.DEFECT, false),
    FOOD_GUARD("food_guard", CatTraitRarity.DEFECT, false,
            CatTraitSlot.COMBAT_BEHAVIOUR),
    MISCHIEVOUS("mischievous", CatTraitRarity.DEFECT, false,
            CatTraitSlot.COMBAT_BEHAVIOUR),
    FILICIDE("filicide", CatTraitRarity.DEFECT, false,
            CatTraitSlot.COMBAT_BEHAVIOUR),
    DROWNING("drowning", CatTraitRarity.DEFECT, false,
            CatTraitSlot.WET_CONDITION),
    CABLE_BITER("cable_biter", CatTraitRarity.DEFECT, false),
    AIR_RAID_SIREN("air_raid_siren", CatTraitRarity.COMMON, false),
    MINOR_ILLNESS("minor_illness", CatTraitRarity.DEFECT, false),
    CUDDLE_ONLY("cuddle_only", CatTraitRarity.DEFECT, false,
            CatTraitSlot.REPRODUCTION_BEHAVIOUR),
    SELECTED_ELDER("selected_elder", CatTraitRarity.GOOD, true,
            CatTraitSlot.HISSING_BEHAVIOUR),
    TOM_TREE_FELLER("tom_tree_feller", CatTraitRarity.COMMON, false),
    HUNTER_KIMI("hunter_kimi", CatTraitRarity.DEFECT, false,
            CatTraitSlot.MOVEMENT_BEHAVIOUR, CatTraitSlot.COMBAT_BEHAVIOUR),
    HIGH_STEP("high_step", CatTraitRarity.GOOD, false),
    SKY_CAT("sky_cat", CatTraitRarity.GOOD, false,
            CatTraitSlot.MOVEMENT_BEHAVIOUR),
    AUTO_ATTACH("auto_attach", CatTraitRarity.COMMON, false,
            CatTraitSlot.MOVEMENT_BEHAVIOUR),
    TRIPOD_CAT("tripod_cat", CatTraitRarity.DEFECT, false,
            CatTraitSlot.MOVEMENT_BEHAVIOUR),
    HIGH_EXPLOSIVE_FUEL("high_explosive_fuel", CatTraitRarity.EXCELLENT, false,
            CatTraitSlot.FIRE_CAREER),
    ROLLING_LOG("rolling_log", CatTraitRarity.GOOD, false,
            CatTraitSlot.APPEARANCE),

    // Common single-stat traits: 4/6/8/10/12/14/16.
    TOUGH("tough", CatTraitRarity.COMMON, CatStat.HEALTH,
            4, 16, CatTraitSlot.HEALTH_BONUS),
    BRUTE_FORCE("brute_force", CatTraitRarity.COMMON, CatStat.ATTACK,
            4, 16, CatTraitSlot.ATTACK_BONUS),
    FLEET_FOOTED("fleet_footed", CatTraitRarity.COMMON, CatStat.SPEED,
            4, 16, CatTraitSlot.SPEED_BONUS),
    STEEL_FRAME("steel_frame", CatTraitRarity.COMMON, CatStat.STAMINA,
            4, 16, CatTraitSlot.STAMINA_BONUS),
    QUICK_WITTED("quick_witted", CatTraitRarity.COMMON, CatStat.INTELLIGENCE,
            4, 16, CatTraitSlot.INTELLIGENCE_BONUS),
    LUCKY_CAT("lucky_cat", CatTraitRarity.COMMON, CatStat.LUCK,
            4, 16, CatTraitSlot.LUCK_BONUS),

    // Good single-stat traits: 15/18/20/23/25/28/30.
    VIGOROUS("vigorous", CatTraitRarity.GOOD, CatStat.HEALTH,
            15, 30, CatTraitSlot.HEALTH_BONUS),
    OVERWHELMING_FORCE("overwhelming_force", CatTraitRarity.GOOD, CatStat.ATTACK,
            15, 30, CatTraitSlot.ATTACK_BONUS),
    GALE_STRIDE("gale_stride", CatTraitRarity.GOOD, CatStat.SPEED,
            15, 30, CatTraitSlot.SPEED_BONUS),
    IRON_BODY("iron_body", CatTraitRarity.GOOD, CatStat.STAMINA,
            15, 30, CatTraitSlot.STAMINA_BONUS),
    BRILLIANT_MIND("brilliant_mind", CatTraitRarity.GOOD, CatStat.INTELLIGENCE,
            15, 30, CatTraitSlot.INTELLIGENCE_BONUS),
    GREAT_FORTUNE("great_fortune", CatTraitRarity.GOOD, CatStat.LUCK,
            15, 30, CatTraitSlot.LUCK_BONUS),

    // Excellent single-stat traits: 25/28/30/33/35/38/40.
    OCEANIC_VITALITY("oceanic_vitality", CatTraitRarity.EXCELLENT, CatStat.HEALTH,
            25, 40, CatTraitSlot.HEALTH_BONUS),
    MIGHT_OVER_ALL("might_over_all", CatTraitRarity.EXCELLENT, CatStat.ATTACK,
            25, 40, CatTraitSlot.ATTACK_BONUS),
    LIGHTNING_STEP("lightning_step", CatTraitRarity.EXCELLENT, CatStat.SPEED,
            25, 40, CatTraitSlot.SPEED_BONUS),
    ADAMANT_BODY("adamant_body", CatTraitRarity.EXCELLENT, CatStat.STAMINA,
            25, 40, CatTraitSlot.STAMINA_BONUS),
    SUPREME_INTELLECT("supreme_intellect", CatTraitRarity.EXCELLENT,
            CatStat.INTELLIGENCE, 25, 40, CatTraitSlot.INTELLIGENCE_BONUS),
    CHOSEN_BY_FATE("chosen_by_fate", CatTraitRarity.EXCELLENT, CatStat.LUCK,
            25, 40, CatTraitSlot.LUCK_BONUS);

    public static final int MAX_UPGRADABLE_LEVEL = 7;

    private final String serializedName;
    private final ResourceLocation id;
    private final CatTraitRarity rarity;
    private final boolean upgradable;
    private final Set<CatTraitSlot> occupiedSlots;
    private final CatStat attributeStat;
    private final int minimumAttributeBonus;
    private final int maximumAttributeBonus;

    CatTrait(String serializedName, CatTraitRarity rarity, boolean upgradable,
             CatTraitSlot... occupiedSlots) {
        this.serializedName = serializedName;
        this.id = ResourceLocation.fromNamespaceAndPath(LaoWuMod.MOD_ID, serializedName);
        this.rarity = rarity;
        this.upgradable = upgradable;
        this.attributeStat = null;
        this.minimumAttributeBonus = 0;
        this.maximumAttributeBonus = 0;
        this.occupiedSlots = occupiedSlots.length == 0
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(occupiedSlots)));
    }

    CatTrait(String serializedName, CatTraitRarity rarity, CatStat attributeStat,
             int minimumAttributeBonus, int maximumAttributeBonus,
             CatTraitSlot occupiedSlot) {
        this.serializedName = serializedName;
        this.id = ResourceLocation.fromNamespaceAndPath(LaoWuMod.MOD_ID, serializedName);
        this.rarity = rarity;
        this.upgradable = true;
        this.attributeStat = attributeStat;
        this.minimumAttributeBonus = minimumAttributeBonus;
        this.maximumAttributeBonus = maximumAttributeBonus;
        this.occupiedSlots = Collections.unmodifiableSet(EnumSet.of(occupiedSlot));
    }

    public String serializedName() {
        return serializedName;
    }

    public ResourceLocation id() {
        return id;
    }

    public CatTraitRarity rarity() {
        return rarity;
    }

    public boolean upgradable() {
        return upgradable;
    }

    public int maxLevel() {
        return upgradable ? MAX_UPGRADABLE_LEVEL : 1;
    }

    public Set<CatTraitSlot> occupiedSlots() {
        return occupiedSlots;
    }

    public boolean isAttributeBonus() {
        return attributeStat != null;
    }

    public CatStat attributeStat() {
        return attributeStat;
    }

    public int attributeBonus(int level) {
        if (!isAttributeBonus()) return 0;
        int progress = clampLevel(level) - 1;
        return minimumAttributeBonus + Math.round(
                (maximumAttributeBonus - minimumAttributeBonus) * progress / 6.0F);
    }

    public int thornsChance(int level) {
        return this == THORNS ? scaledLevelValue(level, 25, 40) : 0;
    }

    public int thornsDamagePercent(int level) {
        return this == THORNS ? 60 + (clampLevel(level) - 1) * 10 : 0;
    }

    public int nightAttackBonus(int level) {
        return this == NIGHT_OWL ? 3 + (clampLevel(level) - 1) * 2 : 0;
    }

    public int nightSpeedBonus(int level) {
        return this == NIGHT_OWL ? 1 + (clampLevel(level) - 1) : 0;
    }

    public int longFurStaminaBonus(int level) {
        return this == LONG_FUR ? 12 + (clampLevel(level) - 1) * 3 : 0;
    }

    public int longFurExtraDrops(int level) {
        return this == LONG_FUR ? (clampLevel(level) + 1) / 2 : 0;
    }

    public int longFurVisionPenalty(int level) {
        return this == LONG_FUR ? clampLevel(level) : 0;
    }

    public int bristlingAttackBonus(int level) {
        return this == BRISTLING_RAGE ? scaledLevelValue(level, 15, 30) : 0;
    }

    public int healingPurrHealthBonus(int level) {
        return this == HEALING_PURR ? 17 + (clampLevel(level) - 1) * 3 : 0;
    }

    public int healingPurrAmount(int level) {
        return this == HEALING_PURR ? (clampLevel(level) + 1) / 2 : 0;
    }

    public int luBuAttackBonus() {
        return this == LU_BU_REBORN ? 20 : 0;
    }

    public int beebeeWorkIntervalSeconds(int level) {
        return this == BEEBEE_GENE ? Math.max(3, 10 - clampLevel(level)) : 10;
    }

    public int prosperousBreedingReductionSeconds(int level) {
        return this == PROSPEROUS_LITTER ? 5 * clampLevel(level) : 0;
    }

    public int anglersLuckBonus(int level) {
        return this == ANGLERS_FORTUNE ? scaledLevelValue(level, 15, 30) : 0;
    }

    public int protectiveAttackBonus(int level) {
        return this == PROTECTIVE_INSTINCT ? scaledLevelValue(level, 15, 30) : 0;
    }

    public int wetFuryAttackBonus(int level) {
        return this == WET_FURY ? 18 + (clampLevel(level) - 1) * 3 : 0;
    }

    public int wetFurySpeedPenalty(int level) {
        return this == WET_FURY ? 6 + (clampLevel(level) - 1) * 2 : 0;
    }

    public int chonkyHealthBonus(int level) {
        return this == CHONKY_PRESENCE ? 18 + (clampLevel(level) - 1) * 4 : 0;
    }

    public int chonkyStaminaBonus(int level) {
        return this == CHONKY_PRESENCE ? 12 + (clampLevel(level) - 1) * 3 : 0;
    }

    public int chonkySpeedPenalty(int level) {
        return this == CHONKY_PRESENCE ? 8 + (clampLevel(level) - 1) * 2 : 0;
    }

    public int glassClawsAttackBonus(int level) {
        return this == GLASS_CLAWS ? 20 + (clampLevel(level) - 1) * 4 : 0;
    }

    public int glassClawsHealthPenalty(int level) {
        return this == GLASS_CLAWS ? 10 + (clampLevel(level) - 1) * 3 : 0;
    }

    public int highTailSpeedBonus(int level) {
        return this == TAIL_HELD_HIGH ? 3 + (clampLevel(level) - 1) * 2 : 0;
    }

    public int highTailLuckBonus(int level) {
        return this == TAIL_HELD_HIGH ? clampLevel(level) : 0;
    }

    public int loafIntelligenceBonus(int level) {
        return this == LOAF_THOUGHTS ? 4 + (clampLevel(level) - 1) * 2 : 0;
    }

    public int loafStaminaBonus(int level) {
        return this == LOAF_THOUGHTS ? 2 + (clampLevel(level) - 1) : 0;
    }

    public int nineLivesChance(int level) {
        return this == NINE_LIVES ? 7 + (clampLevel(level) - 1) * 3 : 0;
    }

    public int waterShyAttackPenalty() {
        return this == WATER_SHY ? 15 : 0;
    }

    public int waterShySpeedPenalty() {
        return this == WATER_SHY ? 20 : 0;
    }

    public int dayDrowsySpeedPenalty() {
        return this == DAY_DROWSY ? 15 : 0;
    }

    public int dayDrowsyIntelligencePenalty() {
        return this == DAY_DROWSY ? 15 : 0;
    }

    public int timidAttackPenalty() {
        return this == TIMID ? 15 : 0;
    }

    public int timidSpeedBonus() {
        return this == TIMID ? 8 : 0;
    }

    public int attentionMagnetStaminaBonus(int level) {
        return this == ATTENTION_MAGNET ? scaledLevelValue(level, 15, 30) : 0;
    }

    public int cainLuckBonus(int level) {
        return this == CAIN_MARK ? scaledLevelValue(level, 15, 30) : 0;
    }

    public int cainBaseAvoidChance(int level) {
        return this == CAIN_MARK ? 3 + clampLevel(level) - 1 : 0;
    }

    public int energyRecoveryPercent() {
        return this == ENERGY_RECOVERY ? 30 : 0;
    }

    public int energyRecoveryCooldownSeconds() {
        return this == ENERGY_RECOVERY ? 60 : 0;
    }

    public int selectedElderAttackBonus(int level) {
        return this == SELECTED_ELDER ? 10 + (clampLevel(level) - 1) * 5 : 0;
    }

    /** Visual and physical size used by Big Chonky Cat, from 115% to 175%. */
    public int bigCatScalePercent(int level) {
        return this == BIG_CHONKY_CAT ? 115 + (clampLevel(level) - 1) * 10 : 100;
    }

    public int rainbowSpeedBonus() {
        return this == RAINBOW_CAT ? 10 : 0;
    }

    public int rainbowLuckBonus() {
        return this == RAINBOW_CAT ? 20 : 0;
    }

    public int nekomataAttackBonus() {
        return this == NEKOMATA ? 20 : 0;
    }

    public int nekomataIntelligenceBonus() {
        return this == NEKOMATA ? 10 : 0;
    }

    private int scaledLevelValue(int level, int minimum, int maximum) {
        int progress = clampLevel(level) - 1;
        return minimum + Math.round((maximum - minimum) * progress / 6.0F);
    }

    public int clampLevel(int level) {
        return Math.max(1, Math.min(maxLevel(), level));
    }

    public Component title() {
        return Component.translatable("trait.laowu." + serializedName + ".title");
    }

    public Component summary(int level) {
        int clamped = clampLevel(level);
        if (isAttributeBonus()) {
            return Component.translatable("trait.laowu.attribute_bonus.summary",
                    statName(), attributeBonus(clamped));
        }
        return switch (this) {
            case THORNS -> Component.translatable("trait.laowu.thorns.summary",
                    thornsChance(clamped), thornsDamagePercent(clamped));
            case NIGHT_OWL -> Component.translatable("trait.laowu.night_owl.summary",
                    nightAttackBonus(clamped), nightSpeedBonus(clamped));
            case HEAT_RESISTANCE -> Component.translatable(
                    "trait.laowu.heat_resistance.summary");
            case DOUGHY -> Component.translatable("trait.laowu.doughy.summary");
            case LONG_FUR -> Component.translatable("trait.laowu.long_fur.summary",
                    longFurStaminaBonus(clamped), longFurExtraDrops(clamped),
                    longFurVisionPenalty(clamped));
            case BRISTLING_RAGE -> Component.translatable(
                    "trait.laowu.bristling_rage.summary", bristlingAttackBonus(clamped));
            case HEALING_PURR -> Component.translatable("trait.laowu.healing_purr.summary",
                    healingPurrHealthBonus(clamped), healingPurrAmount(clamped));
            case LU_BU_REBORN -> Component.translatable("trait.laowu.lu_bu_reborn.summary",
                    luBuAttackBonus());
            case BEEBEE_GENE -> Component.translatable("trait.laowu.beebee_gene.summary",
                    beebeeWorkIntervalSeconds(clamped));
            case PROSPEROUS_LITTER -> Component.translatable(
                    "trait.laowu.prosperous_litter.summary",
                    prosperousBreedingReductionSeconds(clamped));
            case ANGLERS_FORTUNE -> Component.translatable(
                    "trait.laowu.anglers_fortune.summary", anglersLuckBonus(clamped));
            case SUPERHEAT_GENE -> Component.translatable(
                    "trait.laowu.superheat_gene.summary");
            case PROTECTIVE_INSTINCT -> Component.translatable(
                    "trait.laowu.protective_instinct.summary",
                    protectiveAttackBonus(clamped));
            case WET_FURY -> Component.translatable("trait.laowu.wet_fury.summary",
                    wetFuryAttackBonus(clamped), wetFurySpeedPenalty(clamped));
            case CHONKY_PRESENCE -> Component.translatable(
                    "trait.laowu.chonky_presence.summary",
                    chonkyHealthBonus(clamped), chonkyStaminaBonus(clamped),
                    chonkySpeedPenalty(clamped));
            case GLASS_CLAWS -> Component.translatable("trait.laowu.glass_claws.summary",
                    glassClawsAttackBonus(clamped), glassClawsHealthPenalty(clamped));
            case TAIL_HELD_HIGH -> Component.translatable(
                    "trait.laowu.tail_held_high.summary",
                    highTailSpeedBonus(clamped), highTailLuckBonus(clamped));
            case LOAF_THOUGHTS -> Component.translatable("trait.laowu.loaf_thoughts.summary",
                    loafIntelligenceBonus(clamped), loafStaminaBonus(clamped));
            case NINE_LIVES -> Component.translatable("trait.laowu.nine_lives.summary",
                    nineLivesChance(clamped));
            case WATER_SHY -> Component.translatable("trait.laowu.water_shy.summary",
                    waterShyAttackPenalty(), waterShySpeedPenalty());
            case DAY_DROWSY -> Component.translatable("trait.laowu.day_drowsy.summary",
                    dayDrowsySpeedPenalty(), dayDrowsyIntelligencePenalty());
            case TIMID -> Component.translatable("trait.laowu.timid.summary",
                    timidAttackPenalty(), timidSpeedBonus());
            case ATTENTION_MAGNET -> Component.translatable(
                    "trait.laowu.attention_magnet.summary",
                    attentionMagnetStaminaBonus(clamped));
            case CAIN_MARK -> Component.translatable("trait.laowu.cain_mark.summary",
                    cainLuckBonus(clamped), cainBaseAvoidChance(clamped));
            case ENERGY_RECOVERY -> Component.translatable(
                    "trait.laowu.energy_recovery.summary",
                    energyRecoveryPercent());
            case SELECTED_ELDER -> Component.translatable(
                    "trait.laowu.selected_elder.summary",
                    selectedElderAttackBonus(clamped));
            case BIG_CHONKY_CAT -> Component.translatable(
                    "trait.laowu.big_chonky_cat.summary",
                    bigCatScalePercent(clamped));
            case RAINBOW_CAT -> Component.translatable(
                    "trait.laowu.rainbow_cat.summary",
                    rainbowSpeedBonus(), rainbowLuckBonus());
            case NEKOMATA -> Component.translatable(
                    "trait.laowu.nekomata.summary",
                    nekomataAttackBonus(), nekomataIntelligenceBonus());
            case ANOREXIA, GOOD_CAT, LOW_LEVEL_CODE, SHEDDING, STITCH,
                    XIAOTING, DORAEMON, EDWARD, CAT_KING, DING_DONG_CAT,
                    CODE_CONFLICT, FOOD_GUARD, MISCHIEVOUS, FILICIDE,
                    DROWNING, CABLE_BITER, AIR_RAID_SIREN, MINOR_ILLNESS,
                    CUDDLE_ONLY, TOM_TREE_FELLER, HUNTER_KIMI,
                    HIGH_STEP, SKY_CAT, AUTO_ATTACH, TRIPOD_CAT,
                    HIGH_EXPLOSIVE_FUEL, ROLLING_LOG, LOLI, HIM, ISAAC,
                    ROUND_HEAD, OIIAI,
                    PUSS_IN_BOOTS -> Component.translatable(
                    "trait.laowu." + serializedName + ".summary");
            default -> Component.empty();
        };
    }

    public Component description(int level) {
        int clamped = clampLevel(level);
        if (isAttributeBonus()) {
            return Component.translatable("trait.laowu.attribute_bonus.description",
                    statName(), attributeBonus(clamped));
        }
        return switch (this) {
            case THORNS -> Component.translatable("trait.laowu.thorns.description",
                    thornsChance(clamped), thornsDamagePercent(clamped));
            case NIGHT_OWL -> Component.translatable("trait.laowu.night_owl.description",
                    nightAttackBonus(clamped), nightSpeedBonus(clamped));
            case HEAT_RESISTANCE -> Component.translatable(
                    "trait.laowu.heat_resistance.description");
            case DOUGHY -> Component.translatable("trait.laowu.doughy.description");
            case LONG_FUR -> Component.translatable("trait.laowu.long_fur.description",
                    longFurStaminaBonus(clamped), longFurExtraDrops(clamped),
                    longFurVisionPenalty(clamped));
            case BRISTLING_RAGE -> Component.translatable(
                    "trait.laowu.bristling_rage.description", bristlingAttackBonus(clamped));
            case HEALING_PURR -> Component.translatable("trait.laowu.healing_purr.description",
                    healingPurrHealthBonus(clamped), healingPurrAmount(clamped));
            case LU_BU_REBORN -> Component.translatable("trait.laowu.lu_bu_reborn.description",
                    luBuAttackBonus());
            case BEEBEE_GENE -> Component.translatable("trait.laowu.beebee_gene.description",
                    beebeeWorkIntervalSeconds(clamped));
            case PROSPEROUS_LITTER -> Component.translatable(
                    "trait.laowu.prosperous_litter.description",
                    prosperousBreedingReductionSeconds(clamped));
            case ANGLERS_FORTUNE -> Component.translatable(
                    "trait.laowu.anglers_fortune.description", anglersLuckBonus(clamped));
            case SUPERHEAT_GENE -> Component.translatable(
                    "trait.laowu.superheat_gene.description");
            case PROTECTIVE_INSTINCT -> Component.translatable(
                    "trait.laowu.protective_instinct.description",
                    protectiveAttackBonus(clamped));
            case WET_FURY -> Component.translatable("trait.laowu.wet_fury.description",
                    wetFuryAttackBonus(clamped), wetFurySpeedPenalty(clamped));
            case CHONKY_PRESENCE -> Component.translatable(
                    "trait.laowu.chonky_presence.description",
                    chonkyHealthBonus(clamped), chonkyStaminaBonus(clamped),
                    chonkySpeedPenalty(clamped));
            case GLASS_CLAWS -> Component.translatable("trait.laowu.glass_claws.description",
                    glassClawsAttackBonus(clamped), glassClawsHealthPenalty(clamped));
            case TAIL_HELD_HIGH -> Component.translatable(
                    "trait.laowu.tail_held_high.description",
                    highTailSpeedBonus(clamped), highTailLuckBonus(clamped));
            case LOAF_THOUGHTS -> Component.translatable("trait.laowu.loaf_thoughts.description",
                    loafIntelligenceBonus(clamped), loafStaminaBonus(clamped));
            case NINE_LIVES -> Component.translatable("trait.laowu.nine_lives.description",
                    nineLivesChance(clamped));
            case WATER_SHY -> Component.translatable("trait.laowu.water_shy.description",
                    waterShyAttackPenalty(), waterShySpeedPenalty());
            case DAY_DROWSY -> Component.translatable("trait.laowu.day_drowsy.description",
                    dayDrowsySpeedPenalty(), dayDrowsyIntelligencePenalty());
            case TIMID -> Component.translatable("trait.laowu.timid.description",
                    timidAttackPenalty(), timidSpeedBonus());
            case ATTENTION_MAGNET -> Component.translatable(
                    "trait.laowu.attention_magnet.description",
                    attentionMagnetStaminaBonus(clamped));
            case CAIN_MARK -> Component.translatable("trait.laowu.cain_mark.description",
                    cainLuckBonus(clamped), cainBaseAvoidChance(clamped));
            case ENERGY_RECOVERY -> Component.translatable(
                    "trait.laowu.energy_recovery.description",
                    energyRecoveryPercent(), energyRecoveryCooldownSeconds());
            case SELECTED_ELDER -> Component.translatable(
                    "trait.laowu.selected_elder.description",
                    selectedElderAttackBonus(clamped));
            case BIG_CHONKY_CAT -> Component.translatable(
                    "trait.laowu.big_chonky_cat.description",
                    bigCatScalePercent(clamped));
            case RAINBOW_CAT -> Component.translatable(
                    "trait.laowu.rainbow_cat.description",
                    rainbowSpeedBonus(), rainbowLuckBonus());
            case NEKOMATA -> Component.translatable(
                    "trait.laowu.nekomata.description",
                    nekomataAttackBonus(), nekomataIntelligenceBonus());
            case ANOREXIA, GOOD_CAT, LOW_LEVEL_CODE, SHEDDING, STITCH,
                    XIAOTING, DORAEMON, EDWARD, CAT_KING, DING_DONG_CAT,
                    CODE_CONFLICT, FOOD_GUARD, MISCHIEVOUS, FILICIDE,
                    DROWNING, CABLE_BITER, AIR_RAID_SIREN, MINOR_ILLNESS,
                    CUDDLE_ONLY, TOM_TREE_FELLER, HUNTER_KIMI,
                    HIGH_STEP, SKY_CAT, AUTO_ATTACH, TRIPOD_CAT,
                    HIGH_EXPLOSIVE_FUEL, ROLLING_LOG, LOLI, HIM, ISAAC,
                    ROUND_HEAD, OIIAI,
                    PUSS_IN_BOOTS -> Component.translatable(
                    "trait.laowu." + serializedName + ".description");
            default -> Component.empty();
        };
    }

    public Component nextLevelDescription(int level) {
        int next = clampLevel(level + 1);
        if (isAttributeBonus()) {
            return Component.translatable("trait.laowu.attribute_bonus.next",
                    statName(), attributeBonus(next));
        }
        return switch (this) {
            case THORNS -> Component.translatable("trait.laowu.thorns.next",
                    thornsChance(next), thornsDamagePercent(next));
            case NIGHT_OWL -> Component.translatable("trait.laowu.night_owl.next",
                    nightAttackBonus(next), nightSpeedBonus(next));
            case LONG_FUR -> Component.translatable("trait.laowu.long_fur.next",
                    longFurStaminaBonus(next), longFurExtraDrops(next),
                    longFurVisionPenalty(next));
            case BRISTLING_RAGE -> Component.translatable(
                    "trait.laowu.bristling_rage.next", bristlingAttackBonus(next));
            case HEALING_PURR -> Component.translatable("trait.laowu.healing_purr.next",
                    healingPurrHealthBonus(next), healingPurrAmount(next));
            case BEEBEE_GENE -> Component.translatable("trait.laowu.beebee_gene.next",
                    beebeeWorkIntervalSeconds(next));
            case PROSPEROUS_LITTER -> Component.translatable(
                    "trait.laowu.prosperous_litter.next",
                    prosperousBreedingReductionSeconds(next));
            case ANGLERS_FORTUNE -> Component.translatable(
                    "trait.laowu.anglers_fortune.next", anglersLuckBonus(next));
            case PROTECTIVE_INSTINCT -> Component.translatable(
                    "trait.laowu.protective_instinct.next",
                    protectiveAttackBonus(next));
            case WET_FURY -> Component.translatable("trait.laowu.wet_fury.next",
                    wetFuryAttackBonus(next), wetFurySpeedPenalty(next));
            case CHONKY_PRESENCE -> Component.translatable(
                    "trait.laowu.chonky_presence.next",
                    chonkyHealthBonus(next), chonkyStaminaBonus(next),
                    chonkySpeedPenalty(next));
            case GLASS_CLAWS -> Component.translatable("trait.laowu.glass_claws.next",
                    glassClawsAttackBonus(next), glassClawsHealthPenalty(next));
            case TAIL_HELD_HIGH -> Component.translatable(
                    "trait.laowu.tail_held_high.next",
                    highTailSpeedBonus(next), highTailLuckBonus(next));
            case LOAF_THOUGHTS -> Component.translatable("trait.laowu.loaf_thoughts.next",
                    loafIntelligenceBonus(next), loafStaminaBonus(next));
            case NINE_LIVES -> Component.translatable("trait.laowu.nine_lives.next",
                    nineLivesChance(next));
            case ATTENTION_MAGNET -> Component.translatable(
                    "trait.laowu.attention_magnet.next",
                    attentionMagnetStaminaBonus(next));
            case CAIN_MARK -> Component.translatable("trait.laowu.cain_mark.next",
                    cainLuckBonus(next), cainBaseAvoidChance(next));
            case SELECTED_ELDER -> Component.translatable(
                    "trait.laowu.selected_elder.next",
                    selectedElderAttackBonus(next));
            case BIG_CHONKY_CAT -> Component.translatable(
                    "trait.laowu.big_chonky_cat.next",
                    bigCatScalePercent(next));
            default -> Component.empty();
        };
    }

    private Component statName() {
        return Component.translatable("stat.laowu.cat." + attributeStat.serializedName());
    }

    public static Optional<CatTrait> byId(ResourceLocation id) {
        if (id == null) return Optional.empty();
        return Arrays.stream(values()).filter(trait -> trait.id.equals(id)).findFirst();
    }
}
