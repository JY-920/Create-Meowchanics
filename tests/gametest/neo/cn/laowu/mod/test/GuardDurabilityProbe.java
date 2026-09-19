package cn.laowu.mod.test;
import cn.laowu.mod.*;
import cn.laowu.mod.accessory.*;
import cn.laowu.mod.api.CatAccessoryApi;
import cn.laowu.mod.genetics.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class GuardDurabilityProbe {
    private static Cat cat(GameTestHelper h,Vec3 p,String id) {
        var cat=CareerSupportIntegrationProbe.cat(h.getLevel(),p,CatOutfitType.NONE,false);
        CareerSupportIntegrationProbe.stat(cat,CatStat.LUCK,0);
        CareerSupportIntegrationProbe.stat(cat,CatStat.INTELLIGENCE,0);
        var inventory=CatProfileData.openContainer(cat);
        inventory.setItem(0,new ItemStack(BuiltInRegistries.ITEM.get(LaoWuMod.id(id))));inventory.setChanged();
        return cat;
    }
    private static Mob enemy(GameTestHelper h,Vec3 p) {
        var mob=EntityType.HUSK.create(h.getLevel());mob.setNoAi(true);mob.setNoGravity(true);mob.setPos(p);
        h.getLevel().addFreshEntity(mob);return mob;
    }
    private static float hit(Mob from,Cat cat,float amount) {
        cat.invulnerableTime=0;
        for(var stat:java.util.List.of(Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS)){
            cat.getAttribute(stat).removeModifiers();cat.getAttribute(stat).setBaseValue(0);
        }
        float before=cat.getHealth();cat.hurt(from.damageSources().mobAttack(from),amount);return before-cat.getHealth();
    }
    private static ItemStack worn(Cat cat) {return CatProfileData.openContainer(cat).getItem(0);}
    private static void close(GameTestHelper h,float a,float b,String why){h.assertTrue(Math.abs(a-b)<.025,why+": "+a+" vs "+b);}
    private static CompoundTag data(Cat cat){return cat.getPersistentData().getCompound(CatCommonAccessories.DATA);}
    private static final class GuardCancelFinal {
        LivingEntity victim;
        @net.neoforged.bus.api.SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
        public void cancel(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre event){
            if(event.getEntity()==victim)event.setNewDamage(0);
        }
    }
    @GameTest(template="artillery_probe",batch="guard36",timeoutTicks=30)
    public static void triggerCostsAndCancellation(GameTestHelper h){
        var p=CareerSupportIntegrationProbe.floor(h).add(5,0,4);var foe=enemy(h,p.add(2,0,0));
        for(String id:new String[]{"cat_cork_vest","cat_roly_poly"}){
            var cat=cat(h,p,id);var max=cat.getMaxHealth();boolean cap=id.equals("cat_roly_poly");
            h.assertTrue(worn(cat).getMaxDamage()==50&&worn(cat).getDamageValue()==0,"Old-compatible item default has 50 durability");
            cat.setHealth(cap?max:max*.5F);hit(foe,cat,max*.05F);
            h.assertTrue(worn(cat).getDamageValue()==0,"A non-triggering hit costs no durability");
            cat.setHealth(cap?max:max*.5F);
            var cancel=new GuardCancelFinal();cancel.victim=cat;net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(cancel);
            try{close(h,hit(foe,cat,max*(cap?.8F:.2F)),0,"Cancelled final damage");}
            finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}
            h.assertTrue(worn(cat).getDamageValue()==0&&data(cat).getLong(cap?"RolyReady":"ShieldReady")==0,"Cancellation cannot consume durability/cooldown");
            cat.setHealth(cap?max:max*.5F);hit(foe,cat,max*(cap?.8F:.2F));
            h.assertTrue(worn(cat).getDamageValue()==1&&data(cat).getLong(cap?"RolyReady":"ShieldReady")==h.getLevel().getGameTime()+300,"One accepted trigger costs one point and stores 15 seconds");
            cat.setHealth(cap?max:max*.5F);hit(foe,cat,max*(cap?.4F:.2F));
            h.assertTrue(worn(cat).getDamageValue()==1,"Cooldown/shield consumption never costs another point");
            var saved=new CompoundTag();cat.saveWithoutId(saved);var restored=EntityType.CAT.create(h.getLevel());restored.load(saved);
            h.assertTrue(worn(restored).getDamageValue()==1&&worn(restored).getMaxDamage()==50,"Actual cat inventory save/load preserves used durability");
            var handle=CatAccessoryApi.accessory(cat,"laowu:"+id);
            h.assertTrue(handle.getDurability()==49&&handle.getMaxDurability()==50,"Stable script handle reads live durability");
            h.assertTrue(!handle.damageDurability(1)&&worn(cat).getDamageValue()==2,"Script helper updates the live inventory, not a discarded copy");
            cat.discard();
        }
        foe.discard();System.out.println("PASS: guard durable defaults, actual/cancelled/non-triggering hits, cooldown and saved/script durability");h.succeed();
    }
    @GameTest(template="artillery_probe",batch="guard36",timeoutTicks=140)
    public static void fiftyUsesAndFinalShield(GameTestHelper h){
        var p=CareerSupportIntegrationProbe.floor(h).add(5,0,4);var foe=enemy(h,p.add(2,0,0));
        var vest=cat(h,p,"cat_cork_vest");var cap=cat(h,p.add(0,0,2),"cat_roly_poly");
        for(int i=1;i<=50;i++){
            // Accelerate only the fixture's stored deadlines; real 300-tick expiry is covered separately.
            data(vest).putLong("ShieldReady",0);data(vest).putLong("ShieldUntil",0);
            data(cap).putLong("RolyReady",0);
            vest.setHealth(vest.getMaxHealth()*.5F);cap.setHealth(cap.getMaxHealth());
            hit(foe,vest,vest.getMaxHealth()*.2F);
            close(h,hit(foe,cap,cap.getMaxHealth()*.8F),cap.getMaxHealth()*.3F,"Every accepted cap including the final use");
            if(i<50)h.assertTrue(worn(vest).getDamageValue()==i&&worn(cap).getDamageValue()==i,"Exactly one point per trigger "+i);
        }
        h.assertTrue(worn(vest).isEmpty()&&worn(cap).isEmpty(),"Both items break exactly on trigger 50");
        close(h,CatCommonAccessories.shield(vest),vest.getMaxHealth()*.15F,"Last-use shield survives item destruction");
        close(h,hit(foe,vest,vest.getMaxHealth()*.1F),0,"Final shield actually protects");
        cap.setHealth(cap.getMaxHealth());
        close(h,hit(foe,cap,cap.getMaxHealth()*.8F),cap.getMaxHealth()*.8F,"No fifty-first cap");
        h.runAfterDelay(125,()->{
            h.assertTrue(CatCommonAccessories.shield(vest)==0,"Final shield expires normally");
            vest.discard();cap.discard();foe.discard();
            System.out.println("PASS: 50 real guard triggers each, exact break, final protection and six-second expiry");h.succeed();
        });
    }
    @GameTest(template="artillery_probe",batch="guard36_cooldown",timeoutTicks=325)
    public static void exactFifteenSecondCooldown(GameTestHelper h){
        var p=CareerSupportIntegrationProbe.floor(h).add(5,0,4);var foe=enemy(h,p.add(2,0,0));
        var vest=cat(h,p,"cat_cork_vest");var cap=cat(h,p.add(0,0,2),"cat_roly_poly");
        vest.setHealth(vest.getMaxHealth()*.5F);hit(foe,vest,vest.getMaxHealth()*.2F);hit(foe,cap,cap.getMaxHealth()*.8F);
        h.runAfterDelay(299,()->{
            vest.setHealth(vest.getMaxHealth()*.5F);cap.setHealth(cap.getMaxHealth());
            hit(foe,vest,vest.getMaxHealth()*.2F);hit(foe,cap,cap.getMaxHealth()*.4F);
            h.assertTrue(worn(vest).getDamageValue()==1&&worn(cap).getDamageValue()==1,"Still cooling down at tick 299");
        });
        h.runAfterDelay(300,()->{
            vest.setHealth(vest.getMaxHealth()*.5F);cap.setHealth(cap.getMaxHealth());
            hit(foe,vest,vest.getMaxHealth()*.2F);hit(foe,cap,cap.getMaxHealth()*.8F);
            h.assertTrue(worn(vest).getDamageValue()==2&&worn(cap).getDamageValue()==2,"Both ready exactly at tick 300");
            vest.discard();cap.discard();foe.discard();
            System.out.println("PASS: both guard cooldowns expire at exactly 300 server ticks");h.succeed();
        });
    }
}
