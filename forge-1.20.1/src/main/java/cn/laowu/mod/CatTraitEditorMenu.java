package cn.laowu.mod;

import cn.laowu.mod.genetics.CatTrait;
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
    private static final int TRAIT_COUNT = CatTrait.values().length;
    private static final int ACCELERATED_OFFSET = 1_000;
    private final UUID targetId;
    private final ContainerData data;
    private final Cat viewedCat;
    private boolean viewLockReleased;

    public CatTraitEditorMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readUUID(), null);
    }

    public CatTraitEditorMenu(int id, Inventory inventory, Cat target) {
        this(id, inventory, target.getUUID(), target);
    }

    public CatTraitEditorMenu(int id, Inventory inventory, ItemEntity target) {
        this(id, inventory, target.getUUID(), target);
    }

    private CatTraitEditorMenu(int id, Inventory inventory, UUID targetId, Entity target) {
        super(LaoWuMod.CAT_TRAIT_EDITOR_MENU.get(), id);
        this.targetId = targetId;
        this.viewedCat = target instanceof Cat cat ? cat : null;
        if (target == null) {
            this.data = new SimpleContainerData(TRAIT_COUNT);
        } else {
            this.data = new ContainerData() {
                @Override
                public int get(int index) {
                    return readProfile(target).level(CatTrait.values()[index]);
                }

                @Override public void set(int index, int value) {}
                @Override public int getCount() { return TRAIT_COUNT; }
            };
        }
        addDataSlots(data);
        if (viewedCat != null) CatProfileData.beginViewing(viewedCat);
    }

    public int level(CatTrait trait) {
        return data.get(trait.ordinal());
    }

    @Override
    public boolean clickMenuButton(Player player, int encodedId) {
        if (player.level().isClientSide) return true;
        Entity target = resolveTarget(player);
        if (target == null) return false;

        boolean accelerated = encodedId >= ACCELERATED_OFFSET;
        int rawId = encodedId % ACCELERATED_OFFSET;
        int action = rawId / TRAIT_COUNT;
        int traitIndex = rawId % TRAIT_COUNT;
        if (action < 0 || action > 1 || traitIndex < 0 || traitIndex >= TRAIT_COUNT) {
            return false;
        }

        CatTrait trait = CatTrait.values()[traitIndex];
        CatTraitProfile profile = readProfile(target);
        int current = profile.level(trait);
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
