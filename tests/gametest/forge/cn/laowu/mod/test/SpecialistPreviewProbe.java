package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class SpecialistPreviewProbe {
    private static ResourceLocation id(String value) { return ResourceLocation.fromNamespaceAndPath(value.split(":")[0],value.split(":")[1]); }
    @GameTest(template="artillery_probe",batch="specialist_preview",timeoutTicks=100)
    public static void registrationAndPersistence(GameTestHelper h) {
        var level=h.getLevel();var at=CareerSupportIntegrationProbe.floor(h).add(3,0,4);
        String[] old={"none","terminator","fishing","flight","fire","honey","transport","dynamite","engineering","medical","music"};
        for(int i=0;i<old.length;i++)h.assertTrue(CatOutfitType.byOrdinal(i).id().equals(old[i]),"Old ordinal identity unchanged");
        var owner=net.minecraftforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"suit-probe"));
        var cat=CareerSupportIntegrationProbe.cat(level,at,CatOutfitType.NONE,false,owner);
        var inv=CatProfileData.openContainer(cat);
        inv.setItem(CatProfileData.ACCESSORY_SLOTS+8,new ItemStack(Items.DIAMOND,3));
        float hp=cat.getMaxHealth();double armor=cat.getAttributeValue(Attributes.ARMOR);
        for(var outfit:new CatOutfitType[]{CatOutfitType.AGENT,CatOutfitType.DIVING,CatOutfitType.COCKROACH}) {
            Item suit=BuiltInRegistries.ITEM.get(id("laowu:"+outfit.id()+"_suit"));
            h.assertTrue(suit instanceof TerminatorSuitItem,"Usable suit registered "+outfit);
            h.assertTrue(BuiltInRegistries.ITEM.get(id("laowu:incomplete_"+outfit.id()+"_suit"))!=Items.AIR,"Intermediate registered");
            var stack=new ItemStack(suit,2);owner.getAbilities().instabuild=false;
            h.assertTrue(TerminatorSuitItem.tryEquip(stack,owner,cat).consumesAction()&&stack.getCount()==1,
                    "Actual hand/deployer item hook equips and consumes exactly once");
            h.assertTrue(CatClothesData.getOutfit(cat)==outfit&&!outfit.isPreviewOnly(),"Active career identity applied");
            h.assertTrue(!TerminatorSuitItem.tryEquip(stack,owner,cat).consumesAction()&&stack.getCount()==1,
                    "Already dressed cat cannot consume another suit");
            CareerCatBehavior.tick(cat);
            h.assertTrue(ServerConfig.CAREERS.contains(outfit), "Active career world configuration");
            h.assertTrue(cat.getMaxHealth()>hp&&cat.getAttributeValue(Attributes.ARMOR)>=armor,
                    "Active outfit applies live survival settings");
            for(var stat:CatStat.values())
                h.assertTrue(CatAttributeEffects.effectiveValue(cat,stat)==CatSuitSettings.current(outfit).attribute(50,stat),
                        "Only configured six-stat bonuses, without nearby swarm allies");
            h.assertTrue(CatProfileData.INVENTORY_SLOTS==9&&inv.getItem(12).getCount()==3
                    &&!CatChestData.hasInventory(cat)&&!cat.getPersistentData().contains(CatChestData.ITEMS_TAG),
                    "Existing nine-slot inventory preserved, no extra chest");
            var pancake=CatPancakeItem.capture(cat);
            h.assertTrue(CatPancakeItem.getOutfit(pancake)==outfit
                    &&CatFilterRules.CareerFilter.byId(outfit.id()).matches(pancake),"Pancake identity and filter match");
            var restored=EntityType.CAT.create(level);restored.load(cat.saveWithoutId(new CompoundTag()));
            h.assertTrue(CatClothesData.getOutfit(restored)==outfit,"Server save/reload preserves new identity");
            h.assertTrue(restored.getPersistentData().get(CatProfileData.ITEMS_TAG).equals(cat.getPersistentData().get(CatProfileData.ITEMS_TAG)),
                    "Saved profile contents preserved");restored.discard();
            for(String step:new String[]{"item_application","shearing"})
                h.assertTrue(level.getRecipeManager().byKey(id("laowu:"+outfit.id()+"_suit_"+step)).isPresent(),
                        "Actual Create recipe loaded "+step);
            CatPancakeItem.removeOutfit(pancake);
            h.assertTrue(CatPancakeItem.getOutfit(pancake)==CatOutfitType.NONE,"Pancake shearing removes career");
            CatClothesData.unequip(cat);
            h.assertTrue(CatClothesData.getOutfit(cat)==CatOutfitType.NONE&&inv.getItem(12).getCount()==3,
                    "Live shearing data path preserves inventory");
        }
        cat.discard();
        System.out.println("PASS: three specialist active registries, real equip/consume, configured bonuses, no extra chest, nine-slot persistence, filter and loaded Create recipes");
        h.succeed();
    }
}
