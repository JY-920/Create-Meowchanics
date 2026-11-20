package cn.laowu.mod.entity;

/** Loot-only bobber, never spawned or ticked. Working cats use their existing
 * adjacent-water eligibility instead of the player's open-water restrictions. */
public final class CatFishingLootContext extends net.minecraft.world.entity.projectile.FishingHook {
    public CatFishingLootContext(net.minecraft.server.level.ServerLevel level) {
        super(net.minecraft.world.entity.EntityType.FISHING_BOBBER, level);
    }
    @Override public boolean isOpenWaterFishing() { return true; }
}
