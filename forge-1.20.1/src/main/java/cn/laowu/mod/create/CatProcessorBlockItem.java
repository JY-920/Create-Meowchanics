package cn.laowu.mod.create;

import com.simibubi.create.content.logistics.depot.DepotBlock;
import com.simibubi.create.content.processing.basin.BasinBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Create's assembly-operator placement shortcut, adapted to six-way work faces. */
public final class CatProcessorBlockItem extends BlockItem {
    public CatProcessorBlockItem(Block block,Properties properties){super(block,properties);}

    @Override public InteractionResult place(BlockPlaceContext context){
        var level=context.getLevel();
        var player=context.getPlayer();
        Direction face=context.getClickedFace();
        // BlockPlaceContext has already moved to the adjacent placement cell.
        // Run here, like Create, so vanilla block interaction still takes precedence
        // on ordinary right-click, while both actual placement modes share the shortcut.
        BlockPos placedOn=context.getClickedPos().relative(face.getOpposite());
        var target=level.getBlockState(placedOn);
        if(!(target.getBlock() instanceof BasinBlock||target.getBlock() instanceof DepotBlock)
                ||face!=CatMachineOrientation.top(target))return super.place(context);
        BlockPos gap=placedOn.relative(face);
        BlockPos destination=placedOn.relative(face,2);
        if(!level.hasChunkAt(gap)||!level.hasChunkAt(destination)||level.isOutsideBuildHeight(destination)
                ||!level.getWorldBorder().isWithinBounds(destination)
                ||!level.getBlockState(gap).getCollisionShape(level,gap).isEmpty()
                ||(player!=null&&(!level.mayInteract(player,destination)
                ||!player.mayUseItemAt(destination,face,context.getItemInHand()))))return InteractionResult.FAIL;
        var hit=new BlockHitResult(Vec3.atCenterOf(destination),face,destination,false);
        var placement=new BlockPlaceContext(level,player,context.getHand(),context.getItemInHand(),hit){
            // Never fall through to a third cell if the desired destination is occupied.
            @Override public BlockPos getClickedPos(){return getHitResult().getBlockPos();}
            @Override public Direction[] getNearestLookingDirections(){return new Direction[]{getClickedFace().getOpposite()};}
            // The explicit work-face shortcut must stay aligned with its selected basin/depot.
            @Override public boolean isSecondaryUseActive(){return false;}
        };
        if(!level.getBlockState(destination).canBeReplaced(placement))return InteractionResult.FAIL;
        return super.place(placement);
    }
}
