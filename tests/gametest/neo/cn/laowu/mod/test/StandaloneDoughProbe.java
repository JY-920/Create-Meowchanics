package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.compat.create.CreateIntegration;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.item.CatPancakeItem;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import java.util.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class StandaloneDoughProbe {
    @GameTest(template="artillery_probe",timeoutTicks=30)
    public static void bareRecipePreviewUsesOrangeTextureWithoutChangingRealCoats(GameTestHelper h) {
        var orange=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft","textures/entity/cat/red.png");
        var bare=new ItemStack(LaoWuMod.CAT_PANCAKE.get());
        h.assertTrue(CatPancakeItem.texture(bare).equals(orange),"Untagged JEI result must use the orange cat texture, never an empty resource path");
        var black=CatPancakeItem.variantStack(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft","black"));
        h.assertTrue(CatPancakeItem.texture(black).equals(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft","textures/entity/cat/black.png")),"Stored non-orange coat must not be overwritten by preview fallback");
        if(!CreateIntegration.isLoaded()) {
            var recipe=h.getLevel().getRecipeManager().byKey(LaoWuMod.id("standalone/cat_pancake_smelting")).orElseThrow().value();
            h.assertTrue(CatPancakeItem.texture(recipe.getResultItem(h.getLevel().registryAccess())).equals(orange),"Actual loaded furnace result preview is orange");
        }
        h.succeed();
    }

    @GameTest(template="artillery_probe",timeoutTicks=30)
    public static void doughAndCookingAreStandaloneOnly(GameTestHelper h) {
        var manager=h.getLevel().getRecipeManager();
        for(String name:List.of("cat_dough","cat_pancake_smelting"))
            h.assertTrue(manager.byKey(LaoWuMod.id("standalone/"+name)).isPresent()!=CreateIntegration.isLoaded(),
                    "Dough acquisition must load only without Create: "+name);
        h.succeed();
    }

    @GameTest(template="artillery_probe",timeoutTicks=60)
    public static void furnaceRollsOneRealPancakeAndNeverRerollsBlockedOutput(GameTestHelper h) {
        if(CreateIntegration.isLoaded()){h.succeed();return;}
        var pos=new BlockPos(5,2,5);
        h.setBlock(pos,Blocks.FURNACE);
        var absolute=h.absolutePos(pos);
        var furnace=(AbstractFurnaceBlockEntity)h.getLevel().getBlockEntity(absolute);
        furnace.setItem(1,new ItemStack(Items.COAL,64));
        Set<String> skins=new HashSet<>();
        for(int batch=0;batch<32;batch++) {
            furnace.setItem(2,ItemStack.EMPTY);
            furnace.setItem(0,new ItemStack(LaoWuMod.CAT_DOUGH.get(),2));
            for(int tick=0;tick<205;tick++)
                AbstractFurnaceBlockEntity.serverTick(h.getLevel(),absolute,h.getLevel().getBlockState(absolute),furnace);
            var result=furnace.getItem(2);
            h.assertTrue(result.is(LaoWuMod.CAT_PANCAKE.get())&&result.getCount()==1,"Real furnace produces exactly one pancake");
            h.assertTrue(furnace.getItem(0).getCount()==1,"Only one dough consumed while pancake output blocks further cooking");
            var genome=CatGenomeData.read(result).orElseThrow();
            skins.add(genome.material(CatRegion.HEAD_PRIMARY).toString());
            h.assertTrue(genome.isUniform()&&CatMaterialRegistry.catVariants().contains(genome.material(CatRegion.HEAD_PRIMARY)),"Only allowed cat skins generated");
            h.assertTrue(CatAttributeData.read(result).isPresent()&&CatTraitData.read(result).isPresent(),"New pancake has normal cat genes");
            var retained=result.copy();
            for(int tick=0;tick<205;tick++)
                AbstractFurnaceBlockEntity.serverTick(h.getLevel(),absolute,h.getLevel().getBlockState(absolute),furnace);
            h.assertTrue(ItemStack.isSameItemSameComponents(retained,furnace.getItem(2)),"Blocked output never rerolls stored cat data");
            h.assertTrue(furnace.getItem(0).getCount()==1,"Blocked output never consumes additional dough");
        }
        h.assertTrue(skins.size()>1,"Repeated actual burns are random, not the JEI preview skin");
        h.succeed();
    }

    @GameTest(template="artillery_probe",timeoutTicks=40)
    public static void sneakBlockUseRestoresExactlyOneCatWithItsData(GameTestHelper h) {
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"restore-probe"));
        player.setShiftKeyDown(true);
        var original=EntityType.CAT.create(h.getLevel());
        original.setTame(true,true);original.setOwnerUUID(player.getUUID());
        original.setCustomName(net.minecraft.network.chat.Component.literal("Stored Cat"));
        CatClothesData.equip(original,CatOutfitType.MEDICAL);
        CatTraitData.set(original,CatTraitProfile.EMPTY.withLevel(CatTrait.NEKOMATA,1));
        var inventory=CatProfileData.openContainer(original);
        inventory.setItem(0,new ItemStack(LaoWuMod.CAT_INGOT.get(),2));
        inventory.setItem(4,new ItemStack(Items.DIAMOND,3));inventory.setChanged();
        var attributes=CatAttributeData.ensure(original);
        var stack=CatPancakeItem.capture(original);
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var local=new BlockPos(7,1,7);
        h.setBlock(local,Blocks.STONE);
        var pos=h.absolutePos(local);
        player.moveTo(Vec3.atCenterOf(pos).add(0,1,-2));
        var context=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos).add(0,.5,0),Direction.UP,pos,false));
        var result=stack.useOn(context);
        if(CreateIntegration.isLoaded()) {
            h.assertTrue(!result.consumesAction()&&stack.getCount()==1,"Create mode retains its existing restoration route");
        } else {
            h.assertTrue(result.consumesAction()&&stack.isEmpty(),"Standalone sneak-use consumes only after successful spawn");
            var cats=h.getLevel().getEntitiesOfClass(Cat.class,new net.minecraft.world.phys.AABB(pos.above()).inflate(1));
            h.assertTrue(cats.size()==1,"Exactly one cat restored");
            var cat=cats.get(0);
            h.assertTrue(cat.isTame()&&player.getUUID().equals(cat.getOwnerUUID())&&cat.getName().getString().equals("Stored Cat"),"Owner and name retained");
            h.assertTrue(CatClothesData.getOutfit(cat)==CatOutfitType.MEDICAL&&CatTraitData.read(cat).orElseThrow().has(CatTrait.NEKOMATA),"Outfit and traits retained");
            var after=CatAttributeData.read(cat).orElseThrow();
            for(var stat:CatStat.values())h.assertTrue(after.current(stat)==attributes.current(stat)&&after.potential(stat)==attributes.potential(stat),"All six attributes retained");
            h.assertTrue(!CatPoseData.isPancake(cat),"Restored cat is alive in normal pose");
            var restoredInventory=CatProfileData.openContainer(cat);
            h.assertTrue(restoredInventory.getItem(0).is(LaoWuMod.CAT_INGOT.get())
                    &&restoredInventory.getItem(0).getCount()==2&&restoredInventory.getItem(4).is(Items.DIAMOND)
                    &&restoredInventory.getItem(4).getCount()==3,"Inventory slots and quantities retained");
        }
        h.succeed();
    }

    @GameTest(template="artillery_probe",timeoutTicks=30)
    public static void blockedRestorationKeepsPancakeAndOrdinaryUseStillCharges(GameTestHelper h) {
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"blocked-restore"));
        var stack=CatPancakeItem.variantStack(CatPancakeItem.DEFAULT_VARIANT);
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var pos=h.absolutePos(new BlockPos(10,1,10));
        h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.above(),Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.above(2),Blocks.STONE.defaultBlockState());
        var ctx=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        player.setShiftKeyDown(true);
        h.assertTrue(!stack.useOn(ctx).consumesAction()&&stack.getCount()==1,"Obstructed placement must not consume stored cat");
        player.setShiftKeyDown(false);
        var result=stack.getItem().use(h.getLevel(),player,InteractionHand.MAIN_HAND);
        h.assertTrue(result.getResult().consumesAction()&&player.isUsingItem()&&stack.getCount()==1,"Normal use still charges a throw");
        h.succeed();
    }
}
