package cn.laowu.mod.test;
import cn.laowu.mod.*;
import cn.laowu.mod.accessory.CatAccessories;
import cn.laowu.mod.genetics.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class SplitTargetProbe {
    private static Cat parent(GameTestHelper h,Vec3 at,LivingEntity owner) {
        var cat=CareerSupportIntegrationProbe.cat(h.getLevel(),at,CatOutfitType.COCKROACH,true,owner);
        var inventory=CatProfileData.openContainer(cat);
        inventory.setItem(0,new ItemStack(BuiltInRegistries.ITEM.get(LaoWuMod.id("cat_rebirth_ootheca"))));
        inventory.setChanged();CatAccessories.equipmentChanged(cat);
        return cat;
    }
    private static Mob enemy(GameTestHelper h,Vec3 at) {
        var mob=EntityType.HUSK.create(h.getLevel());mob.setPos(at);mob.setNoAi(true);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        mob.getAttribute(Attributes.ARMOR).setBaseValue(0);mob.setHealth(1000);
        h.getLevel().addFreshEntity(mob);return mob;
    }
    @GameTest(template="artillery_probe",batch="split_target35",timeoutTicks=100)
    public static void kittensContinueRealCombat(GameTestHelper h) {
        var base=CareerSupportIntegrationProbe.floor(h).add(5,0,4);
        var owner=PortableWishProbe.player(h);owner.setPos(base.add(-3,0,0));h.getLevel().addNewPlayer(owner);
        var parent=parent(h,base,owner);var target=enemy(h,base.add(3,0,0));
        parent.setTarget(target);parent.hurt(h.getLevel().damageSources().genericKill(),Float.MAX_VALUE);
        var children=h.getLevel().getEntitiesOfClass(Cat.class,new AABB(base,base).inflate(2),c->c!=parent&&c.isAlive());
        h.assertTrue(children.size()==2,"Exactly two real split kittens");
        for(var child:children)h.assertTrue(child.getTarget()==target&&child.getLastHurtByMob()==target,
                "Both kittens inherit target and retaliation memory immediately");
        h.runAfterDelay(65,()->{
            h.assertTrue(target.getHealth()<1000,"Real AI attacks target after split");
            for(var child:children)h.assertTrue(child.isAlive()&&child.getTarget()==target
                    &&child.getPersistentData().getLong(CatCockroachCombat.NEXT_ATTACK)>0
                    &&child.position().distanceToSqr(base)>.1,"Each kitten pursues and performs a career attack");
            for(var child:children)child.discard();parent.discard();target.discard();owner.discard();
            System.out.println("PASS: two owned split kittens inherit hostility and each really pursues + attacks");h.succeed();
        });
    }
    @GameTest(template="artillery_probe",batch="split_target35",timeoutTicks=20)
    public static void lethalSourceFallbackAndFriendlyGuard(GameTestHelper h) {
        var base=CareerSupportIntegrationProbe.floor(h).add(5,0,4);
        var owner=PortableWishProbe.player(h);var target=enemy(h,base.add(2,0,0));
        for(boolean friendly:new boolean[]{false,true}) {
            var parent=parent(h,base,owner);
            parent.setTarget(friendly?owner:null);
            parent.hurt(friendly?h.getLevel().damageSources().playerAttack(owner):
                    h.getLevel().damageSources().mobAttack(target),Float.MAX_VALUE);
            var children=h.getLevel().getEntitiesOfClass(Cat.class,new AABB(base,base).inflate(2),c->c!=parent&&c.isAlive());
            h.assertTrue(children.size()==2,"Death source split succeeds");
            for(var child:children){
                h.assertTrue(friendly?child.getTarget()==null:child.getTarget()==target,
                        "Fallback uses actual lethal enemy; never inherits owner/friendly target");
                child.discard();
            }
            parent.discard();
        }
        target.discard();owner.discard();System.out.println("PASS: lethal enemy fallback, invalid owner target ignored");h.succeed();
    }
}
