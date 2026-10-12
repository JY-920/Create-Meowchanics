package cn.laowu.mod.test;
import cn.laowu.mod.*;
import cn.laowu.mod.create.*;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.item.CatPancakeItem;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.SidedFilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;
@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CreateCatCareProbe {
    @GameTest(template="artillery_probe",timeoutTicks=40)
    public static void nativeAutomationAndConfigurationRoundTrip(GameTestHelper h) throws Exception {
        var pos=h.absolutePos(new BlockPos(5,2,5));
        var state=LaoWuMod.ADVANCED_BREEDING_BOX.get().defaultBlockState();
        h.getLevel().setBlock(pos,state,3);
        var box=(BreedingBoxBlockEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(box instanceof SmartBlockEntity,"Create-installed box remains native SmartBlockEntity");
        var modeField=BreedingBoxBlockEntity.class.getDeclaredField("replacementMode");modeField.setAccessible(true);
        var mode=(ScrollOptionBehaviour<?>)modeField.get(box);mode.setValue(1);
        var filterField=BreedingBoxBlockEntity.class.getDeclaredField("parentFilters");filterField.setAccessible(true);
        var filters=(SidedFilteringBehaviour)filterField.get(box);
        var side=state.getValue(BreedingBoxBlock.FACING).getClockWise();
        h.assertTrue(filters.setFilter(side,new ItemStack(LaoWuMod.CAT_FILTER.get())),"Native cat filter accepted");
        var snapshot=box.saveWithoutMetadata();
        var standalone=new BreedingBoxData(pos,state);standalone.setLevel(h.getLevel());
        standalone.readData(snapshot);
        var saved=new net.minecraft.nbt.CompoundTag();standalone.writeData(saved);
        var restored=new BreedingBoxBlockEntity(pos,state);restored.setLevel(h.getLevel());
        restored.load(saved);
        var restoredFilters=(SidedFilteringBehaviour)filterField.get(restored);
        h.assertTrue(restoredFilters.getFilter(side).is(LaoWuMod.CAT_FILTER.get()),"Installed native filter survives Create -> standalone -> Create save");
        h.assertTrue(((ScrollOptionBehaviour<?>)modeField.get(restored)).getValue()==1,"Native replacement mode survives dependency round trip");
        var parent=CatPancakeItem.defaultDisplayStack();CatTraitData.set(parent,CatTraitProfile.EMPTY);
        var attributes=CatAttributeProfile.founder(h.getLevel().random);
        for(var stat:CatStat.values())attributes=attributes.withValues(stat,0,10);
        CatAttributeData.set(parent,attributes);
        box.insertParent(parent.copy());box.insertParent(parent.copy());box.insertFood(new ItemStack(LaoWuMod.BREEDING_CAT_FOOD.get()));
        var candidate=parent.copy();for(var stat:CatStat.values())attributes=attributes.withValues(stat,95,100);CatAttributeData.set(candidate,attributes);
        var pool=pos.relative(side);h.getLevel().setBlock(pool,net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(),3);
        ((net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(pool)).setItem(0,candidate);
        box.menuData().set(0,box.effectiveDurationTicks()-1);
        BreedingBoxBlockEntity.tickBox(h.getLevel(),pos,state,box);
        h.assertTrue(CatPancakeItem.isBaby(box.inventory().getStackInSlot(2)),"Native box actually breeds");
        h.assertTrue(CatAttributeData.read(box.inventory().getStackInSlot(0)).orElseThrow().potential(CatStat.ATTACK)==100,"Native auto-replacement still selects better adjacent parent");
        h.succeed();
    }
}
