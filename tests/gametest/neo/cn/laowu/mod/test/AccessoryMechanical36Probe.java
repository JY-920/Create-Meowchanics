package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.accessory.*;
import cn.laowu.mod.genetics.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.List;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class AccessoryMechanical36Probe {
    private static Cat cat(GameTestHelper h,Vec3 p,CatOutfitType role){
        var c=CareerSupportIntegrationProbe.cat(h.getLevel(),p,role,false);
        CareerSupportIntegrationProbe.stat(c,CatStat.LUCK,0);
        c.getOwner().setPos(p.add(-20,0,0));return c;
    }
    private static void equip(Cat c,String id){
        var inv=CatProfileData.openContainer(c);
        inv.setItem(0,id==null?ItemStack.EMPTY:new ItemStack(BuiltInRegistries.ITEM.get(LaoWuMod.id(id))));
        inv.setChanged();
    }
    private static Mob enemy(GameTestHelper h,Vec3 p){
        var e=EntityType.HUSK.create(h.getLevel());e.setNoAi(true);e.setNoGravity(true);e.setPos(p);
        e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000);e.setHealth(2000);
        e.getAttribute(Attributes.ARMOR).setBaseValue(0);h.getLevel().addFreshEntity(e);return e;
    }
    private static void close(GameTestHelper h,double actual,double expected,String message){
        h.assertTrue(Math.abs(actual-expected)<.03,message+": "+actual+" expected "+expected);
    }
    @GameTest(template="artillery_probe",batch="mechanical36_threat",timeoutTicks=30)
    public static void actualTargetChanges(GameTestHelper h){
        var p=CareerSupportIntegrationProbe.floor(h).add(5,0,5);
        var target=cat(h,p,CatOutfitType.NONE);var bell=cat(h,p.add(1,0,0),CatOutfitType.NONE);
        var e=enemy(h,p.add(0,0,3));
        equip(bell,"cat_taunt_bell");e.setTarget(target);
        h.assertTrue(e.getTarget()==bell,"Actual target event redirects to the taunt bell");
        equip(bell,null);equip(target,"cat_silent_bell");e.setTarget(target);
        h.assertTrue(e.getTarget()==bell,"Actual target event diverts attention from silent bell");
        e.setLastHurtByMob(target);e.setTarget(target);
        h.assertTrue(e.getTarget()==target,"Silent bell does not suppress direct retaliation");
        equip(target,null);e.setLastHurtByMob(null);e.setTarget(target);
        h.assertTrue(e.getTarget()==target,"Removing bells restores normal target selection");
        for(var entity:List.of(target,bell,e))entity.discard();
        System.out.println("PASS: actual taunt/silent target events, retaliation and removal");h.succeed();
    }
    @GameTest(template="artillery_probe",batch="mechanical36_motion",timeoutTicks=30)
    public static void actualKnockbackAndButter(GameTestHelper h){
        var p=CareerSupportIntegrationProbe.floor(h).add(5,0,5);
        var c=cat(h,p,CatOutfitType.NONE);var e=enemy(h,p.add(2,0,0));
        c.getAttribute(Attributes.KNOCKBACK_RESISTANCE).removeModifiers();
        c.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);
        equip(c,"cat_stability_anchor");c.setDeltaMovement(Vec3.ZERO);c.knockback(.5,1,0);
        close(h,c.getDeltaMovement().lengthSqr(),0,"Anchor blocks actual knockback");
        equip(c,null);c.knockback(.5,1,0);
        h.assertTrue(c.getDeltaMovement().horizontalDistanceSqr()>0,"Removing anchor restores knockback");
        e.setDeltaMovement(Vec3.ZERO);CatProjectileDamage.hurt(e,c.damageSources().mobAttack(c),4);
        close(h,e.getDeltaMovement().horizontalDistanceSqr(),0,"Ordinary cat projectile has no knockback");
        e.invulnerableTime=0;equip(c,"cat_impact_core");
        CatProjectileDamage.hurt(e,c.damageSources().mobAttack(c),4);
        h.assertTrue(e.getDeltaMovement().horizontalDistanceSqr()>0,"Impact core restores projectile knockback");
        equip(c,"cat_butter_cube");
        for(var attribute:List.of(Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS)){
            c.getAttribute(attribute).removeModifiers();c.getAttribute(attribute).setBaseValue(0);
        }
        c.setOrderedToSit(false);c.setInSittingPose(false);c.setDeltaMovement(.15,0,0);
        float before=c.getHealth();c.invulnerableTime=0;c.hurt(e.damageSources().mobAttack(e),4);
        close(h,before-c.getHealth(),3,"Butter mitigates actual moving damage exactly once");
        c.setDeltaMovement(Vec3.ZERO);before=c.getHealth();c.invulnerableTime=0;c.hurt(e.damageSources().mobAttack(e),4);
        close(h,before-c.getHealth(),4,"Stationary cat receives full damage");
        c.discard();e.discard();System.out.println("PASS: real anchor/impact knockback and moving butter health damage");h.succeed();
    }
    @GameTest(template="artillery_probe",batch="mechanical36_explosion",timeoutTicks=30)
    public static void actualDynamiteMultiplier(GameTestHelper h){
        var p=CareerSupportIntegrationProbe.floor(h).add(5,0,5);
        var c=cat(h,p,CatOutfitType.DYNAMITE);equip(c,"cat_blast_fuse");
        var e=enemy(h,p.add(2,0,0));float before=e.getHealth();
        double expected=ServerConfig.scaleDamage(c.getAttributeValue(Attributes.ATTACK_DAMAGE),10);
        try{
            var method=DynamiteCatLastStand.class.getDeclaredMethod("detonate",net.minecraft.server.level.ServerLevel.class,Cat.class);
            method.setAccessible(true);method.invoke(null,h.getLevel(),c);
        }catch(Exception error){throw new IllegalStateException(error);}
        close(h,before-e.getHealth(),expected,"Actual explosion uses ten-times damage");
        h.assertTrue(!c.isAlive(),"Final explosion still finishes the original cat");
        c.discard();e.discard();System.out.println("PASS: real dynamite detonation uses accessory multiplier and preserves final death");h.succeed();
    }
}
