package cn.laowu.mod.test;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import cn.laowu.mod.create.CatBeltStyle;
import cn.laowu.mod.create.CatMachineBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CatBeltProbe {
    @GameTest(template="artillery_probe",batch="cat_belt",timeoutTicks=40)
    public static void legacyConnectorOnlyCreatesPlainNativeBelt(GameTestHelper h) {
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"legacy-belt"));
        player.setGameMode(GameType.SURVIVAL);
        BlockPos start=new BlockPos(3,3,3),end=new BlockPos(3,3,8);
        h.setBlock(start,AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS,Direction.Axis.X));
        h.setBlock(end,AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS,Direction.Axis.X));
        Item legacy=CatMachineBlocks.CAT_BELT_ITEM.get();
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(legacy,2));
        for(BlockPos local:new BlockPos[]{start,end}) {
            BlockPos pos=h.absolutePos(local);
            h.assertTrue(legacy.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false))).consumesAction(),"Legacy connector click failed");
        }
        h.runAfterDelay(15,()->{
            BlockPos pos=h.absolutePos(start);
            h.assertTrue(h.getLevel().getBlockState(pos).is(AllBlocks.BELT.get()),"Legacy connector must still construct a native belt");
            h.assertTrue(!h.getLevel().getBlockState(pos).getValue(BeltBlock.CASING),
                    "Legacy connector must not grant free cat casing");
            h.assertTrue(!((CatBeltStyle)h.getLevel().getBlockEntity(pos)).laowu$isCatBelt(),
                    "Legacy connector must not set cat appearance");
            BlockPos endpoint=h.absolutePos(end);
            var state=h.getLevel().getBlockState(endpoint);
            var hit=new BlockHitResult(Vec3.atCenterOf(endpoint).add(0,0,.49),Direction.SOUTH,endpoint,false);
            h.assertTrue(state.getBlock().use(state,h.getLevel(),endpoint,player,InteractionHand.MAIN_HAND,hit).consumesAction(),
                    "Legacy connector must extend an existing native belt endpoint");
            h.assertTrue(h.getLevel().getBlockState(endpoint.south()).is(AllBlocks.BELT.get()),"Legacy endpoint extension must create the next native belt segment");
            int legacyRemaining=player.getMainHandItem().getCount();
            // Create extends an existing chain without consuming a connector;
            // compare against its real block-use path instead of inventing a cost.
            h.runAfterDelay(5,()->{
                player.setItemInHand(InteractionHand.MAIN_HAND,AllItems.BELT_CONNECTOR.asStack(2));
                BlockPos first=h.absolutePos(start);
                var startState=h.getLevel().getBlockState(first);
                var nativeHit=new BlockHitResult(Vec3.atCenterOf(first).add(0,0,-.49),Direction.NORTH,first,false);
                h.assertTrue(startState.getBlock().use(startState,h.getLevel(),first,player,InteractionHand.MAIN_HAND,nativeHit).consumesAction(),"Native connector control must extend the other endpoint");
                h.assertTrue(h.getLevel().getBlockState(first.north()).is(AllBlocks.BELT.get()),"Native extension control must create a segment");
                h.assertTrue(1-legacyRemaining==2-player.getMainHandItem().getCount(),"Legacy and native extension must have identical item cost");
                h.succeed();
            });
        });
    }

    @GameTest(template="artillery_probe",batch="cat_belt",timeoutTicks=80)
    public static void nativeConnectorCreatesPersistentCatChain(GameTestHelper h) {
        Item item=AllItems.BELT_CONNECTOR.get();
        h.assertTrue(BuiltInRegistries.ITEM.get(new ResourceLocation("laowu","cat_belt"))!=Items.AIR,
                "Legacy cat belt ID must remain registered for saved inventories");
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"cat-belt"));
        player.setGameMode(GameType.SURVIVAL);
        BlockPos start=new BlockPos(3,3,3), end=new BlockPos(3,3,8);
        h.setBlock(start,AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS,Direction.Axis.X));
        h.setBlock(end,AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS,Direction.Axis.X));
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item,2));
        for(BlockPos local:new BlockPos[]{start,end}) {
            BlockPos pos=h.absolutePos(local);
            h.assertTrue(item.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false))).consumesAction(),"Connection click failed");
        }
        h.assertTrue(player.getMainHandItem().getCount()==1,"One complete chain must consume exactly one connector");
        h.runAfterDelay(15,()->{
            BlockPos first=h.absolutePos(start);
            var firstState=h.getLevel().getBlockState(first);
            h.assertTrue(!firstState.getValue(BeltBlock.CASING),"Native connector must create an uncased belt");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(CatMachineBlocks.CAT_CASING_ITEM.get(),2));
            h.assertTrue(firstState.getBlock().use(firstState,h.getLevel(),first,player,InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(first),Direction.UP,first,false)).consumesAction(),
                    "Cat casing must apply directly to a native Create belt");
            for(int z=3;z<=8;z++) {
                BlockPos pos=h.absolutePos(new BlockPos(3,3,z));
                h.assertTrue(h.getLevel().getBlockState(pos).is(AllBlocks.BELT.get()),"Must retain native Create belt compatibility");
                h.assertTrue(h.getLevel().getBlockState(pos).getValue(BeltBlock.CASING)==pos.equals(first),
                        "Cat casing must affect only the clicked native segment");
                var be=(BeltBlockEntity)h.getLevel().getBlockEntity(pos);
                var saved=be.saveWithFullMetadata();
                h.assertTrue(saved.getBoolean("LaoWuCatBelt")==pos.equals(first),"Cat appearance must persist only on cased segment");
                var loaded=net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,be.getBlockState(),saved);
                h.assertTrue(loaded.saveWithFullMetadata().getBoolean("LaoWuCatBelt")==pos.equals(first),"Reload must preserve cat appearance");
                h.assertTrue(be.beltLength==6,"Native controller initialization must stay intact");
            }
            player.setItemInHand(InteractionHand.MAIN_HAND,AllBlocks.ANDESITE_CASING.asStack());
            var catState=h.getLevel().getBlockState(first);
            h.assertTrue(catState.getBlock().use(catState,h.getLevel(),first,player,InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(first),Direction.UP,first,false)).consumesAction(),
                    "Native andesite casing must remain usable");
            h.assertTrue(!((CatBeltStyle)h.getLevel().getBlockEntity(first)).laowu$isCatBelt(),
                    "Native andesite must clear cat appearance even when casing type is unchanged");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(CatMachineBlocks.CAT_CASING_ITEM.get()));
            var andesiteState=h.getLevel().getBlockState(first);
            h.assertTrue(andesiteState.getBlock().use(andesiteState,h.getLevel(),first,player,InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(first),Direction.UP,first,false)).consumesAction(),
                    "Cat casing must replace native andesite casing");
            h.assertTrue(((CatBeltStyle)h.getLevel().getBlockEntity(first)).laowu$isCatBelt(),
                    "Cat appearance must return after replacing andesite");
            h.assertTrue(((BeltBlock)h.getLevel().getBlockState(first).getBlock()).onWrenched(
                    h.getLevel().getBlockState(first),new UseOnContext(player,InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(first),Direction.UP,first,false))).consumesAction(),
                    "Native wrench path must remove a casing");
            h.assertTrue(!((CatBeltStyle)h.getLevel().getBlockEntity(first)).laowu$isCatBelt(),
                    "Native wrench removal must clear cat appearance");
            var bareState=h.getLevel().getBlockState(first);
            h.assertTrue(bareState.getBlock().use(bareState,h.getLevel(),first,player,InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(first),Direction.UP,first,false)).consumesAction(),
                    "Cat casing must be applicable again after wrench removal");
            var state=h.getLevel().getBlockState(first);
            var drops=net.minecraft.world.level.block.Block.getDrops(state,h.getLevel(),first,
                    h.getLevel().getBlockEntity(first),player,new ItemStack(Items.IRON_PICKAXE));
            h.assertTrue(drops.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum()==1,
                    "Breaking a cat-cased belt must return the native connector");
            h.assertTrue(drops.stream().noneMatch(s->s.is(CatMachineBlocks.CAT_BELT_ITEM.get())),
                    "Breaking a cat-cased belt must not manufacture a legacy connector");
            h.assertTrue(state.getBlock().getCloneItemStack(state,new BlockHitResult(Vec3.atCenterOf(first),
                    Direction.UP,first,false),h.getLevel(),first,player).is(item),"Pick block must return native connector");
            player.setItemInHand(InteractionHand.MAIN_HAND,AllBlocks.BRASS_CASING.asStack());
            h.assertTrue(state.getBlock().use(state,h.getLevel(),first,player,InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(first),Direction.UP,first,false)).consumesAction(),
                    "Native brass casing must remain usable");
            h.assertTrue(!((CatBeltStyle)h.getLevel().getBlockEntity(first)).laowu$isCatBelt(),
                    "Native casing must replace cat appearance");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(CatMachineBlocks.CAT_CASING_ITEM.get()));
            var brassState=h.getLevel().getBlockState(first);
            h.assertTrue(brassState.getBlock().use(brassState,h.getLevel(),first,player,InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(first),Direction.UP,first,false)).consumesAction(),
                    "Cat casing must replace native brass casing");
            h.assertTrue(((CatBeltStyle)h.getLevel().getBlockEntity(first)).laowu$isCatBelt(),
                    "Cat appearance must return after replacing brass");
            h.setBlock(start.west(),AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(
                    com.simibubi.create.content.kinetics.motor.CreativeMotorBlock.FACING,Direction.EAST));
            ((com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity)
                    h.getLevel().getBlockEntity(h.absolutePos(start.west()))).generatedSpeed.setValue(128);
            h.runAfterDelay(10,()->{
                var belt=(BeltBlockEntity)h.getLevel().getBlockEntity(first);
                h.assertTrue(Math.abs(belt.getSpeed())==128,"Cat belt must receive real kinetic power");
                var cargo=new com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack(new ItemStack(Items.IRON_INGOT,3));
                cargo.beltPosition=2.5F;
                cargo.prevBeltPosition=2.5F;
                cargo.insertedAt=2;
                cargo.insertedFrom=Direction.UP;
                belt.getControllerBE().getInventory().addItem(cargo);
                h.runAfterDelay(3,()->{
                    h.assertTrue(Math.abs(cargo.beltPosition-2.5F)>.1F,"Native inventory must actually transport cat-belt cargo");
                    h.assertTrue(cargo.stack.getCount()==3,"Transport must conserve cargo");
                    h.succeed();
                });
            });
        });
    }
}
