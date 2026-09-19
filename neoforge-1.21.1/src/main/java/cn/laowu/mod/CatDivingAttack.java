package cn.laowu.mod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;

/** Short cone of water: ordinary career damage and beneficial-potion dispel, never friendly fire. */
public final class CatDivingAttack {
    public static final double RANGE = 4;
    public static int spray(Cat cat, LivingEntity target) {
        if (!(cat.level() instanceof ServerLevel level) || !cat.isAlive()
                || CatClothesData.getOutfit(cat) != CatOutfitType.DIVING
                || target == null || !target.isAlive() || !CatTeamRules.canHarm(cat, target)) return 0;
        Vec3 origin = new Vec3(cat.getX(), cat.getEyeY() + .08, cat.getZ());
        Vec3 direction = target.getBoundingBox().getCenter().subtract(origin).normalize();
        if (direction.lengthSqr() < .001) direction = cat.getLookAngle();
        Vec3 end = origin.add(direction.scale(RANGE));
        int hits = 0;
        boolean cleanse=cn.laowu.mod.accessory.CatAccessories.value(cat,"diving_cleanse")>0;
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, end).inflate(1.8),
                e -> e.isAlive() && e!=cat && (CatTeamRules.canHarm(cat,e)
                        || cleanse&&e instanceof Cat ally&&CatTeamRules.friendly(cat,ally)) && cat.hasLineOfSight(e))) {
            Vec3 relative = other.getBoundingBox().getCenter().subtract(origin);
            double forward = relative.dot(direction), radius = .40 + forward * .30 + other.getBbWidth() * .5;
            if (forward < 0 || forward > RANGE + other.getBbWidth() * .5
                    || relative.lengthSqr() - forward * forward > radius * radius) continue;
            if(other instanceof Cat ally && CatTeamRules.friendly(cat,ally)) {
                boolean changed=false;
                for(var effect:new ArrayList<>(ally.getActiveEffects()))
                    if(effect.getEffect().value().getCategory()==net.minecraft.world.effect.MobEffectCategory.HARMFUL){
                        ally.removeEffect(effect.getEffect());changed=true;
                    }
                if(changed)level.sendParticles(ParticleTypes.HAPPY_VILLAGER,ally.getX(),ally.getY(.6),ally.getZ(),6,.2,.2,.2,0);
                continue;
            }
            // Only remove positive effects after a successful, uncancelled attack.
            if (!other.hurt(level.damageSources().mobAttack(cat), (float)cat.getAttributeValue(Attributes.ATTACK_DAMAGE))) continue;
            for (var effect : new ArrayList<>(other.getActiveEffects()))
                if (effect.getEffect().value().isBeneficial()) other.removeEffect(effect.getEffect());
            hits++;
        }
        var flower = new cn.laowu.mod.particle.NozzleFluidPuffData(
                new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1));
        for (int step = 1; step <= 10; step++) {
            double distance = .15 + step * (RANGE - .15) / 10, spread = .05 + distance * .15;
            Vec3 point = origin.add(direction.scale(distance));
            level.sendParticles(flower, point.x, point.y, point.z, 4, spread, spread * .6, spread, .045);
            level.sendParticles(ParticleTypes.SPLASH, point.x, point.y, point.z, 5, spread, spread, spread, .04);
            level.sendParticles(ParticleTypes.BUBBLE, point.x, point.y, point.z, 2, spread, spread, spread, .02);
        }
        level.playSound(null, cat.blockPosition(), SoundEvents.PLAYER_SPLASH, SoundSource.NEUTRAL, .65F, 1.35F);
        return hits;
    }
    private CatDivingAttack() {}
}
