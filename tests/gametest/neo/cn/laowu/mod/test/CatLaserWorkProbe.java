package cn.laowu.mod.test;
import cn.laowu.mod.*;
import cn.laowu.mod.create.*;
import cn.laowu.mod.entity.MechanicalLaserProjectile;
import cn.laowu.mod.genetics.*;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.kinetics.motor.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CatLaserWorkProbe {

    @GameTest(template="artillery_probe",batch="auto_laser_filtered_work",timeoutTicks=1190)
    public static void filterAllowsPassiveWorkButStillProtectsOwnerCats(GameTestHelper h) {
        CatAutoLaserTestArea.ready(h,()->{
            var p=new BlockPos(25,4,6);var be=powered(h,p);var cat=seated(h,p.west(4),50);
            var cow=EntityType.COW.create(h.getLevel());cow.setPos(h.absoluteVec(new Vec3(40.5,4.3,6.5)));
            cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);cow.setHealth(1000);h.getLevel().addFreshEntity(cow);
            var filter=new ItemStack(LaoWuMod.CREATURE_FILTER.get());
            cn.laowu.mod.item.CreatureFilterRules.of(1,cn.laowu.mod.item.CreatureFilterRules.Mode.WHITELIST,
                List.of(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft","cow"))).write(filter);
            be.setFilter(filter);
            float[] health={1000};
            h.runAfterDelay(90,()->{
                h.assertTrue(cow.getUUID().equals(be.getTargetUuid())&&cow.getHealth()<1000,"Filter-selected passive mob was not attacked by seated mechanical cat");
                health[0]=cow.getHealth();
                be.setFilter(ItemStack.EMPTY);
            });
            h.runAfterDelay(130,()->{
                h.assertTrue(be.getTargetUuid()==null,"Removing filter did not restore hostile-only selection");
                health[0]=cow.getHealth();
            });
            h.runAfterDelay(160,()->{
                h.assertTrue(cow.getHealth()==health[0],"Stale filtered work continued after removal");
                var ally=EntityType.CAT.create(h.getLevel());ally.setTame(true,true);ally.setOwnerUUID(cat.getOwnerUUID());
                ally.setPos(h.absoluteVec(new Vec3(38.5,4.3,6.5)));h.getLevel().addFreshEntity(ally);
                var any=new ItemStack(LaoWuMod.CREATURE_FILTER.get());be.setFilter(any);
                h.assertTrue(!CatAutoLaserTargets.validTarget(h.getLevel(),be.getBlockPos(),cat,ally),"All-species filter bypassed own-pet safety");
                ally.discard();cow.discard();cat.discard();h.succeed();
            });
        });
    }

    @GameTest(template="artillery_probe",batch="auto_laser_aim_transition",timeoutTicks=290)
    public static void aimTakesTimeAndHoldsItsLastDirection(GameTestHelper h) {
        CatAutoLaserTestArea.ready(h,()->{
            var p=new BlockPos(25,4,6);var be=powered(h,p);var cat=seated(h,p.west(4),50);
            var first=enemy(h,new Vec3(40.5,4.3,6.5));float[] held=new float[2];
            h.runAfterDelay(27,()->{
                h.assertTrue(!(boolean)invoke(be,"isAimLocked",new Class<?>[0]),"Freshly unfolded machine instantly locked");
                held[0]=((Number)invoke(be,"getAimYaw",new Class<?>[]{float.class},1f)).floatValue();
                h.assertTrue(Math.abs(held[0])<Math.toRadians(40)&&first.getHealth()==1000,"Target snapped or was attacked before aiming");
            });
            h.runAfterDelay(28,()->{
                float now=((Number)invoke(be,"getAimYaw",new Class<?>[]{float.class},1f)).floatValue();
                h.assertTrue(Math.abs(now-held[0])<=Math.toRadians(8.01),"Aim exceeded its per-tick turn limit");
            });
            h.runAfterDelay(80,()->{
                h.assertTrue((boolean)invoke(be,"isAimLocked",new Class<?>[0])&&first.getHealth()<1000,"Aimed station failed to engage");
                held[0]=((Number)invoke(be,"getAimYaw",new Class<?>[]{float.class},1f)).floatValue();
                held[1]=((Number)invoke(be,"getAimPitch",new Class<?>[]{float.class},1f)).floatValue();first.discard();
            });
            h.runAfterDelay(105,()->{
                h.assertTrue(be.getTargetUuid()==null,"Dead target remained selected");
                h.assertTrue(Math.abs(((Number)invoke(be,"getAimYaw",new Class<?>[]{float.class},1f)).floatValue()-held[0])<.0001,"Idle yaw returned to center");
                h.assertTrue(Math.abs(((Number)invoke(be,"getAimPitch",new Class<?>[]{float.class},1f)).floatValue()-held[1])<.0001,"Idle pitch returned to center");
                cat.discard();h.succeed();
            });
        });
    }
    private static Cat seated(GameTestHelper h,BlockPos relative,int stats) {
        var level=h.getLevel();var p=h.absolutePos(relative);
        level.setBlockAndUpdate(p.below(),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(p,AllBlocks.SEATS.get(DyeColor.WHITE).getDefaultState());
        var cat=EntityType.CAT.create(level);cat.setTame(true,true);cat.setOwnerUUID(UUID.randomUUID());cat.setAge(0);
        cat.setPos(Vec3.atCenterOf(p));level.addFreshEntity(cat);
        CatTraitData.set(cat,CatTraitProfile.EMPTY);cat.setAge(0);
        var genes=CatAttributeData.ensure(cat);
        for(var stat:CatStat.values())genes=genes.withValues(stat,stats,100);
        CatAttributeData.set(cat,genes);CatClothesData.equip(cat,CatOutfitType.TERMINATOR);
        SeatBlock.sitDown(level,p,cat);cat.getVehicle().positionRider(cat);cat.setOrderedToSit(true);cat.setInSittingPose(true);
        return cat;
    }
    private static net.minecraft.world.entity.monster.Husk enemy(GameTestHelper h,Vec3 relative) {
        var mob=EntityType.HUSK.create(h.getLevel());mob.setPos(h.absoluteVec(relative));mob.setNoAi(true);mob.setNoGravity(true);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);mob.setHealth(1000);h.getLevel().addFreshEntity(mob);return mob;
    }
    private static CatAutoLaserBlockEntity powered(GameTestHelper h,BlockPos p) {
        h.setBlock(p,CatMachineBlocks.CAT_AUTO_LASER.get());
        h.setBlock(p.below(),AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING,Direction.UP));
        ((CreativeMotorBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p.below()))).generatedSpeed.setValue(16);
        return (CatAutoLaserBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p));
    }
    private static Object invoke(Object receiver,String method,Class<?>[] types,Object...args) {
        try {return receiver.getClass().getMethod(method,types).invoke(receiver,args);}
        catch(ReflectiveOperationException ex){throw new AssertionError("Missing work interface "+method,ex);}
    }
    private static List<?> participants(GameTestHelper h,BlockPos p) {
        try{return (List<?>)Class.forName("cn.laowu.mod.create.CatAutoLaserTargets").getMethod("participants",ServerLevel.class,BlockPos.class).invoke(null,h.getLevel(),h.absolutePos(p));}
        catch(ReflectiveOperationException ex){throw new AssertionError("Missing seated laser work targeting",ex);}
    }
    @GameTest(template="artillery_probe",batch="auto_laser_work_far",timeoutTicks=280)
    public static void seatedCatHitsFarTargetWithoutLeavingSeat(GameTestHelper h) {
        CatAutoLaserTestArea.ready(h,()->runseatedCatHitsFarTargetWithoutLeavingSeat(h));
    }
    private static void runseatedCatHitsFarTargetWithoutLeavingSeat(GameTestHelper h) {
        var be=powered(h,new BlockPos(35,4,6));var cat=seated(h,new BlockPos(30,4,6),50);
        var mob=enemy(h,new Vec3(59.4,4.2,6.5));var vehicle=cat.getVehicle();
        h.runAfterDelay(100,()->{
            h.assertTrue(mob.getHealth()<1000,"Seated laser failed to hit target approximately 29 blocks away");
            h.assertTrue(cat.getVehicle()==vehicle&&cat.isOrderedToSit()&&cat.getTarget()==null,"Work shot altered ordinary target or seat");
            h.assertTrue(mob.getUUID().equals(invoke(be,"getTargetUuid",new Class<?>[0])),"Machine did not mark the actual enemy");
            cat.discard();mob.discard();h.succeed();
        });
    }
    @GameTest(template="artillery_probe",batch="auto_laser_work_range",timeoutTicks=200)
    public static void fixedSphericalRangesAndHostileSelection(GameTestHelper h) {
        CatAutoLaserTestArea.ready(h,()->runfixedSphericalRangesAndHostileSelection(h));
    }
    private static void runfixedSphericalRangesAndHostileSelection(GameTestHelper h) {
        var center=new BlockPos(35,4,6);var be=powered(h,center);
        var inner=seated(h,center.west(5),1);var outer=seated(h,center.east(6),100);
        h.assertTrue(participants(h,center).contains(inner)&&!participants(h,center).contains(outer),"Seat radius must be fixed spherical five blocks");
        var diagonal=seated(h,center.offset(3,3,3),100);
        h.assertTrue(!participants(h,center).contains(diagonal),"AABB diagonal incorrectly treated as spherical seat radius");
        var far=enemy(h,new Vec3(59.6,4.5,6.5));var near=enemy(h,new Vec3(39.5,4.5,6.5));
        h.runAfterDelay(45,()->{
            h.assertTrue(near.getUUID().equals(invoke(be,"getTargetUuid",new Class<?>[0])),"Nearest eligible hostile not selected");
            near.discard();
        });
        h.runAfterDelay(70,()->{
            h.assertTrue(invoke(be,"getTargetUuid",new Class<?>[0])==null&&far.getHealth()==1000,"Marked target outside 24-block sphere");
            inner.discard();outer.discard();diagonal.discard();far.discard();h.succeed();
        });
    }
    @GameTest(template="artillery_probe",batch="auto_laser_work_los",timeoutTicks=280)
    public static void shotRechecksBothSightLines(GameTestHelper h) {
        CatAutoLaserTestArea.ready(h,()->runshotRechecksBothSightLines(h));
    }
    private static void runshotRechecksBothSightLines(GameTestHelper h) {
        var p=new BlockPos(25,4,6);var be=powered(h,p);var cat=seated(h,p.west(4),50);
        var mob=enemy(h,new Vec3(40.5,4.3,6.5));float[] health={1000};
        h.runAfterDelay(60,()->{
            h.assertTrue(mob.getHealth()<1000,"Unblocked initial shot failed");
            health[0]=mob.getHealth();
            for(int y=2;y<=8;y++)for(int z=2;z<=10;z++)h.setBlock(new BlockPos(30,y,z),Blocks.STONE);
        });
        h.runAfterDelay(130,()->{
            h.assertTrue(mob.getHealth()==health[0],"Work laser fired through newly inserted wall");
            h.assertTrue(invoke(be,"getTargetUuid",new Class<?>[0])==null,"Obstructed machine retained target");
            cat.discard();mob.discard();h.succeed();
        });
    }
    @GameTest(template="artillery_probe",batch="auto_laser_work_lease",timeoutTicks=280)
    public static void leasesExpireAndRejectReplacement(GameTestHelper h) {
        CatAutoLaserTestArea.ready(h,()->runleasesExpireAndRejectReplacement(h));
    }
    private static void runleasesExpireAndRejectReplacement(GameTestHelper h) {
        var p=new BlockPos(25,4,6);powered(h,p);var cat=seated(h,p.west(4),50);
        var mob=enemy(h,new Vec3(40.5,4.3,6.5));float[] health={1000};
        h.runAfterDelay(60,()->{
            h.assertTrue(mob.getHealth()<1000,"Lease fixture never fired");
            h.setBlock(p,Blocks.AIR);
            h.setBlock(p,CatMachineBlocks.CAT_AUTO_LASER.get());
            h.setBlock(p.below(),Blocks.AIR);
        });
        h.runAfterDelay(80,()->health[0]=mob.getHealth());
        h.runAfterDelay(140,()->{
            h.assertTrue(mob.getHealth()==health[0],"Old lease fired after same-position machine replacement");
            h.assertTrue(!cat.getPersistentData().toString().contains("AutoLaser"),"Transient work lease leaked to cat NBT");
            cat.stopRiding();cat.discard();mob.discard();h.succeed();
        });
    }
    @GameTest(template="artillery_probe",batch="auto_laser_multiple",timeoutTicks=350)
    public static void multipleMachinesChooseNearestWithoutStackingShots(GameTestHelper h) {
        CatAutoLaserTestArea.ready(h,()->runmultipleMachinesChooseNearestWithoutStackingShots(h));
    }
    private static void runmultipleMachinesChooseNearestWithoutStackingShots(GameTestHelper h) {
        // Offset stations sideways so the closest station itself cannot block the cat's shot.
        var a=powered(h,new BlockPos(25,4,4));var b=powered(h,new BlockPos(30,4,4));
        var left=enemy(h,new Vec3(10.5,4.3,7.5));var right=enemy(h,new Vec3(48.5,4.3,7.5));
        // Test arbitration with both stations eligible, independent of unequal turn distances.
        h.startSequence().thenIdle(60).thenExecute(()->{
        h.assertTrue(a.isAimLocked()&&b.isAimLocked(),"Both stations must finish aiming before arbitration");
        var cat=seated(h,new BlockPos(28,4,7),50);
        Map<UUID,Integer> fired=new HashMap<>();int interval=CareerCatBehavior.careerAttackIntervalTicks(cat);
        for(int tick=30;tick<130;tick++) {
            final int time=tick;
            h.runAfterDelay(tick,()->{
                for(var shot:h.getLevel().getEntitiesOfClass(MechanicalLaserProjectile.class,cat.getBoundingBox().inflate(33))) {
                    if(shot.getOwner()!=cat||fired.containsKey(shot.getUUID()))continue;
                    h.assertTrue(shot.getDeltaMovement().x>0,"Farther machine overrode the closest station");
                    int launch=time-shot.tickCount;
                    for(int old:fired.values())h.assertTrue(launch-old>=interval-1,"Multiple machines stacked a cat's firing interval: "+launch+" - "+old+" < "+interval);
                    fired.put(shot.getUUID(),launch);
                }
            });
        }
        h.runAfterDelay(130,()->{
            h.assertTrue(fired.size()>=2,"Multiple-machine fixture did not shoot");
            h.assertTrue(left.getHealth()==1000&&right.getHealth()<1000,"Cat did not retain nearest station target");
            cat.discard();left.discard();right.discard();h.succeed();
        });
        });
    }
    @GameTest(template="artillery_probe",batch="auto_laser_seat_los",timeoutTicks=280)
    public static void catOnlyObstructionAndSeatRemovalStopWork(GameTestHelper h) {
        CatAutoLaserTestArea.ready(h,()->runcatOnlyObstructionAndSeatRemovalStopWork(h));
    }
    private static void runcatOnlyObstructionAndSeatRemovalStopWork(GameTestHelper h) {
        var p=new BlockPos(30,4,6);var be=powered(h,p);var cat=seated(h,p.west(4),50);
        var mob=enemy(h,new Vec3(45.5,4.3,6.5));float[] health={1000};
        h.runAfterDelay(60,()->{
            h.assertTrue(mob.getHealth()<1000,"Seat lifecycle fixture did not fire");health[0]=mob.getHealth();
            for(int y=2;y<=7;y++)for(int z=4;z<=8;z++)h.setBlock(new BlockPos(28,y,z),Blocks.STONE);
        });
        h.runAfterDelay(100,()->{
            h.assertTrue(mob.getUUID().equals(be.getTargetUuid()),"Wall beside cat incorrectly blocked machine's sight");
            h.assertTrue(mob.getHealth()==health[0],"Cat fired through its own blocked sight line");
            for(int y=2;y<=7;y++)for(int z=4;z<=8;z++)h.setBlock(new BlockPos(28,y,z),Blocks.AIR);
            cat.stopRiding();cat.setNoAi(true);
        });
        h.runAfterDelay(120,()->health[0]=mob.getHealth());
        h.runAfterDelay(160,()->{
            h.assertTrue(mob.getHealth()==health[0],"Cat continued station work without actual seat vehicle");
            h.assertTrue(participants(h,p).isEmpty(),"Former seated cat still participates");
            cat.discard();mob.discard();h.succeed();
        });
    }
    @GameTest(template="artillery_probe",batch="auto_laser_kube",timeoutTicks=310)
    public static void realKubeJsCancelsAndSlowsWorkShotWithoutLosingRange(GameTestHelper h) {
        CatAutoLaserTestArea.ready(h,()->runrealKubeJsCancelsAndSlowsWorkShotWithoutLosingRange(h));
    }
    private static void runrealKubeJsCancelsAndSlowsWorkShotWithoutLosingRange(GameTestHelper h) {
        if(!net.neoforged.fml.ModList.get().isLoaded("kubejs")){h.succeed();return;}
        var p=new BlockPos(35,4,6);powered(h,p);var cat=seated(h,p.west(5),50);
        var mob=enemy(h,new Vec3(59.4,4.3,6.5));
        var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kubejs","accessory_probe"));
        h.assertTrue(item!=Items.AIR,"KubeJS startup registration not active");
        CatProfileData.openContainer(cat).setItem(0,new ItemStack(item));
        var accessory=cn.laowu.mod.api.CatAccessoryApi.accessory(cat,"kubejs:accessory_probe");
        h.assertTrue(accessory!=null,"KubeJS accessory definition was not loaded");accessory.setText("probe:state","cancel_shot");
        h.runAfterDelay(65,()->{
            h.assertTrue(accessory.number("probe:projectile")>0,"Work shot never reached real KubeJS callback");
            h.assertTrue(mob.getHealth()==1000,"Canceled work shot still damaged target");
            h.assertTrue(h.getLevel().getEntitiesOfClass(MechanicalLaserProjectile.class,cat.getBoundingBox().inflate(33),s->s.getOwner()==cat).isEmpty(),"Canceled work shot was spawned");
            accessory.setText("probe:state","slow_work");
        });
        Set<UUID> shots=new HashSet<>();
        for(int tick=66;tick<=145;tick++)h.runAfterDelay(tick,()->{
            for(var shot:h.getLevel().getEntitiesOfClass(MechanicalLaserProjectile.class,cat.getBoundingBox().inflate(33),s->s.getOwner()==cat)){
                if(!shots.add(shot.getUUID()))continue;
                h.assertTrue(shot.getAccessoryDamage()==7&&Math.abs(shot.getDeltaMovement().length()-.6)<.001,"Script damage/speed edits or constant work speed lost on real projectile");
                h.assertTrue(shot.getMaxLifetimeTicks()>=59,"Lifetime ignored speed after script callback");
            }
        });
        h.runAfterDelay(165,()->{
            h.assertTrue(!shots.isEmpty()&&mob.getHealth()<1000,"Script-slowed work laser failed to reach approximately 29 blocks");
            CatClothesData.unequip(cat);
        });
        h.runAfterDelay(185,()->{
            h.assertTrue(participants(h,p).isEmpty(),"Outfit removal retained work eligibility");
            cat.discard();mob.discard();h.succeed();
        });
    }
    @GameTest(template="artillery_probe",batch="auto_laser_flight",timeoutTicks=180)
    public static void normalCombatKeepsOriginalRangeAndInvalidNbtUsesSafeDefaults(GameTestHelper h) {
        CatAutoLaserTestArea.ready(h,()->runnormalCombatKeepsOriginalRangeAndInvalidNbtUsesSafeDefaults(h));
    }
    private static void runnormalCombatKeepsOriginalRangeAndInvalidNbtUsesSafeDefaults(GameTestHelper h) {
        var cat=seated(h,new BlockPos(5,4,6),50);
        var normal=new MechanicalLaserProjectile(h.getLevel(),cat,2);normal.setPos(h.absoluteVec(new Vec3(10,9,6)));normal.shoot(1,0,0,2.4f,0);
        h.getLevel().addFreshEntity(normal);
        var work=new MechanicalLaserProjectile(h.getLevel(),cat,2);work.setPos(h.absoluteVec(new Vec3(10,10,6)));work.shoot(1,0,0,2.4f,0);
        invoke(work,"setWorkFlightLimits",new Class<?>[]{double.class,int.class},32d,19);h.getLevel().addFreshEntity(work);
        var saved=new net.minecraft.nbt.CompoundTag();work.saveWithoutId(saved);
        var copy=new MechanicalLaserProjectile(LaoWuMod.MECHANICAL_LASER_PROJECTILE.get(),h.getLevel());copy.load(saved);
        h.assertTrue(copy.getMaxTravelDistance()==32&&copy.getMaxLifetimeTicks()==19,"Actual saved projectile lost work-only limits");
        h.runAfterDelay(10,()->{
            h.assertTrue(normal.isRemoved(),"Ordinary laser exceeded normal 16.5-block flight distance");
            h.assertTrue(!work.isRemoved(),"Work-only extended laser was prematurely removed");
            var tag=new net.minecraft.nbt.CompoundTag();tag.putDouble("LaoWuMechanicalLaserMaxDistance",Double.NaN);tag.putInt("LaoWuMechanicalLaserMaxLifetime",-1);
            work.readAdditionalSaveData(tag);
            h.assertTrue(((Number)invoke(work,"getMaxTravelDistance",new Class<?>[0])).doubleValue()==16.5,"Malformed saved limit did not default safely");
            work.discard();cat.discard();h.succeed();
        });
    }
}
