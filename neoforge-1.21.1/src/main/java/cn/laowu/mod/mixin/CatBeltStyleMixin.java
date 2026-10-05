package cn.laowu.mod.mixin;

import cn.laowu.mod.create.CatBeltStyle;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=BeltBlockEntity.class,remap=false)
public abstract class CatBeltStyleMixin extends KineticBlockEntity implements CatBeltStyle {
    @Unique private boolean laowu$catBelt;
    protected CatBeltStyleMixin(BlockEntityType<?> type,BlockPos pos,BlockState state) {super(type,pos,state);}
    @Override public boolean laowu$isCatBelt() {return laowu$catBelt;}
    @Override public void laowu$setCatBelt(boolean enabled) {
        if(laowu$catBelt==enabled)return;
        laowu$catBelt=enabled;
        laowu$refreshClientModel();
        notifyUpdate();
    }
    @Unique private void laowu$refreshClientModel() {
        if(level==null || !level.isClientSide)return;
        requestModelDataUpdate();
        level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),16);
    }
    @Inject(method="setCasingType",at=@At("HEAD"),remap=false)
    private void laowu$clearCatOnNativeCasing(BeltBlockEntity.CasingType type,CallbackInfo ci) {
        laowu$setCatBelt(false);
    }
    @Inject(method="write",at=@At("TAIL"),remap=false)
    private void laowu$writeStyle(CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries,boolean clientPacket,CallbackInfo ci) {
        if(laowu$catBelt)tag.putBoolean("LaoWuCatBelt",true);
    }
    @Inject(method="read",at=@At("RETURN"),remap=false)
    private void laowu$readStyle(CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries,boolean clientPacket,CallbackInfo ci) {
        boolean changed=laowu$catBelt!=tag.getBoolean("LaoWuCatBelt");
        laowu$catBelt=tag.getBoolean("LaoWuCatBelt");
        if(changed && clientPacket) {
            laowu$refreshClientModel();
        }
    }
}
