package cn.laowu.mod;

import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitType;
import cn.laowu.mod.genetics.CatTraitRegistry;
import java.util.List;
import java.util.ArrayList;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTraitProfile;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Server-authoritative development menu for adding, removing and levelling traits. */
public final class CatTraitEditorMenu extends AbstractContainerMenu {
    private final int TRAIT_COUNT;
    private final List<CatTraitType> catalog;
    private final long registryRevision;
    private static final int ACCELERATED_OFFSET = 1_000;
    private final UUID targetId;
    private final ContainerData data;
    private final Cat viewedCat;
    private boolean viewLockReleased;

    public CatTraitEditorMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readUUID(), null, readCatalog(buffer));
    }

    public CatTraitEditorMenu(int id, Inventory inventory, Cat target) {
        this(id, inventory, target.getUUID(), target, null);
    }

    public CatTraitEditorMenu(int id, Inventory inventory, ItemEntity target) {
        this(id, inventory, target.getUUID(), target, null);
    }

    private CatTraitEditorMenu(int id, Inventory inventory, UUID targetId, Entity target, List<CatTraitType> fromServer) {
        super(LaoWuMod.CAT_TRAIT_EDITOR_MENU.get(), id);
        this.targetId = targetId;
        this.catalog = fromServer == null ? catalogFor(target) : fromServer;
        this.TRAIT_COUNT = catalog.size();
        this.registryRevision = CatTraitRegistry.revision(inventory.player.level().isClientSide);
        this.viewedCat = target instanceof Cat cat ? cat : null;
        if (target == null) {
            this.data = new SimpleContainerData(TRAIT_COUNT);
        } else {
            this.data = new ContainerData() {
                @Override
                public int get(int index) {
                    return readProfile(target).rawLevel(catalog.get(index));
                }

                @Override public void set(int index, int value) {}
                @Override public int getCount() { return TRAIT_COUNT; }
            };
        }
        addDataSlots(data);
        if (viewedCat != null) CatProfileData.beginViewing(viewedCat);
    }

    public int level(CatTraitType trait) {
        for (int i = 0; i < catalog.size(); i++)
            if (catalog.get(i).id().equals(trait.id())) return data.get(i);
        return 0;
    }
    public List<CatTraitType> catalog() { return catalog; }
    public int action(CatTraitType trait, boolean increase, boolean accelerated) {
        for (int i = 0; i < catalog.size(); i++)
            if (catalog.get(i).id().equals(trait.id())) return i + (increase ? TRAIT_COUNT : 0)
                    + (accelerated ? ACCELERATED_OFFSET : 0);
        return -1;
    }
    private static List<CatTraitType> catalogFor(Entity target) {
        var values = new ArrayList<>(CatTraitRegistry.values(false));
        if (target != null) for (var saved : readProfile(target).traits())
            if (values.stream().noneMatch(type -> type.id().equals(saved.trait().id()))) values.add(saved.trait());
        return List.copyOf(values);
    }
    public static void writeOpeningData(FriendlyByteBuf buffer, Entity target) {
        buffer.writeUUID(target.getUUID());
        var values = catalogFor(target);
        buffer.writeVarInt(values.size());
        for (var trait : values) buffer.writeUtf(trait.id().toString(), 128);
    }
    private static List<CatTraitType> readCatalog(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 1 || count > CatTrait.values().length + CatTraitRegistry.MAX_CUSTOM_TRAITS + 4)
            throw new IllegalArgumentException("Invalid trait editor catalog length");
        var values = new ArrayList<CatTraitType>();
        var seen = new java.util.HashSet<net.minecraft.resources.ResourceLocation>();
        for (int i = 0; i < count; i++) {
            var id = net.minecraft.resources.ResourceLocation.tryParse(buffer.readUtf(128));
            if (id == null || !seen.add(id)) throw new IllegalArgumentException("Invalid/duplicate trait editor ID");
            values.add(CatTraitRegistry.resolve(id, true));
        }
        return List.copyOf(values);
    }

    @Override
    public boolean clickMenuButton(Player player, int encodedId) {
        if (player.level().isClientSide) return true;
        if (!stillValid(player) || encodedId < 0
                || encodedId >= ACCELERATED_OFFSET + TRAIT_COUNT * 2) return false;
        Entity target = resolveTarget(player);
        if (target == null) return false;

        boolean accelerated = encodedId >= ACCELERATED_OFFSET;
        int rawId = encodedId % ACCELERATED_OFFSET;
        int action = rawId / TRAIT_COUNT;
        int traitIndex = rawId % TRAIT_COUNT;
        if (action < 0 || action > 1 || traitIndex < 0 || traitIndex >= TRAIT_COUNT) {
            return false;
        }

        CatTraitType trait = catalog.get(traitIndex);
        CatTraitProfile profile = readProfile(target);
        int current = profile.rawLevel(trait);
        int next;
        if (action == 0) {
            next = accelerated ? 0 : Math.max(0, current - 1);
        } else if (current == 0) {
            next = 1;
        } else if (trait.upgradable()) {
            next = accelerated ? trait.maxLevel() : Math.min(trait.maxLevel(), current + 1);
        } else {
            next = 1;
        }

        writeProfile(target, profile.withLevel(trait, next));
        broadcastChanges();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().isClientSide) return true;
        if (registryRevision != CatTraitRegistry.revision(false)) return false;
        Entity target = resolveTarget(player);
        return target != null && target.isAlive()
                && isEditableTarget(target) && player.distanceToSqr(target) <= 64.0D;
    }

    private Entity resolveTarget(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return null;
        Entity entity = level.getEntity(targetId);
        return isEditableTarget(entity) ? entity : null;
    }

    private static boolean isEditableTarget(Entity entity) {
        return entity instanceof Cat
                || entity instanceof ItemEntity itemEntity
                && itemEntity.getItem().is(LaoWuMod.CAT_PANCAKE.get());
    }

    private static CatTraitProfile readProfile(Entity target) {
        if (target instanceof Cat cat) return CatTraitData.ensure(cat);
        ItemEntity itemEntity = (ItemEntity) target;
        return CatTraitData.ensure(itemEntity.getItem(), itemEntity.level().random);
    }

    private static void writeProfile(Entity target, CatTraitProfile profile) {
        if (target instanceof Cat cat) {
            CatTraitData.set(cat, profile);
            ModNetwork.syncCatTraitsToTracking(cat);
            return;
        }
        ItemEntity itemEntity = (ItemEntity) target;
        ItemStack edited = itemEntity.getItem().copy();
        CatTraitData.set(edited, profile);
        itemEntity.setItem(edited);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!viewLockReleased && viewedCat != null) {
            viewLockReleased = true;
            CatProfileData.endViewing(viewedCat);
        }
    }
}
