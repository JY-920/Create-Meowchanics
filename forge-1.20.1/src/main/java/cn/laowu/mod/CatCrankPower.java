package cn.laowu.mod;

import net.minecraft.world.entity.animal.Cat;

/** Implemented only by the Create hand-crank mixin. No global stress configuration changes. */
public interface CatCrankPower {
    void laowu$drive(Cat cat);
    static double stressCapacity(double stamina) {
        return 128.0D * Math.max(0.0D, stamina);
    }
}
