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
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;

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
public final class MaodieCombatProbe {
    private static final float VICTIM_HEALTH = 1000;
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("laowu", path); }

    private static final class Arena {
        final GameTestHelper h;
        final List<Entity> entities = new ArrayList<>();
        final Set<ChunkPos> forced = new HashSet<>();
        final List<Runnable> cleanups = new ArrayList<>();
        boolean closed;
        Arena(GameTestHelper h) {
            this.h = h;
            var difficulty=h.getLevel().getDifficulty();
            h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
            cleanups.add(()->h.getLevel().getServer().setDifficulty(difficulty,true));
            // Expected RED assertions must not leak players, listeners, forced chunks
            // or temporary difficulty into the next independent GameTest batch.
            try {
                Field info = GameTestHelper.class.getDeclaredField("testInfo");
                info.setAccessible(true);
                ((GameTestInfo) info.get(h)).addListener(new GameTestListener() {
                    public void testStructureLoaded(GameTestInfo info) {}
                    public void testPassed(GameTestInfo info,GameTestRunner runner) { cleanup(); }
                    public void testFailed(GameTestInfo info,GameTestRunner runner) { cleanup(); }
                    public void testAddedForRerun(GameTestInfo oldInfo,GameTestInfo newInfo,GameTestRunner runner) { cleanup(); }
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
            ServerPlayer player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), new GameProfile(UUID.randomUUID(), "giant-contact-player"), net.minecraft.server.level.ClientInformation.createDefault());
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
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(this);
            arena.cleanups.add(() -> net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(this));
        }
        @net.neoforged.bus.api.SubscribeEvent
        public void sound(net.neoforged.neoforge.event.PlayLevelSoundEvent event) {
            if (event.getLevel() != boss.level() || event.getSound() == null
                    || event.getSound().value() != net.minecraft.sounds.SoundEvents.CAT_HISS) return;
            if (event instanceof net.neoforged.neoforge.event.PlayLevelSoundEvent.AtEntity at && at.getEntity() == boss) hisses++;
            else if (event instanceof net.neoforged.neoforge.event.PlayLevelSoundEvent.AtPosition at
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


    private static GiantCatBoss maodie(Arena a) {
        GiantCatBoss b=a.boss(12,6);
        b.configureMaodie(new CompoundTag(), a.h.absolutePos(new BlockPos(12,1,6)));
        field(b,"attackCooldown",10000); b.setYRot(0); b.yBodyRot=0;
        return b;
    }
    private static Object method(Object o,String name,Class<?>[] types,Object... args) {
        try { Method m=o instanceof Class<?> c?c.getDeclaredMethod(name,types):o.getClass().getDeclaredMethod(name,types);
            m.setAccessible(true);return m.invoke(o instanceof Class<?>?null:o,args);
        } catch(ReflectiveOperationException e){throw new AssertionError("Missing/failed combat "+name,e);}
    }
    private static boolean enraged(GiantCatBoss b) { return (boolean)method(b,"isMaodieEnraged",new Class<?>[0]); }
    private static void slam(GiantCatBoss b){method(b,"performSlam",new Class<?>[0]);}
    private static void roll(GiantCatBoss b){method(b,"performRollImpact",new Class<?>[]{AABB.class,Vec3.class},b.getBoundingBox(),new Vec3(0,0,2));}

    // Break caught: wrong cone axis, angle, range or vertical reach.
    @GameTest(template="artillery_probe",batch="maodie_geometry")
    public static void fixedConeBoundaries(GameTestHelper h) {
        try{
            Class<?> c=Class.forName("cn.laowu.mod.entity.MaodieCombat");
            Class<?>[] types={Vec3.class,float.class,Vec3.class};
            Vec3 origin=Vec3.ZERO;
            h.assertTrue((boolean)method(c,"inHissCone",types,origin,0F,new Vec3(0,0,8)),"Eight blocks forward is inside");
            h.assertTrue(!(boolean)method(c,"inHissCone",types,origin,0F,new Vec3(0,0,8.01)),"Over eight blocks is outside");
            h.assertTrue((boolean)method(c,"inHissCone",types,origin,0F,new Vec3(4,0,6)),"33.69 degrees inside");
            h.assertTrue(!(boolean)method(c,"inHissCone",types,origin,0F,new Vec3(4.3,0,6)),"35.63 degrees outside");
            h.assertTrue(!(boolean)method(c,"inHissCone",types,origin,0F,new Vec3(0,0,-1)),"Rear excluded");
            h.assertTrue(!(boolean)method(c,"inHissCone",types,origin,0F,new Vec3(0,4,3)),"Vertical reach excluded");
            h.assertTrue((boolean)method(c,"inHissCone",types,origin,90F,new Vec3(-7,0,0)),"Yaw ninety aims west");
            h.succeed();
        }catch(ClassNotFoundException e){throw new AssertionError("Maodie cone not implemented",e);}
    }
    // Break caught: variant still uses legacy damage, or legacy damage gets overwritten.
    @GameTest(template="artillery_probe",batch="maodie_damage")
    public static void variantAndLegacyDamage(GameTestHelper h) {
        Arena a=new Arena(h);GiantCatBoss b=maodie(a), old=a.boss(44,6);
        b.setNoGravity(true);old.setNoGravity(true);
        phase(b,GiantCatBoss.SUMMON);phase(old,GiantCatBoss.SUMMON);
        ServerPlayer bite=a.player(b.position().add(0,0,3)), rolled=a.player(b.position().add(0,0,1.5)), slammed=a.player(b.position().add(3,0,0));
        ServerPlayer oldBite=a.player(old.position().add(0,0,3)),oldRoll=a.player(old.position().add(0,0,1.5)),oldSlam=a.player(old.position().add(3,0,0));
        h.runAtTickTime(1,()->{
            phase(b,GiantCatBoss.IDLE);phase(old,GiantCatBoss.IDLE);
            b.setYRot(0);b.yBodyRot=0;old.setYRot(0);old.yBodyRot=0;
            bite.invulnerableTime=oldBite.invulnerableTime=0;bite.setHealth(1000);oldBite.setHealth(1000);
            h.assertTrue(b.doHurtTarget(bite),"Real variant bite accepted");
            h.assertTrue(old.doHurtTarget(oldBite),"Real legacy bite accepted");
            health(h,bite,994,"Variant bite six");health(h,oldBite,992,"Legacy bite eight");
            rolled.invulnerableTime=oldRoll.invulnerableTime=0;rolled.setHealth(1000);oldRoll.setHealth(1000);
            phase(b,GiantCatBoss.ROLL);phase(old,GiantCatBoss.ROLL);roll(b);roll(old);
            health(h,rolled,992,"Variant roll eight");health(h,oldRoll,988,"Legacy roll twelve");
            slammed.invulnerableTime=oldSlam.invulnerableTime=0;slammed.setHealth(1000);oldSlam.setHealth(1000);
            phase(b,GiantCatBoss.JUMP);phase(old,GiantCatBoss.JUMP);slam(b);slam(old);
            health(h,slammed,988,"Variant slam twelve");health(h,oldSlam,982,"Legacy slam eighteen");a.finish();
        });
    }
    // Break caught: windup damages early, homing aim, missing range/side check, repeated damage.
    @GameTest(template="artillery_probe",batch="maodie_hiss",timeoutTicks=100)
    public static void hissTelegraphConeAndOneHit(GameTestHelper h) {
        Arena a=new Arena(h);GiantCatBoss b=maodie(a);
        ServerPlayer front=a.player(b.position().add(0,0,7)), far=a.player(b.position().add(0,0,9)),
            side=a.player(b.position().add(5,0,1)),rear=a.player(b.position().add(0,0,-5));
        b.setTarget(front);
        h.runAtTickTime(1,()->phase(b,(byte)9));
        h.runAtTickTime(19,()->{health(h,front,1000,"Nineteen tick anticipation never hurts");h.assertTrue(b.getPhase()==9,"Still inhaling");});
        h.runAtTickTime(23,()->{health(h,front,996,"Front is hit once for four");health(h,far,1000,"Nine blocks misses");health(h,side,1000,"Side misses");health(h,rear,1000,"Rear misses");
            front.invulnerableTime=0;b.setTarget(rear);front.setPos(b.position().add(0,0,7));});
        h.runAtTickTime(37,()->{health(h,front,996,"Cannot rehit even after vanilla invulnerability cleared");health(h,rear,1000,"Does not swivel with changed target");});
        h.runAtTickTime(44,()->h.assertTrue(b.getPhase()==11,"Attack has a recovery"));
        h.runAtTickTime(59,()->h.assertTrue(b.getPhase()==11,"One second recovery preserved"));
        h.runAtTickTime(64,()->{h.assertTrue(b.getPhase()==GiantCatBoss.IDLE,"Returns to neutral");a.finish();});
    }
    @GameTest(template="artillery_probe",batch="maodie_hiss_wall",timeoutTicks=80)
    public static void hissCannotCrossWall(GameTestHelper h) {
        Arena a=new Arena(h);GiantCatBoss b=maodie(a);ServerPlayer front=a.player(b.position().add(0,0,7));b.setTarget(front);
        h.runAtTickTime(1,()->{phase(b,(byte)9);
            for(int x=10;x<=14;x++)for(int y=1;y<=5;y++)h.setBlock(new BlockPos(x,y,10),Blocks.STONE);});
        h.runAtTickTime(45,()->{health(h,front,1000,"Wall blocks every damage tick");a.finish();});
    }
    @GameTest(template="artillery_probe",batch="maodie_pet",timeoutTicks=80)
    public static void noPetOrWildcatDamage(GameTestHelper h) {
        Arena a=new Arena(h);GiantCatBoss b=maodie(a);ServerPlayer front=a.player(b.position().add(0,0,7));b.setTarget(front);
        var wild=EntityType.CAT.create(h.getLevel());var pet=EntityType.CAT.create(h.getLevel());
        for(var c:List.of(wild,pet)){c.setNoAi(true);c.setPos(b.position().add(0,0,3));h.getLevel().addFreshEntity(c);a.entities.add(c);}
        pet.setTame(true,true);
        h.runAtTickTime(1,()->phase(b,(byte)9));
        h.runAtTickTime(45,()->{health(h,wild,wild.getMaxHealth(),"Wildcat excluded");health(h,pet,pet.getMaxHealth(),"Pet excluded");
            phase(b,GiantCatBoss.ROLL);roll(b);phase(b,GiantCatBoss.JUMP);slam(b);
            health(h,wild,wild.getMaxHealth(),"All specials exclude wildcat");health(h,pet,pet.getMaxHealth(),"All specials exclude pet");a.finish();});
    }
    // Break caught: threshold off by one, repeated transition feedback, reload replay.
    @GameTest(template="artillery_probe",batch="maodie_enrage",timeoutTicks=80)
    public static void halfHealthEnragesOnceAndPersists(GameTestHelper h) {
        Arena a=new Arena(h);GiantCatBoss b=maodie(a);BossSoundWatch sound=new BossSoundWatch(a,b);
        a.player(b.position().add(0,0,15));b.setHealth(151);
        h.runAtTickTime(2,()->{h.assertTrue(!enraged(b),"Above half remains normal");b.setHealth(150);});
        h.runAtTickTime(4,()->{h.assertTrue(enraged(b),"Half health enters enrage");h.assertTrue(sound.hisses==1,"Exactly one growl feedback");b.setHealth(200);});
        h.runAtTickTime(6,()->b.setHealth(140));
        h.runAtTickTime(8,()->{
            h.assertTrue(sound.hisses==1,"Healing and reducing cannot repeat feedback");
            CompoundTag saved=new CompoundTag();b.saveWithoutId(saved);GiantCatBoss copy=a.boss(50,6);copy.load(saved);copy.setUUID(UUID.randomUUID());copy.setPos(a.at(50,1,6));
            h.assertTrue(enraged(copy),"Enrage survives actual entity load");
            phase(b,(byte)11);field(b,"attackCooldown",0);
        });
        h.runAtTickTime(30,()->{
            try{Field f=GiantCatBoss.class.getDeclaredField("attackCooldown");f.setAccessible(true);int n=f.getInt(b);
                h.assertTrue(n>=38&&n<=60,"Enraged cooldown bounded forty to sixty after recovery: "+n);
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}a.finish();
        });
    }
    @GameTest(template="artillery_probe",batch="maodie_noai",timeoutTicks=100)
    public static void noAiStopsEveryAttack(GameTestHelper h) {
        Arena a=new Arena(h);GiantCatBoss b=maodie(a);ServerPlayer p=a.player(b.position().add(0,0,1));b.setTarget(p);
        byte[] phases={3,4,6,7,9,10,11};
        for(int i=0;i<phases.length;i++){int n=i;h.runAtTickTime(1+i*4,()->{phase(b,phases[n]);b.setNoAi(true);});
            h.runAtTickTime(3+i*4,()->{health(h,p,1000,"NoAI cancels phase "+phases[n]);h.assertTrue(b.getPhase()==1,"NoAI returns neutral");});}
        h.runAtTickTime(32,()->{b.setNoAi(false);phase(b,(byte)10);b.markRestorationPending();});
        h.runAtTickTime(36,()->{health(h,p,1000,"Pending restore cannot attack");a.finish();});
    }
    @GameTest(template="artillery_probe",batch="maodie_live_cycle",timeoutTicks=750)
    public static void liveAiCyclesAllThreeWithRecovery(GameTestHelper h) {
        Arena a=new Arena(h);GiantCatBoss b=maodie(a);ServerPlayer p=a.player(a.at(22,1,6));b.setTarget(p);field(b,"attackCooldown",0);
        Set<Byte> seen=new HashSet<>();long[] recoveryStart={-1};byte[] previous={1};
        for(int i=1;i<=690;i++)h.runAtTickTime(i,()->{
            p.setPos(a.at(22,1,6));p.invulnerableTime=0;byte ph=b.getPhase();seen.add(ph);
            boolean recovering=ph==5||ph==8||ph==11;
            if(recovering&&previous[0]!=ph)recoveryStart[0]=h.getLevel().getGameTime();
            if(!recovering&&(previous[0]==5||previous[0]==8||previous[0]==11))
                h.assertTrue(h.getLevel().getGameTime()-recoveryStart[0]>=20,"Every move leaves one second recovery");
            previous[0]=ph;
        });
        h.runAtTickTime(700,()->{CompoundTag tag=new CompoundTag();b.saveWithoutId(tag);h.assertTrue(seen.contains((byte)4)&&seen.contains((byte)7)&&seen.contains((byte)10),"Live AI uses roll, jump and hiss: "+seen+" pos="+b.position()+" ground="+b.onGround()+" phase="+b.getPhase()+" next="+tag.getInt("MaodieNextSpecial")+" cooldown="+tag.getInt("GiantCatAttackCooldown")+" alive="+b.isAlive());a.finish();});
    }
}
