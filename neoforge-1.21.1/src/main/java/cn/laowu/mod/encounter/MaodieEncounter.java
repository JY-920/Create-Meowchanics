package cn.laowu.mod.encounter;

import cn.laowu.mod.*;
import cn.laowu.mod.block.BoguShelfBlock;
import cn.laowu.mod.entity.*;
import cn.laowu.mod.genetics.CatGenomeData;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.Optional;

/** Server-only encounter transaction. The original is never removed before a backed-up boss exists. */
public final class MaodieEncounter {
    public static final String COOLDOWN = "LaoWuMaodieCooldownUntil";
    private MaodieEncounter() {}

    public static void onStroke(BoguShelfSeat seat, Player player, InteractionHand hand) {
        if (!(seat.level() instanceof ServerLevel level) || hand != InteractionHand.MAIN_HAND
                || player.isSpectator() || !player.getItemInHand(hand).isEmpty()) return;
        Cat cat = seat.cat();
        if (!validSource(seat) || seat.isSummoningMaodie()
                || cat.getPersistentData().getLong(COOLDOWN) > level.getGameTime()) return;
        if (level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            seat.maodieState().reset();
            return;
        }
        var state = seat.maodieState();
        if (state.stroke(cat.getUUID(), level.getGameTime()) != MaodieSummonState.StrokeResult.READY) return;
        if (findBossSpawn(level, seat.anchor(), Direction.fromYRot(seat.getYRot())).isEmpty()) {
            state.rejectThirdStroke();
            player.displayClientMessage(Component.literal("腾出空间"), true);
            return;
        }
        state.begin(level.getGameTime());
        seat.syncMaodieAura();
    }

    private static boolean validSource(BoguShelfSeat seat) {
        Cat cat = seat.cat();
        BlockPos root = seat.anchor();
        return cat != null && cat.isAlive() && !cat.isRemoved() && !cat.isTame() && !cat.isLeashed()
                && !CatGiantMount.active(cat) && !CatPoseData.isPancake(cat) && !CatProfileData.isBeingViewed(cat)
                && seat.level().hasChunksAt(root.offset(-2, 0, -2), root.offset(2, 2, 2))
                && BoguShelfBlock.complete(seat.level(), root, Direction.fromYRot(seat.getYRot()));
    }

    public static void tickSeat(BoguShelfSeat seat) {
        if (!(seat.level() instanceof ServerLevel level) || !seat.isSummoningMaodie()) return;
        if (level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            seat.cancelMaodieAura();
            return;
        }
        if (!validSource(seat)) { seat.cancelMaodieAura(); return; }
        if (!seat.maodieState().due(level.getGameTime())) return;
        var location = findBossSpawn(level, seat.anchor(), Direction.fromYRot(seat.getYRot()));
        Cat cat = seat.cat();
        if (location.isEmpty()) { seat.cancelMaodieAura(); return; }
        var boss = LaoWuMod.MAODIE_FIGHTER.get().create(level);
        if (boss == null) { seat.cancelMaodieAura(); return; }
        CompoundTag snapshot = new CompoundTag();
        if (!cat.saveAsPassenger(snapshot)) { seat.cancelMaodieAura(); return; }
        seat.sanitizeSnapshot(snapshot);
        var persistent = cat.getPersistentData().copy();
        persistent.putLong(COOLDOWN, level.getGameTime() + 6000);
        snapshot.put("NeoForgeData", persistent);
        boss.configureMaodie(snapshot, seat.anchor());
        boss.setInheritedGenome(CatGenomeData.getOrFallback(cat));
        Vec3 at = location.get();
        boss.moveTo(at.x, at.y, at.z, seat.getYRot(), 0);
        boss.beginSummoning();
        if (!level.addFreshEntity(boss)) { seat.cancelMaodieAura(); return; }
        seat.cancelMaodieAura();
        CatProfileData.forgetStoredEntity(cat);
        cat.stopRiding();
        cat.discard();
        seat.discard();
    }

    public static Optional<Vec3> findBossSpawn(ServerLevel level, BlockPos origin, Direction facing) {
        var probe = LaoWuMod.MAODIE_FIGHTER.get().create(level);
        if (probe == null) return Optional.empty();
        // Match the display seat's lateral -0.75 offset. Keep the fighter just
        // outside the shelf, at ground height, rather than teleporting four blocks away.
        // If nearby space is obstructed, keep the cat instead of spawning far away.
        Direction right=facing.getClockWise();
        for (double forward : new double[]{1, 1.25, 1.5, 1.75, 2})
            for (double side : new double[]{-.75, -1, -.5, -.25, 0}) {
                for (int dy : new int[]{0, 1, -1}) {
                    Vec3 at = Vec3.atBottomCenterOf(origin).add(
                            facing.getStepX()*forward+right.getStepX()*side,dy,
                            facing.getStepZ()*forward+right.getStepZ()*side);
                    if (safe(level, probe, at, null)) return Optional.of(at);
                }
            }
        return Optional.empty();
    }

    private static Optional<Vec3> findSafe(ServerLevel level, Entity probe, BlockPos origin,
            Direction facing, int minimum, int maximum, Entity ignore) {
        for (int r = minimum; r <= maximum; r++)
            for (int forward = -r; forward <= r; forward++)
                for (int side = -r; side <= r; side++) {
                    if (Math.max(Math.abs(forward), Math.abs(side)) != r || forward*forward+side*side > maximum*maximum) continue;
                    for (int dy : new int[]{0, 1, -1, 2, -2}) {
                        BlockPos block = origin.relative(facing, forward).relative(facing.getClockWise(), side).above(dy);
                        Vec3 pos = new Vec3(block.getX()+.5, block.getY(), block.getZ()+.5);
                        if (safe(level, probe, pos, ignore)) return Optional.of(pos);
                    }
                }
        return Optional.empty();
    }

    private static boolean safe(ServerLevel level, Entity probe, Vec3 at, Entity ignore) {
        AABB box = probe.getDimensions(probe.getPose()).makeBoundingBox(at);
        AABB checked = box.inflate(1).expandTowards(0,-1,0);
        if (box.minY < level.getMinBuildHeight() || box.maxY >= level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(box)
                || !level.hasChunksAt(BlockPos.containing(checked.minX,checked.minY,checked.minZ),
                                     BlockPos.containing(checked.maxX,checked.maxY,checked.maxZ))) return false;
        if (level.getBlockCollisions(probe,box.deflate(1E-5)).iterator().hasNext()
                || !level.getEntities(probe,box,e->e!=ignore&&e.isAlive()&&(e.canBeCollidedWith()||e instanceof LivingEntity)).isEmpty()) return false;
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY-1,box.minZ),
                                                 BlockPos.containing(box.maxX,box.maxY,box.maxZ))) {
            var b=level.getBlockState(p);
            if (!level.getFluidState(p).isEmpty() || b.is(BlockTags.FIRE) || b.is(Blocks.MAGMA_BLOCK)
                    || b.is(Blocks.CACTUS) || b.is(Blocks.SWEET_BERRY_BUSH) || b.is(Blocks.WITHER_ROSE)
                    || b.is(Blocks.POWDER_SNOW)) return false;
        }
        double inset=Math.max(0,probe.getBbWidth()/2-.05);
        for (double x : new double[]{0,-inset,inset}) for (double z : new double[]{0,-inset,inset}) {
            AABB foot = new AABB(at.x+x-.02,at.y-.08,at.z+z-.02,at.x+x+.02,at.y,at.z+z+.02);
            if (!level.getBlockCollisions(probe,foot).iterator().hasNext()) return false;
        }
        return true;
    }

    public static <T extends LivingEntity & MaodieChallenge> boolean tickBoss(T boss) {
        if (boss instanceof MaodieFighter || !(boss.level() instanceof ServerLevel level) || !boss.isMaodie()) return false;
        if (boss.restorationPending()) {
            if (boss.tickCount % 20 == 0) tryRestore(boss);
            return true;
        }
        if (!boss.isSummoning()) {
            boolean present=level.players().stream().anyMatch(p->p.isAlive()&&!p.isSpectator()
                    &&!p.getAbilities().instabuild&&p.distanceToSqr(boss)<=32*32);
            if (boss.updateMaodieAbsence(present) >= 400) { requestEnd(boss); return true; }
        }
        return false;
    }

    public static <T extends LivingEntity & MaodieChallenge> void requestEnd(T boss) {
        if (boss instanceof MaodieFighter || !(boss.level() instanceof ServerLevel) || !boss.isMaodie() || boss.isRemoved()) return;
        boss.markRestorationPending();
        tryRestore(boss);
    }

    public static <T extends LivingEntity & MaodieChallenge> boolean tryRestore(T boss) {
        if (boss instanceof MaodieFighter || !(boss.level() instanceof ServerLevel level) || !boss.isMaodie() || !boss.restorationPending()) return false;
        CompoundTag data = boss.sourceCatSnapshot();
        if (!data.hasUUID("UUID")) return false;
        for (ServerLevel world : level.getServer().getAllLevels()) {
            Entity existing=world.getEntity(data.getUUID("UUID"));
            if (existing!=null) {
                if (existing instanceof Cat && existing.isAlive()) { boss.discard(); return true; }
                return false;
            }
        }
        Entity restored=EntityType.loadEntityRecursive(data,level,e->e);
        if (!(restored instanceof Cat cat) || !cat.isAlive()) return false;
        Optional<Vec3> at=findSafe(level,cat,boss.blockPosition(),Direction.NORTH,0,8,boss);
        if (at.isEmpty()) at=findSafe(level,cat,boss.maodieOrigin(),Direction.NORTH,0,8,boss);
        if (at.isEmpty()) return false;
        cat.moveTo(at.get().x,at.get().y,at.get().z,cat.getYRot(),cat.getXRot());
        cat.setDeltaMovement(Vec3.ZERO);cat.fallDistance=0;
        CatProfileData.recoverInterruptedViewLock(cat);
        CatPoseData.setPose(cat,CatPoseData.NORMAL);
        cat.setInSittingPose(cat.isOrderedToSit());
        if (!level.addFreshEntity(cat)) return false;
        boss.discard();
        return true;
    }
}
