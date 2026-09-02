package cn.laowu.mod;

import cn.laowu.mod.network.ModNetwork;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * A pair of seated container cats acts as a living, two-way package port.
 * Powered packagers load a cat; an equally addressed cat swaps endpoints with it
 * and an unpowered packager at the destination unpacks the delivered parcels.
 */
public final class CatLogisticsBehavior {
    private static final String PHASE_TAG = "LaoWuLogisticsPhase";
    private static final String TICKS_TAG = "LaoWuLogisticsTicks";
    private static final String PARTNER_TAG = "LaoWuLogisticsPartner";
    private static final String SOURCE_SEAT_TAG = "LaoWuLogisticsSourceSeat";
    private static final String DESTINATION_SEAT_TAG = "LaoWuLogisticsDestinationSeat";
    private static final String ASCENT_TICKS_TAG = "LaoWuLogisticsAscentTicks";
    private static final String CRUISE_Y_TAG = "LaoWuLogisticsCruiseY";
    private static final String LOAD_STACK_TAG = "LaoWuLogisticsLoadStack";
    private static final String OLD_LOAD_ENTITY_TAG = "LaoWuLogisticsLoadEntity";
    private static final String LOAD_TICKS_TAG = "LaoWuLogisticsLoadTicks";
    private static final String LOAD_SEAT_TAG = "LaoWuLogisticsLoadSeat";

    private static final int IDLE = 0;
    private static final int WAITING = 1;
    private static final int HOVERING = 2;
    private static final int FALLING = 3;
    private static final int UNLOADING = 4;
    private static final int PACKAGE_SLOTS = CatPackageMenu.PACKAGE_SLOTS;
    private static final int BATCH_WAIT_TICKS = 60;
    /** High enough to become effectively invisible even with a long entity render distance. */
    private static final double CRUISE_CLEARANCE = 220.0D;
    private static final int ASCENT_TICKS = 40;
    private static final int HIGH_ALTITUDE_WAIT_TICKS = 0;
    private static final int PACKAGE_LOAD_TICKS = 12;

    public static boolean tick(Cat cat) {
        if (!(cat.level() instanceof ServerLevel level) || !CatChestData.hasChest(cat)) return false;
        if (HissingCatBehavior.isHissingForbidden(cat) && CatPoseData.isHissing(cat)) {
            setHissing(cat, false);
            ModNetwork.setAudioSession(cat, false);
        }
        migrateOldLoadAnimation(level, cat);
        if (tickPackageLoadAnimation(level, cat)) return true;
        CompoundTag data = cat.getPersistentData();
        int phase = data.getInt(PHASE_TAG);

        if (phase == IDLE || phase == WAITING) {
            BlockPos seat = findSeat(cat);
            if (seat != null && (cat.isPassenger() || cat.isInSittingPose())) {
                pullFromPoweredPackager(cat, seat);
                if (data.contains(LOAD_STACK_TAG, Tag.TAG_COMPOUND)) return true;
                if (phase == IDLE && hasPackage(cat) && !CatChestData.getAddress(cat).isBlank()) {
                    startWaiting(cat, seat);
                    phase = WAITING;
                }
            }
        }

        return switch (phase) {
            case WAITING -> { tickWaiting(level, cat); yield true; }
            case HOVERING -> { tickHovering(level, cat); yield true; }
            case FALLING -> { tickFalling(cat); yield true; }
            case UNLOADING -> { tickUnloading(cat); yield true; }
            default -> false;
        };
    }

    public static boolean isActive(Cat cat) {
        return cat.getPersistentData().getInt(PHASE_TAG) != IDLE
                || cat.getPersistentData().contains(LOAD_STACK_TAG, Tag.TAG_COMPOUND)
                || cat.getPersistentData().hasUUID(OLD_LOAD_ENTITY_TAG);
    }

    public static void abort(Cat cat) {
        if (cat.level() instanceof ServerLevel level) releaseAnimatedPackage(level, cat);
    }

    public static void cancelForPancake(Cat cat) {
        abort(cat);
        if (isActive(cat)) finish(cat, false);
    }

    /** Called by the live cat inventory whenever manual or automated insertion changes it. */
    public static void onInventoryChanged(Cat cat) {
        CompoundTag data = cat.getPersistentData();
        int phase = data.getInt(PHASE_TAG);
        if (phase == HOVERING || phase == FALLING || phase == UNLOADING || !hasPackage(cat)
                || CatChestData.getAddress(cat).isBlank()) return;
        BlockPos seat = findSeat(cat);
        if (seat != null && (cat.isPassenger() || cat.isInSittingPose())) {
            startWaiting(cat, seat); // Every new parcel extends the three-second batching window.
        }
    }

    private static void startWaiting(Cat cat, BlockPos seat) {
        HissingCatBehavior.pauseForLogistics(cat);
        CompoundTag data = cat.getPersistentData();
        data.putInt(PHASE_TAG, WAITING);
        data.putInt(TICKS_TAG, BATCH_WAIT_TICKS);
        data.putLong(SOURCE_SEAT_TAG, seat.asLong());
        setHissing(cat, true);
        cat.getNavigation().stop();
        cat.setOrderedToSit(true);
        cat.setInSittingPose(true);
    }

    private static void tickWaiting(ServerLevel level, Cat cat) {
        CompoundTag data = cat.getPersistentData();
        BlockPos seat = findSeat(cat);
        if (seat == null || CatChestData.getAddress(cat).isBlank() || !hasPackage(cat)) {
            finish(cat, seat != null);
            return;
        }

        cat.getNavigation().stop();
        cat.setOrderedToSit(true);
        cat.setInSittingPose(true);
        pullFromPoweredPackager(cat, seat);
        int remaining = data.getInt(TICKS_TAG);
        if (remaining > 0) {
            data.putInt(TICKS_TAG, remaining - 1);
            return;
        }

        Cat partner = findPartner(level, cat, seat);
        if (partner == null) {
            // Keep the parcel ready and retry once a second without replaying the batching delay.
            data.putInt(TICKS_TAG, 20);
            return;
        }
        BlockPos partnerSeat = findSeat(partner);
        if (partnerSeat != null) beginFlight(cat, seat, partner, partnerSeat);
    }

    private static Cat findPartner(ServerLevel level, Cat cat, BlockPos ownSeat) {
        String address = CatChestData.getAddress(cat);
        Cat onlyOther = null;
        int matchingNames = 0;
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Cat other) || !other.isAlive()
                    || !CatChestData.hasChest(other)
                    || !address.equals(CatChestData.getAddress(other))) continue;
            matchingNames++;
            if (matchingNames > 2) return null;
            if (other == cat) continue;
            onlyOther = other;
        }
        if (matchingNames != 2 || onlyOther == null) return null;

        Cat other = onlyOther;
        int phase = other.getPersistentData().getInt(PHASE_TAG);
        if (phase != IDLE && phase != WAITING) return null;
        BlockPos otherSeat = findSeat(other);
        if (otherSeat == null || otherSeat.equals(ownSeat)
                || (!other.isPassenger() && !other.isInSittingPose())) return null;
        return other;
    }

    private static void beginFlight(Cat first, BlockPos firstSeat, Cat second, BlockPos secondSeat) {
        HissingCatBehavior.pauseForLogistics(first);
        HissingCatBehavior.pauseForLogistics(second);
        double cruiseY = Math.max(firstSeat.getY(), secondSeat.getY()) + CRUISE_CLEARANCE;
        configureFlight(first, second.getUUID(), firstSeat, secondSeat,
                HIGH_ALTITUDE_WAIT_TICKS, cruiseY);
        configureFlight(second, first.getUUID(), secondSeat, firstSeat,
                HIGH_ALTITUDE_WAIT_TICKS, cruiseY);
        ModNetwork.setAudioSession(first, false);
        ModNetwork.setAudioSession(second, false);
        if (first.level() instanceof ServerLevel level) {
            ModNetwork.playLogisticsSound(level, firstSeat, false);
            ModNetwork.playLogisticsSound(level, secondSeat, false);
        }
    }

    private static void configureFlight(Cat cat, UUID partner, BlockPos source, BlockPos destination,
                                        int travelTicks, double cruiseY) {
        cat.stopRiding();
        cat.getNavigation().stop();
        // FollowOwnerGoal checks this flag before using the tame-animal teleport shortcut.
        cat.setOrderedToSit(true);
        cat.setInSittingPose(false);
        cat.setNoGravity(true);
        cat.fallDistance = 0.0F;
        cat.setDeltaMovement(0.0D, 0.0D, 0.0D);
        cat.setPos(source.getX() + 0.5D, source.getY() + 1.0D, source.getZ() + 0.5D);
        CompoundTag data = cat.getPersistentData();
        data.putInt(PHASE_TAG, HOVERING);
        data.putInt(TICKS_TAG, travelTicks);
        data.putInt(ASCENT_TICKS_TAG, ASCENT_TICKS);
        data.putDouble(CRUISE_Y_TAG, cruiseY);
        data.putUUID(PARTNER_TAG, partner);
        data.putLong(SOURCE_SEAT_TAG, source.asLong());
        data.putLong(DESTINATION_SEAT_TAG, destination.asLong());
        setHissing(cat, true);
    }

    private static void tickHovering(ServerLevel level, Cat cat) {
        CompoundTag data = cat.getPersistentData();
        Entity entity = data.hasUUID(PARTNER_TAG) ? level.getEntity(data.getUUID(PARTNER_TAG)) : null;
        if (!(entity instanceof Cat partner) || !partner.isAlive()) {
            startFalling(cat, BlockPos.of(data.getLong(SOURCE_SEAT_TAG)));
            return;
        }

        BlockPos source = BlockPos.of(data.getLong(SOURCE_SEAT_TAG));
        cat.getNavigation().stop();
        cat.setOrderedToSit(true);
        cat.setNoGravity(true);
        cat.fallDistance = 0.0F;
        cat.setDeltaMovement(0.0D, 0.0D, 0.0D);
        int ascent = data.getInt(ASCENT_TICKS_TAG);
        double cruiseY = data.getDouble(CRUISE_Y_TAG);
        if (ascent > 0) {
            double progress = 1.0D - (ascent - 1.0D) / ASCENT_TICKS;
            double y = source.getY() + 1.0D + (cruiseY - source.getY() - 1.0D) * progress;
            cat.setPos(source.getX() + 0.5D, y, source.getZ() + 0.5D);
            data.putInt(ASCENT_TICKS_TAG, ascent - 1);
            return;
        }
        cat.setPos(source.getX() + 0.5D,
                cruiseY + Math.sin(cat.tickCount * 0.18D) * 0.12D,
                source.getZ() + 0.5D);

        int remaining = data.getInt(TICKS_TAG) - 1;
        data.putInt(TICKS_TAG, remaining);
        if (remaining > 0) return;

        BlockPos destination = BlockPos.of(data.getLong(DESTINATION_SEAT_TAG));
        startFalling(cat, destination);
        if (partner.getPersistentData().getInt(PHASE_TAG) == HOVERING) {
            BlockPos partnerDestination = BlockPos.of(
                    partner.getPersistentData().getLong(DESTINATION_SEAT_TAG));
            startFalling(partner, partnerDestination);
        }
    }

    private static void startFalling(Cat cat, BlockPos destination) {
        cat.stopRiding();
        cat.setOrderedToSit(true);
        cat.setNoGravity(false);
        cat.fallDistance = 0.0F;
        cat.setInSittingPose(false);
        double cruiseY = cat.getPersistentData().getDouble(CRUISE_Y_TAG);
        cat.setPos(destination.getX() + 0.5D, cruiseY,
                destination.getZ() + 0.5D);
        cat.setDeltaMovement(0.0D, -0.08D, 0.0D);
        CompoundTag data = cat.getPersistentData();
        data.putInt(PHASE_TAG, FALLING);
        data.putInt(TICKS_TAG, 300);
        data.putLong(DESTINATION_SEAT_TAG, destination.asLong());
    }

    private static void tickFalling(Cat cat) {
        CompoundTag data = cat.getPersistentData();
        BlockPos destination = BlockPos.of(data.getLong(DESTINATION_SEAT_TAG));
        cat.getNavigation().stop();
        cat.setOrderedToSit(true);
        cat.fallDistance = 0.0F;
        cat.setDeltaMovement(0.0D, cat.getDeltaMovement().y, 0.0D);
        int timeout = data.getInt(TICKS_TAG) - 1;
        data.putInt(TICKS_TAG, timeout);
        if (cat.getY() > destination.getY() + 1.15D && timeout > 0) return;

        if (cat.level() instanceof ServerLevel level) {
            ModNetwork.playLogisticsSound(level, destination, true);
        }
        mountSeat(cat, destination);
        data.putInt(PHASE_TAG, UNLOADING);
        data.putLong(DESTINATION_SEAT_TAG, destination.asLong());
        cat.setOrderedToSit(true);
        cat.setInSittingPose(true);
    }

    private static void tickUnloading(Cat cat) {
        CompoundTag data = cat.getPersistentData();
        BlockPos destination = BlockPos.of(data.getLong(DESTINATION_SEAT_TAG));
        cat.getNavigation().stop();
        cat.setOrderedToSit(true);
        cat.setInSittingPose(true);

        if (!hasPackage(cat)) {
            finish(cat, true);
            return;
        }
        BlockPos currentSeat = findSeat(cat);
        if (currentSeat == null || !currentSeat.equals(destination)) return;
        deliverOneToUnpoweredPackager(cat, destination);
        if (!hasPackage(cat)) finish(cat, true);
    }

    private static void pullFromPoweredPackager(Cat cat, BlockPos seat) {
        if (cat.getPersistentData().contains(LOAD_STACK_TAG, Tag.TAG_COMPOUND)) return;
        if (!(cat.level().getBlockEntity(seat.below()) instanceof PackagerBlockEntity packager)
                || !packager.redstonePowered) return;
        ItemStack simulated = packager.inventory.extractItem(0, 1, true);
        if (simulated.isEmpty() || !PackageItem.isPackage(simulated)) return;
        CatChestContainer inventory = CatChestData.openContainer(cat);
        int target = firstEmptySlot(inventory);
        if (target < 0) return;
        ItemStack extracted = packager.inventory.extractItem(0, 1, false);
        if (extracted.isEmpty()) return;
        if (!(cat.level() instanceof ServerLevel level)) return;
        CompoundTag data = cat.getPersistentData();
        data.put(LOAD_STACK_TAG, extracted.save(level.registryAccess()));
        data.putInt(LOAD_TICKS_TAG, PACKAGE_LOAD_TICKS);
        data.putLong(LOAD_SEAT_TAG, seat.asLong());
        ModNetwork.startPackageLoadAnimation(level, cat.getId(), seat, extracted, PACKAGE_LOAD_TICKS);
        cat.setOrderedToSit(true);
        cat.getNavigation().stop();
    }

    private static boolean tickPackageLoadAnimation(ServerLevel level, Cat cat) {
        CompoundTag data = cat.getPersistentData();
        if (!data.contains(LOAD_STACK_TAG, Tag.TAG_COMPOUND)) return false;
        BlockPos seat = BlockPos.of(data.getLong(LOAD_SEAT_TAG));
        BlockPos currentSeat = findSeat(cat);
        if (currentSeat == null || !currentSeat.equals(seat) || !cat.isAlive()) {
            releaseAnimatedPackage(level, cat);
            return false;
        }

        cat.setOrderedToSit(true);
        cat.getNavigation().stop();
        int remaining = data.getInt(LOAD_TICKS_TAG) - 1;
        data.putInt(LOAD_TICKS_TAG, remaining);
        if (remaining > 0) return true;

        CatChestContainer inventory = CatChestData.openContainer(cat);
        int target = firstEmptySlot(inventory);
        if (target < 0) {
            releaseAnimatedPackage(level, cat);
            return false;
        }
        ItemStack parcel = ItemStack.parseOptional(level.registryAccess(),
                data.getCompound(LOAD_STACK_TAG));
        clearLoadAnimation(data);
        inventory.setItem(target, parcel);
        startWaiting(cat, seat);
        return false;
    }

    private static void releaseAnimatedPackage(ServerLevel level, Cat cat) {
        CompoundTag data = cat.getPersistentData();
        if (data.hasUUID(OLD_LOAD_ENTITY_TAG)) {
            Entity entity = level.getEntity(data.getUUID(OLD_LOAD_ENTITY_TAG));
            if (entity instanceof ItemEntity oldAnimated && oldAnimated.isAlive()) {
                oldAnimated.setInvulnerable(false);
                oldAnimated.setNoGravity(false);
                oldAnimated.setDefaultPickUpDelay();
            }
        }
        if (data.contains(LOAD_STACK_TAG, Tag.TAG_COMPOUND)) {
            ItemStack parcel = ItemStack.parseOptional(level.registryAccess(),
                    data.getCompound(LOAD_STACK_TAG));
            if (!parcel.isEmpty()) {
                ItemEntity dropped = new ItemEntity(level, cat.getX(), cat.getY() + 0.4D,
                        cat.getZ(), parcel);
                dropped.setDefaultPickUpDelay();
                level.addFreshEntity(dropped);
            }
        }
        clearLoadAnimation(data);
    }

    private static void migrateOldLoadAnimation(ServerLevel level, Cat cat) {
        CompoundTag data = cat.getPersistentData();
        if (!data.hasUUID(OLD_LOAD_ENTITY_TAG)) return;
        Entity entity = level.getEntity(data.getUUID(OLD_LOAD_ENTITY_TAG));
        if (entity instanceof ItemEntity oldAnimated && oldAnimated.isAlive()) {
            ItemStack parcel = oldAnimated.getItem().copy();
            oldAnimated.discard();
            if (!parcel.isEmpty()) {
                data.put(LOAD_STACK_TAG, parcel.save(level.registryAccess()));
                data.putInt(LOAD_TICKS_TAG, PACKAGE_LOAD_TICKS);
                BlockPos seat = BlockPos.of(data.getLong(LOAD_SEAT_TAG));
                ModNetwork.startPackageLoadAnimation(level, cat.getId(), seat,
                        parcel, PACKAGE_LOAD_TICKS);
            }
        }
        data.remove(OLD_LOAD_ENTITY_TAG);
    }

    private static void clearLoadAnimation(CompoundTag data) {
        data.remove(LOAD_STACK_TAG);
        data.remove(OLD_LOAD_ENTITY_TAG);
        data.remove(LOAD_TICKS_TAG);
        data.remove(LOAD_SEAT_TAG);
    }

    private static boolean deliverOneToUnpoweredPackager(Cat cat, BlockPos seat) {
        if (!(cat.level().getBlockEntity(seat.below()) instanceof PackagerBlockEntity packager)
                || packager.redstonePowered) return false;
        CatChestContainer inventory = CatChestData.openContainer(cat);
        for (int slot = 0; slot < PACKAGE_SLOTS; slot++) {
            ItemStack parcel = inventory.getItem(slot);
            if (parcel.isEmpty() || !PackageItem.isPackage(parcel)) continue;
            ItemStack remainder = packager.inventory.insertItem(0, parcel.copy(), false);
            if (remainder.getCount() == parcel.getCount()) return false;
            inventory.setItem(slot, remainder);
            return true;
        }
        return false;
    }

    private static int firstEmptySlot(CatChestContainer inventory) {
        for (int slot = 0; slot < PACKAGE_SLOTS; slot++) {
            if (inventory.getItem(slot).isEmpty()) return slot;
        }
        return -1;
    }

    private static boolean hasPackage(Cat cat) {
        CatChestContainer inventory = CatChestData.openContainer(cat);
        for (int slot = 0; slot < PACKAGE_SLOTS; slot++) {
            if (PackageItem.isPackage(inventory.getItem(slot))) return true;
        }
        return false;
    }

    private static BlockPos findSeat(Cat cat) {
        if (cat.getVehicle() instanceof SeatEntity seatEntity) {
            BlockPos pos = seatEntity.blockPosition();
            if (cat.level().getBlockState(pos).getBlock() instanceof SeatBlock) return pos;
        }
        BlockPos[] candidates = {cat.blockPosition(), cat.blockPosition().below(), cat.getOnPos()};
        for (BlockPos pos : candidates) {
            if (cat.level().getBlockState(pos).getBlock() instanceof SeatBlock) return pos.immutable();
        }
        return null;
    }

    private static void mountSeat(Cat cat, BlockPos seat) {
        if (!(cat.level().getBlockState(seat).getBlock() instanceof SeatBlock)) {
            cat.setPos(seat.getX() + 0.5D, seat.getY() + 1.0D, seat.getZ() + 0.5D);
            cat.setInSittingPose(true);
            return;
        }
        cat.setPos(seat.getX() + 0.5D, seat.getY() + 0.5D, seat.getZ() + 0.5D);
        var seats = cat.level().getEntitiesOfClass(SeatEntity.class, new AABB(seat));
        if (!seats.isEmpty()) {
            SeatEntity entity = seats.get(0);
            entity.ejectPassengers();
            cat.startRiding(entity, true);
            cat.setInSittingPose(true);
        } else {
            SeatBlock.sitDown(cat.level(), seat, cat);
        }
    }

    private static void finish(Cat cat, boolean keepSitting) {
        ModNetwork.setAudioSession(cat, false);
        cat.setNoGravity(false);
        cat.fallDistance = 0.0F;
        cat.getNavigation().stop();
        cat.setOrderedToSit(keepSitting);
        cat.setInSittingPose(keepSitting);
        CompoundTag data = cat.getPersistentData();
        data.remove(PHASE_TAG);
        data.remove(TICKS_TAG);
        data.remove(PARTNER_TAG);
        data.remove(SOURCE_SEAT_TAG);
        data.remove(DESTINATION_SEAT_TAG);
        data.remove(ASCENT_TICKS_TAG);
        data.remove(CRUISE_Y_TAG);
        setHissing(cat, false);
    }

    private static void setHissing(Cat cat, boolean active) {
        if (active && HissingCatBehavior.isHissingForbidden(cat)) active = false;
        int pose = active ? 1 : 0;
        if (CatPoseData.getPose(cat) == pose) return;
        CatPoseData.setPose(cat, pose);
        ModNetwork.syncToTracking(cat, pose);
    }

    private CatLogisticsBehavior() {}
}
