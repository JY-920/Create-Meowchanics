package cn.laowu.mod;

import cn.laowu.mod.entity.CatFlightCarrier;
import cn.laowu.mod.genetics.CatAttributeEffects;
import cn.laowu.mod.genetics.CatStat;
import com.simibubi.create.AllItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class CatPilotFlight {
    public static final String USED = "LaoWuPilotFlightUsedTicks";
    public static boolean carried(Cat cat) { return cat.getVehicle() instanceof CatFlightCarrier; }
    public static boolean wrench(Player player) {
        return AllItems.WRENCH.isIn(player.getMainHandItem()) || AllItems.WRENCH.isIn(player.getOffhandItem());
    }
    public static long duration(Cat cat) {
        return CatPilotFlightRules.durationTicks(ServerConfig.scale(CatStat.STAMINA,
                CatAttributeEffects.effectiveValue(cat, CatStat.STAMINA)));
    }
    public static double speed(Cat cat) {
        return CatPilotFlightRules.speedPerTick(ServerConfig.scale(CatStat.SPEED,
                CatAttributeEffects.effectiveValue(cat, CatStat.SPEED)));
    }
    public static void recover(Cat cat) {
        if (cat.level().isClientSide || carried(cat) || cat.isPassenger() || !cat.onGround()
                || CatClothesData.getOutfit(cat) != CatOutfitType.FLIGHT || cat.getTarget() != null) return;
        var data = cat.getPersistentData();
        long used = Math.min(duration(cat), data.getLong(USED));
        if (used > 0) data.putLong(USED, Math.max(0, used - 2));
    }
    public static void release(Cat cat) {
        if (!cat.level().isClientSide && cat.getVehicle() instanceof CatFlightCarrier carrier) carrier.release(true);
    }
    public static InteractionResult interact(Cat cat, Player player, InteractionHand hand) {
        if (CatClothesData.getOutfit(cat) != CatOutfitType.FLIGHT
                || !AllItems.WRENCH.isIn(player.getItemInHand(hand)) || player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!cat.isOwnedBy(player) || cat.isBaby()) return InteractionResult.FAIL;
        if (cat.level().isClientSide) return InteractionResult.SUCCESS;
        if (start(cat, player)) return InteractionResult.CONSUME;
        if (eligible(cat, player) && !hasSpace(cat))
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.laowu.pilot_flight.blocked"), true);
        return InteractionResult.FAIL;
    }
    private static boolean eligible(Cat cat, Player player) {
        if (!(cat.level() instanceof ServerLevel) || !cat.isTame() || !cat.isOwnedBy(player)
                || !cat.isAlive() || cat.isBaby() || cat.isNoAi() || cat.isPassenger() || cat.isVehicle()
                || player.isPassenger() || player.isSpectator() || !player.isAlive()
                || !wrench(player) || cat.distanceToSqr(player) > 25
                || CatPoseData.isPancake(cat) || CatProfileData.isBeingViewed(cat)
                || CatClothesData.getOutfit(cat) != CatOutfitType.FLIGHT
                || cat.isInWaterOrBubble() || cat.isInLava()
                || com.simibubi.create.content.kinetics.chainConveyor.ServerChainConveyorHandler.hangingPlayers.containsKey(player.getUUID()))
            return false;
        return true;
    }
    private static boolean hasSpace(Cat cat) {
        AABB space = new AABB(cat.getX() - 0.425, cat.getY() + 0.02, cat.getZ() - 0.425,
                cat.getX() + 0.425, cat.getY() + 3.35, cat.getZ() + 0.425);
        return !cat.level().getBlockCollisions(cat, space).iterator().hasNext();
    }
    public static boolean start(Cat cat, Player player) {
        if (!eligible(cat, player) || !hasSpace(cat)) return false;
        ServerLevel level = (ServerLevel) cat.level();
        var carrier = new CatFlightCarrier(LaoWuMod.CAT_FLIGHT_CARRIER.get(), level);
        carrier.setPos(cat.position());
        carrier.setYRot(player.getYRot());
        if (!level.addFreshEntity(carrier)) return false;
        CatLaserCommands.cancel(cat);
        cat.setTarget(null);
        cat.setOrderedToSit(false);
        cat.setInSittingPose(false);
        cat.getNavigation().stop();
        if (!cat.startRiding(carrier, true) || !player.startRiding(carrier, true)) {
            carrier.release(false);
            return false;
        }
        carrier.positionRider(cat);
        carrier.positionRider(player);
        return true;
    }
    private CatPilotFlight() {}
}
