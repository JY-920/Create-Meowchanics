package cn.laowu.mod;

import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

/**
 * Runtime bridge implemented on cats by CatNavigationMixin.
 * This is a normal interface, so it must stay outside the reserved mixin package.
 */
public interface CatNavigationAccessor {
    void laowu$setNavigation(PathNavigation navigation);
    void laowu$setMoveControl(MoveControl control);
}
