package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.entity.CatFlightCarrier;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.network.MedicalHealingPacket;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class MedicalClinicProbe {
    @GameTest(template="artillery_probe", batch="medical_clinic", timeoutTicks=30)
    public static void intelligenceLedgerAndSuitBalance(GameTestHelper h) {
        var level=h.getLevel(); var at=CareerSupportIntegrationProbe.floor(h);
        h.assertTrue(CatSupportRules.healingPerSecond(0)==1 && CatSupportRules.healingPerSecond(100)==4,
                "Intelligence maps 0 to 1 HP/s and 100 to 4 HP/s");
        h.assertTrue(CatSupportRules.healingPerSecond(50)==2.5 && CatSupportRules.healingPerSecond(200)==7,
                "Healing continues to scale with intelligence");
        h.assertTrue(CatSupportRules.medicalRadius(0)==3 && CatSupportRules.medicalRadius(50)==4.5
                && CatSupportRules.medicalRadius(100)==6 && CatSupportRules.medicalRadius(200)==9,
                "Healing range follows intelligence in three dimensions");
        h.assertTrue(CatSupportRules.medicalRadius(Double.MAX_VALUE)==32
                && CatSupportRules.healingPerSecond(Double.NaN)==1, "Safe extreme admin inputs");
        var engineer=CatSuitSettings.current(CatOutfitType.ENGINEERING);
        h.assertTrue(engineer.value(CatSuitSetting.HEALTH)==30 && engineer.value(CatSuitSetting.ARMOR)==12
                && engineer.value(CatSuitSetting.TOUGHNESS)==5, "Engineer preserves survival bonuses");
        for(var stat:CatSuitSetting.BONUSES) h.assertTrue(engineer.value(stat)==(stat==CatSuitSetting.LUCK_STAT?10:0),
                "Engineer only grants +10 Luck: "+stat);
        var medic=CatSuitSettings.current(CatOutfitType.MEDICAL);
        h.assertTrue(medic.value(CatSuitSetting.HEALTH)==6 && medic.value(CatSuitSetting.ARMOR)==2
                && medic.value(CatSuitSetting.TOUGHNESS)==0 && medic.value(CatSuitSetting.SPEED_STAT)==10,
                "Fragile medic: +6 health, +2 armor, +0 toughness and +10 Speed");
        var patient=EntityType.IRON_GOLEM.create(level); patient.setPos(at);
        patient.setNoAi(true);patient.setNoGravity(true);level.addFreshEntity(patient);patient.setHealth(5);
        CatMedicalHealing.offerHealing(patient,1);CatMedicalHealing.offerHealing(patient,4);CatMedicalHealing.offerHealing(patient,3);
        CatMedicalHealing.flush(level);
        h.assertTrue(patient.getHealth()==6,"Only strongest source heals, regardless of first arrival");
        CatMedicalHealing.offerHealing(patient,100);CatMedicalHealing.flush(level);
        h.assertTrue(patient.getHealth()==6,"Repeated flush cannot bypass patient cooldown");
        CompoundTag saved=patient.saveWithoutId(new CompoundTag());
        h.assertTrue(saved.getCompound("ForgeData").getLong(CatMedicalHealing.NEXT_HEAL)>0
                || patient.getPersistentData().getLong(CatMedicalHealing.NEXT_HEAL)>level.getGameTime(),"Patient cooldown is persisted");
        var packet=new MedicalHealingPacket(patient.getId(),patient.getUUID(),false,true,77,6,true);
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(Unpooled.buffer(),level.registryAccess());
        try {
            MedicalHealingPacket.STREAM_CODEC.encode(buffer,packet);
            h.assertTrue(MedicalHealingPacket.STREAM_CODEC.decode(buffer).equals(packet)&&!buffer.isReadable(),
                    "Medical packet preserves patient UUID, range, channel mode and age");
        }finally{buffer.release();}
        h.runAfterDelay(5,()->{
            CatMedicalHealing.offerHealing(patient,4);CatMedicalHealing.offerHealing(patient,1);
            CatMedicalHealing.flush(level);
            h.assertTrue(patient.getHealth()==7,"Strongest source is independent of offer order");
            patient.discard();
            System.out.println("PASS: medical intelligence/range, strongest nonstacking source, synchronized payload and revised suits");
            h.succeed();
        });
    }

    @GameTest(template="artillery_probe", batch="medical_clinic", setupTicks=5, timeoutTicks=100)
    public static void selfAndAirbornePilot(GameTestHelper h) {
        var level=h.getLevel();var base=CareerSupportIntegrationProbe.floor(h).add(5,0,5);
        var connection=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"med-air-conn"));
        var rider=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"med-air-pilot"),net.minecraft.server.level.ClientInformation.createDefault());
        rider.connection=connection.connection;
        rider.setItemInHand(InteractionHand.MAIN_HAND,AllItems.WRENCH.asStack());
        var medic=CareerSupportIntegrationProbe.cat(level,base,CatOutfitType.MEDICAL,true,rider);
        CareerSupportIntegrationProbe.stat(medic,CatStat.INTELLIGENCE,100);
        var pilot=CareerSupportIntegrationProbe.cat(level,base.add(0,1.3,0),CatOutfitType.FLIGHT,true,rider);
        rider.moveTo(pilot.getX(),pilot.getY(),pilot.getZ(),0,0);
        h.assertTrue(CatPilotFlight.interact(pilot,rider,InteractionHand.MAIN_HAND).consumesAction(),
                "Actual pilot carrier mounts with the real wrench interaction");
        var carrier=(CatFlightCarrier)pilot.getVehicle();
        medic.setHealth(5);pilot.setHealth(5);
        h.runAtTickTime(18,()->{
            h.assertTrue(pilot.isPassenger() && pilot.getY()>medic.getY()+2.5,
                    "Pilot remains airborne beyond the old approach distance");
            h.assertTrue(CatMedicalHealing.casting(medic) && CatMedicalHealing.glowing(medic)
                    && CatMedicalHealing.glowing(pilot),"Caster and airborne patient both receive channel visuals");
        });
        h.runAtTickTime(40,()->{
            h.assertTrue(medic.getHealth()>5 && pilot.getHealth()>5,"Medic heals itself together with its airborne ally");
            float own=medic.getHealth(), other=pilot.getHealth();
            Vec3 origin=medic.position();
            h.runAfterDelay(20,()->{
                h.assertTrue(Math.abs(medic.getHealth()-own-4)<.01
                        && Math.abs(pilot.getHealth()-other-4)<.01,"100 intelligence heals each patient exactly 4 HP per second");
                h.assertTrue(medic.position().distanceToSqr(origin)<.01 && medic.getDeltaMovement().horizontalDistanceSqr()<1e-7,
                        "Only the medic is rooted while healing");
                h.assertTrue(pilot.getVehicle()==carrier && !pilot.isOrderedToSit(),"Healing never detaches or sits the flying patient");
                carrier.release(true);pilot.discard();medic.discard();rider.discard();connection.discard();
                System.out.println("PASS: real pilot carrier at altitude, medic self-heal, exact 4 HP/s and stationary caster");
                h.succeed();
            });
        });
    }

    private static void atCenter(LivingEntity entity,Vec3 center) {
        entity.setPos(center.x,center.y-entity.getBbHeight()*.5,center.z);
        entity.setNoGravity(true);
    }
    @GameTest(template="artillery_probe", batch="medical_clinic", setupTicks=5, timeoutTicks=130)
    public static void seatedClinicTreatsLivingEntities(GameTestHelper h) {
        var level=h.getLevel();var base=CareerSupportIntegrationProbe.floor(h).add(6,0,5);
        var medic=CareerSupportIntegrationProbe.cat(level,base,CatOutfitType.MEDICAL,true);
        CareerSupportIntegrationProbe.stat(medic,CatStat.INTELLIGENCE,100);
        var seatPos=BlockPos.containing(base);
        level.setBlockAndUpdate(seatPos,AllBlocks.SEATS.get(DyeColor.WHITE).getDefaultState());
        SeatBlock.sitDown(level,seatPos,medic);
        medic.getVehicle().positionRider(medic);
        medic.setOrderedToSit(true);
        h.assertTrue(medic.isPassenger() && CatMedicalWork.available(medic),"Actual Create Seat enables work despite sit/passenger flags");
        var center=medic.getBoundingBox().getCenter();
        var cow=EntityType.COW.create(level);cow.setNoAi(true);atCenter(cow,center.add(.9,.2,0));level.addFreshEntity(cow);cow.setHealth(1);
        var husk=EntityType.HUSK.create(level);husk.setNoAi(true);atCenter(husk,center.add(0,.7,.9));level.addFreshEntity(husk);husk.setHealth(1);
        var corner=CareerSupportIntegrationProbe.cat(level,base,CatOutfitType.NONE,false);
        atCenter(corner,center.add(1.4,.2,1.4));corner.setHealth(1);
        var outside=CareerSupportIntegrationProbe.cat(level,base,CatOutfitType.NONE,false);
        atCenter(outside,center.add(-1.6,.2,0));outside.setHealth(1);
        var above=CareerSupportIntegrationProbe.cat(level,base,CatOutfitType.NONE,false);
        atCenter(above,center.add(0,1.6,0));above.setHealth(1);
        var player=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"clinic-patient"));
        atCenter(player,center.add(-.9,.5,-.6));level.addNewPlayer(player);player.setHealth(5);
        medic.setHealth(5);
        h.runAtTickTime(15,()->h.assertTrue(CatMedicalHealing.casting(medic)&&CatMedicalHealing.stationed(medic),
                "Seated medic visibly channels, rather than losing the cast due to its mount"));
        h.runAtTickTime(48,()->{
            h.assertTrue(cow.getHealth()>1&&husk.getHealth()>1&&corner.getHealth()>1&&player.getHealth()>5&&medic.getHealth()>5,
                    "Clinic heals self, player, ordinary cat, passive mob and hostile mob");
            h.assertTrue(CatMedicalHealing.glowing(player)&&CatMedicalHealing.glowing(cow)&&CatMedicalHealing.glowing(husk),
                    "Non-cat patients also have synchronized green-frame state");
            h.assertTrue(outside.getHealth()==1&&above.getHealth()==1,"Fixed 3x3x3 bounds: outside="+outside.getHealth()+", above="+above.getHealth()+", area="+CatMedicalWork.area(medic)+", outsideCenter="+outside.getBoundingBox().getCenter()+", aboveCenter="+above.getBoundingBox().getCenter());
            h.assertTrue(medic.isPassenger()&&medic.isOrderedToSit(),"Clinic does not unmount or clear the sit command");
            level.setBlockAndUpdate(seatPos,Blocks.AIR.defaultBlockState());
            h.runAfterDelay(2,()->{
                float hp=player.getHealth();
                h.runAfterDelay(35,()->{
                    h.assertTrue(player.getHealth()==hp&&!CatMedicalHealing.casting(medic)&&!CatMedicalHealing.glowing(player),
                            "Removing the Seat stops work and expires patient visuals");
                    for(var entity:java.util.List.of(medic,cow,husk,corner,outside,above,player))entity.discard();
                    System.out.println("PASS: actual Create Seat clinic, fixed cube, self/mob/player healing, shared green visuals and clean stop");
                    h.succeed();
                });
            });
        });
    }
}
