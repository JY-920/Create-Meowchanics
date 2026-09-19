package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.accessory.*;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class GeneralAccessoryProbe {
    private static Cat cat(GameTestHelper h, Vec3 p) {
        Cat cat = CareerSupportIntegrationProbe.cat(h.getLevel(),p,CatOutfitType.NONE,false);
        CareerSupportIntegrationProbe.stat(cat,CatStat.INTELLIGENCE,0);
        CareerSupportIntegrationProbe.stat(cat,CatStat.LUCK,0);
        return cat;
    }
    private static ItemStack item(String name) { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("laowu",name))); }
    private static void equip(Cat cat, String... names) {
        var inv=CatProfileData.openContainer(cat);
        for(int i=0;i<4;i++)inv.setItem(i,i<names.length?item(names[i]):ItemStack.EMPTY);
        inv.setChanged();CatAccessories.equipmentChanged(cat);
    }
    private static Mob enemy(ServerLevel level,Vec3 p){
        var mob=EntityType.HUSK.create(level);mob.setNoAi(true);mob.setNoGravity(true);mob.setPos(p);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);mob.setHealth(1000);
        mob.getAttribute(Attributes.ARMOR).setBaseValue(0);level.addFreshEntity(mob);return mob;
    }
    private static float hit(LivingEntity from, LivingEntity to, float amount) {
        to.invulnerableTime=0;float before=to.getHealth();
        to.hurt(from.damageSources().mobAttack(from),amount);return before-to.getHealth();
    }
    private static float unarmoredHit(LivingEntity from,Cat cat,float amount) {
        for(var attribute:List.of(Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS)){
            cat.getAttribute(attribute).removeModifiers();cat.getAttribute(attribute).setBaseValue(0);
        }
        return hit(from,cat,amount);
    }
    private static float criticalHit(Cat cat,LivingEntity victim,float amount) {
        double chance=CatAttributeEffects.criticalChance(CatAttributeEffects.effectiveValue(cat,CatStat.LUCK));
        for(int seed=0;seed<10000;seed++){
            cat.getRandom().setSeed(seed);
            if(cat.getRandom().nextDouble()<chance){cat.getRandom().setSeed(seed);return hit(cat,victim,amount);}
        }
        throw new AssertionError("No deterministic critical seed");
    }
    private static void close(GameTestHelper h,float actual,float expected,String message) {
        h.assertTrue(Math.abs(actual-expected)<.025,message+": expected "+expected+", got "+actual);
    }
    private static void finish(GameTestHelper h,String message,Entity... entities){
        for(var entity:entities)entity.discard();h.succeed();System.out.println("PASS: general accessories "+message);
    }
    @GameTest(template="artillery_probe",batch="general_combo27",timeoutTicks=170)
    public static void comboAndOpener(GameTestHelper h){
        var p=CareerSupportIntegrationProbe.floor(h).add(4,0,4);var level=h.getLevel();
        var cat=cat(h,p);var enemy=enemy(level,p.add(2,0,0));equip(cat,"cat_chew_bone");
        close(h,hit(cat,enemy,20),20,"First hit starts combo");
        close(h,hit(cat,enemy,20),20.8F,"Same-tick second hit cannot gain another layer");
        for(int i=1;i<=5;i++){final int layer=i;h.runAfterDelay(i*11,()->close(h,hit(cat,enemy,20),20*(1+.04F*layer),"Combo layer "+layer));}
        h.runAfterDelay(66,()->close(h,hit(cat,enemy,20),24,"Five-layer cap"));
        h.runAfterDelay(149,()->{
            close(h,hit(cat,enemy,20),20,"Four seconds without a hit clears combo");
            var other=enemy(level,p.add(2,0,2));close(h,hit(cat,other,20),20,"Switch target resets combo");
            equip(cat,"cat_mouse_plush");enemy.setHealth(1000);
            close(h,hit(cat,enemy,20),27,"High-health opener gets 35 percent");
            close(h,hit(cat,enemy,20),27,"Every high-health hit gets the bonus without a cooldown");
            var data=cat.getPersistentData().getCompound(CatCommonAccessories.DATA);
            h.assertTrue(!data.contains("OpenerReady"),"New hits do not create a plush cooldown");
            data.putLong("OpenerReady",level.getGameTime()+10000); // Old saves must not retain the removed restriction.
            other.setHealth(1000);close(h,hit(cat,other,20),27,"Another high-health target is eligible in the same tick");
            equip(cat);equip(cat,"cat_mouse_plush");enemy.setHealth(1000);
            close(h,hit(cat,enemy,20),27,"Legacy saved cooldown and re-equipping do not suppress the bonus");
            enemy.setHealth(900);close(h,hit(cat,enemy,20),20,"Exactly 90 percent health is not above the threshold");
            enemy.setHealth(899);close(h,hit(cat,enemy,20),20,"Lower-health targets do not receive the bonus");
            h.assertTrue(item("cat_chew_bone").getRarity()==Rarity.EPIC
                    &&CatAccessoryRarity.requirements(item("cat_chew_bone"))==4,"Chew Bone is native Epic with four Wish conditions");
            finish(h,"real combo throttle, cap, timeout, target switch; no-cooldown opener and legacy save compatibility; Epic bone",cat,enemy,other);
        });
    }
    @GameTest(template="artillery_probe",batch="general_mint27",timeoutTicks=95)
    public static void criticalHaste(GameTestHelper h){
        var p=CareerSupportIntegrationProbe.floor(h).add(4,0,4);
        var cat=cat(h,p);var enemy=enemy(h.getLevel(),p.add(2,0,0));equip(cat,"cat_catnip_pouch");
        CareerSupportIntegrationProbe.stat(cat,CatStat.LUCK,999);
        criticalHit(cat,enemy,10);
        close(h,(float)CatCommonAccessories.haste(cat),.15F,"Accepted critical grants haste");
        h.assertTrue(CatMusicSupport.attackInterval(cat,100)==87,"15 percent attack frequency routed to real controller");
        h.assertTrue(CatMusicSupport.bonus(cat)==0,"Catnip is not falsely presented as a music aura");
        long until=cat.getPersistentData().getCompound(CatCommonAccessories.DATA).getLong("MintUntil");
        criticalHit(cat,enemy,10);
        h.assertTrue(cat.getPersistentData().getCompound(CatCommonAccessories.DATA).getLong("MintUntil")==until,"Repeated critical cannot refresh duration");
        h.runAfterDelay(82,()->{
            h.assertTrue(CatCommonAccessories.haste(cat)==0,"Four-second haste expires");
            criticalHit(cat,enemy,10);h.assertTrue(CatCommonAccessories.haste(cat)==0,"Eight-second proc cooldown remains");
            finish(h,"real guaranteed critical, haste controller, fixed duration and cooldown",cat,enemy);
        });
    }
    @GameTest(template="artillery_probe",batch="general_guard27",timeoutTicks=145)
    public static void shieldAndRoly(GameTestHelper h){
        var p=CareerSupportIntegrationProbe.floor(h).add(4,0,4);var level=h.getLevel();
        var cat=cat(h,p);var enemy=enemy(level,p.add(2,0,0));
        cat.getAttribute(Attributes.ARMOR).setBaseValue(0);equip(cat,"cat_cork_vest");
        float max=cat.getMaxHealth();cat.setHealth(max*.5F);
        close(h,unarmoredHit(enemy,cat,max*.2F),max*.2F,"Threshold crossing still takes damage");
        close(h,CatCommonAccessories.shield(cat),max*.15F,"Independent 15 percent shield granted");
        cat.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,200,0));
        float vanilla=cat.getAbsorptionAmount();
        close(h,unarmoredHit(enemy,cat,vanilla+max*.1F),0,"Vanilla absorption followed by accessory shield");
        close(h,cat.getAbsorptionAmount(),0,"Existing absorption used normally");
        close(h,CatCommonAccessories.shield(cat),max*.05F,"Only remainder consumed from cork shield");
        close(h,CatAccessoryHooks.withoutEvents(()->unarmoredHit(enemy,cat,max*.01F)),0,"Script/extra attacks still meet the shield");
        close(h,CatCommonAccessories.shield(cat),max*.04F,"Auxiliary damage consumes shield normally");
        var inv=CatProfileData.openContainer(cat);
        h.assertTrue(!CatAccessories.mayEquip(inv,1,item("cat_roly_poly"),false),"Mutual exclusion enforced in real inventory");
        long shieldReady=cat.getPersistentData().getCompound(CatCommonAccessories.DATA).getLong("ShieldReady");
        equip(cat);h.assertTrue(CatCommonAccessories.shield(cat)==0,"Removing vest clears shield");
        equip(cat,"cat_roly_poly");cat.setHealth(max);cat.removeEffect(MobEffects.ABSORPTION);
        close(h,unarmoredHit(enemy,cat,max*.8F),max*.3F,"Large health-bound hit capped");
        long rolyReady=cat.getPersistentData().getCompound(CatCommonAccessories.DATA).getLong("RolyReady");
        h.assertTrue(rolyReady==level.getGameTime()+300&&shieldReady>level.getGameTime(),"Both cooldowns stored independently");
        var saved=new net.minecraft.nbt.CompoundTag();cat.saveWithoutId(saved);
        var restored=EntityType.CAT.create(level);restored.load(saved);
        h.assertTrue(restored.getPersistentData().getCompound(CatCommonAccessories.DATA).getLong("RolyReady")==rolyReady,"Cooldown survives real entity NBT save/load");
        close(h,unarmoredHit(enemy,cat,max*.4F),max*.4F,"Second large attack is not capped during cooldown");
        equip(cat,"cat_cork_vest");cat.setHealth(max*.5F);unarmoredHit(enemy,cat,max*.2F);
        h.assertTrue(CatCommonAccessories.shield(cat)==0,"Vest cannot retrigger during fifteen-second cooldown");
        var expiryCat=cat(h,p.add(0,0,5));equip(expiryCat,"cat_cork_vest");
        float expiryMax=expiryCat.getMaxHealth();expiryCat.setHealth(expiryMax*.5F);
        unarmoredHit(enemy,expiryCat,expiryMax*.2F);
        h.assertTrue(CatCommonAccessories.shield(expiryCat)>0,"Separate expiry shield activated");
        var voidCat=cat(h,p.add(0,0,3));equip(voidCat,"cat_roly_poly");
        float hp=voidCat.getHealth();voidCat.hurt(level.damageSources().fellOutOfWorld(),hp*.4F);
        close(h,hp-voidCat.getHealth(),hp*.4F,"Void bypasses toy protection");
        h.runAfterDelay(125,()->{h.assertTrue(CatCommonAccessories.shield(expiryCat)==0,"Unused shield expires after six seconds");finish(h,"actual threshold shield, absorption, cap, mutual exclusion, saved cooldown, expiry and void bypass",cat,enemy,voidCat,expiryCat);});
    }
    @GameTest(template="artillery_probe",batch="general_reflect27",timeoutTicks=35)
    public static void reflectedActualDamage(GameTestHelper h){
        var p=CareerSupportIntegrationProbe.floor(h).add(4,0,4);var cat=cat(h,p);var foe=enemy(h.getLevel(),p.add(2,0,0));
        equip(cat,"cat_spiked_collar");cat.getAttribute(Attributes.ARMOR).setBaseValue(10);
        float before=foe.getHealth(),taken=hit(foe,cat,4);
        close(h,before-foe.getHealth(),taken*.5F,"Reflect uses post-armor actual health loss");
        before=foe.getHealth();hit(foe,cat,2);close(h,before-foe.getHealth(),0,"One-second reflection throttle");
        h.runAfterDelay(22,()->{
            foe.invulnerableTime=0;float old=foe.getHealth();float damage=hit(foe,cat,2);
            close(h,old-foe.getHealth(),damage*.5F,"Reflection ready again after one second");
            cat.invulnerableTime=0;old=foe.getHealth();cat.hurt(foe.damageSources().thorns(foe),1);
            close(h,old-foe.getHealth(),0,"Thorns cannot reflect again");
            CatClothesData.equip(cat,CatOutfitType.MEDICAL);
            cat.getPersistentData().getCompound(CatCommonAccessories.DATA).putLong("ReflectReady",0);
            foe.invulnerableTime=0;old=foe.getHealth();damage=hit(foe,cat,1);
            close(h,old-foe.getHealth(),damage*.5F,"Support career can passively reflect without acquiring an attack AI");
            finish(h,"melee reflected actual damage, cooldown, anti-chain guards and support passive defence",cat,foe);
        });
    }
    @GameTest(template="artillery_probe",batch="general_heal27",timeoutTicks=325)
    public static void restAndReceivedHealing(GameTestHelper h){
        var p=CareerSupportIntegrationProbe.floor(h).add(4,0,4);var cat=cat(h,p);
        equip(cat,"cat_old_food_bowl","cat_warm_scarf");cat.setHealth(cat.getMaxHealth()*.25F);
        cat.setInvulnerable(true); // Measure healing in isolation from actors left in adjacent combat fixtures.
        float before=cat.getHealth();cat.heal(1);close(h,cat.getHealth()-before,1.2F,"Actual heal event amplified once");
        before=cat.getHealth();CatMedicalHealing.offerHealing(cat,4);CatMedicalHealing.offerHealing(cat,2.5F);CatMedicalHealing.flush(h.getLevel());
        close(h,cat.getHealth()-before,4*CatSupportRules.HEAL_TICKS/20F*1.2F,"Strongest group heal merged BEFORE scarf amplification");
        float baseline=cat.getHealth();
        h.runAfterDelay(195,()->{h.assertTrue(cat.isAlive(),"Healing fixture alive: removed="+cat.isRemoved()+", cause="+cat.getLastDamageSource());close(h,cat.getHealth(),baseline,"No food-bowl healing during ten-second rest delay");});
        h.runAfterDelay(245,()->{
            close(h,cat.getHealth()-baseline,2.4F,"Two food-bowl pulses each amplified once");
            var foe=enemy(h.getLevel(),p.add(2,0,0));cat.setTarget(foe);cat.setInvulnerable(false);hit(foe,cat,1);cat.setInvulnerable(true);float combatHealth=cat.getHealth();
            h.runAfterDelay(60,()->{close(h,cat.getHealth(),combatHealth,"Combat stops recovery immediately");finish(h,"healing event, nonstacking medic offers, rest delay and combat stop",cat,foe);});
        });
    }
    @GameTest(template="artillery_probe",batch="general_owner27",timeoutTicks=30)
    public static void ownerBoundaryAndGenes(GameTestHelper h){
        var p=CareerSupportIntegrationProbe.floor(h).add(4,0,4);var cat=cat(h,p);
        int genes=CatAttributeEffects.effectiveValue(cat,CatStat.ATTACK);equip(cat,"cat_tracking_tag");
        var owner=cat.getOwner();owner.setPos(p.add(4,0,0));CatCommonAccessories.tick(cat);
        h.assertTrue(CatCommonAccessories.ownerBonus(cat)==30&&CatAttributeEffects.effectiveValue(cat,CatStat.ATTACK)==genes+30,"Inclusive four-block boundary grants Attack STAT");
        h.assertTrue(CatAccessories.state(cat).getInt("OwnerAttackBonus")==30,"Dynamic value included in multiplayer/GUI state");
        owner.setPos(p.add(4.01,0,0));CatCommonAccessories.tick(cat);
        h.assertTrue(CatAccessories.statBonus(cat,CatStat.ATTACK)==0&&CatAttributeEffects.effectiveValue(cat,CatStat.ATTACK)==genes,"Outside range immediately removes bonus without gene mutation");
        owner.setPos(p);equip(cat);
        h.assertTrue(CatCommonAccessories.ownerBonus(cat)==0,"Unequipping removes bonus even at owner");
        finish(h,"owner four-block boundary, live stat refresh, packet state and unchanged genes",cat);
    }
    @GameTest(template="artillery_probe",batch="general_pickup27",timeoutTicks=65)
    public static void samplePickupAndMagnet(GameTestHelper h){
        var p=CareerSupportIntegrationProbe.floor(h).add(4,0,4);var level=h.getLevel();var cat=cat(h,p);
        equip(cat,"cat_sorting_pouch","cat_loot_magnet");
        var inv=CatProfileData.openContainer(cat);inv.setItem(4,new ItemStack(Items.DIAMOND));inv.setChanged();
        var yes=new ItemEntity(level,p.x+2.7,p.y+.1,p.z,new ItemStack(Items.DIAMOND,7));yes.setNoPickUpDelay();level.addFreshEntity(yes);
        var no=new ItemEntity(level,p.x+.3,p.y+.1,p.z,new ItemStack(Items.EMERALD,3));no.setNoPickUpDelay();no.setDeltaMovement(Vec3.ZERO);level.addFreshEntity(no);
        h.runAfterDelay(30,()->{
            var cargo=CatProfileData.openContainer(cat);int count=0;
            for(int i=4;i<13;i++)if(cargo.getItem(i).is(Items.DIAMOND))count+=cargo.getItem(i).getCount();
            h.assertTrue(count==8&&!yes.isAlive()&&no.isAlive(),"Magnet only attracts and stores sampled item types");
            h.assertTrue(cargo.getContainerSize()==13,"Still four equipment plus nine cargo slots");
            equip(cat,"cat_sorting_pouch");cargo=CatProfileData.openContainer(cat);cargo.setItem(5,new ItemStack(Items.EMERALD));cargo.setChanged();
            no.setPos(cat.position().add(.3,.1,0));no.setDeltaMovement(Vec3.ZERO);
        });
        h.runAfterDelay(48,()->{
            h.assertTrue(!no.isAlive(),"Pouch alone picks up matching nearby items after adding sample; distance="+cat.distanceTo(no)+", effect="+CatAccessories.value(cat,"sample_pickup")+", sample="+CatProfileData.openContainer(cat).getItem(5));
            finish(h,"actual magnet filtering, close pickup, sample requirement and unchanged inventory size",cat);
        });
    }
    private static final class ScriptedNestedHit {
        Cat attacker; LivingEntity victim; boolean fired;
        @net.neoforged.bus.api.SubscribeEvent
        public void nested(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
            if(!fired&&event.getEntity()==victim&&event.getSource().getEntity()==attacker){
                fired=true;
                CatAccessoryHooks.withoutEvents(()->hit(attacker,victim,2));
                victim.invulnerableTime=0; // Neo Incoming precedes vanilla i-frame checks; isolate nested bookkeeping from that loader difference.
            }
        }
    }
    private static final class CancelFinal {
        LivingEntity victim;
        @net.neoforged.bus.api.SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
        public void cancel(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
            if(event.getEntity()==victim)event.setCanceled(true);
        }
    }
    @GameTest(template="artillery_probe",batch="general_cancel27",timeoutTicks=30)
    public static void canceledDamageDoesNotProc(GameTestHelper h){
        var p=CareerSupportIntegrationProbe.floor(h).add(4,0,4);var cat=cat(h,p);var foe=enemy(h.getLevel(),p.add(2,0,0));
        equip(cat,"cat_mouse_plush","cat_catnip_pouch","cat_chew_bone");CareerSupportIntegrationProbe.stat(cat,CatStat.LUCK,999);
        var listener=new CancelFinal();listener.victim=foe;net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(listener);
        try { close(h,hit(cat,foe,10),0,"Test listener cancels real final damage"); }
        finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener); }
        var data=cat.getPersistentData().getCompound(CatCommonAccessories.DATA);
        h.assertTrue(data.getLong("OpenerReady")==0&&data.getLong("MintUntil")==0,"Canceled hit consumes no opener or mint proc");
        close(h,hit(cat,foe,10),13.5F,"Next accepted hit receives opener, with no phantom combo stack");
        data.putLong("OpenerReady",0);data.putLong("MintReady",0);data.putLong("MintUntil",0);
        foe.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1);foe.setHealth(1);
        criticalHit(cat,foe,20);
        h.assertTrue(!foe.isAlive()&&data.getLong("OpenerReady")==0
                &&data.getLong("MintUntil")>h.getLevel().getGameTime(),"Killing hits grant critical haste without introducing a plush cooldown");
        var nestedVictim=enemy(h.getLevel(),p.add(2,0,2));
        equip(cat);equip(cat,"cat_chew_bone","cat_mouse_plush");data.putLong("OpenerReady",0);
        var nested=new ScriptedNestedHit();nested.attacker=cat;nested.victim=nestedVictim;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(nested);
        try { close(h,hit(cat,nestedVictim,10),15.5F,"Outer opener and two-point non-proccing nested script hit both land"); }
        finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(nested); }
        h.assertTrue(data.getLong("OpenerReady")==0,"Nested hits do not create a plush cooldown");
        close(h,hit(cat,nestedVictim,10),14.04F,"Exactly one combo layer and another high-health bonus after nested damage");
        finish(h,"cancellation, killing criticals, nested script damage, no plush cooldown and no auxiliary combo chains",cat,foe,nestedVictim);
    }
}
