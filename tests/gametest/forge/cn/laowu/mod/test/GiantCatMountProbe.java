package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.accessory.CatAccessories;
import cn.laowu.mod.entity.CatGiantCarrier;
import cn.laowu.mod.genetics.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class GiantCatMountProbe {
    private record Fixture(Cat cat, ServerPlayer owner, ServerPlayer stub) {
        void close() { if (cat.getVehicle() instanceof CatGiantCarrier carrier) carrier.release(); cat.discard(); owner.discard(); stub.discard(); }
    }
    private static Fixture fixture(GameTestHelper h) {
        var level = h.getLevel();
        if (level.getServer() instanceof GameTestServer) {
            var first=h.absolutePos(new BlockPos(0,0,0));
            var last=h.absolutePos(new BlockPos(18,7,12));
            for(int x=Math.floorDiv(first.getX(),16);x<=Math.floorDiv(last.getX(),16);x++)
                for(int z=Math.floorDiv(first.getZ(),16);z<=Math.floorDiv(last.getZ(),16);z++)
                    level.setChunkForced(x,z,true);
        }
        for (int x=0;x<18;x++) for (int z=0;z<12;z++) {
            h.setBlock(new BlockPos(x,0,z), Blocks.STONE);
            for (int y=1;y<7;y++) h.setBlock(new BlockPos(x,y,z), Blocks.AIR);
        }
        Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8,1,6)));
        var stub = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(),"giant-stub"));
        var owner = new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"giant-owner"));
        owner.connection=stub.connection; owner.setPos(at);
        var cat = new Cat(EntityType.CAT,level) {
            @Override public net.minecraft.world.entity.LivingEntity getOwner() { return owner; }
        };
        cat.setTame(true); cat.setOwnerUUID(owner.getUUID()); cat.setAge(0); cat.setPos(at);
        level.addFreshEntity(cat); CatTraitData.set(cat,CatTraitProfile.EMPTY); cat.setAge(0);
        CatAttributeData.set(cat,CatAttributeData.ensure(cat).withValues(CatStat.HEALTH,100,100));
        CatPoseData.setPose(cat,CatPoseData.NORMAL); cat.setOrderedToSit(false); cat.setInSittingPose(false);
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("laowu","cat_giant_collar"));
        var inventory = CatProfileData.openContainer(cat);
        inventory.setItem(0,new ItemStack(item)); inventory.setChanged(); CatAccessories.equipmentChanged(cat);
        return new Fixture(cat,owner,stub);
    }

    @GameTest(template="artillery_probe",batch="giant_career_tick",timeoutTicks=40)
    public static void mountedCatStillRefreshesCareerAttributes(GameTestHelper h) {
        var f=fixture(h);var cat=f.cat();
        h.assertTrue(CatGiantMount.start(cat,f.owner()),"Giant ride starts");
        // A saved outfit marker is first consumed by the real tick, not an equip helper.
        cat.getPersistentData().putString(CatClothesData.OUTFIT_TAG,CatOutfitType.TERMINATOR.id());
        CommonEvents.onLivingTick(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(cat));
        var armor=cat.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        h.assertTrue(armor.getModifier(UUID.fromString("229dfdc2-726f-4f8e-90d8-51fc0e2996ef"))!=null,"Riding still applies career armor");
        h.assertTrue(cat.getTarget()==null && !cat.isNoAi(),"Riding suppresses combat without disabling support AI");
        f.close();h.succeed();
    }
    @GameTest(template="artillery_probe",batch="giant_career_combat",timeoutTicks=40)
    public static void rideSuppressesCareerRetaliationAndProjectileDamageUntilExit(GameTestHelper h) {
        var f=fixture(h);var cat=f.cat();
        CatClothesData.equip(cat,CatOutfitType.TERMINATOR);
        CareerCatBehavior.tick(cat);
        var victim=h.spawn(EntityType.COW,new BlockPos(12,1,6));victim.setNoAi(true);
        cat.setTarget(victim);
        h.assertTrue(CareerCatBehavior.canParticipateInCombat(cat),"Unridden giant retains career combat");
        h.assertTrue(CatGiantMount.start(cat,f.owner()),"Owner starts giant ride");
        cat.setTarget(victim);cat.setLastHurtByMob(victim);
        h.assertTrue(!CareerCatBehavior.canParticipateInCombat(cat),"Ridden giant cannot acquire or continue career attacks");
        CareerCatBehavior.tick(cat);
        h.assertTrue(cat.getTarget()==null && cat.getLastHurtByMob()==null,"Mounted career tick clears stale attack and retaliation targets");
        float health=victim.getHealth();
        victim.hurt(h.getLevel().damageSources().mobAttack(cat),2);
        h.assertTrue(victim.getHealth()==health,"Mounted cat cannot deal melee damage");
        victim.invulnerableTime=0;
        victim.hurt(h.getLevel().damageSources().thorns(cat),2);
        h.assertTrue(victim.getHealth()==health,"Mounted cat cannot retaliate through thorns");
        var arrow=(net.minecraft.world.entity.projectile.AbstractArrow)EntityType.ARROW.create(h.getLevel());
        arrow.setOwner(cat);victim.invulnerableTime=0;
        victim.hurt(h.getLevel().damageSources().arrow(arrow,cat),2);
        h.assertTrue(victim.getHealth()==health,"Already-launched cat projectile cannot hurt while its owner is ridden");
        CatGiantMount.release(cat);
        h.assertTrue(CareerCatBehavior.canParticipateInCombat(cat),"Dismount immediately restores career combat");
        victim.invulnerableTime=0;victim.hurt(h.getLevel().damageSources().mobAttack(cat),2);
        h.assertTrue(victim.getHealth()<health,"Unridden giant deals career damage again");
        arrow.discard();victim.discard();f.close();h.succeed();
    }
    @GameTest(template="artillery_probe",batch="giant_career_support",timeoutTicks=50)
    public static void riddenMedicAndMusicianKeepNearbySupport(GameTestHelper h) {
        var f=fixture(h);var cat=f.cat();
        CatClothesData.equip(cat,CatOutfitType.MEDICAL);
        h.assertTrue(CatGiantMount.start(cat,f.owner()),"Medic giant ride starts");
        var carrier=(CatGiantCarrier)cat.getVehicle();carrier.tick();
        cat.setHealth(cat.getMaxHealth()-2);
        var medic=new CatMedicalSupportGoal(cat);
        h.assertTrue(medic.canUse(),"Mounted medic remains eligible to support");
        medic.start();medic.tick();
        h.assertTrue(CatMedicalHealing.casting(cat),"Grounded mount permits nearby medic channel");
        // The carrier, not support navigation, owns movement.
        h.assertTrue(cat.getVehicle()==carrier && f.owner().getVehicle()==carrier,"Support keeps the giant ride intact");
        CatClothesData.equip(cat,CatOutfitType.MUSIC);
        var ally=h.spawn(EntityType.CAT,new BlockPos(10,1,6));
        ally.setTame(true);ally.setOwnerUUID(f.owner().getUUID());
        CatClothesData.equip(ally,CatOutfitType.TERMINATOR);ally.setNoAi(true);
        var enemy=h.spawn(EntityType.COW,new BlockPos(12,1,6));enemy.setNoAi(true);ally.setTarget(enemy);
        var musician=new CatMusicSupportGoal(cat);
        h.assertTrue(musician.canUse(),"Mounted musician remains eligible to support");
        musician.start();musician.tick();CatMusicSupport.flush(h.getLevel());
        h.assertTrue(CatMusicSupport.performing(cat) && CatMusicSupport.bonus(ally)>0,"Mounted musician grants real nearby career haste");
        h.assertTrue(cat.getVehicle()==carrier,"Support never swaps to a small career mount");
        musician.stop();medic.stop();enemy.discard();ally.discard();f.close();h.succeed();
    }

    @GameTest(template="artillery_probe",batch="giant_career_healing_pulse",timeoutTicks=65)
    public static void mountedMedicCompletesHealingPulseOnRealServerTicks(GameTestHelper h) {
        var f=fixture(h);var cat=f.cat();
        CatAttributeData.set(cat,CatAttributeData.ensure(cat).withValues(CatStat.INTELLIGENCE,0,0));
        CatClothesData.equip(cat,CatOutfitType.MEDICAL);
        h.assertTrue(CatGiantMount.start(cat,f.owner()),"Medic starts giant ride");
        var carrier=(CatGiantCarrier)cat.getVehicle();
        cat.setHealth(5);long started=h.getLevel().getGameTime();
        h.runAfterDelay(40,()->{
            h.assertTrue(cat.getVehicle()==carrier && f.owner().getVehicle()==carrier
                    && carrier.grounded() && carrier.horizontalSpeed()<.001,
                    "Real server ticks keep the idle giant ride grounded and intact");
            h.assertTrue(CatMedicalHealing.casting(cat),"Real mounted career AI continues the healing channel");
            h.assertTrue(cat.getHealth()>5 && cat.getPersistentData().getLong(CatMedicalHealing.NEXT_HEAL)>started,
                    "Mounted medic completes a real healing pulse after windup and server-end flush");
            f.close();h.succeed();
        });
    }
    @GameTest(template="artillery_probe",batch="giant_career_seat_crossing",timeoutTicks=40)
    public static void ridingAcrossSeatDoesNotBecomeStationedCareer(GameTestHelper h) {
        var f=fixture(h);var cat=f.cat();
        CatClothesData.equip(cat,CatOutfitType.MEDICAL);
        h.assertTrue(CatGiantMount.start(cat,f.owner()),"Medic starts giant ride before crossing a seat");
        BlockPos seat=cat.blockPosition().below();
        h.getLevel().setBlockAndUpdate(seat,com.simibubi.create.AllBlocks.SEATS
                .get(net.minecraft.world.item.DyeColor.WHITE).getDefaultState());
        h.assertTrue(CareerCatBehavior.findSeat(cat)==null,
                "A Create Seat beneath the giant carrier is not the cat's workstation");
        h.assertTrue(!CareerCatBehavior.isCombatResting(cat) && CatSupportRules.canWork(cat),
                "Crossing a seat does not disable mounted nearby support");
        h.assertTrue(!CatMedicalWork.available(cat),
                "Crossing a seat cannot activate the seat-only clinic");
        CatGiantMount.release(cat);cat.setInSittingPose(true);
        h.assertTrue(seat.equals(CareerCatBehavior.findSeat(cat)),
                "A genuinely sitting cat still recognizes the existing seat workstation");
        f.close();h.succeed();
    }
    @GameTest(template="artillery_probe",batch="giant_career_last_stand",timeoutTicks=40)
    public static void fatalDynamiteTransitionReleasesRideBeforeCharge(GameTestHelper h) {
        var f=fixture(h);var cat=f.cat();
        CatClothesData.equip(cat,CatOutfitType.DYNAMITE);
        h.assertTrue(CatGiantMount.start(cat,f.owner()),"Dynamite career can ride normally");
        var enemy=h.spawn(EntityType.COW,new BlockPos(12,1,6));enemy.setNoAi(true);
        float health=enemy.getHealth();
        h.assertTrue(DynamiteCatLastStand.tryBegin(cat,h.getLevel().damageSources().mobAttack(enemy)),
                "Fatal transition preserves career last stand");
        h.assertTrue(!cat.isPassenger() && !f.owner().isPassenger(),"Charge releases giant ride before combat can resume");
        h.assertTrue(enemy.getHealth()==health,"Fatal transition itself never attacks from the ride");
        f.owner().setPos(cat.position());
        h.assertTrue(!CatGiantMount.start(cat,f.owner()),"Active suicide charge cannot be remounted");
        enemy.discard();f.close();h.succeed();
    }
    @GameTest(template="artillery_probe",batch="giant_mount_identity",timeoutTicks=40)
    public static void mountsSameCatAndUnequipReleases(GameTestHelper h) {
        var f=fixture(h); var cat=f.cat(); var owner=f.owner(); var id=cat.getUUID();
        cat.getPersistentData().putString("GiantProbeIdentity","kept");
        h.assertTrue(CatGiantMount.active(cat),"Collar makes the original adult cat giant");
        cat.refreshDimensions();
        h.assertTrue(cat.getBbWidth()>1.5 && cat.getBbHeight()>2.5,"Giant silhouette has a real enlarged collision box");
        h.assertTrue(CatGiantMount.start(cat,owner),"Owner can mount barehanded");
        h.assertTrue(owner.getVehicle() instanceof CatGiantCarrier,"Owner rides the giant carrier");
        var carrier=(CatGiantCarrier)owner.getVehicle();
        h.assertTrue(carrier.cat()==cat && carrier.rider()==owner,"Carrier reuses the original cat and owner");
        h.assertTrue(cat.getUUID().equals(id) && cat.getPersistentData().getString("GiantProbeIdentity").equals("kept"),"Cat identity and data remain unchanged");
        var inventory=CatProfileData.openContainer(cat); inventory.setItem(0,ItemStack.EMPTY); inventory.setChanged();
        CatAccessories.equipmentChanged(cat); CatGiantMount.tick(cat);
        h.assertTrue(owner.getVehicle()!=carrier && cat.getVehicle()!=carrier && carrier.isRemoved(),"Unequipping releases both passengers");
        cat.refreshDimensions();
        h.assertTrue(cat.getBbWidth()<1.0 && cat.getBbHeight()<1.0,"Removing collar restores vanilla collision");
        h.assertTrue(cat.getUUID().equals(id) && cat.getPersistentData().getString("GiantProbeIdentity").equals("kept"),"Original cat and data survive removal");
        f.close(); h.succeed();
    }
    @GameTest(template="artillery_probe",batch="giant_health_size",timeoutTicks=40)
    public static void healthScalesCollisionAndSeatWithoutDamageShrink(GameTestHelper h) {
        var f=fixture(h);var cat=f.cat();var owner=f.owner();
        CatGiantMount.tick(cat);
        h.assertTrue(Math.abs(cat.getBbWidth()-2)<.001,"100 health keeps existing width");
        // Genetics remain 0..100; valid script bonuses exercise values beyond that ceiling.
        setHealthAttribute(cat,400);
        CatGiantMount.tick(cat);
        h.assertTrue(Math.abs(cat.getBbWidth()-4)<.001 && Math.abs(cat.getBbHeight()-6)<.001,
                "400 health doubles each dimension immediately");
        cat.setHealth(1);CatGiantMount.tick(cat);
        h.assertTrue(Math.abs(cat.getBbWidth()-4)<.001,"Damage does not shrink health-attribute size");
        h.assertTrue(CatGiantMount.start(cat,owner),"400-health mount starts in clear space");
        var carrier=(CatGiantCarrier)owner.getVehicle();carrier.positionRider(owner);
        h.assertTrue(Math.abs(carrier.getBbWidth()-4)<.001 && carrier.getBbHeight()>6.8,
                "Carrier collision follows giant scale while covering rider");
        h.assertTrue(Math.abs(owner.getY()-carrier.getY()-5)<.001,"400-health saddle seat scales but rider does not");
        carrier.release();
        setHealthAttribute(cat,10000);
        CatGiantMount.tick(cat);
        h.assertTrue(Math.abs(cat.getBbWidth()-20)<.01,"10000 health is not clamped to old giant size");
        f.close();h.succeed();
    }
    private static void setHealthAttribute(Cat cat,int health) {
        CatAttributeData.set(cat,CatAttributeData.ensure(cat).withValues(CatStat.HEALTH,Math.min(100,health),100));
        CatTraitData.set(cat,CatTraitProfile.EMPTY.withLevel(CatTrait.NIGHT_OWL,1));
        var state=new CompoundTag();var bonus=new CompoundTag();bonus.putInt("health",Math.max(0,health-100));
        state.put("StatBonuses",bonus);CatTraitScriptState.write(cat,CatTrait.NIGHT_OWL.id().toString(),state);
    }
    @GameTest(template="artillery_probe",batch="giant_growth_clearance",timeoutTicks=40)
    public static void mountedGrowthUnderRoofSafelyDismounts(GameTestHelper h) {
        var f=fixture(h);var cat=f.cat();var owner=f.owner();
        h.assertTrue(CatGiantMount.start(cat,owner),"Start at 100 health");
        var carrier=(CatGiantCarrier)owner.getVehicle();
        for(int x=6;x<=10;x++)for(int z=4;z<=8;z++)h.setBlock(new BlockPos(x,6,z),Blocks.STONE);
        setHealthAttribute(cat,400);carrier.updateSize();
        h.assertTrue(owner.getVehicle()==null && carrier.isRemoved(),"Growth releases rider before raising seat through roof");
        h.assertTrue(!h.getLevel().getBlockCollisions(owner,owner.getBoundingBox()).iterator().hasNext(),
                "Released rider remains outside solid blocks");
        f.close();h.succeed();
    }
    @GameTest(template="artillery_probe",batch="giant_rest_toggle",timeoutTicks=40)
    public static void ownerSneakTogglesPersistentRest(GameTestHelper h) {
        var f=fixture(h);var cat=f.cat();var owner=f.owner();owner.setShiftKeyDown(true);
        h.assertTrue(CatGiantMount.interact(cat,owner,InteractionHand.MAIN_HAND).consumesAction(),
                "Sneak empty-hand interaction is consumed");
        h.assertTrue(cat.isOrderedToSit() && cat.isInSittingPose() && !cat.isPassenger(),"Rest orders stay and exposes synced pose");
        var saved=new CompoundTag();cat.saveWithoutId(saved);
        h.assertTrue(saved.getBoolean("Sitting"),"Rest order persists in vanilla cat save");
        CatGiantMount.interact(cat,owner,InteractionHand.MAIN_HAND);
        h.assertTrue(!cat.isOrderedToSit() && !cat.isInSittingPose(),"Second sneak interaction stands up");
        owner.setShiftKeyDown(false);f.close();h.succeed();
    }
    @GameTest(template="artillery_probe",batch="giant_mount_authority",timeoutTicks=40)
    public static void rejectsNonOwnerAndInvalidInput(GameTestHelper h) {
        var f=fixture(h); var cat=f.cat(); var owner=f.owner();
        var stranger = new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"giant-stranger"));
        stranger.connection=f.stub().connection; stranger.setPos(cat.position());
        h.assertTrue(!CatGiantMount.start(cat,stranger),"Non-owner cannot mount");
        h.assertTrue(CatGiantMount.start(cat,owner),"Owner mounts");
        var carrier=(CatGiantCarrier)owner.getVehicle(); var before=carrier.position();
        carrier.input(stranger,1,0,0,false,true);
        carrier.input(owner,Float.NaN,0,0,false,true);
        carrier.tick();
        h.assertTrue(carrier.position().distanceToSqr(before)<.001,"Forged or non-finite controls do not move the mount");
        carrier.input(owner,1,0,0,false,true);
        for(int i=0;i<5;i++) carrier.tick();
        h.assertTrue(carrier.position().distanceToSqr(before)>.01,"Validated owner controls move server-side");
        carrier.release(); stranger.discard(); f.close(); h.succeed();
    }
    @GameTest(template="artillery_probe",batch="giant_mount_server_motion",timeoutTicks=50)
    public static void serverTicksMoveCollideAndJump(GameTestHelper h) {
        var f=fixture(h); var cat=f.cat(); var owner=f.owner();
        h.assertTrue(CatGiantMount.start(cat,owner),"Real owner starts mount");
        var carrier=(CatGiantCarrier)owner.getVehicle();
        for(int y=1;y<=5;y++)for(int z=4;z<=8;z++)h.setBlock(new BlockPos(10,y,z),Blocks.STONE);
        double startX=carrier.getX(),floorY=carrier.getY();
        h.runAfterDelay(3,()->{
            h.assertTrue(carrier.grounded(),"Normal server ticks establish ground contact");
            carrier.input(owner,1,0,-90,false,true);
        });
        h.runAfterDelay(15,()->{
            h.assertTrue(carrier.getX()>startX+.1,"Server ticks move from validated forward input");
            h.assertTrue(carrier.getX()<=h.absolutePos(new BlockPos(10,1,6)).getX()-CatGiantMount.WIDTH/2+.01,
                    "Carrier width stops before a solid wall");
            h.assertTrue(carrier.grounded(),"Idle downward contact keeps ground state");
            h.assertTrue(carrier.horizontalSpeed()<.05,"Synced speed reflects collision-clamped movement");
            carrier.input(owner,0,0,-90,true,false);
        });
        h.runAfterDelay(17,()->{
            h.assertTrue(carrier.grounded() && carrier.jumpWindup()>0,"Jump has a grounded anticipation phase");
        });
        h.runAfterDelay(20,()->{
            h.assertTrue(carrier.getY()>floorY+.1 && !carrier.grounded(),"Jump rises on real server ticks");
            carrier.release(); f.close(); h.succeed();
        });
    }


    // Network jitter may deliver a neutral packet, press and release in one server tick.
    @GameTest(template="artillery_probe",batch="giant_mount_jitter_tap",timeoutTicks=30)
    public static void shortTapSurvivesSameServerTickPackets(GameTestHelper h) {
        var f=fixture(h);var owner=f.owner();
        h.assertTrue(CatGiantMount.start(f.cat(),owner),"Owner mounts for packet jitter test");
        var carrier=(CatGiantCarrier)owner.getVehicle();
        double floorY=carrier.getY();
        h.runAfterDelay(3,()->{
            h.assertTrue(carrier.grounded(),"Jitter tap starts grounded");
            carrier.input(owner,0,0,0,false,false);
            carrier.input(owner,0,0,0,true,false);
            carrier.input(owner,0,0,0,false,false);
        });
        h.runAfterDelay(9,()->{
            h.assertTrue(carrier.getY()>floorY+.1&&!carrier.grounded(),"Valid short tap must survive same-tick packet coalescing");
            carrier.release();f.close();h.succeed();
        });
    }

    // A one-tick press followed by release must survive anticipation and launch exactly once.
    @GameTest(template="artillery_probe",batch="giant_mount_tap_jump",timeoutTicks=85)
    public static void shortTapCompletesJumpAndHoldNeverAutoRepeats(GameTestHelper h) {
        var f=fixture(h);var owner=f.owner();
        h.assertTrue(CatGiantMount.start(f.cat(),owner),"Real owner starts tap-jump test");
        var carrier=(CatGiantCarrier)owner.getVehicle();
        double floorY=carrier.getY();
        h.runAfterDelay(3,()->{
            h.assertTrue(carrier.grounded(),"Tap starts on actual ground");
            carrier.input(owner,0,0,0,true,false);
        });
        h.runAfterDelay(4,()->carrier.input(owner,0,0,0,false,false));
        h.runAfterDelay(9,()->h.assertTrue(carrier.getY()>floorY+.1&&!carrier.grounded(),"Released one-tick tap must still launch"));
        h.runAfterDelay(34,()->{
            h.assertTrue(carrier.grounded()&&Math.abs(carrier.getY()-floorY)<.02,"Tap lands back on floor");
            carrier.input(owner,0,0,0,true,false);
        });
        for(int tick=35;tick<76;tick++)h.runAfterDelay(tick,()->carrier.input(owner,0,0,0,true,false));
        h.runAfterDelay(40,()->h.assertTrue(carrier.getY()>floorY+.1,"Next distinct press launches another jump"));
        h.runAfterDelay(76,()->{
            h.assertTrue(carrier.grounded()&&Math.abs(carrier.getY()-floorY)<.02&&carrier.jumpWindup()==0,"Held key must not automatically jump again after landing");
            carrier.release();f.close();h.succeed();
        });
    }

    @GameTest(template="artillery_probe",batch="giant_mount_reload",timeoutTicks=40)
    public static void orphanedSavedMountReleasesOriginalCat(GameTestHelper h) {
        var f=fixture(h); var cat=f.cat(); var owner=f.owner();
        cat.getPersistentData().putString("GiantProbeIdentity","reload-kept");
        h.assertTrue(CatGiantMount.start(cat,owner),"Owner starts saveable mount");
        var carrier=(CatGiantCarrier)owner.getVehicle();
        var saved=new CompoundTag();
        h.assertTrue(carrier.saveAsPassenger(saved),"Carrier root can be saved with original cat passenger");
        var restored=EntityType.loadEntityRecursive(saved,h.getLevel(),entity->entity);
        h.assertTrue(restored instanceof CatGiantCarrier,"Saved root reloads as giant carrier");
        var loaded=((CatGiantCarrier)restored).cat();
        h.assertTrue(loaded!=null && loaded.getUUID().equals(cat.getUUID()),"Reloaded passenger is the same cat UUID");
        h.assertTrue(loaded.getPersistentData().getString("GiantProbeIdentity").equals("reload-kept"),"Cat NBT survives root save");
        restored.tick();
        h.assertTrue(!loaded.isRemoved() && !loaded.isPassenger() && restored.isRemoved(),
                "Orphaned carrier releases cat after player logout/reload");
        carrier.release(); f.close(); h.succeed();
    }
    @GameTest(template="artillery_probe",batch="giant_mount_other_vehicle",timeoutTicks=40)
    public static void otherMountKeepsItsOwnCatPose(GameTestHelper h) {
        var f=fixture(h); var cat=f.cat();
        var boat=EntityType.BOAT.create(h.getLevel());
        h.assertTrue(boat!=null,"Create ordinary vehicle fixture");
        boat.setPos(cat.position());h.getLevel().addFreshEntity(boat);
        h.assertTrue(cat.startRiding(boat,true),"Collared cat can be a passenger of another vehicle");
        h.assertTrue(!CatGiantMount.active(cat),"Other vehicle retains its original cat pose and dimensions");
        cat.stopRiding();
        h.assertTrue(CatGiantMount.active(cat),"Giant accessory resumes after the other ride ends");
        boat.discard();f.close();h.succeed();
    }
}
