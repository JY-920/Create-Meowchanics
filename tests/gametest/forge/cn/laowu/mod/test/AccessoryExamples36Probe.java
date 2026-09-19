package cn.laowu.mod.test;
import cn.laowu.mod.*;
import cn.laowu.mod.accessory.*;
import cn.laowu.mod.genetics.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import java.util.*;
@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class AccessoryExamples36Probe {
    private static final Map<String,String> SCRIPTED=Map.of(
        "cat_fire_charm","fire_immune","cat_chew_bone","damage_combo","cat_mouse_plush","opening_damage",
        "cat_ace_feather","pilot_dodge_per_speed","cat_butter_cube","moving_damage_reduction",
        "cat_warm_scarf","healing_received","cat_spiked_collar","melee_reflect");
    @GameTest(template="artillery_probe",batch="examples36_catalog",timeoutTicks=30)
    public static void allThirtySixDefinitionsAndStats(GameTestHelper h){
        boolean scripts=Boolean.getBoolean("laowu.examples36");
        h.assertTrue(cn.laowu.mod.api.CatAccessoryApi.supportsApi(1)
                &&cn.laowu.mod.api.CatAccessoryApi.supportsApi(2)
                &&cn.laowu.mod.api.CatAccessoryApi.supportsApi(3)
                &&!cn.laowu.mod.api.CatAccessoryApi.supportsApi(0)
                &&!cn.laowu.mod.api.CatAccessoryApi.supportsApi(4),"Truthful v1-v3 compatibility declaration");
        var p=CareerSupportIntegrationProbe.floor(h).add(4,0,4);
        int count=0;
        for(var original:CatAccessoryItems.DEFAULTS.values()){
            String path=original.item().substring(6);
            var stack=new ItemStack(BuiltInRegistries.ITEM.get(LaoWuMod.id(path)));
            var def=CatAccessoryRegistry.find(stack,false);
            h.assertTrue(def!=null&&def.script().equals(scripts?"laowu:examples36":""),"Expected profile actually loaded: "+path);
            h.assertTrue(def.requiredOutfit().equals(original.requiredOutfit())&&def.exclusiveGroup().equals(original.exclusiveGroup()),"Original outfit/mutual exclusion retained "+path);
            for(var entry:original.effects().entrySet()){
                boolean delegated=scripts&&entry.getKey().equals(SCRIPTED.get(path));
                h.assertTrue(def.value(entry.getKey())==(delegated?0:entry.getValue()),"Exactly one implementation for "+path+" / "+entry.getKey());
            }
            var outfit=original.requiredOutfit().equals("any")?CatOutfitType.NONE:CatOutfitType.byId(original.requiredOutfit());
            var cat=CareerSupportIntegrationProbe.cat(h.getLevel(),p,outfit,false);
            cat.getOwner().setPos(p.add(-20,0,0));
            int[] before=Arrays.stream(CatStat.values()).mapToInt(stat->CatAttributeEffects.effectiveValue(cat,stat)).toArray();
            var inventory=CatProfileData.openContainer(cat);
            h.assertTrue(CatAccessories.mayEquip(inventory,0,stack,false),"Every definition remains equippable "+path);
            inventory.setItem(0,stack);inventory.setChanged();
            for(var stat:CatStat.values())h.assertTrue(CatAttributeEffects.effectiveValue(cat,stat)==before[stat.ordinal()]+(int)original.value(stat.serializedName()),
                    "All six effective stats and costs match for "+path+" / "+stat);
            inventory.setItem(0,ItemStack.EMPTY);
            for(var stat:CatStat.values())h.assertTrue(CatAttributeEffects.effectiveValue(cat,stat)==before[stat.ordinal()],"Removal restores "+path+" / "+stat);
            cat.discard();count++;
        }
        h.assertTrue(count==36,"All 36 actual items covered");
        System.out.println("PASS: "+(scripts?"KubeJS replacement":"native")+" 36/36 definitions, outfit gates, exclusion and all six stat effects with no duplication");h.succeed();
    }
    @GameTest(template="artillery_probe",batch="examples36_fire",timeoutTicks=30)
    public static void fireCharmRealDamageAndCleanup(GameTestHelper h){
        var p=CareerSupportIntegrationProbe.floor(h).add(4,0,4);
        var cat=CareerSupportIntegrationProbe.cat(h.getLevel(),p,CatOutfitType.NONE,false);
        var inventory=CatProfileData.openContainer(cat);
        inventory.setItem(0,new ItemStack(BuiltInRegistries.ITEM.get(LaoWuMod.id("cat_fire_charm"))));inventory.setChanged();
        float health=cat.getHealth();cat.setRemainingFireTicks(100);
        h.assertTrue(!cat.hurt(h.getLevel().damageSources().onFire(),2)&&cat.getHealth()==health,"Actual fire damage is rejected");
        cat.setRemainingFireTicks(100);
        h.runAfterDelay(15,()->{
            h.assertTrue(!cat.isOnFire()&&cat.getHealth()==health,"Fire cleanup runs at the native 10-tick cadence");
            inventory.setItem(0,ItemStack.EMPTY);
            cat.invulnerableTime=0;cat.hurt(h.getLevel().damageSources().onFire(),2);
            h.assertTrue(cat.getHealth()<health,"Removing the charm removes immunity");
            cat.discard();System.out.println("PASS: real fire charm immunity, scheduled extinguish and removal");h.succeed();
        });
    }
}
