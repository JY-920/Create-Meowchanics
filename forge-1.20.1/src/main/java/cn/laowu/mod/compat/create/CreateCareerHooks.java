package cn.laowu.mod.compat.create;
import cn.laowu.mod.*;
import cn.laowu.mod.entity.FishingRodProjectile;
import cn.laowu.mod.entity.MechanicalLaserProjectile;
import cn.laowu.mod.entity.HoneyMissileProjectile;
import cn.laowu.mod.entity.LogisticsSupportProjectile;
import cn.laowu.mod.entity.DynamiteProjectile;
import cn.laowu.mod.mixin.BlazeBurnerBlockEntityAccessor;
import cn.laowu.mod.genetics.CatAttributeEffects;
import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitData;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.foundation.fluid.FluidHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
public final class CreateCareerHooks {
    private static final int SUPERHEAT_DURATION = 20 * 5;
    public static BlockPos findSeat(Cat cat) {
        // Riding across a Seat block is not occupying that workstation.
        if (CatGiantMount.carried(cat)) return null;
        if (cat.getVehicle() instanceof SeatEntity seatEntity) {
            BlockPos pos = seatEntity.blockPosition();
            if (cat.level().getBlockState(pos).getBlock() instanceof SeatBlock) {
                return pos.immutable();
            }
        }
        BlockPos[] candidates = {cat.blockPosition(), cat.blockPosition().below(), cat.getOnPos()};
        for (BlockPos pos : candidates) {
            if (cat.level().getBlockState(pos).getBlock() instanceof SeatBlock
                    && (cat.isPassenger() || cat.isInSittingPose())) {
                return pos.immutable();
            }
        }
        return null;
    }
    public static void tickFire(Cat cat) {
        if (cat.tickCount % 10 != 0) return;
        BlockPos seat = findSeat(cat);
        if (seat == null) return;
        var traits = CatTraitData.ensure(cat);
        boolean sustainedSuperheat = traits.has(CatTrait.SUPERHEAT_GENE);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!(cat.level().getBlockEntity(seat.relative(direction))
                    instanceof BlazeBurnerBlockEntity burner) || burner.isCreative()) continue;
            if (sustainedSuperheat) superheatBurner(burner);
            else keepBurnerKindled(burner);
        }
    }
    private static void superheatBurner(BlazeBurnerBlockEntity burner) {
        if (burner.getActiveFuel() == BlazeBurnerBlockEntity.FuelType.SPECIAL
                && burner.getRemainingBurnTime() > 40) return;
        boolean changedHeat = burner.getActiveFuel()
                != BlazeBurnerBlockEntity.FuelType.SPECIAL;
        BlazeBurnerBlockEntityAccessor accessor = (BlazeBurnerBlockEntityAccessor) burner;
        accessor.laowu$setActiveFuel(BlazeBurnerBlockEntity.FuelType.SPECIAL);
        accessor.laowu$setRemainingBurnTime(SUPERHEAT_DURATION);
        burner.setChanged();
        if (changedHeat) burner.updateBlockState();
    }
    private static void keepBurnerKindled(BlazeBurnerBlockEntity burner) {
        if (burner.getActiveFuel() == BlazeBurnerBlockEntity.FuelType.SPECIAL
                || burner.getRemainingBurnTime() > 20) return;
        boolean wasActive = burner.getActiveFuel() != BlazeBurnerBlockEntity.FuelType.NONE
                && burner.getRemainingBurnTime() > 0;
        BlazeBurnerBlockEntityAccessor accessor = (BlazeBurnerBlockEntityAccessor) burner;
        accessor.laowu$setActiveFuel(BlazeBurnerBlockEntity.FuelType.NORMAL);
        accessor.laowu$setRemainingBurnTime(40);
        burner.setChanged();
        if (!wasActive) burner.updateBlockState();
    }
    public static boolean isSeatPassenger(Cat cat) { return cat.getVehicle() instanceof SeatEntity; }
}
