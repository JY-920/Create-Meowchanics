package cn.laowu.mod;

import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatAttributeProfile;
import cn.laowu.mod.genetics.CatStat;
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

/** Server-authoritative development menu for editing a cat's saved loci. */
public final class CatAttributeEditorMenu extends AbstractContainerMenu {
    private static final int STAT_COUNT = CatStat.values().length;
    private static final int DATA_COUNT = STAT_COUNT * 2;

    private final UUID targetId;
    private final ContainerData data;
    private final Cat viewedCat;
    private boolean viewLockReleased;

    public CatAttributeEditorMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readUUID(), null);
    }

    public CatAttributeEditorMenu(int id, Inventory inventory, Cat target) {
        this(id, inventory, target.getUUID(), target);
    }

    public CatAttributeEditorMenu(int id, Inventory inventory, ItemEntity target) {
        this(id, inventory, target.getUUID(), target);
    }

    private CatAttributeEditorMenu(int id, Inventory inventory, UUID targetId, Entity target) {
        super(LaoWuMod.CAT_ATTRIBUTE_EDITOR_MENU.get(), id);
        this.targetId = targetId;
        this.viewedCat = target instanceof Cat cat ? cat : null;
        if (target == null) {
            this.data = new SimpleContainerData(DATA_COUNT);
        } else {
            this.data = new ContainerData() {
                @Override
                public int get(int index) {
                    CatAttributeProfile profile = readProfile(target);
                    CatStat stat = CatStat.values()[index % STAT_COUNT];
                    return index < STAT_COUNT ? profile.current(stat) : profile.potential(stat);
                }

                @Override public void set(int index, int value) {}
                @Override public int getCount() { return DATA_COUNT; }
            };
        }
        addDataSlots(data);
        if (viewedCat != null) CatProfileData.beginViewing(viewedCat);
    }

    public int current(CatStat stat) {
        return data.get(stat.ordinal());
    }

    public int potential(CatStat stat) {
        return data.get(STAT_COUNT + stat.ordinal());
    }

    @Override
    public boolean clickMenuButton(Player player, int encodedId) {
        if (player.level().isClientSide) return true;
        Entity target = resolveTarget(player);
        if (target == null) return false;

        int step = encodedId >= 100 ? 10 : 1;
        int id = encodedId % 100;
        int band = id / 10;
        int statIndex = id % 10;
        if (band < 0 || band > 3 || statIndex < 0 || statIndex >= STAT_COUNT) return false;

        CatStat stat = CatStat.values()[statIndex];
        CatAttributeProfile profile = readProfile(target);
        int current = profile.current(stat);
        int potential = profile.potential(stat);
        switch (band) {
            case 0 -> current = Math.max(CatAttributeProfile.MIN_VALUE, current - step);
            case 1 -> current = Math.min(potential, current + step);
            case 2 -> potential = Math.max(current, potential - step);
            case 3 -> potential = Math.min(CatAttributeProfile.MAX_VALUE, potential + step);
            default -> { return false; }
        }
        writeProfile(target, profile.withValues(stat, current, potential));
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

    private static CatAttributeProfile readProfile(Entity target) {
        if (target instanceof Cat cat) return CatAttributeData.ensure(cat);
        ItemEntity itemEntity = (ItemEntity) target;
        return CatAttributeData.ensure(itemEntity.getItem(), itemEntity.level().random);
    }

    private static void writeProfile(Entity target, CatAttributeProfile profile) {
        if (target instanceof Cat cat) {
            CatAttributeData.set(cat, profile);
            ModNetwork.syncCatAttributesToTracking(cat);
            return;
        }
        ItemEntity itemEntity = (ItemEntity) target;
        ItemStack edited = itemEntity.getItem().copy();
        CatAttributeData.set(edited, profile);
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
