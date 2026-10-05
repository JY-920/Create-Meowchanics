package cn.laowu.mod.test;

import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.lang.reflect.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public class CreatureTransmitterProbe {

    @GameTest(template="artillery_probe",batch="creature_transmitter",timeoutTicks=30)
    public static void clipboardRejectsForeignFiltersBeforeReturningStoredItem(GameTestHelper h) {
        var be=(cn.laowu.mod.create.CreatureTransmitterBlockEntity)setup(h);
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"Clipboard"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(be.getBlockPos()));
        be.setFilter(new ItemStack(cn.laowu.mod.LaoWuMod.CREATURE_FILTER.get()));
        var filtering=be.getBehaviour(com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour.TYPE);
        var clipboard=new net.minecraft.nbt.CompoundTag();clipboard.put("Filter",new ItemStack(net.minecraft.world.item.Items.STONE).save(h.getLevel().registryAccess()));
        for(int i=0;i<2;i++)filtering.readFromClipboard(h.getLevel().registryAccess(),clipboard,player,Direction.UP,false);
        h.assertTrue(!be.getFilter().isEmpty()&&player.getInventory().countItem(cn.laowu.mod.LaoWuMod.CREATURE_FILTER.get())==0,
            "Rejected clipboard must not return a copy while leaving filter installed");
        clipboard.put("Filter",new net.minecraft.nbt.CompoundTag());
        filtering.readFromClipboard(h.getLevel().registryAccess(),clipboard,player,Direction.UP,false);
        h.assertTrue(be.getFilter().isEmpty()&&player.getInventory().countItem(cn.laowu.mod.LaoWuMod.CREATURE_FILTER.get())==1,
            "Valid native clear returns exactly one installed filter");
        h.succeed();
    }
    @GameTest(template="artillery_probe",batch="creature_transmitter",timeoutTicks=30)
    public static void topSlotUsesNativeCreateFiltering(GameTestHelper h) {
        var be=(cn.laowu.mod.create.CreatureTransmitterBlockEntity)setup(h);
        var filtering=be.getBehaviour(com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour.TYPE);
        h.assertTrue(filtering!=null,"Top slot must use Create filtering behaviour, not a parallel custom slot");
        var offset=filtering.getSlotPositioning().getLocalOffset(h.getLevel(),be.getBlockPos(),be.getBlockState());
        h.assertTrue(offset.distanceTo(new Vec3(.5,15.5/16,.5))<1e-6,"Top slot uses stockpile switch surface inset");
        h.succeed();
    }
    @GameTest(template="artillery_probe",batch="creature_transmitter",timeoutTicks=30)
    public static void filterOnlyInstallsAtAuthoredTopSlot(GameTestHelper h) {
        var be=(cn.laowu.mod.create.CreatureTransmitterBlockEntity)setup(h);
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TopSlot"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var stack=new ItemStack(cn.laowu.mod.LaoWuMod.CREATURE_FILTER.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        var pos=be.getBlockPos();
        try {
            var block=be.getBlockState().getBlock();
            var method=block.getClass().getDeclaredMethod("useItemOn",
                ItemStack.class,net.minecraft.world.level.block.state.BlockState.class,net.minecraft.world.level.Level.class,
                BlockPos.class,net.minecraft.world.entity.player.Player.class,net.minecraft.world.InteractionHand.class,
                net.minecraft.world.phys.BlockHitResult.class);
            method.setAccessible(true);
            method.invoke(block,stack,be.getBlockState(),h.getLevel(),pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos).add(0,0,-.5),Direction.NORTH,pos,false));
            h.assertTrue(be.getFilter().isEmpty()&&stack.getCount()==1,"Side face must not consume a filter");
            method.invoke(block,stack,be.getBlockState(),h.getLevel(),pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos).add(0,.5,0),Direction.UP,pos,false));
            h.assertTrue(!be.getFilter().isEmpty()&&player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty(),"Top slot installs exactly one filter");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            var chunk=h.getLevel().getChunkAt(pos);chunk.setUnsaved(false);
            method.invoke(block,ItemStack.EMPTY,be.getBlockState(),h.getLevel(),pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos).add(0,.5,0),Direction.UP,pos,false));
            h.assertTrue(be.getFilter().isEmpty()&&player.getInventory().countItem(cn.laowu.mod.LaoWuMod.CREATURE_FILTER.get())==1,
                "Physical removal returns exactly one filter");
            h.assertTrue(chunk.isUnsaved(),"Physical removal marks storage dirty even when counted mobs do not change");
        } catch(ReflectiveOperationException e){throw new AssertionError(e);}
        h.succeed();
    }
    @GameTest(template="artillery_probe",batch="creature_transmitter",timeoutTicks=30)
    public static void analogOutputUsesConfiguredEndpoints(GameTestHelper h) {
        var be=(cn.laowu.mod.create.CreatureTransmitterBlockEntity)setup(h);
        config(be,4,0,15,false);
        var tag=be.saveWithFullMetadata(h.getLevel().registryAccess());tag.putString("OutputMode","analog");be.loadWithComponents(tag,h.getLevel().registryAccess());
        var cow=spawn(h,EntityType.COW,1);be.refresh();
        h.assertTrue(signal(h)==1,"Analog 0..15 with one mob outputs 1");
        config(be,4,0,15,true);
        h.assertTrue(signal(h)==14,"Analog inversion complements signal");
        config(be,4,0,150,false);be.refresh();
        h.assertTrue(signal(h)==0,"Analog fraction rounds down");
        var extra=new java.util.ArrayList<Mob>();
        for(int i=0;i<9;i++)extra.add(spawn(h,EntityType.COW,1));
        be.refresh();h.assertTrue(signal(h)==1,"Ten mobs in 0..150 range output one");
        for(var side:Direction.values())h.assertTrue(be.getBlockState().getSignal(h.getLevel(),be.getBlockPos(),side)==1,"Output applies to every face");
        config(be,4,0,20,false);be.refresh();
        h.assertTrue(signal(h)==7,"Analog midpoint floors 7.5 to 7");
        var restored=new cn.laowu.mod.create.CreatureTransmitterBlockEntity(be.getBlockPos(),be.getBlockState());
        restored.loadWithComponents(be.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(restored.getOutputMode()==cn.laowu.mod.create.CreatureTransmitterBlockEntity.OutputMode.ANALOG&&restored.getSignal()==7,"Analog mode and signal survive a new entity NBT reload");
        extra.forEach(net.minecraft.world.entity.Entity::discard);
        config(be,4,0,1,false);be.refresh();
        h.assertTrue(signal(h)==15,"Analog upper boundary saturates");
        cow.discard();be.refresh();h.assertTrue(signal(h)==0,"Analog zero count returns zero");
        h.succeed();
    }
    @GameTest(template="artillery_probe",batch="creature_transmitter",timeoutTicks=60)
    public static void automaticTicksAndSurvivalFilterInteraction(GameTestHelper h) {
        var be=(cn.laowu.mod.create.CreatureTransmitterBlockEntity)setup(h);
        var cow=spawn(h,EntityType.COW,1);
        h.runAfterDelay(12,()->{
            h.assertTrue(be.getCount()==1&&signal(h)==15,"Automatic ticker counts and powers without forced refresh");
            var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"FilterSurvival"));
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.setPos(be.getBlockPos().getX()+1,be.getBlockPos().getY(),be.getBlockPos().getZ());
            var stack=new ItemStack(cn.laowu.mod.LaoWuMod.CREATURE_FILTER.get(),2);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
            cn.laowu.mod.create.CreatureTransmitterRegistration.BLOCK.get().interact(h.getLevel(),be.getBlockPos(),player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(be.getBlockPos()).add(0,.5,0),Direction.UP,be.getBlockPos(),false));
            h.assertTrue(stack.getCount()==1&&be.getFilter().getCount()==1,"Survival installation consumes exactly one");
            h.assertTrue(be.getCount()==0&&signal(h)==0,"Filter change immediately updates output");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setShiftKeyDown(true);
            cn.laowu.mod.create.CreatureTransmitterRegistration.BLOCK.get().interact(h.getLevel(),be.getBlockPos(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
            h.assertTrue(!be.getFilter().isEmpty()&&player.getMainHandItem().isEmpty(),"Sneak interaction must preserve installed filter for range display");
            try {
                var packet=(cn.laowu.mod.network.ConfigureCreatureTransmitterPacket)cn.laowu.mod.network.ConfigureCreatureTransmitterPacket.class
                    .getMethod("removeFilter",BlockPos.class).invoke(null,be.getBlockPos());
                packet.apply(player);
            } catch(ReflectiveOperationException e){throw new AssertionError(e);}
            h.assertTrue(be.getFilter().isEmpty()&&player.getInventory().countItem(cn.laowu.mod.LaoWuMod.CREATURE_FILTER.get())==1&&signal(h)==15,"GUI removal returns exactly one filter and restores counting");
            cow.discard();
            h.runAfterDelay(12,()->{h.assertTrue(be.getCount()==0&&signal(h)==0,"Automatic ticker notices departed mob");h.succeed();});
        });
    }
    @GameTest(template="artillery_probe",batch="creature_transmitter",timeoutTicks=30)
    public static void persistenceAndPacketPermissions(GameTestHelper h) {
        var object=setup(h);
        var be=(cn.laowu.mod.create.CreatureTransmitterBlockEntity)object;
        config(be,6,2,5,true);
        var filter=new ItemStack(cn.laowu.mod.LaoWuMod.CREATURE_FILTER.get());
        cn.laowu.mod.item.CreatureFilterRules.conditions(cn.laowu.mod.item.CreatureFilterRules.Mode.WHITELIST,
            java.util.List.of(cn.laowu.mod.item.CreatureFilterRules.Condition.entity(ResourceLocation.tryParse("minecraft:cow"),false))).write(filter);
        be.setFilter(filter);
        var tag=be.saveWithFullMetadata(h.getLevel().registryAccess());
        var restored=new cn.laowu.mod.create.CreatureTransmitterBlockEntity(be.getBlockPos(),be.getBlockState());
        restored.loadWithComponents(tag,h.getLevel().registryAccess());
        h.assertTrue(restored.getRadius()==6&&restored.getLower()==2&&restored.getUpper()==5&&restored.isInverted(),"Configuration survives NBT reload");
        h.assertTrue(!restored.getFilter().isEmpty()&&cn.laowu.mod.item.CreatureFilterRules.read(restored.getFilter()).entityIds().contains(ResourceLocation.tryParse("minecraft:cow")),"Filter rules survive reload");
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TransmitterTest"));
        var p=be.getBlockPos();
        var packet=new cn.laowu.mod.network.ConfigureCreatureTransmitterPacket(p,3,0,7,false);
        player.setPos(p.getX()+100,p.getY(),p.getZ());packet.apply(player);
        h.assertTrue(be.getRadius()==6,"Far packet rejected");
        var remove=cn.laowu.mod.network.ConfigureCreatureTransmitterPacket.removeFilter(p);remove.apply(player);
        h.assertTrue(!be.getFilter().isEmpty(),"Far filter removal rejected");
        player.setPos(p.getX()+1,p.getY(),p.getZ());player.getAbilities().mayBuild=false;packet.apply(player);
        h.assertTrue(be.getRadius()==6,"Build-denied packet rejected");
        remove.apply(player);h.assertTrue(!be.getFilter().isEmpty(),"Build-denied filter removal rejected");
        player.getAbilities().mayBuild=true;packet.apply(player);
        h.assertTrue(be.getRadius()==3&&be.getUpper()==7,"Nearby authorized packet accepted");
        remove.apply(player);remove.apply(player);
        h.assertTrue(be.getFilter().isEmpty()&&player.getInventory().countItem(cn.laowu.mod.LaoWuMod.CREATURE_FILTER.get())==1,
            "Repeated authorized removal returns one filter");
        h.assertTrue(be.getRadius()==3&&be.getUpper()==7,"Filter removal preserves counting settings");
        h.succeed();
    }
    @GameTest(template="artillery_probe",batch="creature_transmitter",timeoutTicks=30)
    public static void filterDropAndSpeciesSelection(GameTestHelper h) {
        var be=(cn.laowu.mod.create.CreatureTransmitterBlockEntity)setup(h);
        config(be,4,0,1,false);
        var cow=spawn(h,EntityType.COW,1);var pig=spawn(h,EntityType.PIG,2);
        var filter=new ItemStack(cn.laowu.mod.LaoWuMod.CREATURE_FILTER.get());
        cn.laowu.mod.item.CreatureFilterRules.conditions(cn.laowu.mod.item.CreatureFilterRules.Mode.WHITELIST,
            java.util.List.of(cn.laowu.mod.item.CreatureFilterRules.Condition.entity(ResourceLocation.tryParse("minecraft:cow"),false))).write(filter);
        be.setFilter(filter);
        h.assertTrue(count(be)==1,"ID filter counts cow but not pig");
        var pos=be.getBlockPos();h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2),
            e->e.getItem().is(cn.laowu.mod.LaoWuMod.CREATURE_FILTER.get()));
        h.assertTrue(drops.size()==1&&drops.get(0).getItem().getCount()==1,"Break drops exactly one installed filter");
        cow.discard();pig.discard();drops.forEach(net.minecraft.world.entity.Entity::discard);h.succeed();
    }
    private static Object call(Object be, String name, Class<?>[] types, Object... values) {
        try {return be.getClass().getMethod(name, types).invoke(be, values);}
        catch (ReflectiveOperationException e) {throw new AssertionError(e);}
    }
    private static Object setup(GameTestHelper h) {
        var id=ResourceLocation.tryParse("laowu:creature_transmitter");
        h.assertTrue(BuiltInRegistries.BLOCK.containsKey(id),"Missing registered creature transmitter");
        var pos=new BlockPos(30,2,7);
        h.setBlock(pos,BuiltInRegistries.BLOCK.get(id));
        return h.getLevel().getBlockEntity(h.absolutePos(pos));
    }
    private static void config(Object be,int radius,int low,int high,boolean invert) {
        call(be,"configure",new Class[]{int.class,int.class,int.class,boolean.class},radius,low,high,invert);
    }
    private static int count(Object be) {
        call(be,"refresh",new Class[]{});
        return (int)call(be,"getCount",new Class[]{});
    }
    private static Mob spawn(GameTestHelper h,EntityType<?> type,double offset) {
        Mob m=(Mob)type.create(h.getLevel());m.setNoAi(true);m.setNoGravity(true);
        var p=Vec3.atCenterOf(h.absolutePos(new BlockPos(30,2,7)));
        m.setPos(p.x+offset,p.y,p.z);h.getLevel().addFreshEntity(m);return m;
    }
    private static int signal(GameTestHelper h) {
        var p=h.absolutePos(new BlockPos(30,2,7));
        return h.getLevel().getBlockState(p).getSignal(h.getLevel(),p,Direction.NORTH);
    }
    @GameTest(template="artillery_probe",batch="creature_transmitter",timeoutTicks=30)
    public static void countRadiusDeathsAndHysteresis(GameTestHelper h) {
        var be=setup(h);config(be,4,1,3,false);
        var a=spawn(h,EntityType.COW,1);var b=spawn(h,EntityType.CAT,2);
        var c=spawn(h,EntityType.PIG,3);var far=spawn(h,EntityType.SHEEP,4.1);
        h.assertTrue(count(be)==3&&signal(h)==15,"Three inside radius must enable signal; actual count="+count(be)
            +" signal="+signal(h)+" position="+((net.minecraft.world.level.block.entity.BlockEntity)be).getBlockPos()
            +" nearby="+h.getLevel().getEntitiesOfClass(Mob.class,new net.minecraft.world.phys.AABB(
                ((net.minecraft.world.level.block.entity.BlockEntity)be).getBlockPos()).inflate(4)).stream()
                .map(m->m.getType()+":"+m.position()).toList());
        c.discard();h.assertTrue(count(be)==2&&signal(h)==15,"Intermediate count retains signal");
        b.discard();h.assertTrue(count(be)==1&&signal(h)==0,"Lower boundary switches off");
        config(be,4,1,3,true);h.assertTrue(signal(h)==15,"Inversion applies immediately");
        a.discard();far.discard();h.succeed();
    }
    @GameTest(template="artillery_probe",batch="creature_transmitter",timeoutTicks=30)
    public static void filtersAndWalls(GameTestHelper h) {
        var be=setup(h);config(be,4,0,1,false);
        var a=spawn(h,EntityType.COW,2);
        h.setBlock(new BlockPos(31,2,7),Blocks.STONE);
        h.assertTrue(count(be)==1,"Walls must not occlude counting");
        var filter=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse("laowu:creature_filter")));
        call(be,"setFilter",new Class[]{ItemStack.class},filter);
        h.assertTrue(count(be)==0&&signal(h)==0,"Installed blank filter matches none");
        call(be,"setFilter",new Class[]{ItemStack.class},ItemStack.EMPTY);
        h.assertTrue(count(be)==1&&signal(h)==15,"Removing filter restores all mobs");
        a.discard();h.succeed();
    }
    @GameTest(template="artillery_probe",batch="creature_transmitter",timeoutTicks=30)
    public static void malformedConfigurationIsBounded(GameTestHelper h) {
        var be=setup(h);config(be,Integer.MAX_VALUE,-50,-20,false);
        h.assertTrue((int)call(be,"getRadius",new Class[]{})==16,"Radius capped");
        h.assertTrue((int)call(be,"getLower",new Class[]{})==0,"Lower clamped");
        h.assertTrue((int)call(be,"getUpper",new Class[]{})==1,"Upper must exceed lower");
        h.succeed();
    }
}
