package cn.laowu.mod;

import cn.laowu.mod.item.CreatureFilterRules;
import cn.laowu.mod.network.ModNetwork;
import com.simibubi.create.content.logistics.filter.AbstractFilterMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import java.util.*;

/** Create-style held-filter draft, bound to the exact stack rather than its item type. */
public final class CreatureFilterMenu extends AbstractFilterMenu {
    public record EntityEntry(ResourceLocation id, Component name, CreatureFilterRules.Category category) {}
    private CreatureFilterRules rules;
    private List<EntityEntry> catalog;
    private boolean closed;

    public CreatureFilterMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readItem());
    }
    public CreatureFilterMenu(int id, Inventory inventory, ItemStack stack) {
        super(LaoWuMod.CREATURE_FILTER_MENU.get(), id, inventory, stack);
        rules = CreatureFilterRules.read(stack);
    }
    public CreatureFilterRules rules() { return rules; }
    public List<EntityEntry> catalog() {
        if (catalog != null) return catalog;
        var entries = new ArrayList<EntityEntry>();
        BuiltInRegistries.ENTITY_TYPE.keySet().stream().sorted().forEach(id -> {
            if (id.equals(ResourceLocation.tryParse("minecraft:player"))) return;
            var type = BuiltInRegistries.ENTITY_TYPE.get(id);
            try {
                // Not added to the world: instantiate once on opening the catalogue to
                // include modded MISC mobs without listing projectiles, boats or players.
                if (type.create(player.level()) instanceof Mob mob)
                    entries.add(new EntityEntry(id, type.getDescription(), CreatureFilterRules.classify(mob)));
            } catch (RuntimeException | LinkageError ignored) {
                // A mod type that cannot be constructed here remains selectable by ID.
            }
        });
        return catalog = List.copyOf(entries);
    }
    @Override public boolean stillValid(Player actor) {
        return !closed && actor == player && (actor.level().isClientSide || actor.isAlive()
                && !actor.isSpectator() && contentHolder.is(LaoWuMod.CREATURE_FILTER.get()) && super.stillValid(actor));
    }
    public boolean configure(int mask, boolean blacklist, List<ResourceLocation> ids) {
        CreatureFilterRules next;
        try { next = CreatureFilterRules.of(mask, blacklist ? CreatureFilterRules.Mode.BLACKLIST : CreatureFilterRules.Mode.WHITELIST, ids); }
        catch (IllegalArgumentException invalid) { return false; }
        return configure(next);
    }
    public boolean configure(CreatureFilterRules next) {
        if (next == null || !stillValid(player)) return false;
        // The native screen compares the held stack every tick. Save only on close.
        rules = next;
        return true;
    }
    @Override protected int getPlayerInventoryXOffset() { return 51; }
    @Override protected int getPlayerInventoryYOffset() { return 125; }
    @Override protected void addFilterSlots() {}
    @Override protected ItemStackHandler createGhostInventory() { return new ItemStackHandler(0); }
    @Override protected void saveData(ItemStack stack) {
        if (rules != null && !player.level().isClientSide && stillValid(player)) {
            rules.write(stack);
            playerInventory.setChanged();
        }
    }
    @Override public void clearContents() {
        configure(rules.isGrouped()?CreatureFilterRules.grouped(List.of()).withEntryMode(rules.mode()):CreatureFilterRules.conditions(rules.mode(), List.of()));
    }
    @Override public void sendClearPacket() {
        ModNetwork.setCreatureFilter(containerId, rules);
    }
    @Override public ItemStack quickMoveStack(Player actor, int index) {
        if (!stillValid(actor) || isInSlot(index)) return ItemStack.EMPTY;
        return super.quickMoveStack(actor, index);
    }
    @Override public void removed(Player actor) {
        if (closed) return;
        super.removed(actor); closed = true;
    }
}
