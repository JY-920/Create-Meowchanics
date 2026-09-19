package cn.laowu.mod;
import cn.laowu.mod.entity.CatDivingCarrier;
import cn.laowu.mod.genetics.*;
import com.simibubi.create.AllItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
/** Wrench-operated underwater mount; reuses the cat, profile storage and flight control protocol. */
public final class CatDivingMount {
    public static final String USED = "LaoWuDivingUsedTicks";
    public static boolean carried(Cat cat) { return cat.getVehicle() instanceof CatDivingCarrier; }
    public static boolean swimming(Cat cat) { return cat.getVehicle() instanceof CatDivingCarrier c && c.swimming(); }
    public static long duration(Cat cat) { return CatPilotFlight.duration(cat); }
    public static double speed(Cat cat, boolean wet) {
        return CatDivingRules.speed(ServerConfig.scale(CatStat.SPEED, CatAttributeEffects.effectiveValue(cat, CatStat.SPEED)), wet);
    }
    public static void recover(Cat cat) {
        if (cat.level().isClientSide || cat.isPassenger() || !cat.onGround() || cat.isInWaterOrBubble()
                || cat.getTarget() != null || CatClothesData.getOutfit(cat) != CatOutfitType.DIVING) return;
        long used = Math.max(0, Math.min(duration(cat), cat.getPersistentData().getLong(USED)));
        if (used > 0) cat.getPersistentData().putLong(USED, Math.max(0, used - 2));
    }
    public static void release(Cat cat) {
        if (!cat.level().isClientSide && cat.getVehicle() instanceof CatDivingCarrier carrier) carrier.release();
    }
    public static InteractionResult interact(Cat cat, Player player, InteractionHand hand) {
        if (CatClothesData.getOutfit(cat) != CatOutfitType.DIVING
                || !AllItems.WRENCH.isIn(player.getItemInHand(hand)) || player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!cat.isOwnedBy(player) || cat.isBaby()) return InteractionResult.FAIL;
        if (cat.level().isClientSide) return InteractionResult.SUCCESS;
        if (start(cat, player)) return InteractionResult.CONSUME;
        if (eligible(cat, player) && !hasSpace(cat))
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.laowu.pilot_flight.blocked"), true);
        return InteractionResult.FAIL;
    }
    private static boolean eligible(Cat cat, Player player) {
        return cat.level() instanceof ServerLevel && cat.isAlive() && cat.isTame() && cat.isOwnedBy(player)
                && !cat.isBaby() && !cat.isNoAi() && !cat.isPassenger() && !cat.isVehicle()
                && !player.isPassenger() && player.isAlive() && !player.isSpectator()
                && CatPilotFlight.wrench(player) && cat.distanceToSqr(player) <= 25
                && !CatPoseData.isPancake(cat) && !CatProfileData.isBeingViewed(cat)
                && CatClothesData.getOutfit(cat) == CatOutfitType.DIVING && !cat.isInLava()
                && !com.simibubi.create.content.kinetics.chainConveyor.ServerChainConveyorHandler.hangingPlayers.containsKey(player.getUUID());
    }
    public static boolean hasSpace(Cat cat) {
        return !cat.level().getBlockCollisions(cat, new AABB(cat.getX()-.425, cat.getY()+.02, cat.getZ()-.425,
                cat.getX()+.425, cat.getY()+2.45, cat.getZ()+.425)).iterator().hasNext();
    }
    public static boolean start(Cat cat, Player player) {
        if (!eligible(cat, player) || !hasSpace(cat)) return false;
        var carrier = new CatDivingCarrier(LaoWuMod.CAT_DIVING_CARRIER.get(), cat.level());
        carrier.setPos(cat.position()); carrier.setYRot(player.getYRot());
        if (!cat.level().addFreshEntity(carrier)) return false;
        CatLaserCommands.cancel(cat); cat.setTarget(null); cat.setOrderedToSit(false); cat.setInSittingPose(false);
        cat.getNavigation().stop();
        if (!cat.startRiding(carrier, true) || !player.startRiding(carrier, true)) { carrier.release(); return false; }
        carrier.positionRider(cat); carrier.positionRider(player);
        return true;
    }
    private CatDivingMount() {}
}
