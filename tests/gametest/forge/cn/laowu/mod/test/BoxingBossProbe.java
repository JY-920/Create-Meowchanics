package cn.laowu.mod.test;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.entity.MaodieFighter;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.lang.reflect.Method;
import java.util.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class BoxingBossProbe {
    private static final Set<String> TRAINING_REWARDS=Set.of(
            "super_attack_cat_can","super_health_cat_can","super_speed_cat_can",
            "super_stamina_cat_can","super_intelligence_cat_can","super_luck_cat_can","super_dried_fish");

    private static final class Arena {
        final GameTestHelper h;
        final List<Entity> entities=new ArrayList<>();
        final List<Runnable> cleanups=new ArrayList<>();
        final net.minecraft.world.Difficulty prior;
        Arena(GameTestHelper h) {
            this.h=h;prior=h.getLevel().getDifficulty();
            h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
            for(int x=0;x<5;x++)for(int z=0;z<5;z++)
                h.setBlock(new BlockPos(x,0,z),net.minecraft.world.level.block.Blocks.STONE);
            try {
                var field=GameTestHelper.class.getDeclaredField("testInfo");field.setAccessible(true);
                ((GameTestInfo)field.get(h)).addListener(new GameTestListener(){
                    public void testStructureLoaded(GameTestInfo info){}
                    public void testPassed(GameTestInfo info){clean();}
                    public void testFailed(GameTestInfo info){clean();}
                });
            } catch(ReflectiveOperationException e){throw new AssertionError(e);}
        }
        void clean(){
            entities.forEach(Entity::discard);
            drops().forEach(Entity::discard);
            cleanups.forEach(Runnable::run);
            h.getLevel().getServer().setDifficulty(prior,true);
        }
        MaodieFighter fighter(){
            var fighter=LaoWuMod.MAODIE_FIGHTER.get().create(h.getLevel());
            fighter.setNoAi(true);fighter.setNoGravity(true);
            fighter.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(2,2,2))));
            h.getLevel().addFreshEntity(fighter);entities.add(fighter);return fighter;
        }
        ServerPlayer viewer(){
            var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"boxing-viewer"));
            entities.add(player);return player;
        }
        List<ItemEntity> drops(){
            return h.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new net.minecraft.world.phys.AABB(h.absolutePos(BlockPos.ZERO)).inflate(6));
        }
    }

    private static ServerBossEvent bar(GameTestHelper h,MaodieFighter fighter) {
        for(var field:MaodieFighter.class.getDeclaredFields()) {
            if(ServerBossEvent.class.isAssignableFrom(field.getType())) {
                try {field.setAccessible(true);return (ServerBossEvent)field.get(fighter);}
                catch(ReflectiveOperationException e){throw new AssertionError(e);}
            }
        }
        h.assertTrue(false,"Boxing cat must expose a real server Boss bar to tracked players");
        throw new AssertionError("Missing server boss event");
    }

    @GameTest(template="accessory_probe",batch="boxing_boss_bar",timeoutTicks=15)
    public static void bossBarTracksDisplayNameAndHealthEvenWithNoAi(GameTestHelper h) {
        var a=new Arena(h);var fighter=a.fighter();var bar=bar(h,fighter);
        h.assertTrue(bar.getName().equals(fighter.getDisplayName()),"Initial Boss title uses the entity display name");
        h.assertTrue(bar.getProgress()==1F,"A fresh 300 HP boxer has a full Boss bar");
        fighter.setHealth(75);fighter.setCustomName(Component.literal("Named boxing cat"));
        h.runAfterDelay(2,()->{
            h.assertTrue(Math.abs(bar.getProgress()-.25F)<.000001F,"NoAI must not freeze Boss health synchronization");
            h.assertTrue(bar.getName().equals(fighter.getDisplayName()),"Boss bar follows the current display name");
            fighter.setHealth(300);
        });
        h.runAfterDelay(4,()->{
            h.assertTrue(bar.getProgress()==1F,"Healing restores the full Boss bar");
            h.succeed();
        });
    }

    @GameTest(template="accessory_probe",batch="boxing_boss_tracking")
    public static void trackingExitAndRemovalRemoveBossBarViewers(GameTestHelper h) {
        var a=new Arena(h);var fighter=a.fighter();var viewer=a.viewer();var bar=bar(h,fighter);
        fighter.startSeenByPlayer(viewer);
        h.assertTrue(bar.getPlayers().contains(viewer),"Tracking the fighter adds its Boss bar");
        fighter.stopSeenByPlayer(viewer);
        h.assertTrue(!bar.getPlayers().contains(viewer),"Leaving tracking removes the Boss bar");
        fighter.startSeenByPlayer(viewer);fighter.discard();
        h.assertTrue(bar.getPlayers().isEmpty(),"Discard/unload removes every remaining Boss bar viewer");
        fighter.startSeenByPlayer(viewer);
        h.assertTrue(bar.getPlayers().isEmpty(),"A removed fighter cannot re-add Boss bar viewers");
        h.succeed();
    }

    @GameTest(template="accessory_probe",batch="boxing_boss_death",timeoutTicks=30)
    public static void realDeathClearsBossBarDropsRewardsAndRemainsPermanent(GameTestHelper h) {
        var a=new Arena(h);var fighter=a.fighter();var viewer=a.viewer();var bar=bar(h,fighter);
        fighter.startSeenByPlayer(viewer);
        fighter.hurt(h.getLevel().damageSources().genericKill(),Float.MAX_VALUE);
        h.assertTrue(!fighter.isAlive()&&!fighter.isRemoved(),"Lethal damage retains the vanilla death animation");
        h.assertTrue(bar.getPlayers().isEmpty(),"Death immediately removes Boss bar viewers");
        fighter.startSeenByPlayer(viewer);
        h.assertTrue(bar.getPlayers().isEmpty(),"A corpse cannot gain new Boss bar viewers");
        assertTrainingDrops(h,a.drops());
        var rewardsAtDeath=rewardCounts(a.drops());
        h.runAfterDelay(23,()->{
            h.assertTrue(fighter.isRemoved(),"Death completes permanent corpse removal");
            h.assertTrue(!fighter.restorationPending(),"Boss promotion does not restore the original cat");
            h.assertTrue(rewardCounts(a.drops()).equals(rewardsAtDeath),
                    "Corpse removal does not reroll, duplicate or remove death rewards");
            h.succeed();
        });
    }

    @GameTest(template="accessory_probe",batch="boxing_boss_no_mob_loot",timeoutTicks=30)
    public static void disabledMobLootSuppressesAllTrainingAndTrophyDrops(GameTestHelper h) {
        var a=new Arena(h);
        var rule=h.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT);
        boolean before=rule.get();
        a.cleanups.add(()->rule.set(before,h.getLevel().getServer()));
        rule.set(false,h.getLevel().getServer());
        var fighter=a.fighter();
        fighter.hurt(h.getLevel().damageSources().genericKill(),Float.MAX_VALUE);
        h.assertTrue(!fighter.isAlive(),"The gamerule case exercises a real lethal hit");
        h.assertTrue(a.drops().isEmpty(),"doMobLoot=false suppresses all training and trophy drops at death");
        h.runAfterDelay(23,()->{
            h.assertTrue(fighter.isRemoved(),"Loot-disabled death still removes the corpse normally");
            h.assertTrue(a.drops().isEmpty(),"Corpse removal cannot bypass doMobLoot=false");
            h.succeed();
        });
    }

    private static Map<String,Integer> rewardCounts(List<ItemEntity> drops){
        var counts=new HashMap<String,Integer>();
        for(var drop:drops)counts.merge(BuiltInRegistries.ITEM.getKey(drop.getItem().getItem()).toString(),
                drop.getItem().getCount(),Integer::sum);
        return counts;
    }

    @GameTest(template="accessory_probe",batch="boxing_boss_rewards")
    public static void trainingLootRetainsOneToThreeItemsAndAllSevenButterRewardKinds(GameTestHelper h) {
        var a=new Arena(h);var fighter=a.fighter();var seen=new HashSet<String>();var counts=new HashSet<Integer>();
        for(int seed=0;seed<128;seed++){
            a.drops().forEach(Entity::discard);fighter.getRandom().setSeed(seed);
            dropRewards(fighter);
            var drops=a.drops();counts.add(assertTrainingDrops(h,drops));
            for(var drop:drops){
                var id=BuiltInRegistries.ITEM.getKey(drop.getItem().getItem()).getPath();
                if(TRAINING_REWARDS.contains(id))seen.add(id);
            }
        }
        h.assertTrue(seen.equals(TRAINING_REWARDS),"Every one of the six advanced cans and Super Dried Fish can drop");
        h.assertTrue(counts.equals(Set.of(1,2,3)),"Training drop count retains all of one, two and three");
        h.succeed();
    }

    @GameTest(template="accessory_probe",batch="boxing_boss_chance")
    public static void boxingGlovesDropBelowButNotAtExactQuarterBoundary(GameTestHelper h) {
        var a=new Arena(h);var fighter=a.fighter();
        Item gloves=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("laowu","cat_boxing_gloves"));
        h.assertTrue(gloves!=Items.AIR,"Boxing gloves reward is registered");
        // Hand-derived LegacyRandomSource fixtures: count=1, then reward index, then trophy roll.
        // Rolls are 0.24999994039535522, exactly 0.25, and 0.2500000596046448.
        long[] seeds={24547080999544L,188099349047084L,75294395451130L};
        String[] expectedReward={"super_intelligence_cat_can","super_health_cat_can","super_stamina_cat_can"};
        int[] expectedGloves={1,0,0};
        for(int index=0;index<seeds.length;index++){
            a.drops().forEach(Entity::discard);fighter.getRandom().setSeed(seeds[index]);dropRewards(fighter);
            var drops=a.drops();
            h.assertTrue(assertTrainingDrops(h,drops)==1,"The independent trophy roll never replaces the training reward");
            final String reward=expectedReward[index];
            h.assertTrue(drops.stream().anyMatch(e->BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).getPath().equals(reward)),
                    "Training reward is selected before the independent trophy roll");
            int count=drops.stream().filter(e->e.getItem().is(gloves)).mapToInt(e->e.getItem().getCount()).sum();
            h.assertTrue(count==expectedGloves[index],"25% threshold is strict, fixture="+index+" actual gloves="+count);
        }
        h.succeed();
    }

    private static int assertTrainingDrops(GameTestHelper h,List<ItemEntity> drops){
        int training=0,gloves=0;
        for(var drop:drops){
            var id=BuiltInRegistries.ITEM.getKey(drop.getItem().getItem());
            h.assertTrue(id.getNamespace().equals("laowu"),"Boss drops only its designated rewards");
            if(TRAINING_REWARDS.contains(id.getPath()))training+=drop.getItem().getCount();
            else {
                h.assertTrue(id.getPath().equals("cat_boxing_gloves"),"No butter/giant trophy or unrelated loot: "+id);
                gloves+=drop.getItem().getCount();
            }
        }
        h.assertTrue(training>=1&&training<=3,"Actual world contains one to three training rewards, got "+training);
        h.assertTrue(gloves<=1,"Independent trophy drop is at most one boxing glove item");
        return training;
    }

    private static void dropRewards(MaodieFighter fighter){
        try {
            Class<?> type=fighter.getClass();Method method=null;
            while(type!=null&&method==null){
                try {method=type.getDeclaredMethod("dropCustomDeathLoot",net.minecraft.world.damagesource.DamageSource.class,int.class,boolean.class);}
                catch(NoSuchMethodException ignored){type=type.getSuperclass();}
            }
            if(method==null)throw new AssertionError("Vanilla death-loot hook is missing");
            method.setAccessible(true);method.invoke(fighter,fighter.damageSources().genericKill(),0,true);
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
}
