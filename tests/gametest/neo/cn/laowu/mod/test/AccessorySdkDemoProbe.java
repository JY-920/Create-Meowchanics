package cn.laowu.mod.test;
import cn.laowu.mod.*;
import cn.laowu.mod.accessory.*;
import cn.laowu.mod.genetics.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class AccessorySdkDemoProbe {
    @GameTest(template="artillery_probe",batch="sdk37_demo",timeoutTicks=220)
    public static void newItemRegistrationAndMechanic(GameTestHelper h){
        if(!Boolean.getBoolean("laowu.sdk_demo")){h.succeed();return;}
        var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kubejs","example_cat_charm");
        var item=BuiltInRegistries.ITEM.get(id);
        h.assertTrue(item!=net.minecraft.world.item.Items.AIR,"Real startup script registered the new item");
        var p=CareerSupportIntegrationProbe.floor(h).add(4,0,4);
        var cat=CareerSupportIntegrationProbe.cat(h.getLevel(),p,CatOutfitType.NONE,false);
        CareerSupportIntegrationProbe.stat(cat,CatStat.LUCK,0);
        int before=CatAttributeEffects.effectiveValue(cat,CatStat.SPEED);
        var inv=CatProfileData.openContainer(cat);inv.setItem(0,new ItemStack(item));inv.setChanged();
        h.assertTrue(CatAttributeEffects.effectiveValue(cat,CatStat.SPEED)==before+10,"Real generated definition grants Speed +10");
        var def=CatAccessoryRegistry.find(inv.getItem(0),false);
        h.assertTrue(def.script().equals("examplepack:heal_on_hit_v1"),"New data and event implementation agree");
        var enemy=EntityType.HUSK.create(h.getLevel());enemy.setNoAi(true);enemy.setPos(p.add(2,0,0));
        enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);enemy.setHealth(1000);h.getLevel().addFreshEntity(enemy);
        cat.setHealth(cat.getMaxHealth()-10);float health=cat.getHealth();
        enemy.hurt(cat.damageSources().mobAttack(cat),1);
        h.assertTrue(Math.abs(cat.getHealth()-health-2)<.01,"Actual acceptedAttack heals once");
        enemy.invulnerableTime=0;enemy.hurt(cat.damageSources().mobAttack(cat),1);
        h.assertTrue(Math.abs(cat.getHealth()-health-2)<.01,"Same-tick second hit respects cooldown");
        h.runAfterDelay(200,()->{
            enemy.invulnerableTime=0;enemy.hurt(cat.damageSources().mobAttack(cat),1);
            h.assertTrue(Math.abs(cat.getHealth()-health-4)<.01,"Exact 200-tick cooldown expires");
            inv.setItem(0,ItemStack.EMPTY);inv.setChanged();
            h.assertTrue(CatAttributeEffects.effectiveValue(cat,CatStat.SPEED)==before,"Unequip removes data bonus");
            enemy.invulnerableTime=0;enemy.hurt(cat.damageSources().mobAttack(cat),1);
            h.assertTrue(Math.abs(cat.getHealth()-health-4)<.01,"Unequip removes event effect");
            cat.discard();enemy.discard();System.out.println("PASS: SDK startup registration, generated definition, actual heal-on-hit, cooldown and removal");h.succeed();
        });
    }
}
