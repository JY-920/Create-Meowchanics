package cn.laowu.mod.genetics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Rare, bounded block-material cat spawning that works in every dimension. */
public final class NaturalCatMaterialSpawner {
    private static final String GENERATED_TAG = "LaoWuNaturalBlockMaterialCat";
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final int MIN_SPAWN_DELAY_TICKS = 10 * 60 * 20;
    private static final int MAX_SPAWN_DELAY_TICKS = 15 * 60 * 20;
    private static final int FAILED_ATTEMPT_RETRY_TICKS = 60 * 20;
    private static final float ORDINARY_NATURAL_CAT_CHANCE = 0.04F;
    private static final float REGISTRY_MATERIAL_CHANCE = 0.25F;
    private static final int LOCAL_CAP = 4;
    private static final int LOCAL_CAP_RADIUS = 96;
    /** Server levels disappear on world unload, so their timers must not retain them. */
    private static final Map<ServerLevel, Long> NEXT_SPAWN_TICKS = new WeakHashMap<>();

    /**
     * Gives ordinary biome/structure cat spawns a small block-material chance.
     * The caller is responsible for filtering to natural spawn reasons.
     */
    public static void maybeMaterializeNaturalCat(Cat cat,
                                                  ServerLevelAccessor level) {
        if (CatGenomeData.has(cat)
                || cat.getRandom().nextFloat() >= ORDINARY_NATURAL_CAT_CHANCE) return;
        chooseMaterial(level, cat.blockPosition(), cat.getRandom())
                .ifPresent(material -> CatGenomeData.set(cat,
                        CatGenome.uniform(material)));
    }

    /**
     * One successful special-cat spawn schedules the next one 10-15 minutes
     * later in that active dimension. A blocked attempt retries after one
     * minute, rather than silently turning the advertised interval into a
     * much longer geometric wait.
     */
    public static void tick(ServerLevel level) {
        if (level.getGameTime() % CHECK_INTERVAL_TICKS != 0
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
        RandomSource random = level.random;

        List<ServerPlayer> players = level.players().stream()
                .filter(player -> player.isAlive() && !player.isSpectator())
                .toList();
        if (players.isEmpty()) {
            NEXT_SPAWN_TICKS.remove(level);
            return;
        }

        long now = level.getGameTime();
        Long nextSpawn = NEXT_SPAWN_TICKS.get(level);
        if (nextSpawn == null) {
            scheduleNormal(level, now, random);
            return;
        }
        if (now < nextSpawn) return;

        ServerPlayer player = players.get(random.nextInt(players.size()));
        AABB localArea = new AABB(player.blockPosition()).inflate(
                LOCAL_CAP_RADIUS, 48.0D, LOCAL_CAP_RADIUS);
        if (level.getEntitiesOfClass(Cat.class, localArea,
                cat -> cat.isAlive() && cat.getPersistentData()
                        .getBoolean(GENERATED_TAG)).size() >= LOCAL_CAP) {
            scheduleRetry(level, now);
            return;
        }

        Optional<BlockPos> spawnPos = findSpawnPosition(level, player, random);
        if (spawnPos.isEmpty()) {
            scheduleRetry(level, now);
            return;
        }
        Optional<ResourceLocation> material = chooseMaterial(
                level, spawnPos.get(), random);
        if (material.isEmpty()) {
            scheduleRetry(level, now);
            return;
        }

        Cat cat = EntityType.CAT.create(level);
        if (cat == null) {
            scheduleRetry(level, now);
            return;
        }
        BlockPos pos = spawnPos.get();
        cat.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                random.nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(cat)) {
            scheduleRetry(level, now);
            return;
        }
        cat.finalizeSpawn(level, level.getCurrentDifficultyAt(pos),
                MobSpawnType.NATURAL, null);
        CatGenomeData.set(cat, CatGenome.uniform(material.get()));
        cat.getPersistentData().putBoolean(GENERATED_TAG, true);
        if (level.addFreshEntity(cat)) scheduleNormal(level, now, random);
        else scheduleRetry(level, now);
    }

    private static void scheduleNormal(ServerLevel level, long now,
                                       RandomSource random) {
        NEXT_SPAWN_TICKS.put(level, now + Mth.nextInt(random,
                MIN_SPAWN_DELAY_TICKS, MAX_SPAWN_DELAY_TICKS));
    }

    private static void scheduleRetry(ServerLevel level, long now) {
        NEXT_SPAWN_TICKS.put(level, now + FAILED_ATTEMPT_RETRY_TICKS);
    }

    private static Optional<BlockPos> findSpawnPosition(ServerLevel level,
                                                        ServerPlayer player,
                                                        RandomSource random) {
        BlockPos origin = player.blockPosition();
        for (int attempt = 0; attempt < 12; attempt++) {
            int xOffset;
            int zOffset;
            do {
                xOffset = Mth.nextInt(random, -40, 40);
                zOffset = Mth.nextInt(random, -40, 40);
            } while (xOffset * xOffset + zOffset * zOffset < 16 * 16);
            int x = origin.getX() + xOffset;
            int z = origin.getZ() + zOffset;
            BlockPos column = new BlockPos(x, origin.getY(), z);
            if (!level.getWorldBorder().isWithinBounds(column)
                    || !level.hasChunkAt(column)) {
                continue;
            }
            int top = Math.min(level.getMaxBuildHeight() - 2, origin.getY() + 12);
            int bottom = Math.max(level.getMinBuildHeight() + 1, origin.getY() - 20);
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int y = top; y >= bottom; y--) {
                cursor.set(x, y, z);
                if (isSafeStandingPosition(level, cursor)) return Optional.of(cursor.immutable());
            }
        }
        return Optional.empty();
    }

    private static boolean isSafeStandingPosition(ServerLevel level, BlockPos pos) {
        BlockPos floorPos = pos.below();
        BlockState floor = level.getBlockState(floorPos);
        if (!floor.isFaceSturdy(level, floorPos, Direction.UP)
                || floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.CACTUS)
                || !level.getFluidState(floorPos).isEmpty()) return false;
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getFluidState(pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                && level.getFluidState(pos.above()).isEmpty();
    }

    private static Optional<ResourceLocation> chooseMaterial(ServerLevelAccessor level,
                                                             BlockPos center,
                                                             RandomSource random) {
        if (random.nextFloat() < REGISTRY_MATERIAL_CHANCE) {
            Optional<ResourceLocation> global = CatMaterialRegistry
                    .randomBlockMaterial(random);
            if (global.isPresent()) return global;
        }
        Optional<ResourceLocation> local = sampleLocalMaterial(level, center, random);
        return local.isPresent() ? local : CatMaterialRegistry.randomBlockMaterial(random);
    }

    private static Optional<ResourceLocation> sampleLocalMaterial(
            ServerLevelAccessor level, BlockPos center, RandomSource random) {
        // Prefer the actual floor, then inspect at most sixteen nearby blocks.
        Optional<ResourceLocation> floor = CatMaterialRegistry.blockMaterial(
                level.getBlockState(center.below()).getBlock());
        if (floor.isPresent()) return floor;
        for (int attempt = 0; attempt < 16; attempt++) {
            BlockPos sample = center.offset(Mth.nextInt(random, -8, 8),
                    Mth.nextInt(random, -5, 5), Mth.nextInt(random, -8, 8));
            if (!level.hasChunk(sample.getX() >> 4, sample.getZ() >> 4)) continue;
            Optional<ResourceLocation> material = CatMaterialRegistry.blockMaterial(
                    level.getBlockState(sample).getBlock());
            if (material.isPresent()) return material;
        }
        return Optional.empty();
    }

    private NaturalCatMaterialSpawner() {}
}
