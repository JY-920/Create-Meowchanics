package cn.laowu.mod.entity;

import net.minecraft.world.phys.Vec3;

/** Additive encounter-only phases. Legacy giant phases 0–8 remain untouched. */
public final class MaodieCombat {
    public static final byte HISS_WINDUP=9, HISS=10, HISS_RECOVERY=11;
    private MaodieCombat() {}
    public static boolean inHissCone(Vec3 origin,float lockedYaw,Vec3 victim) {
        Vec3 delta=victim.subtract(origin);
        double distance=delta.horizontalDistance();
        if (!Float.isFinite(lockedYaw)||!Double.isFinite(distance)||Math.abs(delta.y)>3||distance>8) return false;
        if (distance<.001) return true;
        double radians=Math.toRadians(lockedYaw);
        return (-Math.sin(radians)*delta.x+Math.cos(radians)*delta.z)/distance>=Math.cos(Math.toRadians(35));
    }
    public static float damageForPhase(int phase) {
        return switch(phase) { case GiantCatBossCombat.IDLE -> 6; case GiantCatBossCombat.ROLL -> 8;
            case GiantCatBossCombat.JUMP -> 12; case HISS -> 4; default -> 0; };
    }
    public static int recoveryTicks() { return 20; }
    public static byte nextPhase(byte phase,int ticks,boolean grounded,boolean blocked,boolean targetValid) {
        return switch(phase) {
            case HISS_WINDUP -> !targetValid ? HISS_RECOVERY : ticks>=20 ? HISS : HISS_WINDUP;
            case HISS -> ticks>=20 ? HISS_RECOVERY : HISS;
            case HISS_RECOVERY -> ticks>=recoveryTicks() ? GiantCatBossCombat.IDLE : HISS_RECOVERY;
            default -> GiantCatBossCombat.nextPhase(phase,ticks,grounded,blocked,targetValid);
        };
    }
}
