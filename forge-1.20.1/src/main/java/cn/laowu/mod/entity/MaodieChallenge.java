package cn.laowu.mod.entity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
/** A challenge owns the original cat until safe restoration commits. Also supports legacy saves. */
public interface MaodieChallenge {
 boolean isMaodie();
 boolean isSummoning();
 CompoundTag sourceCatSnapshot();
 BlockPos maodieOrigin();
 boolean restorationPending();
 void markRestorationPending();
 int updateMaodieAbsence(boolean present);
}
