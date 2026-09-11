package cn.laowu.mod;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.DyeColor;

/** Collar colour is the team ID; vanilla synced/persisted collar data remains the only source. */
public final class CatTeamRules {
    public static DyeColor colour(TamableAnimal pet) {
        if (pet instanceof Cat cat) return cat.getCollarColor();
        if (pet instanceof Wolf wolf) return wolf.getCollarColor();
        return DyeColor.RED;
    }
    public static boolean friendly(Entity first, Entity second) {
        if (!(first instanceof TamableAnimal a) || !(second instanceof TamableAnimal b)
                || !a.isTame() || !b.isTame()) return false;
        var teamA = a.getTeam();
        var teamB = b.getTeam();
        return PetTeamPolicy.friendly(colour(a).getId(), colour(b).getId(),
                a.getOwnerUUID(), b.getOwnerUUID(),
                teamA == null ? null : teamA.getName(), teamB == null ? null : teamB.getName());
    }
    public static boolean canHarm(Cat attacker, LivingEntity target) {
        if (target == null || target == attacker || !target.isAlive() || target == attacker.getOwner()
                || target instanceof net.minecraft.world.entity.player.Player) return false;
        if (attacker.isTame() && target instanceof TamableAnimal pet && pet.isTame())
            return !friendly(attacker, pet); // Different collars are neutral, not automatic enemies.
        return attacker.canAttack(target) && !attacker.isAlliedTo(target);
    }
    public static int rgb(Cat cat) {
        return cat.getCollarColor().getTextureDiffuseColor();
    }
    private CatTeamRules() {}
}
