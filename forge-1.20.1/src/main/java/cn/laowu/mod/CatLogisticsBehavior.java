package cn.laowu.mod;
import cn.laowu.mod.compat.create.*;
import net.minecraft.world.entity.animal.Cat;
/** No-Create mode never resolves the industrial parcel/seat implementation. */
public final class CatLogisticsBehavior {
    public static boolean tick(Cat cat) {
        if (CreateIntegration.isLoaded()) return CreateLogisticsBehavior.tick(cat);
        releaseStandaloneFlight(cat);
        return false;
    }
    public static boolean isActive(Cat cat) { return CreateIntegration.isLoaded() && CreateLogisticsBehavior.isActive(cat); }
    public static void abort(Cat cat) {
        if (CreateIntegration.isLoaded()) CreateLogisticsBehavior.abort(cat);
        else releaseStandaloneFlight(cat);
    }
    public static void cancelForPancake(Cat cat) {
        if (CreateIntegration.isLoaded()) CreateLogisticsBehavior.cancelForPancake(cat);
        else releaseStandaloneFlight(cat);
    }
    public static void onInventoryChanged(Cat cat) { if (CreateIntegration.isLoaded()) CreateLogisticsBehavior.onInventoryChanged(cat); }
    private static void releaseStandaloneFlight(Cat cat) {
        if (cat.level().isClientSide) return;
        var data = cat.getPersistentData();
        int phase = data.getInt("LaoWuLogisticsPhase");
        if (phase < 1 || phase > 4) return;
        cat.setNoGravity(false);
        cat.fallDistance = 0;
        cat.setOrderedToSit(false);
        cat.setInSittingPose(false);
        // Stop only the unavailable flight phase. Keep cargo, pending package NBT and addresses.
        data.remove("LaoWuLogisticsPhase");
    }
    private CatLogisticsBehavior() {}
}
