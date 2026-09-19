package cn.laowu.mod.test;

import cn.laowu.mod.CommonEvents;
import cn.laowu.mod.genetics.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CatLoadingProbe {
    /** Detect the dangerous query without actually hanging the old server. */
    private static final class LoadingCat extends Cat {
        boolean ready;
        int prematureWeatherReads;
        int readyWeatherReads;

        LoadingCat(ServerLevel level) { super(EntityType.CAT, level); }

        @Override public boolean isInWaterOrRain() {
            if (!ready) {
                prematureWeatherReads++;
                return false;
            }
            readyWeatherReads++;
            return super.isInWaterOrRain();
        }
    }

    private static LoadingCat cat(GameTestHelper h) {
        h.assertTrue(h.getLevel().getServer() instanceof GameTestServer,
                "Run this isolated fixture in GameTestServer, never in a player save");
        BlockPos pos = h.absolutePos(new BlockPos(2, 2, 2));
        h.getLevel().setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        h.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        var cat = new LoadingCat(h.getLevel());
        cat.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
        cat.setNoAi(true);
        cat.setNoGravity(true);
        return cat;
    }

    private static CatAttributeProfile genes() {
        var profile = CatAttributeProfile.founder(RandomSource.create(20260918L));
        for (var stat : CatStat.values()) profile = profile.withValues(stat, 50, 100);
        return profile;
    }

    @GameTest(template="accessory_probe", batch="cat_loading", timeoutTicks=80)
    public static void freshCatDoesNotReadWorldOnJoin(GameTestHelper h) {
        var cat = cat(h);
        h.assertTrue(h.getLevel().addFreshEntity(cat), "Fresh cat joins");
        cat.ready = true;
        h.assertTrue(cat.prematureWeatherReads == 0,
                "EntityJoinLevel must not call weather/world queries, including ensure -> set -> refresh");
        h.assertTrue(CatAttributeData.read(cat).isEmpty() && CatTraitData.read(cat).isEmpty(),
                "Joining only schedules initialization, without generating or mutating profiles");
        h.runAfterDelay(4, () -> {
            h.assertTrue(cat.tickCount > 0 && cat.readyWeatherReads > 0,
                    "Actual entity tick performs deferred initialization");
            h.assertTrue(CatAttributeData.read(cat).isPresent() && CatTraitData.read(cat).isPresent(),
                    "New cats still acquire their attributes and traits");
            cat.discard();
            System.out.println("PASS: cat join has no world queries; first tick initializes both profiles");
            h.succeed();
        });
    }

    @GameTest(template="accessory_probe", batch="cat_loading", timeoutTicks=80)
    public static void loadedCatPreservesDataAndRecoversViewLock(GameTestHelper h) {
        var cat = cat(h);
        CompoundTag savedGenes = genes().save();
        CompoundTag savedTraits = CatTraitProfile.EMPTY.save();
        cat.getPersistentData().put(CatAttributeData.TAG, savedGenes.copy());
        cat.getPersistentData().put(CatTraitData.TAG, savedTraits.copy());
        cat.getPersistentData().putBoolean("LaoWuProfileViewLock", true);
        cat.getPersistentData().putBoolean("LaoWuProfilePreviousNoAi", true);
        cat.setHealth(cat.getMaxHealth() * .5F);
        h.assertTrue(h.getLevel().addFreshEntity(cat), "Saved-profile cat joins");
        cat.ready = true;
        h.assertTrue(cat.prematureWeatherReads == 0, "Saved cats also avoid world reads in join event");
        h.runAfterDelay(4, () -> {
            h.assertTrue(savedGenes.equals(CatAttributeData.serialized(cat))
                            && savedTraits.equals(CatTraitData.serialized(cat)),
                    "Existing six stats, potential ceilings and traits are not rerolled");
            h.assertTrue(!cat.getPersistentData().contains("LaoWuProfileViewLock") && cat.isNoAi(),
                    "Interrupted profile UI lock is recovered while retaining original NoAI");
            h.assertTrue(Math.abs(cat.getMaxHealth() - CatAttributeEffects.maximumHealth(50)) < .001
                            && Math.abs(cat.getHealth() / cat.getMaxHealth() - .5F) < .001,
                    "Derived maximum health is restored without changing the health fraction");
            cat.discard();
            System.out.println("PASS: loaded cat preserves genetics/health and recovers an interrupted UI lock");
            h.succeed();
        });
    }

    @GameTest(template="accessory_probe", batch="cat_loading", timeoutTicks=80)
    public static void rejoinAfterFirstTickIsDeferredAgain(GameTestHelper h) {
        var cat = cat(h);
        h.getLevel().addFreshEntity(cat);
        cat.ready = true;
        h.runAfterDelay(4, () -> {
            h.assertTrue(cat.tickCount > 0, "Rejoining instance has already ticked");
            var saved = CatAttributeData.serialized(cat).copy();
            int reads = cat.readyWeatherReads;
            cat.prematureWeatherReads = 0;
            cat.ready = false;
            CommonEvents.initializeCatTraits(new EntityJoinLevelEvent(cat, h.getLevel(), true));
            cat.ready = true;
            h.assertTrue(cat.prematureWeatherReads == 0, "Rejoin is deferred even with a nonzero tickCount");
            h.runAfterDelay(3, () -> {
                h.assertTrue(cat.readyWeatherReads > reads && saved.equals(CatAttributeData.serialized(cat)),
                        "Rejoin initialization resumes at the next tick and preserves the profile");
                cat.discard();
                System.out.println("PASS: same-instance rejoin queues initialization again without rerolling genes");
                h.succeed();
            });
        });
    }

    /**
     * Reproduce the reporter's actual ChunkMap -> runPostLoad -> entity join path.
     * A real Twilight Forest runtime adds its cloud hook to the weather query.
     * The baseline can hang here, so this case must be explicitly opted into.
     */
    @GameTest(template="accessory_probe", batch="cat_loading", timeoutTicks=240)
    public static void protoChunkCatCompletesFullPromotion(GameTestHelper h) {
        if (!Boolean.getBoolean("laowu.chunk_loading_promotion")) {
            System.out.println("SKIP: opt in with -PcatLoadingPromotion for real proto-chunk promotion");
            h.succeed();
            return;
        }
        var level = h.getLevel();
        h.assertTrue(level.getServer() instanceof GameTestServer, "Isolated test server required");
        if (Boolean.getBoolean("laowu.twilight_required")) {
            h.assertTrue(net.minecraftforge.fml.ModList.get().isLoaded("twilightforest"), "Real Twilight Forest is loaded");
            var cloud = net.minecraft.core.registries.BuiltInRegistries.BLOCK.entrySet().stream()
                    .filter(entry -> entry.getKey().location().toString().equals("twilightforest:rainy_cloud"))
                    .map(java.util.Map.Entry::getValue).findFirst().orElseThrow();
            BlockPos weatherPos = h.absolutePos(new BlockPos(2, 2, 2));
            level.setWeatherParameters(0, 1200, false, false);
            level.setBlockAndUpdate(weatherPos.above(), cloud.defaultBlockState());
            h.assertTrue(!level.isRaining() && level.isRainingAt(weatherPos),
                    "Real Twilight cloud hook is active, even without global rain");
            level.setBlockAndUpdate(weatherPos.above(), Blocks.AIR.defaultBlockState());
            System.out.println("PASS: actual Twilight rainy cloud changes the vanilla weather query");
        }
        int x = 4096, z = 4096;
        var chunk = level.getChunk(x, z, ChunkStatus.FEATURES, true);
        h.assertTrue(chunk instanceof ProtoChunk, "Fresh, unfinished proto-chunk fixture required");
        var proto = (ProtoChunk) chunk;
        Cat cat = EntityType.CAT.create(level);
        cat.moveTo(x * 16 + 8.5, level.getSeaLevel() + 4, z * 16 + 8.5, 0, 0);
        cat.setNoAi(true);
        cat.setNoGravity(true);
        var id = cat.getUUID();
        proto.addEntity(cat);
        // This is exactly where the old join -> rain -> cloud -> getChunk path self-waits.
        var full = level.getChunk(x, z, ChunkStatus.FULL, true);
        h.assertTrue(full != null, "FULL promotion returned without deadlock");
        level.setChunkForced(x, z, true);
        h.succeedWhen(() -> {
            var loaded = level.getEntity(id);
            h.assertTrue(loaded instanceof Cat && loaded.tickCount > 0,
                    "Promoted chunk's cat becomes queryable and ticks");
            Cat live = (Cat) loaded;
            h.assertTrue(CatAttributeData.read(live).isPresent() && CatTraitData.read(live).isPresent(),
                    "Deferred initialization completed after proto-chunk promotion");
            live.discard();
            level.setChunkForced(x, z, false);
            System.out.println("PASS: real proto-chunk cat promoted to FULL and initialized without deadlock; twilight="
                    + net.minecraftforge.fml.ModList.get().isLoaded("twilightforest"));
        });
    }
}
