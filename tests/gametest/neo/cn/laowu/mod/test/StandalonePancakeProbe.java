package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.item.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import java.util.UUID;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class StandalonePancakeProbe {
    private static Cat cat(GameTestHelper h) {
        var cat=EntityType.CAT.create(h.getLevel());
        cat.moveTo(h.absolutePos(new BlockPos(5,2,5)),0,0);
        h.getLevel().addFreshEntity(cat);
        CatTraitData.set(cat,CatTraitProfile.EMPTY);cat.setAge(0);
        return cat;
    }

    @GameTest(template="artillery_probe",timeoutTicks=30)
    public static void shovelSlapIsNonlethalAndPickupKeepsIdentity(GameTestHelper h) {
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"shovel-probe"));
        var cat=cat(h);
        cat.setTame(true,true);cat.setOwnerUUID(player.getUUID());
        cat.setCustomName(Component.literal("Shovel Cat"));
        CatClothesData.equip(cat,CatOutfitType.MEDICAL);
        var profile=CatAttributeData.ensure(cat);float health=cat.getHealth();
        player.moveTo(cat.getX()-1,cat.getY(),cat.getZ(),0,0);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SHOVEL));
        player.attack(cat);
        h.assertTrue(cat.isAlive()&&CatPoseData.isPancake(cat),"Shovel left click flattens instead of attacking");
        h.assertTrue(cat.getHealth()==health,"Flattening never damages the cat");
        var event=new PlayerInteractEvent.EntityInteract(player,InteractionHand.MAIN_HAND,cat);
        CommonEvents.onCatInteract(event);
        h.assertTrue(event.isCanceled()&&cat.isRemoved(),"Shovel right click picks up exactly one pancake");
        ItemStack stored=ItemStack.EMPTY;
        for(var item:player.getInventory().items)if(item.is(LaoWuMod.CAT_PANCAKE.get()))stored=item;
        h.assertTrue(!stored.isEmpty()&&stored.getCount()==1,"One stored cat, no blank or duplicate output");
        h.assertTrue(stored.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag().getCompound(CatPancakeItem.CAT_DATA_TAG).getUUID("Owner").equals(player.getUUID()),"Owner retained");
        h.assertTrue(CatPancakeItem.getOutfit(stored)==CatOutfitType.MEDICAL,"Outfit retained");
        h.assertTrue(CatPancakeItem.customCatName(stored).equals("Shovel Cat"),"Custom cat name retained");
        var after=CatAttributeData.read(stored).orElseThrow();
        for(var stat:CatStat.values())h.assertTrue(after.current(stat)==profile.current(stat)&&after.potential(stat)==profile.potential(stat),"No attribute death penalty");
        h.assertTrue(player.getMainHandItem().getDamageValue()>0,"Pickup uses the held shovel");
        player.discard();h.succeed();
    }

    @GameTest(template="artillery_probe",timeoutTicks=30)
    public static void everyShovelFlattensButOrdinarySwordsStillAttack(GameTestHelper h) {
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"tool-core"));
        var tools=new java.util.ArrayList<Item>(java.util.List.of(Items.WOODEN_SHOVEL,Items.STONE_SHOVEL,Items.IRON_SHOVEL,
                Items.GOLDEN_SHOVEL,Items.DIAMOND_SHOVEL,Items.NETHERITE_SHOVEL,LaoWuMod.CAT_SHOVEL.get()));
        var cardboard=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create","cardboard_sword");
        if (net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(cardboard))
            tools.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(cardboard));
        for(var tool:tools) {
            var cat=cat(h);float health=cat.getHealth();
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(tool));
            player.attack(cat);
            h.assertTrue(CatPoseData.isPancake(cat)&&cat.getHealth()==health,"Every shovel/cardboard contact is nonlethal: "+tool);
            cat.discard();
        }
        var normal=cat(h);float health=normal.getHealth();
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WOODEN_SWORD));player.attack(normal);
        h.assertTrue(!CatPoseData.isPancake(normal)&&normal.getHealth()<health,"Ordinary sword attacks are not intercepted");
        normal.discard();player.discard();h.succeed();
    }

    @GameTest(template="artillery_probe",timeoutTicks=30)
    public static void rangedDamageIsNotConvertedWhenShooterHoldsShovel(GameTestHelper h) {
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"ranged-core"));
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SHOVEL));
        var cat=cat(h);float health=cat.getHealth();
        var arrow=EntityType.ARROW.create(h.getLevel());arrow.setOwner(player);
        cat.hurt(cat.damageSources().arrow(arrow,player),1);
        h.assertTrue(!CatPoseData.isPancake(cat)&&cat.getHealth()<health,"Switching to a shovel cannot convert an arrow hit into a slap");
        cat.discard();player.discard();h.succeed();
    }

    @GameTest(template="artillery_probe",timeoutTicks=30)
    public static void alreadyCanceledShovelAttackCannotFlatten(GameTestHelper h) {
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"protected-core"));
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SHOVEL));
        var cat=cat(h);float health=cat.getHealth();
        var event=new net.neoforged.neoforge.event.entity.player.AttackEntityEvent(player,cat);event.setCanceled(true);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        h.assertTrue(!CatPoseData.isPancake(cat)&&cat.getHealth()==health,"Canceled/protected attacks cannot change the cat");
        cat.discard();player.discard();h.succeed();
    }

    @GameTest(template="artillery_probe",timeoutTicks=30)
    public static void anvilFlatteningAndPouchCaptureRemainAvailable(GameTestHelper h) {
        var cat=cat(h);
        cat.setCustomName(Component.literal("Pouch Cat"));
        var profile=CatAttributeData.ensure(cat);
        float health=cat.getHealth();
        var source=new DamageSource(h.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.FALLING_ANVIL));
        cat.hurt(source,100);
        h.assertTrue(cat.isAlive()&&CatPoseData.isPancake(cat)&&cat.getHealth()==health,"Anvil crushing is nonlethal");
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"pouch-core"));
        player.moveTo(cat.getX()-1,cat.getY(),cat.getZ(),0,0);
        var pouch=new ItemStack(LaoWuMod.CAT_POUCH.get());player.setItemInHand(InteractionHand.MAIN_HAND,pouch);
        CommonEvents.onCatInteract(new PlayerInteractEvent.EntityInteract(player,InteractionHand.MAIN_HAND,cat));
        h.assertTrue(cat.isRemoved()&&CatPouchItem.count(pouch)==1,"Flattened cat can still be stored directly in a pouch");
        var stored=CatPouchItem.peek(pouch);
        h.assertTrue(CatPancakeItem.customCatName(stored).equals("Pouch Cat"),"Pouch preserves cat name");
        var after=CatAttributeData.read(stored).orElseThrow();
        for(var stat:CatStat.values())h.assertTrue(after.current(stat)==profile.current(stat)&&after.potential(stat)==profile.potential(stat),"Pouch preserves attribute values");
        var normal=cat(h);
        CommonEvents.onCatInteract(new PlayerInteractEvent.EntityInteract(player,InteractionHand.MAIN_HAND,normal));
        h.assertTrue(normal.isRemoved()&&CatPouchItem.count(pouch)==2,"Normal cat pouch interaction remains unchanged");
        player.discard();h.succeed();
    }
}
