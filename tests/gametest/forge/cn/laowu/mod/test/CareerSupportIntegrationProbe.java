package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.entity.EngineeringCogwheelProjectile;
import cn.laowu.mod.genetics.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CareerSupportIntegrationProbe {
    private static final UUID OWNER = UUID.fromString("334937c5-3590-4e1c-bf76-159335a34261");

    static Vec3 floor(GameTestHelper helper) {
        // In a headless GameTestServer, loading a structure is not a player simulation ticket.
        // Keep all fixture entities ticking, regardless of where this batch crosses a chunk edge.
        // Never force extra chunks when these tests are invoked in a player's ordinary world.
        if(helper.getLevel().getServer() instanceof net.minecraft.gametest.framework.GameTestServer){
            var first=helper.absolutePos(BlockPos.ZERO);
            var last=helper.absolutePos(new BlockPos(71,11,13));
            for(int x=Math.floorDiv(first.getX(),16);x<=Math.floorDiv(last.getX(),16);x++)
                for(int z=Math.floorDiv(first.getZ(),16);z<=Math.floorDiv(last.getZ(),16);z++)
                    helper.getLevel().setChunkForced(x,z,true);
        }
        // The test world is reused by Gradle: remove only this fixture's saved entities.
        var bounds = new net.minecraft.world.phys.AABB(Vec3.atLowerCornerOf(helper.absolutePos(BlockPos.ZERO)),
                Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(71, 11, 13))));
        for (var entity : helper.getLevel().getEntities((net.minecraft.world.entity.Entity) null, bounds,
                entity -> !(entity instanceof net.minecraft.server.level.ServerPlayer))) entity.discard();
        for (int x = 0; x < 18; x++) for (int z = 0; z < 12; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            for (int y = 1; y < 8; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
        return Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
    }
    static Cat cat(ServerLevel level, Vec3 at, CatOutfitType outfit, boolean ai) {
        // Vanilla cats sit when their owner is offline. Stub only owner lookup,
        // retaining the real goal selectors and all support eligibility checks.
        var owner = net.minecraftforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "support-probe"));
        owner.moveTo(at.x, at.y, at.z, 0, 0);
        Cat cat = cat(level, at, outfit, ai, owner);
        cat.setOwnerUUID(OWNER);
        return cat;
    }
    static Cat cat(ServerLevel level, Vec3 at, CatOutfitType outfit, boolean ai, LivingEntity owner) {
        Cat cat = new Cat(EntityType.CAT, level) {
            @Override public LivingEntity getOwner() { return owner; }
        };
        cat.setTame(true);
        cat.setOwnerUUID(owner.getUUID());
        cat.setNoAi(!ai);
        cat.setPos(at);
        cat.setNoGravity(!ai);
        cat.setAge(0);
        level.addFreshEntity(cat);
        // Joining can randomly assign LOLI before the fixture clears traits; explicitly restore adult age.
        CatTraitData.set(cat, CatTraitProfile.EMPTY);
        cat.setAge(0);
        var genes = CatAttributeData.ensure(cat);
        for (CatStat stat : CatStat.values()) genes = genes.withValues(stat, 50, 100);
        CatAttributeData.set(cat, genes);
        CatPoseData.setPose(cat, CatPoseData.NORMAL);
        CatClothesData.equip(cat, outfit);
        CareerCatBehavior.tick(cat);
        return cat;
    }
    static void stat(Cat cat, CatStat stat, int value) {
        CatAttributeData.set(cat, CatAttributeData.ensure(cat).withValues(stat, value, 100));
        CatAttributeEffects.refresh(cat);
        CareerCatBehavior.tick(cat);
    }
    private static LivingEntity victim(ServerLevel level, Vec3 at) {
        var entity = EntityType.IRON_GOLEM.create(level);
        entity.setPos(at);
        entity.setNoAi(true);
        entity.setNoGravity(true);
        entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        entity.getAttribute(Attributes.ARMOR).setBaseValue(0);
        entity.setHealth(1000);
        level.addFreshEntity(entity);
        return entity;
    }
    private static EngineeringCogwheelProjectile shot(ServerLevel level, Cat owner, Vec3 start,
                                                     CatArtilleryMunition type, double length) {
        var shot = new EngineeringCogwheelProjectile(level, owner, 10);
        shot.setMunition(type);
        shot.setPos(start);
        shot.setDeltaMovement(length, 0, 0);
        level.addFreshEntity(shot);
        return shot;
    }

    @GameTest(template = "artillery_probe", batch = "support", setupTicks = 5, timeoutTicks = 60)
    public static void munitionSweeps(GameTestHelper helper) {
        var level = helper.getLevel();
        Vec3 base = floor(helper);
        Cat owner = cat(level, base, CatOutfitType.ENGINEERING, false);
        Vec3 lane = base.add(0, 0.8, 2);
        LivingEntity a = victim(level, base.add(3, 0, 2));
        LivingEntity b = victim(level, base.add(5, 0, 2));
        LivingEntity c = victim(level, base.add(7, 0, 2));
        Cat friendly = cat(level, base.add(4, 0, 2), CatOutfitType.FIRE, false);
        float allyHealth = friendly.getHealth();
        EngineeringCogwheelProjectile small = shot(level, owner, lane, CatArtilleryMunition.SMALL_COG, 8);
        small.tick();
        helper.assertTrue(a.getHealth() < 1000 && b.getHealth() == 1000 && c.getHealth() == 1000 && small.isRemoved(),
                "Small cog stops at its first victim: hp="+a.getHealth()+","+b.getHealth()+","+c.getHealth()
                        +", removed="+small.isRemoved()+", visible="+level.getEntitiesOfClass(LivingEntity.class, small.getBoundingBox().inflate(10)).size());

        for (LivingEntity target : List.of(a,b,c)) { target.setHealth(1000); target.invulnerableTime = 0; }
        EngineeringCogwheelProjectile shaft = shot(level, owner, lane, CatArtilleryMunition.SHAFT, 8);
        shaft.tick();
        helper.assertTrue(a.getHealth() < 1000 && b.getHealth() < 1000 && c.getHealth() < 1000 && !shaft.isRemoved(),
                "One shaft crosses all three entities in a single tick");
        helper.assertTrue(friendly.getHealth() == allyHealth, "Piercing ignores friendly cats");
        float before = b.getHealth();
        CompoundTag data = shaft.saveWithoutId(new CompoundTag());
        var restored = new EngineeringCogwheelProjectile(LaoWuMod.ENGINEERING_COGWHEEL_PROJECTILE.get(), level);
        restored.load(data);
        restored.setPos(b.getBoundingBox().getCenter());
        restored.setDeltaMovement(0.1, 0, 0);
        b.invulnerableTime = 0;
        level.addFreshEntity(restored);
        restored.tick();
        helper.assertTrue(restored.munition() == CatArtilleryMunition.SHAFT && b.getHealth() == before,
                "Saved munition and UUID hit set prevent repeated damage after reload");
        restored.discard(); shaft.discard();

        Vec3 blockedLane = base.add(0, 0.8, 6);
        LivingEntity front = victim(level, base.add(3, 0, 6));
        LivingEntity behind = victim(level, base.add(7, 0, 6));
        BlockPos wall = BlockPos.containing(base.add(5, 0, 6));
        for (int y = 0; y < 4; y++) level.setBlockAndUpdate(wall.above(y), Blocks.STONE.defaultBlockState());
        var blocked = shot(level, owner, blockedLane, CatArtilleryMunition.SHAFT, 10);
        blocked.tick();
        helper.assertTrue(front.getHealth() < 1000 && behind.getHealth() == 1000 && blocked.isRemoved(),
                "Shaft pierces entities before the wall, never entities behind it");
        helper.succeed();
    }

    @GameTest(template = "artillery_probe", batch = "support", setupTicks = 5, timeoutTicks = 60)
    public static void cogBurst(GameTestHelper helper) {
        var level = helper.getLevel();
        Vec3 base = floor(helper);
        Cat owner = cat(level, base, CatOutfitType.ENGINEERING, false);
        LivingEntity main = victim(level, base.add(4, 0, 4));
        LivingEntity splash = victim(level, base.add(4, 0, 5.4));
        LivingEntity far = victim(level, base.add(9, 0, 4));
        LivingEntity covered = victim(level, base.add(4, 0, 2.4));
        Cat ally = cat(level, base.add(4, 0, 4.8), CatOutfitType.FIRE, false);
        float allyHealth = ally.getHealth();
        BlockPos cover = BlockPos.containing(base.add(4, 0, 3));
        for (int x=-1;x<=1;x++) for(int y=0;y<4;y++)
            level.setBlockAndUpdate(cover.offset(x,y,0), Blocks.STONE.defaultBlockState());
        var large = shot(level, owner, base.add(0, 1, 4), CatArtilleryMunition.LARGE_COG, 8);
        large.tick();
        helper.assertTrue(main.getHealth() < 1000 && splash.getHealth() < 1000 && large.isRemoved(),
                "Large cog damages its direct target and nearby creatures");
        helper.assertTrue(far.getHealth() == 1000 && covered.getHealth() == 1000 && ally.getHealth() == allyHealth,
                "Area damage respects radius, walls and friendly teams");
        helper.assertTrue(level.getBlockState(cover).is(Blocks.STONE), "No explosion terrain changes");
        helper.succeed();
    }

    @GameTest(template = "artillery_probe", batch = "support", setupTicks = 5, timeoutTicks = 110)
    public static void medicalTriage(GameTestHelper helper) {
        var level = helper.getLevel();
        Vec3 base = floor(helper);
        Cat medic = cat(level, base.add(2,0,4), CatOutfitType.MEDICAL, true);
        Cat near = cat(level, base.add(4,0,4), CatOutfitType.FIRE, false);
        Cat critical = cat(level, base.add(8,0,4), CatOutfitType.FIRE, false);
        Cat ordinary = cat(level, base.add(3,0,4), CatOutfitType.NONE, false);
        Cat enemy = cat(level, base.add(3,0,6), CatOutfitType.FIRE, false);
        enemy.setOwnerUUID(UUID.randomUUID());
        CompoundTag enemyData = enemy.saveWithoutId(new CompoundTag());
        enemyData.putByte("CollarColor", (byte) DyeColor.BLUE.getId());
        enemy.load(enemyData);
        near.setHealth(near.getMaxHealth() * .7F);
        critical.setHealth(3); ordinary.setHealth(1); enemy.setHealth(1);
        stat(medic, CatStat.INTELLIGENCE, 0);
        helper.assertTrue(CatMedicalSupportGoal.selectRecipient(medic) == near,
                "Low intelligence chooses nearby patient: selected="+CatMedicalSupportGoal.selectRecipient(medic)
                            +", valid="+CatMedicalSupportGoal.recipient(medic, near)+", expected="+near);
        stat(medic, CatStat.INTELLIGENCE, 100);
        helper.assertTrue(CatMedicalSupportGoal.selectRecipient(medic) == critical, "High intelligence prioritizes lowest health fraction");
        helper.assertTrue(CatAttributeEffects.effectiveValue(medic, CatStat.SPEED) == 60, "Medical suit grants 10 Speed");
        helper.assertTrue(medic.getAttributeValue(Attributes.MOVEMENT_SPEED) > ordinary.getAttributeValue(Attributes.MOVEMENT_SPEED),
                "Support movement advantage is a real attribute modifier");
        helper.assertTrue(!CatTeamRules.canHarm(medic, enemy), "Support cannot choose an attack target");
        float enemyHealth = enemy.getHealth();
        enemy.hurt(level.damageSources().mobAttack(medic), 100);
        helper.assertTrue(enemy.getHealth() == enemyHealth, "Support damage is rejected even through the direct event path");
        stat(ordinary, CatStat.ATTACK, 100);
        helper.assertTrue(Math.abs(ordinary.getAttributeValue(Attributes.ATTACK_DAMAGE)-5) < .001,
                "Unsuited cat damage = 1 + .04 * Combat Power");
        float nearHealth = near.getHealth();
        helper.runAtTickTime(45, () -> {
            helper.assertTrue(critical.getHealth() > 3 && near.getHealth() > nearHealth,
                    "Medical AI approaches critical ally and heals both patients within its circle");
            helper.assertTrue(ordinary.getHealth() == 1 && enemy.getHealth() == 1,
                    "No treatment of ordinary or enemy cats");
            medic.setOrderedToSit(true);
            float beforeEndOfTick = critical.getHealth();
            // Offers already collected during this tick flush at server END. A GameTest
            // callback can run between collection and flush; let that tick settle first.
            helper.runAfterDelay(1, () -> {
                helper.assertTrue(!CatMedicalHealing.casting(medic), "Sitting stops the caster immediately");
                float stopped = critical.getHealth();
                helper.assertTrue(stopped <= beforeEndOfTick + 1.001F,
                        "At most one already-offered 0.25-second healing pulse may finish");
                helper.runAfterDelay(20, () -> {
                    helper.assertTrue(critical.getHealth() == stopped,
                            "Sitting produces no new healing offers after the current tick");
                    helper.succeed();
                });
            });
        });
    }


    @GameTest(template = "artillery_probe", batch = "support", setupTicks = 5, timeoutTicks = 125)
    public static void medicalCircle(GameTestHelper helper) {
        var level = helper.getLevel();
        Vec3 base = floor(helper);
        Cat first = cat(level, base.add(3,0,5), CatOutfitType.MEDICAL, true);
        Cat second = cat(level, base.add(5,0,5), CatOutfitType.MEDICAL, true);
        stat(first, CatStat.INTELLIGENCE, 0); stat(second, CatStat.INTELLIGENCE, 0);
        Cat a = cat(level, base.add(3.5,0,6), CatOutfitType.FIRE, false);
        Cat b = cat(level, base.add(4.5,0,6), CatOutfitType.FLIGHT, false);
        Cat outside = cat(level, base.add(12,0,5), CatOutfitType.FIRE, false);
        Cat covered = cat(level, base.add(4,0,2), CatOutfitType.FIRE, false);
        for (int x=1;x<=7;x++) for (int y=0;y<4;y++)
            level.setBlockAndUpdate(BlockPos.containing(base.add(x,y,3)), Blocks.STONE.defaultBlockState());
        for (Cat patient : List.of(a,b,outside,covered)) patient.setHealth(10);
        // Freeze neither recipients nor their goals: an unrelated custom movement goal keeps ticking.
        Cat active = cat(level, base.add(4,0,7), CatOutfitType.FIRE, true);
        active.goalSelector.removeAllGoals(goal -> true);
        active.targetSelector.removeAllGoals(goal -> true);
        var actions = new java.util.concurrent.atomic.AtomicInteger();
        active.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.Goal() {
            { setFlags(java.util.EnumSet.of(Flag.MOVE)); }
            @Override public boolean canUse() { return true; }
            @Override public boolean requiresUpdateEveryTick() { return true; }
            @Override public void tick() { actions.incrementAndGet(); }
        });
        active.setHealth(10);
        java.util.List<Cat> all = List.of(first,second,a,b,outside,covered,active);
        helper.runAtTickTime(16, () -> {
            helper.assertTrue(CatMedicalHealing.casting(first) && CatMedicalHealing.casting(second),
                    "Both nearby medics deploy their healing circles");
            helper.assertTrue(a.getHealth()==10 && b.getHealth()==10, "No immediate burst during channel warmup");
        });
        helper.runAtTickTime(65, () -> {
            helper.assertTrue(a.getHealth()>10 && b.getHealth()>10 && active.getHealth()>10,
                    "All eligible in-circle patients receive healing");
            helper.assertTrue(a.getHealth()<=13 && b.getHealth()<=13 && active.getHealth()<=13,
                    "Two overlapping medics cannot exceed 1 HP/second per patient: "+a.getHealth()+","+b.getHealth());
            helper.assertTrue(outside.getHealth()==10 && covered.getHealth()==10,
                    "Patients outside radius or behind walls receive no healing");
            helper.assertTrue(first.getDeltaMovement().horizontalDistanceSqr()<1e-7
                            && second.getDeltaMovement().horizontalDistanceSqr()<1e-7,
                    "Casters stand still while channeling: "+first.getDeltaMovement()+", "+second.getDeltaMovement()
                            +"; casting="+CatMedicalHealing.casting(first)+","+CatMedicalHealing.casting(second)
                            +"; navigation="+first.getNavigation().isDone()+","+second.getNavigation().isDone());
            helper.assertTrue(actions.get()>35 && !active.isNoAi() && !active.isOrderedToSit(),
                    "Recipients retain their own active AI");
            long next=a.getPersistentData().getLong(CatMedicalHealing.NEXT_HEAL);
            float beforeOffer=a.getHealth();
            CatMedicalHealing.offerHealing(a, 1);
            CatMedicalHealing.flush(level);
            helper.assertTrue(a.getHealth() <= beforeOffer + .25F, "Shared recipient gate allows at most one scheduled quarter-second pulse");
            float hp = a.getHealth();
            CompoundTag saved=a.saveWithoutId(new CompoundTag());
            Cat restored=EntityType.CAT.create(level); restored.load(saved);
            helper.assertTrue(restored.getPersistentData().getLong(CatMedicalHealing.NEXT_HEAL)==next,
                    "Non-stacking cooldown survives save/load");
            first.setOrderedToSit(true); second.setOrderedToSit(true);
            helper.runAfterDelay(35, () -> {
                helper.assertTrue(a.getHealth()==hp && !CatMedicalHealing.casting(first) && !CatMedicalHealing.glowing(a),
                        "Stopping the medics stops healing and expires the recipient aura");
                all.forEach(Cat::discard);
                System.out.println("PASS: medical group circle, windup, slow nonstacking heal, LOS, stationary caster, unaffected allies and cleanup");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "artillery_probe", batch = "support", setupTicks = 5, timeoutTicks = 220)
    public static void logisticsPacing(GameTestHelper helper) {
        var level = helper.getLevel();
        Vec3 base = floor(helper);
        Cat logistics = cat(level, base.add(2,0,5), CatOutfitType.TRANSPORT, true);
        stat(logistics, CatStat.INTELLIGENCE, 100);
        var enemy = victim(level, base.add(12,0,5));
        List<Cat> recipients = new ArrayList<>();
        for (int i=0;i<5;i++) {
            Cat ally = cat(level, base.add(4+i,0,3), CatOutfitType.FIRE, true);
            ally.setTarget(enemy);
            recipients.add(ally);
        }
        Map<UUID,Long> last = new HashMap<>();
        List<Long> casts = new ArrayList<>();
        for (int tick=1;tick<=180;tick++) {
            helper.runAtTickTime(tick, () -> {
                for (Cat ally : recipients) {
                    long until = ally.getPersistentData().getLong("LaoWuNextLogisticsReceived");
                    long previous = last.getOrDefault(ally.getUUID(),0L);
                    if (until <= previous) continue;
                    if (previous > 0) helper.assertTrue(until-previous >= 100, "Same target cooldown is at least 5 seconds");
                    long cast = until-100;
                    if (!casts.isEmpty()) helper.assertTrue(cast-casts.get(casts.size()-1) >= 20, "Different targets respect the 1-second cast cooldown");
                    casts.add(cast);
                    last.put(ally.getUUID(),until);
                }
            });
        }
        helper.runAtTickTime(185, () -> {
            helper.assertTrue(last.size() == 5 && casts.size() >= 6,
                    "Logistics actively rotates across five allies and returns after the individual cooldown; casts="+casts.size()
                            +", allies="+last.size()+", canWork="+CatSupportRules.canWork(logistics)
                            +", sitting="+logistics.isInSittingPose()+", targets="+recipients.stream().filter(c -> c.getTarget()!=null).count());
            helper.assertTrue(logistics.getTarget() == null, "Logistics never switches into combat");
            helper.succeed();
        });
    }
}
