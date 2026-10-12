package cn.laowu.mod.test;
import cn.laowu.mod.*;
import cn.laowu.mod.create.*;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.gametest.*;
@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CatCareProbe {
    @GameTest(template="artillery_probe",timeoutTicks=30)
    public static void nativeConfigurationSurvivesDependencyRemoval(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(4,2,4));
        var state=LaoWuMod.ADVANCED_BREEDING_BOX.get().defaultBlockState();
        var standalone=new BreedingBoxData(pos,state);standalone.setLevel(h.getLevel());
        var tag=new net.minecraft.nbt.CompoundTag();
        var filters=new net.minecraft.nbt.ListTag();var filter=new net.minecraft.nbt.CompoundTag();
        filter.putString("Side","west");var stack=new net.minecraft.nbt.CompoundTag();
        stack.putString("id","laowu:cat_filter");stack.putInt("Count",1);
        filter.put("Filter",stack);filter.putInt("FilterAmount",2);filter.putBoolean("UpTo",true);filters.add(filter);
        tag.put("Filters",filters.copy());tag.putInt("ScrollValue",2);
        standalone.readData(tag);standalone.menuData().set(0,77);
        var saved=new net.minecraft.nbt.CompoundTag();standalone.writeData(saved);
        h.assertTrue(filters.equals(saved.getList("Filters",net.minecraft.nbt.Tag.TAG_COMPOUND))&&saved.getInt("ScrollValue")==2,"Raw Create configuration survives standalone save");
        h.assertTrue(saved.getInt("BreedingProgress")==77,"Opaque preservation cannot overwrite live core progress");
        h.succeed();
    }
    @GameTest(template="artillery_probe",timeoutTicks=30)
    public static void allTiersBreedPersistAndExposeMenus(GameTestHelper h) {
        var blocks=java.util.List.of(LaoWuMod.BASIC_BREEDING_BOX.get(),LaoWuMod.INTERMEDIATE_BREEDING_BOX.get(),LaoWuMod.ADVANCED_BREEDING_BOX.get());
        int[] durations={2400,1800,1200};
        for(int i=0;i<3;i++) {
            var pos=h.absolutePos(new BlockPos(4+i*4,2,4));
            h.getLevel().setBlock(pos,blocks.get(i).defaultBlockState(),3);
            var entity=h.getLevel().getBlockEntity(pos);
            var data=((BreedingBoxAccess)entity).breedingData();
            var father=CatPancakeItem.defaultDisplayStack();var mother=father.copy();
            CatTraitData.set(father,CatTraitProfile.EMPTY);CatTraitData.set(mother,CatTraitProfile.EMPTY);
            h.assertTrue(data.insertParent(father).isEmpty()&&data.insertParent(mother).isEmpty(),"Both parents accepted");
            h.assertTrue(data.insertFood(new ItemStack(LaoWuMod.BREEDING_CAT_FOOD.get(),2)).isEmpty(),"Breeding food accepted");
            h.assertTrue(data.effectiveDurationTicks()==durations[i],"Distinct existing tier durations");
            data.menuData().set(0,123);
            var tag=entity.saveWithoutMetadata();
            var restored=entity.getType().create(pos,entity.getBlockState());restored.setLevel(h.getLevel());
            restored.load(tag);
            var saved=((BreedingBoxAccess)restored).breedingData();
            h.assertTrue(saved.menuData().get(0)==123&&saved.inventory().getStackInSlot(3).getCount()==2,"Progress and inventory survive NBT");
            h.assertTrue(saved.createMenu(2,net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"cat-care")).getInventory(),net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"cat-care"))) instanceof BreedingBoxMenu,"Original observation menu available");
            data.menuData().set(0,durations[i]-1);
            BreedingBoxData.tickBox(h.getLevel(),pos,entity.getBlockState(),entity);
            h.assertTrue(CatPancakeItem.isBaby(data.inventory().getStackInSlot(2)),"Actual breeding creates baby pancake");
            h.assertTrue(data.inventory().getStackInSlot(3).getCount()==1,"Exactly one food consumed");
            h.assertTrue(CatGenomeData.read(data.inventory().getStackInSlot(2)).isPresent()&&CatAttributeData.read(data.inventory().getStackInSlot(2)).isPresent(),"Child keeps inherited gameplay data");
            h.assertTrue(!net.minecraft.world.level.block.Block.getDrops(entity.getBlockState(),h.getLevel(),pos,entity).isEmpty(),"Restored breeding box still drops itself");
            h.getLevel().removeBlock(pos,false);
        }
        h.succeed();
    }
    @GameTest(template="artillery_probe",timeoutTicks=30)
    public static void ordinaryAdoptionCompletesWithoutSeats(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(5,2,5));h.getLevel().setBlock(pos,LaoWuMod.ADOPTION_BOX.get().defaultBlockState(),3);
        var box=(AdoptionBoxBlockEntity)h.getLevel().getBlockEntity(pos);
        var pancake=CatPancakeItem.defaultDisplayStack();CatTraitData.set(pancake,CatTraitProfile.EMPTY);
        box.inventory().setStackInSlot(0,pancake);
        var tag=box.saveWithoutMetadata();tag.putIntArray("AdoptionProgress",new int[]{AdoptionBoxBlockEntity.durationForQuality(box.qualityScore(pancake))-1});
        box.load(tag);
        AdoptionBoxBlockEntity.serverTick(h.getLevel(),pos,box.getBlockState(),box);
        h.assertTrue(box.inventory().getStackInSlot(0).isEmpty(),"Completed adoption consumes exactly one cat");
        int rewards=0;for(int i=9;i<18;i++)rewards+=box.inventory().getStackInSlot(i).getCount();
        h.assertTrue(rewards>0,"Native adoption rewards work with no Create seats");
        h.assertTrue(box.createMenu(3,net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"cat-care")).getInventory(),net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"cat-care"))) instanceof AdoptionBoxMenu,"Original adoption menu available");
        h.succeed();
    }
    @GameTest(template="artillery_probe",timeoutTicks=30)
    public static void storageBoxCapturesAndReleasesLivingCat(GameTestHelper h) {
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"cat-care"));var cat=h.spawn(EntityType.CAT,5,2,5);cat.setTame(true);cat.setOwnerUUID(player.getUUID());
        CatTraitData.set(cat,CatTraitProfile.EMPTY.withLevel(CatTrait.THORNS,3));
        CatClothesData.equip(cat,CatOutfitType.MEDICAL);
        var uuid=cat.getUUID();var stack=new ItemStack(LaoWuMod.CAT_STORAGE_BOX.get());
        var item=(CatStorageBoxItem)stack.getItem();
        CatClothesData.equip(cat,CatOutfitType.TRANSPORT);
        h.getLevel().addNewPlayer(player);
        player.containerMenu=net.minecraft.world.inventory.ChestMenu.threeRows(4,player.getInventory(),CatChestData.openContainer(cat));
        item.interactLivingEntity(stack,player,cat,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(CatStorageBoxItem.count(stack)==0&&!cat.isRemoved(),"Open standalone cargo inventory blocks capture");
        player.closeContainer();
        h.getLevel().removePlayerImmediately(player,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        CatClothesData.equip(cat,CatOutfitType.MEDICAL);
        h.assertTrue(item.interactLivingEntity(stack,player,cat,net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction(),"Storage interaction handled");
        h.assertTrue(CatStorageBoxItem.count(stack)==1&&cat.isRemoved(),"Living cat stored without Create");
        var pos=h.absolutePos(new BlockPos(8,1,5));h.getLevel().setBlock(pos,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        item.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false)));
        var released=h.getLevel().getEntity(uuid);
        h.assertTrue(CatStorageBoxItem.count(stack)==0&&released instanceof net.minecraft.world.entity.animal.Cat,"Stored cat released");
        var restored=(net.minecraft.world.entity.animal.Cat)released;
        h.assertTrue(player.getUUID().equals(restored.getOwnerUUID())&&CatClothesData.getOutfit(restored)==CatOutfitType.MEDICAL,"Owner and outfit retained");
        h.assertTrue(CatTraitData.read(restored).orElseThrow().level(CatTrait.THORNS)==3,"Stored trait level retained");
        restored.discard();h.succeed();
    }
}
