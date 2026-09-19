package cn.laowu.mod.entity;

import cn.laowu.mod.*;
import com.simibubi.create.AllBlocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.*;

/** Solid, finite Create munitions: single hit, area burst, or one hit per victim along a shaft path. */
public final class EngineeringCogwheelProjectile extends ThrowableItemProjectile
        implements cn.laowu.mod.api.CatAccessoryProjectile {
    public static final double BLAST_RADIUS = 2.5;
    private float damage = 2;
    private double travelled;
    private final Set<UUID> pierced = new HashSet<>();

    public EngineeringCogwheelProjectile(EntityType<? extends EngineeringCogwheelProjectile> type, Level level) {
        super(type, level);
    }
    public EngineeringCogwheelProjectile(Level level, Cat cat, float damage) {
        super(LaoWuMod.ENGINEERING_COGWHEEL_PROJECTILE.get(), cat, level);
        setAccessoryDamage(damage);
    }
    @Override protected Item getDefaultItem() { return AllBlocks.COGWHEEL.asItem(); }
    public CatArtilleryMunition munition() {
        if (getItem().is(AllBlocks.LARGE_COGWHEEL.asItem())) return CatArtilleryMunition.LARGE_COG;
        if (getItem().is(AllBlocks.SHAFT.asItem())) return CatArtilleryMunition.SHAFT;
        return CatArtilleryMunition.SMALL_COG;
    }
    public void setMunition(CatArtilleryMunition type) {
        setItem(switch (type) {
            case LARGE_COG -> AllBlocks.LARGE_COGWHEEL.asStack();
            case SHAFT -> AllBlocks.SHAFT.asStack();
            case SMALL_COG -> AllBlocks.COGWHEEL.asStack();
        });
    }
    @Override public float getAccessoryDamage() { return damage; }
    @Override public void setAccessoryDamage(double amount) {
        damage = (float)cn.laowu.mod.accessory.CatAccessoryScriptRules.damage(amount);
    }
    @Override protected float getGravity() { return 0.0F; }
    @Override public void tick() {
        if (!level().isClientSide) {
            if (travelled >= 34 || tickCount > 80 || !(getOwner() instanceof Cat cat) || !cat.isAlive()) {
                discard();
                return;
            }
            double remaining = 34 - travelled;
            if (getDeltaMovement().length() > remaining)
                setDeltaMovement(getDeltaMovement().normalize().scale(remaining));
            // Sweep the whole segment in distance order, including spawn-inside contacts.
            // Clip the segment to the first wall BEFORE finding victims; shafts never pierce terrain.
            Vec3 start = position(), end = start.add(getDeltaMovement());
            BlockHitResult wall = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, this));
            if (wall.getType() != HitResult.Type.MISS) end = wall.getLocation();
            List<EntityHitResult> hits = new ArrayList<>();
            for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().expandTowards(end.subtract(start)).inflate(0.2), this::canHitEntity)) {
                AABB bounds = target.getBoundingBox().inflate(0.1);
                Vec3 point = bounds.contains(start) ? start : bounds.clip(start, end).orElse(null);
                if (point != null) hits.add(new EntityHitResult(target, point));
            }
            hits.sort(Comparator.comparingDouble(hit -> hit.getLocation().distanceToSqr(start)));
            for (EntityHitResult hit : hits) {
                onHitEntity(hit);
                if (isRemoved()) return;
            }
            if (wall.getType() != HitResult.Type.MISS) { onHitBlock(wall); return; }
        }
        Vec3 before = position();
        super.tick();
        travelled += before.distanceTo(position());
        if (!level().isClientSide && travelled >= 34) discard();
    }
    @Override protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity instanceof LivingEntity living
                && !(entity instanceof Player) && !pierced.contains(entity.getUUID())
                && getOwner() instanceof Cat cat && CatTeamRules.canHarm(cat, living);
    }
    @Override protected void onHitEntity(EntityHitResult result) {
        if (!(level() instanceof ServerLevel level) || !(getOwner() instanceof Cat cat)
                || !cat.isAlive() || !(result.getEntity() instanceof LivingEntity target)
                || !CatEngineeringCombat.validTarget(cat, target) || !pierced.add(target.getUUID())) return;
        if (munition() == CatArtilleryMunition.LARGE_COG) {
            burst(level, cat, result.getLocation(), target);
            discard();
            return;
        }
        CatProjectileDamage.hurt(target, level.damageSources().mobProjectile(this, cat), damage);
        impact(level, result.getLocation());
        if (munition() != CatArtilleryMunition.SHAFT) discard();
    }
    @Override protected void onHitBlock(BlockHitResult result) {
        if (level() instanceof ServerLevel level) {
            if (munition() == CatArtilleryMunition.LARGE_COG && getOwner() instanceof Cat cat) {
                // The outer face offset prevents numerical self-occlusion without reaching through the wall.
                burst(level, cat, result.getLocation().add(Vec3.atLowerCornerOf(result.getDirection().getNormal()).scale(0.01)), null);
            } else impact(level, result.getLocation());
            discard();
        }
    }
    private void burst(ServerLevel level, Cat cat, Vec3 hit, LivingEntity primary) {
        if (primary != null)
            CatProjectileDamage.hurt(primary, level.damageSources().mobProjectile(this, cat), damage);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(hit, hit).inflate(BLAST_RADIUS), candidate -> candidate != primary
                        && CatEngineeringCombat.validTarget(cat, candidate))) {
            Vec3 center = target.getBoundingBox().getCenter();
            if (center.distanceToSqr(hit) > BLAST_RADIUS * BLAST_RADIUS
                    || level.clip(new ClipContext(hit, center, ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS) continue;
            CatProjectileDamage.hurt(target, level.damageSources().mobProjectile(this, cat), damage);
        }
        // Visual burst only: no terrain explosion, drops, fire or knockback bypass.
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, hit.x, hit.y, hit.z, 1, 0, 0, 0, 0);
        impact(level, hit);
    }
    private void impact(ServerLevel level, Vec3 hit) {
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                hit.x, hit.y, hit.z, 8, 0.1, 0.1, 0.1, 0.04);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putDouble("Travelled", travelled);
        ListTag victims = new ListTag();
        for (UUID id : pierced) victims.add(StringTag.valueOf(id.toString()));
        tag.put("PiercedVictims", victims);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setAccessoryDamage(tag.getFloat("Damage"));
        travelled = Math.max(0, tag.getDouble("Travelled"));
        pierced.clear();
        for (Tag victim : tag.getList("PiercedVictims", Tag.TAG_STRING)) {
            try { pierced.add(UUID.fromString(victim.getAsString())); } catch (IllegalArgumentException ignored) {}
        }
    }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
}
