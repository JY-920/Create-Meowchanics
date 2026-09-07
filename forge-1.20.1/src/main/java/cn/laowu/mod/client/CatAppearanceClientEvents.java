package cn.laowu.mod.client;

import cn.laowu.mod.CatPoseData;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitData;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/** Client-local particles for appearance traits; no per-tick network traffic. */
@Mod.EventBusSubscriber(modid = LaoWuMod.MOD_ID, value = Dist.CLIENT)
public final class CatAppearanceClientEvents {
    private static final Vector3f[] RAINBOW = {
            new Vector3f(1.00F, 0.18F, 0.18F),
            new Vector3f(1.00F, 0.55F, 0.10F),
            new Vector3f(1.00F, 0.92F, 0.16F),
            new Vector3f(0.18F, 0.88F, 0.28F),
            new Vector3f(0.18F, 0.48F, 1.00F),
            new Vector3f(0.72F, 0.25F, 1.00F)
    };

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Cat cat) || !cat.level().isClientSide
                || !cat.isAlive()) return;
        var traits = CatTraitData.read(cat).orElse(null);
        if (traits == null) return;
        if (traits.has(CatTrait.ISAAC) && cat.tickCount % 4 == 0) {
            spawnTears(cat);
        }
        if (traits.has(CatTrait.RAINBOW_CAT) && cat.tickCount % 2 == 0) {
            spawnRainbowTrail(cat);
        }
    }

    private static void spawnTears(Cat cat) {
        float yaw = cat.getYHeadRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
        Vec3 centre = cat.position().add(forward.scale(0.29D));
        double y = CatPoseData.isPancake(cat)
                ? cat.getY() + 0.18D : cat.getEyeY() - 0.12D;
        for (double side : new double[] {-0.135D, 0.135D}) {
            cat.level().addParticle(ParticleTypes.FALLING_WATER,
                    centre.x + right.x * side, y,
                    centre.z + right.z * side,
                    0.0D, -0.025D, 0.0D);
        }
    }

    private static void spawnRainbowTrail(Cat cat) {
        Vec3 movement = cat.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        if (movement.horizontalDistanceSqr() < 0.0004D) return;
        Vec3 direction = movement.normalize();
        Vec3 behind = cat.position().subtract(direction.scale(
                cat.getBbWidth() * 0.65D + 0.16D));
        Vec3 side = new Vec3(-direction.z, 0.0D, direction.x);
        for (int index = 0; index < RAINBOW.length; index++) {
            double lateral = (index - 2.5D) * 0.012D;
            double y = CatPoseData.isPancake(cat)
                    ? cat.getY() + 0.08D + index * 0.025D
                    : cat.getY() + cat.getBbHeight() * (0.18D + index * 0.105D);
            cat.level().addParticle(new DustParticleOptions(RAINBOW[index], 0.62F),
                    behind.x + side.x * lateral, y,
                    behind.z + side.z * lateral,
                    -movement.x * 0.05D, 0.005D, -movement.z * 0.05D);
        }
    }

    private CatAppearanceClientEvents() {}
}
