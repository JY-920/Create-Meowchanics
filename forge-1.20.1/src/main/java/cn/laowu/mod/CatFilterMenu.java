package cn.laowu.mod;

import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.item.CatFilterRules;
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

/** Held-item menu that synchronizes four min/max arrays without custom packets. */
public final class CatFilterMenu extends AbstractFilterMenu {
    private static final int CURRENT_MIN_START = 0;
    private static final int CURRENT_MAX_START = CURRENT_MIN_START + CatFilterRules.STAT_COUNT;
    private static final int POTENTIAL_MIN_START = CURRENT_MAX_START + CatFilterRules.STAT_COUNT;
    private static final int POTENTIAL_MAX_START = POTENTIAL_MIN_START + CatFilterRules.STAT_COUNT;
    public static final int MAX_REQUIRED_TRAITS = 4;
    private static final int TRAIT_SELECTION_START =
            POTENTIAL_MAX_START + CatFilterRules.STAT_COUNT;
    private static final int DATA_COUNT = TRAIT_SELECTION_START + MAX_REQUIRED_TRAITS;

    private static final int RANGE_BUTTON_BASE = 1000;
    private static final int PAGE_STRIDE = 2000;
    private static final int STAT_STRIDE = 250;
    private static final int BOUND_STRIDE = 125;
    private static final int ADD_TRAIT_BUTTON_BASE = 10_000;
    private static final int REMOVE_TRAIT_BUTTON_BASE = 11_000;

    private final SimpleContainerData ranges = new SimpleContainerData(DATA_COUNT);

    public CatFilterMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readItem());
    }

    public CatFilterMenu(int containerId, Inventory inventory, ItemStack filterStack) {
        super(LaoWuMod.CAT_FILTER_MENU.get(), containerId, inventory, filterStack);
        load(CatFilterRules.read(filterStack));
        addDataSlots(ranges);
    }

    @Override
    protected int getPlayerInventoryXOffset() {
        // AttributeFilterMenu uses 51 together with AttributeFilterScreen's
        // -11px window offset. This screen intentionally stays centered, so
        // compensate here to keep item icons on the centered inventory slots.
        return 40;
    }

    @Override
    protected int getPlayerInventoryYOffset() {
        return 147;
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
                    CatFilterRules.MAX_VALUE);
            ranges.set(index(CatFilterRules.POTENTIAL_PAGE, false, stat),
                    CatFilterRules.MIN_VALUE);
            ranges.set(index(CatFilterRules.POTENTIAL_PAGE, true, stat),
                    CatFilterRules.MAX_VALUE);
        }
        for (int slot = 0; slot < MAX_REQUIRED_TRAITS; slot++) {
            ranges.set(TRAIT_SELECTION_START + slot, 0);
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= REMOVE_TRAIT_BUTTON_BASE) {
            int slot = id - REMOVE_TRAIT_BUTTON_BASE;
            if (slot < 0 || slot >= MAX_REQUIRED_TRAITS) return false;
            for (int index = slot; index < MAX_REQUIRED_TRAITS - 1; index++) {
                ranges.set(TRAIT_SELECTION_START + index,
                        ranges.get(TRAIT_SELECTION_START + index + 1));
            }
            ranges.set(TRAIT_SELECTION_START + MAX_REQUIRED_TRAITS - 1, 0);
            return true;
        }
        if (id >= ADD_TRAIT_BUTTON_BASE) {
            int selection = id - ADD_TRAIT_BUTTON_BASE;
            if (selection <= 0 || selection > CatTrait.values().length) return false;
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
                || bound < 0 || bound > 1 || value > CatFilterRules.MAX_VALUE) {
            return false;
        }

        CatStat stat = CatStat.values()[statIndex];
        boolean maximum = bound == 1;
        int minimum = min(page, stat);
        int upper = max(page, stat);
        int clamped = maximum
                ? Mth.clamp(value, minimum, CatFilterRules.MAX_VALUE)
                : Mth.clamp(value, CatFilterRules.MIN_VALUE, upper);
        ranges.set(index(page, maximum, stat), clamped);
        // Do not change the held stack while this screen is open. Create's
        // AbstractFilterScreen treats an NBT change as a replaced filter and
        // closes itself. MenuBase.removed() persists the accumulated values.
        return true;
    }

    public int min(int page, CatStat stat) {
        return ranges.get(index(page, false, stat));
    }

    public int max(int page, CatStat stat) {
        return ranges.get(index(page, true, stat));
    }

    /** Zero means an empty requirement slot; other values are ordinal + 1. */
    public int traitSelection(int slot) {
        return slot < 0 || slot >= MAX_REQUIRED_TRAITS
                ? 0 : ranges.get(TRAIT_SELECTION_START + slot);
    }

    public CatTrait selectedTrait(int slot) {
        int selection = traitSelection(slot);
        return selection <= 0 || selection > CatTrait.values().length
                ? null : CatTrait.values()[selection - 1];
    }

    public List<CatTrait> selectedTraits() {
        List<CatTrait> selected = new ArrayList<>(MAX_REQUIRED_TRAITS);
        for (int slot = 0; slot < MAX_REQUIRED_TRAITS; slot++) {
            CatTrait trait = selectedTrait(slot);
            if (trait != null) selected.add(trait);
        }
        return List.copyOf(selected);
    }

    public static int rangeButton(int page, CatStat stat, boolean maximum, int value) {
        return RANGE_BUTTON_BASE + page * PAGE_STRIDE + stat.ordinal() * STAT_STRIDE
                + (maximum ? BOUND_STRIDE : 0)
                + Mth.clamp(value, CatFilterRules.MIN_VALUE, CatFilterRules.MAX_VALUE);
    }

    public static int addTraitButton(int selection) {
        return ADD_TRAIT_BUTTON_BASE
                + Mth.clamp(selection, 0, CatTrait.values().length);
    }

    public static int removeTraitButton(int slot) {
        return REMOVE_TRAIT_BUTTON_BASE
                + Mth.clamp(slot, 0, MAX_REQUIRED_TRAITS - 1);
    }

    private void load(CatFilterRules rules) {
        for (CatStat stat : CatStat.values()) {
            for (int page = CatFilterRules.CURRENT_PAGE;
                 page <= CatFilterRules.POTENTIAL_PAGE; page++) {
                ranges.set(index(page, false, stat), rules.min(page, stat));
                ranges.set(index(page, true, stat), rules.max(page, stat));
            }
        }
        List<CatTrait> traits = rules.requiredTraits();
        for (int slot = 0; slot < MAX_REQUIRED_TRAITS; slot++) {
            ranges.set(TRAIT_SELECTION_START + slot, slot < traits.size()
                    ? traits.get(slot).ordinal() + 1 : 0);
        }
    }

    private CatFilterRules rules() {
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
                potentialMin, potentialMax, selectedTraits());
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
