package cn.laowu.mod.entity;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.Comparator;

public final class CatBallEntity extends ThrowableItemProjectile {
    /** Model-space measurements include the rotated ears, tail and side pieces. */
    public static final float WORLD_SCALE = 1.4F;
    public static final float MODEL_DIAMETER_PIXELS = 7.2424F;
    public static final float MODEL_HEIGHT_PIXELS = 6.0835F;
    public static final float MODEL_MIN_Y_PIXELS = -0.5835F;

    private static final double CONTACT_MARGIN = 0.22D;
    private static final double WALK_SPEED = 0.22D;
    private static final double REST_VERTICAL_SPEED = 0.16D;
    private static final double REST_HORIZONTAL_SPEED = 0.025D;
    private int kickCooldown;
    private boolean facingChangedDuringTick;

    public CatBallEntity(EntityType<? extends CatBallEntity> type, Level level) {
        super(type, level);
    }

    public CatBallEntity(Level level, LivingEntity owner) {
        super(LaoWuMod.CAT_BALL_ENTITY.get(), owner, level);
        // A lightly charged package-style throw can start close to the owner;
        // do not reinterpret that initial overlap as a football kick.
        kickCooldown = 8;
    }

    @Override
    protected Item getDefaultItem() {
        return LaoWuMod.CAT_BALL.get();
    }

    @Override
    protected float getGravity() {
        return 0.045F;
    }

    @Override
    public void tick() {
        // ThrowableProjectile normally rewrites yaw and pitch from velocity on
        // every tick. A cat ball keeps its pose until a player kicks it.
        float fixedYaw = getYRot();
        facingChangedDuringTick = false;
        if (!level().isClientSide && isNoGravity()
                && level().noCollision(this, getBoundingBox().move(0.0D, -0.035D, 0.0D))) {
            // The supporting block disappeared while the ball was resting.
            setNoGravity(false);
        }
        super.tick();
        if (!facingChangedDuringTick) {
            setYRot(fixedYaw);
            yRotO = fixedYaw;
        }
        setXRot(0.0F);
        xRotO = 0.0F;

        if (isNoGravity()) {
            Vec3 motion = getDeltaMovement();
            double x = motion.x * 0.91D;
            double z = motion.z * 0.91D;
            if (x * x + z * z < REST_HORIZONTAL_SPEED * REST_HORIZONTAL_SPEED) {
                x = 0.0D;
                z = 0.0D;
            }
            setDeltaMovement(x, 0.0D, z);
        }

        if (kickCooldown > 0) kickCooldown--;
        if (level().isClientSide || kickCooldown > 0 || !isAlive()) return;

        // Projectile collision only notices the ball's own movement. This
        // compact contact query also notices a player running into a resting
        // ball, which is the important half of football-like behaviour.
        level().getEntitiesOfClass(Player.class,
                        getBoundingBox().inflate(CONTACT_MARGIN, 0.08D, CONTACT_MARGIN),
                        player -> !player.isSpectator() && player.isAlive())
                .stream()
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .ifPresent(this::kickFromContact);
    }

    private void kickFromContact(Player player) {
        // ServerPlayer#getDeltaMovement does not reliably represent ordinary
        // keyboard walking. Position delta does, so use it for the two ranges.
        Vec3 positionDelta = new Vec3(player.getX() - player.xo, 0.0D,
                player.getZ() - player.zo);
        Vec3 reportedMotion = player.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        Vec3 playerMotion = positionDelta.lengthSqr() >= reportedMotion.lengthSqr()
                ? positionDelta : reportedMotion;
        double speed = playerMotion.length();

        Vec3 away = position().subtract(player.position()).multiply(1.0D, 0.0D, 1.0D);
        Vec3 look = player.getLookAngle();
        Vec3 flatLook = new Vec3(look.x, 0.0D, look.z);
        if (flatLook.lengthSqr() < 1.0E-6D) flatLook = new Vec3(0.0D, 0.0D, 1.0D);
        else flatLook = flatLook.normalize();
        if (away.lengthSqr() < 1.0E-6D) away = flatLook;
        else away = away.normalize();

        // Contact position supplies the natural outward direction; view angle
        // lets the player deliberately steer it. Running speed and how directly
        // the player approaches the ball determine the impulse strength.
        double approach = Math.max(0.0D, playerMotion.dot(away));
        Vec3 direction = away.scale(0.65D).add(flatLook.scale(0.35D));
        if (direction.lengthSqr() < 1.0E-6D) direction = away;
        else direction = direction.normalize();

        // Sneaking deliberately requests the short, controlled kick even if
        // the client still reports a lingering sprint state.
        boolean longKick = !player.isShiftKeyDown()
                && (speed > WALK_SPEED || player.isSprinting());
        double power = longKick
                ? Mth.clamp(0.75D + speed * 0.75D + approach * 0.50D, 0.75D, 1.05D)
                : Mth.clamp(0.30D + speed * 0.65D + approach * 0.30D, 0.30D, 0.52D);
        double lift = longKick
                ? Mth.clamp(0.11D + power * 0.10D + Math.max(0.0D, look.y) * 0.11D,
                        0.16D, 0.24D)
                : Mth.clamp(0.07D + power * 0.10D + Math.max(0.0D, look.y) * 0.07D,
                        0.09D, 0.16D);

        setOwner(player);
        setNoGravity(false);
        setDeltaMovement(direction.x * power, lift, direction.z * power);
        faceDirection(direction);
        hasImpulse = true;
        kickCooldown = 6;
        playSound(SoundEvents.WOOL_HIT, 0.9F, 0.9F + random.nextFloat() * 0.25F);
    }

    private void faceDirection(Vec3 direction) {
        if (direction.x * direction.x + direction.z * direction.z < 1.0E-8D) return;
        float yaw = (float) (Mth.atan2(-direction.x, direction.z) * Mth.RAD_TO_DEG);
        setYRot(yaw);
        yRotO = yaw;
        facingChangedDuringTick = true;
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        Vec3 motion = getDeltaMovement();
        if (hit.getDirection() == net.minecraft.core.Direction.UP
                && motion.y < 0.0D
                && -motion.y < REST_VERTICAL_SPEED) {
            double x = motion.x * 0.78D;
            double z = motion.z * 0.78D;
            if (x * x + z * z < REST_HORIZONTAL_SPEED * REST_HORIZONTAL_SPEED) {
                x = 0.0D;
                z = 0.0D;
            }
            setPos(getX(), hit.getLocation().y, getZ());
            setDeltaMovement(x, 0.0D, z);
            faceDirection(getDeltaMovement());
            setNoGravity(true);
            playSound(SoundEvents.WOOL_HIT, 0.45F, 0.9F + random.nextFloat() * 0.2F);
            return;
        }
        switch (hit.getDirection().getAxis()) {
            case X -> setDeltaMovement(-motion.x * 0.68D, motion.y * 0.84D, motion.z * 0.84D);
            case Y -> setDeltaMovement(motion.x * 0.82D, -motion.y * 0.52D, motion.z * 0.82D);
            case Z -> setDeltaMovement(motion.x * 0.84D, motion.y * 0.84D, -motion.z * 0.68D);
        }
        faceDirection(getDeltaMovement());
        playSound(SoundEvents.WOOL_HIT, 0.8F, 0.8F + random.nextFloat() * 0.35F);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        Entity entity = hit.getEntity();
        if (entity instanceof LivingEntity living) {
            Vec3 motion = getDeltaMovement();
            living.push(motion.x * 0.28D, 0.10D, motion.z * 0.28D);
            setNoGravity(false);
            setDeltaMovement(motion.scale(-0.62D).add(0.0D, 0.12D, 0.0D));
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public void push(Entity entity) {
        if (entity instanceof Player player) {
            if (!level().isClientSide && kickCooldown == 0) kickFromContact(player);
            return;
        }
        super.push(entity);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living) {
            var look = living.getLookAngle();
            setOwner(living);
            setNoGravity(false);
            setDeltaMovement(look.x * 1.25D, Math.max(0.22D, look.y * 0.75D + 0.25D), look.z * 1.25D);
            hasImpulse = true;
            playSound(SoundEvents.WOOL_HIT, 1.0F, 1.15F);
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide) {
            ItemStack ball = getItem().copyWithCount(1);
            if (!player.getInventory().add(ball)) player.drop(ball, false);
            discard();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
