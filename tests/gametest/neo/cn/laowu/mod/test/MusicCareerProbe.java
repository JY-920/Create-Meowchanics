package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.network.*;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class MusicCareerProbe {
    @GameTest(template="artillery_probe", batch="music_rules", timeoutTicks=160)
    public static void rulesAndDpsSelection(GameTestHelper h) {
        var level=h.getLevel();var at=CareerSupportIntegrationProbe.floor(h).add(1,0,4);
        h.assertTrue(Math.abs(CatMusicRules.haste(0)-.05)<1e-8 && Math.abs(CatMusicRules.haste(100)-.25)<1e-8
                && Math.abs(CatMusicRules.haste(1000)-2.05)<1e-8 && CatMusicRules.haste(1000000)>1000,
                "5/25 percent at 0/100, with no percentage ceiling above 100");
        h.assertTrue(CatMusicRules.radius(0)==5 && CatMusicRules.radius(100)==8 && CatMusicRules.radius(1000000)==24
                && CatMusicRules.radius(100)==8&&CatMusicRules.radius(100)>CatSupportRules.medicalRadius(100),
                "Reduced 5/8-block radius at 0/100 intelligence, capped at 24 and still larger than medical");
        var music=CareerSupportIntegrationProbe.cat(level,at,CatOutfitType.MUSIC,false);
        h.assertTrue(!CatOutfitType.MUSIC.isPreviewOnly()&&CatOutfitType.MUSIC.isSupport()
                && ServerConfig.CAREERS.contains(CatOutfitType.MUSIC),"Active support career and live config");
        h.assertTrue(CatAttributeEffects.effectiveValue(music,CatStat.SPEED)==60,"Actual outfit grants +10 Speed");
        var settings=CatSuitSettings.current(CatOutfitType.MUSIC);
        h.assertTrue(settings.value(CatSuitSetting.HEALTH)==6&&settings.value(CatSuitSetting.ARMOR)==2
                &&settings.value(CatSuitSetting.TOUGHNESS)==0,"Low-survival music defaults");
        var near=CareerSupportIntegrationProbe.cat(level,at.add(1,0,0),CatOutfitType.FLIGHT,false);
        var high=CareerSupportIntegrationProbe.cat(level,at.add(14,0,0),CatOutfitType.FIRE,false);
        var ranged1=CareerSupportIntegrationProbe.cat(level,at.add(7,0,0),CatOutfitType.ENGINEERING,false);
        var ranged2=CareerSupportIntegrationProbe.cat(level,at.add(8,0,0),CatOutfitType.FISHING,false);
        var ranged3=CareerSupportIntegrationProbe.cat(level,at.add(9,0,0),CatOutfitType.HONEY,false);
        var allies=List.of(near,high,ranged1,ranged2,ranged3);
        var victim=EntityType.IRON_GOLEM.create(level);victim.setNoAi(true);victim.setPos(at.add(3,0,3));level.addFreshEntity(victim);
        victim.hurt(level.damageSources().mobAttack(high),12);
        h.assertTrue(CatMusicDps.recent(high)>0,"Real final damage event credits its cat owner");
        CatMusicDps.record(high,40);CatMusicDps.record(near,2);
        h.assertTrue(CatMusicSupportGoal.choose(music,allies,0,3)==near,"Low intelligence can stay near the closest ally");
        h.assertTrue(CatMusicSupportGoal.choose(music,allies,100,3)==high,"High intelligence chooses highest recent DPS");
        h.runAfterDelay(101,()->{
            h.assertTrue(CatMusicDps.recent(high)==0,"DPS sample expires instead of retaining historical winners forever");
            var chosen=CatMusicSupportGoal.choose(music,allies,100,3);
            h.assertTrue(chosen==ranged1||chosen==ranged2||chosen==ranged3,"No samples: prioritize the ranged cluster");
            for(var cat:allies)cat.discard();music.discard();victim.discard();
            System.out.println("PASS: music unbounded haste, live Speed/survival bonuses, actual DPS events and ranged fallback");
            h.succeed();
        });
    }

    @GameTest(template="artillery_probe", batch="music_group", timeoutTicks=160)
    public static void actualGroupPerformance(GameTestHelper h) {
        var level=h.getLevel();var at=CareerSupportIntegrationProbe.floor(h).add(5,0,4);
        var music=CareerSupportIntegrationProbe.cat(level,at,CatOutfitType.MUSIC,true);
        var weaker=CareerSupportIntegrationProbe.cat(level,at.add(0,0,2),CatOutfitType.MUSIC,true);
        CareerSupportIntegrationProbe.stat(music,CatStat.SPEED,90);
        CareerSupportIntegrationProbe.stat(music,CatStat.INTELLIGENCE,100);
        CareerSupportIntegrationProbe.stat(weaker,CatStat.SPEED,40);
        var ally=CareerSupportIntegrationProbe.cat(level,at.add(2,0,0),CatOutfitType.ENGINEERING,true);
        CatCombatControl.tick(ally);
        ally.goalSelector.removeAllGoals(goal -> true);ally.targetSelector.removeAllGoals(goal -> true);
        var actions=new java.util.concurrent.atomic.AtomicInteger();
        ally.goalSelector.addGoal(0,new net.minecraft.world.entity.ai.goal.Goal() {
            {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
            @Override public boolean canUse(){return true;}
            @Override public boolean requiresUpdateEveryTick(){return true;}
            @Override public void tick(){actions.incrementAndGet();}
        });
        var airborne=CareerSupportIntegrationProbe.cat(level,at.add(0,5,0),CatOutfitType.FLIGHT,false);
        var enemy=EntityType.HUSK.create(level);enemy.setNoAi(true);enemy.setPos(at.add(8,0,1));level.addFreshEntity(enemy);
        ally.setTarget(enemy);airborne.setTarget(enemy);
        CatMusicDps.record(ally,30);
        h.runAtTickTime(30,()->{
            h.assertTrue(CatMusicSupport.performing(music)&&CatMusicSupport.pose(music)>=0,
                    "Actual support goal selects and starts a server-owned performance");
            h.assertTrue(Math.abs(CatMusicSupport.bonus(ally)-.25)<1e-8&&Math.abs(CatMusicSupport.bonus(airborne)-.25)<1e-8,
                    "Overlapping real musicians grant only strongest 25 percent, including aerial allies");
            h.assertTrue(CatMusicSupport.glowing(ally)&&!CatMusicSupport.glowing(music),
                    "Purple state belongs to recipients, not the performer");
            h.assertTrue(CatMusicSupport.attackInterval(ally,40)==32&&CatMusicSupport.bonus(music)==0,
                    "Actual weapon intervals shortened; support cannot buff itself");
            h.assertTrue(music.getTarget()==null&&!CareerCatBehavior.canParticipateInCombat(music),
                    "Music remains non-attacking support");
            Vec3 position=music.position();int pose=CatMusicSupport.pose(music);
            h.runAfterDelay(10,()->{
                h.assertTrue(music.position().distanceToSqr(position)<.001&&CatMusicSupport.pose(music)==pose,
                        "Performer is stationary and animation choice does not reroll every tick");
                h.assertTrue(ally.getTarget()==enemy&&actions.get()>30&&!ally.isNoAi()
                                &&!ally.isOrderedToSit()&&!airborne.isOrderedToSit(),
                        "Recipients retain their own combat and airborne posture");
                music.setOrderedToSit(true);weaker.setOrderedToSit(true);
                h.runAfterDelay(45,()->{
                    h.assertTrue(!CatMusicSupport.performing(music)&&CatMusicSupport.bonus(ally)==0
                            &&!CatMusicSupport.glowing(airborne),"Performance and recipient effects stop after release");
                    for(var entity:List.of(music,weaker,ally,airborne,enemy))entity.discard();
                    System.out.println("PASS: real music AI, strongest group haste, airborne coverage, stable animation, stationary performer and expiry");
                    h.succeed();
                });
            });
        });
    }

    @GameTest(template="artillery_probe", batch="music_records", timeoutTicks=80)
    public static void nineSlotRecordsAndPackets(GameTestHelper h) throws Exception {
        var level=h.getLevel();var at=CareerSupportIntegrationProbe.floor(h).add(5,0,4);
        var music=CareerSupportIntegrationProbe.cat(level,at,CatOutfitType.MUSIC,true);
        var inventory=CatProfileData.openContainer(music);
        inventory.setItem(0,new ItemStack(Items.MUSIC_DISC_11)); // Even a forged accessory slot must not become a playlist slot.
        inventory.setItem(CatProfileData.ACCESSORY_SLOTS,new ItemStack(Items.STONE,7));
        inventory.setItem(CatProfileData.ACCESSORY_SLOTS+2,new ItemStack(Items.MUSIC_DISC_13));
        inventory.setItem(CatProfileData.ACCESSORY_SLOTS+8,new ItemStack(Items.MUSIC_DISC_CAT));
        h.assertTrue(CatProfileData.INVENTORY_SLOTS==9&&CatMusicRecords.nextSlot(music,-1)==2
                &&CatMusicRecords.nextSlot(music,2)==8&&CatMusicRecords.nextSlot(music,8)==2,"Only nine inventory slots loop in order");
        h.assertTrue(!CatChestData.hasInventory(music)&&!music.getPersistentData().contains(CatChestData.ITEMS_TAG),
                "Music neither opens nor creates a 27-slot chest inventory");
        CatMusicRecords.tick(music);
        h.assertTrue(CatMusicRecords.playingSlot(music)==2,"Inventory starts music without a Seat");
        CompoundTag before=music.getPersistentData().copy();
        var seat=BlockPos.containing(at);level.setBlockAndUpdate(seat,AllBlocks.SEATS.get(DyeColor.WHITE).getDefaultState());
        SeatBlock.sitDown(level,seat,music);music.getVehicle().positionRider(music);music.setOrderedToSit(true);
        CatMusicRecords.tick(music);
        h.assertTrue(CatMusicRecords.playingSlot(music)==2,"Actual Create Seat starts first record");
        forceTrackEnd(music);CatMusicRecords.tick(music);
        h.assertTrue(CatMusicRecords.playingSlot(music)==8,"End of record advances to final slot");
        forceTrackEnd(music);CatMusicRecords.tick(music);
        h.assertTrue(CatMusicRecords.playingSlot(music)==2,"Final record wraps without a long gap");
        h.assertTrue(before.get(CatProfileData.ITEMS_TAG).equals(music.getPersistentData().get(CatProfileData.ITEMS_TAG)),
                "Playback does not consume or change stored items");
        CatProfileData.beginViewing(music);
        inventory.removeItem(CatProfileData.ACCESSORY_SLOTS+2,1);
        h.assertTrue(CatMusicRecords.playingSlot(music)==8,"Removing the playing record advances while profile remains open");
        CatProfileData.endViewing(music);
        var restored=EntityType.CAT.create(level);restored.load(music.saveWithoutId(new CompoundTag()));
        CatProfileData.forgetStoredEntity(music);
        h.assertTrue(CatProfileData.openContainer(restored).getItem(CatProfileData.ACCESSORY_SLOTS+8).is(Items.MUSIC_DISC_CAT),
                "Existing nine-slot contents survive saved entity reload");restored.discard();
        var haste=new MusicSupportPacket(music.getId(),music.getUUID(),1,49,20.5,12);
        var record=new MusicRecordPacket(music.getId(),music.getUUID(),"minecraft:music_disc.cat",1234,800);
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),level.registryAccess());
        try {
            MusicSupportPacket.STREAM_CODEC.encode(buffer,haste);
            h.assertTrue(MusicSupportPacket.STREAM_CODEC.decode(buffer).equals(haste),"Haste packet preserves unbounded percent and server pose");
            MusicRecordPacket.STREAM_CODEC.encode(buffer,record);
            h.assertTrue(MusicRecordPacket.STREAM_CODEC.decode(buffer).equals(record)&&!buffer.isReadable(),"Record packet preserves UUID, asset, sequence and remaining length");
            var swarm=new CockroachStatePacket(music.getId(),music.getUUID(),10000,2,1199);
            CockroachStatePacket.STREAM_CODEC.encode(buffer,swarm);
            h.assertTrue(CockroachStatePacket.STREAM_CODEC.decode(buffer).equals(swarm)&&!buffer.isReadable(),"Swarm count/mode/age packet round trip");
        }finally{buffer.release();}
        level.setBlockAndUpdate(seat,Blocks.AIR.defaultBlockState());
        h.runAfterDelay(3,()->{
            CatMusicRecords.tick(music);
            h.assertTrue(CatMusicRecords.playingSlot(music)==8,"Leaving Seat keeps the mobile nine-slot playlist");
            CatClothesData.unequip(music);CatMusicRecords.tick(music);
            h.assertTrue(CatMusicRecords.playingSlot(music)==-1,"Removing music outfit stops playback");
            music.discard();
            System.out.println("PASS: actual nine-slot playlist, ordered looping, live edits, save/reload, no chest or consumption and serialized music packets");
            h.succeed();
        });
    }
    private static void forceTrackEnd(Cat cat) throws Exception {
        var field=CatMusicRecords.class.getDeclaredField("SESSIONS");field.setAccessible(true);
        Object session=((Map<?,?>)field.get(null)).get(cat);
        var until=session.getClass().getDeclaredField("until");until.setAccessible(true);
        until.setLong(session,cat.level().getGameTime());
    }
}
