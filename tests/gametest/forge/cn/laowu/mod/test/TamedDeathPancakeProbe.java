package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.item.CatPancakeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class TamedDeathPancakeProbe {
    private static boolean inMemorySettings;
    private static final UUID OWNER=UUID.fromString("9cb9322d-45bc-44b7-9a12-65dc1f449fa4");
    private static Vec3 floor(GameTestHelper h) {
        if(h.getLevel().getServer() instanceof GameTestServer) {
            if (!inMemorySettings) {
                // Rapid case-by-case .set() on Forge's autosaving file races its file watcher.
                // Exercise the same real config spec without disk I/O between independent fixtures.
                var config=com.electronwill.nightconfig.core.CommentedConfig.inMemory();
                ServerConfig.SPEC.correct(config);ServerConfig.SPEC.setConfig(config);
                inMemorySettings=true;
            }
            var first=h.absolutePos(BlockPos.ZERO);var last=h.absolutePos(new BlockPos(71,11,13));
            for(int x=Math.floorDiv(first.getX(),16);x<=Math.floorDiv(last.getX(),16);x++)
                for(int z=Math.floorDiv(first.getZ(),16);z<=Math.floorDiv(last.getZ(),16);z++)
                    h.getLevel().setChunkForced(x,z,true);
        }
        for(int x=0;x<12;x++)for(int z=0;z<12;z++) {
            h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
            for(int y=1;y<7;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
        }
        return Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(5,1,5)));
    }
    private static Cat cat(GameTestHelper h,Vec3 pos,boolean tamed,boolean baby,CatOutfitType outfit) {
        var cat=EntityType.CAT.create(h.getLevel());
        cat.setTame(tamed);if(tamed)cat.setOwnerUUID(OWNER);
        cat.setNoAi(true);cat.setNoGravity(true);cat.setPos(pos);
        h.getLevel().addFreshEntity(cat);
        CatTraitData.set(cat,CatTraitProfile.EMPTY.withLevel(CatTrait.LONG_FUR,1));
        cat.setAge(baby?-24000:0);cat.setCustomName(Component.literal("Pancake regression"));
        var collarData=new net.minecraft.nbt.CompoundTag();cat.addAdditionalSaveData(collarData);
        collarData.putByte("CollarColor",(byte)DyeColor.BLUE.getId());cat.readAdditionalSaveData(collarData);
        CatPoseData.setPose(cat,CatPoseData.NORMAL);
        var genes=CatAttributeData.ensure(cat);
        for(var stat:CatStat.values())genes=genes.withValues(stat,50,100);
        CatAttributeData.set(cat,genes);CatClothesData.equip(cat,outfit);CareerCatBehavior.tick(cat);
        var inventory=CatProfileData.openContainer(cat);
        inventory.setItem(0,new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("laowu","cat_speed_badge"))));
        inventory.setItem(4,new ItemStack(Items.DIAMOND,3));
        inventory.setItem(12,new ItemStack(Items.APPLE,2));inventory.setChanged();
        cat.setHealth(cat.getMaxHealth());
        return cat;
    }
    private static List<ItemEntity> drops(GameTestHelper h,Vec3 pos) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos,pos).inflate(4));
    }
    private static int count(List<ItemEntity> drops,Item item) {
        return drops.stream().filter(d->d.getItem().is(item)).mapToInt(d->d.getItem().getCount()).sum();
    }
    private static void snapshot(GameTestHelper h,boolean baby,CatOutfitType outfit) {
        snapshot(h,baby,outfit,true,ServerConfig.DEFAULT_DEATH_ATTRIBUTE_LOSS);
    }
    private static void snapshot(GameTestHelper h,boolean baby,CatOutfitType outfit,boolean enabled,int loss) {
        var level=h.getLevel();var pos=floor(h);var cat=cat(h,pos,true,baby,outfit);
        var variant=cat.getVariant();
        h.assertTrue(level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT),"Fixture has mob loot enabled");
        boolean previousEnabled=ServerConfig.DEATH_ATTRIBUTE_PENALTY_ENABLED.get();
        int previousLoss=ServerConfig.DEATH_ATTRIBUTE_LOSS.get();
        int expectedLoss=enabled?Math.min(50,loss):0;
        try {
            ServerConfig.DEATH_ATTRIBUTE_PENALTY_ENABLED.set(enabled);
            ServerConfig.DEATH_ATTRIBUTE_LOSS.set(loss);
            var ordinaryCapture=CatAttributeData.read(CatPancakeItem.capture(cat)).orElseThrow();
            for(var stat:CatStat.values())h.assertTrue(ordinaryCapture.current(stat)==50&&ordinaryCapture.potential(stat)==100,
                    "Non-death capture never applies the death penalty");
            cat.hurt(level.damageSources().genericKill(),Float.MAX_VALUE);
        } finally {
            // Restore before yielding so no other test/tick sees these temporary world settings.
            ServerConfig.DEATH_ATTRIBUTE_PENALTY_ENABLED.set(previousEnabled);
            ServerConfig.DEATH_ATTRIBUTE_LOSS.set(previousLoss);
        }
        h.runAfterDelay(2,()->{
            var dropped=drops(h,pos);
            h.assertTrue(count(dropped,LaoWuMod.CAT_PANCAKE.get())==1,"Exactly one death pancake, including tamed cats without careers");
            var pancake=dropped.stream().filter(d->d.getItem().is(LaoWuMod.CAT_PANCAKE.get())).findFirst().orElseThrow();
            var stack=pancake.getItem();
            h.assertTrue(CatPancakeItem.isTamed(stack)&&CatPancakeItem.isBaby(stack)==baby
                    &&CatPancakeItem.getOutfit(stack)==outfit,"Taming, age and optional career survive capture");
            h.assertTrue(count(dropped,Items.DIAMOND)==0&&count(dropped,Items.APPLE)==0
                    &&count(dropped,BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("laowu","cat_speed_badge")))==0,
                    "Inventory and accessories stay in the pancake, never also drop loose");
            var saved=CatAttributeData.read(stack).orElseThrow();int reduced=0;
            for(var stat:CatStat.values()) {
                h.assertTrue(saved.potential(stat)==100,"Potential retained for "+stat);
                if(expectedLoss>0&&saved.current(stat)==50-expectedLoss)reduced++;
                else h.assertTrue(saved.current(stat)==50,"No additional base-stat change");
            }
            h.assertTrue(reduced==(expectedLoss>0?1:0),"Configured penalty changes one random base stat or none when disabled/zero");
            // Exercise the existing production restore routine; wind detection itself is unchanged.
            try {
                var restore=CatPancakeItem.class.getDeclaredMethod("restoreCat",ServerLevel.class,ItemEntity.class,ItemStack.class);
                restore.setAccessible(true);restore.invoke(null,level,pancake,stack);
            }catch(ReflectiveOperationException error){throw new RuntimeException(error);}
            h.runAfterDelay(2,()->{
                var restored=level.getEntitiesOfClass(Cat.class,new AABB(pos,pos).inflate(4),
                        candidate->candidate!=cat&&candidate.isAlive());
                h.assertTrue(restored.size()==1&&pancake.isRemoved(),"One live cat restored, pancake consumed");
                var live=restored.get(0);
                h.assertTrue(live.isTame()&&OWNER.equals(live.getOwnerUUID())&&live.isBaby()==baby
                        &&live.getVariant().equals(variant)&&live.getCollarColor()==DyeColor.BLUE
                        &&live.getName().getString().equals("Pancake regression")&&CatClothesData.getOutfit(live)==outfit,
                        "Owner, kitten age, coat, collar, name and career restored");
                h.assertTrue(CatTraitData.ensure(live).has(CatTrait.LONG_FUR),"Traits retained");
                var inventory=CatProfileData.openContainer(live);
                h.assertTrue(inventory.getItem(4).is(Items.DIAMOND)&&inventory.getItem(4).getCount()==3
                        &&inventory.getItem(12).is(Items.APPLE)&&inventory.getItem(12).getCount()==2
                        &&inventory.getItem(0).is(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("laowu","cat_speed_badge"))),
                        "Exact inventory/accessory slots and counts restored");
                for(var stat:CatStat.values())h.assertTrue(CatAttributeData.ensure(live).current(stat)==saved.current(stat)
                        &&CatAttributeData.ensure(live).potential(stat)==saved.potential(stat),"Death genes restored: "+stat);
                live.discard();cat.discard();for(var drop:drops(h,pos))drop.discard();
                h.succeed();System.out.println("PASS: real tamed death and production restore, baby="+baby+", outfit="+outfit+", enabled="+enabled+", loss="+loss+", preserved identity/items");
            });
        });
    }
    private static void outcome(GameTestHelper h,int mode,boolean penalty,boolean loot,boolean tamed,boolean baby) {
        var level=h.getLevel();var pos=floor(h);
        var cat=cat(h,pos,tamed,baby,tamed?CatOutfitType.NONE:CatOutfitType.FIRE);
        int oldMode=ServerConfig.DEATH_OUTCOME.get(),oldLoss=ServerConfig.DEATH_ATTRIBUTE_LOSS.get();
        boolean oldPenalty=ServerConfig.DEATH_ATTRIBUTE_PENALTY_ENABLED.get();
        var rule=level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT);boolean oldLoot=rule.get();
        try {
            ServerConfig.DEATH_OUTCOME.set(mode);ServerConfig.DEATH_ATTRIBUTE_LOSS.set(7);
            ServerConfig.DEATH_ATTRIBUTE_PENALTY_ENABLED.set(penalty);rule.set(loot,level.getServer());
            cat.hurt(level.damageSources().genericKill(),Float.MAX_VALUE);
        } finally {
            ServerConfig.DEATH_OUTCOME.set(oldMode);ServerConfig.DEATH_ATTRIBUTE_LOSS.set(oldLoss);
            ServerConfig.DEATH_ATTRIBUTE_PENALTY_ENABLED.set(oldPenalty);rule.set(oldLoot,level.getServer());
        }
        h.runAfterDelay(3,()->{
            var dropped=drops(h,pos);
            var living=level.getEntitiesOfClass(Cat.class,new AABB(pos,pos).inflate(4),
                    value->value!=cat&&value.isAlive());
            if(!loot) {
                h.assertTrue(dropped.isEmpty()&&living.isEmpty(),"No mode bypasses doMobLoot");
            } else if(!tamed) {
                h.assertTrue(count(dropped,LaoWuMod.CAT_PANCAKE.get())==1&&living.isEmpty(),
                        "Untamed career cats keep legacy item outcome");
            } else if(mode==0) {
                h.assertTrue(living.isEmpty()&&count(dropped,LaoWuMod.CAT_PANCAKE.get())==0,"None mode creates no pancake");
                h.assertTrue(count(dropped,Items.DIAMOND)==3&&count(dropped,Items.APPLE)==2
                        &&count(dropped,BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("laowu","cat_speed_badge")))==1,
                        "None mode drops every inventory/accessory item exactly once");
            } else {
                h.assertTrue(living.size()==1&&count(dropped,LaoWuMod.CAT_PANCAKE.get())==0,
                        "Entity mode creates exactly one living pancake, not an item");
                var flat=living.get(0);
                h.assertTrue(CatPoseData.isPancake(flat)&&flat.isTame()&&OWNER.equals(flat.getOwnerUUID())
                        &&flat.isBaby()==baby&&flat.getName().getString().equals("Pancake regression"),
                        "Flattened entity preserves owner, age and name");
                h.assertTrue(CatTraitData.ensure(flat).has(CatTrait.LONG_FUR),"Entity preserves traits");
                var inventory=CatProfileData.openContainer(flat);
                h.assertTrue(inventory.getItem(4).getCount()==3&&inventory.getItem(12).getCount()==2
                        &&inventory.getItem(0).is(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("laowu","cat_speed_badge")))
                        &&count(dropped,Items.DIAMOND)==0&&count(dropped,Items.APPLE)==0,
                        "Entity retains exact cargo and accessory slots without loose duplication");
                int changed=0;var genes=CatAttributeData.ensure(flat);
                for(var stat:CatStat.values()){
                    h.assertTrue(genes.potential(stat)==100,"Entity retains potential");
                    if(genes.current(stat)==43)changed++;else h.assertTrue(genes.current(stat)==50,"No extra penalty");
                }
                h.assertTrue(changed==(penalty?1:0),"Entity applies configured random loss exactly once");
                var recaptured=CatPancakeItem.capture(flat);
                h.assertTrue(CatPancakeItem.hasOwner(recaptured),"Living pancake remains safely capturable");
            }
            for(var value:living)value.discard();cat.discard();for(var drop:dropped)drop.discard();
            h.succeed();System.out.println("PASS: death outcome "+mode+", penalty="+penalty+", loot="+loot+", tamed="+tamed);
        });
    }
    @GameTest(template="artillery_probe",batch="death_mode_none",timeoutTicks=40)
    public static void noPancakeDropsCargo(GameTestHelper h){outcome(h,0,true,true,true,false);}
    @GameTest(template="artillery_probe",batch="death_mode_entity",timeoutTicks=40)
    public static void entityPancake(GameTestHelper h){outcome(h,2,true,true,true,false);}
    @GameTest(template="artillery_probe",batch="death_mode_entity_disabled",timeoutTicks=40)
    public static void entityKittenWithoutPenalty(GameTestHelper h){outcome(h,2,false,true,true,true);}
    @GameTest(template="artillery_probe",batch="death_mode_none_no_loot",timeoutTicks=40)
    public static void noneRespectsNoLoot(GameTestHelper h){outcome(h,0,true,false,true,false);}
    @GameTest(template="artillery_probe",batch="death_mode_entity_no_loot",timeoutTicks=40)
    public static void entityRespectsNoLoot(GameTestHelper h){outcome(h,2,true,false,true,false);}
    @GameTest(template="artillery_probe",batch="death_mode_untamed",timeoutTicks=40)
    public static void untamedCareerIgnoresMode(GameTestHelper h){outcome(h,0,true,true,false,false);}
    @GameTest(template="artillery_probe",batch="death_tamed_adult",timeoutTicks=40)
    public static void tamedWithoutCareer(GameTestHelper h){snapshot(h,false,CatOutfitType.NONE);}
    @GameTest(template="artillery_probe",batch="death_tamed_kitten",timeoutTicks=40)
    public static void tamedKittenWithoutCareer(GameTestHelper h){snapshot(h,true,CatOutfitType.NONE);}
    @GameTest(template="artillery_probe",batch="death_career_control",timeoutTicks=40)
    public static void careerDeathUnchanged(GameTestHelper h){snapshot(h,false,CatOutfitType.FIRE);}
    @GameTest(template="artillery_probe",batch="death_penalty_disabled",timeoutTicks=40)
    public static void disabledPenalty(GameTestHelper h){snapshot(h,false,CatOutfitType.NONE,false,Integer.MAX_VALUE);}
    @GameTest(template="artillery_probe",batch="death_penalty_disabled_kitten",timeoutTicks=40)
    public static void disabledCareerKittenPenalty(GameTestHelper h){snapshot(h,true,CatOutfitType.FIRE,false,20);}
    @GameTest(template="artillery_probe",batch="death_penalty_custom",timeoutTicks=40)
    public static void customPenalty(GameTestHelper h){snapshot(h,false,CatOutfitType.NONE,true,7);}
    @GameTest(template="artillery_probe",batch="death_penalty_custom_career",timeoutTicks=40)
    public static void customCareerPenalty(GameTestHelper h){snapshot(h,false,CatOutfitType.FIRE,true,7);}
    @GameTest(template="artillery_probe",batch="death_penalty_zero",timeoutTicks=40)
    public static void zeroPenalty(GameTestHelper h){snapshot(h,false,CatOutfitType.NONE,true,0);}
    @GameTest(template="artillery_probe",batch="death_penalty_clamp",timeoutTicks=40)
    public static void penaltyFlooredAtZero(GameTestHelper h){snapshot(h,false,CatOutfitType.NONE,true,Integer.MAX_VALUE);}
    @GameTest(template="artillery_probe",batch="death_wild_control",timeoutTicks=30)
    public static void wildWithoutCareerStillDropsContents(GameTestHelper h){
        var pos=floor(h);var cat=cat(h,pos,false,false,CatOutfitType.NONE);
        cat.hurt(h.getLevel().damageSources().genericKill(),Float.MAX_VALUE);
        h.runAfterDelay(2,()->{
            var dropped=drops(h,pos);
            h.assertTrue(count(dropped,LaoWuMod.CAT_PANCAKE.get())==0,"Untamed no-career cat does not yield a pancake");
            h.assertTrue(count(dropped,Items.DIAMOND)==3&&count(dropped,Items.APPLE)==2
                    &&count(dropped,BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("laowu","cat_speed_badge")))==1,
                    "Wild cat keeps its old loose-loot behavior, without lost or duplicated cargo");
            cat.discard();for(var drop:dropped)drop.discard();h.succeed();
            System.out.println("PASS: untamed no-career cat remains ordinary loose loot, not a pancake");
        });
    }
    @GameTest(template="artillery_probe",batch="death_no_mob_loot",timeoutTicks=30)
    public static void mobLootDisabled(GameTestHelper h){
        var level=h.getLevel();var pos=floor(h);var cat=cat(h,pos,true,false,CatOutfitType.NONE);
        var rule=level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT);boolean previous=rule.get();
        try{rule.set(false,level.getServer());cat.hurt(level.damageSources().genericKill(),Float.MAX_VALUE);}
        finally{rule.set(previous,level.getServer());}
        h.runAfterDelay(2,()->{
            h.assertTrue(drops(h,pos).isEmpty(),"doMobLoot=false suppresses pancake and cargo drops");
            cat.discard();h.succeed();System.out.println("PASS: no new death drop bypasses doMobLoot");
        });
    }
    @GameTest(template="artillery_probe",batch="death_protected_control",timeoutTicks=30)
    public static void savedByTotemDoesNotDropPancake(GameTestHelper h){
        var pos=floor(h);var cat=cat(h,pos,true,false,CatOutfitType.NONE);
        cat.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.TOTEM_OF_UNDYING));
        cat.hurt(h.getLevel().damageSources().generic(),10000);
        h.runAfterDelay(2,()->{
            h.assertTrue(cat.isAlive()&&cat.getMainHandItem().isEmpty(),"Real vanilla totem prevents death");
            h.assertTrue(drops(h,pos).isEmpty()&&CatProfileData.openContainer(cat).getItem(4).getCount()==3,
                    "Death protection produces no pancake and does not move inventory");
            cat.discard();h.succeed();System.out.println("PASS: prevented death never duplicates a tamed cat");
        });
    }
}
