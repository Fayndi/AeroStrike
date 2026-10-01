package net.strike.aerostrike.common.entity.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.strike.aerostrike.common.chunkloading.MissileChunkManager;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Base class for all cruise missiles in AeroStrike.
 *
 * Implements a 3-stage flight state machine:
 * 1. BOOST: Solid rocket booster climb to cruising altitude.
 * 2. CRUISE: Turbofan cruise flight with terrain-following radar/raycasting.
 * 3. TERMINAL: High-speed terminal dive towards target coordinates.
 *
 * Integrated with chunkloading management and GeckoLib 4.
 */
public abstract class AbstractCruiseMissileEntity extends Entity implements GeoEntity {

    public enum FlightPhase {
        STANDBY,
        BOOST,
        CRUISE,
        TERMINAL
    }

    private static final EntityDataAccessor<Byte> FLIGHT_PHASE =
            SynchedEntityData.defineId(AbstractCruiseMissileEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Optional<BlockPos>> TARGET_POS =
            SynchedEntityData.defineId(AbstractCruiseMissileEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Flight variables
    protected int ticksInFlight = 0;

    public AbstractCruiseMissileEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true; // We perform custom high-precision raycast physics
    }

    public abstract float getMaxCruiseSpeed();      // Blocks per tick (e.g. 1.2f = 24 m/s)
    public abstract float getAcceleration();        // Acceleration rate per tick
    public abstract float getTurnRate();            // Max steering angle per tick in degrees
    public abstract int getBoosterDurationTicks();  // Duration of solid rocket booster phase (or drop phase)
    public abstract float getCruiseClearance();     // Desired altitude above ground in cruise phase
    public abstract float getWarheadYield();        // Explosion power

    public boolean isAirLaunched() {
        return false;
    }

    public float getRadarSignature() {
        return 1.0f; // 1.0 = standard, 0.15 = stealth
    }

    public int getPenetrationDepth() {
        return 0; // Number of blocks to penetrate before warhead detonation (BROACH bunker-buster)
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(FLIGHT_PHASE, (byte) FlightPhase.STANDBY.ordinal());
        this.entityData.define(TARGET_POS, Optional.empty());
    }

    public FlightPhase getFlightPhase() {
        byte ordinal = this.entityData.get(FLIGHT_PHASE);
        if (ordinal >= 0 && ordinal < FlightPhase.values().length) {
            return FlightPhase.values()[ordinal];
        }
        return FlightPhase.STANDBY;
    }

    public void setFlightPhase(FlightPhase phase) {
        this.entityData.set(FLIGHT_PHASE, (byte) phase.ordinal());
    }

    @Nullable
    public BlockPos getTargetPos() {
        return this.entityData.get(TARGET_POS).orElse(null);
    }

    public void setTargetPos(@Nullable BlockPos pos) {
        this.entityData.set(TARGET_POS, Optional.ofNullable(pos));
    }

    public void launch(BlockPos target) {
        this.setTargetPos(target);
        this.setFlightPhase(FlightPhase.BOOST);
        this.ticksInFlight = 0;

        Vec3 toTarget = new Vec3(target.getX() - this.getX(), 0, target.getZ() - this.getZ()).normalize();

        if (isAirLaunched()) {
            // Air drop: initial forward momentum along target vector with gentle downward drop
            Vec3 initialMotion = new Vec3(toTarget.x * 0.7, -0.2, toTarget.z * 0.7);
            this.setDeltaMovement(initialMotion);
            this.updateRotationFromMotion(initialMotion);
            if (!this.level().isClientSide) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.PISTON_CONTRACT, SoundSource.HOSTILE, 2.0f, 1.2f);
            }
        } else {
            // Ground launch: 45-degree booster climb towards target direction
            Vec3 initialMotion = new Vec3(toTarget.x * 0.4, 0.7, toTarget.z * 0.4);
            this.setDeltaMovement(initialMotion);
            this.updateRotationFromMotion(initialMotion);

            if (!this.level().isClientSide) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 3.0f, 0.8f);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        FlightPhase phase = getFlightPhase();

        if (phase == FlightPhase.STANDBY) {
            return;
        }

        this.ticksInFlight++;

        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            // 1. Maintain active chunkloading around missile
            MissileChunkManager.forceChunk(serverLevel, this);

            // 2. State-machine flight steering
            switch (phase) {
                case BOOST -> tickBoosterPhase();
                case CRUISE -> tickCruisePhase();
                case TERMINAL -> tickTerminalPhase();
            }

            // 3. Raycast collision sweep from current pos to next pos
            Vec3 currentPos = this.position();
            Vec3 motion = this.getDeltaMovement();
            Vec3 nextPos = currentPos.add(motion);

            HitResult hitResult = this.level().clip(new ClipContext(
                    currentPos, nextPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.ANY,
                    this
            ));

            if (hitResult.getType() != HitResult.Type.MISS) {
                this.setPos(hitResult.getLocation());
                this.onImpact(hitResult);
                return;
            }

            // 4. Move entity
            this.setPos(nextPos);
            this.updateRotationFromMotion(motion);
        } else {
            // Client side: spawn exhaust trails based on flight phase
            spawnExhaustParticles(phase);
        }
    }

    /**
     * Stage 1: Booster Phase. Solid rocket climbs and accelerates to transition altitude.
     */
    protected void tickBoosterPhase() {
        Vec3 motion = this.getDeltaMovement();

        if (isAirLaunched()) {
            // Air-drop phase: free fall under gravity, slight aerodynamic drag, then turbojet ignite
            motion = new Vec3(motion.x * 0.98, motion.y - 0.03, motion.z * 0.98);
            this.setDeltaMovement(motion);

            if (this.ticksInFlight >= getBoosterDurationTicks()) {
                this.setFlightPhase(FlightPhase.CRUISE);
                BlockPos target = getTargetPos();
                if (target != null) {
                    Vec3 horizDir = new Vec3(target.getX() - this.getX(), 0, target.getZ() - this.getZ()).normalize();
                    this.setDeltaMovement(new Vec3(horizDir.x * getMaxCruiseSpeed() * 0.8, -0.05, horizDir.z * getMaxCruiseSpeed() * 0.8));
                }
                if (!this.level().isClientSide) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.2f, 1.8f);
                }
            }
            return;
        }

        float currentSpeed = (float) motion.length();
        float targetSpeed = getMaxCruiseSpeed() * 0.8f;

        if (currentSpeed < targetSpeed) {
            motion = motion.scale(1.0f + (getAcceleration() * 2.0f));
        }

        // Steer towards climb angle (pitch around 35-45 degrees)
        BlockPos target = getTargetPos();
        if (target != null) {
            Vec3 horizDir = new Vec3(target.getX() - this.getX(), 0, target.getZ() - this.getZ()).normalize();
            Vec3 desiredMotion = new Vec3(horizDir.x, 0.6, horizDir.z).normalize().scale(Math.max(currentSpeed, 0.5));
            motion = steerTowards(motion, desiredMotion, getTurnRate());
        }

        this.setDeltaMovement(motion);

        if (this.ticksInFlight >= getBoosterDurationTicks()) {
            this.setFlightPhase(FlightPhase.CRUISE);
            if (!this.level().isClientSide) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5f, 1.8f);
            }
        }
    }

    /**
     * Stage 2: Cruise Phase. Maintains cruise speed, steers towards target X/Z,
     * and contours terrain using downward and forward raycasts.
     */
    protected void tickCruisePhase() {
        BlockPos target = getTargetPos();
        if (target == null) {
            return;
        }

        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double horizontalDistSq = dx * dx + dz * dz;

        // If close enough horizontally, enter terminal dive
        double terminalThreshold = Math.max(64.0, (this.getY() - target.getY()) * 1.5);
        if (horizontalDistSq <= terminalThreshold * terminalThreshold) {
            this.setFlightPhase(FlightPhase.TERMINAL);
            return;
        }

        Vec3 horizontalDir = new Vec3(dx, 0, dz).normalize();

        // Terrain-following elevation calculation
        double desiredY = calculateDesiredCruiseAltitude();
        double yDiff = desiredY - this.getY();
        double targetVy = Mth.clamp(yDiff * 0.1, -0.3, 0.4);

        Vec3 desiredMotion = new Vec3(horizontalDir.x, targetVy, horizontalDir.z).normalize().scale(getMaxCruiseSpeed());
        Vec3 currentMotion = this.getDeltaMovement();

        this.setDeltaMovement(steerTowards(currentMotion, desiredMotion, getTurnRate()));
    }

    /**
     * Stage 3: Terminal Dive. High-speed direct dive onto target coordinates.
     */
    protected void tickTerminalPhase() {
        BlockPos target = getTargetPos();
        if (target == null) {
            return;
        }

        Vec3 toTarget = new Vec3(
                target.getX() + 0.5 - this.getX(),
                target.getY() + 0.5 - this.getY(),
                target.getZ() + 0.5 - this.getZ()
        ).normalize();

        float terminalSpeed = getMaxCruiseSpeed() * 1.25f;
        Vec3 desiredMotion = toTarget.scale(terminalSpeed);
        Vec3 currentMotion = this.getDeltaMovement();

        this.setDeltaMovement(steerTowards(currentMotion, desiredMotion, getTurnRate() * 1.5f));
    }

    /**
     * Terrain-following radar simulation:
     * Scans surface directly below and ahead along the velocity vector.
     */
    protected double calculateDesiredCruiseAltitude() {
        Level lvl = this.level();
        double currentX = this.getX();
        double currentZ = this.getZ();

        // 1. Altitude of terrain directly below
        int groundBelowY = findGroundY(new BlockPos((int) currentX, (int) this.getY(), (int) currentZ));

        // 2. Obstacle scanning ahead (lookahead ~30 blocks along motion)
        Vec3 motion = this.getDeltaMovement();
        Vec3 lookahead = motion.normalize().scale(30.0);
        int groundAheadY = findGroundY(new BlockPos(
                (int) (currentX + lookahead.x),
                (int) (this.getY()),
                (int) (currentZ + lookahead.z)
        ));

        int highestObstacle = Math.max(groundBelowY, groundAheadY);
        return highestObstacle + getCruiseClearance();
    }

    private int findGroundY(BlockPos pos) {
        Level lvl = this.level();
        int searchStartY = Math.min(lvl.getMaxBuildHeight(), (int) this.getY() + 40);
        int minY = lvl.getMinBuildHeight();

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(pos.getX(), searchStartY, pos.getZ());
        while (cursor.getY() > minY) {
            BlockState state = lvl.getBlockState(cursor);
            if (!state.isAir() && state.blocksMotion()) {
                return cursor.getY();
            }
            cursor.move(0, -1, 0);
        }
        return (int) lvl.getSeaLevel();
    }

    /**
     * Spherical linear steering interpolation between two motion vectors with max degrees limit.
     */
    protected Vec3 steerTowards(Vec3 current, Vec3 target, float maxAngleDegrees) {
        double currentLen = current.length();
        double targetLen = target.length();

        if (currentLen < 0.001 || targetLen < 0.001) {
            return target;
        }

        Vec3 from = current.normalize();
        Vec3 to = target.normalize();

        double dot = Mth.clamp(from.dot(to), -1.0, 1.0);
        double angle = Math.acos(dot);
        double maxAngleRad = Math.toRadians(maxAngleDegrees);

        if (angle <= maxAngleRad) {
            return to.scale(targetLen);
        }

        double t = maxAngleRad / angle;
        Vec3 interpolated = from.scale(1.0 - t).add(to.scale(t)).normalize();
        return interpolated.scale(targetLen);
    }

    protected void updateRotationFromMotion(Vec3 motion) {
        double horizLen = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float yaw = (float) (Mth.atan2(motion.x, motion.z) * (180.0 / Math.PI));
        float pitch = (float) (Mth.atan2(motion.y, horizLen) * (180.0 / Math.PI));

        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;
    }

    protected void spawnExhaustParticles(FlightPhase phase) {
        Vec3 motion = this.getDeltaMovement();
        Vec3 back = motion.normalize().scale(-1.2);
        Vec3 exhaustPos = this.position().add(back);

        if (phase == FlightPhase.BOOST) {
            if (isAirLaunched()) {
                if (this.ticksInFlight < 8) {
                    this.level().addParticle(ParticleTypes.CLOUD, exhaustPos.x, exhaustPos.y, exhaustPos.z, 0, 0, 0);
                }
                return;
            }
            // Intense solid rocket motor flame and smoke plume
            this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    exhaustPos.x, exhaustPos.y, exhaustPos.z,
                    back.x * 0.2 + (this.random.nextGaussian() * 0.05),
                    back.y * 0.2 + (this.random.nextGaussian() * 0.05),
                    back.z * 0.2 + (this.random.nextGaussian() * 0.05));

            this.level().addParticle(ParticleTypes.FLAME,
                    exhaustPos.x, exhaustPos.y, exhaustPos.z,
                    back.x * 0.3, back.y * 0.3, back.z * 0.3);
        } else if (phase == FlightPhase.CRUISE || phase == FlightPhase.TERMINAL) {
            // Clean turbofan jet exhaust smoke trail
            this.level().addParticle(ParticleTypes.SMOKE,
                    exhaustPos.x, exhaustPos.y, exhaustPos.z,
                    back.x * 0.1, back.y * 0.1, back.z * 0.1);
        }
    }

    /**
     * Detonation handler when the missile impacts a block or target.
     */
    protected void onImpact(HitResult hitResult) {
        if (!this.level().isClientSide) {
            int penetration = getPenetrationDepth();
            if (penetration > 0 && hitResult.getType() == HitResult.Type.BLOCK) {
                // BROACH penetrating warhead logic: burrow penetration blocks in direction of motion
                Vec3 motion = this.getDeltaMovement();
                if (motion.lengthSqr() > 0.001) {
                    Vec3 burstPos = this.position().add(motion.normalize().scale(penetration));
                    this.setPos(burstPos);
                }
            }
            this.explode();
            this.discard();
        }
    }

    protected void explode() {
        this.level().explode(
                this,
                this.getX(), this.getY(), this.getZ(),
                getWarheadYield(),
                Level.ExplosionInteraction.BLOCK
        );
    }

    @Override
    public void remove(RemovalReason reason) {
        // Release chunkloading ticket cleanly on server
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            MissileChunkManager.releaseTicket(serverLevel, this);
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("FlightPhase")) {
            this.setFlightPhase(FlightPhase.values()[tag.getByte("FlightPhase")]);
        }
        if (tag.contains("TargetPos")) {
            this.setTargetPos(NbtUtils.readBlockPos(tag.getCompound("TargetPos")));
        }
        this.ticksInFlight = tag.getInt("TicksInFlight");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putByte("FlightPhase", (byte) getFlightPhase().ordinal());
        BlockPos target = getTargetPos();
        if (target != null) {
            tag.put("TargetPos", NbtUtils.writeBlockPos(target));
        }
        tag.putInt("TicksInFlight", this.ticksInFlight);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // GeckoLib 4 Animatable implementation
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Subclasses can register specific flap/wing-unfolding and engine controllers
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
