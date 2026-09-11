package cn.laowu.mod.item;

import cn.laowu.mod.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** A nine-cat living-entity carrier, distinct from the existing pancake-only Cat Pouch. */
public final class CatStorageBoxItem extends Item {
    public static final int CAPACITY = 9;
    public static final double COLLECTION_RADIUS = 32.0D;
    private static final String CATS = "StoredPetCats";
    public CatStorageBoxItem(Properties properties) { super(properties.stacksTo(1)); }
    private static CompoundTag data(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().copy() : new CompoundTag();
    }
    private static void store(ItemStack stack, CompoundTag root) {
        stack.setTag(root.isEmpty() ? null : root);
    }
    public static int count(ItemStack stack) {
        // Render/tooltip reads must not clone nine cats' inventories every frame. Never mutate this view.
        CompoundTag root = stack.getTag();
        return root == null ? 0 : root.getList(CATS, Tag.TAG_COMPOUND).size();
    }
    private static boolean owned(Cat cat, Player player) {
        return cat.isAlive() && !cat.isRemoved() && cat.isTame()
                && player.getUUID().equals(cat.getOwnerUUID());
    }
    private static boolean busy(Cat cat, ServerLevel level) {
        if (CatProfileData.isBeingViewed(cat)) return true;
        for (ServerPlayer viewer : level.players()) {
            if (viewer.containerMenu instanceof CatPackageMenu menu && menu.getCatId() == cat.getId()) return true;
        }
        return false;
    }

    private static boolean onSeat(Cat cat) {
        return cat.getVehicle() instanceof com.simibubi.create.content.contraptions.actors.seat.SeatEntity
                || CareerCatBehavior.findSeat(cat) != null;
    }

    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                             LivingEntity entity, InteractionHand hand) {
        if (!(entity instanceof Cat clicked)) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.CONSUME;
        if (!(player.level() instanceof ServerLevel level)) return InteractionResult.FAIL;
        if (!owned(clicked, player)) {
            message(player, "not_owned");
            return InteractionResult.CONSUME;
        }
        if (stack.getCount() != 1) return InteractionResult.FAIL;
        CompoundTag root = data(stack);
        ListTag cats = root.getList(CATS, Tag.TAG_COMPOUND);
        if (cats.size() >= CAPACITY) { message(player, "full"); return InteractionResult.CONSUME; }

        List<Cat> candidates = new ArrayList<>();
        candidates.add(clicked);
        if (player.isShiftKeyDown()) {
            // Query only nearby loaded sections, then enforce a spherical player-centred radius.
            List<Cat> others = new ArrayList<>(level.getEntitiesOfClass(Cat.class,
                    player.getBoundingBox().inflate(COLLECTION_RADIUS),
                    cat -> cat != clicked && owned(cat, player)
                            && player.distanceToSqr(cat) <= COLLECTION_RADIUS * COLLECTION_RADIUS
                            && !onSeat(cat)));
            others.sort(Comparator.comparingDouble(player::distanceToSqr));
            candidates.addAll(others);
        }
        int captured = 0;
        for (Cat cat : candidates) {
            if (cats.size() >= CAPACITY) break;
            if (!owned(cat, player) || onSeat(cat) || busy(cat, level)) continue;
            if (player.isShiftKeyDown()
                    && player.distanceToSqr(cat) > COLLECTION_RADIUS * COLLECTION_RADIUS) continue;
            CompoundTag saved = cat.saveWithoutId(new CompoundTag());
            saved.putString("id", EntityType.getKey(cat.getType()).toString());
            // Preserve UUID, owner, health, inventory and genes; drop only world attachments/orders.
            for (String key : List.of("Passengers", "Leash", "Pos", "Motion", "Rotation"))
                saved.remove(key);
            saved.putFloat("FallDistance", 0);
            CompoundTag persistent = saved.getCompound("ForgeData");
            for (String key : List.of("LaoWuLaserUntil", "LaoWuLaserTarget", "LaoWuLaserX", "LaoWuLaserY", "LaoWuLaserZ"))
                persistent.remove(key);
            cats.add(saved);
            root.put(CATS, cats);
            store(stack, root); // Commit the snapshot before removing the live entity.
            cat.stopRiding();
            if (cat.isLeashed()) cat.dropLeash(true, true);
            CatProfileData.forgetStoredEntity(cat);
            whiteCloud(level, cat);
            cat.discard(); // Not death: no loot, no career death penalty.
            captured++;
        }
        if (captured == 0) message(player, "unavailable");
        else player.displayClientMessage(Component.translatable("message.laowu.cat_storage_box.captured", captured, cats.size(), CAPACITY), true);
        return InteractionResult.CONSUME;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != Direction.UP || context.getPlayer() == null) return InteractionResult.PASS;
        Player player = context.getPlayer();
        if (context.getLevel().isClientSide) return InteractionResult.CONSUME;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.FAIL;
        ItemStack stack = context.getItemInHand();
        if (stack.getCount() != 1) return InteractionResult.FAIL;
        CompoundTag root = data(stack);
        ListTag cats = root.getList(CATS, Tag.TAG_COMPOUND);
        if (cats.isEmpty()) { message(player, "empty"); return InteractionResult.CONSUME; }
        int limit = player.isShiftKeyDown() ? cats.size() : 1;
        int released = 0;
        for (int index = 0; index < cats.size() && released < limit;) {
            if (!release(level, cats.getCompound(index), context.getClickedPos().above(), player.getYRot())) {
                if (!player.isShiftKeyDown()) break;
                index++;
                continue;
            }
            cats.remove(index); // Failed placement/spawn leaves the stored cat untouched.
            if (cats.isEmpty()) root.remove(CATS); else root.put(CATS, cats);
            store(stack, root);
            released++;
        }
        if (released == 0) message(player, "blocked");
        else player.displayClientMessage(Component.translatable("message.laowu.cat_storage_box.released", released, cats.size()), true);
        return InteractionResult.CONSUME;
    }

    private static boolean release(ServerLevel level, CompoundTag saved, BlockPos origin, float yaw) {
        if (!saved.hasUUID("UUID")) return false;
        // Never duplicate a live UUID, even if another copy of the box exists or the cat is in another dimension.
        UUID uuid = saved.getUUID("UUID");
        for (ServerLevel dimension : level.getServer().getAllLevels())
            if (dimension.getEntity(uuid) != null) return false;
        Entity restored = EntityType.create(saved.copy(), level).orElse(null);
        if (!(restored instanceof Cat cat) || !cat.isTame() || !cat.isAlive()) return false;
        cat.refreshDimensions();
        Vec3 position = placement(level, cat, origin);
        if (position == null) return false;
        cat.moveTo(position.x, position.y, position.z, yaw, 0);
        cat.setDeltaMovement(Vec3.ZERO);
        cat.fallDistance = 0;
        cat.setTarget(null);
        cat.setInSittingPose(cat.isOrderedToSit());
        cat.setPersistenceRequired();
        if (!level.addFreshEntity(cat)) return false;
        whiteCloud(level, cat);
        return true;
    }

    private static void whiteCloud(ServerLevel level, Cat cat) {
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                cat.getX(), cat.getY() + cat.getBbHeight() * .5, cat.getZ(),
                20, Math.max(.2, cat.getBbWidth() * .4),
                Math.max(.15, cat.getBbHeight() * .35), Math.max(.2, cat.getBbWidth() * .4), .025);
    }

    private static Vec3 placement(ServerLevel level, Cat cat, BlockPos origin) {
        for (int radius = 0; radius <= 2; radius++) {
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
                for (int y : new int[]{0, 1, -1}) {
                    BlockPos feet = origin.offset(x, y, z);
                    if (!level.hasChunkAt(feet)) continue;
                    if (!level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) continue;
                    Vec3 point = new Vec3(feet.getX() + .5, feet.getY() + .01, feet.getZ() + .5);
                    cat.setPos(point.x, point.y, point.z);
                    if (!level.getWorldBorder().isWithinBounds(cat.getBoundingBox())
                            || !level.noCollision(cat, cat.getBoundingBox())
                            || level.containsAnyLiquid(cat.getBoundingBox())) continue;
                    if (!level.getEntitiesOfClass(Cat.class, cat.getBoundingBox(), Cat::isAlive).isEmpty()) continue;
                    return point;
                }
            }
        }
        return null;
    }
    private static void message(Player player, String key) {
        player.displayClientMessage(Component.translatable("message.laowu.cat_storage_box." + key), true);
    }
    @Override public void appendHoverText(ItemStack stack, Level level,
                                          List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.laowu.cat_storage_box.count", count(stack), CAPACITY)
                .withStyle(ChatFormatting.GOLD));
    }
}
