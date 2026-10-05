package cn.laowu.mod.create;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Reverse assembly shortcut: click the processor's working end to place its basin/depot. */
public final class CatWorkSurfaceBlockItem extends BlockItem {
    public CatWorkSurfaceBlockItem(Block block, Properties properties) { super(block, properties); }

    @Override public InteractionResult place(BlockPlaceContext context) {
        // Replacing snow/grass is not a click on the processor behind that cell.
        if(context.replacingClickedOnBlock())return super.place(context);
        var level=context.getLevel();
        var player=context.getPlayer();
        Direction face=context.getClickedFace();
        BlockPos clicked=context.getClickedPos().relative(face.getOpposite());
        var processor=level.getBlockState(clicked);
        if(!(processor.getBlock() instanceof CatProcessorBlock)
                ||face!=CatMachineOrientation.bottom(processor))return super.place(context);
        BlockPos gap=clicked.relative(face);
        BlockPos destination=clicked.relative(face,2);
        if(!level.hasChunkAt(gap)||!level.hasChunkAt(destination)||level.isOutsideBuildHeight(destination)
                ||!level.getWorldBorder().isWithinBounds(destination)
                ||!level.getBlockState(gap).getCollisionShape(level,gap).isEmpty()
                ||(player!=null&&(!level.mayInteract(player,destination)
                ||!player.mayUseItemAt(destination,face,context.getItemInHand()))))return InteractionResult.FAIL;

        // Present the new container's work face toward the processor; freeze the exact
        // destination so an occupied target can never spill into a third placement cell.
        var hit=new BlockHitResult(Vec3.atCenterOf(destination),face.getOpposite(),destination,false);
        var placement=new BlockPlaceContext(level,player,context.getHand(),context.getItemInHand(),hit) {
            @Override public BlockPos getClickedPos(){return getHitResult().getBlockPos();}
            @Override public boolean isSecondaryUseActive(){return false;}
        };
        if(!level.getBlockState(destination).canBeReplaced(placement))return InteractionResult.FAIL;
        return super.place(placement);
    }
}
