package cn.laowu.mod.client;
import cn.laowu.mod.create.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.phys.*;
final class CatAutoLaserFilterSlotProbe {
    static void verify(Minecraft mc) {
        var previousHit=mc.hitResult;
        var previousScreen=mc.screen;
        var position=mc.player.blockPosition().offset(0,2,3);
        var oldState=mc.level.getBlockState(position);
        try {
        mc.setScreen(null);
        for(Direction bottom:Direction.values()) {
            var state=CatMachineBlocks.CAT_AUTO_LASER.get().defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom);
            var face=CatMachineOrientation.toWorld(bottom,Direction.NORTH);
            var pos=BlockPos.ZERO;
            for(var point:new Vec3[]{new Vec3(.5,3d/16,0),new Vec3(.4,.1,0)})
                check(CatAutoLaserBlock.hitsFilter(state,new BlockHitResult(CatMachineOrientation.position(state,point),face,pos,false)),"Authored back inset should accept filter "+bottom);
            for(var point:new Vec3[]{new Vec3(.2,.4,0),new Vec3(.5,.5,0),new Vec3(.8,.2,0)})
                check(!CatAutoLaserBlock.hitsFilter(state,new BlockHitResult(CatMachineOrientation.position(state,point),face,pos,false)),"Do not accept clicks outside actual four-pixel back slot "+bottom);
            var expected=CatMachineOrientation.position(state,new Vec3(.5,3d/16,-1d/512));
            check(CatAutoLaserFilterSlot.INSTANCE.getLocalOffset(mc.level,position,state).distanceTo(expected)<1e-8,"Render transform must share authored centre");
            mc.level.setBlock(position,state,19);
            mc.hitResult=new BlockHitResult(CatMachineOrientation.position(state,new Vec3(.5,3d/16,0)).add(Vec3.atLowerCornerOf(position)),face,position,false);
            CatAutoLaserFilterHighlight.tick();
            var outliner=net.createmod.catnip.outliner.Outliner.getInstance();
            var key=net.createmod.catnip.data.Pair.of("laowu_auto_laser_filter",position);
            var entry=outliner.getOutlines().get(key);
            check(entry!=null,"Empty filter slot must register native hover outline "+bottom);
            check(entry.getOutline() instanceof com.simibubi.create.foundation.blockEntity.behaviour.ValueBox,"Hover must be native ValueBox");
            var box=(com.simibubi.create.foundation.blockEntity.behaviour.ValueBox)entry.getOutline();
            check(!box.isPassive&&box.getOutline()==com.simibubi.create.foundation.gui.AllIcons.VALUE_BOX_HOVER_4PX,"Native white four-pixel plate must be active");
            mc.hitResult=new BlockHitResult(CatMachineOrientation.position(state,new Vec3(.5,.6,0)).add(Vec3.atLowerCornerOf(position)),face,position,false);
            CatAutoLaserFilterHighlight.tick();
            check(!outliner.getOutlines().containsKey(key),"Hover outside actual inset must clear plate");
        }
        } finally {
            mc.hitResult=previousHit;mc.level.setBlock(position,oldState,19);mc.setScreen(previousScreen);
        }
        System.out.println("PASS: FILTER SLOT - six-way authored square, shared render/pick centre, native white hover plate, outside cleanup");
    }
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
