package cn.laowu.mod.mixin;

import cn.laowu.mod.CatNavigationAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/** Direct inherited-field access is remapped by ForgeGradle in production jars. */
@Mixin(Cat.class)
public abstract class CatNavigationMixin extends TamableAnimal implements CatNavigationAccessor {
    protected CatNavigationMixin(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }
    @Override public void laowu$setNavigation(PathNavigation navigation) { this.navigation = navigation; }
    @Override public void laowu$setMoveControl(MoveControl control) { this.moveControl = control; }
}
