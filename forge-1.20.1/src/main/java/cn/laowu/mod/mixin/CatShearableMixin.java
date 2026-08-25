package cn.laowu.mod.mixin;

import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.CatPoseData;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.IForgeShearable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

/**
 * Lets Forge's normal shears pipeline treat cats exactly like other shearable
 * entities. Create's Deployer already understands that pipeline and captures
 * the returned drops, so no deployer-specific polling or inventory code is
 * necessary.
 */
@Mixin(Cat.class)
public abstract class CatShearableMixin implements IForgeShearable {
    @Override
    public boolean isShearable(@NotNull ItemStack item, Level level, BlockPos pos) {
        Cat cat = (Cat) (Object) this;
        // An equipped cat is sheared through the outfit-removal interaction
        // first; it must not yield fur during that same action.
        return !CatClothesData.isEquipped(cat);
    }

    @Override
    public @NotNull List<ItemStack> onSheared(@Nullable Player player,
                                               @NotNull ItemStack item,
                                               Level level, BlockPos pos,
                                               int fortune) {
        Cat cat = (Cat) (Object) this;
        cat.playSound(SoundEvents.SHEEP_SHEAR, 1.0F, 1.05F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF,
                    cat.getX(), cat.getY() + (CatPoseData.isPancake(cat)
                            ? 0.12D : cat.getBbHeight() * 0.55D), cat.getZ(),
                    5, 0.18D, 0.05D, 0.18D, 0.015D);
        }
        int traitLevel = CatTraitData.ensure(cat).level(CatTrait.LONG_FUR);
        int amount = 1 + (traitLevel <= 0 ? 0
                : CatTrait.LONG_FUR.longFurExtraDrops(traitLevel));
        return List.of(new ItemStack(LaoWuMod.CAT_FUR.get(), amount));
    }
}
