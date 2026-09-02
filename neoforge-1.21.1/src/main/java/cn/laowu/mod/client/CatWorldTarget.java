package cn.laowu.mod.client;

import cn.laowu.mod.item.CatPancakeItem;
import cn.laowu.mod.item.ItemCustomData;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Resolves the cat-like object under the crosshair across vanilla entities and
 * Create's non-entity transported-item representations.
 */
@OnlyIn(Dist.CLIENT)
public record CatWorldTarget(@Nullable Cat cat, ItemStack pancake, long identity) {
    private static final double ITEM_PICK_RADIUS = 0.28D;
    private static final double BELT_PICK_RADIUS_SQR = 0.80D * 0.80D;

    public static CatWorldTarget living(Cat cat) {
        return new CatWorldTarget(cat, ItemStack.EMPTY,
                0x1000_0000_0000_0000L ^ Integer.toUnsignedLong(cat.getId()));
    }

    public static CatWorldTarget item(ItemStack stack, long identity) {
        return new CatWorldTarget(null, stack, identity);
    }

    public boolean isLiving() {
        return cat != null;
    }

    @Nullable
    public static CatWorldTarget find(Minecraft minecraft, double reach) {
        if (minecraft.player == null || minecraft.level == null) return null;

        if (minecraft.hitResult instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof Cat cat) {
            return living(cat);
        }

        Vec3 start = minecraft.player.getEyePosition();
        Vec3 end = start.add(minecraft.player.getViewVector(1.0F).scale(reach));
        double obstructionDistanceSqr = minecraft.hitResult == null
                || minecraft.hitResult.getType() == HitResult.Type.MISS
                ? start.distanceToSqr(end)
                : start.distanceToSqr(minecraft.hitResult.getLocation());

        CatWorldTarget dropped = findDroppedPancake(minecraft, start, end,
                obstructionDistanceSqr);
        if (dropped != null) return dropped;

        if (!(minecraft.hitResult instanceof BlockHitResult blockHit)) return null;
        BlockPos hitPos = blockHit.getBlockPos();
        BlockEntity blockEntity = minecraft.level.getBlockEntity(hitPos);

        if (blockEntity instanceof DepotBlockEntity depot) {
            ItemStack held = depot.getHeldItem();
            if (isPancake(held)) {
                return item(held, blockIdentity(hitPos, held, 0x3000_0000_0000_0000L));
            }
        }

        if (blockEntity instanceof BeltBlockEntity segment) {
            CatWorldTarget beltTarget = findBeltPancake(minecraft, segment,
                    start, end, hitPos);
            if (beltTarget != null) return beltTarget;
        }
        return null;
    }

    @Nullable
    private static CatWorldTarget findDroppedPancake(Minecraft minecraft,
                                                       Vec3 start, Vec3 end,
                                                       double obstructionDistanceSqr) {
        AABB search = minecraft.player.getBoundingBox()
                .expandTowards(end.subtract(start)).inflate(1.0D);
        ItemEntity selected = null;
        double closest = obstructionDistanceSqr;
        for (ItemEntity item : minecraft.level.getEntitiesOfClass(ItemEntity.class,
                search, entity -> isPancake(entity.getItem()))) {
            var hit = item.getBoundingBox().inflate(ITEM_PICK_RADIUS).clip(start, end);
            if (hit.isEmpty()) continue;
            double distance = start.distanceToSqr(hit.get());
            if (distance >= closest) continue;
            closest = distance;
            selected = item;
        }
        return selected == null ? null : item(selected.getItem(),
                0x2000_0000_0000_0000L ^ Integer.toUnsignedLong(selected.getId()));
    }

    @Nullable
    private static CatWorldTarget findBeltPancake(Minecraft minecraft,
                                                   BeltBlockEntity segment,
                                                   Vec3 start, Vec3 end,
                                                   BlockPos hitPos) {
        BeltBlockEntity controller = segment.getControllerBE();
        if (controller == null) return null;

        TransportedItemStack selected = null;
        double closest = Double.MAX_VALUE;
        Set<TransportedItemStack> candidates = new HashSet<>(
                controller.getInventory().getTransportedItems());
        TransportedItemStack lazy = controller.getInventory().getLazyClientItem();
        if (lazy != null) candidates.add(lazy);

        float partialTicks = minecraft.getTimer()
                .getGameTimeDeltaPartialTick(false);
        for (TransportedItemStack transported : candidates) {
            if (transported == null || !isPancake(transported.stack)) continue;
            float offset = Mth.lerp(partialTicks, transported.prevBeltPosition,
                    transported.beltPosition);
            Vec3 position = BeltHelper.getVectorForOffset(controller, offset)
                    .add(0.0D, 0.38D, 0.0D);
            // Only accept the item rendered over the segment the player is
            // actually looking at; a long belt may contain many cat pancakes.
            if (Math.abs(position.x - (hitPos.getX() + 0.5D)) > 1.15D
                    || Math.abs(position.z - (hitPos.getZ() + 0.5D)) > 1.15D
                    || Math.abs(position.y - (hitPos.getY() + 0.65D)) > 1.25D) {
                continue;
            }
            double distance = distanceToRaySqr(position, start, end);
            if (distance > BELT_PICK_RADIUS_SQR || distance >= closest) continue;
            closest = distance;
            selected = transported;
        }

        if (selected == null) return null;
        // beltPosition changes every client tick. Including it in the target
        // key made the goggles restart their fade-in on every movement frame,
        // producing a visible flash. Controller position plus pancake NBT is
        // stable for the whole trip; identical pancakes may deliberately share
        // a key because switching between them cannot change the shown panel.
        return item(selected.stack,
                blockIdentity(controller.getBlockPos(), selected.stack,
                        0x4000_0000_0000_0000L));
    }

    private static double distanceToRaySqr(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start);
        double lengthSqr = direction.lengthSqr();
        if (lengthSqr <= 1.0E-7D) return point.distanceToSqr(start);
        double progress = Mth.clamp(point.subtract(start).dot(direction) / lengthSqr,
                0.0D, 1.0D);
        return point.distanceToSqr(start.add(direction.scale(progress)));
    }

    private static boolean isPancake(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof CatPancakeItem;
    }

    private static long blockIdentity(BlockPos pos, ItemStack stack, long namespace) {
        int tagHash = ItemCustomData.copy(stack).hashCode();
        return namespace ^ pos.asLong() ^ Integer.toUnsignedLong(tagHash);
    }
}
