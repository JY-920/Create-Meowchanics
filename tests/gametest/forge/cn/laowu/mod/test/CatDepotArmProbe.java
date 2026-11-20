package cn.laowu.mod.test;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.mechanicalArm.*;
import cn.laowu.mod.create.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public class CatDepotArmProbe {
    @GameTest(template="artillery_probe",timeoutTicks=50)
    public static void armSelectsTransfersAndReloadsAllDepotOrientations(GameTestHelper h) {
        var armPos=new BlockPos(2,2,2);
        h.setBlock(armPos,AllBlocks.MECHANICAL_ARM.get());
        var arm=(ArmBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(armPos));
        var pos=new BlockPos(4,2,4);
        int index=0;
        for(var bottom:Direction.values()) {
            var local=pos.offset(index++*2,0,0);
            h.setBlock(local,CatDepotRegistration.CAT_DEPOT.get().defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom));
            var absolute=h.absolutePos(local);
            var state=h.getBlockState(local);
            h.assertTrue(ArmInteractionPoint.isInteractable(h.getLevel(),absolute,state),"Arm must select cat depot: "+bottom);
            var point=ArmInteractionPoint.create(h.getLevel(),absolute,state);
            h.assertTrue(point!=null&&point.isValid(),"Registered arm point is valid");
            try {
                var facing=point.getClass().getDeclaredMethod("getInteractionDirection");
                facing.setAccessible(true);
                h.assertTrue(facing.invoke(point)==bottom,"Gripper points into the working face");
                var position=point.getClass().getDeclaredMethod("getInteractionPositionVector");
                position.setAccessible(true);
                var expected=net.minecraft.world.phys.Vec3.atCenterOf(absolute).add(
                        net.minecraft.world.phys.Vec3.atLowerCornerOf(bottom.getOpposite().getNormal()).scale(.375));
                h.assertTrue(((net.minecraft.world.phys.Vec3)position.invoke(point)).distanceTo(expected)<1e-6,"Grip follows rotated tabletop");
            } catch(ReflectiveOperationException error) { throw new AssertionError(error); }
            var origin=h.absolutePos(armPos);
            point.cycleMode();
            var restored=ArmInteractionPoint.deserialize(point.serialize(origin),h.getLevel(),origin);
            h.assertTrue(restored!=null&&restored.isValid()&&restored.getMode()==point.getMode(),"Target and take mode survive saving");
            h.assertTrue(restored.insert(new ItemStack(Items.IRON_INGOT,12),true).isEmpty(),"Simulated deposit accepted");
            h.assertTrue(((com.simibubi.create.content.logistics.depot.DepotBlockEntity)h.getLevel().getBlockEntity(absolute)).getHeldItem().isEmpty(),"Simulation must not create items");
            h.assertTrue(restored.insert(new ItemStack(Items.IRON_INGOT,12),false).isEmpty(),"Real arm deposit accepted");
            h.assertTrue(restored.extract(0,4,true).getCount()==4,"Simulated take available");
            h.assertTrue(restored.extract(0,4,false).getCount()==4,"Real arm take returns exact count");
            h.assertTrue(restored.extract(0,64,false).getCount()==8,"Remaining count conserved");
            restored.getTargetAngles(origin,false);
            h.setBlock(local,state.setValue(CatMachineOrientation.BOTTOM,bottom.getOpposite()));
            restored.updateCachedState();
            var rotated=restored.getTargetAngles(origin,false);
            var fresh=ArmInteractionPoint.create(h.getLevel(),absolute,h.getBlockState(local)).getTargetAngles(origin,false);
            try {
                for(var name:new String[]{"baseAngle","lowerArmAngle","upperArmAngle","headAngle"}) {
                    var field=ArmAngleTarget.class.getDeclaredField(name);field.setAccessible(true);
                    h.assertTrue(Math.abs(field.getFloat(rotated)-field.getFloat(fresh))<1e-4,"Selected target must refresh rotated grip angles: "+name);
                }
            } catch(ReflectiveOperationException error) { throw new AssertionError(error); }
        }
        h.setBlock(pos,AllBlocks.DEPOT.get());
        h.assertTrue(ArmInteractionPoint.isInteractable(h.getLevel(),h.absolutePos(pos),h.getBlockState(pos)),"Native depot stays selectable");
        h.setBlock(pos,net.minecraft.world.level.block.Blocks.STONE);
        h.assertTrue(!ArmInteractionPoint.isInteractable(h.getLevel(),h.absolutePos(pos),h.getBlockState(pos)),"Stone must not become an arm target");
        h.succeed();
    }
}
