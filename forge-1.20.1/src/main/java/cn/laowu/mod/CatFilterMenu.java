package cn.laowu.mod;

import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.genetics.CatTraitType;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitRegistry;
import cn.laowu.mod.item.CatFilterRules;
import cn.laowu.mod.item.CatFilterLogic;
import com.simibubi.create.content.logistics.filter.AbstractFilterMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

/** Held-item menu for numeric, trait and categorical cat-pancake conditions. */
public final class CatFilterMenu extends AbstractFilterMenu {
    private static final int CURRENT_MIN_START = 0;
    private static final int CURRENT_MAX_START = CURRENT_MIN_START + CatFilterRules.STAT_COUNT;
    private static final int POTENTIAL_MIN_START = CURRENT_MAX_START + CatFilterRules.STAT_COUNT;
    private static final int POTENTIAL_MAX_START = POTENTIAL_MIN_START + CatFilterRules.STAT_COUNT;
    public static final int MAX_REQUIRED_TRAITS = 4;
    private static final int TRAIT_SELECTION_START =
            POTENTIAL_MAX_START + CatFilterRules.STAT_COUNT;
    private static final int GROWTH_FILTER_INDEX =
            TRAIT_SELECTION_START + MAX_REQUIRED_TRAITS;
    private static final int OWNERSHIP_FILTER_INDEX = GROWTH_FILTER_INDEX + 1;
    private static final int CAREER_FILTER_INDEX = OWNERSHIP_FILTER_INDEX + 1;
    private static final int CURRENT_ENABLED_INDEX = CAREER_FILTER_INDEX + 1;
    private static final int LIMIT_ENABLED_INDEX = CURRENT_ENABLED_INDEX + 1;
    private static final int LOGIC_FLAGS_INDEX = LIMIT_ENABLED_INDEX + 1;
    private static final int BASE_CURRENT_INDEX = LOGIC_FLAGS_INDEX + 1;
    private static final int DATA_COUNT = BASE_CURRENT_INDEX + 1;
    private static final int LOGIC_BUTTON_BASE = 25_000;
    private static final int ENABLE_BUTTON_BASE = 25_100;

    private static final int RANGE_BUTTON_BASE = 1000;
    private static final int BOUND_STRIDE = CatFilterRules.MAX_CURRENT_VALUE + 1;
    private static final int STAT_STRIDE = BOUND_STRIDE * 2;
    private static final int PAGE_STRIDE = STAT_STRIDE * CatFilterRules.STAT_COUNT;
    private static final int ADD_TRAIT_BUTTON_BASE = 30_000;
    private static final int REMOVE_TRAIT_BUTTON_BASE = 31_000;
    private static final int IDENTITY_BUTTON_BASE = 32_000;
    private static final int IDENTITY_FIELD_STRIDE = 100;
    public static final int GROWTH_FIELD = 0;
    public static final int OWNERSHIP_FIELD = 1;
    public static final int CAREER_FIELD = 2;

    private final SimpleContainerData ranges = new SimpleContainerData(DATA_COUNT);
    private String nameQuery = "";
    private final List<CatTraitType> traitCatalog;
    private final long traitRevision;

    public CatFilterMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readItem());
    }

    public CatFilterMenu(int containerId, Inventory inventory, ItemStack filterStack) {
        super(LaoWuMod.CAT_FILTER_MENU.get(), containerId, inventory, filterStack);
        var rules = CatFilterRules.read(filterStack);
        boolean client = inventory.player.level().isClientSide;
        var catalog = new ArrayList<>(CatTraitRegistry.values(client));
        for (var trait : rules.requiredTraits())
            if (catalog.stream().noneMatch(t -> t.id().equals(trait.id()))) catalog.add(trait);
        this.traitCatalog = List.copyOf(catalog);
        this.traitRevision = CatTraitRegistry.revision(client);
        load(rules);
        addDataSlots(ranges);
    }

    public List<CatTraitType> traitCatalog() { return traitCatalog; }

    @Override
    public boolean stillValid(Player player) {
        return (player.level().isClientSide || traitRevision == CatTraitRegistry.revision(false))
                && super.stillValid(player);
    }

    @Override
    protected int getPlayerInventoryXOffset() {
        // The custom filter is 32px wider than Create's source panel. Keep the
        // inventory slots fixed over the separately centred inventory texture.
        return 56;
    }

    @Override
    protected int getPlayerInventoryYOffset() {
        return 177;
    }

    @Override
    protected void addFilterSlots() {
        // Attribute ranges are controls rather than ghost item slots.
    }

    @Override
    protected ItemStackHandler createGhostInventory() {
        return new ItemStackHandler(0);
    }

    @Override
    protected void saveData(ItemStack stack) {
        rules().write(stack);
    }

    @Override
    public void clearContents() {
        for (CatStat stat : CatStat.values()) {
            ranges.set(index(CatFilterRules.CURRENT_PAGE, false, stat),
                    CatFilterRules.MIN_VALUE);
            ranges.set(index(CatFilterRules.CURRENT_PAGE, true, stat),
                    CatFilterRules.MAX_CURRENT_VALUE);
            ranges.set(index(CatFilterRules.POTENTIAL_PAGE, false, stat),
                    CatFilterRules.MIN_VALUE);
            ranges.set(index(CatFilterRules.POTENTIAL_PAGE, true, stat),
                    CatFilterRules.MAX_POTENTIAL_VALUE);
        }
        for (int slot = 0; slot < MAX_REQUIRED_TRAITS; slot++) {
            ranges.set(TRAIT_SELECTION_START + slot, 0);
        }
        ranges.set(GROWTH_FILTER_INDEX, CatFilterRules.GrowthFilter.ANY.ordinal());
        ranges.set(OWNERSHIP_FILTER_INDEX, CatFilterRules.OwnershipFilter.ANY.ordinal());
        ranges.set(CAREER_FILTER_INDEX, CatFilterRules.CareerFilter.ANY.ordinal());
        nameQuery = "";
        ranges.set(CURRENT_ENABLED_INDEX, 0);
        ranges.set(LIMIT_ENABLED_INDEX, 0);
        ranges.set(LOGIC_FLAGS_INDEX, 0);
        ranges.set(BASE_CURRENT_INDEX, 0);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!player.level().isClientSide && traitRevision != CatTraitRegistry.revision(false)) return false;
        if (id >= LOGIC_BUTTON_BASE && id < LOGIC_BUTTON_BASE + 10) {
            int option = (id - LOGIC_BUTTON_BASE) / 2;
            int bit = 1 << option;
            int flags = ranges.get(LOGIC_FLAGS_INDEX);
            ranges.set(LOGIC_FLAGS_INDEX, (id & 1) == 1 ? flags | bit : flags & ~bit);
            return true;
        }
        if (id >= ENABLE_BUTTON_BASE && id < ENABLE_BUTTON_BASE + 24) {
            int encoded = id - ENABLE_BUTTON_BASE;
            int page = encoded / 12, stat = (encoded % 12) / 2;
            setEnabled(page, stat, (encoded & 1) == 1);
            return true;
        }
        if (id >= IDENTITY_BUTTON_BASE
                && id < IDENTITY_BUTTON_BASE + 3 * IDENTITY_FIELD_STRIDE) {
            int encoded = id - IDENTITY_BUTTON_BASE;
            int field = encoded / IDENTITY_FIELD_STRIDE;
            int selection = encoded % IDENTITY_FIELD_STRIDE;
            int maximum = switch (field) {
                case GROWTH_FIELD -> CatFilterRules.GrowthFilter.values().length;
                case OWNERSHIP_FIELD -> CatFilterRules.OwnershipFilter.values().length;
                case CAREER_FIELD -> CatFilterRules.CareerFilter.values().length;
                default -> 0;
            };
            if (selection < 0 || selection >= maximum) return false;
            ranges.set(switch (field) {
                case GROWTH_FIELD -> GROWTH_FILTER_INDEX;
                case OWNERSHIP_FIELD -> OWNERSHIP_FILTER_INDEX;
                case CAREER_FIELD -> CAREER_FILTER_INDEX;
                default -> throw new IllegalStateException("Unknown identity field " + field);
            }, selection);
            return true;
        }
        if (id >= REMOVE_TRAIT_BUTTON_BASE
                && id < REMOVE_TRAIT_BUTTON_BASE + MAX_REQUIRED_TRAITS) {
            int slot = id - REMOVE_TRAIT_BUTTON_BASE;
            for (int index = slot; index < MAX_REQUIRED_TRAITS - 1; index++) {
                ranges.set(TRAIT_SELECTION_START + index,
                        ranges.get(TRAIT_SELECTION_START + index + 1));
            }
            ranges.set(TRAIT_SELECTION_START + MAX_REQUIRED_TRAITS - 1, 0);
            return true;
        }
        if (id >= ADD_TRAIT_BUTTON_BASE
                && id <= ADD_TRAIT_BUTTON_BASE + traitCatalog.size()) {
            int selection = id - ADD_TRAIT_BUTTON_BASE;
            if (selection <= 0 || selection > traitCatalog.size()) return false;
            for (int slot = 0; slot < MAX_REQUIRED_TRAITS; slot++) {
                if (traitSelection(slot) == selection) return true;
            }
            for (int slot = 0; slot < MAX_REQUIRED_TRAITS; slot++) {
                if (traitSelection(slot) == 0) {
                    ranges.set(TRAIT_SELECTION_START + slot, selection);
                    return true;
                }
            }
            return false;
        }
        if (id < RANGE_BUTTON_BASE) return false;
        int encoded = id - RANGE_BUTTON_BASE;
        int page = encoded / PAGE_STRIDE;
        encoded %= PAGE_STRIDE;
        int statIndex = encoded / STAT_STRIDE;
        encoded %= STAT_STRIDE;
        int bound = encoded / BOUND_STRIDE;
        int value = encoded % BOUND_STRIDE;
        if (page < CatFilterRules.CURRENT_PAGE || page > CatFilterRules.POTENTIAL_PAGE
                || statIndex < 0 || statIndex >= CatFilterRules.STAT_COUNT
                || bound < 0 || bound > 1 || value > CatFilterRules.maxValue(page)) {
            return false;
        }

        CatStat stat = CatStat.values()[statIndex];
        boolean maximum = bound == 1;
        int minimum = min(page, stat);
        int upper = max(page, stat);
        int clamped = maximum
                ? Mth.clamp(value, minimum, CatFilterRules.maxValue(page))
                : Mth.clamp(value, CatFilterRules.MIN_VALUE, upper);
        ranges.set(index(page, maximum, stat), clamped);
        setEnabled(page, stat.ordinal(), true);
        // Do not change the held stack while this screen is open. Create's
        // AbstractFilterScreen treats an NBT change as a replaced filter and
        // closes itself. MenuBase.removed() persists the accumulated values.
        return true;
    }

    public boolean baseCurrent() { return ranges.get(BASE_CURRENT_INDEX) == 1; }

    public boolean enabled(int page, CatStat stat) {
        return (ranges.get(page == 1 ? LIMIT_ENABLED_INDEX : CURRENT_ENABLED_INDEX)
                & (1 << stat.ordinal())) != 0;
    }

    private void setEnabled(int page, int stat, boolean enabled) {
        int index = page == 1 ? LIMIT_ENABLED_INDEX : CURRENT_ENABLED_INDEX;
        int mask = ranges.get(index), bit = 1 << stat;
        ranges.set(index, enabled ? mask | bit : mask & ~bit);
    }

    public boolean option(int option) {
        return (ranges.get(LOGIC_FLAGS_INDEX) & (1 << option)) != 0;
    }

    public static int logicButton(int option, boolean value) {
        return LOGIC_BUTTON_BASE + Mth.clamp(option, 0, 4) * 2 + (value ? 1 : 0);
    }

    public static int enabledButton(int page, CatStat stat, boolean value) {
        return ENABLE_BUTTON_BASE + Mth.clamp(page, 0, 1) * 12
                + stat.ordinal() * 2 + (value ? 1 : 0);
    }

    public int min(int page, CatStat stat) {
        return ranges.get(index(page, false, stat));
    }

    public int max(int page, CatStat stat) {
        return ranges.get(index(page, true, stat));
    }

    /** Zero means an empty requirement slot; other values are the frozen catalog index + 1. */
    public int traitSelection(int slot) {
        return slot < 0 || slot >= MAX_REQUIRED_TRAITS
                ? 0 : ranges.get(TRAIT_SELECTION_START + slot);
    }

    public CatTraitType selectedTrait(int slot) {
        int selection = traitSelection(slot);
        return selection <= 0 || selection > traitCatalog.size()
                ? null : traitCatalog.get(selection - 1);
    }

    public List<CatTraitType> selectedTraits() {
        List<CatTraitType> selected = new ArrayList<>(MAX_REQUIRED_TRAITS);
        for (int slot = 0; slot < MAX_REQUIRED_TRAITS; slot++) {
            CatTraitType trait = selectedTrait(slot);
            if (trait != null) selected.add(trait);
        }
        return List.copyOf(selected);
    }

    public int growthSelection() {
        return ranges.get(GROWTH_FILTER_INDEX);
    }

    public int ownershipSelection() {
        return ranges.get(OWNERSHIP_FILTER_INDEX);
    }

    public int careerSelection() {
        return ranges.get(CAREER_FILTER_INDEX);
    }

    public String nameQuery() {
        return nameQuery;
    }

    public void setNameQuery(String nameQuery) {
        this.nameQuery = CatFilterRules.cleanName(nameQuery);
    }

    public static int rangeButton(int page, CatStat stat, boolean maximum, int value) {
        return RANGE_BUTTON_BASE + page * PAGE_STRIDE + stat.ordinal() * STAT_STRIDE
                + (maximum ? BOUND_STRIDE : 0)
                + Mth.clamp(value, CatFilterRules.MIN_VALUE,
                CatFilterRules.maxValue(page));
    }

    public static int addTraitButton(int selection) {
        return ADD_TRAIT_BUTTON_BASE
                + Mth.clamp(selection, 0, CatTrait.values().length + CatTraitRegistry.MAX_CUSTOM_TRAITS + MAX_REQUIRED_TRAITS);
    }

    public static int removeTraitButton(int slot) {
        return REMOVE_TRAIT_BUTTON_BASE
                + Mth.clamp(slot, 0, MAX_REQUIRED_TRAITS - 1);
    }

    public static int identityButton(int field, int selection) {
        return IDENTITY_BUTTON_BASE
                + Mth.clamp(field, GROWTH_FIELD, CAREER_FIELD) * IDENTITY_FIELD_STRIDE
                + Mth.clamp(selection, 0, IDENTITY_FIELD_STRIDE - 1);
    }

    private int traitIndex(CatTraitType trait) {
        for (int i = 0; i < traitCatalog.size(); i++) if (traitCatalog.get(i).id().equals(trait.id())) return i;
        return -1;
    }

    private void load(CatFilterRules rules) {
        for (CatStat stat : CatStat.values()) {
            for (int page = CatFilterRules.CURRENT_PAGE;
                 page <= CatFilterRules.POTENTIAL_PAGE; page++) {
                ranges.set(index(page, false, stat), rules.min(page, stat));
                ranges.set(index(page, true, stat), rules.max(page, stat));
            }
        }
        List<CatTraitType> traits = rules.requiredTraits();
        for (int slot = 0; slot < MAX_REQUIRED_TRAITS; slot++) {
            ranges.set(TRAIT_SELECTION_START + slot, slot < traits.size()
                    ? traitIndex(traits.get(slot)) + 1 : 0);
        }
        ranges.set(GROWTH_FILTER_INDEX, rules.growth().ordinal());
        ranges.set(OWNERSHIP_FILTER_INDEX, rules.ownership().ordinal());
        ranges.set(CAREER_FILTER_INDEX, rules.career().ordinal());
        nameQuery = rules.catName();
        ranges.set(CURRENT_ENABLED_INDEX, rules.logic().currentMask());
        ranges.set(LIMIT_ENABLED_INDEX, rules.logic().limitMask());
        ranges.set(LOGIC_FLAGS_INDEX, rules.logic().flags());
        ranges.set(BASE_CURRENT_INDEX, rules.baseCurrent() ? 1 : 0);
    }

    public CatFilterRules rules() {
        int[] currentMin = new int[CatFilterRules.STAT_COUNT];
        int[] currentMax = new int[CatFilterRules.STAT_COUNT];
        int[] potentialMin = new int[CatFilterRules.STAT_COUNT];
        int[] potentialMax = new int[CatFilterRules.STAT_COUNT];
        for (CatStat stat : CatStat.values()) {
            int statIndex = stat.ordinal();
            currentMin[statIndex] = min(CatFilterRules.CURRENT_PAGE, stat);
            currentMax[statIndex] = max(CatFilterRules.CURRENT_PAGE, stat);
            potentialMin[statIndex] = min(CatFilterRules.POTENTIAL_PAGE, stat);
            potentialMax[statIndex] = max(CatFilterRules.POTENTIAL_PAGE, stat);
        }
        return CatFilterRules.fromValues(currentMin, currentMax,
                potentialMin, potentialMax, selectedTraits(),
                CatFilterRules.GrowthFilter.values()[Mth.clamp(growthSelection(), 0,
                        CatFilterRules.GrowthFilter.values().length - 1)],
                CatFilterRules.OwnershipFilter.values()[Mth.clamp(ownershipSelection(), 0,
                        CatFilterRules.OwnershipFilter.values().length - 1)],
                CatFilterRules.CareerFilter.values()[Mth.clamp(careerSelection(), 0,
                        CatFilterRules.CareerFilter.values().length - 1)],
                nameQuery).withLogic(new CatFilterLogic(ranges.get(CURRENT_ENABLED_INDEX),
                        ranges.get(LIMIT_ENABLED_INDEX), ranges.get(LOGIC_FLAGS_INDEX)))
                .withBaseCurrent(baseCurrent());
    }

    private static int index(int page, boolean maximum, CatStat stat) {
        int start;
        if (page == CatFilterRules.POTENTIAL_PAGE) {
            start = maximum ? POTENTIAL_MAX_START : POTENTIAL_MIN_START;
        } else {
            start = maximum ? CURRENT_MAX_START : CURRENT_MIN_START;
        }
        return start + stat.ordinal();
    }
}
