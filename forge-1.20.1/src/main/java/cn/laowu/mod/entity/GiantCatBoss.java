package cn.laowu.mod.entity;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.accessory.CatAccessoryItems;
import cn.laowu.mod.genetics.CatGenome;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** Four-times-size cat with server-authoritative, collision-aware roll and slam attacks. */
public final class GiantCatBoss extends Monster {
    public static final float MODEL_SCALE = 4.0F, BASE_WIDTH = 0.5F, BASE_HEIGHT = 0.75F;
    public static final float WIDTH = 2.0F, HEIGHT = 3.0F;
    public static final int SUMMON_DURATION_TICKS = 60;
    public static final byte SUMMON = GiantCatBossCombat.SUMMON, IDLE = GiantCatBossCombat.IDLE,
            MELEE = GiantCatBossCombat.MELEE, ROLL_WINDUP = GiantCatBossCombat.ROLL_WINDUP,
            ROLL = GiantCatBossCombat.ROLL, ROLL_RECOVERY = GiantCatBossCombat.ROLL_RECOVERY,
            JUMP_WINDUP = GiantCatBossCombat.JUMP_WINDUP, JUMP = GiantCatBossCombat.JUMP,
            SLAM_RECOVERY = GiantCatBossCombat.SLAM_RECOVERY;
    private static final EntityDataAccessor<Byte> DATA_PHASE =
            SynchedEntityData.defineId(GiantCatBoss.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> DATA_PHASE_START =
            SynchedEntityData.defineId(GiantCatBoss.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_ROLL_RECOVERY_START =
            SynchedEntityData.defineId(GiantCatBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_RECOVERY_FROM_PHASE =
            SynchedEntityData.defineId(GiantCatBoss.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_RECOVERY_FROM_TICKS =
            SynchedEntityData.defineId(GiantCatBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<CompoundTag> DATA_GENOME =
            SynchedEntityData.defineId(GiantCatBoss.class, EntityDataSerializers.COMPOUND_TAG);
    private final ServerBossEvent bossEvent = new ServerBossEvent(getDisplayName(),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private final Set<ServerPlayer> trackingPlayers = new HashSet<>();
    private final GiantCatBossCombat contacts = new GiantCatBossCombat();
    private final GiantCatBossCombat touchContacts = new GiantCatBossCombat();
    private int attackCooldown = 20;
    private boolean nextSpecialIsJump;
    private float rollYaw;
    private float rollBodyStartYaw;
    private boolean clientHadRollFacing;
    private final PursuitMoveControl pursuitMoveControl;

    public GiantCatBoss(EntityType<? extends GiantCatBoss> type, Level level) {
        super(type, level);
        this.pursuitMoveControl = new PursuitMoveControl();
        this.moveControl = this.pursuitMoveControl;
        this.xpReward = 80;
        this.setPersistenceRequired();
        this.bossEvent.setDarkenScreen(false);
        this.bossEvent.setPlayBossMusic(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 800.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D).add(Attributes.MOVEMENT_SPEED, 0.33D)
                .add(Attributes.FOLLOW_RANGE, 40.0D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, IDLE);
        this.entityData.define(DATA_PHASE_START, 0L);
        this.entityData.define(DATA_ROLL_RECOVERY_START, 60);
        this.entityData.define(DATA_RECOVERY_FROM_PHASE, IDLE);
        this.entityData.define(DATA_RECOVERY_FROM_TICKS, 0);
        this.entityData.define(DATA_GENOME, new CompoundTag());
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public byte getPhase() { return this.entityData.get(DATA_PHASE); }

    public float getPhaseTicks(float partialTick) {
        return Math.max(0.0F, this.level().getGameTime() - this.entityData.get(DATA_PHASE_START) + partialTick);
    }

    public float getRollRecoveryStartTicks() { return this.entityData.get(DATA_ROLL_RECOVERY_START); }
    public byte getRecoveryFromPhase() { return this.entityData.get(DATA_RECOVERY_FROM_PHASE); }
    public float getRecoveryFromTicks() { return this.entityData.get(DATA_RECOVERY_FROM_TICKS); }
    public boolean isSummoning() { return getPhase() == SUMMON; }

    public float getSummonProgress(float partialTick) {
        return isSummoning() ? Mth.clamp(getPhaseTicks(partialTick) / SUMMON_DURATION_TICKS, 0.0F, 1.0F) : 1.0F;
    }

    public void beginSummoning() {
        setPhase(SUMMON);
        this.setTarget(null);
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.bossEvent.removeAllPlayers();
    }

    public void setInheritedGenome(CatGenome genome) { this.entityData.set(DATA_GENOME, genome.save()); }

    public Optional<CatGenome> getInheritedGenome() {
        CompoundTag tag = this.entityData.get(DATA_GENOME);
        return tag.isEmpty() ? Optional.empty() : CatGenome.load(tag);
    }

    private void setPhase(byte phase) {
        byte previous = getPhase();
        if (phase == previous) return;
        if (phase == ROLL_WINDUP) {
            this.rollYaw = targetYaw(this.getTarget());
            this.rollBodyStartYaw = this.getYRot();
        } else if (phase == ROLL_RECOVERY) {
            this.rollBodyStartYaw = this.getYRot();
        }
        if (phase == ROLL_RECOVERY || phase == SLAM_RECOVERY) {
            this.entityData.set(DATA_RECOVERY_FROM_PHASE, previous);
            this.entityData.set(DATA_RECOVERY_FROM_TICKS, Mth.clamp((int) getPhaseTicks(0), 0, 100));
        }
        if (previous == ROLL && phase == ROLL_RECOVERY) {
            this.entityData.set(DATA_ROLL_RECOVERY_START, Mth.clamp((int) getPhaseTicks(0), 0, 60));
        }
        this.entityData.set(DATA_PHASE, phase);
        this.entityData.set(DATA_PHASE_START, this.level().getGameTime());
        if (phase != IDLE) this.getNavigation().stop();
        if (phase != ROLL && phase != JUMP) {
            this.setDeltaMovement(0.0D, Math.min(0.0D, this.getDeltaMovement().y), 0.0D);
        }
        if (phase == IDLE) this.attackCooldown = Math.max(this.attackCooldown, Mth.nextInt(this.random, 80, 120));
        if (phase == ROLL) this.contacts.clearHits();
        this.setAggressive(phase == ROLL_WINDUP || phase == ROLL || phase == JUMP_WINDUP || phase == JUMP);
    }

    @Override
    public void tick() {
        // Abort before travel, so toggling NoAI/death cannot grant one final damaging movement.
        if (!this.level().isClientSide && (!this.isAlive() || (this.isNoAi() && !isSummoning()))
                && getPhase() != IDLE) {
            setPhase(IDLE);
            this.contacts.clearHits();
            this.setDeltaMovement(Vec3.ZERO);
            this.getNavigation().stop();
        }
        super.tick();
        if (this.level().isClientSide) {
            boolean rollFacing = getPhase() == ROLL_WINDUP || getPhase() == ROLL || getPhase() == ROLL_RECOVERY;
            if (rollFacing || this.clientHadRollFacing) {
                // Vanilla body control follows movement, which is perpendicular
                // to our rolling body. Keep the first idle frame continuous too.
                this.yBodyRotO = this.yRotO;
                this.yBodyRot = this.getYRot();
            }
            this.clientHadRollFacing = rollFacing;
            return;
        }
        applyRollFacing();
        this.bossEvent.setProgress(Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.0F, 1.0F));
        this.bossEvent.setName(this.getDisplayName());
        if (isSummoning() && this.isAlive()) {
            this.setTarget(null);
            if (getPhaseTicks(0) >= SUMMON_DURATION_TICKS) finishSummoning();
        }
        if (getPhase() == IDLE && this.isAlive() && !this.isNoAi()) {
            for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(2.5D, 0, 2.5D), this::validVictim)) {
                doHurtTarget(victim);
                if (!this.isAlive()) break;
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isSummoning() || !this.isAlive()) return;
        if (this.attackCooldown > 0) this.attackCooldown--;
        LivingEntity target = this.getTarget();
        if (!validVictim(target)) {
            this.setTarget(null);
            target = null;
        }
        byte phase = getPhase();
        int elapsed = (int) getPhaseTicks(0);
        // Roll and jump are advanced after actual collision-aware movement, never by predicted landing.
        if (phase == ROLL || phase == JUMP) return;
        // Anticipation keeps its entry aim: changing this endpoint mid-turn can
        // flip the shortest yaw arc. Only actual ROLL travel steers, once a tick.
        applyRollFacing();
        byte next = GiantCatBossCombat.nextPhase(phase, elapsed, this.onGround(), false, target != null);
        if (next != phase) {
            if (next == ROLL) {
                faceYaw(this.rollYaw + 90.0F);
                setPhase(ROLL);
            } else if (next == JUMP) {
                launchJump(target);
            } else {
                setPhase(next);
            }
            return;
        }
        if (phase != IDLE) {
            if (target != null && phase != ROLL_WINDUP && phase != ROLL_RECOVERY) {
                this.getLookControl().setLookAt(target, 20.0F, 30.0F);
            }
            return;
        }
        if (target == null) {
            this.getNavigation().stop();
            return;
        }
        this.getLookControl().setLookAt(target, 20.0F, 30.0F);
        double distance = this.distanceToSqr(target);
        if (this.attackCooldown == 0 && this.onGround() && this.getSensing().hasLineOfSight(target)) {
            if (distance <= 22.0D * 22.0D) {
                setPhase(this.nextSpecialIsJump ? JUMP_WINDUP : ROLL_WINDUP);
                this.nextSpecialIsJump = !this.nextSpecialIsJump;
                return;
            }
        }
        if (canPursueDirectly(target)) {
            // Bring the muzzle to the player instead of walking the central
            // body through them. Rotate naturally only during ordinary pursuit.
            this.getNavigation().stop();
            this.pursuitMoveControl.closePursuit = true;
            this.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0D);
        } else {
            this.pursuitMoveControl.closePursuit = false;
            this.getNavigation().moveTo(target, 1.0D);
            smoothPursuitPath();
        }
    }

    /** One yaw writer for both path nodes and the final muzzle approach. */
    private final class PursuitMoveControl extends MoveControl {
        private boolean closePursuit;

        PursuitMoveControl() { super(GiantCatBoss.this); }

        @Override
        protected float rotlerp(float current, float wanted, float maximum) {
            return super.rotlerp(current, wanted, Math.min(maximum, 10.0F));
        }

        @Override
        public void tick() {
            if (getPhase() != IDLE || getTarget() == null) {
                this.operation = Operation.WAIT;
                setSpeed(0);
                setXxa(0); setZza(0);
                return;
            }
            if (this.operation == Operation.MOVE_TO) {
                double dx = this.wantedX - getX(), dz = this.wantedZ - getZ();
                float aim = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
                float turned = rotlerp(getYRot(), aim, 10.0F);
                float error = Math.abs(Mth.wrapDegrees(aim - turned));
                if (error > (this.closePursuit ? 25.0F : 60.0F)
                        || (this.closePursuit && dx * dx + dz * dz <= 2.4D * 2.4D)) {
                    // Align BEFORE vanilla can request a step jump and enter
                    // JUMPING (which deliberately keeps its launch heading).
                    setYRot(turned);
                    this.operation = Operation.WAIT;
                    setSpeed(0);
                    setXxa(0); setZza(0);
                    setDeltaMovement(0, getDeltaMovement().y, 0);
                    return;
                }
            }
            // Aligned moves keep vanilla step/jump and collision handling;
            // rotlerp is called only once, including turn-in-place ticks.
            super.tick();
        }
    }

    @Override
    protected float tickHeadTurn(float yaw, float distance) {
        if (getPhase() == IDLE) {
            // This artist model has no independent ordinary head rotation.
            // Vanilla's stationary look/body controller otherwise aims the
            // body at the target, then snaps back to movement yaw on its first
            // walking tick. Render and muzzle collision must share one heading
            // on both server and client, including turn-in-place ticks.
            this.yBodyRot = this.getYRot();
            return distance;
        }
        return super.tickHeadTurn(yaw, distance);
    }

    private boolean canPursueDirectly(LivingEntity target) {
        return canPursueDirectly(target.position());
    }

    private void smoothPursuitPath() {
        var path = this.getNavigation().getPath();
        if (path == null || path.isDone()) return;
        if (path.getNextNode().type != BlockPathTypes.WALKABLE) return;
        int next = path.getNextNodeIndex();
        int selected = next;
        // Look ahead only three nodes/3.2 blocks, using the same full-width,
        // loaded-chunk and supported-feet checks as the final approach. Never
        // cut through walls, across pits, or past a step that needs vanilla AI.
        for (int node = next + 1; node < Math.min(path.getNodeCount(), next + 4); node++) {
            Vec3 point = path.getEntityPosAtNode(this, node);
            if (path.getNode(node).type != BlockPathTypes.WALKABLE
                    || !canPursueDirectly(point) || !safeShortcutTerrain(point)) break;
            selected = node;
        }
        if (selected == next) return;
        path.setNextNodeIndex(selected);
        Vec3 waypoint = path.getNextEntityPos(this);
        this.getMoveControl().setWantedPosition(waypoint.x, waypoint.y, waypoint.z, 1.0D);
    }

    private boolean safeShortcutTerrain(Vec3 point) {
        // Geometric clearance alone would cut native fire/magma/berry/fluid
        // detours. Check the whole swept body using the loader's vanilla path
        // classifier, including hazards that appeared after path computation.
        // canPursueDirectly already checked this envelope plus its neighbors is
        // loaded; the volume is bounded by the three-node/3.2-block lookahead.
        AABB swept = getBoundingBox().expandTowards(point.subtract(position())).deflate(1.0E-5D);
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        
        for (BlockPos block : BlockPos.betweenClosed(BlockPos.containing(swept.minX, swept.minY, swept.minZ),
                BlockPos.containing(swept.maxX, swept.maxY, swept.maxZ))) {
            var terrain = WalkNodeEvaluator.getBlockPathTypeStatic(level(), probe.set(block));
            if ((terrain != BlockPathTypes.OPEN && terrain != BlockPathTypes.WALKABLE)
                    || getPathfindingMalus(terrain) != 0) return false;
        }
        return true;
    }

    private boolean canPursueDirectly(Vec3 position) {
        Vec3 offset = position.subtract(this.position());
        if (!this.onGround() || Math.abs(offset.y) > 0.1D || offset.lengthSqr() > 3.2D * 3.2D) return false;
        AABB swept = this.getBoundingBox().expandTowards(offset);
        // Collision iterators inspect neighboring blocks too. Check their full
        // chunk envelope first so close pursuit never causes a chunk load.
        AABB checked = swept.inflate(1.0D).expandTowards(0, -0.15D, 0);
        if (!this.level().hasChunksAt(BlockPos.containing(checked.minX, checked.minY, checked.minZ),
                BlockPos.containing(checked.maxX, checked.maxY, checked.maxZ))) return false;
        if (this.level().getBlockCollisions(this, swept.deflate(1.0E-5D)).iterator().hasNext()) return false;

        // At most eight samples over 3.2 blocks, checking the center and all
        // four feet corners against real collision shapes, not just block IDs.
        int steps = Math.max(1, (int) Math.ceil(offset.horizontalDistance() / 0.5D));
        double inset = this.getBbWidth() * 0.5D - 0.05D;
        for (int step = 0; step <= steps; step++) {
            Vec3 feet = this.position().add(offset.scale((double) step / steps));
            if (!hasPursuitSupport(feet.x, feet.y, feet.z)) return false;
            for (int x = -1; x <= 1; x += 2) {
                for (int z = -1; z <= 1; z += 2) {
                    if (!hasPursuitSupport(feet.x + x * inset, feet.y, feet.z + z * inset)) return false;
                }
            }
        }
        return true;
    }

    private boolean hasPursuitSupport(double x, double y, double z) {
        AABB foot = new AABB(x - 0.025D, y - 0.1D, z - 0.025D, x + 0.025D, y, z + 0.025D);
        return this.level().getBlockCollisions(this, foot).iterator().hasNext();
    }

    private boolean validVictim(LivingEntity victim) {
        return victim != null && victim != this && victim.isAlive() && !victim.isSpectator()
                && !(victim instanceof GiantCatBoss) && !this.isAlliedTo(victim)
                && (!(victim instanceof Player player) || !player.getAbilities().instabuild);
    }

    @Override
    public boolean doHurtTarget(Entity victim) {
        // Ordinary bites use the forward artist head/muzzle, independently of
        // the central movement box; special attacks keep their own geometry.
        if (getPhase() != IDLE || this.isNoAi() || !this.isAlive() || this.level().isClientSide
                || !(victim instanceof LivingEntity target) || !validVictim(target)
                || !headTouchesVictim(target)
                || !this.touchContacts.canHit(target.getUUID(), this.tickCount)
                || !this.getSensing().hasLineOfSight(target)) return false;
        boolean hit = target.hurt(this.damageSources().mobAttack(this), 8.0F);
        if (hit) {
            this.touchContacts.recordHit(target.getUUID(), this.tickCount);
            // Successful bites add a visible shove; vanilla and this extra
            // impulse both honor the victim's knockback resistance/events.
            target.knockback(0.6D, this.getX() - target.getX(), this.getZ() - target.getZ());
            this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.1F, 0.7F);
        }
        return hit;
    }

    private boolean headTouchesVictim(LivingEntity victim) {
        AABB box = victim.getBoundingBox();
        return GiantCatBossCombat.headTouches(this.yBodyRot,
                box.minX - getX(), box.minY - getY(), box.minZ - getZ(),
                box.maxX - getX(), box.maxY - getY(), box.maxZ - getZ());
    }

    private float targetYaw(LivingEntity target) {
        return target == null ? this.getYRot()
                : (float) (Mth.atan2(target.getZ() - this.getZ(), target.getX() - this.getX()) * Mth.RAD_TO_DEG) - 90.0F;
    }

    private void faceYaw(float yaw) {
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        this.setXRot(0.0F);
    }

    private void applyRollFacing() {
        switch (getPhase()) {
            case ROLL_WINDUP -> faceYaw(GiantCatBossCombat.rollWindupYaw(
                    this.rollBodyStartYaw, this.rollYaw, getPhaseTicks(0)));
            case ROLL -> faceYaw(this.rollYaw + 90.0F);
            case ROLL_RECOVERY -> faceYaw(GiantCatBossCombat.rollRecoveryYaw(
                    this.rollBodyStartYaw, this.rollYaw, getPhaseTicks(0)));
            default -> { }
        }
    }

    private void launchJump(LivingEntity target) {
        if (target == null) { setPhase(IDLE); return; }
        // Target position is sampled once. No homing, teleporting, or persisted NoGravity flag.
        double[] launch = GiantCatBossCombat.leap(target.getX() - this.getX(),
                target.getY() - this.getY(), target.getZ() - this.getZ());
        faceYaw(targetYaw(target));
        setPhase(JUMP);
        this.setDeltaMovement(launch[0], launch[1], launch[2]);
        this.hasImpulse = true;
        this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.7F, 0.55F);
    }

    @Override
    public void travel(Vec3 input) {
        byte phase = getPhase();
        if (this.level().isClientSide || !this.isAlive() || this.isNoAi() || phase == IDLE) {
            super.travel(input);
            return;
        }
        if (phase != ROLL && phase != JUMP) {
            // Telegraphs and recoveries are immobile horizontally, but retain gravity.
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            super.travel(Vec3.ZERO);
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            return;
        }
        AABB previousBox = this.getBoundingBox();
        Vec3 previousPosition = this.position();
        Vec3 requested = this.getDeltaMovement();
        if (phase == ROLL) {
            LivingEntity target = this.getTarget();
            if (validVictim(target)) this.rollYaw = GiantCatBossCombat.steer(this.rollYaw, targetYaw(target));
            faceYaw(this.rollYaw + 90.0F);
            double radians = Math.toRadians(this.rollYaw);
            requested = new Vec3(-Math.sin(radians) * 0.48D,
                    this.onGround() ? -GiantCatBossCombat.GRAVITY : requested.y,
                    Math.cos(radians) * 0.48D);
        }
        AABB destination = previousBox.expandTowards(requested);
        boolean loaded = this.level().hasChunksAt(BlockPos.containing(destination.minX, destination.minY, destination.minZ),
                BlockPos.containing(destination.maxX, destination.maxY, destination.maxZ));
        if (!loaded) {
            // Never load chunks for an attack or advance into an unloaded landing zone.
            setPhase(phase == ROLL ? ROLL_RECOVERY : SLAM_RECOVERY);
            this.setDeltaMovement(0.0D, Math.min(0.0D, requested.y), 0.0D);
            return;
        }
        this.setDeltaMovement(requested);
        this.move(MoverType.SELF, requested);
        Vec3 actual = this.position().subtract(previousPosition);
        this.setDeltaMovement(this.horizontalCollision ? 0.0D : requested.x,
                (this.verticalCollision ? 0.0D : requested.y) - GiantCatBossCombat.GRAVITY,
                this.horizontalCollision ? 0.0D : requested.z);
        int elapsed = (int) getPhaseTicks(0);
        if (phase == ROLL) {
            performRollImpact(previousBox, actual);
            byte next = GiantCatBossCombat.nextPhase(ROLL, elapsed, this.onGround(), this.horizontalCollision, true);
            if (next != ROLL) setPhase(next);
        } else {
            boolean landed = this.verticalCollision && requested.y < 0.0D && this.onGround();
            byte next = GiantCatBossCombat.nextPhase(JUMP, elapsed, landed, false, true);
            if (next != JUMP) {
                // A timeout/unloaded edge is an abort, not a mid-air slam.
                if (landed && elapsed > 1) performSlam();
                setPhase(next);
            }
        }
        this.calculateEntityAnimation(false);
    }

    private void performRollImpact(AABB previousBox, Vec3 actual) {
        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class,
                ButterCatCharge.sweptBox(previousBox, actual), this::validVictim)) {
            if (!this.contacts.canHit(victim.getUUID(), this.tickCount)
                    || !ButterCatCharge.touches(previousBox, actual, victim.getBoundingBox())
                    || !this.getSensing().hasLineOfSight(victim)) continue;
            if (victim.hurt(this.damageSources().mobAttack(this), 12.0F)) {
                this.contacts.recordHit(victim.getUUID(), this.tickCount);
                victim.knockback(0.9D, this.getX() - victim.getX(), this.getZ() - victim.getZ());
            }
            if (!this.isAlive()) break;
        }
    }

    private void performSlam() {
        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(4.0D, 1.5D, 4.0D), this::validVictim)) {
            double dx = victim.getX() - this.getX(), dz = victim.getZ() - this.getZ();
            if (dx * dx + dz * dz > 16.0D || !this.getSensing().hasLineOfSight(victim)) continue;
            if (victim.hurt(this.damageSources().mobAttack(this), 18.0F)) {
                victim.knockback(1.25D, -dx, -dz);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0), this);
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), this);
            }
            if (!this.isAlive()) break;
        }
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.15D, this.getZ(),
                    60, 2.0D, 0.1D, 2.0D, 0.09D);
            server.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 0.25D, this.getZ(),
                    35, 2.0D, 0.3D, 2.0D, 0.15D);
        }
        this.playSound(SoundEvents.GENERIC_EXPLODE, 1.5F, 0.6F);
    }

    private void finishSummoning() {
        setPhase(IDLE);
        this.attackCooldown = 20;
        this.trackingPlayers.forEach(this.bossEvent::addPlayer);
        if (!(this.level() instanceof ServerLevel server)) return;
        double y = this.getY() + BASE_HEIGHT * 0.55D;
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), y, this.getZ(), 1, 0, 0, 0, 0);
        server.sendParticles(ParticleTypes.POOF, this.getX(), y, this.getZ(), 42, 0.7, 0.45, 0.7, 0.14);
        server.sendParticles(ParticleTypes.CLOUD, this.getX(), y, this.getZ(), 24, 0.8, 0.35, 0.8, 0.08);
        server.playSound(null, this.getX(), y, this.getZ(), SoundEvents.GENERIC_EXPLODE,
                this.getSoundSource(), 1.25F, 0.88F + this.random.nextFloat() * 0.08F);
        this.gameEvent(net.minecraft.world.level.gameevent.GameEvent.EXPLODE);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return (isSummoning() && !source.is(DamageTypes.FELL_OUT_OF_WORLD) && !source.is(DamageTypes.GENERIC_KILL))
                || super.isInvulnerableTo(source);
    }

    @Override
    public void knockback(double strength, double x, double z) { /* Immovable boss; includes scripted knockback. */ }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

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
    protected SoundEvent getAmbientSound() { return SoundEvents.CAT_AMBIENT; }
    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.CAT_HURT; }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.CAT_DEATH; }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        // Preserve the existing seeded trophy roll independently of new rewards.
        boolean dropsTrophy = this.random.nextFloat() < 0.25F;
        int drops = Mth.nextInt(this.random, 1, 3);
        for (int roll = 0; roll < drops; roll++) this.spawnAtLocation(new ItemStack(randomSuperReward()));
        if (dropsTrophy) this.spawnAtLocation(new ItemStack(CatAccessoryItems.giantReward()));
    }

    /** Same seven equally likely advanced training rewards as Butter Cat. */
    private Item randomSuperReward() {
        return switch (this.random.nextInt(7)) {
            case 0 -> LaoWuMod.SUPER_ATTACK_CAT_CAN.get();
            case 1 -> LaoWuMod.SUPER_HEALTH_CAT_CAN.get();
            case 2 -> LaoWuMod.SUPER_SPEED_CAT_CAN.get();
            case 3 -> LaoWuMod.SUPER_STAMINA_CAT_CAN.get();
            case 4 -> LaoWuMod.SUPER_INTELLIGENCE_CAT_CAN.get();
            case 5 -> LaoWuMod.SUPER_LUCK_CAT_CAN.get();
            default -> LaoWuMod.SUPER_DRIED_FISH.get();
        };
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("InheritedCatGenome", this.entityData.get(DATA_GENOME).copy());
        tag.putByte("GiantCatPhase", getPhase());
        tag.putInt("GiantCatPhaseTicks", (int) getPhaseTicks(0));
        tag.putInt("GiantCatAttackCooldown", this.attackCooldown);
        tag.putBoolean("GiantCatNextJump", this.nextSpecialIsJump);
        tag.putFloat("GiantCatRollYaw", this.rollYaw);
        tag.putFloat("GiantCatRollBodyStartYaw", this.rollBodyStartYaw);
        tag.putInt("GiantCatRollRecoveryStart", this.entityData.get(DATA_ROLL_RECOVERY_START));
        tag.putByte("GiantCatRecoveryFromPhase", getRecoveryFromPhase());
        tag.putInt("GiantCatRecoveryFromTicks", this.entityData.get(DATA_RECOVERY_FROM_TICKS));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        migrateLegacyAttributes();
        if (tag.contains("InheritedCatGenome", Tag.TAG_COMPOUND)) {
            this.entityData.set(DATA_GENOME, tag.getCompound("InheritedCatGenome").copy());
        }
        this.attackCooldown = Mth.clamp(tag.getInt("GiantCatAttackCooldown"), 14, 100);
        this.nextSpecialIsJump = tag.getBoolean("GiantCatNextJump");
        byte saved = tag.getByte("GiantCatPhase");
        boolean hadPhase = tag.contains("GiantCatPhase", Tag.TAG_BYTE);
        byte restored = !hadPhase ? IDLE : saved == SUMMON ? SUMMON
                : (saved == ROLL || saved == ROLL_WINDUP || saved == ROLL_RECOVERY) ? ROLL_RECOVERY
                : (saved == JUMP || saved == JUMP_WINDUP || saved == SLAM_RECOVERY) ? SLAM_RECOVERY : IDLE;
        this.rollYaw = tag.contains("GiantCatRollYaw", Tag.TAG_FLOAT)
                ? tag.getFloat("GiantCatRollYaw") : this.getYRot();
        if (!Float.isFinite(this.rollYaw)) this.rollYaw = this.getYRot();
        this.rollBodyStartYaw = saved == ROLL_RECOVERY && tag.contains("GiantCatRollBodyStartYaw", Tag.TAG_FLOAT)
                ? tag.getFloat("GiantCatRollBodyStartYaw") : this.getYRot();
        if (!Float.isFinite(this.rollBodyStartYaw)) this.rollBodyStartYaw = this.getYRot();
        this.entityData.set(DATA_PHASE, restored);
        boolean resumingRecovery = saved == restored && (restored == ROLL_RECOVERY || restored == SLAM_RECOVERY);
        int duration = restored == ROLL_RECOVERY ? 30 : 40;
        int elapsed = restored == SUMMON ? Mth.clamp(tag.getInt("GiantCatPhaseTicks"), 0, 60)
                : resumingRecovery ? Mth.clamp(tag.getInt("GiantCatPhaseTicks"), 0, duration) : 0;
        this.entityData.set(DATA_PHASE_START, this.level().getGameTime() - elapsed);
        int rollTicks = saved == ROLL ? tag.getInt("GiantCatPhaseTicks") : tag.getInt("GiantCatRollRecoveryStart");
        this.entityData.set(DATA_ROLL_RECOVERY_START, Mth.clamp(rollTicks, 0, 60));
        if (restored == ROLL_RECOVERY || restored == SLAM_RECOVERY) {
            byte from = resumingRecovery ? tag.getByte("GiantCatRecoveryFromPhase") : saved;
            int fromTicks = resumingRecovery ? tag.getInt("GiantCatRecoveryFromTicks") : tag.getInt("GiantCatPhaseTicks");
            boolean validSource = restored == ROLL_RECOVERY ? from == ROLL || from == ROLL_WINDUP
                    : from == JUMP || from == JUMP_WINDUP;
            if (!validSource) {
                // Older saves did not retain the source of a recovery. Use the
                // same completed attack pose their old renderer would have shown.
                from = restored == ROLL_RECOVERY ? ROLL : JUMP;
                fromTicks = restored == ROLL_RECOVERY ? rollTicks : 24;
            }
            this.entityData.set(DATA_RECOVERY_FROM_PHASE, from);
            this.entityData.set(DATA_RECOVERY_FROM_TICKS, Mth.clamp(fromTicks, 0, 100));
        } else {
            this.entityData.set(DATA_RECOVERY_FROM_PHASE, IDLE);
            this.entityData.set(DATA_RECOVERY_FROM_TICKS, 0);
        }
        this.contacts.clearHits();
        this.touchContacts.clearHits();
        this.setDeltaMovement(Vec3.ZERO);
        this.setAggressive(false);
    }

    private void migrateLegacyAttributes() {
        // Vanilla has restored both saved attributes and saved Health before this
        // hook. Only replace known old defaults: preserve custom bases/modifiers.
        var maximumHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maximumHealth != null && Math.abs(maximumHealth.getBaseValue() - 550.0D) < 1.0E-6D) {
            float previousMaximum = this.getMaxHealth();
            float previousHealth = this.getHealth();
            maximumHealth.setBaseValue(800.0D);
            this.setHealth(GiantCatBossCombat.healthAfterMaxChange(
                    previousHealth, previousMaximum, this.getMaxHealth()));
        }
        var movementSpeed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null && Math.abs(movementSpeed.getBaseValue() - 0.27D) < 1.0E-6D) {
            movementSpeed.setBaseValue(0.33D);
        }
    }
}
