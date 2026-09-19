package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.item.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CatPouchProbe {
    private static ServerPlayer player(GameTestHelper h) {
        var level=h.getLevel();
        var fake=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"pouch-link"));
        var p=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"pouch-probe"),net.minecraft.server.level.ClientInformation.createDefault());
        p.connection=fake.connection;
        var pos=h.absolutePos(new BlockPos(2,2,2));p.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LaoWuMod.CAT_POUCH.get()));
        return p;
    }
    private static Cat cat(GameTestHelper h,ServerPlayer p) {
        Cat cat=EntityType.CAT.create(h.getLevel());
        cat.moveTo(p.getX()+1,p.getY(),p.getZ(),0,0);cat.setNoAi(true);
        h.getLevel().addFreshEntity(cat);
        CatTraitData.set(cat,CatTraitProfile.EMPTY);cat.setAge(0);
        CatAttributeData.set(cat,CatAttributeProfile.founder(RandomSource.create(31)));
        return cat;
    }
    private static PlayerInteractEvent.EntityInteract capture(ServerPlayer p,Cat cat) {
        var event=new PlayerInteractEvent.EntityInteract(p,InteractionHand.MAIN_HAND,cat);
        CommonEvents.onCatInteract(event);return event;
    }
    private static CompoundTag data(ItemStack stack) { return ItemCustomData.copy(stack); }
    private static void finish(GameTestHelper h,String message) {
        System.out.println("PASS: cat pouch "+message);h.succeed();
    }

    @GameTest(template="accessory_probe",batch="pouch31",timeoutTicks=20)
    public static void normalAndFlattenedRoundTrip(GameTestHelper h) {
        var p=player(h);var pouch=p.getMainHandItem();var cat=cat(h,p);
        UUID owner=UUID.randomUUID();cat.setTame(true,true);cat.setOwnerUUID(owner);
        cat.setCustomName(Component.literal("Pouch 31"));CatClothesData.equip(cat,CatOutfitType.MEDICAL);
        var inventory=CatProfileData.openContainer(cat);
        inventory.setItem(0,new ItemStack(BuiltInRegistries.ITEM.get(LaoWuMod.id("cat_health_badge"))));
        inventory.setItem(CatProfileData.ACCESSORY_SLOTS,new ItemStack(Items.DIAMOND,23));
        inventory.setChanged();cat.setHealth(3.5F);
        var before=CatAttributeData.ensure(cat);
        var event=capture(p,cat);
        h.assertTrue(event.isCanceled()&&event.getCancellationResult().consumesAction(),"Real entity event intercepts before vanilla sit");
        h.assertTrue(cat.isRemoved()&&CatPouchItem.count(pouch)==1,"One live cat becomes exactly one stored pancake");
        var stored=CatPouchItem.extractOne(pouch);
        var after=CatAttributeData.read(stored).orElseThrow();
        for(CatStat stat:CatStat.values())h.assertTrue(after.current(stat)==before.current(stat)
                &&after.potential(stat)==before.potential(stat),"No death penalty or change to "+stat);
        h.assertTrue(CatPancakeItem.getOutfit(stored)==CatOutfitType.MEDICAL,"Outfit retained");
        var snapshot=data(stored).getCompound(CatPancakeItem.CAT_DATA_TAG);
        h.assertTrue(snapshot.getUUID("Owner").equals(owner)&&Math.abs(snapshot.getFloat("Health")-3.5F)<.001,"Original owner and actual health retained");
        h.assertTrue(!snapshot.contains("UUID")&&!snapshot.contains("Passengers")&&!snapshot.contains("Leash"),"No duplicate live identity or world attachments");
        Cat restored=EntityType.CAT.create(h.getLevel());restored.load(snapshot);
        h.assertTrue(restored.getCustomName().getString().equals("Pouch 31")&&restored.getOwnerUUID().equals(owner),"Full living NBT restores name and owner");
        var restoredInventory=CatProfileData.openContainer(restored);
        h.assertTrue(restoredInventory.getItem(0).is(BuiltInRegistries.ITEM.get(LaoWuMod.id("cat_health_badge"))),"Accessory preserved");
        h.assertTrue(restoredInventory.getItem(CatProfileData.ACCESSORY_SLOTS).is(Items.DIAMOND)
                &&restoredInventory.getItem(CatProfileData.ACCESSORY_SLOTS).getCount()==23,"Nine-slot cargo preserved");
        var flat=cat(h,p);flat.setNoAi(false);CatPancakeBehavior.flatten(flat);
        h.assertTrue(CatPoseData.isPancake(flat),"Fixture is a living flattened cat");
        capture(p,flat);
        h.assertTrue(flat.isRemoved()&&CatPouchItem.count(pouch)==1,"Flattened living cat is captured without shovel or paper stick");
        var flatSnapshot=data(CatPouchItem.peek(pouch)).getCompound(CatPancakeItem.CAT_DATA_TAG);
        h.assertTrue(!flatSnapshot.getBoolean("NoAI"),"Flattening AI lock is sanitized on capture");
        p.discard();CatProfileData.forgetStoredEntity(restored);restored.discard();finish(h,"normal and pancake-cat capture; owner, stats, suit, health, accessories and cargo round trip");
    }

    @GameTest(template="accessory_probe",batch="pouch31",timeoutTicks=20)
    public static void capacityAndInteractionGuards(GameTestHelper h) {
        var p=player(h);var cat=cat(h,p);var pouch=p.getMainHandItem();
        CatProfileData.beginViewing(cat);capture(p,cat);
        h.assertTrue(cat.isAlive()&&CatPouchItem.count(pouch)==0,"Open cat profile prevents storage snapshot races");
        CatProfileData.endViewing(cat);
        p.setGameMode(GameType.SPECTATOR);capture(p,cat);
        h.assertTrue(cat.isAlive()&&CatPouchItem.count(pouch)==0,"Spectator cannot capture");
        p.setGameMode(GameType.SURVIVAL);
        cat.moveTo(p.getX()+20,p.getY(),p.getZ());capture(p,cat);
        h.assertTrue(cat.isAlive()&&CatPouchItem.count(pouch)==0,"Remote interaction rejected");
        cat.moveTo(p.getX()+1,p.getY(),p.getZ());
        for(int i=0;i<CatPouchItem.CAPACITY;i++)
            h.assertTrue(CatPouchItem.insertOne(pouch,new ItemStack(LaoWuMod.CAT_PANCAKE.get())),"Fill up to 128");
        capture(p,cat);
        h.assertTrue(cat.isAlive()&&!cat.isRemoved()&&CatPouchItem.count(pouch)==128,"Full pouch cannot discard a cat");
        CatPouchItem.extractOne(pouch);capture(p,cat);capture(p,cat);
        h.assertTrue(cat.isRemoved()&&CatPouchItem.count(pouch)==128,"Retrying removed entity cannot duplicate it");
        p.discard();finish(h,"128 capacity, no item loss, viewer/spectator/distance guards and duplicate interaction rejection");
    }
}
