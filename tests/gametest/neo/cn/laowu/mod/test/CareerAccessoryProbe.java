package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.accessory.*;
import cn.laowu.mod.entity.*;
import cn.laowu.mod.genetics.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CareerAccessoryProbe {
    private static Cat cat(GameTestHelper h,Vec3 p,CatOutfitType outfit){return CareerSupportIntegrationProbe.cat(h.getLevel(),p,outfit,false);}
    private static void equip(Cat cat,String name){
        var items=CatProfileData.openContainer(cat);
        items.setItem(0,name==null?ItemStack.EMPTY:new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("laowu",name))));
        items.setChanged();CatAccessories.equipmentChanged(cat);
    }
    private static Mob enemy(ServerLevel level,Vec3 p){
        var mob=EntityType.HUSK.create(level);mob.setNoAi(true);mob.setNoGravity(true);mob.setPos(p);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);mob.setHealth(1000);
        mob.getAttribute(Attributes.ARMOR).setBaseValue(0);level.addFreshEntity(mob);return mob;
    }
    private static void hit(Object projectile,LivingEntity target){
        try{var method=projectile.getClass().getDeclaredMethod("onHitEntity",EntityHitResult.class);method.setAccessible(true);method.invoke(projectile,new EntityHitResult(target));}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    private static void fire(Cat cat,LivingEntity target){
        try{var method=CareerCatBehavior.class.getDeclaredMethod("applyFireBreathDamage",Cat.class,LivingEntity.class);method.setAccessible(true);method.invoke(null,cat,target);}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    private static void rollBelow(Cat cat,double limit){
        for(int seed=0;seed<10000;seed++){cat.getRandom().setSeed(seed);if(cat.getRandom().nextDouble()<limit){cat.getRandom().setSeed(seed);return;}}
        throw new AssertionError("Unable to establish deterministic roll");
    }
    @GameTest(template="artillery_probe",batch="magnet26",timeoutTicks=50)
    public static void fastMagnet(GameTestHelper h){
        var level=h.getLevel();var p=CareerSupportIntegrationProbe.floor(h).add(5,0,4);
        var cat=cat(h,p,CatOutfitType.NONE);equip(cat,"cat_loot_magnet");
        var loot=new ItemEntity(level,p.x+2.8,p.y+.1,p.z,new ItemStack(Items.DIAMOND,7));
        loot.setNoPickUpDelay();level.addFreshEntity(loot);
        var delayed=new ItemEntity(level,p.x+1,p.y+.1,p.z+1,new ItemStack(Items.EMERALD,3));
        delayed.setPickUpDelay(100);level.addFreshEntity(delayed);
        h.runAfterDelay(25,()->{
            var inv=CatProfileData.openContainer(cat);int diamonds=0;
            for(int slot=4;slot<13;slot++)if(inv.getItem(slot).is(Items.DIAMOND))diamonds+=inv.getItem(slot).getCount();
            h.assertTrue(!loot.isAlive()&&diamonds==7,"Magnet collects from 2.8 blocks in at most 1.25 seconds");
            h.assertTrue(delayed.isAlive()&&delayed.getItem().getCount()==3,"Pickup delay still respected");
            cat.discard();delayed.discard();h.succeed();System.out.println("PASS: fast magnet real floor movement, exact item transfer and pickup-delay guard");
        });
    }
    @GameTest(template="artillery_probe",batch="laser26",timeoutTicks=35)
    public static void laserFollowupAndPilotDodge(GameTestHelper h){
        var level=h.getLevel();var p=CareerSupportIntegrationProbe.floor(h).add(5,0,4);
        var laser=cat(h,p,CatOutfitType.TERMINATOR);equip(laser,"cat_followup_gear");
        var enemy=enemy(level,p.add(2,0,0));float health=enemy.getHealth();
        h.assertTrue(CatAccessories.value(laser,"attack")==-20&&CatAccessories.value(laser,"extra_strike_chance")==25,"Laser gear cost and 25 percent effect");
        rollBelow(laser,.25);CatAccessories.acceptedAttack(enemy,laser.damageSources().mobAttack(laser),1);
        var pilot=cat(h,p.add(0,0,3),CatOutfitType.FLIGHT);equip(pilot,"cat_ace_feather");
        CareerSupportIntegrationProbe.stat(pilot,CatStat.SPEED,100);float pilotHealth=pilot.getHealth();
        rollBelow(pilot,.15);
        h.assertTrue(!pilot.hurt(level.damageSources().mobAttack(enemy),3)&&pilot.getHealth()==pilotHealth,"Real incoming attack cancelled by pilot dodge");
        h.assertTrue(CatAccessoryDefinition.dodgeChance(10000,.15)==.8,"Dodge never exceeds eighty percent");
        float[] after={0};
        h.runAfterDelay(5,()->{after[0]=enemy.getHealth();h.assertTrue(after[0]<health,"Deferred single follow-up lands");});
        h.runAfterDelay(18,()->{
            h.assertTrue(enemy.getHealth()==after[0],"Follow-up cannot chain into more hits");
            laser.discard();pilot.discard();enemy.discard();h.succeed();System.out.println("PASS: actual deferred laser follow-up without chains and actual pilot attack dodge");
        });
    }
    @GameTest(template="artillery_probe",batch="projectiles26",timeoutTicks=35)
    public static void fishingHoneyAndBlueFlame(GameTestHelper h){
        var level=h.getLevel();var p=CareerSupportIntegrationProbe.floor(h).add(5,0,4);
        var fisher=cat(h,p,CatOutfitType.FISHING);var foe=enemy(level,p.add(2,0,0));
        hit(new FishingRodProjectile(level,fisher,1),foe);
        h.assertTrue(foe.getDeltaMovement().x>0,"Default fishing rod pushes away");
        foe.invulnerableTime=0;foe.setDeltaMovement(Vec3.ZERO);equip(fisher,"cat_reel_hook");
        hit(new FishingRodProjectile(level,fisher,1),foe);
        h.assertTrue(foe.getDeltaMovement().x<0&&CatAccessories.value(fisher,"speed")==10,"Exclusive hook pulls instead and grants ten Speed");
        var honey=cat(h,p,CatOutfitType.HONEY);equip(honey,"cat_honey_stamp");foe.invulnerableTime=0;
        hit(new HoneyMissileProjectile(level,honey,1),foe);
        var patches=level.getEntitiesOfClass(CatHoneyPatch.class,foe.getBoundingBox().inflate(2));
        h.assertTrue(patches.size()==1,"Accepted honey hit creates one real ground patch");
        var visitor=enemy(level,foe.position().add(.3,0,.2));visitor.setDeltaMovement(Vec3.ZERO);
        var fire=cat(h,p,CatOutfitType.FIRE);CareerSupportIntegrationProbe.stat(fire,CatStat.LUCK,0);
        foe.invulnerableTime=0;foe.setHealth(1000);fire.getRandom().setSeed(18);fire(fire,foe);float ordinary=1000-foe.getHealth();
        foe.invulnerableTime=0;foe.setHealth(1000);fire.getRandom().setSeed(18);equip(fire,"cat_blue_flame_nozzle");fire(fire,foe);float blue=1000-foe.getHealth();
        h.assertTrue(ordinary>0&&Math.abs(blue/ordinary-1.5)<.02,"Blue flame multiplies real cone damage by 1.5");
        h.runAfterDelay(8,()->{
            h.assertTrue(visitor.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),"Walking into the honey patch slows a different creature");
            h.assertTrue(level.getBlockState(foe.blockPosition().below()).is(net.minecraft.world.level.block.Blocks.STONE),"Honey never replaces terrain");
            for(var e:List.of(fisher,honey,fire,foe,visitor,patches.get(0)))e.discard();
            h.succeed();System.out.println("PASS: fishing direction reversal, real honey decal/contact and 1.5x superheated cone damage");
        });
    }
    @GameTest(template="artillery_probe",batch="logistics_ammo26",timeoutTicks=30)
    public static void upgradedPotionsAndAmmunition(GameTestHelper h){
        var level=h.getLevel();var p=CareerSupportIntegrationProbe.floor(h).add(5,0,4);
        var sender=cat(h,p,CatOutfitType.TRANSPORT);equip(sender,"cat_concentrated_pouch");
        var recipient=cat(h,p.add(2,0,0),CatOutfitType.FIRE);
        recipient.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,80,0));
        var parcel=new LogisticsSupportProjectile(level,sender,recipient,MobEffects.MOVEMENT_SPEED,200);
        parcel.setPos(recipient.getBoundingBox().getCenter());level.addFreshEntity(parcel);parcel.tick();
        h.assertTrue(recipient.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier()==1,"Actual logistics projectile upgrades level I to II");
        var engineer=cat(h,p.add(0,0,4),CatOutfitType.ENGINEERING);
        for(int i=0;i<100;i++)h.assertTrue(CatArtilleryMunition.select(engineer)==CatArtilleryMunition.SMALL_COG,"No special munitions without the magazine");
        equip(engineer,"cat_mixed_magazine");
        h.assertTrue(CatAccessories.value(engineer,"speed")==-10,"Engineering accessory speed cost");
        var target=enemy(level,engineer.position().add(8,0,0));
        var cannon=new EngineeringCannon(LaoWuMod.ENGINEERING_CANNON.get(),level);
        cannon.setPos(engineer.position());level.addFreshEntity(cannon);engineer.startRiding(cannon,true);
        for(var expected:CatArtilleryMunition.values()){
            for(int seed=0;seed<10000;seed++){engineer.getRandom().setSeed(seed);if(CatArtilleryMunition.select(engineer)==expected){engineer.getRandom().setSeed(seed);break;}}
            h.assertTrue(cannon.fire(engineer,target),"Real cannon fires "+expected);
            var rounds=level.getEntitiesOfClass(EngineeringCogwheelProjectile.class,cannon.getBoundingBox().inflate(2));
            h.assertTrue(rounds.size()==1&&rounds.get(0).munition()==expected,"Real cannon spawned selected munition");
            h.assertTrue(Math.abs(rounds.get(0).getAccessoryDamage()-engineer.getAttributeValue(Attributes.ATTACK_DAMAGE)*expected.damageMultiplier())<.001,
                    "Real cannon applies correct per-munition damage before projectile hooks");
            rounds.get(0).discard();
        }
        cannon.release();target.discard();
        int[] counts=new int[3];
        for(int i=0;i<10000;i++)counts[CatArtilleryMunition.select(true,(i+.5)/10000).ordinal()]++;
        h.assertTrue(counts[0]==6000&&counts[1]==2000&&counts[2]==2000,"Sixty/twenty/twenty munition probabilities");
        h.assertTrue(CatArtilleryMunition.LARGE_COG.damageMultiplier()==1&&CatArtilleryMunition.SHAFT.damageMultiplier()==1.2F,"Same burst damage and twenty-percent stronger shaft");
        sender.discard();recipient.discard();engineer.discard();parcel.discard();h.succeed();
        System.out.println("PASS: real level-II logistics delivery, exclusive ammunition gating and exact distribution");
    }
    @GameTest(template="artillery_probe",batch="auras26",timeoutTicks=20)
    public static void nonstackingSupportAccessories(GameTestHelper h){
        var level=h.getLevel();var p=CareerSupportIntegrationProbe.floor(h).add(5,0,4);
        var medic=cat(h,p,CatOutfitType.MEDICAL);var second=cat(h,p.add(1,0,0),CatOutfitType.MEDICAL);
        var ally=cat(h,p.add(2,0,0),CatOutfitType.FIRE);ally.setHealth(ally.getMaxHealth()/2);
        equip(medic,"cat_guard_bandage");equip(second,"cat_guard_bandage");
        CatMedicalHealing.cast(medic,6,false);CatMedicalHealing.cast(second,6,false);
        CatMedicalHealing.treat(medic,ally,level.getGameTime()-25);CatMedicalHealing.treat(second,ally,level.getGameTime()-25);
        var source=level.damageSources().generic();
        h.assertTrue(Math.abs(CatAccessories.mitigateMovingDamage(ally,source,10)-8)<.001,"Two protective medics still reduce damage by only twenty percent");
        var patient=enemy(level,p.add(3,0,1));
        CatAccessoryAuras.protect(medic,patient);float before=patient.getHealth();patient.hurt(source,10);
        h.assertTrue(Math.abs(before-patient.getHealth()-8)<.001,"Real non-cat clinic patient incoming damage reduced by twenty percent");
        patient.discard();equip(medic,null);equip(second,null);
        h.assertTrue(CatAccessories.mitigateMovingDamage(ally,source,10)==10,"Unequipping immediately revokes protection");
        var musician=cat(h,p,CatOutfitType.MUSIC);var other=cat(h,p.add(1,0,2),CatOutfitType.MUSIC);
        equip(musician,"cat_rhythm_tambourine");equip(other,"cat_rhythm_tambourine");
        double speed=ally.getAttributeValue(Attributes.MOVEMENT_SPEED);
        int stat=CatAttributeEffects.effectiveValue(ally,CatStat.SPEED), gene=CatAttributeData.ensure(ally).current(CatStat.SPEED);
        CatMusicSupport.offer(ally,.2);CatMusicSupport.flush(level);
        int interval=CareerCatBehavior.careerAttackIntervalTicks(ally);
        CatAccessoryAuras.accelerate(musician,ally);CatAccessoryAuras.accelerate(other,ally);CatAccessoryAuras.tick(ally);
        h.assertTrue(CatAttributeEffects.effectiveValue(ally,CatStat.SPEED)==stat+20,"Two musicians grant twenty Speed points, not forty");
        double ratio=CatAttributeEffects.movementMultiplier(stat+20)/CatAttributeEffects.movementMultiplier(stat);
        h.assertTrue(Math.abs(ally.getAttributeValue(Attributes.MOVEMENT_SPEED)/speed-ratio)<.0001,"Speed points drive the real movement formula, with no old ten-percent multiplier");
        h.assertTrue(CareerCatBehavior.careerAttackIntervalTicks(ally)<interval,"Flat Speed also improves the real career attack interval");
        h.assertTrue(CatAccessories.state(ally).getInt("MusicSpeedBonus")==20
                &&CatAttributeData.ensure(ally).current(CatStat.SPEED)==gene,"Client state includes temporary Speed; raw genes are unchanged");
        h.assertTrue(CatAttributeEffects.effectiveValue(musician,CatStat.SPEED)==60,"Musician keeps its original suit +10, without receiving the target bonus");
        equip(musician,null);equip(other,null);CatAccessoryAuras.tick(ally);
        h.assertTrue(Math.abs(ally.getAttributeValue(Attributes.MOVEMENT_SPEED)-speed)<.0001
                &&CatAttributeEffects.effectiveValue(ally,CatStat.SPEED)==stat
                &&CatAccessories.state(ally).getInt("MusicSpeedBonus")==0,"Unequipping revokes Speed, movement and synchronized bonus");
        equip(musician,"cat_rhythm_tambourine");CatAccessoryAuras.accelerate(musician,ally);CatAccessoryAuras.tick(ally);
        h.assertTrue(CatAttributeEffects.effectiveValue(ally,CatStat.SPEED)==stat+20,"Re-equipping starts one fresh aura");
        musician.discard();CatAccessoryAuras.tick(ally);
        h.assertTrue(CatAttributeEffects.effectiveValue(ally,CatStat.SPEED)==stat,"Removed musician cannot leave a permanent stat buff");
        for(var c:List.of(medic,second,ally,musician,other))c.discard();
        h.succeed();System.out.println("PASS: medical protection and music +20 Speed are source-gated, nonstacking, formula-backed and removable");
    }
    @GameTest(template="artillery_probe",batch="music_speed29",timeoutTicks=65)
    public static void musicSpeedExpiry(GameTestHelper h){
        var level=h.getLevel();var p=CareerSupportIntegrationProbe.floor(h).add(5,0,4);
        var musician=cat(h,p,CatOutfitType.MUSIC);var ally=cat(h,p.add(2,0,0),CatOutfitType.FLIGHT);
        equip(musician,"cat_rhythm_tambourine");
        int before=CatAttributeEffects.effectiveValue(ally,CatStat.SPEED);
        CatMusicSupport.offer(ally,.25);CatMusicSupport.flush(level);
        CatAccessoryAuras.accelerate(musician,ally);CatAccessoryAuras.tick(ally);
        h.assertTrue(CatAttributeEffects.effectiveValue(ally,CatStat.SPEED)==before+20,"Initial target Speed aura");
        h.runAfterDelay(41,()->{
            CatAccessoryAuras.tick(ally);
            h.assertTrue(CatAttributeEffects.effectiveValue(ally,CatStat.SPEED)==before
                    &&CatAccessories.state(ally).getInt("MusicSpeedBonus")==0,"No fresh range offers: both bonus and client state expire");
            musician.discard();ally.discard();h.succeed();
            System.out.println("PASS: music Speed bonus expires after leaving performance coverage; no saved stat increase");
        });
    }
    @GameTest(template="artillery_probe",batch="smoke_heal26",timeoutTicks=70)
    public static void greenHealingSmoke(GameTestHelper h){
        var level=h.getLevel();var p=CareerSupportIntegrationProbe.floor(h).add(5,0,4);
        var agent=cat(h,p,CatOutfitType.AGENT);equip(agent,"cat_medic_smoke_canister");
        var ally=cat(h,p.add(1,0,0),CatOutfitType.FIRE);var hostile=cat(h,p.add(-1,0,0),CatOutfitType.FIRE);
        hostile.setOwnerUUID(UUID.randomUUID());
        var nbt=new net.minecraft.nbt.CompoundTag();hostile.addAdditionalSaveData(nbt);nbt.putByte("CollarColor",(byte)11);hostile.readAdditionalSaveData(nbt);
        agent.setHealth(agent.getMaxHealth()/2);ally.setHealth(ally.getMaxHealth()/2);hostile.setHealth(hostile.getMaxHealth()/2);
        float own=agent.getHealth(),friend=ally.getHealth(),bad=hostile.getHealth();
        CatAgentSmoke.burst(agent,p);CatHealingSmoke.create(agent,p); // Duplicate clouds must not multiply healing.
        h.runAfterDelay(40,()->{
            h.assertTrue(Math.abs((agent.getHealth()-own)-5)<.02&&Math.abs((ally.getHealth()-friend)-5)<.02,
                    "Two seconds at fixed Intelligence-50 rate heals five HP, not twice that");
            h.assertTrue(hostile.getHealth()==bad,"Enemy cats are never healed");
            h.assertTrue(CatMedicalHealing.glowing(agent)&&CatMedicalHealing.glowing(ally),"Self and allies receive medical visual state");
            agent.discard();ally.discard();hostile.discard();h.succeed();
            System.out.println("PASS: green medicine cloud, exact fixed 2.5 HP/s, self/allies, medical visuals and no stacked healing");
        });
    }
    @GameTest(template="artillery_probe",batch="diver_cleanse26",timeoutTicks=20)
    public static void friendlyWaterCleanse(GameTestHelper h){
        var level=h.getLevel();var p=CareerSupportIntegrationProbe.floor(h).add(5,0,4);
        var diver=cat(h,p,CatOutfitType.DIVING);var foe=enemy(level,p.add(3,0,0));
        var ally=cat(h,p.add(2,0,.15),CatOutfitType.FIRE);float health=ally.getHealth();
        ally.addEffect(new MobEffectInstance(MobEffects.POISON,200,0));ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,200,0));
        CatDivingAttack.spray(diver,foe);
        h.assertTrue(ally.hasEffect(MobEffects.POISON),"Without accessory the allied negative effect stays");
        foe.invulnerableTime=0;equip(diver,"cat_purifying_filter");CatDivingAttack.spray(diver,foe);
        h.assertTrue(!ally.hasEffect(MobEffects.POISON)&&ally.hasEffect(MobEffects.MOVEMENT_SPEED)&&ally.getHealth()==health,
                "Accessory removes harmful effects only and never damages the ally");
        diver.discard();foe.discard();ally.discard();h.succeed();
        System.out.println("PASS: actual diver cone allied harmful-only cleanse, preserved positive effect and zero friendly damage");
    }
    @GameTest(template="artillery_probe",batch="split26",timeoutTicks=30)
    public static void splitWithoutInventoryCopies(GameTestHelper h){
        var level=h.getLevel();var p=CareerSupportIntegrationProbe.floor(h).add(5,0,4);
        var parent=cat(h,p,CatOutfitType.COCKROACH);equip(parent,"cat_rebirth_ootheca");
        var inv=CatProfileData.openContainer(parent);inv.setItem(4,new ItemStack(Items.DIAMOND,3));inv.setChanged();
        UUID owner=parent.getOwnerUUID(),uuid=parent.getUUID();
        h.assertTrue(!CatCockroachSplit.trySplit(parent),"Living cats cannot split");
        parent.hurt(level.damageSources().genericKill(),Float.MAX_VALUE);
        h.runAfterDelay(2,()->{
            var children=level.getEntitiesOfClass(Cat.class,new AABB(p,p).inflate(3),c->c!=parent&&c.isAlive());
            h.assertTrue(children.size()==2,"Real death produces exactly two kittens");
            for(var child:children){
                h.assertTrue(child.isBaby()&&child.isTame()&&owner.equals(child.getOwnerUUID())&&!uuid.equals(child.getUUID())
                        &&CatClothesData.getOutfit(child)==CatOutfitType.COCKROACH,"Fresh baby UUID, owner and career");
                for(var stat:CatStat.values())h.assertTrue(CatAttributeData.ensure(child).current(stat)==25,"Half raw "+stat+", not temporary swarm or accessory bonuses");
                for(int slot=0;slot<13;slot++)h.assertTrue(CatProfileData.openContainer(child).getItem(slot).isEmpty(),"No inventory or accessory duplication");
            }
            var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(p,p).inflate(3));int diamonds=0,accessories=0,pancakes=0;
            for(var drop:drops){var stack=drop.getItem();if(stack.is(Items.DIAMOND))diamonds+=stack.getCount();
                if(stack.is(LaoWuMod.CAT_PANCAKE.get()))pancakes++;
                if(CatAccessories.isAccessory(stack,false))accessories+=stack.getCount();}
            h.assertTrue(diamonds==3&&accessories==1&&pancakes==0,"Original items drop once and no third revivable pancake remains");
            for(var child:children)child.discard();for(var drop:drops)drop.discard();parent.discard();
            h.succeed();System.out.println("PASS: genuine death splits two owned half-stat kittens, no cloned cargo/accessories, one set of original drops and no extra pancake");
        });
    }
}
