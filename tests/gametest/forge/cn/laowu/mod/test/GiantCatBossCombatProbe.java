package cn.laowu.mod.test;

import cn.laowu.mod.entity.GiantCatBoss;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Integration-only probes: attacks/collision advance through real server entity ticks. */
@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class GiantCatBossCombatProbe {
    private static final float VICTIM_HEALTH = 1000;
    private static ResourceLocation id(String path) { return new ResourceLocation("laowu", path); }

    private static final class Arena {
        final GameTestHelper h;
        final List<Entity> entities = new ArrayList<>();
        final Set<ChunkPos> forced = new HashSet<>();
        final List<Runnable> cleanups = new ArrayList<>();
        boolean closed;
        Arena(GameTestHelper h) {
            this.h = h;
            // Expected RED assertions must not leak players, listeners, forced chunks
            // or temporary difficulty into the next independent GameTest batch.
            try {
                Field info = GameTestHelper.class.getDeclaredField("testInfo");
                info.setAccessible(true);
                ((GameTestInfo) info.get(h)).addListener(new GameTestListener() {
                    public void testStructureLoaded(GameTestInfo info) {}
                    public void testPassed(GameTestInfo info) { cleanup(); }
                    public void testFailed(GameTestInfo info) { cleanup(); }
                });
            } catch (ReflectiveOperationException error) { throw new AssertionError("Register test fixture cleanup", error); }
            BlockPos first = h.absolutePos(BlockPos.ZERO), last = h.absolutePos(new BlockPos(71, 11, 13));
            if (h.getLevel().getServer() instanceof GameTestServer) {
                for (int x = Math.floorDiv(first.getX(), 16); x <= Math.floorDiv(last.getX(), 16); x++)
                    for (int z = Math.floorDiv(first.getZ(), 16); z <= Math.floorDiv(last.getZ(), 16); z++) {
                        ChunkPos chunk = new ChunkPos(x, z);
                        if (!h.getLevel().getForcedChunks().contains(chunk.toLong())) {
                            h.getLevel().setChunkForced(x, z, true);
                            forced.add(chunk);
                        }
                    }
            }
            for (int x = 0; x < 72; x++) for (int z = 0; z < 14; z++) {
                h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                for (int y = 1; y < 12; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
            }
        }
        Vec3 at(int x, int y, int z) { return Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(x, y, z))); }
        GiantCatBoss boss(int x, int z) {
            Entity entity = BuiltInRegistries.ENTITY_TYPE.get(id("giant_cat_boss")).create(h.getLevel());
            h.assertTrue(entity instanceof GiantCatBoss, "Registered giant_cat_boss creates its real server entity");
            GiantCatBoss boss = (GiantCatBoss) entity;
            boss.setPos(at(x, 1, z));
            h.getLevel().addFreshEntity(boss);
            entities.add(boss);
            return boss;
        }
        Cow victim(int x, int z) {
            Cow cow = EntityType.COW.create(h.getLevel());
            cow.setNoAi(true);
            cow.setPersistenceRequired();
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(VICTIM_HEALTH);
            cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            cow.setHealth(VICTIM_HEALTH);
            cow.setPos(at(x, 1, z));
            h.getLevel().addFreshEntity(cow);
            entities.add(cow);
            return cow;
        }
        ServerPlayer player(Vec3 position) {
            // Borrow only the loader's no-network connection. Keep real ServerPlayer
            // damage/tick behavior; FakePlayer itself is invulnerable by design.
            var stub = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "giant-contact-connection"));
            ServerPlayer player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), new GameProfile(UUID.randomUUID(), "giant-contact-player"));
            player.connection = stub.connection;
            entities.add(stub);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getAbilities().invulnerable = false;
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(VICTIM_HEALTH);
            player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            player.setHealth(VICTIM_HEALTH);
            player.getFoodData().setFoodLevel(17);
            player.setNoGravity(true);
            player.setPos(position);
            try {
                Field protection = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
                protection.setAccessible(true); protection.setInt(player, 0);
            } catch (ReflectiveOperationException error) { throw new AssertionError("Disable only join protection for real player fixture", error); }
            h.getLevel().addNewPlayer(player);
            entities.add(player);
            return player;
        }
        void cleanup() {
            if (closed) return;
            closed = true;
            for (Runnable action : cleanups) action.run();
            for (Entity entity : entities) if (!entity.isRemoved()) {
                if (entity instanceof ServerPlayer player && h.getLevel().players().contains(player))
                    h.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
                else entity.discard();
            }
            for (ChunkPos chunk : forced) h.getLevel().setChunkForced(chunk.x, chunk.z, false);
        }
        void finish() {
            cleanup();
            h.succeed();
        }
    }


    /** Observe actual published server sounds, without muting or replacing them. */
    public static final class BossSoundWatch {
        private final GiantCatBoss boss;
        int hisses;
        BossSoundWatch(Arena arena, GiantCatBoss boss) {
            this.boss = boss;
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
            arena.cleanups.add(() -> net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(this));
        }
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public void sound(net.minecraftforge.event.PlayLevelSoundEvent event) {
            if (event.getLevel() != boss.level() || event.getSound() == null
                    || event.getSound().value() != net.minecraft.sounds.SoundEvents.CAT_HISS) return;
            if (event instanceof net.minecraftforge.event.PlayLevelSoundEvent.AtEntity at && at.getEntity() == boss) hisses++;
            else if (event instanceof net.minecraftforge.event.PlayLevelSoundEvent.AtPosition at
                    && at.getPosition().distanceToSqr(boss.position()) < .01D) hisses++;
        }
    }


    private static void phase(GiantCatBoss boss, byte phase) {
        try {
            Method method = GiantCatBoss.class.getDeclaredMethod("setPhase", byte.class);
            method.setAccessible(true);
            method.invoke(boss, phase);
        } catch (ReflectiveOperationException error) { throw new AssertionError("Unable to arrange attack phase", error); }
    }
    private static void field(GiantCatBoss boss, String name, Object value) {
        try {
            Field field = GiantCatBoss.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(boss, value);
        } catch (ReflectiveOperationException error) { throw new AssertionError("Unable to arrange " + name, error); }
    }
    private static void leap(GiantCatBoss boss, LivingEntity target) {
        try {
            Method method = GiantCatBoss.class.getDeclaredMethod("launchJump", LivingEntity.class);
            method.setAccessible(true);
            method.invoke(boss, target);
        } catch (ReflectiveOperationException error) { throw new AssertionError("Unable to arrange jump launch", error); }
    }
    private static void health(GameTestHelper h, LivingEntity victim, float expected, String message) {
        h.assertTrue(Math.abs(victim.getHealth() - expected) < .001F,
                message + ": actual=" + victim.getHealth() + ", expected=" + expected);
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_live_cycle", timeoutTicks = 450)
    public static void liveAiSelectsSpecialAttacksWithoutMeleeAndHonorsRecoveries(GameTestHelper h) {
        Arena arena = new Arena(h);
        GiantCatBoss boss = arena.boss(12, 6);
        Cow victim = arena.victim(22, 6);
        BossSoundWatch sound = new BossSoundWatch(arena, boss);
        boss.setTarget(victim);
        field(boss, "attackCooldown", 0);
        Set<Byte> seen = new HashSet<>();
        int[] stage = {1};
        long[] recoveryStart = {-1, -1};
        float[] recoveryHealth = {VICTIM_HEALTH};
        byte[] previous = {GiantCatBoss.IDLE};
        // No attack method/phase is invoked: distance and normal server AI choose each move.
        for (int tick = 1; tick <= 430; tick++) h.runAtTickTime(tick, () -> {
            byte current = boss.getPhase();
            h.assertTrue(sound.hisses == 0, "Real boss attack selection and transitions never publish CAT_HISS");
            seen.add(current);
            long now = h.getLevel().getGameTime();
            h.assertTrue(current != 2, "Normal AI never enters the removed melee attack phase");
            if (current == GiantCatBoss.ROLL_RECOVERY && previous[0] != current) {
                recoveryStart[0] = now;
                recoveryHealth[0] = victim.getHealth();
                stage[0] = 2;
            }
            if (current == GiantCatBoss.ROLL_RECOVERY)
                health(h, victim, recoveryHealth[0], "Roll recovery cannot attack");
            if (previous[0] == GiantCatBoss.ROLL_RECOVERY && current != previous[0]) {
                h.assertTrue(now - recoveryStart[0] >= 30 && now - recoveryStart[0] <= 31,
                        "Real roll recovery lasts 30 server ticks");
                h.assertTrue(current == GiantCatBoss.IDLE, "Roll recovery returns to neutral");
            }
            if (current == GiantCatBoss.SLAM_RECOVERY && previous[0] != current) {
                recoveryStart[1] = now;
                recoveryHealth[0] = victim.getHealth();
            }
            if (current == GiantCatBoss.SLAM_RECOVERY)
                health(h, victim, recoveryHealth[0], "Slam recovery cannot attack");
            if (previous[0] == GiantCatBoss.SLAM_RECOVERY && current == GiantCatBoss.IDLE) {
                h.assertTrue(now - recoveryStart[1] >= 40 && now - recoveryStart[1] <= 41,
                        "Real slam recovery lasts 40 server ticks");
                h.assertTrue(seen.containsAll(List.of(GiantCatBoss.ROLL_WINDUP,
                                GiantCatBoss.ROLL, GiantCatBoss.ROLL_RECOVERY, GiantCatBoss.JUMP_WINDUP,
                                GiantCatBoss.JUMP, GiantCatBoss.SLAM_RECOVERY)),
                        "Unmodified AI completes roll and jump with both telegraphs and recoveries, without melee");
                arena.finish();
                return;
            }
            // Keep a clear distant roll destination inside the 72-block arena.
            // For jump, freeze the victim once launch begins so landing is a real ballistic shot.
            if (current == GiantCatBoss.IDLE || stage[0] == 1 && current == GiantCatBoss.ROLL) {
                // The new four-to-six-second normal window must not let the
                // continually moved fixture lure the boss outside its arena.
                victim.setPos(stage[0] == 1 ? boss.getX() + 10.0D : arena.at(54, 1, 6).x,
                        arena.at(12, 1, 6).y, arena.at(12, 1, 6).z);
                victim.setDeltaMovement(Vec3.ZERO);
            }
            boss.setTarget(victim);
            previous[0] = current;
        });
        h.runAtTickTime(440, () -> System.out.println("GIANT_LIVE_AI_TIMEOUT_DIAGNOSTIC stage=" + stage[0]
                + " seen=" + seen + " phase=" + boss.getPhase() + " targetMatches=" + (boss.getTarget() == victim)
                + " bossTracked=" + (h.getLevel().getEntity(boss.getUUID()) == boss)
                + " victimTracked=" + (h.getLevel().getEntity(victim.getUUID()) == victim)
                + " bossTicking=" + h.getLevel().isPositionEntityTicking(boss.blockPosition())
                + " victimTicking=" + h.getLevel().isPositionEntityTicking(victim.blockPosition())
                + " bossPos=" + boss.position() + " victimPos=" + victim.position()
                + " grounded=" + boss.onGround() + " victimAlive=" + victim.isAlive()));
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_roll_wall", timeoutTicks = 120)
    public static void rollingStopsAtSolidWallAndNeverHitsThroughIt(GameTestHelper h) {
        Arena arena = new Arena(h);
        GiantCatBoss boss = arena.boss(12, 6);
        Cow victim = arena.victim(23, 6);
        for (int y = 1; y <= 8; y++) for (int z = 0; z < 14; z++)
            h.setBlock(new BlockPos(20, y, z), Blocks.STONE);
        double wallX = h.absolutePos(new BlockPos(20, 1, 6)).getX();
        boss.setTarget(victim);
        phase(boss, GiantCatBoss.ROLL_WINDUP);
        boolean[] moved = {false}, recovering = {false};
        long[] recoveryStart = {-1};
        double startX = boss.getX();
        for (int tick = 1; tick <= 110; tick++) h.runAtTickTime(tick, () -> {
            h.assertTrue(boss.getBoundingBox().maxX <= wallX + .001D,
                    "Actual rolling collision box never passes into or through the stone wall");
            health(h, victim, VICTIM_HEALTH, "A victim behind the wall receives no roll damage");
            if (boss.getX() > startX + .5D) moved[0] = true;
            if (boss.getPhase() == GiantCatBoss.ROLL_RECOVERY && !recovering[0]) {
                recovering[0] = true;
                recoveryStart[0] = h.getLevel().getGameTime();
                h.assertTrue(moved[0] && boss.getX() > wallX - 2,
                        "Roll really traveled to the wall before entering collision recovery");
                h.assertTrue(boss.getRollRecoveryStartTicks() < 60,
                        "Wall interrupted a live roll before its normal timeout");
                h.assertTrue(boss.getRecoveryFromPhase() == GiantCatBoss.ROLL
                                && boss.getRecoveryFromTicks() > 0 && boss.getRecoveryFromTicks() < 60,
                        "Collision recovery preserves the actual interrupted roll pose");
            }
            if (recovering[0] && boss.getPhase() == GiantCatBoss.IDLE) {
                h.assertTrue(h.getLevel().getGameTime() - recoveryStart[0] >= 30,
                        "Collision does not skip roll recovery");
                arena.finish();
            }
        });
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_roll_damage", timeoutTicks = 90)
    public static void actualRollSweepDealsTwelveDamage(GameTestHelper h) {
        Arena arena = new Arena(h);
        GiantCatBoss boss = arena.boss(12, 6);
        Cow victim = arena.victim(17, 6);
        boss.setTarget(victim);
        phase(boss, GiantCatBoss.ROLL_WINDUP);
        double startX = boss.getX();
        for (int tick = 1; tick <= 75; tick++) h.runAtTickTime(tick, () -> {
            Vec3 motion = boss.getDeltaMovement();
            if (boss.getPhase() == GiantCatBoss.ROLL && motion.horizontalDistanceSqr() > .04D) {
                float movementYaw = (float) Math.toDegrees(Math.atan2(-motion.x, motion.z));
                h.assertTrue(Math.abs(Mth.wrapDegrees(boss.getYRot() - movementYaw - 90)) < .1F,
                        "Rolling body points 90 degrees sideways to its actual movement, not head-first");
            }
            if (victim.getHealth() < VICTIM_HEALTH) {
                h.assertTrue(boss.getPhase() == GiantCatBoss.ROLL && boss.getX() > startX + 1,
                        "Damage comes from an actual traveling roll, not direct helper invocation");
                health(h, victim, VICTIM_HEALTH - 12, "First swept roll contact deals exactly 12");
                arena.finish();
            }
        });
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_jump_land", timeoutTicks = 125)
    public static void jumpingDamagesOnlyOnRealLandingThenRecoversFortyTicks(GameTestHelper h) {
        Arena arena = new Arena(h);
        GiantCatBoss boss = arena.boss(12, 6);
        Cow victim = arena.victim(20, 6);
        boss.setTarget(victim);
        phase(boss, GiantCatBoss.JUMP_WINDUP);
        boolean[] airborne = {false}, landed = {false};
        long[] recoveryStart = {-1};
        double floorY = boss.getY();
        for (int tick = 1; tick <= 115; tick++) h.runAtTickTime(tick, () -> {
            if (boss.getPhase() == GiantCatBoss.JUMP) {
                if (boss.getY() > floorY + .5 && !boss.onGround()) airborne[0] = true;
                health(h, victim, VICTIM_HEALTH, "Airborne jump cannot produce an early slam");
            }
            if (boss.getPhase() == GiantCatBoss.SLAM_RECOVERY && !landed[0]) {
                landed[0] = true;
                recoveryStart[0] = h.getLevel().getGameTime();
                h.assertTrue(airborne[0] && boss.onGround() && Math.abs(boss.getY() - floorY) < .05,
                        "Slam follows a visible ballistic rise and collision with the real floor");
                health(h, victim, VICTIM_HEALTH - 18, "Landing slam deals exactly 18 damage once");
                var slow = victim.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
                var weak = victim.getEffect(MobEffects.WEAKNESS);
                h.assertTrue(slow != null && weak != null && slow.getAmplifier() == 0 && weak.getAmplifier() == 0
                                && slow.getDuration() >= 99 && slow.getDuration() <= 100
                                && weak.getDuration() >= 99 && weak.getDuration() <= 100,
                        "A successful real landing applies Slowness I and Weakness I for 100 ticks");
                h.assertTrue(boss.getRecoveryFromPhase() == GiantCatBoss.JUMP
                                && boss.getRecoveryFromTicks() > 1 && boss.getRecoveryFromTicks() < 100,
                        "Landing recovery records the actual jump phase and landing time");
            }
            if (landed[0]) health(h, victim, VICTIM_HEALTH - 18, "Recovery never repeats the landing slam");
            if (landed[0] && boss.getPhase() == GiantCatBoss.IDLE) {
                h.assertTrue(h.getLevel().getGameTime() - recoveryStart[0] >= 40,
                        "Landing retains the complete 40-tick vulnerability/recovery window");
                arena.finish();
            }
        });
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_abort", timeoutTicks = 75)
    public static void noAiAndDeathAbortActiveAttacksBeforeFurtherDamage(GameTestHelper h) {
        Arena arena = new Arena(h);
        GiantCatBoss roller = arena.boss(12, 3);
        Cow rollVictim = arena.victim(21, 3);
        GiantCatBoss jumper = arena.boss(40, 10);
        Cow jumpVictim = arena.victim(48, 10);
        roller.setTarget(rollVictim);
        jumper.setTarget(jumpVictim);
        phase(roller, GiantCatBoss.ROLL_WINDUP);
        phase(jumper, GiantCatBoss.JUMP_WINDUP);
        boolean[] stoppedRoll = {false}, stoppedJump = {false};
        float[] stopHealth = {VICTIM_HEALTH, VICTIM_HEALTH};
        Vec3[] stopPosition = {Vec3.ZERO};
        long[] stoppedAt = {-1};
        for (int tick = 1; tick <= 65; tick++) h.runAtTickTime(tick, () -> {
            if (!stoppedRoll[0] && roller.getPhase() == GiantCatBoss.ROLL && roller.getPhaseTicks(0) >= 2) {
                stoppedRoll[0] = true;
                stopHealth[0] = rollVictim.getHealth();
                stopPosition[0] = roller.position();
                roller.setNoAi(true);
                // Put a fresh victim directly into the old swept path before the next real entity tick.
                rollVictim.setPos(roller.position().add(1.6, 0, 0));
            } else if (stoppedRoll[0]) {
                h.assertTrue(roller.getPhase() == GiantCatBoss.IDLE, "NoAI clears the live attack on its next server tick");
                h.assertTrue(roller.position().subtract(stopPosition[0]).horizontalDistanceSqr() < .0025,
                        "NoAI abort has no final damaging rolling movement");
                health(h, rollVictim, stopHealth[0], "NoAI prevents another contact hit");
            }
            if (!stoppedJump[0] && jumper.getPhase() == GiantCatBoss.JUMP && jumper.getY() > arena.at(40, 1, 10).y + 1) {
                stoppedJump[0] = true;
                stopHealth[1] = jumpVictim.getHealth();
                stoppedAt[0] = h.getLevel().getGameTime();
                jumper.hurt(h.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
                h.assertTrue(!jumper.isAlive(), "Fixture genuinely kills the active airborne boss");
            } else if (stoppedJump[0]) {
                health(h, jumpVictim, stopHealth[1], "Dead airborne boss never emits a delayed landing slam");
                if (!jumper.isRemoved()) h.assertTrue(jumper.getPhase() == GiantCatBoss.IDLE,
                        "Death clears the synchronized attack state");
            }
            if (stoppedRoll[0] && stoppedJump[0] && h.getLevel().getGameTime() - stoppedAt[0] >= 25)
                arena.finish();
        });
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_reload", timeoutTicks = 85)
    public static void activeAttackNbtReloadsIntoSafeFullRecovery(GameTestHelper h) {
        Arena arena = new Arena(h);
        GiantCatBoss roller = arena.boss(12, 3);
        GiantCatBoss jumper = arena.boss(40, 10);
        Cow rollVictim = arena.victim(19, 3), jumpVictim = arena.victim(48, 10);
        roller.setTarget(rollVictim);
        phase(roller, GiantCatBoss.ROLL);
        field(roller, "rollYaw", -90F);
        jumper.setTarget(jumpVictim);
        leap(jumper, jumpVictim);
        GiantCatBoss[] restored = new GiantCatBoss[2];
        h.runAfterDelay(5, () -> {
            h.assertTrue(roller.getPhase() == GiantCatBoss.ROLL && jumper.getPhase() == GiantCatBoss.JUMP,
                    "Save captures attacks actually advanced by server ticks");
            GiantCatBoss[] originals = {roller, jumper};
            byte[] recovery = {GiantCatBoss.ROLL_RECOVERY, GiantCatBoss.SLAM_RECOVERY};
            for (int index = 0; index < originals.length; index++) {
                CompoundTag saved = new CompoundTag();
                h.assertTrue(originals[index].saveAsPassenger(saved), "Active boss writes real entity NBT");
                originals[index].discard();
                Entity loaded = EntityType.loadEntityRecursive(saved, h.getLevel(), entity -> entity);
                h.assertTrue(loaded instanceof GiantCatBoss, "NBT restores the registered boss type");
                restored[index] = (GiantCatBoss) loaded;
                h.assertTrue(restored[index].getPhase() == recovery[index]
                                && restored[index].getPhaseTicks(0) == 0,
                        "Interrupted saved attack restarts a complete safe recovery");
                h.assertTrue(restored[index].getDeltaMovement().lengthSqr() < 1e-9 && restored[index].getTarget() == null,
                        "Reload drops stale target and attack velocity");
                h.assertTrue(restored[index].getRecoveryFromPhase() == (index == 0 ? GiantCatBoss.ROLL : GiantCatBoss.JUMP)
                                && restored[index].getRecoveryFromTicks() == saved.getInt("GiantCatPhaseTicks"),
                        "Reloaded recovery starts from the saved active-attack pose, not a full-cycle endpoint");
                h.getLevel().addFreshEntity(restored[index]);
                arena.entities.add(restored[index]);
            }
        });
        h.runAfterDelay(7, () -> {
            h.assertTrue(restored[0].getPhase() == GiantCatBoss.ROLL_RECOVERY
                            && restored[1].getPhase() == GiantCatBoss.SLAM_RECOVERY,
                    "Real post-load server ticks cannot restart an interrupted attack");
        });
        h.runAfterDelay(15, () -> {
            for (int index = 0; index < restored.length; index++) {
                GiantCatBoss recovering = restored[index];
                byte from = recovering.getRecoveryFromPhase();
                float fromTicks = recovering.getRecoveryFromTicks(), progress = recovering.getPhaseTicks(0);
                h.assertTrue(progress >= 8 && progress < 20, "Second save occurs partway through real recovery");
                CompoundTag saved = new CompoundTag();
                h.assertTrue(recovering.saveAsPassenger(saved), "Partly recovered boss writes real entity NBT");
                recovering.discard();
                restored[index] = (GiantCatBoss) EntityType.loadEntityRecursive(saved, h.getLevel(), entity -> entity);
                h.assertTrue(restored[index] != null && restored[index].getRecoveryFromPhase() == from
                                && restored[index].getRecoveryFromTicks() == fromTicks
                                && Math.abs(restored[index].getPhaseTicks(0) - progress) < .01F,
                        "Reloading recovery preserves both source pose and elapsed recovery instead of rewinding");
                h.getLevel().addFreshEntity(restored[index]);
                arena.entities.add(restored[index]);
            }
        });
        h.runAfterDelay(49, () -> {
            h.assertTrue(restored[0].getPhase() == GiantCatBoss.IDLE && restored[1].getPhase() == GiantCatBoss.IDLE,
                    "Both loaded recoveries finish normally rather than remaining frozen");
            health(h, rollVictim, VICTIM_HEALTH, "Loaded roll cannot hit a stale victim");
            health(h, jumpVictim, VICTIM_HEALTH, "Loaded jump never slams after load-time falling");
            arena.finish();
        });
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_reward", timeoutTicks = 30)
    public static void rewardThresholdIsTwentyFivePercentAndLootingNeverBoostsIt(GameTestHelper h) {
        Arena arena = new Arena(h);
        GiantCatBoss boss = arena.boss(12, 6);
        boss.setNoAi(true);
        ServerPlayer player = FakePlayerFactory.get(h.getLevel(),
                new GameProfile(UUID.randomUUID(), "giant-drop-probe"));
        arena.entities.add(player);
        player.setPos(arena.at(60, 1, 6));
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(Enchantments.MOB_LOOTING, 3);
        var collar = BuiltInRegistries.ITEM.get(id("cat_giant_collar"));
        h.assertTrue(collar != Items.AIR, "Reward uses the actual registered giant collar");
        // Hand-checked java.util.Random/LegacyRandomSource first floats:
        // seed7011=.24991244 (<.25); seed7010=.25000203 (>=.25).
        // Isolate the real custom loot hook from unrelated vanilla death-table RNG;
        // no hit/drop implementation is mocked, and emitted ItemEntities are observed.
        long[] seeds = {7011, 7010, 4096, 0};
        int[] expected = {1, 0, 1, 0};
        h.runAfterDelay(2, () -> {
            for (int looting : new int[]{0, 3}) for (int index = 0; index < seeds.length; index++) {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                        looting == 0 ? ItemStack.EMPTY : sword.copy());
                boss.getRandom().setSeed(seeds[index]);
                customLoot(boss, h, h.getLevel().damageSources().playerAttack(player), looting);
                List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class,
                        new AABB(boss.position(), boss.position()).inflate(4));
                h.assertTrue(drops.stream().filter(drop -> drop.getItem().is(collar)).count() == expected[index],
                        "Fixed 25% threshold does not change with Looting III: seed=" + seeds[index] + ", looting=" + looting);
                for (ItemEntity drop : drops) {
                    if (drop.getItem().is(collar)) h.assertTrue(drop.getItem().getCount() == 1,
                            "A successful trophy roll produces one collar, never a bonus stack");
                    drop.discard();
                }
            }
            arena.finish();
        });
    }


    @GameTest(template = "artillery_probe", batch = "giant_boss_low_ceiling", timeoutTicks = 95)
    public static void lowCeilingLandingRecoversFromTheActualEarlyJumpPose(GameTestHelper h) {
        Arena arena = new Arena(h);
        GiantCatBoss boss = arena.boss(12, 6);
        Cow victim = arena.victim(16, 6);
        // Boss is three blocks tall at floor Y1; roof Y4 touches its head without
        // intersecting its standing collision box. An upward sweep must hit it.
        for (int x = 10; x <= 20; x++) for (int z = 3; z <= 9; z++)
            h.setBlock(new BlockPos(x, 4, z), Blocks.STONE);
        boss.setTarget(victim);
        phase(boss, GiantCatBoss.JUMP_WINDUP);
        boolean[] sawJump = {false}, recovered = {false};
        long[] recoveryStart = {-1};
        double floorY = boss.getY();
        for (int tick = 1; tick <= 85; tick++) h.runAtTickTime(tick, () -> {
            h.assertTrue(boss.getY() <= floorY + .01D,
                    "Actual upward collision cannot carry the three-block boss through the low roof");
            if (boss.getPhase() == GiantCatBoss.JUMP) {
                sawJump[0] = true;
                health(h, victim, VICTIM_HEALTH, "Ceiling collision itself is not a mid-air slam");
            }
            if (boss.getPhase() == GiantCatBoss.SLAM_RECOVERY && !recovered[0]) {
                recovered[0] = true;
                recoveryStart[0] = h.getLevel().getGameTime();
                h.assertTrue(sawJump[0] && boss.onGround(), "Low ceiling jump ends on real downward floor contact");
                h.assertTrue(boss.getRecoveryFromPhase() == GiantCatBoss.JUMP
                                && boss.getRecoveryFromTicks() >= 2 && boss.getRecoveryFromTicks() <= 4,
                        "Early landing retains its two-to-four-tick jump pose rather than a full-flight endpoint");
                health(h, victim, VICTIM_HEALTH - 18, "Only the real early landing performs the slam");
            }
            if (recovered[0] && boss.getPhase() == GiantCatBoss.SLAM_RECOVERY)
                health(h, victim, VICTIM_HEALTH - 18, "Low-roof recovery cannot repeat slam");
            if (recovered[0] && boss.getPhase() == GiantCatBoss.IDLE) {
                h.assertTrue(h.getLevel().getGameTime() - recoveryStart[0] >= 40,
                        "An early landing still has the full 40-tick recovery");
                arena.finish();
            }
        });
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_lost_target", timeoutTicks = 80)
    public static void losingTargetDuringEitherWindupRecoversFromItsPartialPose(GameTestHelper h) {
        Arena arena = new Arena(h);
        GiantCatBoss roller = arena.boss(12, 3), jumper = arena.boss(40, 10);
        Cow rollVictim = arena.victim(20, 3), jumpVictim = arena.victim(48, 10);
        roller.setTarget(rollVictim); jumper.setTarget(jumpVictim);
        phase(roller, GiantCatBoss.ROLL_WINDUP); phase(jumper, GiantCatBoss.JUMP_WINDUP);
        GiantCatBoss[] bosses = {roller, jumper};
        byte[] expected = {GiantCatBoss.ROLL_RECOVERY, GiantCatBoss.SLAM_RECOVERY};
        byte[] source = {GiantCatBoss.ROLL_WINDUP, GiantCatBoss.JUMP_WINDUP};
        int[] duration = {30, 40};
        Vec3[] origin = {roller.position(), jumper.position()};
        long[] started = {-1, -1};
        boolean[] finished = {false, false};
        h.runAtTickTime(6, () -> {
            h.assertTrue(roller.getPhase() == GiantCatBoss.ROLL_WINDUP
                            && jumper.getPhase() == GiantCatBoss.JUMP_WINDUP,
                    "Targets disappear while both real telegraphs are incomplete");
            rollVictim.discard(); jumpVictim.discard();
        });
        for (int tick = 7; tick <= 70; tick++) h.runAtTickTime(tick, () -> {
            for (int index = 0; index < bosses.length; index++) {
                GiantCatBoss boss = bosses[index];
                h.assertTrue(boss.position().subtract(origin[index]).horizontalDistanceSqr() < .0025D,
                        "Canceled windup never launches a damaging roll or leap");
                h.assertTrue(boss.getPhase() != GiantCatBoss.ROLL && boss.getPhase() != GiantCatBoss.JUMP,
                        "Invalid target cannot advance the windup into its active attack");
                if (boss.getPhase() == expected[index] && started[index] < 0) {
                    started[index] = h.getLevel().getGameTime();
                    h.assertTrue(boss.getRecoveryFromPhase() == source[index]
                                    && boss.getRecoveryFromTicks() >= 6 && boss.getRecoveryFromTicks() <= 8,
                            "Canceled recovery preserves the actual partial windup pose");
                }
                if (boss.getPhase() == GiantCatBoss.IDLE && !finished[index]) {
                    h.assertTrue(started[index] >= 0
                                    && h.getLevel().getGameTime() - started[index] >= duration[index],
                            "Lost target recovery completes its full vulnerability window before idle");
                    finished[index] = true;
                }
            }
            if (finished[0] && finished[1]) arena.finish();
        });
    }


@GameTest(template = "artillery_probe", batch = "giant_boss_recovery_heading", timeoutTicks = 45)
    public static void rollRecoveryAndFirstIdleTickRetainActualBodyHeading(GameTestHelper h) {
        Arena arena = new Arena(h);
        GiantCatBoss boss = arena.boss(30, 6);
        boss.setYRot(-37F); boss.yBodyRot = -37F; boss.yHeadRot = -37F;
        field(boss, "rollYaw", -127F);
        phase(boss, GiantCatBoss.ROLL);
        phase(boss, GiantCatBoss.ROLL_RECOVERY);
        for (int tick = 1; tick <= 32; tick++) {
            final int time = tick;
            h.runAtTickTime(tick, () -> {
                h.assertTrue(Math.abs(Mth.wrapDegrees(boss.getYRot() + 37F)) < .01F
                                && Math.abs(Mth.wrapDegrees(boss.yBodyRot + 37F)) < .01F,
                        "Recovery and entry into idle preserve the actual heading without a compulsory quarter turn");
                if (time == 32) {
                    h.assertTrue(boss.getPhase() == GiantCatBoss.IDLE, "Full recovery still finishes");
                    arena.finish();
                }
            });
        }
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_head_contact", timeoutTicks = 20)
    public static void noseContactOutsideBodyHitsButTorsoRearAndHighPlayersDoNot(GameTestHelper h) {
        Arena arena = new Arena(h);
        var previousDifficulty = h.getLevel().getDifficulty();
        arena.cleanups.add(() -> h.getLevel().getServer().setDifficulty(previousDifficulty, true));
        h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        GiantCatBoss[] bosses = new GiantCatBoss[4];
        ServerPlayer[][] players = new ServerPlayer[4][4];
        for (int index = 0; index < 4; index++) {
            var boss = bosses[index] = arena.boss(8 + index * 15, 6);
            boss.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
            field(boss, "attackCooldown", 1000);
            double angle = index * Math.PI / 2;
            Vec3 forward = new Vec3(-Math.sin(angle), 0, Math.cos(angle));
            players[index][0] = arena.player(boss.position().add(forward.scale(3.15)).add(0, 6, 0));
            players[index][1] = arena.player(boss.position());
            players[index][2] = arena.player(boss.position().subtract(forward.scale(2.5)));
            players[index][3] = arena.player(boss.position().add(forward.scale(3.15)).add(0, 3.2, 0));
        }
        h.runAtTickTime(2, () -> {
            for (int index = 0; index < 4; index++) {
                GiantCatBoss boss = bosses[index];
                boss.setYRot(index * 90F); boss.yBodyRot = index * 90F; boss.yHeadRot = index * 90F;
                double angle = index * Math.PI / 2;
                players[index][0].setPos(boss.position().add(-Math.sin(angle) * 3.15, 0, Math.cos(angle) * 3.15));
                for (ServerPlayer player : players[index]) { player.setHealth(VICTIM_HEALTH); player.invulnerableTime = 0; }
                h.assertTrue(!boss.getBoundingBox().intersects(players[index][0].getBoundingBox()),
                        "Nose fixture lies outside the physical body box");
                h.assertTrue(boss.doHurtTarget(players[index][0]), "Actual nose contact must bite at every cardinal heading");
                health(h, players[index][0], VICTIM_HEALTH - 8, "Nose contact deals eight damage");
                for (int player = 1; player < 4; player++) {
                    h.assertTrue(!boss.doHurtTarget(players[index][player]),
                            "Torso, rear and above-head contact cannot count as a bite");
                    health(h, players[index][player], VICTIM_HEALTH, "Non-head contact remains unharmed");
                }
            }
            arena.finish();
        });
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_idle_contact", timeoutTicks = 95)
    public static void idlePlayerContactDealsEightPerPlayerAtMostEveryTwentyTicks(GameTestHelper h) {
        Arena arena = new Arena(h);
        var previousDifficulty = h.getLevel().getDifficulty();
        arena.cleanups.add(() -> h.getLevel().getServer().setDifficulty(previousDifficulty, true));
        h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        GiantCatBoss boss = arena.boss(12, 6);
        boss.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        field(boss, "attackCooldown", 1000);
        boss.setYRot(-90F); boss.yBodyRot = -90F; boss.yHeadRot = -90F;
        ServerPlayer first = arena.player(boss.position().add(2.6, 0, .2));
        ServerPlayer second = arena.player(boss.position().add(2.6, 0, -.2));
        ServerPlayer outside = arena.player(boss.position().add(-.7, 0, 0));
        boss.setTarget(first);
        ServerPlayer[] players = {first, second};
        float[] previous = {VICTIM_HEALTH, VICTIM_HEALTH};
        long[] lastHit = {-1000, -1000};
        int[] hits = {0, 0};
        float[] separated = {0, 0};
        for (int tick = 1; tick <= 75; tick++) {
            final int time = tick;
            h.runAtTickTime(tick, () -> {
                h.assertTrue(boss.getPhase() == GiantCatBoss.IDLE,
                        "Ordinary head contact stays in IDLE instead of a melee attack stage");
                // This fixture measures per-player cooldown, not target choice.
                // Its nearer torso-only player must not pull the muzzle away.
                boss.setTarget(first);
                boss.setYRot(-90F); boss.yBodyRot = -90F; boss.yHeadRot = -90F;
                for (int index = 0; index < players.length; index++) {
                    ServerPlayer player = players[index];
                    float damage = previous[index] - player.getHealth();
                    if (damage > .001F) {
                        h.assertTrue(Math.abs(damage - 8) < .001F, "Unarmored NORMAL player contact deals exactly 8 damage");
                        long now = h.getLevel().getGameTime();
                        h.assertTrue(now - lastHit[index] >= 20, "Each player's normal contact cooldown is at least 20 real ticks");
                        lastHit[index] = now;
                        hits[index]++;
                    }
                    previous[index] = player.getHealth();
                    // Remove vanilla immunity so the boss's own contact cooldown is what is tested.
                    player.invulnerableTime = 0;
                    player.setDeltaMovement(Vec3.ZERO);
                    player.getFoodData().setFoodLevel(17);
                    player.setPos(boss.position().add(time < 50 ? 2.6 : 5.0, 0, index == 0 ? .2 : -.2));
                    if (time == 50) separated[index] = player.getHealth();
                    if (time > 50) health(h, player, separated[index], "Non-overlapping nearby players never receive ranged contact damage");
                }
                outside.setPos(boss.position().add(-.7, 0, 0));
                outside.setDeltaMovement(Vec3.ZERO);
                health(h, outside, VICTIM_HEALTH, "Body contact outside the head is never bitten");
                if (time == 5) h.assertTrue(hits[0] == 1 && hits[1] == 1,
                        "Both overlapping actual players are hit independently on the first few server ticks");
                if (time == 75) {
                    h.assertTrue(hits[0] >= 2 && hits[1] >= 2, "Continued overlap repeats contact after the per-player cooldown"
                            + ": hits=" + hits[0] + "/" + hits[1] + ", bodyYaw=" + boss.yBodyRot
                            + ", target=" + (boss.getTarget() == first ? "first" : boss.getTarget() == second ? "second" : "body fixture"));
                    arena.finish();
                }
            });
        }
    }


    @GameTest(template = "artillery_probe", batch = "giant_boss_contact_phase_safety", timeoutTicks = 25)
    public static void summonWindupsRecoveriesAndAirborneJumpHaveNoOrdinaryContactDamage(GameTestHelper h) {
        Arena arena = new Arena(h);
        byte[] phases = {GiantCatBoss.SUMMON, GiantCatBoss.ROLL_WINDUP, GiantCatBoss.JUMP_WINDUP,
                GiantCatBoss.ROLL_RECOVERY, GiantCatBoss.SLAM_RECOVERY, GiantCatBoss.JUMP};
        GiantCatBoss[] bosses = new GiantCatBoss[phases.length];
        ServerPlayer[] players = new ServerPlayer[phases.length];
        for (int index = 0; index < phases.length; index++) {
            GiantCatBoss boss = arena.boss(6 + index * 11, 6);
            bosses[index] = boss;
            if (phases[index] == GiantCatBoss.JUMP) boss.setPos(boss.position().add(0, 4, 0));
            players[index] = arena.player(boss.position().add(.5, 0, 0));
            boss.setTarget(players[index]);
            field(boss, "attackCooldown", 1000);
            phase(boss, phases[index]);
            boss.setDeltaMovement(Vec3.ZERO);
        }
        for (int tick = 1; tick <= 8; tick++) {
            final int time = tick;
            h.runAtTickTime(tick, () -> {
                for (int index = 0; index < phases.length; index++) {
                    h.assertTrue(bosses[index].getPhase() == phases[index],
                            "Fixture still observes the intended non-IDLE state, including a genuinely airborne jump");
                    health(h, players[index], VICTIM_HEALTH,
                            "Overlapping player receives no ordinary contact damage during phase " + phases[index]);
                    players[index].invulnerableTime = 0;
                    players[index].setPos(bosses[index].position().add(.5, 0, 0));
                    players[index].setDeltaMovement(Vec3.ZERO);
                }
                if (time == 8) arena.finish();
            });
        }
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_slow_sideways_turn", timeoutTicks = 80)
    public static void liveRollTurnsSlowlyWhileBothBodyHeadingsRemainSideways(GameTestHelper h) {
        Arena arena = new Arena(h);
        GiantCatBoss boss = arena.boss(18, 6);
        Cow victim = arena.victim(38, 6);
        boss.setTarget(victim);
        phase(boss, GiantCatBoss.ROLL_WINDUP);
        Vec3[] previousPosition = {boss.position()};
        float[] previousMovementYaw = {0}, previousBodyYaw = {0};
        int[] movingFrames = {0};
        float[] firstMovementYaw = {0};
        for (int tick = 1; tick <= 65; tick++) h.runAtTickTime(tick, () -> {
            Vec3 movement = boss.position().subtract(previousPosition[0]);
            previousPosition[0] = boss.position();
            if (boss.getPhase() != GiantCatBoss.ROLL || movement.horizontalDistanceSqr() < .04D) return;
            float yaw = (float) Math.toDegrees(Math.atan2(-movement.x, movement.z));
            h.assertTrue(Math.abs(Mth.wrapDegrees(boss.getYRot() - yaw - 90)) < .15F
                            && Math.abs(Mth.wrapDegrees(boss.yBodyRot - yaw - 90)) < .15F,
                    "Both actual entity yaw and body-control yaw are sideways to measured displacement");
            if (movingFrames[0] > 0) {
                h.assertTrue(Math.abs(Mth.wrapDegrees(yaw - previousMovementYaw[0])) <= 3.1F
                                && Math.abs(Mth.wrapDegrees(boss.getYRot() - previousBodyYaw[0])) <= 3.1F,
                        "Retargeting changes roll motion/body heading by at most three degrees per real tick");
            } else firstMovementYaw[0] = yaw;
            previousMovementYaw[0] = yaw;
            previousBodyYaw[0] = boss.getYRot();
            movingFrames[0]++;
            if (movingFrames[0] == 3) {
                victim.setPos(boss.position().add(12, 0, 5));
                victim.setDeltaMovement(Vec3.ZERO);
            }
            if (movingFrames[0] == 13) {
                h.assertTrue(Math.abs(Mth.wrapDegrees(yaw - firstMovementYaw[0])) > 8,
                        "Live roll actually responds to the moved target rather than freezing its direction");
                arena.finish();
            }
        });
    }


    @GameTest(template = "artillery_probe", batch = "giant_boss_windup_yaw_boundary", timeoutTicks = 45)
    public static void movingTargetAcrossWindupHalfTurnNeverFlipsBodyOrRelocksLaunch(GameTestHelper h) {
        Arena arena = new Arena(h);
        GiantCatBoss boss = arena.boss(36, 6);
        Cow target = arena.victim(44, 6);
        float entryAim = 66.0F;
        double initial = Math.toRadians(entryAim);
        target.setPos(boss.position().add(-Math.sin(initial) * 8, 0, Math.cos(initial) * 8));
        boss.setYRot(0); boss.yRotO = 0; boss.yBodyRot = 0; boss.yBodyRotO = 0; boss.yHeadRot = 0;
        boss.setTarget(target);
        phase(boss, GiantCatBoss.ROLL_WINDUP);
        float[] previousBody = {0}, previousControlBody = {0};
        Vec3[] previousPosition = {boss.position()};
        int[] observedWindup = {0};
        // The old implementation tracks 66,69,...,87,90 degrees during windup.
        // Its body endpoint crosses +180 relative to entry0 and shortest-arc
        // interpolation flips sign mid-animation. Only real entity ticks turn it.
        for (int tick = 1; tick <= 32; tick++) h.runAtTickTime(tick, () -> {
            Vec3 movement = boss.position().subtract(previousPosition[0]);
            previousPosition[0] = boss.position();
            h.assertTrue(Math.abs(Mth.wrapDegrees(boss.getYRot() - previousBody[0])) <= 20.0F
                            && Math.abs(Mth.wrapDegrees(boss.yBodyRot - previousControlBody[0])) <= 20.0F,
                    "Moving the target across the windup's 180-degree boundary must not flip the body");
            previousBody[0] = boss.getYRot();
            previousControlBody[0] = boss.yBodyRot;
            if (boss.getPhase() == GiantCatBoss.ROLL_WINDUP) {
                observedWindup[0]++;
                double desired = Math.toRadians(entryAim + Math.min(54, boss.getPhaseTicks(0) * 3));
                target.setPos(boss.position().add(-Math.sin(desired) * 8, 0, Math.cos(desired) * 8));
                target.setDeltaMovement(Vec3.ZERO);
            } else if (boss.getPhase() == GiantCatBoss.ROLL && movement.horizontalDistanceSqr() > .04D) {
                h.assertTrue(observedWindup[0] >= 12, "Fixture traversed the actual sixteen-tick telegraph");
                float firstMovementYaw = (float) Math.toDegrees(Math.atan2(-movement.x, movement.z));
                h.assertTrue(Math.abs(Mth.wrapDegrees(firstMovementYaw - entryAim)) <= 3.1F,
                        "First real roll step steers at most three degrees from the direction locked on windup entry");
                arena.finish();
            }
        });
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_roll_launch_single_steer", timeoutTicks = 45)
    public static void targetMovesOnLastWindupTickButLaunchSteersOnlyOnce(GameTestHelper h) {
        Arena arena = new Arena(h);
        GiantCatBoss boss = arena.boss(36, 6);
        Cow target = arena.victim(44, 6); // Exact movement yaw -90, sideways body yaw 0.
        boss.setYRot(0); boss.yRotO = 0; boss.yBodyRot = 0; boss.yBodyRotO = 0; boss.yHeadRot = 0;
        boss.setTarget(target);
        phase(boss, GiantCatBoss.ROLL_WINDUP);
        boolean[] movedTarget = {false};
        Vec3[] previousPosition = {boss.position()};
        for (int tick = 1; tick <= 32; tick++) h.runAtTickTime(tick, () -> {
            Vec3 movement = boss.position().subtract(previousPosition[0]);
            previousPosition[0] = boss.position();
            if (boss.getPhase() == GiantCatBoss.ROLL_WINDUP && boss.getPhaseTicks(0) >= 15 && !movedTarget[0]) {
                double angle = Math.toRadians(-60);
                target.setPos(boss.position().add(-Math.sin(angle) * 8, 0, Math.cos(angle) * 8));
                target.setDeltaMovement(Vec3.ZERO);
                movedTarget[0] = true;
            } else if (boss.getPhase() == GiantCatBoss.ROLL && movement.horizontalDistanceSqr() > .04D) {
                h.assertTrue(movedTarget[0], "Target moved after fifteen real windup ticks and before launch");
                float firstMovementYaw = (float) Math.toDegrees(Math.atan2(-movement.x, movement.z));
                h.assertTrue(Math.abs(Mth.wrapDegrees(firstMovementYaw + 90)) <= 3.1F,
                        "Launch tick must not steer once in AI and again in travel, producing a six-degree turn");
                h.assertTrue(Math.abs(Mth.wrapDegrees(boss.getYRot() - firstMovementYaw - 90)) < .15F,
                        "Single-steer launch still has sideways body orientation");
                arena.finish();
            }
        });
    }

@GameTest(template = "artillery_probe", batch = "giant_boss_shortcut_terrain", timeoutTicks = 20)
    public static void pathSmoothingDoesNotSkipIntoNewDangerousTerrain(GameTestHelper h) {
        Arena arena = new Arena(h);
        GiantCatBoss boss = arena.boss(12, 6);
        boss.setNoAi(true); boss.setOnGround(true);
        BlockPos origin = h.absolutePos(new BlockPos(11, 1, 5));
        var nodes = new ArrayList<net.minecraft.world.level.pathfinder.Node>();
        for (int[] offset : new int[][]{{0,0},{1,0},{2,0},{2,2}}) {
            var node = new net.minecraft.world.level.pathfinder.Node(origin.getX()+offset[0],
                    origin.getY(),origin.getZ()+offset[1]);
            node.type = net.minecraft.world.level.pathfinder.BlockPathTypes.WALKABLE;
            nodes.add(node);
        }
        var path = new net.minecraft.world.level.pathfinder.Path(nodes, origin.offset(2,0,2), true);
        boss.getNavigation().moveTo(path, 1);
        // A hazard can appear after the native path was computed. It has no
        // solid collision and still provides footing, but is not safe to cut.
        h.setBlock(new BlockPos(14, 0, 8), Blocks.MAGMA_BLOCK);
        try {
            Method smooth = GiantCatBoss.class.getDeclaredMethod("smoothPursuitPath");
            smooth.setAccessible(true); smooth.invoke(boss);
        } catch (ReflectiveOperationException error) { throw new AssertionError("Exercise real path shortcut", error); }
        h.assertTrue(path.getNextNodeIndex() < 3,
                "The real smoothing consumer must not skip directly onto a magma waypoint: " + path.getNextNodeIndex());
        arena.finish();
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_step_alignment", timeoutTicks = 20)
    public static void ordinaryStepJumpWaitsForAlignmentWithTheRaisedWaypoint(GameTestHelper h) {
        Arena arena = new Arena(h);
        GiantCatBoss boss = arena.boss(12, 6);
        Cow target = arena.victim(18, 6);
        boss.setTarget(target); boss.setOnGround(true);
        boss.setYRot(90); boss.yBodyRot = boss.yHeadRot = 90;
        h.setBlock(new BlockPos(13, 1, 6), Blocks.STONE);
        Vec3 step = arena.at(13, 2, 6);
        // Drive the production controller at its real navigation boundary.
        // Vanilla queues JumpControl here even if motion is zeroed afterwards.
        boss.getMoveControl().setWantedPosition(step.x, step.y, step.z, 1);
        boss.getMoveControl().tick();
        boss.getJumpControl().tick();
        try {
            Field jumping = LivingEntity.class.getDeclaredField("jumping");
            jumping.setAccessible(true);
            h.assertTrue(!jumping.getBoolean(boss),
                    "An opposite-facing ordinary step must not queue an airborne jump before alignment");
        } catch (ReflectiveOperationException error) { throw new AssertionError("Observe actual jump request", error); }
        arena.finish();
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_smooth_pursuit", timeoutTicks = 230)
    public static void ordinaryPursuitTurnsSmoothlyWithoutOrbitingBeforeItsFirstBite(GameTestHelper h) {
        Arena arena = new Arena(h);
        var previousDifficulty = h.getLevel().getDifficulty();
        arena.cleanups.add(() -> h.getLevel().getServer().setDifficulty(previousDifficulty, true));
        h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        GiantCatBoss boss = arena.boss(12, 6);
        ServerPlayer player = arena.player(arena.at(32, 1, 9));
        field(boss, "attackCooldown", 1000);
        boss.setYRot(100);
        boss.yBodyRot = boss.yHeadRot = 100;
        float[] yaw = {100}, body = {100}, totalTurn = {0};
        float[] unwrapped = {100}, lowest = {100}, highest = {100};
        for (int tick = 1; tick <= 215; tick++) {
            final int time = tick;
            h.runAtTickTime(tick, () -> {
                float turn = Math.abs(Mth.wrapDegrees(boss.getYRot() - yaw[0]));
                float bodyTurn = Math.abs(Mth.wrapDegrees(boss.yBodyRot - body[0]));
                h.assertTrue(bodyTurn <= 10.1F,
                        "The rendered body must also turn smoothly while paused to align: tick=" + time
                                + ", bodyTurn=" + bodyTurn + ", body=" + boss.yBodyRot
                                + ", " + pursuitState(boss, player));
                body[0] = boss.yBodyRot;
                totalTurn[0] += turn;
                unwrapped[0] += Mth.wrapDegrees(boss.getYRot() - yaw[0]);
                lowest[0] = Math.min(lowest[0], unwrapped[0]);
                highest[0] = Math.max(highest[0], unwrapped[0]);
                h.assertTrue(turn <= 10.1F,
                        "Ordinary AI cannot snap toward a path node: tick=" + time + ", turn=" + turn
                                + ", totalTurn=" + totalTurn[0] + ", " + pursuitState(boss, player));
                // Absolute angular travel wrongly counts necessary small path
                // corrections as a full spin. Unwrapped angular span detects
                // a real revolution, including one followed by a reversal.
                h.assertTrue(highest[0] - lowest[0] < 230,
                        "A stationary target must not make ordinary pursuit orbit/spin before contact: "
                                + "angularSpan=" + (highest[0] - lowest[0])
                                + ", absoluteTravel=" + totalTurn[0] + ", " + pursuitState(boss, player));
                float directAim = (float) (Mth.atan2(player.getZ() - boss.getZ(), player.getX() - boss.getX())
                        * Mth.RAD_TO_DEG) - 90F;
                if (time >= 50) h.assertTrue(Math.abs(Mth.wrapDegrees(directAim - boss.getYRot())) < 30,
                        "An open diagonal route must not repeatedly veer sideways away from the target: "
                                + pursuitState(boss, player));
                yaw[0] = boss.getYRot();
                player.setDeltaMovement(Vec3.ZERO);
                player.getFoodData().setFoodLevel(17);
                if (player.getHealth() < VICTIM_HEALTH) arena.finish();
                else if (time == 215) h.assertTrue(false,
                        "Smooth steering must still arrive and bite, rather than merely stop rotating: " + pursuitState(boss, player));
            });
        }
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_smooth_wall_route", timeoutTicks = 270)
    public static void smoothPursuitStillRoutesAroundAFullHeightWallAndBites(GameTestHelper h) {
        Arena arena = new Arena(h);
        var previousDifficulty = h.getLevel().getDifficulty();
        arena.cleanups.add(() -> h.getLevel().getServer().setDifficulty(previousDifficulty, true));
        h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        for (int y = 1; y <= 4; y++) for (int z = 2; z <= 9; z++)
            h.setBlock(new BlockPos(22, y, z), Blocks.STONE);
        GiantCatBoss boss = arena.boss(12, 6);
        ServerPlayer player = arena.player(arena.at(32, 1, 6));
        // The wall deliberately blocks sight; isolate an already acquired target,
        // not the independent player-acquisition/line-of-sight policy.
        boss.setTarget(player);
        field(boss, "attackCooldown", 1000);
        boss.setYRot(-90);
        boss.yBodyRot = boss.yHeadRot = -90;
        float[] yaw = {-90};
        for (int tick = 1; tick <= 255; tick++) {
            final int time = tick;
            h.runAtTickTime(tick, () -> {
                float turn = Math.abs(Mth.wrapDegrees(boss.getYRot() - yaw[0]));
                h.assertTrue(turn <= 10.1F,
                        "Wall path nodes must not cause sudden sideways turns: tick=" + time + ", turn=" + turn
                                + ", " + pursuitState(boss, player));
                yaw[0] = boss.getYRot();
                player.setDeltaMovement(Vec3.ZERO);
                player.getFoodData().setFoodLevel(17);
                if (player.getHealth() < VICTIM_HEALTH) arena.finish();
                else if (time == 255) h.assertTrue(false,
                        "The full-body wall must be navigated, not crossed or stalled against: " + pursuitState(boss, player));
            });
        }
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_yaw_boundary", timeoutTicks = 110)
    public static void pursuitAcrossNorthUsesTheShortArcRatherThanAFullRotation(GameTestHelper h) {
        Arena arena = new Arena(h);
        var previousDifficulty = h.getLevel().getDifficulty();
        arena.cleanups.add(() -> h.getLevel().getServer().setDifficulty(previousDifficulty, true));
        h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        GiantCatBoss boss = arena.boss(30, 9);
        Vec3 target = arena.at(30, 1, 3);
        ServerPlayer player = arena.player(target.add(.12, 0, 0));
        field(boss, "attackCooldown", 1000);
        boss.setYRot(179);
        boss.yBodyRot = boss.yHeadRot = 179;
        float[] yaw = {179}, totalTurn = {0};
        for (int tick = 1; tick <= 90; tick++) {
            final int time = tick;
            h.runAtTickTime(tick, () -> {
                float turn = Math.abs(Mth.wrapDegrees(boss.getYRot() - yaw[0]));
                totalTurn[0] += turn;
                h.assertTrue(turn <= 10.1F && totalTurn[0] < 140,
                        "Crossing +/-180 degrees must retain shortest-arc steering, not spin: tick=" + time
                                + ", turn=" + turn + ", totalTurn=" + totalTurn[0] + ", " + pursuitState(boss, player));
                yaw[0] = boss.getYRot();
                if (time % 15 == 0) player.setPos(target.add(time % 30 == 0 ? .12 : -.12, 0, 0));
                player.setDeltaMovement(Vec3.ZERO);
                player.getFoodData().setFoodLevel(17);
                if (time == 90) {
                    h.assertTrue(player.getHealth() < VICTIM_HEALTH,
                            "Boundary-angle pursuit still reaches real contact damage: " + pursuitState(boss, player));
                    arena.finish();
                }
            });
        }
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_player_pursuit", timeoutTicks = 210)
    public static void ordinaryAiFindsAndPursuesAPlayerAllTheWayToContactThenRepaths(GameTestHelper h) {
        Arena arena = new Arena(h);
        var previousDifficulty = h.getLevel().getDifficulty();
        arena.cleanups.add(() -> h.getLevel().getServer().setDifficulty(previousDifficulty, true));
        h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        GiantCatBoss boss = arena.boss(12, 6);
        ServerPlayer player = arena.player(arena.at(22, 1, 6));
        BossSoundWatch sounds = new BossSoundWatch(arena, boss);
        // Isolate ordinary pursuit, not navigation: real target selection, path
        // finding, movement, overlap and damage all run on actual server ticks.
        field(boss, "attackCooldown", 1000);
        Vec3 origin = boss.position();
        int[] bites = {0};
        float[] previousHealth = {VICTIM_HEALTH};
        long[] movedAt = {-1};
        for (int tick = 1; tick <= 195; tick++) {
            final int time = tick;
            h.runAtTickTime(tick, () -> {
                h.assertTrue(boss.getPhase() == GiantCatBoss.IDLE && sounds.hisses == 0,
                        "Ordinary pursuit/bite has neither an attack animation phase nor CAT_HISS");
                float damage = previousHealth[0] - player.getHealth();
                if (damage > .001F) {
                    h.assertTrue(Math.abs(damage - 8) < .001F,
                            "Natural ordinary pursuit ends with an eight-damage contact bite");
                    h.assertTrue(boss.distanceTo(player) > 1.3 && boss.distanceTo(player) < 3.8,
                            "Navigation brings the actual head to the player without requiring central body overlap");
                    bites[0]++;
                    if (bites[0] == 1) {
                        movedAt[0] = h.getLevel().getGameTime();
                        player.setPos(boss.position().add(6, 0, 0));
                    } else {
                        h.assertTrue(h.getLevel().getGameTime() - movedAt[0] >= 20,
                                "Pursuing again retains the per-player bite cooldown");
                        arena.finish();
                    }
                }
                previousHealth[0] = player.getHealth();
                player.setDeltaMovement(Vec3.ZERO);
                player.getFoodData().setFoodLevel(17);
                if (time == 45) h.assertTrue(boss.getTarget() == player
                                && boss.position().subtract(origin).horizontalDistanceSqr() > 4,
                        "Unassigned natural player target is acquired and genuinely pursued");
                if (time == 105) h.assertTrue(bites[0] >= 1,
                        "Ordinary navigation must close its stopping gap and reach a first actual bite: " + pursuitState(boss, player));
                if (time == 195) h.assertTrue(bites[0] >= 2,
                        "A player who moves away is repathed and bitten again: " + pursuitState(boss, player));
            });
        }
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_legacy_balance", timeoutTicks = 30)
    public static void legacySavedAttributesUpgradeWithoutHealingOrDoubleScaling(GameTestHelper h) {
        Arena arena = new Arena(h);
        GiantCatBoss old = arena.boss(12, 6);
        old.getAttribute(Attributes.MAX_HEALTH).setBaseValue(550);
        old.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.27D);
        old.setHealth(275);
        old.setNoAi(true);
        CompoundTag legacy = new CompoundTag();
        old.saveWithoutId(legacy);
        old.discard();
        GiantCatBoss loaded = arena.boss(12, 6);
        loaded.load(legacy);
        h.assertTrue(loaded.getMaxHealth() == 800
                        && Math.abs(loaded.getAttributeValue(Attributes.MOVEMENT_SPEED) - .33D) < .000001D,
                "A real legacy save with 550/.27 attributes migrates to 800/.33");
        health(h, loaded, 400, "Legacy half health stays half health rather than healing or retaining 275");
        h.runAtTickTime(3, () -> {
            CompoundTag upgraded = new CompoundTag();
            loaded.saveWithoutId(upgraded);
            loaded.discard();
            GiantCatBoss reloaded = arena.boss(12, 6);
            reloaded.load(upgraded);
            health(h, reloaded, 400, "Reloading an upgraded save must not scale health a second time");
            h.assertTrue(reloaded.getMaxHealth() == 800
                            && Math.abs(reloaded.getAttributeValue(Attributes.MOVEMENT_SPEED) - .33D) < .000001D,
                    "Upgraded attributes survive another real save/load cycle");
            arena.finish();
        });
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_close_rechase", timeoutTicks = 150)
    public static void closeRangeAiRechasesPlayerWithSpecialsIsolated(GameTestHelper h) {
        Arena arena = new Arena(h);
        var previousDifficulty = h.getLevel().getDifficulty();
        arena.cleanups.add(() -> h.getLevel().getServer().setDifficulty(previousDifficulty, true));
        h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        GiantCatBoss boss = arena.boss(30, 6);
        ServerPlayer player = arena.player(boss.position().add(2.5, 0, 0));
        BossSoundWatch sounds = new BossSoundWatch(arena, boss);
        // Isolate ordinary retargeting; periodic near specials have their own real-AI test.
        field(boss, "attackCooldown", 1000);
        int[] bites = {0};
        float[] previousHealth = {VICTIM_HEALTH};
        long[] firstBite = {-1};
        for (int tick = 1; tick <= 135; tick++) {
            final int time = tick;
            h.runAtTickTime(tick, () -> {
                System.out.println("GIANT_CLOSE_PURSUIT tick=" + time + ", bites=" + bites[0]
                        + ", hisses=" + sounds.hisses + ", health=" + player.getHealth()
                        + ", " + pursuitState(boss, player));
                h.assertTrue(boss.getPhase() == GiantCatBoss.IDLE && sounds.hisses == 0,
                        "Natural close-range pursuit must stay IDLE without hiss: tick=" + time
                                + ", bites=" + bites[0] + ", phase=" + boss.getPhase()
                                + ", distance=" + boss.distanceTo(player) + "; see GIANT_CLOSE_PURSUIT log");
                float damage = previousHealth[0] - player.getHealth();
                if (damage > .001F) {
                    h.assertTrue(Math.abs(damage - 8) < .001F
                                    && boss.distanceTo(player) < 3.8,
                            "Ordinary AI reaches the player with its head for eight damage");
                    if (++bites[0] == 1) {
                        firstBite[0] = h.getLevel().getGameTime();
                        player.setPos(boss.position().add(-2.5, 0, 0));
                    } else {
                        h.assertTrue(h.getLevel().getGameTime() - firstBite[0] >= 20,
                                "Turning around to pursue cannot bypass the ordinary bite cooldown");
                        arena.finish();
                    }
                }
                previousHealth[0] = player.getHealth();
                player.setDeltaMovement(Vec3.ZERO);
                player.getFoodData().setFoodLevel(17);
                if (time == 65) h.assertTrue(bites[0] >= 1,
                        "Nearby natural player target is chased into a first bite: " + pursuitState(boss, player));
                if (time == 135) h.assertTrue(bites[0] >= 2,
                        "Changing sides triggers another real pursuit and bite: " + pursuitState(boss, player));
            });
        }
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_bite_knockback", timeoutTicks = 25)
    public static void realContactBitePushesOutwardAndHonorsFullResistance(GameTestHelper h) {
        Arena arena = new Arena(h);
        var previousDifficulty = h.getLevel().getDifficulty();
        arena.cleanups.add(() -> h.getLevel().getServer().setDifficulty(previousDifficulty, true));
        h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        GiantCatBoss boss = arena.boss(30, 6);
        boss.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        field(boss, "attackCooldown", 1000);
        boss.setYRot(-90F); boss.yBodyRot = -90F; boss.yHeadRot = -90F;
        ServerPlayer pushed = arena.player(boss.position().add(2.6, 0, 0));
        ServerPlayer resistant = arena.player(boss.position().add(2.6, 0, 0));
        pushed.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);
        ServerPlayer[] players = {pushed, resistant};
        boolean[] checked = {false, false};
        // Separate teams disable incidental entity shoving, not damage knockback.
        // Neither player shares the boss's team, so validVictim remains genuine.
        for (ServerPlayer player : players) {
            var scoreboard = h.getLevel().getScoreboard();
            var team = scoreboard.addPlayerTeam("gcb" + UUID.randomUUID().toString().substring(0, 10));
            team.setCollisionRule(net.minecraft.world.scores.Team.CollisionRule.NEVER);
            scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
            arena.cleanups.add(() -> scoreboard.removePlayerTeam(team));
            player.setDeltaMovement(Vec3.ZERO);
        }
        boss.setTarget(pushed);
        for (int tick = 1; tick <= 12; tick++) {
            final int time = tick;
            h.runAtTickTime(tick, () -> {
                for (int index = 0; index < players.length; index++) {
                    ServerPlayer player = players[index];
                    if (!checked[index] && player.getHealth() < VICTIM_HEALTH) {
                        health(h, player, VICTIM_HEALTH - 8, "Knockback is observed on a real successful contact bite");
                        Vec3 velocity = player.getDeltaMovement();
                        double horizontal = Math.sqrt(velocity.horizontalDistanceSqr());
                        if (index == 0) {
                            h.assertTrue(velocity.x > .6D && horizontal >= .65D && horizontal <= 1.0D,
                                    "Ordinary bite delivers visible outward knockback: " + velocity);
                            h.assertTrue(Math.abs(velocity.z) < .08D && velocity.y <= .45D,
                                    "The bite does not launch the player sideways or excessively upward: " + velocity);
                        } else h.assertTrue(horizontal < .001D,
                                "Full knockback resistance blocks actual bite impulse: " + velocity);
                        checked[index] = true;
                    }
                }
                if (checked[0] && checked[1]) arena.finish();
                if (time == 12) h.assertTrue(checked[0] && checked[1],
                        "Both vulnerable real player fixtures receive ordinary contact damage");
            });
        }
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_close_wall_safety", timeoutTicks = 55)
    public static void closeOrdinaryPursuitCannotCrossAFullBodyWall(GameTestHelper h) {
        Arena arena = new Arena(h);
        GiantCatBoss boss = arena.boss(24, 6);
        ServerPlayer player = arena.player(arena.at(27, 1, 6));
        field(boss, "attackCooldown", 1000);
        boss.setTarget(player);
        for (int z = 0; z < 14; z++) for (int y = 1; y <= 5; y++)
            h.setBlock(new BlockPos(26, y, z), Blocks.STONE);
        BlockPos wallMin = h.absolutePos(new BlockPos(26, 1, 0));
        AABB wall = new AABB(wallMin.getX(), wallMin.getY(), wallMin.getZ(),
                wallMin.getX() + 1, wallMin.getY() + 5, wallMin.getZ() + 14);
        for (int tick = 1; tick <= 40; tick++) {
            final int time = tick;
            h.runAtTickTime(tick, () -> {
                h.assertTrue(!boss.getBoundingBox().deflate(.001D).intersects(wall),
                        "Actual ordinary pursuit never moves its large body through the intervening wall");
                h.assertTrue(boss.getPhase() == GiantCatBoss.IDLE,
                        "Wall fixture isolates normal movement rather than a special attack");
                // A legitimate fallback route around the wall is allowed. Only
                // opposite-side contact through the solid wall must remain impossible.
                if (boss.getX() < wall.minX && boss.getZ() >= wall.minZ && boss.getZ() <= wall.maxZ)
                    health(h, player, VICTIM_HEALTH, "Solid wall prevents a through-wall ordinary bite");
                player.setDeltaMovement(Vec3.ZERO);
                if (time == 40) arena.finish();
            });
        }
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_close_drop_safety", timeoutTicks = 60)
    public static void closePlayerOverADeepGapDoesNotLureDirectPursuitOffSupport(GameTestHelper h) {
        Arena arena = new Arena(h);
        GiantCatBoss boss = arena.boss(45, 6);
        // A stationary real player just over the edge keeps the target in the
        // close-range branch. NoGravity belongs only to the player fixture.
        ServerPlayer player = arena.player(boss.position().add(2.5, 0, 0));
        for (int x = 47; x <= 54; x++) for (int z = 0; z < 14; z++)
            for (int y = -5; y <= 0; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        field(boss, "attackCooldown", 1000);
        boss.setTarget(player);
        double floorY = boss.getY();
        for (int tick = 1; tick <= 45; tick++) {
            final int time = tick;
            h.runAtTickTime(tick, () -> {
                h.assertTrue(boss.getPhase() == GiantCatBoss.IDLE,
                        "Deep-gap fixture exercises normal movement, not jumping/rolling");
                // Safe edge approach, stopping, jumping in place or a normal
                // navigation detour are all allowed; falling into this deep gap is not.
                h.assertTrue(boss.getY() >= floorY - .1D,
                        "Close direct pursuit must not walk the real body into an unsupported deep gap: "
                                + pursuitState(boss, player));
                player.setDeltaMovement(Vec3.ZERO);
                if (time == 45) arena.finish();
            });
        }
    }

private static net.minecraft.world.entity.animal.Cat smallCat(Arena arena, Vec3 at, boolean baby) {
        var cat = EntityType.CAT.create(arena.h.getLevel());
        cat.setNoAi(true); cat.setNoGravity(true); cat.setPos(at);
        arena.h.getLevel().addFreshEntity(cat); arena.entities.add(cat);
        cn.laowu.mod.genetics.CatTraitData.set(cat, cn.laowu.mod.genetics.CatTraitProfile.EMPTY);
        cn.laowu.mod.genetics.CatAttributeData.set(cat,
                cn.laowu.mod.genetics.CatAttributeData.ensure(cat).withValues(cn.laowu.mod.genetics.CatStat.SPEED, 0, 100));
        cn.laowu.mod.genetics.CatAttributeEffects.refresh(cat);
        cat.setAge(baby ? -24000 : 0);
        cat.getAttribute(Attributes.MAX_HEALTH).setBaseValue(VICTIM_HEALTH);
        cat.getAttribute(Attributes.ARMOR).removeModifiers();
        cat.getAttribute(Attributes.ARMOR).setBaseValue(0);
        cat.getAttribute(Attributes.ARMOR_TOUGHNESS).removeModifiers();
        cat.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);
        cat.getAttribute(Attributes.KNOCKBACK_RESISTANCE).removeModifiers();
        cat.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        cat.setHealth(VICTIM_HEALTH); return cat;
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_short_bite", timeoutTicks = 20)
    public static void actualAdultCatAndKittenTakeForwardBitesOutsideBodyBox(GameTestHelper h) {
        Arena arena = new Arena(h);
        var bosses = new GiantCatBoss[]{arena.boss(20, 6), arena.boss(40, 6)};
        var cats = new net.minecraft.world.entity.animal.Cat[2];
        for (int i = 0; i < 2; i++) {
            var boss = bosses[i];
            boss.setYRot(-90F); boss.yBodyRot = -90F; boss.yHeadRot = -90F;
            boss.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
            field(boss, "attackCooldown", 1000);
            cats[i] = smallCat(arena, boss.position().add(2.6, 0, 0), i == 1);
            boss.setTarget(cats[i]);
            h.assertTrue(cats[i].getBbHeight() <= 1, "Real cats remain below the old head-box minimum");
            h.assertTrue(!boss.getBoundingBox().intersects(cats[i].getBoundingBox()), "Low target lies outside torso");
        }
        h.runAtTickTime(5, () -> {
            for (var cat : cats) health(h, cat, VICTIM_HEALTH - 8, "Low cat receives ordinary eight-damage bite");
            arena.finish();
        });
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_near_specials", timeoutTicks = 280)
    public static void closeCatGetsAlternatingSpecialsWithANormalAttackWindow(GameTestHelper h) {
        Arena arena = new Arena(h);
        var boss = arena.boss(30, 6);
        boss.setYRot(-90F); boss.yBodyRot = -90F; boss.yHeadRot = -90F;
        var cat = smallCat(arena, boss.position().add(2.5, 0, 0), false);
        boss.setTarget(cat);
        // Bound real roll movement with a wall; retain collision/recovery and real cooldown.
        for (int z = 0; z < 14; z++) for (int y = 1; y < 7; y++) h.setBlock(new BlockPos(34, y, z), Blocks.STONE);
        boolean[] roll = {false}, jump = {false};
        long[] recovered = {-1}; byte[] previous = {GiantCatBoss.IDLE};
        for (int tick = 1; tick <= 260; tick++) {
            final int time = tick;
            h.runAtTickTime(tick, () -> {
                byte phase = boss.getPhase();
                if (phase == GiantCatBoss.IDLE) {
                    cat.setPos(boss.position().add(boss.getX() > arena.at(31,1,6).x ? -2.5 : 2.5, 0, 0));
                    if (previous[0] == GiantCatBoss.ROLL_RECOVERY) recovered[0] = h.getLevel().getGameTime();
                }
                if (phase == GiantCatBoss.ROLL_WINDUP && previous[0] != phase) {
                    h.assertTrue(boss.distanceToSqr(cat) <= 3.2 * 3.2, "Close low target can trigger roll");
                    roll[0] = true;
                }
                if (phase == GiantCatBoss.JUMP_WINDUP && previous[0] != phase) {
                    h.assertTrue(roll[0] && recovered[0] >= 0 && h.getLevel().getGameTime() - recovered[0] >= 80,
                            "Specials alternate with at least four seconds of normal combat after recovery");
                    h.assertTrue(boss.distanceToSqr(cat) <= 3.2 * 3.2, "Jump also starts at close range");
                    jump[0] = true; arena.finish();
                }
                previous[0] = phase;
                if (time == 80) h.assertTrue(roll[0], "Close pursuit cannot suppress specials forever");
                if (time == 260) h.assertTrue(jump[0], "Close combat eventually alternates to jump");
            });
        }
    }

    @GameTest(template = "artillery_probe", batch = "giant_boss_training_loot", timeoutTicks = 30)
    public static void realDeathsDropOneToThreeAdvancedTrainingItemsAlongsideIndependentTrophy(GameTestHelper h) {
        Arena arena = new Arena(h);
        var rules = h.getLevel().getGameRules();
        boolean oldLoot = rules.getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT);
        rules.getRule(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT).set(true, h.getLevel().getServer());
        arena.cleanups.add(() -> rules.getRule(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT).set(oldLoot, h.getLevel().getServer()));
        Set<String> training = Set.of("super_attack_cat_can", "super_health_cat_can", "super_speed_cat_can",
                "super_stamina_cat_can", "super_intelligence_cat_can", "super_luck_cat_can", "super_dried_fish");
        h.runAtTickTime(2, () -> {
            for (int seed = 0; seed < 12; seed++) {
                var boss = arena.boss(30, 6); boss.setNoAi(true); boss.getRandom().setSeed(700 + seed);
                AABB box = boss.getBoundingBox().inflate(4);
                h.assertTrue(boss.hurt(boss.damageSources().genericKill(), Float.MAX_VALUE), "Real death succeeds");
                int rewards = 0, trophies = 0;
                for (var drop : h.getLevel().getEntitiesOfClass(ItemEntity.class, box)) {
                    var itemId = BuiltInRegistries.ITEM.getKey(drop.getItem().getItem());
                    if (itemId.getNamespace().equals("laowu") && training.contains(itemId.getPath())) rewards += drop.getItem().getCount();
                    else if (itemId.equals(id("cat_giant_collar"))) trophies += drop.getItem().getCount();
                    else h.assertTrue(false, "Unexpected giant boss reward " + itemId);
                    drop.discard();
                }
                h.assertTrue(rewards >= 1 && rewards <= 3, "Death drops one to three super rewards: " + rewards);
                h.assertTrue(trophies <= 1, "Training rewards do not duplicate optional collar");
            }
            arena.finish();
        });
    }

    private static String pursuitState(GiantCatBoss boss, ServerPlayer player) {
        var path = boss.getNavigation().getPath();
        return "phase=" + boss.getPhase() + ", phaseTicks=" + boss.getPhaseTicks(0)
                + ", boss=" + boss.position() + ", player=" + player.position()
                + ", distance=" + boss.distanceTo(player) + ", navigationDone=" + boss.getNavigation().isDone()
                + ", pathEnd=" + (path == null ? null : path.getEndNode()) + ", grounded=" + boss.onGround()
                + ", pathProgress=" + (path == null ? "null" : path.getNextNodeIndex() + "/" + path.getNodeCount())
                + ", nextEntityPos=" + (path == null || path.isDone() ? null : path.getNextEntityPos(boss))
                + ", wanted=" + new Vec3(boss.getMoveControl().getWantedX(),
                        boss.getMoveControl().getWantedY(), boss.getMoveControl().getWantedZ())
                + ", speed=" + boss.getSpeed() + ", speedAttribute=" + boss.getAttributeValue(Attributes.MOVEMENT_SPEED)
                + ", velocity=" + boss.getDeltaMovement() + ", bodyYaw=" + boss.getYRot()
                + ", target=" + (boss.getTarget() == null ? null : boss.getTarget().getUUID())
                + ", expectedPlayer=" + player.getUUID();
    }

    private static void customLoot(GiantCatBoss boss, GameTestHelper h, DamageSource source, int looting) {
        try {
            Method method = GiantCatBoss.class.getDeclaredMethod("dropCustomDeathLoot",
                    DamageSource.class, int.class, boolean.class);
            method.setAccessible(true);
            method.invoke(boss, source, looting, true);
        } catch (ReflectiveOperationException error) { throw new AssertionError("Unable to exercise real custom loot", error); }
    }
}
