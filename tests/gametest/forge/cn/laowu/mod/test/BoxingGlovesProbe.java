package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.accessory.*;
import cn.laowu.mod.genetics.*;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class BoxingGlovesProbe {
    private static ItemStack item(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(LaoWuMod.id(path)));
    }

    @GameTest(template="accessory_probe", batch="boxing_gloves_definition", timeoutTicks=20)
    public static void glovesAreEpicBossOnlyAndHaveMatchingPackDefinition(GameTestHelper h) throws Exception {
        var gloves=item("cat_boxing_gloves");
        h.assertTrue(!gloves.isEmpty(),"Boxing gloves must be a registered item");
        h.assertTrue(gloves.getRarity()==Rarity.EPIC,"Boxing gloves have Epic name coloring");
        h.assertTrue(gloves.is(CatAccessoryRarity.BOSS_ONLY)&&CatAccessoryRarity.bossOnly(gloves),
                "Boxing gloves are marked as boss-exclusive");
        h.assertTrue(!CatAccessoryRegistry.rewardItemIds().contains("laowu:cat_boxing_gloves"),
                "Boss gloves never enter Wish/adoption reward selection");
        var fallback=CatAccessoryItems.DEFAULTS.get("laowu:cat_boxing_gloves");
        h.assertTrue(fallback!=null&&fallback.effects().equals(Map.of("melee_career_damage",30.0)),
                "Native fallback grants only 30-percent melee-career damage");
        try(var reader=h.getLevel().getServer().getResourceManager()
                .getResource(LaoWuMod.id("cat_accessories/cat_boxing_gloves.json")).orElseThrow().openAsReader()){
            var pack=CatAccessoryDefinition.parse("laowu:cat_boxing_gloves",JsonParser.parseReader(reader).getAsJsonObject());
            h.assertTrue(pack.equals(fallback),"Shipped datapack and native fallback agree");
        }
        h.assertTrue(CatAccessoryRegistry.find(gloves,false).equals(fallback),"Loaded definition agrees with fallback");
        h.succeed();
    }

    @GameTest(template="accessory_probe", batch="boxing_gloves_tooltip", timeoutTicks=20)
    public static void allBossAccessoriesUseOneLocalizedSourceFormat(GameTestHelper h) {
        for(var entry:List.of(
                new String[]{"cat_butter_cube","entity.laowu.butter_cat"},
                new String[]{"cat_giant_collar","entity.laowu.giant_cat_boss"},
                new String[]{"cat_boxing_gloves","entity.laowu.maodie_fighter"})){
            var stack=item(entry[0]);
            h.assertTrue(!stack.isEmpty(),"Registered boss accessory "+entry[0]);
            var lines=new ArrayList<Component>(List.of(Component.literal("name")));
            CatAccessoryTooltip.append(stack,lines);
            h.assertTrue(lines.size()>1&&lines.get(1).getContents() instanceof TranslatableContents,
                    "Boss source is localized for "+entry[0]);
            var source=(TranslatableContents)lines.get(1).getContents();
            h.assertTrue(source.getKey().equals("cat_accessory.laowu.boss_reward"),
                    "All three boss accessories use the same source format: "+entry[0]);
            var args=source.getArgs();
            h.assertTrue(args.length==2&&args[0] instanceof Component&&Integer.valueOf(25).equals(args[1]),
                    "Boss source includes localized name and 25-percent rate");
            h.assertTrue(((Component)args[0]).getContents() instanceof TranslatableContents
                    &&((TranslatableContents)((Component)args[0]).getContents()).getKey().equals(entry[1]),
                    "Source names the correct boss for "+entry[0]);
            h.assertTrue(lines.get(1).getStyle().getColor().equals(
                    net.minecraft.network.chat.TextColor.fromLegacyFormat(ChatFormatting.DARK_PURPLE)),
                    "Boss sources share their existing purple styling");
        }
        h.succeed();
    }

    private static float hit(LivingEntity from,LivingEntity to) {
        to.invulnerableTime=0;
        float before=to.getHealth();
        to.hurt(from.damageSources().mobAttack(from),20);
        return before-to.getHealth();
    }
    private static void close(GameTestHelper h,float actual,float expected,String why) {
        h.assertTrue(Math.abs(actual-expected)<.025,why+": expected "+expected+", got "+actual);
    }

    @GameTest(template="artillery_probe", batch="boxing_gloves_combat", timeoutTicks=170)
    public static void glovesBoostOnlyMeleeCareerDirectHits(GameTestHelper h) {
        var gloves=item("cat_boxing_gloves");
        h.assertTrue(!gloves.isEmpty(),"Boxing gloves must exist before combat checks");
        var p=net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(4,2,4)));
        var cat=EntityType.CAT.create(h.getLevel());
        cat.setTame(true);
        cat.setOwnerUUID(UUID.randomUUID());
        cat.setNoAi(true);cat.setNoGravity(true);cat.setPos(p);
        h.getLevel().addFreshEntity(cat);
        CatTraitData.set(cat,CatTraitProfile.EMPTY);
        cat.setAge(0);
        var genes=CatAttributeData.ensure(cat);
        for(var stat:CatStat.values())genes=genes.withValues(stat,
                stat==CatStat.INTELLIGENCE||stat==CatStat.LUCK?0:50,100);
        CatAttributeData.set(cat,genes);
        CatClothesData.equip(cat,CatOutfitType.NONE);
        CatAttributeEffects.refresh(cat);
        int baseline=CatAttributeEffects.effectiveValue(cat,CatStat.ATTACK);
        var inventory=CatProfileData.openContainer(cat);
        inventory.setItem(0,gloves);
        inventory.setChanged();
        CatAccessories.equipmentChanged(cat);
        h.assertTrue(CatAttributeEffects.effectiveValue(cat,CatStat.ATTACK)==baseline,
                "Equipping gloves does not change the cat's attack attribute");
        var enemy=EntityType.HUSK.create(h.getLevel());
        enemy.setNoAi(true);enemy.setNoGravity(true);enemy.setPos(p.add(2,0,0));
        enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);enemy.setHealth(1000);
        enemy.getAttribute(Attributes.ARMOR).setBaseValue(0);h.getLevel().addFreshEntity(enemy);
        close(h,hit(cat,enemy),20,"No outfit is not a melee career");
        CatClothesData.equip(cat,CatOutfitType.FLIGHT);
        close(h,hit(cat,enemy),26,"Flight melee career receives 30 percent more direct-hit damage");
        CatClothesData.equip(cat,CatOutfitType.TERMINATOR);
        close(h,hit(cat,enemy),20,"Ranged career direct hits receive no gloves bonus");
        CatClothesData.equip(cat,CatOutfitType.MEDICAL);
        close(h,hit(cat,enemy),0,"Support career cannot attack even with gloves equipped");
        CatClothesData.equip(cat,CatOutfitType.AGENT);
        close(h,hit(cat,enemy),26,"Agent melee career also receives 30 percent more damage");
        h.assertTrue(enemy.getDeltaMovement().y<.1,"Gloves do not retain uppercut launch");
        inventory.setItem(0,ItemStack.EMPTY);inventory.setChanged();CatAccessories.equipmentChanged(cat);
        close(h,hit(cat,enemy),20,"Removing gloves removes the damage bonus");
        cat.discard();enemy.discard();
        h.succeed();
    }
}
