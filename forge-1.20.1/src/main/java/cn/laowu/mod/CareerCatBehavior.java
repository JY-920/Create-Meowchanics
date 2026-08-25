package cn.laowu.mod;

import cn.laowu.mod.mixin.BlazeBurnerBlockEntityAccessor;
import cn.laowu.mod.genetics.CatAttributeEffects;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitData;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.foundation.fluid.FluidHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** Server-side attributes and jobs supplied by the six cat career outfits. */
public final class CareerCatBehavior {
    public static final double CAREER_ARMOR = 10.0D;
    public static final double CAREER_TOUGHNESS = 0.0D;
    public static final double CAREER_HEALTH_BONUS = 30.0D;
    public static final double TERMINATOR_ATTACK_BONUS = 2.0D;
    private static final double MAX_OWNER_DISTANCE_SQR = 32.0D * 32.0D;

    private static final UUID HEALTH_MODIFIER_ID =
            UUID.fromString("69621850-71c7-4a62-9cf9-a9030a28e807");
    private static final UUID ARMOR_MODIFIER_ID =
            UUID.fromString("229dfdc2-726f-4f8e-90d8-51fc0e2996ef");
    private static final UUID TOUGHNESS_MODIFIER_ID =
            UUID.fromString("6d01b591-9353-49a9-94b2-a5d0fc4c3971");
    private static final UUID ATTACK_MODIFIER_ID =
            UUID.fromString("a01ea9c1-ee05-44cf-acf8-d786966cdf50");

    private static final String NEXT_FISH_TAG = "LaoWuCareerNextFish";
    private static final String NEXT_WATER_SCAN_TAG = "LaoWuCareerNextWaterScan";
    private static final String WATER_ROOT_TAG = "LaoWuCareerWaterRoot";
    private static final String WATER_SIZE_TAG = "LaoWuCareerWaterSize";
    private static final String NEXT_HONEY_TAG = "LaoWuCareerNextHoney";

    private static final int WATER_SCAN_INTERVAL = 20 * 10;
    private static final int MAX_WATER_BLOCKS = 300;
    private static final int WATER_SEARCH_RANGE = 32;
    private static final int HONEY_INTERVAL = 20 * 10;
    private static final int SUPERHEAT_DURATION = 20 * 5;

    /** Transient bookkeeping avoids serialising modifiers or rescanning goal lists. */
    private static final Map<Cat, CatOutfitType> APPLIED_OUTFITS = new WeakHashMap<>();
    private static final Set<Cat> COMBAT_GOALS_INSTALLED =
            Collections.newSetFromMap(new WeakHashMap<>());

    public static void tick(Cat cat) {
        if (!(cat.level() instanceof ServerLevel level)) return;

        CatOutfitType outfit = CatClothesData.getOutfit(cat);
        if (APPLIED_OUTFITS.get(cat) != outfit) {
            AttributeInstance healthAttribute = cat.getAttribute(Attributes.MAX_HEALTH);
            boolean newlyGranted = outfit != CatOutfitType.NONE && healthAttribute != null
                    && healthAttribute.getModifier(HEALTH_MODIFIER_ID) == null;
            applyAttributes(cat, outfit, newlyGranted);
            APPLIED_OUTFITS.put(cat, outfit);
        }
        if (outfit == CatOutfitType.NONE || CatPoseData.isPancake(cat)) return;

        switch (outfit) {
            case TERMINATOR -> tickTerminator(level, cat);
            case FISHING -> tickFishing(level, cat);
            case FIRE -> tickFire(cat);
            case HONEY -> tickHoney(level, cat);
            default -> {
                // Flight and transport jobs are interaction/event driven.
            }
        }
    }

    /** Called on equipment changes so health and armour update before the next entity tick. */
    public static void onOutfitChanged(Cat cat, boolean preserveMissingHealth) {
        CatOutfitType outfit = CatClothesData.getOutfit(cat);
        applyAttributes(cat, outfit, preserveMissingHealth);
        CatAttributeEffects.refresh(cat);
        APPLIED_OUTFITS.put(cat, outfit);
        if (outfit != CatOutfitType.TERMINATOR) cat.setTarget(null);
    }

    private static void applyAttributes(Cat cat, CatOutfitType outfit,
                                        boolean preserveMissingHealth) {
        float oldMax = cat.getMaxHealth();
        float oldHealth = cat.getHealth();
        double health = outfit == CatOutfitType.NONE ? 0.0D : CAREER_HEALTH_BONUS;

        setModifier(cat, Attributes.MAX_HEALTH, HEALTH_MODIFIER_ID,
                "Lao Wu career cat health", health);
        setModifier(cat, Attributes.ARMOR, ARMOR_MODIFIER_ID,
                "Lao Wu career cat armor",
                outfit == CatOutfitType.NONE ? 0.0D : CAREER_ARMOR);
        setModifier(cat, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_MODIFIER_ID,
                "Lao Wu career cat toughness",
                outfit == CatOutfitType.NONE ? 0.0D : CAREER_TOUGHNESS);
        setModifier(cat, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER_ID,
                "Lao Wu terminator cat damage",
                outfit == CatOutfitType.TERMINATOR ? TERMINATOR_ATTACK_BONUS : 0.0D);

        float newMax = cat.getMaxHealth();
        if (preserveMissingHealth && newMax > oldMax) {
            // Equipping adds usable health without also erasing existing wounds.
            cat.setHealth(Math.min(newMax, oldHealth + newMax - oldMax));
        } else if (oldHealth > newMax) {
            cat.setHealth(newMax);
        }
    }

    private static void setModifier(Cat cat, Attribute attribute, UUID id,
                                    String name, double amount) {
        AttributeInstance instance = cat.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier current = instance.getModifier(id);
        if (current != null && Double.compare(current.getAmount(), amount) == 0) return;
        if (current != null) instance.removeModifier(id);
        if (amount != 0.0D) {
            // Career equipment is persistent NBT state, so save its stable-ID
            // modifiers with the cat as well. This prevents health from being
            // clamped to the vanilla maximum while an equipped cat is loading.
            instance.addPermanentModifier(new AttributeModifier(id, name, amount,
                    AttributeModifier.Operation.ADDITION));
        }
    }

    private static void ensureTerminatorCombat(Cat cat) {
        if (!COMBAT_GOALS_INSTALLED.add(cat)) return;
        cat.goalSelector.addGoal(4, new TerminatorMeleeGoal(cat));
        cat.targetSelector.addGoal(1, new TerminatorOwnerHurtByGoal(cat));
        cat.targetSelector.addGoal(2, new TerminatorOwnerHurtTargetGoal(cat));
        cat.targetSelector.addGoal(3, new TerminatorHurtByGoal(cat));
    }

    private static void tickTerminator(ServerLevel level, Cat cat) {
        ensureTerminatorCombat(cat);

        LivingEntity target = cat.getTarget();
        if (isForbiddenTerminatorTarget(target)) {
            cat.setTarget(null);
            cat.getNavigation().stop();
            target = null;
        }
        int longFurLevel = CatTraitData.ensure(cat).level(CatTrait.LONG_FUR);
        if (target != null && longFurLevel > 0) {
            double sight = Math.max(1.0D,
                    cat.getAttributeValue(Attributes.FOLLOW_RANGE));
            if (cat.distanceToSqr(target) > sight * sight) {
                cat.setTarget(null);
                cat.getNavigation().stop();
                target = null;
            }
        }
        if (isForbiddenTerminatorTarget(cat.getLastHurtByMob())) {
            cat.setLastHurtByMob(null);
        }

        LivingEntity owner = cat.getOwner();
        boolean resting = cat.isInSittingPose() || findSeat(cat) != null;
        boolean activelyFighting = target != null && target.isAlive();
        if (owner == null || resting || !activelyFighting
                || cat.distanceToSqr(owner) <= MAX_OWNER_DISTANCE_SQR) return;

        // The 32-block limit is a combat leash, not an idle follow rule. Cats
        // stationed far away (including on Seats) stay there; only an active
        // chase can trigger disengagement and a return to the owner.
        cat.setTarget(null);
        cat.setLastHurtByMob(null);
        cat.getNavigation().stop();
        teleportNearOwner(level, cat, owner);
    }

    private static void teleportNearOwner(ServerLevel level, Cat cat, LivingEntity owner) {
        BlockPos origin = owner.blockPosition();
        for (int attempt = 0; attempt < 12; attempt++) {
            int offsetX = Mth.nextInt(cat.getRandom(), -2, 2);
            int offsetY = Mth.nextInt(cat.getRandom(), -1, 1);
            int offsetZ = Mth.nextInt(cat.getRandom(), -2, 2);
            BlockPos feet = origin.offset(offsetX, offsetY, offsetZ);
            BlockPos floor = feet.below();
            if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) continue;

            double x = feet.getX() + 0.5D;
            double y = feet.getY();
            double z = feet.getZ() + 0.5D;
            AABB destination = cat.getBoundingBox().move(
                    x - cat.getX(), y - cat.getY(), z - cat.getZ());
            if (!level.noCollision(cat, destination) || level.containsAnyLiquid(destination)) continue;

            cat.stopRiding();
            cat.teleportTo(x, y, z);
            cat.setDeltaMovement(Vec3.ZERO);
            cat.fallDistance = 0.0F;
            return;
        }

        // The owner's exact position is a last-resort destination for unusual
        // terrain; the distance guard is more important than preserving a
        // hostile target indefinitely.
        cat.stopRiding();
        cat.teleportTo(owner.getX(), owner.getY(), owner.getZ());
        cat.setDeltaMovement(Vec3.ZERO);
        cat.fallDistance = 0.0F;
    }

    public static boolean isForbiddenTerminatorTarget(LivingEntity target) {
        return target instanceof Player || target instanceof Cat;
    }

    private static boolean canFight(Cat cat) {
        return cat.isTame() && cat.isAlive() && !CatPoseData.isPancake(cat)
                && CatClothesData.getOutfit(cat) == CatOutfitType.TERMINATOR;
    }

    private static void tickFishing(ServerLevel level, Cat cat) {
        // Capabilities and the cached water calculation need no per-tick polling.
        if (cat.tickCount % 10 != 0) return;
        BlockPos seat = findSeat(cat);
        if (seat == null) {
            resetFishing(cat);
            return;
        }

        BlockPos waterRoot = findAdjacentWaterSource(level, seat.below());
        ContainerTarget container = findAdjacentContainer(level, seat);
        if (waterRoot == null || container == null) {
            resetFishing(cat);
            return;
        }

        CompoundTag data = cat.getPersistentData();
        long now = level.getGameTime();
        long root = waterRoot.asLong();
        if (data.getLong(WATER_ROOT_TAG) != root
                || now >= data.getLong(NEXT_WATER_SCAN_TAG)) {
            int waterBlocks = countConnectedWater(level, waterRoot);
            data.putLong(WATER_ROOT_TAG, root);
            data.putInt(WATER_SIZE_TAG, waterBlocks);
            data.putLong(NEXT_WATER_SCAN_TAG, now + WATER_SCAN_INTERVAL);
            if (waterBlocks <= 0) {
                resetFishing(cat);
                return;
            }
        }

        int waterBlocks = Mth.clamp(data.getInt(WATER_SIZE_TAG), 1, MAX_WATER_BLOCKS);
        if (!data.contains(NEXT_FISH_TAG, Tag.TAG_LONG)) {
            data.putLong(NEXT_FISH_TAG, now + nextFishingDelay(cat, waterBlocks));
            return;
        }
        if (now < data.getLong(NEXT_FISH_TAG)) return;

        catchFish(level, cat, waterRoot, container);
        data.putLong(NEXT_FISH_TAG, now + nextFishingDelay(cat, waterBlocks));
    }

    private static int nextFishingDelay(Cat cat, int waterBlocks) {
        double progress = (waterBlocks - 1.0D) / (MAX_WATER_BLOCKS - 1.0D);
        double speedMultiplier = 0.1D + 1.4D * Mth.clamp(progress, 0.0D, 1.0D);
        // Vanilla FishingHook selects a 100-600 tick lure delay. Scaling that
        // same range gives the requested 0.1x through 1.5x fishing rates.
        int vanillaDelay = Mth.nextInt(cat.getRandom(), 100, 600);
        return Math.max(10, Mth.ceil(vanillaDelay / speedMultiplier));
    }

    private static void catchFish(ServerLevel level, Cat cat, BlockPos water,
                                  ContainerTarget target) {
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(water))
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                .withParameter(LootContextParams.THIS_ENTITY, cat)
                .withLuck(CatAttributeEffects.fishingLootLuck(cat))
                .create(LootContextParamSets.FISHING);
        var table = level.getServer().getLootData().getLootTable(BuiltInLootTables.FISHING);
        for (ItemStack caught : table.getRandomItems(params)) {
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(
                    target.handler(), caught.copy(), false);
            if (!remainder.isEmpty()) {
                Containers.dropItemStack(level,
                        target.pos().getX() + 0.5D, target.pos().getY() + 1.0D,
                        target.pos().getZ() + 0.5D, remainder);
            }
        }
        level.sendParticles(ParticleTypes.SPLASH,
                water.getX() + 0.5D, water.getY() + 1.0D, water.getZ() + 0.5D,
                8, 0.32D, 0.04D, 0.32D, 0.12D);
        level.sendParticles(ParticleTypes.BUBBLE,
                water.getX() + 0.5D, water.getY() + 0.85D, water.getZ() + 0.5D,
                6, 0.3D, 0.05D, 0.3D, 0.04D);
        level.playSound(null, water, SoundEvents.FISHING_BOBBER_SPLASH,
                SoundSource.NEUTRAL, 0.8F, 1.05F);
    }

    private static void resetFishing(Cat cat) {
        cat.getPersistentData().remove(NEXT_FISH_TAG);
    }

    private static BlockPos findAdjacentWaterSource(ServerLevel level, BlockPos support) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = support.relative(direction);
            if (!level.hasChunkAt(candidate)) continue;
            var fluid = level.getFluidState(candidate);
            if (fluid.isSource() && fluid.is(FluidTags.WATER)) return candidate.immutable();
        }
        return null;
    }

    /**
     * Bounded version of Create's hose-pulley flood search: six directions,
     * no downward expansion, still/flowing fluid normalisation, unloaded-chunk
     * avoidance and a squared range limit. The requested 300-block cap makes
     * the scan substantially smaller than a hose pulley search.
     */
    private static int countConnectedWater(ServerLevel level, BlockPos root) {
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        frontier.add(root.immutable());
        int count = 0;
        int rangeSqr = WATER_SEARCH_RANGE * WATER_SEARCH_RANGE;

        while (!frontier.isEmpty() && count < MAX_WATER_BLOCKS) {
            BlockPos pos = frontier.removeFirst();
            if (!visited.add(pos) || root.distSqr(pos) > rangeSqr || !level.hasChunkAt(pos)) {
                continue;
            }
            var fluidState = level.getFluidState(pos);
            if (fluidState.isEmpty()
                    || !FluidHelper.convertToStill(fluidState.getType()).isSame(Fluids.WATER)) {
                continue;
            }
            count++;
            for (Direction direction : Direction.values()) {
                if (direction == Direction.DOWN) continue;
                BlockPos next = pos.relative(direction);
                if (!visited.contains(next) && root.distSqr(next) <= rangeSqr
                        && level.hasChunkAt(next)) {
                    frontier.addLast(next.immutable());
                }
            }
        }
        return count;
    }

    private static ContainerTarget findAdjacentContainer(ServerLevel level, BlockPos seat) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            ContainerTarget target = containerAt(level, seat.relative(direction),
                    direction.getOpposite());
            if (target != null) return target;
        }
        // Also accept a container touching the supporting block, useful for
        // low hoppers and compact shore-side builds.
        BlockPos support = seat.below();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            ContainerTarget target = containerAt(level, support.relative(direction),
                    direction.getOpposite());
            if (target != null) return target;
        }
        return null;
    }

    private static ContainerTarget containerAt(ServerLevel level, BlockPos pos, Direction side) {
        if (!level.hasChunkAt(pos)) return null;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return null;
        IItemHandler handler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side)
                .resolve().orElse(null);
        return handler == null ? null : new ContainerTarget(pos.immutable(), handler);
    }

    private static void tickFire(Cat cat) {
        if (cat.tickCount % 10 != 0) return;
        BlockPos seat = findSeat(cat);
        if (seat == null) return;
        var traits = CatTraitData.ensure(cat);
        boolean sustainedSuperheat = traits.has(CatTrait.SUPERHEAT_GENE);
        int level = traits.level(CatTrait.BLAZING_FORM);
        boolean superheat = level > 0 && cat.tickCount % (20 * 10) == 0
                && cat.getRandom().nextInt(100)
                < CatTrait.BLAZING_FORM.blazingSuperheatChance(level);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!(cat.level().getBlockEntity(seat.relative(direction))
                    instanceof BlazeBurnerBlockEntity burner) || burner.isCreative()) continue;
            if (sustainedSuperheat || superheat) superheatBurner(burner);
            else keepBurnerKindled(burner);
        }
    }

    private static void superheatBurner(BlazeBurnerBlockEntity burner) {
        if (burner.getActiveFuel() == BlazeBurnerBlockEntity.FuelType.SPECIAL
                && burner.getRemainingBurnTime() > 40) return;
        boolean changedHeat = burner.getActiveFuel()
                != BlazeBurnerBlockEntity.FuelType.SPECIAL;
        BlazeBurnerBlockEntityAccessor accessor = (BlazeBurnerBlockEntityAccessor) burner;
        accessor.laowu$setActiveFuel(BlazeBurnerBlockEntity.FuelType.SPECIAL);
        accessor.laowu$setRemainingBurnTime(SUPERHEAT_DURATION);
        burner.setChanged();
        if (changedHeat) burner.updateBlockState();
    }

    private static void keepBurnerKindled(BlazeBurnerBlockEntity burner) {
        if (burner.getActiveFuel() == BlazeBurnerBlockEntity.FuelType.SPECIAL
                || burner.getRemainingBurnTime() > 20) return;
        boolean wasActive = burner.getActiveFuel() != BlazeBurnerBlockEntity.FuelType.NONE
                && burner.getRemainingBurnTime() > 0;
        BlazeBurnerBlockEntityAccessor accessor = (BlazeBurnerBlockEntityAccessor) burner;
        accessor.laowu$setActiveFuel(BlazeBurnerBlockEntity.FuelType.NORMAL);
        accessor.laowu$setRemainingBurnTime(40);
        burner.setChanged();
        if (!wasActive) burner.updateBlockState();
    }

    private static void tickHoney(ServerLevel level, Cat cat) {
        BlockPos seat = findSeat(cat);
        if (seat == null) {
            cat.getPersistentData().remove(NEXT_HONEY_TAG);
            return;
        }
        BlockPos hive = seat.below();
        BlockState state = level.getBlockState(hive);
        if (!(state.getBlock() instanceof BeehiveBlock)) {
            cat.getPersistentData().remove(NEXT_HONEY_TAG);
            return;
        }

        CompoundTag data = cat.getPersistentData();
        long now = level.getGameTime();
        int traitLevel = CatTraitData.ensure(cat).level(CatTrait.BEEBEE_GENE);
        int interval = traitLevel <= 0 ? HONEY_INTERVAL
                : CatTrait.BEEBEE_GENE.beebeeWorkIntervalSeconds(traitLevel) * 20;
        if (!data.contains(NEXT_HONEY_TAG, Tag.TAG_LONG)) {
            data.putLong(NEXT_HONEY_TAG, now + interval);
            return;
        }
        if (data.getLong(NEXT_HONEY_TAG) > now + interval) {
            data.putLong(NEXT_HONEY_TAG, now + interval);
        }
        if (now < data.getLong(NEXT_HONEY_TAG)) return;
        data.putLong(NEXT_HONEY_TAG, now + interval);

        int honey = state.getValue(BeehiveBlock.HONEY_LEVEL);
        if (honey >= BeehiveBlock.MAX_HONEY_LEVELS) return;
        level.setBlock(hive, state.setValue(BeehiveBlock.HONEY_LEVEL, honey + 1),
                Block.UPDATE_ALL);
        level.playSound(null, hive, SoundEvents.BEEHIVE_WORK,
                SoundSource.BLOCKS, 0.6F, 1.1F);
    }

    public static BlockPos findSeat(Cat cat) {
        if (cat.getVehicle() instanceof SeatEntity seatEntity) {
            BlockPos pos = seatEntity.blockPosition();
            if (cat.level().getBlockState(pos).getBlock() instanceof SeatBlock) {
                return pos.immutable();
            }
        }
        BlockPos[] candidates = {cat.blockPosition(), cat.blockPosition().below(), cat.getOnPos()};
        for (BlockPos pos : candidates) {
            if (cat.level().getBlockState(pos).getBlock() instanceof SeatBlock
                    && (cat.isPassenger() || cat.isInSittingPose())) {
                return pos.immutable();
            }
        }
        return null;
    }

    private record ContainerTarget(BlockPos pos, IItemHandler handler) {
    }

    private static final class TerminatorMeleeGoal extends MeleeAttackGoal {
        private final Cat cat;
        private int attributeAttackCooldown;

        private TerminatorMeleeGoal(Cat cat) {
            super(cat, 1.25D, true);
            this.cat = cat;
        }

        @Override
        public void start() {
            super.start();
            attributeAttackCooldown = 0;
        }

        @Override
        public void stop() {
            super.stop();
            attributeAttackCooldown = 0;
        }

        @Override
        public void tick() {
            if (attributeAttackCooldown > 0) attributeAttackCooldown--;
            super.tick();
        }

        @Override
        protected void resetAttackCooldown() {
            attributeAttackCooldown = CatAttributeEffects.attackIntervalTicks(cat);
        }

        @Override
        protected boolean isTimeToAttack() {
            return attributeAttackCooldown <= 0;
        }

        @Override
        protected int getTicksUntilNextAttack() {
            return attributeAttackCooldown;
        }

        @Override
        protected int getAttackInterval() {
            return CatAttributeEffects.attackIntervalTicks(cat);
        }

        @Override
        public boolean canUse() {
            return canFight(cat) && !isForbiddenTerminatorTarget(cat.getTarget())
                    && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return canFight(cat) && !isForbiddenTerminatorTarget(cat.getTarget())
                    && super.canContinueToUse();
        }
    }

    private static final class TerminatorOwnerHurtByGoal extends OwnerHurtByTargetGoal {
        private final Cat cat;

        private TerminatorOwnerHurtByGoal(Cat cat) {
            super(cat);
            this.cat = cat;
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = cat.getOwner();
            LivingEntity attacker = owner == null ? null : owner.getLastHurtByMob();
            return canFight(cat) && attacker != null
                    && !isForbiddenTerminatorTarget(attacker) && super.canUse();
        }
    }

    private static final class TerminatorOwnerHurtTargetGoal extends OwnerHurtTargetGoal {
        private final Cat cat;

        private TerminatorOwnerHurtTargetGoal(Cat cat) {
            super(cat);
            this.cat = cat;
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = cat.getOwner();
            LivingEntity target = owner == null ? null : owner.getLastHurtMob();
            return canFight(cat) && target != null
                    && !isForbiddenTerminatorTarget(target) && super.canUse();
        }
    }

    private static final class TerminatorHurtByGoal extends HurtByTargetGoal {
        private final Cat cat;

        private TerminatorHurtByGoal(Cat cat) {
            super(cat);
            this.cat = cat;
        }

        @Override
        public boolean canUse() {
            LivingEntity attacker = cat.getLastHurtByMob();
            LivingEntity owner = cat.getOwner();
            return canFight(cat) && attacker != null && attacker != owner
                    && !isForbiddenTerminatorTarget(attacker) && super.canUse();
        }
    }

    private CareerCatBehavior() {
    }
}
