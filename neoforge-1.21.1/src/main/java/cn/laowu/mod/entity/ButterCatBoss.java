package cn.laowu.mod.entity;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatGenome;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;

/** Hostile 1.5x cat boss whose authored Blockbench roll is its charge attack. */
public final class ButterCatBoss extends Monster {
    public static final float MODEL_SCALE = 1.5F;
    public static final float BASE_WIDTH = 0.6F;
    public static final float BASE_HEIGHT = 0.7F;
    public static final int SUMMON_DURATION_TICKS = 60;

    private static final String INHERITED_GENOME_TAG = "InheritedCatGenome";
    private static final String SUMMONING_TAG = "ButterCatSummoning";
    private static final String SUMMON_START_TAG = "ButterCatSummonStart";

    /** animation2 is authored as five seconds and is sampled at 2x speed. */
    public static final int CHARGE_WINDUP_TICKS = 5 * 20 / 2;
    private static final int CHARGE_ACTIVE_TICKS = 14;
    private static final int CHARGE_COOLDOWN_TICKS = 70;
    private static final double CHARGE_MIN_DISTANCE_SQR = 3.0D * 3.0D;
    private static final double CHARGE_MAX_DISTANCE_SQR = 20.0D * 20.0D;
    private static final float CHARGE_DAMAGE = 14.0F;
    private static final double CHARGE_DISTANCE = 12.0D;
    private static final double CHARGE_SPEED = 1.05D;

    public static final byte ATTACK_IDLE = 0;
    public static final byte ATTACK_WINDUP = 1;
    public static final byte ATTACK_DASH = 2;

    private static final EntityDataAccessor<Byte> DATA_ATTACK_PHASE =
            SynchedEntityData.defineId(ButterCatBoss.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> DATA_PHASE_ANIMATION_START =
            SynchedEntityData.defineId(ButterCatBoss.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<CompoundTag> DATA_INHERITED_GENOME =
            SynchedEntityData.defineId(ButterCatBoss.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Boolean> DATA_SUMMONING =
            SynchedEntityData.defineId(ButterCatBoss.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> DATA_SUMMON_START =
            SynchedEntityData.defineId(ButterCatBoss.class, EntityDataSerializers.LONG);

    private final ServerBossEvent bossEvent = new ServerBossEvent(getDisplayName(),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
    private final Set<ServerPlayer> trackingPlayers =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private int chargeCooldown;

    public ButterCatBoss(EntityType<? extends ButterCatBoss> type, Level level) {
        super(type, level);
        this.xpReward = 50;
        this.bossEvent.setDarkenScreen(false);
        this.bossEvent.setPlayBossMusic(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 180.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 36.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.55D);
    }

    /** Keep the vanilla cat's innate immunity to fall damage. */
    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK_PHASE, ATTACK_IDLE);
        builder.define(DATA_PHASE_ANIMATION_START, 0L);
        builder.define(DATA_INHERITED_GENOME, new CompoundTag());
        builder.define(DATA_SUMMONING, false);
        builder.define(DATA_SUMMON_START, 0L);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ChargeAttackGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false));
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2,
                new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide && isDashing() && (this.tickCount & 1) == 0) {
            Vec3 backwards = this.getLookAngle().scale(-0.65D);
            this.level().addParticle(ParticleTypes.CLOUD,
                    this.getX() + backwards.x,
                    this.getY() + this.getBbHeight() * 0.45D,
                    this.getZ() + backwards.z,
                    -this.getDeltaMovement().x * 0.15D, 0.025D,
                    -this.getDeltaMovement().z * 0.15D);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || !isSummoning()) return;

        this.setNoAi(true);
        this.setTarget(null);
        this.getNavigation().stop();
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(0.0D, motion.y, 0.0D);
        if (this.level().getGameTime() - getSummonStartGameTime()
                >= SUMMON_DURATION_TICKS) {
            finishSummoning();
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.chargeCooldown > 0) this.chargeCooldown--;
        this.bossEvent.setProgress(Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.0F, 1.0F));
        if (this.tickCount % 20 == 0) this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.trackingPlayers.add(player);
        if (!isSummoning()) this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.trackingPlayers.remove(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CAT_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 1.35F;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (isSummoning() && !source.is(DamageTypes.FELL_OUT_OF_WORLD)
                && !source.is(DamageTypes.GENERIC_KILL)) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    /** Starts the visible bread descent before this entity becomes an active boss. */
    public void beginSummoning() {
        this.entityData.set(DATA_SUMMONING, true);
        this.entityData.set(DATA_SUMMON_START, this.level().getGameTime());
        this.setNoAi(true);
        this.setTarget(null);
        this.setDeltaMovement(Vec3.ZERO);
    }

    public boolean isSummoning() {
        return this.entityData.get(DATA_SUMMONING);
    }

    public float getSummonProgress(float partialTick) {
        if (!isSummoning()) return 1.0F;
        long elapsed = this.level().getGameTime() - getSummonStartGameTime();
        return Mth.clamp((elapsed + partialTick) / SUMMON_DURATION_TICKS,
                0.0F, 1.0F);
    }

    public void setInheritedGenome(CatGenome genome) {
        this.entityData.set(DATA_INHERITED_GENOME, genome.save());
    }

    public Optional<CatGenome> getInheritedGenome() {
        CompoundTag serialized = this.entityData.get(DATA_INHERITED_GENOME);
        return serialized.isEmpty() ? Optional.empty() : CatGenome.load(serialized);
    }

    private long getSummonStartGameTime() {
        return this.entityData.get(DATA_SUMMON_START);
    }

    private void finishSummoning() {
        this.entityData.set(DATA_SUMMONING, false);
        this.setNoAi(false);
        this.setDeltaMovement(Vec3.ZERO);
        this.chargeCooldown = 20;
        this.trackingPlayers.forEach(this.bossEvent::addPlayer);
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        double centerY = this.getY() + BASE_HEIGHT * 0.55D;
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                this.getX(), centerY, this.getZ(), 1,
                0.0D, 0.0D, 0.0D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.POOF,
                this.getX(), centerY, this.getZ(), 42,
                0.7D, 0.45D, 0.7D, 0.14D);
        serverLevel.sendParticles(ParticleTypes.CLOUD,
                this.getX(), centerY, this.getZ(), 24,
                0.8D, 0.35D, 0.8D, 0.08D);
        serverLevel.playSound(null, this.getX(), centerY, this.getZ(),
                SoundEvents.GENERIC_EXPLODE, this.getSoundSource(),
                1.25F, 0.88F + this.random.nextFloat() * 0.08F);
        this.gameEvent(net.minecraft.world.level.gameevent.GameEvent.EXPLODE);
    }

    /** Drops one to three uniformly selected super attribute cans or Super Dried Fish. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source,
                                       boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        int drops = Mth.nextInt(random, 1, 3);
        for (int roll = 0; roll < drops; roll++) {
            spawnAtLocation(new ItemStack(randomSuperReward()));
        }
    }

    private Item randomSuperReward() {
        return switch (random.nextInt(7)) {
            case 0 -> LaoWuMod.SUPER_ATTACK_CAT_CAN.get();
            case 1 -> LaoWuMod.SUPER_HEALTH_CAT_CAN.get();
            case 2 -> LaoWuMod.SUPER_SPEED_CAT_CAN.get();
            case 3 -> LaoWuMod.SUPER_STAMINA_CAT_CAN.get();
            case 4 -> LaoWuMod.SUPER_INTELLIGENCE_CAT_CAN.get();
            case 5 -> LaoWuMod.SUPER_LUCK_CAT_CAN.get();
            default -> LaoWuMod.SUPER_DRIED_FISH.get();
        };
    }

    public boolean isWindingUp() {
        return this.entityData.get(DATA_ATTACK_PHASE) == ATTACK_WINDUP;
    }

    public boolean isDashing() {
        return this.entityData.get(DATA_ATTACK_PHASE) == ATTACK_DASH;
    }

    /** Authored animation2 time in seconds, sampled at 2x and clamped to five seconds. */
    public float getWindupAnimationSeconds(float partialTick) {
        if (!isWindingUp()) return 0.0F;
        long elapsed = this.level().getGameTime() - this.entityData.get(DATA_PHASE_ANIMATION_START);
        return Mth.clamp((elapsed + partialTick) * 2.0F / 20.0F, 0.0F, 5.0F);
    }

    /** animation1 is one second long and intentionally runs at 2x during the dash. */
    public float getDashAnimationProgress(float partialTick) {
        if (!isDashing()) return 0.0F;
        long elapsed = this.level().getGameTime() - this.entityData.get(DATA_PHASE_ANIMATION_START);
        return Mth.positiveModulo((elapsed + partialTick) * 2.0F, 20.0F) / 20.0F;
    }

    private void setAttackPhase(byte phase) {
        if (phase == this.entityData.get(DATA_ATTACK_PHASE)) return;
        this.entityData.set(DATA_ATTACK_PHASE, phase);
        this.entityData.set(DATA_PHASE_ANIMATION_START, this.level().getGameTime());
    }

    private boolean performChargeImpact() {
        AABB impactBox = this.getBoundingBox().inflate(0.45D, 0.25D, 0.45D);
        boolean hit = false;
        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, impactBox,
                entity -> entity.isAlive() && entity != this
                        && !(entity instanceof ButterCatBoss))) {
            if (!victim.hurt(this.damageSources().mobAttack(this), CHARGE_DAMAGE)) continue;
            double x = victim.getX() - this.getX();
            double z = victim.getZ() - this.getZ();
            victim.knockback(1.55D, -x, -z);
            hit = true;
        }
        if (hit) playChargeImpactEffects();
        return hit;
    }

    private void playChargeImpactEffects() {
        this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.4F, 1.25F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
                    22, 0.5D, 0.3D, 0.5D, 0.2D);
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    this.getX(), this.getY() + 0.15D, this.getZ(),
                    12, 0.55D, 0.1D, 0.55D, 0.08D);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ChargeCooldown", this.chargeCooldown);
        CompoundTag inheritedGenome = this.entityData.get(DATA_INHERITED_GENOME);
        if (!inheritedGenome.isEmpty()) {
            tag.put(INHERITED_GENOME_TAG, inheritedGenome.copy());
        }
        tag.putBoolean(SUMMONING_TAG, isSummoning());
        tag.putLong(SUMMON_START_TAG, getSummonStartGameTime());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.chargeCooldown = Math.max(0, tag.getInt("ChargeCooldown"));
        if (tag.contains(INHERITED_GENOME_TAG, Tag.TAG_COMPOUND)) {
            this.entityData.set(DATA_INHERITED_GENOME,
                    tag.getCompound(INHERITED_GENOME_TAG).copy());
        }
        boolean summoning = tag.getBoolean(SUMMONING_TAG);
        this.entityData.set(DATA_SUMMONING, summoning);
        this.entityData.set(DATA_SUMMON_START, tag.getLong(SUMMON_START_TAG));
        if (summoning) this.setNoAi(true);
    }

    private final class ChargeAttackGoal extends Goal {
        private int chargeTicks;
        private Vec3 chargeDirection = Vec3.ZERO;
        private Vec3 dashStart = Vec3.ZERO;
        private boolean impacted;

        private ChargeAttackGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = ButterCatBoss.this.getTarget();
            if (chargeCooldown > 0 || target == null || !target.isAlive()
                    || !ButterCatBoss.this.onGround()) return false;
            double distance = ButterCatBoss.this.distanceToSqr(target);
            return distance >= CHARGE_MIN_DISTANCE_SQR
                    && distance <= CHARGE_MAX_DISTANCE_SQR
                    && ButterCatBoss.this.getSensing().hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = ButterCatBoss.this.getTarget();
            return !this.impacted && this.chargeTicks < CHARGE_WINDUP_TICKS + CHARGE_ACTIVE_TICKS
                    && target != null && target.isAlive();
        }

        @Override
        public void start() {
            this.chargeTicks = 0;
            this.impacted = false;
            this.chargeDirection = Vec3.ZERO;
            this.dashStart = Vec3.ZERO;
            ButterCatBoss.this.getNavigation().stop();
            ButterCatBoss.this.setAggressive(true);
            ButterCatBoss.this.setAttackPhase(ATTACK_WINDUP);
            ButterCatBoss.this.playSound(SoundEvents.CAT_HISS, 1.5F, 0.72F);
        }

        @Override
        public void tick() {
            this.chargeTicks++;
            LivingEntity target = ButterCatBoss.this.getTarget();
            if (target == null) return;

            if (this.chargeTicks < CHARGE_WINDUP_TICKS) {
                ButterCatBoss.this.getLookControl().setLookAt(target, 35.0F, 30.0F);
                Vec3 motion = ButterCatBoss.this.getDeltaMovement();
                ButterCatBoss.this.setDeltaMovement(motion.x * 0.25D, motion.y,
                        motion.z * 0.25D);
                return;
            }

            if (ButterCatBoss.this.isWindingUp()) {
                Vec3 towardTarget = target.getEyePosition()
                        .subtract(ButterCatBoss.this.position()
                                .add(0.0D, ButterCatBoss.this.getBbHeight() * 0.45D, 0.0D));
                Vec3 horizontal = new Vec3(towardTarget.x, 0.0D, towardTarget.z);
                if (horizontal.lengthSqr() < 1.0E-4D) horizontal = ButterCatBoss.this.getLookAngle();
                this.chargeDirection = horizontal.normalize();
                this.dashStart = ButterCatBoss.this.position();
                ButterCatBoss.this.setAttackPhase(ATTACK_DASH);
                ButterCatBoss.this.setYRot((float) (Mth.atan2(-this.chargeDirection.x,
                        this.chargeDirection.z) * Mth.RAD_TO_DEG));
                ButterCatBoss.this.yBodyRot = ButterCatBoss.this.getYRot();
                ButterCatBoss.this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.3F, 0.65F);
            }

            double travelled = horizontalDistance(this.dashStart,
                    ButterCatBoss.this.position());
            if (travelled >= CHARGE_DISTANCE) {
                finishDash();
                return;
            }
            double remaining = CHARGE_DISTANCE - travelled;
            double speed = Math.min(CHARGE_SPEED, remaining);
            Vec3 current = ButterCatBoss.this.getDeltaMovement();
            ButterCatBoss.this.setDeltaMovement(this.chargeDirection.x * speed,
                    Math.max(current.y, -0.2D), this.chargeDirection.z * speed);
            this.impacted = ButterCatBoss.this.performChargeImpact();
            if (this.impacted) {
                finishDash();
                return;
            }
            if (ButterCatBoss.this.horizontalCollision) {
                ButterCatBoss.this.playChargeImpactEffects();
                finishDash();
            }
        }

        @Override
        public void stop() {
            ButterCatBoss.this.setAttackPhase(ATTACK_IDLE);
            ButterCatBoss.this.setAggressive(false);
            ButterCatBoss.this.chargeCooldown = CHARGE_COOLDOWN_TICKS;
            Vec3 motion = ButterCatBoss.this.getDeltaMovement();
            ButterCatBoss.this.setDeltaMovement(0.0D, motion.y, 0.0D);
        }

        private static double horizontalDistance(Vec3 first, Vec3 second) {
            double x = second.x - first.x;
            double z = second.z - first.z;
            return Math.sqrt(x * x + z * z);
        }

        private void finishDash() {
            Vec3 motion = ButterCatBoss.this.getDeltaMovement();
            ButterCatBoss.this.setDeltaMovement(0.0D, motion.y, 0.0D);
            ButterCatBoss.this.setAttackPhase(ATTACK_IDLE);
            this.impacted = true;
        }
    }
}
