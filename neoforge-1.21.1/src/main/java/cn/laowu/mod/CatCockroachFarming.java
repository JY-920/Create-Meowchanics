package cn.laowu.mod;

import cn.laowu.mod.genetics.CatAttributeEffects;
import cn.laowu.mod.genetics.CatStat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FarmBlock;

/** Idle pest-fertilizer: one crop per roll, never an inventory or dropped-item consumer. */
public final class CatCockroachFarming {
    public static final int INTERVAL = 100, HALF_SIZE = 4;
    private static final String NEXT = "LaoWuCockroachNextFertilize";
    public static double chance(int health) { return Math.min(1, Math.max(0, health) / 200.0); }
    public static boolean available(Cat cat) {
        return !cat.level().isClientSide && cat.isAlive() && cat.isTame()
                && CatClothesData.getOutfit(cat) == CatOutfitType.COCKROACH
                && !cat.isNoAi() && !CatProfileData.isBeingViewed(cat) && !CatPoseData.isPancake(cat)
                && (cat.getTarget() == null || !cat.getTarget().isAlive())
                && (!cat.isPassenger() || CareerCatBehavior.findSeat(cat) != null);
    }
    public static void tick(Cat cat) {
        if (!available(cat)) return;
        long now = cat.level().getGameTime();
        var data = cat.getPersistentData();
        if (!data.contains(NEXT)) { data.putLong(NEXT, now + INTERVAL); return; }
        if (data.getLong(NEXT) > now + INTERVAL) data.putLong(NEXT, now + INTERVAL);
        if (now < data.getLong(NEXT)) return;
        data.putLong(NEXT, now + INTERVAL);
        if (cat.getRandom().nextDouble() < chance(CatAttributeEffects.effectiveValue(cat, CatStat.HEALTH)))
            fertilize(cat);
    }
    /** The 9-cube is centered on the cat's block; require farmland and actual vanilla bonemeal eligibility. */
    public static boolean fertilize(Cat cat) {
        if (!available(cat) || !(cat.level() instanceof ServerLevel level)) return false;
        BlockPos center = cat.blockPosition(), selected = null;
        int eligible = 0;
        for (BlockPos soil : BlockPos.betweenClosed(center.offset(-HALF_SIZE,-HALF_SIZE,-HALF_SIZE),
                center.offset(HALF_SIZE,HALF_SIZE,HALF_SIZE))) {
            BlockPos pos = soil.above();
            if (!level.hasChunkAt(soil) || !level.hasChunkAt(pos)
                    || !(level.getBlockState(soil).getBlock() instanceof FarmBlock)) continue;
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BonemealableBlock crop)
                    || !crop.isValidBonemealTarget(level, pos, state)) continue;
            if (cat.getRandom().nextInt(++eligible) == 0) selected = pos.immutable();
        }
        if (selected == null) return false;
        // A virtual dose invokes the crop's ordinary growth/hook behavior; no sample is consumed.
        if (!BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, selected)) return false;
        level.levelEvent(1505, selected, 0);
        return true;
    }
    private CatCockroachFarming() {}
}
