package cn.laowu.mod.test;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.compat.create.CreateIntegration;
import cn.laowu.mod.mixin.CreateOptionalMixinPlugin;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.gametest.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class StandaloneCatProbe {
    @GameTest(template="artillery_probe",timeoutTicks=600)
    public static void savedDeliveryCatResumesGravityWithoutLosingCargo(GameTestHelper h) {
        floor(h);
        var cat=h.spawn(EntityType.CAT,8,3,6);
        cn.laowu.mod.genetics.CatTraitData.set(cat,cn.laowu.mod.genetics.CatTraitProfile.EMPTY);
        cat.setPos(cat.getX(),cat.getY()+220,cat.getZ());
        var data=cat.getPersistentData();
        data.putInt("LaoWuLogisticsPhase",2);data.putLong("LaoWuLogisticsSourceSeat",1234);
        var payload=new net.minecraft.nbt.CompoundTag();payload.putString("id","create:cardboard_package_10x8");
        payload.putInt("Count",1);data.put("LaoWuLogisticsLoadStack",payload.copy());
        cat.setNoGravity(true);cat.setOrderedToSit(true);
        h.assertTrue(!cn.laowu.mod.CatLogisticsBehavior.tick(cat),"Standalone has no delivery job");
        h.assertTrue(!cat.isNoGravity()&&!cat.isOrderedToSit(),"Saved delivery flight releases forced gravity and sitting");
        h.assertTrue(data.getInt("LaoWuLogisticsPhase")==0,"Unavailable transient flight is stopped");
        h.assertTrue(payload.equals(data.getCompound("LaoWuLogisticsLoadStack")),"Pending package bytes remain untouched");
        h.assertTrue(!cat.causeFallDamage(220,1,h.getLevel().damageSources().fall()),"Vanilla cat fall immunity remains active");
        h.succeedWhen(()->{
            h.assertTrue(cat.isAlive()&&cat.onGround(),"Recovered delivery cat lands alive from the actual220-block altitude: ticks="+cat.tickCount+",pos="+cat.position());
            h.assertTrue(payload.equals(data.getCompound("LaoWuLogisticsLoadStack")),"Landing never rewrites pending parcel data");
            cat.discard();
        });
    }
    @GameTest(template="artillery_probe",timeoutTicks=30)
    public static void disabledMachinePacketsCannotLinkCreate(GameTestHelper h) {
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"standalone-packets"));
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(3,2,3))));
        new cn.laowu.mod.network.SetCreatureFilterPacket(0,0,false,java.util.List.of()).apply(player);
        new cn.laowu.mod.network.ConfigureCreatureTransmitterPacket(player.blockPosition(),5,0,10,false).apply(player);
        new cn.laowu.mod.network.CatEditorActionPacket(player.containerMenu.containerId,0).apply(player);
        h.assertTrue(player.containerMenu==player.inventoryMenu,"Disabled machine packets leave normal inventory untouched");
        player.discard();h.succeed();
    }
    private static void floor(GameTestHelper h) {
        for (int x=0;x<18;x++) for(int z=0;z<12;z++)
            h.setBlock(new net.minecraft.core.BlockPos(x,0,z),net.minecraft.world.level.block.Blocks.STONE);
    }
    @GameTest(template="artillery_probe",timeoutTicks=50)
    public static void emptyHandPilotAndDiverRidingWork(GameTestHelper h) {
        floor(h);
        var stub=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"standalone-stub"));
        // NeoForge FakePlayer intentionally refuses every startRiding call.
        var owner=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"standalone-rider"));
        owner.connection=stub.connection;
        var cat=new net.minecraft.world.entity.animal.Cat(EntityType.CAT,h.getLevel()) {
            @Override public net.minecraft.world.entity.LivingEntity getOwner() { return owner; }
        };
        cat.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(8,1,6))));
        h.getLevel().addFreshEntity(cat);
        cat.setTame(true);cat.setOwnerUUID(owner.getUUID());
        cn.laowu.mod.genetics.CatTraitData.set(cat,cn.laowu.mod.genetics.CatTraitProfile.EMPTY);
        cat.setAge(0);owner.setPos(cat.position());
        cat.setOrderedToSit(false);cat.setInSittingPose(false);
        cn.laowu.mod.CatPoseData.setPose(cat,0);
        cn.laowu.mod.CatClothesData.equip(cat,cn.laowu.mod.CatOutfitType.FLIGHT);
        h.assertTrue(cn.laowu.mod.CatPilotFlight.interact(cat,owner,net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction(),
                "Empty hand starts pilot ride");
        h.assertTrue(owner.getVehicle()==cat.getVehicle(),"Pilot shares carrier");
        cn.laowu.mod.CatPilotFlight.release(cat);
        cn.laowu.mod.CatClothesData.equip(cat,cn.laowu.mod.CatOutfitType.DIVING);
        owner.setPos(cat.position());
        h.assertTrue(cn.laowu.mod.CatDivingMount.interact(cat,owner,net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction(),
                "Empty hand starts diving ride");
        h.assertTrue(owner.getVehicle()==cat.getVehicle(),"Diver shares carrier");
        cn.laowu.mod.CatDivingMount.release(cat);
        var inventory=cn.laowu.mod.CatProfileData.openContainer(cat);
        inventory.setItem(0,new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(LaoWuMod.id("cat_giant_collar"))));
        inventory.setChanged();cn.laowu.mod.accessory.CatAccessories.equipmentChanged(cat);
        cn.laowu.mod.genetics.CatAttributeData.set(cat,cn.laowu.mod.genetics.CatAttributeData.ensure(cat).withValues(cn.laowu.mod.genetics.CatStat.HEALTH,100,100));
        cat.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(8,1,6))));owner.setPos(cat.position());
        h.assertTrue(cn.laowu.mod.CatGiantMount.interact(cat,owner,net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction(),"Giant empty-hand ride survives without Create");
        cn.laowu.mod.CatGiantMount.release(cat);
        inventory.setItem(0,net.minecraft.world.item.ItemStack.EMPTY);inventory.setChanged();
        cn.laowu.mod.accessory.CatAccessories.equipmentChanged(cat);
        cn.laowu.mod.CatClothesData.equip(cat,cn.laowu.mod.CatOutfitType.TRANSPORT);
        var cargo=cn.laowu.mod.CatChestData.openContainer(cat);
        cargo.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND));
        owner.setShiftKeyDown(true);
        cn.laowu.mod.CommonEvents.onCatInteract(new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract(owner,net.minecraft.world.InteractionHand.MAIN_HAND,cat));
        h.assertTrue(owner.containerMenu instanceof net.minecraft.world.inventory.ChestMenu,"Standalone transport gesture opens vanilla inventory, not logistics menu");
        h.assertTrue(cn.laowu.mod.CatChestData.openContainer(cat).getItem(0).is(net.minecraft.world.item.Items.DIAMOND),"Saved cat cargo remains accessible");
        owner.closeContainer();
        cat.discard();owner.discard();stub.discard();h.succeed();
    }
    @GameTest(template="artillery_probe",timeoutTicks=60)
    public static void captureAndRestorePreserveCatData(GameTestHelper h) {
        floor(h);
        var cat=h.spawn(EntityType.CAT,8,1,6);
        cat.setTame(true);var owner=java.util.UUID.randomUUID();cat.setOwnerUUID(owner);
        cn.laowu.mod.CatClothesData.equip(cat,cn.laowu.mod.CatOutfitType.MEDICAL);
        cn.laowu.mod.genetics.CatTraitData.set(cat,cn.laowu.mod.genetics.CatTraitProfile.EMPTY.withLevel(cn.laowu.mod.genetics.CatTrait.THORNS,3));
        var pancake=cn.laowu.mod.item.CatPancakeItem.capture(cat);var at=cat.position();cat.discard();
        var restored=cn.laowu.mod.item.CatPancakeItem.deployCat(h.getLevel(),pancake,at,0);
        h.assertTrue(restored!=null,"Captured cat restores without Create");
        h.assertTrue(owner.equals(restored.getOwnerUUID()),"Owner preserved");
        h.assertTrue(cn.laowu.mod.CatClothesData.getOutfit(restored)==cn.laowu.mod.CatOutfitType.MEDICAL,"Outfit preserved");
        h.assertTrue(cn.laowu.mod.genetics.CatTraitData.ensure(restored).level(cn.laowu.mod.genetics.CatTrait.THORNS)==3,"Trait preserved");
        cn.laowu.mod.CatPoseData.setPose(restored,cn.laowu.mod.CatPoseData.HISSING);
        h.runAfterDelay(20,()->{h.assertTrue(restored.isAlive(),"Hissing pose ticks without industrial gas work");restored.discard();h.succeed();});
    }
    @GameTest(template="artillery_probe",timeoutTicks=60)
    public static void artilleryKeepsCombatWithoutCreateModels(GameTestHelper h) {
        floor(h);
        var cat=h.spawn(EntityType.CAT,8,1,6);cat.setTame(true);cat.setOwnerUUID(java.util.UUID.randomUUID());
        cn.laowu.mod.genetics.CatTraitData.set(cat,cn.laowu.mod.genetics.CatTraitProfile.EMPTY);cat.setAge(0);
        cn.laowu.mod.CatClothesData.equip(cat,cn.laowu.mod.CatOutfitType.ENGINEERING);
        var target=h.spawn(EntityType.ZOMBIE,12,1,6);target.setNoAi(true);
        for(var type:cn.laowu.mod.CatArtilleryMunition.values()) {
            var projectile=new cn.laowu.mod.entity.EngineeringCogwheelProjectile(h.getLevel(),cat,5);
            projectile.setMunition(type);h.assertTrue(projectile.munition()==type,"Standalone munition type round-trips");
            projectile.discard();
        }
        var cannon=new cn.laowu.mod.entity.EngineeringCannon(LaoWuMod.ENGINEERING_CANNON.get(),h.getLevel());
        cannon.setPos(cat.position());h.getLevel().addFreshEntity(cannon);cat.startRiding(cannon,true);
        cannon.aimAt(target);float health=target.getHealth();
        h.assertTrue(cannon.fire(cat,target),"Engineering cat fires without Create");
        h.runAfterDelay(15,()->{
            h.assertTrue(target.getHealth()<health,"Standalone artillery projectile deals real damage");
            cannon.release();cat.discard();target.discard();h.succeed();
        });
    }
    @GameTest(template="artillery_probe",timeoutTicks=80)
    public static void allCatOutfitsAndSavedTraitsSurvive(GameTestHelper h) {
        var cats=new java.util.ArrayList<net.minecraft.world.entity.animal.Cat>();
        for(var outfit:cn.laowu.mod.CatOutfitType.values()) {
            var cat=h.spawn(EntityType.CAT, 5, 2, 5);
            cat.setTame(true);cat.setOwnerUUID(java.util.UUID.randomUUID());
            cn.laowu.mod.genetics.CatTraitData.set(cat,cn.laowu.mod.genetics.CatTraitProfile.EMPTY.withLevel(cn.laowu.mod.genetics.CatTrait.THORNS,3));
            cn.laowu.mod.CatClothesData.equip(cat,outfit);
            var pancake=cn.laowu.mod.item.CatPancakeItem.capture(cat);
            h.assertTrue(cn.laowu.mod.item.CatPancakeItem.getOutfit(pancake)==outfit,"Pancake retains outfit "+outfit);
            h.assertTrue(cn.laowu.mod.genetics.CatTraitData.read(pancake).orElseThrow().level(cn.laowu.mod.genetics.CatTrait.THORNS)==3,"Pancake retains trait level");
            cats.add(cat);
        }
        h.runAfterDelay(40,()->{for(var cat:cats)h.assertTrue(cat.isAlive(),"Cat outfit tick survives without Create");h.succeed();});
    }
    @GameTest(template="artillery_probe",timeoutTicks=60)
    public static void catGameplayStartsWithoutCreate(GameTestHelper h) {
        h.assertTrue(!CreateIntegration.isLoaded(), "This fixture must not contain Create");
        h.assertTrue(!new CreateOptionalMixinPlugin().shouldApplyMixin("com.simibubi.create.content.kinetics.belt.BeltBlock", "cn.laowu.mod.mixin.CatBeltCasingMixin"), "Create injections must be disabled");
        h.assertTrue(BuiltInRegistries.ITEM.containsKey(LaoWuMod.id("cat_pancake")), "Core cat items must remain registered");
        h.assertTrue(!BuiltInRegistries.ITEM.containsKey(LaoWuMod.id("creature_filter")), "Create filters must not be registered");
        h.assertTrue(!BuiltInRegistries.BLOCK.containsKey(LaoWuMod.id("cat_engine")), "Create machines must not be registered");
        var cat=h.spawn(EntityType.CAT, 3, 2, 3);
        h.runAfterDelay(20, () -> {
            h.assertTrue(cat.isAlive(), "Cat entity and tick effects must remain functional");
            h.succeed();
        });
    }
}
