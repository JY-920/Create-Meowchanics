package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.accessory.CatAccessoryRegistry;
import cn.laowu.mod.accessory.CatAccessoryRarity;
import cn.laowu.mod.create.*;
import cn.laowu.mod.genetics.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class WishAdoptionProbe {
    private static final String REWARD = "laowu:cat_loot_magnet";
    private static WishAdoptionOffer offer(boolean max) {
        return new WishAdoptionOffer(max, List.of(
                new WishAdoptionOffer.Condition(CatStat.HEALTH,60,-1),
                new WishAdoptionOffer.Condition(CatStat.ATTACK,40,90),
                new WishAdoptionOffer.Condition(CatStat.SPEED,-1,50)), REWARD);
    }
    private static ItemStack cat(int now,int max) {
        ItemStack stack = new ItemStack(LaoWuMod.CAT_PANCAKE.get());
        var profile = CatAttributeProfile.founder(RandomSource.create(3));
        for (CatStat stat : CatStat.values()) profile = profile.withValues(stat,now,max);
        profile = profile.withValues(CatStat.ATTACK,50,80).withValues(CatStat.SPEED,40,50);
        CatAttributeData.set(stack,profile);CatTraitData.set(stack,CatTraitProfile.EMPTY);
        return stack;
    }
    private static WishAdoptionBoxBlockEntity box(GameTestHelper h) {
        BlockPos pos=h.absolutePos(new BlockPos(1,1,1));
        h.getLevel().setBlock(pos,LaoWuMod.WISH_ADOPTION_BOX.get().defaultBlockState(),3);
        return (WishAdoptionBoxBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    private static CompoundTag save(WishAdoptionBoxBlockEntity box) { return box.saveWithoutMetadata(); }
    private static void load(WishAdoptionBoxBlockEntity box,CompoundTag tag) { box.load(tag); }
    private static void setOffer(WishAdoptionBoxBlockEntity box,WishAdoptionOffer offer,boolean locked) {
        CompoundTag tag=save(box);tag.put("Offer",offer.save());tag.putBoolean("Locked",locked);load(box,tag);
    }
    private static void tick(WishAdoptionBoxBlockEntity box) {
        WishAdoptionBoxBlockEntity.serverTick(box.getLevel(),box.getBlockPos(),box.getBlockState(),box);
    }
    private static int outputs(WishAdoptionBoxBlockEntity box) {
        int result=0;for(int i=9;i<18;i++)result+=box.inventory().getStackInSlot(i).getCount();return result;
    }
    private static void finish(GameTestHelper h,String message) { System.out.println("PASS: wish adoption "+message);h.succeed(); }

    @GameTest(template="accessory_probe",batch="wish_adoption31",timeoutTicks=20)
    public static void randomOffersAndBounds(GameTestHelper h) {
        var rng=RandomSource.create(30000);var seen=new HashSet<String>();var counts=new HashSet<Integer>();
        var modes=new HashSet<Boolean>();WishAdoptionOffer previous=null;
        for(int i=0;i<5000;i++) {
            var rolled=WishAdoptionOffer.roll(rng,previous);
            h.assertTrue(rolled!=null&&!rolled.rewardStack().isEmpty(),"Every offer has a registered reward");
            h.assertTrue(rolled.equals(WishAdoptionOffer.load(rolled.save())),"Lossless offer NBT");
            h.assertTrue(rolled.conditions().stream().map(WishAdoptionOffer.Condition::stat).distinct().count()==rolled.conditions().size(),"Distinct stats");
            if(previous!=null){
                h.assertTrue(!rolled.reward().equals(previous.reward()),"Reward changes on reroll");
                h.assertTrue(rolled.maximum()!=previous.maximum()||!rolled.conditions().equals(previous.conditions()),"Card changes on reroll");
            }
            for(var c:rolled.conditions()){
                if(c.min()>=0)h.assertTrue(c.matches(c.min())&&!c.matches(c.min()-1),"Inclusive lower bound");
                if(c.max()>=0)h.assertTrue(c.matches(c.max())&&!c.matches(c.max()+1),"Inclusive upper bound");
                if(c.max()<0)h.assertTrue(c.matches(999),"Lower-only conditions impose no hidden maximum");
            }
            h.assertTrue(rolled.conditions().size()==CatAccessoryRarity.requirements(rolled.rewardStack()),"Requirement count matches native rarity");
            h.assertTrue(!CatAccessoryRarity.bossOnly(rolled.rewardStack()),"Boss rewards never enter random pool");
            counts.add(rolled.conditions().size());modes.add(rolled.maximum());seen.add(rolled.reward());previous=rolled;
        }
        h.assertTrue(counts.equals(Set.of(2,3,4))&&modes.size()==2,"Native rarity fixes requirements at 2/3/4; both NOW/MAX rolled");
        h.assertTrue(seen.equals(new HashSet<>(CatAccessoryRegistry.rewardItemIds())),"All non-Boss registered accessories eligible");
        h.assertTrue(seen.stream().filter(id->id.startsWith("laowu:")).count()==35&&!seen.contains("laowu:cat_butter_cube"),"35 eligible rewards; butter is Boss-only");
        h.assertTrue(seen.contains("laowu:cat_rebirth_ootheca"),"Milk tea included");
        var profile=CatAttributeData.read(cat(20,80)).orElseThrow();
        h.assertTrue(!offer(false).matches(profile)&&offer(true).matches(profile),"NOW checks current and MAX checks ceilings");
        var corrupt=offer(false).save();corrupt.putInt("Version",99);
        h.assertTrue(WishAdoptionOffer.load(corrupt)==null,"Reject unsupported data versions");
        finish(h,"5000 random cards, all rewards, exact bounds and NOW/MAX separation");
    }
    @GameTest(template="accessory_probe",batch="wish_adoption31",timeoutTicks=20)
    public static void immediateAndFullOutput(GameTestHelper h) {
        var box=box(h);setOffer(box,offer(false),true);
        var wrong=cat(59,100);box.inventory().setStackInSlot(0,wrong.copy());tick(box);
        h.assertTrue(ItemStack.isSameItemSameTags(box.inventory().getStackInSlot(0),wrong)&&outputs(box)==0,"Nonmatching cat untouched");
        box.inventory().setStackInSlot(0,cat(60,100));tick(box);
        h.assertTrue(box.inventory().getStackInSlot(0).isEmpty()&&outputs(box)==1,"Matching cat immediately trades on next tick");
        h.assertTrue(box.offer().equals(offer(false))&&box.locked(),"Locked successful offer retained");
        for(int i=9;i<18;i++)box.inventory().setStackInSlot(i,new ItemStack(Items.STONE,64));
        var before=cat(100,100);box.inventory().setStackInSlot(1,before.copy());tick(box);
        h.assertTrue(ItemStack.isSameItemSameTags(box.inventory().getStackInSlot(1),before)&&box.offer().equals(offer(false)),"Full output cannot consume or reroll");
        h.assertTrue(box.inventory().insertItem(9,new ItemStack(Items.DIAMOND),false).is(Items.DIAMOND),"Automation cannot insert into outputs");
        h.assertTrue(box.inventory().insertItem(2,new ItemStack(Items.STONE),false).is(Items.STONE),"Inputs reject non-cats");
        box.inventory().extractItem(9,64,false);tick(box);
        h.assertTrue(box.inventory().getStackInSlot(1).isEmpty()&&box.inventory().getStackInSlot(9).is(box.rewardPreview().getItem()),"Extraction unblocks reward without losing pancake");
        h.getLevel().setBlock(box.getBlockPos(),Blocks.AIR.defaultBlockState(),3);
        finish(h,"instant trade, full-buffer atomicity and real automation handler");
    }
    @GameTest(template="accessory_probe",batch="wish_adoption31",timeoutTicks=20)
    public static void refreshAndPersistence(GameTestHelper h) {
        var box=box(h);setOffer(box,offer(false),false);
        box.inventory().setStackInSlot(0,cat(60,100));tick(box);
        h.assertTrue(!box.offer().reward().equals(REWARD)&&outputs(box)==1,"Unlocked completion refreshes next reward");
        h.assertTrue(!box.offer().conditions().equals(offer(false).conditions())||box.offer().maximum(),"Unlocked completion refreshes card");
        setOffer(box,offer(true),true);box.inventory().setStackInSlot(2,cat(20,80));tick(box);
        h.assertTrue(box.inventory().getStackInSlot(2).isEmpty()&&outputs(box)==2,"MAX accepts low current, sufficient ceilings");
        box.inventory().setStackInSlot(4,cat(10,50));
        CompoundTag saved=save(box);
        var restored=new WishAdoptionBoxBlockEntity(box.getBlockPos(),box.getBlockState());
        restored.setLevel(h.getLevel());load(restored,saved);
        h.assertTrue(restored.offer().equals(box.offer())&&restored.locked(),"Offer and lock saved across recreation");
        h.assertTrue(outputs(restored)==2&&!restored.inventory().getStackInSlot(4).isEmpty(),"Both inventories preserved");
        var update=box.getUpdateTag();
        h.assertTrue(!update.contains("Inventory"),"Nearby client packets never include nested cat inventories");
        var clientCopy=new WishAdoptionBoxBlockEntity(box.getBlockPos(),box.getBlockState());load(clientCopy,update);
        h.assertTrue(clientCopy.offer().equals(box.offer())&&clientCopy.locked(),"Compact update carries world display and lock");
        h.getLevel().setBlock(box.getBlockPos(),Blocks.AIR.defaultBlockState(),3);
        finish(h,"refresh, MAX adoption, persistent lock/inventory and compact display sync");
    }
    @GameTest(template="accessory_probe",batch="wish_adoption31",timeoutTicks=20)
    public static void legacyOfferMigration(GameTestHelper h) {
        var random=RandomSource.create(31000);
        for(String id:List.of("cat_health_badge","cat_loot_magnet","cat_rebirth_ootheca")){
            var old=new WishAdoptionOffer(false,List.of(new WishAdoptionOffer.Condition(CatStat.HEALTH,60,-1)),"laowu:"+id).save();
            old.putInt("Version",1);
            var revised=WishAdoptionOffer.load(old).normalized(random);
            h.assertTrue(revised.conditions().size()==CatAccessoryRarity.requirements(revised.rewardStack()),"Old card adopts native rarity count");
            h.assertTrue(revised.reward().equals("laowu:"+id)&&revised.conditions().contains(new WishAdoptionOffer.Condition(CatStat.HEALTH,60,-1)),"Legal reward and existing requirement preserved");
        }
        var box=box(h);
        var boss=new WishAdoptionOffer(true,offer(true).conditions(),"laowu:cat_butter_cube");
        setOffer(box,boss,true);
        for(int i=9;i<18;i++)box.inventory().setStackInSlot(i,new ItemStack(Items.STONE,64));
        var before=cat(30,80);box.inventory().setStackInSlot(0,before.copy());
        tick(box);
        h.assertTrue(box.locked()&&!CatAccessoryRarity.bossOnly(box.rewardPreview()),"Locked legacy Boss offer is replaced while lock remains");
        h.assertTrue(box.offer().conditions().size()==CatAccessoryRarity.requirements(box.rewardPreview()),"Migrated Boss card uses new rarity rules");
        h.assertTrue(outputs(box)==9*64&&!box.inventory().getStackInSlot(0).isEmpty(),"Migration preserves full output and input");
        h.getLevel().setBlock(box.getBlockPos(),Blocks.AIR.defaultBlockState(),3);
        finish(h,"v1 migration, native rarity and locked Boss exclusion without inventory loss");
    }
    private static ServerPlayer player(GameTestHelper h) {
        var level=h.getLevel();var fake=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"wish-connection"));
        var player=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"wish-probe"));
        player.connection=fake.connection;var pos=h.absolutePos(new BlockPos(2,1,1));
        player.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);return player;
    }
    @GameTest(template="accessory_probe",batch="wish_adoption31",timeoutTicks=20)
    public static void menuScopeAndTwoViewers(GameTestHelper h) {
        var box=box(h);setOffer(box,offer(false),false);var a=player(h);var b=player(h);
        var ma=new WishAdoptionBoxMenu(70,a.getInventory(),box);var mb=new WishAdoptionBoxMenu(71,b.getInventory(),box);
        a.containerMenu=ma;b.containerMenu=mb;
        h.assertTrue(ma.slots.size()==54&&mb.slots.size()==54,"Only 18 actual machine slots plus vanilla 36; preview is not a slot");
        h.assertTrue(ma.clickMenuButton(a,1)&&mb.clickMenuButton(b,1)&&box.locked(),"Repeated lock actions from two players are idempotent");
        h.assertTrue(!ma.clickMenuButton(b,0)&&box.locked(),"Wrong open menu cannot unlock");
        h.assertTrue(!ma.clickMenuButton(a,99)&&box.locked(),"Malformed button rejected");
        b.moveTo(b.getX()+20,b.getY(),b.getZ(),0,0);
        h.assertTrue(!mb.clickMenuButton(b,0)&&box.locked(),"Remote player cannot unlock");
        a.setGameMode(GameType.SPECTATOR);
        h.assertTrue(!ma.clickMenuButton(a,0)&&box.locked(),"Spectators cannot edit offers");
        a.setGameMode(GameType.SURVIVAL);
        a.getInventory().setItem(9,cat(60,100));ma.quickMoveStack(a,18);tick(box);
        h.assertTrue(a.getInventory().getItem(9).isEmpty()&&outputs(box)==1,"Shift-click cat trades once");
        h.assertTrue(mb.getSlot(9).hasItem(),"Both menus share live reward inventory");
        h.assertTrue(!ma.getSlot(9).mayPlace(new ItemStack(Items.DIAMOND)),"GUI output rejects insertion");
        ma.quickMoveStack(a,9);h.assertTrue(outputs(box)==0,"Reward can be shift-extracted");
        h.assertTrue(ma.clickMenuButton(a,0)&&!box.locked()&&box.offer().equals(offer(false)),"Unlock does not reroll existing offer");
        h.getLevel().setBlock(box.getBlockPos(),Blocks.AIR.defaultBlockState(),3);
        h.getLevel().setBlock(box.getBlockPos(),LaoWuMod.WISH_ADOPTION_BOX.get().defaultBlockState(),3);
        h.assertTrue(!ma.stillValid(a)&&!ma.clickMenuButton(a,1),"Stale menu cannot edit a replacement box");
        a.discard();b.discard();finish(h,"two viewers, shift-click, no preview theft, spectator/distance/stale menu guards");
    }
}
