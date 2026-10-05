package cn.laowu.mod.test;
import cn.laowu.mod.*;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.item.*;
import cn.laowu.mod.recipe.RandomBabyCatPancakeFillingRecipe;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CatMaterialRemovalProbe {
    private static final ResourceLocation COPY=ResourceLocation.tryParse("copycats:copy_cat");
    private static final ResourceLocation RED=ResourceLocation.tryParse("minecraft:red");
    private static final List<ResourceLocation> REMOVED=List.of(CatMaterialRegistry.OBSIDIAN,CatMaterialRegistry.WOOD,COPY);
    @GameTest(template="artillery_probe",batch="material_legacy",timeoutTicks=60)
    public static void legacyPancakeFallsBackWithoutChangingOtherData(GameTestHelper h){
        var stack=new ItemStack(LaoWuMod.CAT_PANCAKE.get());var data=stack.getOrCreateTag();
        data.putString(CatPancakeItem.CAT_VARIANT_TAG,COPY.toString());
        data.putString(CatPancakeItem.CAT_TEXTURE_TAG,"copycats:textures/entity/cat/copy_cat.png");
        data.putInt("OtherModData",123);
        h.assertTrue(RED.equals(CatPancakeItem.variantId(stack)),"Legacy pancake retains unsupported variant");
        h.assertTrue(BuiltInRegistries.CAT_VARIANT.get(CatVariant.RED).texture().equals(CatPancakeItem.texture(stack)),"Legacy pancake retains unsupported texture");
        data.remove(CatPancakeItem.CAT_VARIANT_TAG);
        h.assertTrue(BuiltInRegistries.CAT_VARIANT.get(CatVariant.RED).texture().equals(CatPancakeItem.texture(stack)),"Texture-only legacy pancake retains unsupported texture");
        h.assertTrue(data.getInt("OtherModData")==123,"Unrelated pancake data changed");h.succeed();
    }
    @GameTest(template="artillery_probe",batch="material_legacy",timeoutTicks=60)
    public static void legacyMixedGenomeOnlyReplacesUnsupportedCopycat(GameTestHelper h){
        var cat=EntityType.CAT.create(h.getLevel());
        var mixed=CatGenome.uniform(CatMaterialRegistry.OBSIDIAN).withMaterial(CatRegion.BODY_FRONT,COPY);
        cat.getPersistentData().put(CatGenomeData.TAG,mixed.save());
        var read=CatGenomeData.getOrFallback(cat);
        h.assertTrue(RED.equals(read.material(CatRegion.BODY_FRONT)),"Legacy genome still renders copycat");
        h.assertTrue(CatMaterialRegistry.OBSIDIAN.equals(read.material(CatRegion.BODY_REAR)),"Unrelated old preset was erased");
        h.succeed();
    }
    @GameTest(template="artillery_probe",batch="material_egg",timeoutTicks=60)
    public static void actualSpawnEggRejectsUnsupportedVariant(GameTestHelper h){
        var egg=new ItemStack(Items.CAT_SPAWN_EGG);var tag=new net.minecraft.nbt.CompoundTag();
        tag.putString("id","minecraft:cat");tag.putString("variant",COPY.toString());
        egg.getOrCreateTag().put("EntityTag",tag);
        var cat=EntityType.CAT.spawn(h.getLevel(),egg,null,h.absolutePos(new BlockPos(1,2,1)),MobSpawnType.SPAWN_EGG,false,false);
        h.assertTrue(cat!=null,"Spawn egg fixture failed");
        h.assertTrue(!COPY.equals(BuiltInRegistries.CAT_VARIANT.getKey(cat.getVariant())),"Actual spawn egg produced copycat");
        cat.discard();h.succeed();
    }
    @GameTest(template="artillery_probe",batch="material_removal",timeoutTicks=40)
    public static void retiredPresetsCannotReenterTheEditor(GameTestHelper h){
        for(var retired:REMOVED){
            var choices=CatMaterialRegistry.selectableMaterials(retired,CatGenome.uniform(retired));
            h.assertTrue(choices.stream().noneMatch(REMOVED::contains),"Retired preset leaked into editor: "+retired);
            h.assertTrue(choices.contains(RED),"Vanilla coat vanished");
        }
        var wand=new ItemStack(LaoWuMod.MATERIAL_DEBUG_WAND.get());
        h.assertTrue(!REMOVED.contains(MaterialDebugWandItem.selectedMaterial(wand)),"Fresh wand selects retired preset");
        for(var id:REMOVED){MaterialDebugWandItem.setSelectedMaterial(wand,id);
            h.assertTrue(!REMOVED.contains(MaterialDebugWandItem.selectedMaterial(wand)),"Saved wand restores retired preset");}
        h.succeed();
    }
    @GameTest(template="artillery_probe",batch="material_removal",timeoutTicks=40)
    public static void mutationPoolExcludesPresetsButStillMapsRealBlocks(GameTestHelper h){
        var pool=CatMaterialRegistry.mutationMaterials();
        h.assertTrue(pool.stream().noneMatch(REMOVED::contains),"Random mutation still emits removed preset");
        for(var block:List.of(Blocks.OBSIDIAN,Blocks.OAK_LOG)){
            var id=CatMaterialRegistry.blockMaterial(block).orElseThrow();
            h.assertTrue(pool.contains(id)&&CatMaterialRegistry.selectableMaterials(id).contains(id),"Actual block sampling was removed with preset: "+block);
        }
        h.succeed();
    }
    @GameTest(template="artillery_probe",batch="material_removal",timeoutTicks=60)
    public static void fillingAndPancakeCatalogExcludeCopycat(GameTestHelper h){
        h.assertTrue(CatPancakeItem.allVariantStacks().stream().noneMatch(s->COPY.equals(CatPancakeItem.variantId(s))),"Pancake variant catalogue offers copycat");
        h.assertTrue(!COPY.equals(CatPancakeItem.variantId(CatPancakeItem.babyVariantStack(COPY))),"Direct baby pancake retains copycat");
        var recipe=h.getLevel().getRecipeManager().getRecipes().stream()
            .filter(r->r instanceof RandomBabyCatPancakeFillingRecipe).map(r->(RandomBabyCatPancakeFillingRecipe)r).findFirst().orElseThrow();
        for(int i=0;i<512;i++)for(var stack:recipe.rollResults())
            h.assertTrue(!COPY.equals(CatPancakeItem.variantId(stack)),"Actual spout output rolled copycat");
        h.succeed();
    }
    @GameTest(template="artillery_probe",batch="material_removal",timeoutTicks=60)
    public static void spawnedCopycatIsReplacedWithoutLosingPetData(GameTestHelper h){
        var variant=BuiltInRegistries.CAT_VARIANT.get(COPY);
        if(variant==null){h.assertTrue(!System.getProperty("laowu.material_copycats","false").equals("true"),"Required real Copycats variant missing");h.succeed();return;}
        var cat=EntityType.CAT.create(h.getLevel());cat.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(1,2,1)));
        cat.setVariant(variant);cat.setTame(true);var owner=UUID.randomUUID();cat.setOwnerUUID(owner);
        cat.setCustomName(net.minecraft.network.chat.Component.literal("preserve-name"));cat.getPersistentData().putInt("OtherModData",123);
        h.getLevel().addFreshEntity(cat);
        h.runAfterDelay(3,()->{
            h.assertTrue(!COPY.equals(BuiltInRegistries.CAT_VARIANT.getKey(cat.getVariant())),"Spawn/join still exposes copycat");
            h.assertTrue(cat.isTame()&&owner.equals(cat.getOwnerUUID())&&cat.getPersistentData().getInt("OtherModData")==123&&cat.getName().getString().equals("preserve-name"),"Sanitization destroyed pet data");
            cat.discard();h.succeed();
        });
    }
}
