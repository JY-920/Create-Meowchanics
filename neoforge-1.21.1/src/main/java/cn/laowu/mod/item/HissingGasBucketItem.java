package cn.laowu.mod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

/** A tank transport container that vents into particles instead of placing a fluid block. */
public final class HissingGasBucketItem extends BucketItem {
    public HissingGasBucketItem(Supplier<? extends Fluid> fluid, Properties properties) {
        super(fluid.get(), properties);
    }

    @Override
    public boolean emptyContents(Player player, Level level, BlockPos pos, BlockHitResult hitResult) {
        return vent(level, pos);
    }

    @Override
    public boolean emptyContents(Player player, Level level, BlockPos pos, BlockHitResult hitResult,
                                 ItemStack container) {
        return vent(level, pos);
    }

    private static boolean vent(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            double x = pos.getX() + 0.5D;
            double y = pos.getY() + 0.35D;
            double z = pos.getZ() + 0.5D;
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    x, y, z, 36, 0.38D, 0.32D, 0.38D, 0.035D);
            serverLevel.sendParticles(ParticleTypes.POOF,
                    x, y, z, 18, 0.32D, 0.25D, 0.32D, 0.025D);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS, 0.75F, 1.35F);
        }
        // Returning true lets BucketItem replace the gas bucket with an empty bucket.
        return true;
    }
}
