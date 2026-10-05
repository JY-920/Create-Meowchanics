package cn.laowu.mod.mixin;

import cn.laowu.mod.create.CatBeltStyle;
import cn.laowu.mod.create.CatMachineBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Accept the cat casing on Create's belt without replacing its belt block or connector. */
@Mixin(value=BeltBlock.class,remap=false)
public abstract class CatBeltCasingMixin {
    @Inject(method="useItemOn",at=@At("HEAD"),cancellable=true,remap=false)
    private void laowu$applyCatCasing(ItemStack stack,BlockState state,Level level,BlockPos pos,
            Player player,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<ItemInteractionResult> ci) {
        if(player.isShiftKeyDown() || !player.mayBuild())return;
        // Native BeltBlock gates extension/joining by exact connector identity.
        // Saved legacy stacks need this same block-use path as well as useOn.
        if(stack.is(CatMachineBlocks.CAT_BELT_ITEM.get())) {
            ci.setReturnValue(com.simibubi.create.content.kinetics.belt.BeltSlicer.useConnector(
                    state,level,pos,player,hand,hit,new com.simibubi.create.content.kinetics.belt.BeltSlicer.Feedback()));
            return;
        }
        if(!stack.is(CatMachineBlocks.CAT_CASING_ITEM.get())
                || !(level.getBlockEntity(pos) instanceof BeltBlockEntity))return;
        BeltBlockEntity belt=(BeltBlockEntity)level.getBlockEntity(pos);
        belt.setCasingType(BeltBlockEntity.CasingType.ANDESITE);
        if(level.getBlockEntity(pos) instanceof CatBeltStyle style)style.laowu$setCatBelt(true);
        ((BeltBlock)(Object)this).updateCoverProperty(level,pos,level.getBlockState(pos));
        SoundType sound=CatMachineBlocks.CAT_CASING.get().defaultBlockState().getSoundType(level,pos,player);
        level.playSound(null,pos,sound.getPlaceSound(),SoundSource.BLOCKS,
                (sound.getVolume()+1.0F)/2.0F,sound.getPitch()*0.8F);
        ci.setReturnValue(ItemInteractionResult.SUCCESS);
    }
}
