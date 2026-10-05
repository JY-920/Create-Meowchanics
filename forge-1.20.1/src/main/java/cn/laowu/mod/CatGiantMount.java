package cn.laowu.mod;

import cn.laowu.mod.accessory.CatAccessories;
import cn.laowu.mod.entity.CatGiantCarrier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import java.util.Map;
import java.util.WeakHashMap;

/** Accessory-owned size and mount lifecycle; the real Cat remains the visible, saved animal. */
public final class CatGiantMount {
    public static final float SCALE = 4.0F;
    public static final float WIDTH = 2.0F;
    public static final float HEIGHT = 3.0F;
    public static final double SEAT = 2.875D;
    private static final Map<Cat, Float> LAST_SIZE = java.util.Collections.synchronizedMap(new WeakHashMap<>());

    public static float sizeFactor(Cat cat) {
        return (float)Math.sqrt(Math.max(1L,cn.laowu.mod.genetics.CatAttributeEffects.giantHealth(cat))/100.0D);
    }
    public static float width(Cat cat) { return WIDTH*sizeFactor(cat); }
    public static float height(Cat cat) { return HEIGHT*sizeFactor(cat); }
    public static double seat(Cat cat) { return Math.max(0,SEAT*sizeFactor(cat)-.75); }
    public static float rideHeight(Cat cat) { return (float)Math.max(height(cat),seat(cat)+1.925); }

    public static boolean active(Cat cat) {
        return cat.isAlive() && cat.isTame() && !cat.isBaby() && !CatPoseData.isPancake(cat)
                && (!cat.isPassenger() || cat.getVehicle() instanceof CatGiantCarrier)
                && CatAccessories.value(cat, "giant_mount") > 0;
    }
    public static boolean carried(Cat cat) { return cat.getVehicle() instanceof CatGiantCarrier; }
    public static void tick(Cat cat) {
        boolean giant = active(cat);
        float size=giant?sizeFactor(cat):0;
        Float old = LAST_SIZE.put(cat, size);
        boolean stale=giant && (Math.abs(cat.getBbWidth()-WIDTH*size)>.001F
                || Math.abs(cat.getBbHeight()-HEIGHT*size)>.001F);
        if ((old == null ? giant : Float.compare(old,size)!=0) || stale) cat.refreshDimensions();
        if (!cat.level().isClientSide && !giant && cat.getVehicle() instanceof CatGiantCarrier carrier)
            carrier.release();
    }
    public static void release(Cat cat) {
        if (!cat.level().isClientSide && cat.getVehicle() instanceof CatGiantCarrier carrier) carrier.release();
    }
    public static InteractionResult interact(Cat cat, Player player, InteractionHand hand) {
        if (!active(cat) || !player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
        if (!cat.isOwnedBy(player)) return InteractionResult.FAIL;
        if (cat.level().isClientSide) return InteractionResult.SUCCESS;
        if(player.isShiftKeyDown()) {
            if(cat.isPassenger())return InteractionResult.FAIL;
            boolean rest=!cat.isOrderedToSit();
            CatLaserCommands.cancel(cat);cat.setTarget(null);cat.getNavigation().stop();
            cat.setOrderedToSit(rest);cat.setInSittingPose(rest);
            cat.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            return InteractionResult.CONSUME;
        }
        return start(cat, player) ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }
    private static boolean eligible(Cat cat, Player player) {
        return cat.level() instanceof ServerLevel && active(cat) && cat.isOwnedBy(player)
                && !cat.isNoAi() && !DynamiteCatLastStand.isActive(cat) && !cat.isPassenger() && !cat.isVehicle()
                && !player.isPassenger() && player.isAlive() && !player.isSpectator()
                && cat.getBoundingBox().inflate(5).contains(player.position()) && !CatProfileData.isBeingViewed(cat)
                && !cat.isInLava() && !com.simibubi.create.content.kinetics.chainConveyor.ServerChainConveyorHandler.hangingPlayers.containsKey(player.getUUID());
    }
    public static boolean hasSpace(Cat cat) {
        double half=width(cat)/2;
        AABB box = new AABB(cat.getX()-half,cat.getY()+0.01,cat.getZ()-half,
                cat.getX()+half,cat.getY()+rideHeight(cat),cat.getZ()+half);
        return cat.level().hasChunksAt(BlockPos.containing(box.minX,box.minY,box.minZ),
                BlockPos.containing(box.maxX,box.maxY,box.maxZ))
                && cat.level().getWorldBorder().isWithinBounds(box)
                && !cat.level().getBlockCollisions(cat,box).iterator().hasNext();
    }
    public static boolean start(Cat cat, Player player) {
        if (!eligible(cat,player) || !hasSpace(cat)) return false;
        var carrier = new CatGiantCarrier(LaoWuMod.CAT_GIANT_CARRIER.get(),cat.level());
        carrier.setPos(cat.position()); carrier.setYRot(player.getYRot());
        if (!cat.level().addFreshEntity(carrier)) return false;
        CatLaserCommands.cancel(cat); cat.setTarget(null); cat.setOrderedToSit(false); cat.setInSittingPose(false);
        cat.getNavigation().stop();
        if (!cat.startRiding(carrier,true) || !player.startRiding(carrier,true)) { carrier.release(); return false; }
        carrier.updateSize();
        carrier.positionRider(cat); carrier.positionRider(player);
        return true;
    }
    private CatGiantMount() {}
}
