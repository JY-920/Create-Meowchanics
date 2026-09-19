package cn.laowu.mod.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/** Agent clouds share vanilla campfire smoke's soft puffs, drift and translucent render pass. */
public final class CatHealingSmokeParticle extends TextureSheetParticle {
    private CatHealingSmokeParticle(ClientLevel level, double x, double y, double z,
                                    double dx, double dy, double dz, SpriteSet sprites, boolean healing) {
        super(level, x, y, z);
        scale(4); setSize(.25F, .25F);
        gravity = .000003F;
        xd = dx; yd = dy + .02 + random.nextFloat() / 500; zd = dz;
        pickSprite(sprites);
        if (healing) setColor(.20F, .82F, .30F);
        else setColor(.85F, .85F, .85F);
        setAlpha(1F);
        lifetime = 60 + random.nextInt(20);
    }
    // 1.20.1's campfire constructor is package-private. Keep its small horizontal drift
    // locally on both ports rather than widening vanilla access or touching global particle state.
    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime) { remove(); return; }
        xd += random.nextFloat() / 5000 * (random.nextBoolean() ? 1 : -1);
        zd += random.nextFloat() / 5000 * (random.nextBoolean() ? 1 : -1);
        yd -= gravity;
        move(xd, yd, zd);
        setAlpha(net.minecraft.util.Mth.clamp((lifetime - age) / 10F, 0, 1));
    }
    @Override public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double dx, double dy, double dz) {
            return new CatHealingSmokeParticle(level, x, y, z, dx, dy, dz, sprites, true);
        }
    }
    public record SmokeProvider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double dx, double dy, double dz) {
            return new CatHealingSmokeParticle(level, x, y, z, dx, dy, dz, sprites, false);
        }
    }
}
